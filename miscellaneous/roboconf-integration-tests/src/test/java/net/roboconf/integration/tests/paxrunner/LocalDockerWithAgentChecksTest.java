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

package net.roboconf.integration.tests.paxrunner;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;

import java.io.File;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Logger;

import javax.inject.Inject;

import net.roboconf.core.Constants;
import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.integration.probes.DmTest;
import net.roboconf.integration.tests.internal.ItUtils;
import net.roboconf.integration.tests.internal.runners.RoboconfPaxRunnerWithDocker;
import net.roboconf.target.docker.internal.DockerTestUtils;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.ProbeBuilder;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;

/**
 * A test where the DM launches Docker containers and checks agents work.
 * <p>
 * This test takes quite a long time to run (more than 3 minutes). It also requires
 * a high internet connection to update the Docker image system (sudo apt-get update...).
 * </p>
 * <p>
 * It was tested on the campus of Université Joseph Fourier (which has a very good internet
 * connection). It also needs a local Docker install and a local RabbitMQ with "roboconf/roboconf"
 * credentials ("localhost/guest/guest" does not work - "localhost" does not designate the same thing
 * for the Docker container and the host machine - and "guest/guest" only works with "localhost").
 * </p>
 * <p>
 * For all these reasons, this test is disabled (@ignored). You can activate it when you want to
 * verify Docker support with the DM, agents, Karaf and OSGi environments.
 * </p>
 *
 * @author Vincent Zurczak - Linagora
 */
@RunWith( RoboconfPaxRunnerWithDocker.class )
@ExamReactorStrategy( PerMethod.class )
@Ignore
public class LocalDockerWithAgentChecksTest extends DmTest {

	private static final String APP_LOCATION = "my.app.location";
	private static final String AGENT_LOC = "agent.loc";
	private static final String DM_DIR = "dm.dir";

	@Inject
	protected Manager manager;


	@ProbeBuilder
	public TestProbeBuilder probeConfiguration( TestProbeBuilder probe ) {

		// We need to specify the classes we need
		// and that come from external modules.
		probe.addTest( DmTest.class );
		probe.addTest( TestUtils.class );

		return probe;
	}


	@Override
	@Configuration
	public Option[] config() throws Exception {

		List<Option> options = new ArrayList<> ();
		options.addAll( Arrays.asList( super.config()));
		Logger logger = Logger.getLogger( getClass().getName());

		// Indicate the location of the application to deploy
		File resourcesDirectory = TestUtils.findApplicationDirectory( "lamp" );
		String appLocation = resourcesDirectory.getAbsolutePath();
		options.add( systemProperty( APP_LOCATION ).value( appLocation ));

		// Configure the DM's messaging
		String ipAddress = findIpAddress();
		logger.info( "Configuring the DM with IP " + ipAddress );
		options.add( editConfigurationFilePut(
				"etc/net.roboconf.messaging.rabbitmq.cfg",
				"net.roboconf.messaging.rabbitmq.server.ip",
				ipAddress ));

		options.add( editConfigurationFilePut(
				"etc/net.roboconf.messaging.rabbitmq.cfg",
				"net.roboconf.messaging.rabbitmq.server.username",
				RoboconfPaxRunnerWithDocker.RBCF ));

		options.add( editConfigurationFilePut(
				"etc/net.roboconf.messaging.rabbitmq.cfg",
				"net.roboconf.messaging.rabbitmq.server.password",
				RoboconfPaxRunnerWithDocker.RBCF ));

		// Install Docker support
		String roboconfVersion = ItUtils.findRoboconfVersion();
		options.add( mavenBundle()
				.groupId( "net.roboconf" )
				.artifactId( "roboconf-target-docker" )
				.version( roboconfVersion )
				.start());

		// Prepare the location of the agent's archive
		StringBuilder sb = new StringBuilder();
		sb.append( ".m2/repository/net/roboconf/roboconf-karaf-dist-agent/" );
		sb.append( roboconfVersion );
		sb.append( "/roboconf-karaf-dist-agent-" );
		sb.append( roboconfVersion );
		sb.append( ".tar.gz" );

		File f = new File( System.getProperty( "user.home" ), sb.toString());
		options.add( systemProperty( AGENT_LOC ).value( f.getCanonicalPath()));

		return options.toArray( new Option[ options.size()]);
	}


	@Test
	public void run() throws Exception {

		// Get the DM's data
		String dmDir = System.getProperty( DM_DIR );
		Assert.assertNotNull( dmDir );
		File dir = new File( dmDir );

		// Load the application
		String appLocation = System.getProperty( APP_LOCATION );
		ApplicationTemplate tpl = this.manager.applicationTemplateMngr().loadApplicationTemplate( new File( appLocation ));
		ManagedApplication ma = this.manager.applicationMngr().createApplication( "test", null, tpl );
		Assert.assertNotNull( ma );
		Assert.assertEquals( 1, this.manager.applicationMngr().getManagedApplications().size());

		// Write Docker properties
		File appDir = new File( dir, "application-templates/Legacy LAMP - sample/graph" );
		Assert.assertTrue( appDir.isDirectory());

		File resDir = new File( appDir, "VM" );
		Assert.assertTrue( resDir.mkdir());

		String agentLocation = System.getProperty( AGENT_LOC );
		Assert.assertNotNull( agentLocation );
		Assert.assertTrue( new File( agentLocation ).exists());

		StringBuilder sb = new StringBuilder();
		sb.append( "target.id = docker\n" );
		sb.append( "docker.endpoint = http://localhost:" + DockerTestUtils.DOCKER_TCP_PORT );
		sb.append( "\ndocker.user = roboconf-it\n" );
		sb.append( "docker.agent.package = " );
		sb.append( agentLocation );

		Utils.writeStringInto( sb.toString(), new File( resDir, Constants.TARGET_PROPERTIES_FILE_NAME ));

		// Same thing for components
		resDir = new File( appDir, "Apache" );
		Assert.assertTrue( resDir.mkdir());
		Utils.writeStringInto( "#!/bin/bash\necho apache > apache.txt", new File( resDir, "deploy.sh" ));

		resDir = new File( appDir, "Tomcat" );
		Assert.assertTrue( resDir.mkdir());
		Utils.writeStringInto( "#!/bin/bash\necho tomcat > tomcat.txt", new File( resDir, "deploy.sh" ));

		resDir = new File( appDir, "MySQL" );
		Assert.assertTrue( resDir.mkdir());
		Utils.writeStringInto( "#!/bin/bash\necho mysql > mysql.txt", new File( resDir, "deploy.sh" ));

		// Wait few seconds for the Docker handler to be registered
		Thread.sleep( 1000 * 4 );

		// Instantiate root instances
		Instance rootInstance = InstanceHelpers.findInstanceByPath( ma.getApplication(), "/MySQL VM" );
		Assert.assertNotNull( rootInstance );
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, rootInstance.getStatus());

		Instance anotherRootInstance = InstanceHelpers.findInstanceByPath( ma.getApplication(), "/Tomcat VM 1" );
		Assert.assertNotNull( anotherRootInstance );
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, anotherRootInstance.getStatus());

		Logger logger = Logger.getLogger( getClass().getName());
		logger.info( "About to deploy the first root instance." );
		try {
			// The image is generated once, on the first deployment.
			// 30 seconds is enough is the internet speed connection is very good...
			this.manager.instancesMngr().changeInstanceState( ma, rootInstance, InstanceStatus.DEPLOYED_STARTED );
			Thread.sleep( 1000 * 30 );
			Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, rootInstance.getStatus());
			logger.info( "The first root instance was sucessfully deployed." );

			// The image is reused, so we only create a new container
			logger.info( "About to deploy the second root instance." );
			this.manager.instancesMngr().changeInstanceState( ma, anotherRootInstance, InstanceStatus.DEPLOYED_STARTED );
			Thread.sleep( 1000 * 10 );
			Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, anotherRootInstance.getStatus());
			Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, rootInstance.getStatus());
			logger.info( "The second root instance was sucessfully deployed." );

		} finally {
			// Undeploy them all
			logger.info( "About to undeploy all the root instances." );
			this.manager.instancesMngr().changeInstanceState( ma, rootInstance, InstanceStatus.NOT_DEPLOYED );
			this.manager.instancesMngr().changeInstanceState( ma, anotherRootInstance, InstanceStatus.NOT_DEPLOYED );

			Thread.sleep( 300 );
			Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, anotherRootInstance.getStatus());
			Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, rootInstance.getStatus());
		}
	}


	/**
	 * Finds a Docker-reachable IP address for this machine.
	 * @return an IP address (which will never be 127.0.0.1)
	 * @throws Exception if no IP address was found
	 */
	private String findIpAddress() throws Exception {

		String ipAddress = null;
		Enumeration<?> e = NetworkInterface.getNetworkInterfaces();
		loop: while( e.hasMoreElements()) {
			NetworkInterface n = (NetworkInterface) e.nextElement();
			Enumeration<?> ee = n.getInetAddresses();

			while( ee.hasMoreElements()) {
				InetAddress i = (InetAddress) ee.nextElement();
				if( i instanceof Inet4Address
						&& ! "127.0.0.1".equals( i.getHostAddress())) {
					ipAddress = i.getHostAddress();
					break loop;
				}
			}
		}

		if( ipAddress == null )
			throw new Exception( "No IP address was found." );

		return ipAddress;
	}
}
