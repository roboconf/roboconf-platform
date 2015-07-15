/**
 * Copyright 2015 Linagora, Université Joseph Fourier, Floralis
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.utils.Utils;
import net.roboconf.target.api.AbstractThreadedTargetHandler.MachineConfigurator;
import net.roboconf.target.api.TargetException;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Image;

/**
 * @author Vincent Zurczak - Linagora
 */
public class DockerMachineConfigurator implements MachineConfigurator {

	private static final String DEFAULT_IMG_NAME = "generated.by.roboconf";

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
			ConcurrentHashMap<String,String> imagesInCreation,
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

		this.dockerClient = DockerUtils.createDockerClient( this.targetProperties );
		String imageId = this.targetProperties.get( DockerHandler.IMAGE_ID );
		String fixedImageId = fixImageId( imageId );

		Image image = DockerUtils.findImageByIdOrByTag( fixedImageId, this.dockerClient );
		if( image == null )
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
		this.logger.info( "Creating container " + this.machineId + " from image " + imageId );

		// Build the command line, passing the messaging configuration.
		List<String> args = new ArrayList<> ();

		// We do not have to execute our command. A default command may have been set into the Dockerfile.
		// In this case, it means the user does not want the DM to pass dynamic parameters in the command.
		// We at least use it in tests (do not launch a real Roboconf agent, just an empty container).
		String useCommandAS = this.targetProperties.get( DockerHandler.USE_COMMAND );
		boolean useCommand = useCommandAS == null ? true : Boolean.valueOf( useCommandAS );

		if( useCommand ) {
			// Add the command
			String command = this.targetProperties.get( DockerHandler.COMMAND );
			if( Utils.isEmptyOrWhitespaces( command ))
				command = "/usr/local/roboconf-agent/start.sh";

			args.add( command );

			// Pass the name of the configuration file for the messaging.
			// By convention, we will pass it as the first command argument.
			String messagingType = this.messagingConfiguration.get( "net.roboconf.messaging.type" );
			args.add( "etc/net.roboconf.messaging." + messagingType + ".cfg" );

			// Agent configuration is prefixed with 'agent.'
			args.add( "agent.application-name=" + this.applicationName );
			args.add( "agent.scoped-instance-path=" + this.scopedInstancePath );
			args.add( "agent.messaging-type=" + messagingType );

			// Messaging parameters are prefixed with 'msg.'
			for( Map.Entry<String,String> e : this.messagingConfiguration.entrySet())
				args.add( "msg." + e.getKey() + '=' + e.getValue());
		}

		// Deal with the options.
		Map<String,String> options = new HashMap<> ();
		for( Map.Entry<String,String> entry : this.targetProperties.entrySet()) {
			if( entry.getKey().toLowerCase().startsWith( DockerHandler.OPTION_PREFIX_RUN )) {
				String key = entry.getKey().substring( DockerHandler.OPTION_PREFIX_RUN.length());
				options.put( key, entry.getValue());
			}
		}

		// Execute...
		try {
			String containerName = this.scopedInstancePath.replaceFirst( "^/", "" ).replace( "/", "-" ).replaceAll( "\\s+", "_" );
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
					sb.append( s + "\n" );

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

		// Verify the base image exists, if any
		String baseImageRef = this.targetProperties.get( DockerHandler.BASE_IMAGE );
		if( ! Utils.isEmptyOrWhitespaces( baseImageRef )) {
			Image baseImage = DockerUtils.findImageByIdOrByTag( baseImageRef, this.dockerClient );
			if( baseImage == null )
				throw new TargetException( "Base image '" + baseImageRef + "' was not found. Image generation is not possible." );
		}

		// Create the image
		InputStream response = null;
		File dockerfile = null;
		try {
			// Generate a Dockerfile
			DockerfileGenerator gen = new DockerfileGenerator(
					this.targetProperties.get( DockerHandler.AGENT_PACKAGE ),
					this.targetProperties.get( DockerHandler.AGENT_JRE_AND_PACKAGES ),
					baseImageRef );

			dockerfile = gen.generateDockerfile();

			// Start the build.
			// This will block the current thread until the creation is complete.
			this.logger.fine( "Asking Docker to build the image from our Dockerfile." );
			response = this.dockerClient.buildImageCmd( dockerfile ).withTag( imageId ).exec();

			// Reading the stream does not take time as everything is sent at once by Docker.
			if( this.logger.isLoggable( Level.FINE )) {
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				Utils.copyStream(response, out);
				String s = out.toString("UTF-8").trim();
				this.logger.fine( "Docker's output: " + s );
			}

			// No need to get the real image ID... Docker has it.
			// Besides, we search images by both IDs and tags.

		} catch( Exception e ) {
			throw new TargetException( e );

		} finally {
			Utils.closeQuietly( response );
			Utils.deleteFilesRecursivelyAndQuitely( dockerfile );
		}
	}


	/**
	 * @param imageId
	 * @return a non-null string
	 */
	private String fixImageId( String imageId ) {
		return Utils.isEmptyOrWhitespaces( imageId ) ? DEFAULT_IMG_NAME : imageId;
	}
}
