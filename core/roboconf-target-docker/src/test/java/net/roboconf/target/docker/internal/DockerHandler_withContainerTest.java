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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import junit.framework.Assert;
import net.roboconf.core.utils.ProgramUtils;
import net.roboconf.core.utils.Utils;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.BuildImageCmd;
import com.github.dockerjava.api.command.CreateImageResponse;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig.DockerClientConfigBuilder;
import com.github.dockerjava.jaxrs.DockerClientBuilder;

/**
 * FIXME (VZ): tests do not run on my machine, and they will most likely not run on Travis CI.
 * <p>
 * I have a 404 error when I try to prepare the tests. I guess the image does not match
 * anything in my Docker install.
 * </p>
 *
 * @author Pierre-Yves Gibello - Linagora
 */
public class DockerHandler_withContainerTest {

	private static final String DOCKER_TCP_PORT = "4243";
	private final Logger logger = Logger.getLogger( getClass().getName());

	private boolean dockerIsInstalled = true;
	private DockerClient docker;
	private String dockerImageId;



	@Before
	public void checkDockerIsInstalled() throws Exception {

		Assume.assumeTrue( this.dockerIsInstalled );
		try {
			List<String> command = Arrays.asList( "docker", "version" );
			int exitCode = ProgramUtils.executeCommand( this.logger, command, null );
			if( exitCode != 0 )
				throw new Exception( "Docker is not installed." );

			checkOrUpdateDockerTcpConfig();
			prepareDockerTest();

		} catch( Exception e ) {
			this.logger.warning( "Tests are skipped because Docker is not installed or misconfigured." );
			this.logger.finest( Utils.writeException( e ));

			this.dockerIsInstalled = false;
			Assume.assumeNoException( e );
		}
	}


	@After
	public void dockerCleanup() {

		if( this.docker != null ) {
			if( this.dockerImageId != null )
				this.docker.removeImageCmd( this.dockerImageId ).exec();

			try {
				this.docker.close();

			} catch( IOException e ) {
				// nothing
			}
		}
	}


	@Test
	public void testValidConfiguration() throws Exception {

		Assume.assumeTrue( this.dockerIsInstalled );
		Map<String, String> targetProperties = loadTargetProperties();
		DockerHandler target = new DockerHandler();
		target.setTargetProperties( targetProperties );
	}


	@Test
	public void testCreateAndTerminateVM() throws Exception {

		Assume.assumeTrue( this.dockerIsInstalled );
		DockerHandler target = new DockerHandler();
		target.setTargetProperties( loadTargetProperties());

		String rootInstanceName = "test";
		String applicationName = "roboconf";
		String ipMessagingServer;
		try {
			ipMessagingServer = java.net.InetAddress.getLocalHost().getHostAddress();
			NetworkInterface ni = NetworkInterface.getByName( "eth0" );
			Enumeration<InetAddress> inetAddresses =  ni.getInetAddresses();
			while( inetAddresses.hasMoreElements()) {
				InetAddress ia = inetAddresses.nextElement();
				if( ! ia.isLinkLocalAddress())
					ipMessagingServer = ia.getHostAddress();
			}

		} catch( Exception e ) {
			ipMessagingServer = "127.0.0.1";
		}

		String user = "roboconf";
		String pwd = "roboconf";
		String containerId = null;

		containerId = target.createOrConfigureMachine( ipMessagingServer, user, pwd, rootInstanceName, applicationName );
		Assert.assertNotNull( containerId );
		target.terminateMachine( containerId );
	}


	/**
	 * Checks that Docker is configured to listen on the right TCP port.
	 * <p>
	 * If not, try to change Docker config and restart (may require root access).
	 * </p>
	 *
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private void checkOrUpdateDockerTcpConfig() throws IOException, InterruptedException {

		File dockerConf = new File( "/etc/default/docker" );
		if( ! dockerConf.exists())
			dockerConf = new File( "/etc/default/docker.io" );

		if( ! dockerConf.exists() || ! dockerConf.canRead())
			throw new IOException( "The docker configuration file could not be found or is not readable." );

		// Look for the expected port in the configuration file
		BufferedReader reader = null;
		boolean ok = false;
		try {
			reader = new BufferedReader( new FileReader(dockerConf));
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
				this.logger.severe( "There is no TCP configuration for port " + DOCKER_TCP_PORT + " in " + dockerConf );
				this.logger.info( "Update the file " + dockerConf + " with DOCKER_OPTS=\"-H tcp://localhost:" + DOCKER_TCP_PORT + " -H unix:///var/run/docker.sock\"" );
				throw new IOException( "The Docker configuration is missing TCP configuration." );
			}

			BufferedWriter writer = null;
			try {
				writer = new BufferedWriter( new FileWriter(dockerConf, true));
				writer.append("DOCKER_OPTS=\"-H tcp://localhost:" + DOCKER_TCP_PORT + " -H unix:///var/run/docker.sock\"\n");

			} finally {
				Utils.closeQuietly( writer );
			}

			List<String> command = Arrays.asList( "docker", "restart" );
			int exitCode = ProgramUtils.executeCommand( this.logger, command, null );
			Assert.assertEquals( 0, exitCode );
		}
	}


	/**
	 * Loads the target properties for the configuration of Docker.
	 */
	private Map<String,String> loadTargetProperties() throws Exception {

		File propertiesFile = new File( Thread.currentThread().getContextClassLoader().getResource("conf/docker.properties").getFile());
		FileInputStream fis = null;
		Properties p = new Properties();
		try {
			fis = new FileInputStream( propertiesFile );
			p.load( fis );

		} finally {
			Utils.closeQuietly( fis );
		}

		HashMap<String,String> targetProperties = new HashMap<String,String> ();
		for( Map.Entry<Object,Object> entry : p.entrySet())
			targetProperties.put( entry.getKey().toString(), entry.getValue().toString());

		if( this.dockerImageId != null )
			targetProperties.put( DockerConstants.IMAGE_ID, this.dockerImageId );

		return targetProperties;
	}


	/**
	 * Prepares the docker environment (image, etc...) for testing.
	 * @throws IOException
	 */
	private void prepareDockerTest() throws Exception {

		DockerClientConfigBuilder config = DockerClientConfig.createDefaultConfigBuilder();
		config.withUri( "http://localhost:" + DOCKER_TCP_PORT );

		this.docker = DockerClientBuilder.getInstance( config.build()).build();
		File baseDir = new File( Thread.currentThread().getContextClassLoader().getResource("image").getFile());
		BuildImageCmd img = this.docker.buildImageCmd(baseDir).withNoCache().withTag("roboconf-test");
		CreateImageResponse rsp = this.docker.createImageCmd( "roboconf-test", img.getTarInputStream()).exec();

		this.dockerImageId = rsp.getId();
	}
}
