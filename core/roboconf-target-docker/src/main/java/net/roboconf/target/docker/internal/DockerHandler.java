/**
 * Copyright 2014 Linagora, Université Joseph Fourier
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

import java.util.Map;
import java.util.logging.Logger;

import net.roboconf.target.api.TargetException;
import net.roboconf.target.api.TargetHandler;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig.DockerClientConfigBuilder;
import com.github.dockerjava.jaxrs.DockerClientBuilder;

/**
 * @author Pierre-Yves Gibello - Linagora
 */
public class DockerHandler implements TargetHandler {

	public static final String TARGET_ID = "docker";

	private final Logger logger;
	private String machineImageId;
	private DockerClient docker;


	/**
	 * Constructor.
	 */
	public DockerHandler() {
		this.logger = Logger.getLogger( getClass().getName());
	}


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
	 * @see net.roboconf.target.api.TargetHandler#setTargetProperties(java.util.Map)
	 */
	@Override
	public void setTargetProperties( Map<String, String> targetProperties ) throws TargetException {

		this.logger.fine( "Setting the target properties." );
		if((this.machineImageId = targetProperties.get(DockerConstants.IMAGE_ID)) == null)
			throw new TargetException(DockerConstants.IMAGE_ID + " is missing.");

		String endpoint = targetProperties.get(DockerConstants.ENDPOINT);
		DockerClientConfigBuilder config = DockerClientConfig.createDefaultConfigBuilder();
		if(endpoint != null)
			config.withUri(endpoint);

		String username = targetProperties.get(DockerConstants.USER);
		if(username != null) {
			String password = targetProperties.get(DockerConstants.PASSWORD);
			String email = targetProperties.get(DockerConstants.EMAIL);
			if(password == null)
				password = "";
			if(email == null)
				email = "";

			config.withUsername(username);
			config.withPassword(password);
			config.withEmail(email);
		}

		this.docker = DockerClientBuilder.getInstance( config.build()).build();
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.target.api.TargetHandler
	 * #createOrConfigureMachine(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public String createOrConfigureMachine(
			String messagingIp,
			String messagingUsername,
			String messagingPassword,
			String rootInstanceName,
			String applicationName )
	throws TargetException {

		this.logger.fine( "Creating a new machine." );
		CreateContainerResponse container = this.docker
			.createContainerCmd(this.machineImageId)
			.withCmd("/etc/rc.local",
					applicationName,
					rootInstanceName,
					messagingIp,
					messagingUsername,
					messagingPassword)
			.exec();

		this.docker.startContainerCmd(container.getId()).exec();
		//this.docker.waitContainerCmd(container.getId()).exec();

		return container.getId();
	}

	/*
	 * (non-Javadoc)
	 * @see net.roboconf.target.api.TargetHandler
	 * #terminateMachine(java.lang.String)
	 */
	@Override
	public void terminateMachine(String instanceId) throws TargetException {

		this.logger.fine( "Terminating machine " + instanceId );
		try {
			this.docker.killContainerCmd(instanceId).exec();
			this.docker.removeContainerCmd(instanceId).exec();

		} catch( Exception e ) {
			throw new TargetException(e);
		}
	}
}
