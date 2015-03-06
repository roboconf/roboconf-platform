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

package net.roboconf.integration.tests;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;

import java.io.File;
import java.net.Inet4Address;
import java.util.List;
import java.util.logging.Logger;

import javax.inject.Inject;

import net.roboconf.core.Constants;
import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.integration.probes.AbstractTest;
import net.roboconf.integration.probes.DmTest;
import net.roboconf.integration.tests.internal.RoboconfPaxRunnerWithDocker;
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
		probe.addTest( AbstractTest.class );
		probe.addTest( DmTest.class );
		probe.addTest( TestUtils.class );

		return probe;
	}


	@Override
	@Configuration
	public Option[] config() throws Exception {

		List<Option> options = getBaseOptions();
		Logger logger = Logger.getLogger( getClass().getName());

		// Indicate the location of the application to deploy
		File resourcesDirectory = TestUtils.findTestFile( "/lamp", getClass());
		String appLocation = resourcesDirectory.getAbsolutePath();
		options.add( systemProperty( APP_LOCATION ).value( appLocation ));

		// Configure the DM
		File dir = newFolder();
		options.add( systemProperty( DM_DIR ).value( dir.getCanonicalPath()));
		options.add( editConfigurationFilePut(
				"etc/net.roboconf.dm.configuration.cfg",
				"configuration-directory-location",
				dir.getCanonicalPath()));

		String ipAddress = Inet4Address.getLocalHost().getHostAddress();
		logger.info( "Configuring the DM with IP " + ipAddress );
		options.add( editConfigurationFilePut(
				"etc/net.roboconf.dm.configuration.cfg",
				"message-server-ip",
				ipAddress ));

		// Install Docker support
		String roboconfVersion = getRoboconfVersion();
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


	@Override
	@Test
	public void run() throws Exception {

		// Get the DM's data
		String dmDir = System.getProperty( DM_DIR );
		Assert.assertNotNull( dmDir );
		File dir = new File( dmDir );

		// Load the application
		String appLocation = System.getProperty( APP_LOCATION );
		ManagedApplication ma = this.manager.loadNewApplication( new File( appLocation ));
		Assert.assertNotNull( ma );
		Assert.assertEquals( 1, this.manager.getAppNameToManagedApplication().size());

		// Write Docker properties
		File appDir = new File( dir, "applications/Legacy LAMP/graph" );
		Assert.assertTrue( appDir.isDirectory());

		File resDir = new File( appDir, "VM" );
		Assert.assertTrue( resDir.mkdir());

		String agentLocation = System.getProperty( AGENT_LOC );
		Assert.assertNotNull( agentLocation );
		System.out.println( agentLocation );
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

		// Instantiate root instances
		Instance rootInstance = InstanceHelpers.findInstanceByPath( ma.getApplication(), "/MySQL VM" );
		Assert.assertNotNull( rootInstance );
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, rootInstance.getStatus());

		Instance anotherRootInstance = InstanceHelpers.findInstanceByPath( ma.getApplication(), "/Tomcat VM 1" );
		Assert.assertNotNull( anotherRootInstance );
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, anotherRootInstance.getStatus());
		try {
			this.manager.changeInstanceState( ma, rootInstance, InstanceStatus.DEPLOYED_STARTED );
			Thread.sleep( 800 );
			Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, rootInstance.getStatus());

			this.manager.changeInstanceState( ma, anotherRootInstance, InstanceStatus.DEPLOYED_STARTED );
			Thread.sleep( 800 );
			Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, anotherRootInstance.getStatus());
			Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, rootInstance.getStatus());

		} finally {
			// Undeploy them all
			this.manager.changeInstanceState( ma, rootInstance, InstanceStatus.NOT_DEPLOYED );
			this.manager.changeInstanceState( ma, anotherRootInstance, InstanceStatus.NOT_DEPLOYED );

			Thread.sleep( 300 );
			Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, anotherRootInstance.getStatus());
			Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, rootInstance.getStatus());
		}
	}
}
