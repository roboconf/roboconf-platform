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

package net.roboconf.target.docker.internal.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.junit.Assert;

import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.utils.ProgramUtils;
import net.roboconf.core.utils.Utils;

/**
 * @author Vincent Zurczak - Linagora
 * @author Pierre-Yves Gibello - Linagora
 * @author Pierre Bourret - Université Joseph Fourier
 */
public final class DockerTestUtils {

	public static final String DOCKER_TCP_PORT = "4243";

	/**
	 * The maximum time we allow the docker handler to take to configure a machine.
	 * <p>
	 * Installing packages may take a while... So this value is quite large!
	 */
	public static final long DOCKER_CONFIGURE_TIMEOUT = 150000L;


	/**
	 * Private empty constructor.
	 */
	private DockerTestUtils() {
		// nothing
	}


	/**
	 * Checks that Docker is installed and configures it if necessary.
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public static void checkDockerIsInstalled() throws IOException, InterruptedException {

		Logger logger = Logger.getLogger( DockerTestUtils.class.getName());
		List<String> command = Arrays.asList( "docker", "version" );
		int exitCode = ProgramUtils.executeCommand( logger, command, null, null, null, null);
		if( exitCode != 0 )
			throw new IOException( "Docker is not installed." );

		checkOrUpdateDockerTcpConfig( logger );
	}


	/**
	 * Checks that Docker is configured to listen on the right TCP port.
	 * <p>
	 * If not, try to change Docker config and restart (may require root access).
	 * </p>
	 *
	 * @param logger
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private static void checkOrUpdateDockerTcpConfig( Logger logger )
	throws IOException, InterruptedException {

		File dockerConf = new File( "/etc/default/docker" );
		if( ! dockerConf.exists())
			dockerConf = new File( "/etc/default/docker.io" );

		if( ! dockerConf.exists() || ! dockerConf.canRead())
			throw new IOException( "The docker configuration file could not be found or is not readable." );

		// Look for the expected port in the configuration file
		BufferedReader reader = null;
		boolean ok = false;
		try {
			reader = new BufferedReader( new InputStreamReader( new FileInputStream( dockerConf ), StandardCharsets.UTF_8 ));
			String line;
			while( ! ok && (line = reader.readLine()) != null) {
				if( line.indexOf("#") < 0
					&& line.indexOf("DOCKER_OPTS") >= 0
					&& (line.indexOf("-H=tcp:") > 0 || line.indexOf("-H tcp:") > 0)
					&& line.indexOf( ":" + DOCKER_TCP_PORT ) > 0)
						ok = true;
			}

		} finally {
			Utils.closeQuietly( reader );
		}

		// If not present, try to update the file
		if( ! ok ) {
			if( ! dockerConf.canWrite()) {
				logger.severe( "There is no TCP configuration for port " + DOCKER_TCP_PORT + " in " + dockerConf );
				logger.info( "Update the file " + dockerConf + " with DOCKER_OPTS=\"-H tcp://localhost:" + DOCKER_TCP_PORT + " -H unix:///var/run/docker.sock\"" );
				throw new IOException( "The Docker configuration is missing TCP configuration." );
			}

			OutputStreamWriter writer = null;
			try {
				writer = new OutputStreamWriter( new FileOutputStream( dockerConf, true ), StandardCharsets.UTF_8 );
				writer.append("DOCKER_OPTS=\"-H tcp://localhost:" + DOCKER_TCP_PORT + " -H unix:///var/run/docker.sock\"\n");

			} finally {
				Utils.closeQuietly( writer );
			}

			List<String> command = Arrays.asList( "docker", "restart" );
			int exitCode = ProgramUtils.executeCommand( logger, command, null, null, null, null);
			Assert.assertEquals( 0, exitCode );
		}
	}

	/**
	 * Wait until the Docker target updates its machine id, or the timeout expires...
	 * @param machineId the current machine id.
	 * @param instanceData the instance data to pull for updates.
	 * @param timeOut the time period to wait before abandoning.
	 * @return the updated Docker machine id, or {@code null} if the timeout has expired.
	 * @throws InterruptedException if interrupted while waiting for update.
	 */
	public static String waitForMachineId(
			final String machineId,
			final Map<String, String> instanceData,
			long timeOut )
	throws InterruptedException {

		long deadLine = System.currentTimeMillis() + timeOut;
		String containerId;
		do {
			containerId = instanceData.get(Instance.MACHINE_ID);
			if (containerId != null && !containerId.equals(machineId)) {
				break;
			}
			Thread.sleep(1000);
		} while (System.currentTimeMillis() < deadLine);

		return !machineId.equals(containerId) ? containerId : null;
	}
}
