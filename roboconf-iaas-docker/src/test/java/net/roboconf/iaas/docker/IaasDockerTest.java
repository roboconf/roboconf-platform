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

package net.roboconf.iaas.docker;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import junit.framework.Assert;
import net.roboconf.core.utils.ProgramUtils;
import net.roboconf.core.utils.Utils;
import net.roboconf.iaas.api.IaasException;
import net.roboconf.iaas.docker.internal.DockerConstants;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.BuildImageCmd;
import com.github.dockerjava.api.command.CreateImageResponse;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig.DockerClientConfigBuilder;
import com.github.dockerjava.core.DockerClientImpl;

/**
 * @author Pierre-Yves Gibello - Linagora
 */
public class IaasDockerTest {

	private String dockerTcpPort = "4243";
	private boolean running = true;
	private String dockerImageId;
	private DockerClient docker;

	/**
	 * A method to check that Docker is installed and properly configured,
	 * and prepares Docker environment for testing.
	 * <p>
	 * If it is not running, tests in this class will be skipped.
	 * </p>
	 */
	@Before
	public void checkDockerIsInstalled() throws Exception {

		Assume.assumeTrue( this.running );
		try {
			List<String> command = new ArrayList<String> ();
			command.add( "docker" );
			command.add( "version" );

			Logger logger = Logger.getLogger( getClass().getName());
			ProgramUtils.executeCommand( logger, command, null );

			checkOrUpdateDockerTcpConfig(this.dockerTcpPort);

		} catch(Exception e) {
			Logger logger = Logger.getLogger( getClass().getName());
			logger.warning( "Tests are skipped because Docker is not installed or misconfigured." );
			logger.finest( Utils.writeException(e));

			this.running = false;
			Assume.assumeNoException(e);
		}
		
		if(! running) return;

		try {
			prepareDockerTest();
		} catch(Exception e) {
			Logger logger = Logger.getLogger( getClass().getName());
			logger.warning("Tests are skipped because of Docker problem.");
			logger.finest( Utils.writeException(e));
			this.running = false;
			Assume.assumeNoException(e);
		}
	}

	/**
	 * Prepare docker environment (image, etc...) for testing.
	 * @throws IOException
	 */
	private void prepareDockerTest() throws IOException {
		String endpoint = "http://localhost:" + this.dockerTcpPort;
		
		DockerClientConfigBuilder config = DockerClientConfig.createDefaultConfigBuilder();
		if(endpoint != null) config.withUri(endpoint);

		this.docker = new DockerClientImpl(config.build());
		
		File baseDir = new File(Thread.currentThread().getContextClassLoader()
				.getResource("image").getFile());
		BuildImageCmd img = docker.buildImageCmd(baseDir).withNoCache().withTag("roboconf-test");
		CreateImageResponse rsp = docker.createImageCmd("roboconf-test", img.getTarInputStream())
				.exec();

		this.dockerImageId = rsp.getId();
	}

	/**
	 * Test the parsing of Docker config.
	 */
	@Test
	public void testConfigurationParsing() {

		// Empty configuration
		Map<String, String> iaasProperties = new HashMap<String, String>();
		IaasDocker iaas = new IaasDocker();
		try {
			iaas.setIaasProperties( iaasProperties );
			Assert.fail( "An invalid configuration should have been detected." );

		} catch( IaasException e ) {
			Assert.assertTrue(true);
		}

		// Move on
		try {
			iaasProperties = loadIaasProperties();
			iaas.setIaasProperties( iaasProperties );
			Assert.assertTrue(true);

		} catch(Exception e) {
			Assert.fail("A valid configuration should have been detected: " + e);
		}
	}
	
	/**
	 * Test creating and deleting a Docker VM.
	 */
	@Test
	public void testCreateVM() {

		IaasDocker iaas = new IaasDocker();
		try {
			Map<String, String> conf = loadIaasProperties();
			iaas.setIaasProperties(conf);
		} catch (Exception e) {
			Assert.fail("Error setting Docker IaaS properties: " + e);
		}

		String rootInstanceName = "test";
		String applicationName = "roboconf";
		
		String ipMessagingServer;
		try {
			ipMessagingServer = java.net.InetAddress.getLocalHost().getHostAddress();
			java.net.NetworkInterface ni = java.net.NetworkInterface.getByName("eth0");    
			java.util.Enumeration<java.net.InetAddress> inetAddresses =  ni.getInetAddresses();
			while(inetAddresses.hasMoreElements()) {  
				java.net.InetAddress ia = inetAddresses.nextElement();  
				if(!ia.isLinkLocalAddress()) {  
					ipMessagingServer = ia.getHostAddress();
				}    
			}
		} catch (Exception e) {
			ipMessagingServer = "127.0.0.1";
		}

		String user = "roboconf";
		String pwd = "roboconf";
		String containerId = null;
		try {
			containerId = iaas.createVM( ipMessagingServer, user, pwd, rootInstanceName, applicationName);
		} catch (IaasException e1) {
			Assert.fail("Error creating Docker container VM.");
		}
		if(containerId == null) Assert.fail("Null Docker container ID.");
		
		System.out.println("Docker container ID=" + containerId);

		try {
			iaas.terminateVM(containerId);
		} catch (IaasException e) {
			Assert.fail("Error terminating Docker container VM #" + containerId);
		}
	}

	@After
	public void dockerCleanup() {
		if(this.docker != null) {
			this.docker.removeImageCmd(this.dockerImageId).exec();
			try { this.docker.close(); } catch (IOException e) { /*ignore*/ }
		}
	}
	
	/**
	 * Load IaaS properties for Docker configuration
	 * @return
	 * @throws Exception
	 */
	private Map<String, String> loadIaasProperties() throws Exception {
		Properties p = new Properties();
		p.load(new FileReader(new File(Thread.currentThread().getContextClassLoader()
					.getResource("conf/docker.properties").getFile())));
		HashMap<String, String> iaasProperties = new HashMap<String, String>();
		for( Map.Entry<Object,Object> entry : p.entrySet()) {
			iaasProperties.put( entry.getKey().toString(), entry.getValue().toString());
		}
		
		if(this.dockerImageId != null) iaasProperties.put(DockerConstants.IMAGE_ID, this.dockerImageId);
		return iaasProperties;
	}
	
	/**
	 * Check that Docker is configured to listen on specified TCP port.
	 * If not, try to change Docker config and restart (may require root access).
	 * @param port The TCP port for Docker daemon to listen
	 * @throws IOException, InterruptedException
	 */
	private void checkOrUpdateDockerTcpConfig(String port) throws IOException, InterruptedException {
		File dockerConf = new File("/etc/default/docker");
		if(! dockerConf.exists() || ! dockerConf.canRead())
			throw new IOException("File " + dockerConf + " not found or not readable");
		BufferedReader in = new BufferedReader(new FileReader(dockerConf));
		String line;
		boolean ok = false;
		while((line = in.readLine()) != null) {
			if(line.indexOf("#") < 0
				&& line.indexOf("DOCKER_OPTS") >= 0
				&& (line.indexOf("-H=tcp:") > 0 || line.indexOf("-H tcp:") > 0)
				&& line.indexOf(":" + port) > 0) {
					ok = true;
					break;
			}
		}
		in.close();

		if(! ok) {
			// Try to update Docker config, then restart daemon
			if(! dockerConf.canWrite()) {
				throw new IOException("No TCP configuration on port " + port
					+ " in " + dockerConf
					+ ": specify DOCKER_OPTS=\"-H tcp://0.0.0.0:"
					+ port + " -H unix:///var/run/docker.sock\"");
			}

			BufferedWriter out = new BufferedWriter(new FileWriter(dockerConf, true));
			out.append("DOCKER_OPTS=\"-H tcp://0.0.0.0:4243 -H unix:///var/run/docker.sock\"\n");
			out.close();
			
			List<String> command = new ArrayList<String> ();
			command.add("restart");
			command.add("docker");
			Logger logger = Logger.getLogger(getClass().getName());
			ProgramUtils.executeCommand(logger, command, null);
		}
	}

}
