/**
 * Copyright 2014-2015 Linagora, Université Joseph Fourier, Floralis
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
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import net.roboconf.core.utils.Utils;
import net.roboconf.target.api.TargetException;
import net.roboconf.target.api.TargetHandler;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Image;

/**
 * @author Pierre-Yves Gibello - Linagora
 */
public class DockerHandler implements TargetHandler {

	public static final String TARGET_ID = "docker";
	public static final String DEFAULT_IMG_NAME = "generated.by.roboconf";

	static String IMAGE_ID = "docker.image";
	static String ENDPOINT = "docker.endpoint";
	static String USER = "docker.user";
	static String PASSWORD = "docker.password";
	static String EMAIL = "docker.email";
	static String VERSION = "docker.version";
	static String AGENT_PACKAGE = "docker.agent.package";
	static String AGENT_JRE_AND_PACKAGES = "docker.agent.jre-packages";

	private final Logger logger = Logger.getLogger( getClass().getName());


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.target.api.TargetHandler#getTargetId()
	 */
	@Override
	public String getTargetId() {
		return TARGET_ID;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.target.api.TargetHandler#createMachine(java.util.Map,
	 * java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public String createMachine(
			Map<String,String> targetProperties,
			Map<String,String> messagingConfiguration,
			String scopedInstancePath,
			String applicationName )
	throws TargetException {

		this.logger.fine( "Creating a new machine." );
		DockerClient dockerClient = DockerUtils.createDockerClient( targetProperties );

		// Search an existing image in the local Docker repository
		String imageId = targetProperties.get(IMAGE_ID);
		Image image = null;
		if( ! Utils.isEmptyOrWhitespaces( imageId )) {
			List<Image> images = dockerClient.listImagesCmd().exec();
			images = images == null ? new ArrayList<Image>( 0 ) : images;

			image = DockerUtils.findImageById( imageId, images );
			if( image != null )
				this.logger.fine( "Found a Docker image with ID " + imageId );
			else if(( image = DockerUtils.findImageByTag( imageId, images )) != null )
				this.logger.fine( "Found a Docker image with tag " + imageId );
		}

		// Generate a Docker image, if possible
		if( image == null ) {

			String pack = targetProperties.get( AGENT_PACKAGE );
			if( Utils.isEmptyOrWhitespaces( pack ))
				throw new TargetException("Docker image " + imageId + " not found, and no " + AGENT_PACKAGE + " specified.");

			if( Utils.isEmptyOrWhitespaces( imageId ))
				imageId = DEFAULT_IMG_NAME;

			// Generate docker image
			this.logger.fine("Docker image not found: build one from generated Dockerfile.");
			InputStream response = null;
			File dockerfile = null;
			try {
				dockerfile = new DockerfileGenerator(pack, targetProperties.get(AGENT_JRE_AND_PACKAGES)).generateDockerfile();
				this.logger.fine( "Creating an image from the generated Dockerfile." );
				response = dockerClient.buildImageCmd( dockerfile ).withTag( imageId ).exec();

				// Check response (last line = "Successfully built <imageId>") ...
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				Utils.copyStream(response, out);
				String s = out.toString("UTF-8").trim();
				this.logger.fine( "Docker's output: " + s );

				String realImageId = s.substring(s.lastIndexOf(' ') + 1).substring(0, 12);
				this.logger.fine( "Generated image's ID: " + realImageId );

				// Tag the new image with the specified ID (so that it gets reused next time)
				dockerClient.tagImageCmd( realImageId, imageId, imageId ).exec();

			} catch( Exception e ) {
				throw new TargetException(e);

			} finally {
				Utils.closeQuietly(response);
				Utils.deleteFilesRecursivelyAndQuitely( dockerfile );
			}
		}

		// Build the command line, passing the messaging configuration.
		List<String> args = new ArrayList<>();
		args.add("/usr/local/roboconf-agent/start.sh");
		args.add("application-name=" + applicationName);
		args.add("scoped-instance-path=" + scopedInstancePath);
		args.add("messaging-type=" + messagingConfiguration.get("net.roboconf.messaging.type"));

		// TODO: modify the agent launcher script to take care of these arguments. May need to rethink the following lines. Maybe in a separate messaging configuration file
		for(Map.Entry<String, String> e : messagingConfiguration.entrySet()) {
			args.add(e.getKey() + '=' + e.getValue());
		}

		CreateContainerResponse container = dockerClient
			.createContainerCmd(imageId)
			.withCmd(args.toArray(new String[args.size()]))
			.exec();

		dockerClient.startContainerCmd(container.getId()).exec();
		return container.getId();
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.target.api.TargetHandler#configureMachine(java.util.Map,
	 * java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public void configureMachine(
		Map<String,String> targetProperties,
		Map<String,String> messagingConfiguration,
		String machineId,
		String scopedInstancePath,
		String applicationName )
	throws TargetException {
		this.logger.fine( "Configuring machine '" + machineId + "': nothing to configure with Docker." );
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.target.api.TargetHandler
	 * #isMachineRunning(java.util.Map, java.lang.String)
	 */
	@Override
	public boolean isMachineRunning( Map<String,String> targetProperties, String machineId )
	throws TargetException {

		boolean result = false;
		try {
			DockerClient dockerClient = DockerUtils.createDockerClient( targetProperties );
			List<Container> containers = dockerClient.listContainersCmd().exec();
			containers = containers == null ? new ArrayList<Container>( 0 ) : containers;
			result = DockerUtils.findContainerById( machineId, containers ) != null;

		} catch( Exception e ) {
			// nothing, we can consider it is not running
		}

		return result;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.target.api.TargetHandir.delete()
	 * #terminateMachine(java.util.Map, java.lang.String)
	 */
	@Override
	public void terminateMachine( Map<String, String> targetProperties, String instanceId ) throws TargetException {

		this.logger.fine( "Terminating machine " + instanceId );
		try {
			DockerClient dockerClient = DockerUtils.createDockerClient( targetProperties );
			dockerClient.killContainerCmd(instanceId).exec();
			dockerClient.removeContainerCmd(instanceId).exec();

		} catch( Exception e ) {
			throw new TargetException(e);
		}
	}
}
