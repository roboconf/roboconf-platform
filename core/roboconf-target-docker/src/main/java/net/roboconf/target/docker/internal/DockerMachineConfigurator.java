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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

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

	/**
	 * The various states this class instances may have.
	 * @author Vincent Zurczak - Linagora
	 */
	enum State {
		NO_IMAGE, IMAGE_UNDER_CREATION, HAS_IMAGE, DONE
	}

	private DockerClient dockerClient;
	private State state = State.NO_IMAGE;
	private final Logger logger = Logger.getLogger( getClass().getName());

	private final Map<String,String> targetProperties;
	private final Map<String,String> messagingConfiguration;
	private final String machineId, scopedInstancePath, applicationName;
	private final ConcurrentHashMap<String,String> imagesInCreation;



	/**
	 * Constructor.
	 * @param targetProperties the target properties (e.g. access key, secret key, etc.)
	 * @param messagingConfiguration the messaging configuration
	 * @param machineId the ID machine of the machine to configure
	 * @param applicationName the application name
	 * @param scopedInstancePath the path of the scoped/root instance
	 * @param imagesInCreation the images in creation (not null)
	 */
	public DockerMachineConfigurator(
			Map<String,String> targetProperties,
			Map<String,String> messagingConfiguration,
			String machineId,
			String scopedInstancePath,
			String applicationName,
			ConcurrentHashMap<String,String> imagesInCreation ) {

		this.targetProperties = targetProperties;
		this.messagingConfiguration = messagingConfiguration;
		this.applicationName = applicationName;
		this.scopedInstancePath = scopedInstancePath;
		this.machineId = machineId;
		this.imagesInCreation = imagesInCreation;
	}


	/* (non-Javadoc)
	 * @see net.roboconf.target.api.AbstractThreadedTargetHandler.MachineConfigurator
	 * #configure()
	 */
	@Override
	public boolean configure() throws TargetException {

		if( this.dockerClient == null )
			this.dockerClient = DockerUtils.createDockerClient( this.targetProperties );

		String imageId = this.targetProperties.get( DockerHandler.IMAGE_ID );
		String fixedImageId = fixImageId( imageId );
		Image image = DockerUtils.findImageByIdOrByTag( fixedImageId, this.dockerClient );

		if( image != null ) {
			this.logger.fine( "Creating image " + fixedImageId + "..." );
			this.state = State.HAS_IMAGE;

		} else if( this.state == State.NO_IMAGE ) {
			this.logger.fine( "Creating image " + fixedImageId + "..." );
			createImage( fixedImageId );
		}
		// else: the image is under creation...

		if( this.state == State.HAS_IMAGE ) {
			this.logger.fine( "Creating a container from image " + fixedImageId + "..." );
			this.imagesInCreation.remove( fixedImageId );
			createContainer( fixedImageId );
		}

		return this.state == State.DONE;
	}


	/**
	 * Creates a container.
	 * @param imageId the image ID
	 * @throws TargetException if something went wrong
	 */
	private void createContainer( String imageId ) throws TargetException {
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
			CreateContainerCmd cmd = this.dockerClient.createContainerCmd( imageId )
					.withName( this.machineId )
					.withCmd( args.toArray( new String[ args.size()]));

			//DockerUtils.configureOptions( options, cmd );
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
			this.state = State.DONE;
			this.dockerClient.close();

		} catch( Exception e ) {
			throw new TargetException( e );
		}
	}


	/**
	 * Creates an image.
	 * @param imageId the image ID
	 * @throws TargetException if something went wrong
	 */
	private void createImage( String imageId ) throws TargetException {

		// Acquire a lock for the image ID. This prevent concurrent insertions.
		// The configurator that inserted the value is in charge of creating the image.
		if( this.imagesInCreation.putIfAbsent( imageId, "anything" ) != null )
			return;

		// Create the image
		this.state = State.IMAGE_UNDER_CREATION;
		this.logger.info( "Creating image " + imageId + " from a generated Dockerfile." );

		InputStream response = null;
		File dockerfile = null;
		try {
			DockerfileGenerator gen = new DockerfileGenerator(
					this.targetProperties.get( DockerHandler.AGENT_PACKAGE ),
					this.targetProperties.get( DockerHandler.AGENT_JRE_AND_PACKAGES ),
					this.targetProperties.get( DockerHandler.BASE_IMAGE ));

			dockerfile = gen.generateDockerfile();
			response = this.dockerClient.buildImageCmd( dockerfile ).withTag( imageId ).exec();

			// Creating an image can take time, as system commands may be executed (apt-get...).
			// Copying the output makes the thread last longer. Therefore, given the way the threaded
			// target handler is implemented, other Docker creations/configurations will be delayed.
			// This is why we check whether getting the output is really necessary.
			if( this.logger.isLoggable( Level.FINE )) {
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				Utils.copyStream(response, out);
				String s = out.toString("UTF-8").trim();
				this.logger.fine( "Docker's output: " + s );
			}

			// No need to get the real image ID... Docker has it.
			// Besides, we search images by both IDs and tags.

			// Release the lock so that we can try again (e.g. if the base image was not already there).
			//this.imagesInCreation.remove( imageId );

		} catch( Exception e ) {
			// The image creation will not be tried again.
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
