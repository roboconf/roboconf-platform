/**
 * Copyright 2015-2016 Linagora, Université Joseph Fourier, Floralis
 *
 * The present code is developed in the scope of the joint LINAGORA -
 * Université Joseph Fourier - Floralis research program and is designated
 * as a "Result" pursuant to the terms and conditions of the LINAGORA
 * - Université Joseph Fourier - Floralis research program. Each copyright
 * holder of Results enumerated here above fully & independently holds complete
 * ownership of the complete Intellectual Property rights applicable to the whole
 * of said Results, and may freely exploit it in any manner which does not infringe
 * the moral rights of the other copyright holders.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.roboconf.target.docker.internal;

import static net.roboconf.target.docker.internal.DockerHandler.ADDITIONAL_DEPLOY;
import static net.roboconf.target.docker.internal.DockerHandler.ADDITIONAL_PACKAGES;
import static net.roboconf.target.docker.internal.DockerHandler.AGENT_JRE_AND_PACKAGES;
import static net.roboconf.target.docker.internal.DockerHandler.AGENT_JRE_AND_PACKAGES_DEFAULT;
import static net.roboconf.target.docker.internal.DockerHandler.AGENT_PACKAGE_URL;
import static net.roboconf.target.docker.internal.DockerHandler.BASE_IMAGE;
import static net.roboconf.target.docker.internal.DockerHandler.DEFAULT_DOCKER_IMAGE_REGISTRY;
import static net.roboconf.target.docker.internal.DockerHandler.DOCKER_IMAGE_REGISTRY;
import static net.roboconf.target.docker.internal.DockerHandler.DOWNLOAD_BASE_IMAGE;
import static net.roboconf.target.docker.internal.DockerHandler.GENERATE_IMAGE;
import static net.roboconf.target.docker.internal.DockerHandler.IMAGE_ID;
import static net.roboconf.target.docker.internal.DockerHandler.OPTION_PREFIX_RUN;
import static net.roboconf.target.docker.internal.DockerHandler.RUN_EXEC;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.PullImageCmd;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.core.command.BuildImageResultCallback;
import com.github.dockerjava.core.command.PullImageResultCallback;

import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.utils.ManifestUtils;
import net.roboconf.core.utils.MavenUtils;
import net.roboconf.core.utils.Utils;
import net.roboconf.target.api.AbstractThreadedTargetHandler.MachineConfigurator;
import net.roboconf.target.api.TargetException;

/**
 * @author Vincent Zurczak - Linagora
 * @author Pierre Bourret - Université Joseph Fourier
 */
public class DockerMachineConfigurator implements MachineConfigurator {

	static final String DEFAULT_IMG_NAME = "generated.by.roboconf";

	DockerClient dockerClient;

	private final Logger logger = Logger.getLogger( getClass().getName());
	private final Instance scopedInstance;
	private final Map<String,String> targetProperties;
	private final Map<String,String> messagingConfiguration;
	private final String machineId, scopedInstancePath, applicationName;



	/**
	 * Constructor.
	 * @param targetProperties the target properties (e.g. access key, secret key, etc.)
	 * @param messagingConfiguration the messaging configuration
	 * @param machineId the ID machine of the machine to configure
	 * @param applicationName the application name
	 * @param scopedInstancePath the path of the scoped/root instance
	 * @param scopedInstance the scoped instance
	 */
	public DockerMachineConfigurator(
			Map<String,String> targetProperties,
			Map<String,String> messagingConfiguration,
			String machineId,
			String scopedInstancePath,
			String applicationName,
			Instance scopedInstance ) {

		this.targetProperties = targetProperties;
		this.messagingConfiguration = messagingConfiguration;
		this.applicationName = applicationName;
		this.scopedInstancePath = scopedInstancePath;
		this.machineId = machineId;
		this.scopedInstance = scopedInstance;
	}


	@Override
	public Instance getScopedInstance() {
		return this.scopedInstance;
	}


	@Override
	public void close() throws IOException {
		if( this.dockerClient != null )
			this.dockerClient.close();
	}


	@Override
	public boolean configure() throws TargetException {

		// Creating a container is almost immediate.
		// And building an image with the REST API is blocking the thread until the creation is complete.
		// So, this is not asynchronous configuration.
		// Said differently, this method will be invoked only once!

		this.dockerClient = DockerUtils.createDockerClient( this.targetProperties );
		String imageId = this.targetProperties.get( IMAGE_ID );
		String fixedImageId = fixImageId( imageId );

		String generateAS = this.targetProperties.get( GENERATE_IMAGE );
		boolean generate = Boolean.parseBoolean( generateAS );
		Image img = DockerUtils.findImageByIdOrByTag( fixedImageId, this.dockerClient );
		if( generate && img == null )
			createImage( fixedImageId );

		createContainer( fixedImageId );
		return true;
	}


	/**
	 * Creates a container.
	 * @param imageId the image ID
	 * @throws TargetException if something went wrong
	 */
	void createContainer( String imageId ) throws TargetException {

		// Get the command to pass to the container
		this.logger.info( "Creating container " + this.machineId + " from image " + imageId );
		List<String> args = DockerUtils.buildRunCommand(
				this.targetProperties.get( RUN_EXEC ),
				this.messagingConfiguration , this.applicationName, this.scopedInstancePath );

		// Deal with the Docker run options
		Map<String,String> options = new HashMap<> ();
		for( Map.Entry<String,String> entry : this.targetProperties.entrySet()) {
			if( entry.getKey().toLowerCase().startsWith( OPTION_PREFIX_RUN )) {
				String key = entry.getKey().substring( OPTION_PREFIX_RUN.length());
				options.put( key, entry.getValue());
			}
		}

		// Execute...
		try {
			String containerName = this.scopedInstancePath + "_from_" + this.applicationName;
			containerName = containerName.replaceFirst( "^/", "" ).replace( "/", "-" ).replaceAll( "\\s+", "_" );

			// Prevent container names from being too long (see #480)
			if( containerName.length() > 61 )
				containerName = containerName.substring( 0, 61 );

			CreateContainerCmd cmd = this.dockerClient.createContainerCmd( imageId )
					.withName( containerName )
					.withCmd( args.toArray( new String[ args.size()]));

			DockerUtils.configureOptions( options, cmd );
			CreateContainerResponse container = cmd.exec();

			// Log warnings, if any
			if( container.getWarnings() != null
					&& container.getWarnings().length > 0
					&& this.logger.isLoggable( Level.FINE )) {

				StringBuilder sb = new StringBuilder();
				sb.append( "The following warnings have been found.\n" );
				for( String s : container.getWarnings())
					sb.append( s ).append( '\n' );

				this.logger.fine( sb.toString().trim());
			}

			// And start the container
			this.dockerClient.startContainerCmd( container.getId()).exec();

			// We're done here!
			this.logger.fine( "Container " + this.machineId + " was succesfully created as " + container.getId());

			// We replace the machine ID in the instance.
			// The configurator will be stopped anyway.
			this.scopedInstance.data.put( Instance.MACHINE_ID, container.getId());

		} catch( Exception e ) {
			throw new TargetException( e );
		}
	}


	/**
	 * Creates an image.
	 * @param imageId the image ID
	 * @throws TargetException if something went wrong
	 */
	void createImage( String imageId ) throws TargetException {
		this.logger.fine( "Trying to create image " + imageId + " from a generated Dockerfile." );

		// Find the agent's package URL
		String agentPackageUrl = this.targetProperties.get( AGENT_PACKAGE_URL );
		if( Utils.isEmptyOrWhitespaces( agentPackageUrl )) {
			String bundleVersion = ManifestUtils.findBundleVersion();
			String mavenVersion = ManifestUtils.findMavenVersion( bundleVersion );
			if( bundleVersion == null )
				throw new TargetException( "Roboconf's version could not be determined (guessing the agent package URL failed)." );

			IOException exception = null;
			try {
				agentPackageUrl = MavenUtils.findMavenUrl( "net.roboconf", "roboconf-karaf-dist-agent", mavenVersion, "tar.gz" );
			} catch( IOException e ) {
				exception = e;
			}

			if( Utils.isEmptyOrWhitespaces( agentPackageUrl )) {
				String s = "No Maven package was found for the agent distribution " + mavenVersion + " (guessing the agent package URL failed).";
				throw new TargetException( s, exception );
			}
		}

		// Verify the base image exists, if any
		// (otherwise, a default one is provided by the image generator).
		String baseImageRef = this.targetProperties.get( BASE_IMAGE );
		if( ! Utils.isEmptyOrWhitespaces( baseImageRef )) {

			Image baseImage = DockerUtils.findImageByIdOrByTag( baseImageRef, this.dockerClient );
			String downloadImage = this.targetProperties.get( DOWNLOAD_BASE_IMAGE );
			if( baseImage == null ) {

				// Should we download it?
				if( ! Boolean.parseBoolean( downloadImage ))
					throw new TargetException( "Base image '" + baseImageRef + "' was not found. Image generation is not possible." );

				// Download the base image then
				String imageRegistry = Utils.getValue( this.targetProperties, DOCKER_IMAGE_REGISTRY, DEFAULT_DOCKER_IMAGE_REGISTRY );
				this.logger.fine( "Asking Docker to download image '" + baseImageRef + "' from the registry: " + imageRegistry );
				List<String> imageInfos = Utils.splitNicely( baseImageRef, ":" );

				PullImageCmd cmd  = this.dockerClient.pullImageCmd( imageInfos.get( 0 )).withRegistry( imageRegistry );
				if( imageInfos.size() > 1 )
					cmd = cmd.withTag( imageInfos.get( 1 ));

				try {
					cmd.exec( new RoboconfPullImageResultCallback()).awaitSuccess();

				} catch( Exception e ) {
					Utils.logException( this.logger, e );
					throw new TargetException( "An error occurred while downloading image '" + baseImageRef + "'.", e );
				}

				baseImage = DockerUtils.findImageByIdOrByTag( baseImageRef, this.dockerClient);
				if( baseImage == null)
					throw new TargetException( "Base image '" + baseImageRef + "' was not found, even after a download attempt. Image generation is not possible." );

				this.logger.info( "Base image '" + baseImageRef + "' was successfulyl downloaded." );
			}
		}

		// Build the packages list.
		String packages = this.targetProperties.get( AGENT_JRE_AND_PACKAGES );
		final String additionalPackages = this.targetProperties.get( ADDITIONAL_PACKAGES );
		if( ! Utils.isEmptyOrWhitespaces(additionalPackages)) {

			// Sets to the default here, so we do not override the JRE package.
			if (Utils.isEmptyOrWhitespaces(packages))
				packages = AGENT_JRE_AND_PACKAGES_DEFAULT;

			packages = packages + ' ' + additionalPackages;
		}

		// Build the additional URLs-to-deploy list.
		final String deploy = this.targetProperties.get( ADDITIONAL_DEPLOY );
		List<String> deployList;
		if( Utils.isEmptyOrWhitespaces( deploy ))
			deployList = Collections.emptyList();
		else
			deployList = Utils.splitNicely( deploy, " " );

		// Create the image
		File dockerfile = null;
		try {
			// Generate a Dockerfile
			DockerfileGenerator gen = dockerfileGenerator( agentPackageUrl, packages, deployList, baseImageRef );
			dockerfile = gen.generateDockerfile();

			// Start the build.
			// This will block the current thread until the creation is complete.
			this.logger.fine( "Asking Docker to build the image from our Dockerfile." );
			String builtImageId = this.dockerClient
					.buildImageCmd( dockerfile )
					.withTag( imageId )
					.exec( new RoboconfBuildImageResultCallback())
					.awaitImageId();

			// No need to store the real image ID... Docker has it.
			// Besides, we search images by both IDs and tags.
			// Anyway, we can log the information anyway.
			this.logger.fine( "Image '" + builtImageId + "' was succesfully generated by Roboconf." );

		} catch( Exception e ) {
			Utils.logException( this.logger, e );
			throw new TargetException( e );

		} finally {
			Utils.deleteFilesRecursivelyAndQuietly( dockerfile );
		}
	}


	/**
	 * @param baseImageRef
	 * @param deployList
	 * @param packages
	 * @param agentPackageUrl
	 * @return a new Docker image generator (externalized for tests)
	 */
	DockerfileGenerator dockerfileGenerator(String agentPackageUrl , String packages , List<String> deployList , String baseImageRef ) {
		return new DockerfileGenerator( agentPackageUrl, packages, deployList, baseImageRef );
	}


	/**
	 * @param imageId
	 * @return a non-null string
	 */
	static String fixImageId( String imageId ) {
		return Utils.isEmptyOrWhitespaces( imageId ) ? DEFAULT_IMG_NAME : imageId;
	}


	/**
	 * A call back invoked when a Docker image was pulled.
	 * <p>
	 * No anonymous class in Roboconf.
	 * </p>
	 *
	 * @author Vincent Zurczak - Linagora
	 */
	static class RoboconfPullImageResultCallback extends PullImageResultCallback {
		// nothing
	}


	/**
	 * A call back invoked when a Docker image was built.
	 * <p>
	 * No anonymous class in Roboconf.
	 * </p>
	 *
	 * @author Vincent Zurczak - Linagora
	 */
	static class RoboconfBuildImageResultCallback extends BuildImageResultCallback {
		// nothing
	}
}
