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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import net.roboconf.core.utils.Utils;
import net.roboconf.target.api.TargetException;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig.DockerClientConfigBuilder;
import com.github.dockerjava.jaxrs.DockerCmdExecFactoryImpl;

/**
 * @author Vincent Zurczak - Linagora
 */
public final class DockerUtils {

	/**
	 * Private empty constructor.
	 */
	private DockerUtils() {
		// nothing
	}


	/**
	 * Creates a Docker client from target properties.
	 * @param targetProperties a non-null map
	 * @return a Docker client
	 * @throws TargetException if something went wrong
	 */
	public static DockerClient createDockerClient( Map<String,String> targetProperties ) throws TargetException {

		// Validate what needs to be validated.
		Logger logger = Logger.getLogger( DockerHandler.class.getName());
		logger.fine( "Setting the target properties." );
		if( Utils.isEmptyOrWhitespaces( targetProperties.get( DockerHandler.IMAGE_ID ))
				&& Utils.isEmptyOrWhitespaces( targetProperties.get( DockerHandler.AGENT_PACKAGE )))
			throw new TargetException( DockerHandler.IMAGE_ID + " or " + DockerHandler.AGENT_PACKAGE + " is missing in the configuration." );

		String edpt = targetProperties.get( DockerHandler.ENDPOINT );
		if( Utils.isEmptyOrWhitespaces( edpt ))
			edpt = "http://localhost:4243";

		// The configuration is straight-forward.
		DockerClientConfigBuilder config =
				DockerClientConfig.createDefaultConfigBuilder()
				.withUri( edpt )
				.withUsername( targetProperties.get( DockerHandler.USER ))
				.withPassword( targetProperties.get( DockerHandler.PASSWORD ))
				.withEmail( targetProperties.get( DockerHandler.EMAIL ))
				.withVersion( targetProperties.get( DockerHandler.VERSION ));

		// We must force the factory because otherwise, its finding relies on services loaders.
		// And this Java mechanism does not work in OSGi.
		DockerClientBuilder clientBuilder = DockerClientBuilder
				.getInstance( config.build())
				.withDockerCmdExecFactory( new DockerCmdExecFactoryImpl());

		return clientBuilder.build();
	}


	/**
	 * Deletes a Docker image if it exists.
	 * @param imageId the image ID (not null)
	 * @param dockerClient a Docker client
	 */
	public static void deleteImageIfItExists( String imageId, DockerClient dockerClient ) {

		if( imageId != null ) {
			List<Image> images = dockerClient.listImagesCmd().exec();
			images = images == null ? new ArrayList<Image>( 0 ) : images;
			if( findImageById( imageId, images ) != null )
				dockerClient.removeImageCmd( imageId ).withForce( true ).exec();
		}
	}


	/**
	 * Finds an image by ID.
	 * @param imageId the image ID (not null)
	 * @param images a non-null list of images
	 * @return an image, or null if none was found
	 */
	public static Image findImageById( String imageId, List<Image> images ) {

		Image result = null;
		for( Image img : images ) {
			if( img.getId().equals(imageId)) {
				result = img;
				break;
			}
		}

		return result;
	}


	/**
	 * Finds a container by ID.
	 * @param containerId the container ID (not null)
	 * @param containers a non-null list of containers
	 * @return an container, or null if none was found
	 */
	public static Container findContainerById( String containerId, List<Container> containers ) {

		Container result = null;
		for( Container container : containers ) {
			if( container.getId().equals( containerId )) {
				result = container;
				break;
			}
		}

		return result;
	}


	/**
	 * Finds an image by tag.
	 * @param imageTag the image tag (not null)
	 * @param images a non-null list of images
	 * @return an image, or null if none was found
	 */
	public static Image findImageByTag( String imageTag, List<Image> images ) {

		Image result = null;
		for( Image img : images ) {
			for( String s : img.getRepoTags()) {
				if( s.contains( imageTag )) {
					result = img;
					break;
				}
			}
		}

		return result;
	}
}
