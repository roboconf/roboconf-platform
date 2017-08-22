/**
 * Copyright 2014-2017 Linagora, Université Joseph Fourier, Floralis
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

import static net.roboconf.target.docker.internal.DockerUtils.extractBoolean;

import java.io.File;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse.ContainerState;
import com.github.dockerjava.api.model.Container;

import net.roboconf.core.utils.Utils;
import net.roboconf.target.api.AbstractThreadedTargetHandler;
import net.roboconf.target.api.TargetException;
import net.roboconf.target.api.TargetHandlerParameters;

/**
 * @author Pierre-Yves Gibello - Linagora
 * @author Pierre Bourret - Université Joseph Fourier
 * @author Amadou Diarra -  Université Joseph Fourier
 */
public class DockerHandler extends AbstractThreadedTargetHandler {

	// Other properties
	public static final String TARGET_ID = "docker";
	static final String LOCALHOST = "localhost";

	static final String DEFAULT_IMAGE = "roboconf/roboconf-agent";
	static final String DEFAULT_ENDPOINT = "tcp://localhost:4243";

	// Container creation
	static final String IMAGE_ID = "docker.image";
	static final String ENDPOINT = "docker.endpoint";
	static final String USER = "docker.user";
	static final String PASSWORD = "docker.password";
	static final String EMAIL = "docker.email";
	static final String VERSION = "docker.version";
	static final String RUN_EXEC = "docker.run.exec";

	// Image generation
	static final String GENERATE_IMAGE_FROM = "docker.generate.image.from";

	// Run options
	static final String OPTION_PREFIX = "docker.option.";
	static final String OPTION_PREFIX_RUN = OPTION_PREFIX + "run.";
	static final String OPTION_PREFIX_ENV = OPTION_PREFIX + "env.";

	// Private fields
	private final Logger logger = Logger.getLogger( getClass().getName());
	private final Map<String,File> containerIdToVolume;

	// A directory that is used to store temporary volumes for our containers
	// (can be overridden for tests)
	File userDataVolume = new File( System.getProperty( "java.io.tmpdir" ), "rbcf-docker" );


	/**
	 * Constructor.
	 */
	public DockerHandler() {
		// Wait 2 seconds between every polling
		this.delay = 2000;
		this.containerIdToVolume = new ConcurrentHashMap<> ();
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
	 * @see net.roboconf.target.api.TargetHandler
	 * #createMachine(net.roboconf.target.api.TargetHandlerParameters)
	 */
	@Override
	public String createMachine( TargetHandlerParameters parameters ) throws TargetException {

		this.logger.fine( "Creating a new machine..." );

		// We do not do anything else here.
		// We return a UUID.
		return "rbcf_" + UUID.randomUUID().toString();
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.target.api.AbstractThreadedTargetHandler#machineConfigurator(
	 * net.roboconf.target.api.TargetHandlerParameters, java.lang.String)
	 */
	@Override
	public MachineConfigurator machineConfigurator( TargetHandlerParameters parameters, String machineId ) {

		// machineId does not match a real container ID.
		// It is the name of the container we will create.
		DockerMachineConfigurator configurator = new DockerMachineConfigurator(
				parameters,
				machineId,
				this.userDataVolume,
				this.containerIdToVolume );

		return configurator;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.target.api.TargetHandler
	 * #isMachineRunning(net.roboconf.target.api.TargetHandlerParameters, java.lang.String)
	 */
	@Override
	public boolean isMachineRunning( TargetHandlerParameters parameters, String machineId )
	throws TargetException {

		boolean result = false;
		try {
			DockerClient dockerClient = DockerUtils.createDockerClient( parameters.getTargetProperties());
			ContainerState state = DockerUtils.getContainerState( machineId, dockerClient );
			result = state != null && extractBoolean( state.getRunning());

		} catch( Exception e ) {
			// nothing, we consider it is not running
			Utils.logException( this.logger, e );
		}

		return result;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.target.api.TargetHandler
	 * #terminateMachine(net.roboconf.target.api.TargetHandlerParameters, java.lang.String)
	 */
	@Override
	public void terminateMachine( TargetHandlerParameters parameters, String machineId )
	throws TargetException {

		this.logger.fine( "Terminating machine " + machineId );
		try {
			cancelMachineConfigurator( machineId );
			DockerClient dockerClient = DockerUtils.createDockerClient( parameters.getTargetProperties());
			Container container = DockerUtils.findContainerByIdOrByName( machineId, dockerClient );

			// The case "container == null" is possible.
			// Indeed, it may have been killed manually. This method will then
			// just mark the Roboconf instance as "not deployed" without throwing an exception.
			if( container != null ) {

				// Stop the container
				ContainerState state = DockerUtils.getContainerState( machineId, dockerClient );
				if( state != null
						&& ( extractBoolean( state.getRunning()) || extractBoolean( state.getPaused())))
					dockerClient.killContainerCmd( container.getId()).exec();

				dockerClient.removeContainerCmd( container.getId()).withForce( true ).exec();

				// Delete the volume we used to share user data
				// (container names are prefixed by their parent, here "/").
				// See https://github.com/moby/moby/issues/6705
				String containerName = container.getNames()[ 0 ].substring( 1 );
				File userDataDir = this.containerIdToVolume.remove( containerName );
				Utils.deleteFilesRecursivelyAndQuietly( userDataDir );
			}

		} catch( Exception e ) {
			throw new TargetException( e );
		}
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.target.api.TargetHandler
	 * #retrievePublicIpAddress(net.roboconf.target.api.TargetHandlerParameters, java.lang.String)
	 */
	@Override
	public String retrievePublicIpAddress( TargetHandlerParameters parameters, String machineId )
	throws TargetException {
		return LOCALHOST;
	}
}
