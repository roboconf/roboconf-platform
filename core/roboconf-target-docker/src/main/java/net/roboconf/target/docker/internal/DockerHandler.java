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

import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.utils.Utils;
import net.roboconf.target.api.AbstractThreadedTargetHandler;
import net.roboconf.target.api.TargetException;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse.ContainerState;
import com.github.dockerjava.api.model.Container;

/**
 * @author Pierre-Yves Gibello - Linagora
 * @author Pierre Bourret - Université Joseph Fourier
 */
public class DockerHandler extends AbstractThreadedTargetHandler {

	public static final String TARGET_ID = "docker";
	static final String MESSAGING_TYPE = "net.roboconf.messaging.type";

	static final String IMAGE_ID = "docker.image";
	static final String BASE_IMAGE = "docker.base.image";
	static final String ENDPOINT = "docker.endpoint";
	static final String USER = "docker.user";
	static final String PASSWORD = "docker.password";
	static final String EMAIL = "docker.email";
	static final String VERSION = "docker.version";
	static final String AGENT_PACKAGE = "docker.agent.package";
	static final String AGENT_JRE_AND_PACKAGES = "docker.agent.jre-packages";
	static final String AGENT_JRE_AND_PACKAGES_DEFAULT = "openjdk-7-jre-headless";
	static final String ADDITIONAL_PACKAGES = "docker.additional.packages";
	static final String ADDITIONAL_DEPLOY = "docker.additional.deploy";
	static final String RUN_EXEC = "docker.run.exec";

	// Docker exec markers for Roboconf configuration injection.
	static final String MARKER_MESSAGING_CONFIGURATION = "$msgConfig$";
	static final String MARKER_APPLICATION_NAME = "$applicationName$";
	static final String MARKER_INSTANCE_PATH = "$instancePath$";
	static final String MARKER_MESSAGING_TYPE = "$messagingType$";

	static final String OPTION_PREFIX = "docker.option.";
	static final String OPTION_PREFIX_RUN = OPTION_PREFIX + "run.";

	private final Logger logger = Logger.getLogger( getClass().getName());


	/**
	 * Constructor.
	 */
	public DockerHandler() {
		// Wait 2 seconds between every polling
		this.delay = 2000;
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

		// Search an existing image in the local Docker repository
		String imageId = targetProperties.get( IMAGE_ID );
		String pack = targetProperties.get( AGENT_PACKAGE );
		if( imageId == null && Utils.isEmptyOrWhitespaces( pack ))
			throw new TargetException("Neither " + IMAGE_ID + " nor " + AGENT_PACKAGE + " were specified.");

		// We do not do anything else here.
		// We return a UUID.
		return "rbcf_" + UUID.randomUUID().toString();
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.target.api.AbstractThreadedTargetHandler#machineConfigurator(java.util.Map,
	 * java.util.Map, java.lang.String, java.lang.String, java.lang.String, net.roboconf.core.model.beans.Instance)
	 */
	@Override
	public MachineConfigurator machineConfigurator(
			Map<String,String> targetProperties,
			Map<String,String> messagingConfiguration,
			String machineId,
			String scopedInstancePath,
			String applicationName,
			Instance scopedInstance ) {

		// machineId does not match a real container ID.
		// It is the name of the container we will create.
		return new DockerMachineConfigurator(
				targetProperties, messagingConfiguration, machineId,
				scopedInstancePath, applicationName, scopedInstance );
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
			ContainerState state = DockerUtils.getContainerState( machineId, dockerClient );
			result = state != null && state.isRunning();

		} catch( Exception e ) {
			// nothing, we consider it is not running
		}

		return result;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.target.api.TargetHandir.delete()
	 * #terminateMachine(java.util.Map, java.lang.String)
	 */
	@Override
	public void terminateMachine( Map<String,String> targetProperties, String machineId ) throws TargetException {

		this.logger.fine( "Terminating machine " + machineId );
		try {
			cancelMachineConfigurator( machineId );
			DockerClient dockerClient = DockerUtils.createDockerClient( targetProperties );
			Container container = DockerUtils.findContainerByIdOrByName( machineId, dockerClient );

			// The case "container == null" is possible.
			// Indeed, it may have been killed manually. This method will then
			// just mark the Roboconf instance as "not deployed" without throwing an exception.
			if( container != null ) {
				ContainerState state = DockerUtils.getContainerState( machineId, dockerClient );
				if( state.isRunning() || state.isPaused())
					dockerClient.killContainerCmd( container.getId()).exec();

				dockerClient.removeContainerCmd( container.getId()).withForce( true ).exec();
			}

		} catch( Exception e ) {
			throw new TargetException( e );
		}
	}
}
