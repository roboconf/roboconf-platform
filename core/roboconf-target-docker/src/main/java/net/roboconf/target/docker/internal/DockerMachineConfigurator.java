/**
 * Copyright 2015-2017 Linagora, Université Joseph Fourier, Floralis
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

import static net.roboconf.target.docker.internal.DockerHandler.DEFAULT_IMAGE;
import static net.roboconf.target.docker.internal.DockerHandler.GENERATE_IMAGE_FROM;
import static net.roboconf.target.docker.internal.DockerHandler.IMAGE_ID;
import static net.roboconf.target.docker.internal.DockerHandler.OPTION_PREFIX_ENV;
import static net.roboconf.target.docker.internal.DockerHandler.OPTION_PREFIX_RUN;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.command.BuildImageResultCallback;
import com.google.common.collect.Sets;

import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.userdata.UserDataHelpers;
import net.roboconf.core.utils.ManifestUtils;
import net.roboconf.core.utils.Utils;
import net.roboconf.messaging.api.MessagingConstants;
import net.roboconf.target.api.AbstractThreadedTargetHandler.MachineConfigurator;
import net.roboconf.target.api.TargetException;
import net.roboconf.target.api.TargetHandlerParameters;

/**
 * @author Vincent Zurczak - Linagora
 * @author Pierre Bourret - Université Joseph Fourier
 */
public class DockerMachineConfigurator implements MachineConfigurator {

	DockerClient dockerClient;
	Logger logger = Logger.getLogger( getClass().getName());

	static final String USER_DATA_DIR = "/tmp/user-data/";
	static final String USER_DATA_FILE = "parameters.properties";

	private final TargetHandlerParameters parameters;
	private final String machineId;

	private final File userDataVolume;
	private final Map<String,File> containerIdToVolume;


	/**
	 * Constructor.
	 * @param parameters the target parameters
	 * @param machineId the ID machine of the machine to configure
	 * @param userDataVolume the directory into which user data volume should be created
	 * @param containerIdToVolume a map to associate container IDs with user data directories
	 */
	public DockerMachineConfigurator(
			TargetHandlerParameters parameters,
			String machineId,
			File userDataVolume,
			Map<String,File> containerIdToVolume ) {

		this.parameters = parameters;
		this.machineId = machineId;

		this.userDataVolume = userDataVolume;
		this.containerIdToVolume = containerIdToVolume;
	}


	/**
	 * @return the parameters
	 */
	public TargetHandlerParameters getParameters() {
		return this.parameters;
	}


	@Override
	public Instance getScopedInstance() {
		return this.parameters.getScopedInstance();
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

		Map<String,String> targetProperties = this.parameters.getTargetProperties();
		this.dockerClient = DockerUtils.createDockerClient( targetProperties );

		String rbcfVersion = DockerUtils.findDefaultImageVersion( ManifestUtils.findBundleVersion());
		String imageId = Utils.getValue( targetProperties, IMAGE_ID, DEFAULT_IMAGE + ":" + rbcfVersion );
		this.logger.fine( "Used image: " + imageId );

		Image img = DockerUtils.findImageByIdOrByTag( imageId, this.dockerClient );
		if( img == null )
			createImage( imageId );

		createContainer( imageId );
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

		// Extract parameters as local variables
		Map<String,String> targetProperties = this.parameters.getTargetProperties();
		String applicationName = this.parameters.getApplicationName();
		String scopedInstancePath = this.parameters.getScopedInstancePath();

		// Deal with the Docker run options
		Map<String,String> options = new HashMap<> ();
		for( Map.Entry<String,String> entry : targetProperties.entrySet()) {
			if( entry.getKey().toLowerCase().startsWith( OPTION_PREFIX_RUN )) {
				String key = entry.getKey().substring( OPTION_PREFIX_RUN.length());
				options.put( key, entry.getValue());
			}
		}

		// Deal with environment variables (user)
		List<String> env = new ArrayList<> ();
		for( Map.Entry<String,String> entry : targetProperties.entrySet()) {
			if( entry.getKey().toLowerCase().startsWith( OPTION_PREFIX_ENV )) {

				String key = entry.getKey().substring( OPTION_PREFIX_ENV.length());
				String value = entry.getValue();

				value = value.replace( "<application-name>", applicationName );
				value = value.replace( "<scoped-instance-path>", scopedInstancePath );
				value = value.replace( "<scoped-messaging_type>", Utils.getValue(
						this.parameters.getMessagingProperties(),
						MessagingConstants.MESSAGING_TYPE_PROPERTY,
						"" ));

				env.add( key + "=" + value );
			}
		}

		// Deal with environment variables (default one: the Roboconf version)
		String rbcfVersion = DockerUtils.findDefaultImageVersion( ManifestUtils.findBundleVersion());
		env.add( "RBCF_VERSION=" + rbcfVersion );

		// Deal with environment variables (default one: the user data)
		env.add( "AGENT_PARAMETERS=file:" + USER_DATA_DIR + USER_DATA_FILE );

		// Execute...
		try {
			String containerName = DockerUtils.buildContainerNameFrom( scopedInstancePath, applicationName );

			// Create a volume
			File dir = new File( this.userDataVolume, containerName );
			Utils.createDirectory( dir );
			this.containerIdToVolume.put( containerName, dir );

			String userData = UserDataHelpers.writeUserDataAsString(
					this.parameters.getMessagingProperties(),
					this.parameters.getDomain(),
					this.parameters.getApplicationName(),
					this.parameters.getScopedInstancePath());

			Utils.writeStringInto( userData, new File( dir, USER_DATA_FILE ));
			Volume volume = new Volume( USER_DATA_DIR );
			Bind volumeBind = new Bind( dir.getAbsolutePath(), volume );

			// Create the container
			CreateContainerCmd cmd = this.dockerClient.createContainerCmd( imageId )
					.withName( containerName )
					.withEnv( env )
					.withVolumes( Arrays.asList( volume ))
					.withBinds( Arrays.asList( volumeBind ));

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
			this.parameters.getScopedInstance().data.put( Instance.MACHINE_ID, container.getId());

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

		// If there is no Dockerfile, this method will do nothing
		File targetDirectory = this.parameters.getTargetPropertiesDirectory();
		String dockerFilePath = this.parameters.getTargetProperties().get( GENERATE_IMAGE_FROM );
		if( ! Utils.isEmptyOrWhitespaces( dockerFilePath ) && targetDirectory != null ) {

			this.logger.fine( "Trying to create image " + imageId + " from a Dockerfile." );
			File dockerFile = new File( targetDirectory, dockerFilePath );
			if( ! dockerFile.exists())
				throw new TargetException( "No Dockerfile was found at " + dockerFile );

			// Start the build.
			// This will block the current thread until the creation is complete.
			String builtImageId;
			this.logger.fine( "Asking Docker to build the image from our Dockerfile." );
			try {
				builtImageId = this.dockerClient
					.buildImageCmd( dockerFile )
					.withTags( Sets.newHashSet( imageId ))
					.withPull( true )
					.exec( new RoboconfBuildImageResultCallback())
					.awaitImageId();

			} catch( Exception e ) {
				Utils.logException( this.logger, e );
				throw new TargetException( e );
			}

			// No need to store the real image ID... Docker has it.
			// Besides, we search images by both IDs and tags.
			// Anyway, we can log the information.
			this.logger.fine( "Image '" + builtImageId + "' was succesfully generated by Roboconf." );
		}
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
