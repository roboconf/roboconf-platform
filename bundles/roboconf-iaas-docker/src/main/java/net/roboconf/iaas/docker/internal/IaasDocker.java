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

package net.roboconf.iaas.docker.internal;

import java.util.Map;
import java.util.logging.Logger;

import net.roboconf.iaas.api.IaasException;
import net.roboconf.iaas.api.IaasInterface;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig.DockerClientConfigBuilder;
import com.github.dockerjava.jaxrs.DockerClientBuilder;

/**
 * @author Pierre-Yves Gibello - Linagora
 */
public class IaasDocker implements IaasInterface {

	public static final String IAAS_TYPE = "docker";

	private final Logger logger;
	private String machineImageId;
	private DockerClient docker;

	/**
	 * Constructor.
	 */
	public IaasDocker() {
		this.logger = Logger.getLogger( getClass().getName());
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.iaas.api.IaasInterface
	 * #getIaasType()
	 */
	@Override
	public String getIaasType() {
		return IAAS_TYPE;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.iaas.api.IaasInterface
	 * #setIaasProperties(net.roboconf.iaas.api.IaasProperties)
	 */
	@Override
	public void setIaasProperties( Map<String, String> iaasProperties ) throws IaasException {

		this.logger.fine( "Setting the IaaS properties." );
		if((this.machineImageId = iaasProperties.get(DockerConstants.IMAGE_ID)) == null)
			throw new IaasException(DockerConstants.IMAGE_ID + " is missing.");

		String endpoint = iaasProperties.get(DockerConstants.ENDPOINT);
		DockerClientConfigBuilder config = DockerClientConfig.createDefaultConfigBuilder();
		if(endpoint != null)
			config.withUri(endpoint);

		String username = iaasProperties.get(DockerConstants.USER);
		if(username != null) {
			String password = iaasProperties.get(DockerConstants.PASSWORD);
			String email = iaasProperties.get(DockerConstants.EMAIL);
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
	 * @see net.roboconf.iaas.api.IaasInterface
	 * #createVM(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public String createVM(
			String messagingIp,
			String messagingUsername,
			String messagingPassword,
			String rootInstanceName,
			String applicationName )
	throws IaasException {

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
	 * @see net.roboconf.iaas.api.IaasInterface
	 * #terminateVM(java.lang.String)
	 */
	@Override
	public void terminateVM(String instanceId) throws IaasException {

		this.logger.fine( "Terminating machine " + instanceId );
		try {
			this.docker.killContainerCmd(instanceId).exec();
			this.docker.removeContainerCmd(instanceId).exec();

		} catch(Exception e) {
			throw new IaasException(e);
		}
	}
}
