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
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;

import java.util.List;

import javax.inject.Inject;

import net.roboconf.agent.AgentMessagingInterface;
import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.integration.probes.AbstractTest;
import net.roboconf.integration.probes.DmTest;
import net.roboconf.integration.tests.internal.RoboconfPaxRunner;

import org.junit.Assert;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.ProbeBuilder;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;

/**
 * Checks delayed initialization.
 * <p>
 * Configure an agent correctly and the DM incorrectly.<br />
 * Wait a little bit and reconfigure the DM with the right messaging
 * credentials. Make sure the agent's model is initialized correctly.
 * </p>
 *
 * @author Vincent Zurczak - Linagora
 */
@RunWith( RoboconfPaxRunner.class )
@ExamReactorStrategy( PerMethod.class )
public class AgentWithDelayedInitializationTest extends DmTest {

	@Inject
	protected Manager manager;

	@Inject
	protected AgentMessagingInterface agentItf;


	@ProbeBuilder
	public TestProbeBuilder probeConfiguration( TestProbeBuilder probe ) {

		// We need to specify the classes we need
		// and that come from external modules.
		probe.addTest( AbstractTest.class );
		probe.addTest( DmTest.class );
		probe.addTest( TestApplication.class );

		return probe;
	}


	@Override
	@Configuration
	public Option[] config() {

		List<Option> options = getBaseOptions();

		// Add a valid configuration for the agent
		options.add( editConfigurationFilePut(
				  "etc/net.roboconf.agent.configuration.cfg",
				  "message-server-ip",
				  "localhost" ));

		options.add( editConfigurationFilePut(
				  "etc/net.roboconf.agent.configuration.cfg",
				  "message-server-username",
				  "guest" ));

		options.add( editConfigurationFilePut(
				  "etc/net.roboconf.agent.configuration.cfg",
				  "message-server-password",
				  "guest" ));

		TestApplication app = new TestApplication();
		options.add( editConfigurationFilePut(
				  "etc/net.roboconf.agent.configuration.cfg",
				  "application-name",
				  app.getName()));

		options.add( editConfigurationFilePut(
				  "etc/net.roboconf.agent.configuration.cfg",
				  "root-instance-name",
				  app.getMySqlVm().getName()));

		// Add an invalid configuration for the DM
		options.add( editConfigurationFilePut(
				  "etc/net.roboconf.dm.configuration.cfg",
				  "message-server-username",
				  "invalid" ));

		// Deploy the agent's bundles
		options.add( mavenBundle()
				.groupId( "net.roboconf" )
				.artifactId( "roboconf-plugin-api" )
				.version( getRoboconfVersion())
				.start());

		options.add( mavenBundle()
				.groupId( "net.roboconf" )
				.artifactId( "roboconf-agent" )
				.version( getRoboconfVersion())
				.start());

		options.add( mavenBundle()
				.groupId( "net.roboconf" )
				.artifactId( "roboconf-agent-default" )
				.version( getRoboconfVersion())
				.start());

		return options.toArray( new Option[ options.size()]);
	}


	@Override
	public void run() throws Exception {

		// Update the manager.
		this.manager.setConfigurationDirectoryLocation( newFolder().getAbsolutePath());
		this.manager.reconfigure();

		// Make like if the DM had already deployed an application's part
		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app, null );
		this.manager.getAppNameToManagedApplication().put( app.getName(), ma );

		// Check the DM
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getMySqlVm().getStatus());
		Assert.assertNotNull( this.manager.getMessagingClient());
		Assert.assertFalse( this.manager.getMessagingClient().isConnected());

		// Check the agent
		Assert.assertEquals( app.getName(), this.agentItf.getApplicationName());
		Assert.assertNull( this.agentItf.getRootInstance());
		Assert.assertNotNull( this.agentItf.getMessagingClient());
		Assert.assertTrue( this.agentItf.getMessagingClient().isConnected());

		// Both cannot communicate.
		// Let's wait a little bit and let's reconfigure the DM with the right credentials.
		this.manager.setMessageServerUsername( "guest" );
		this.manager.reconfigure();

		// Manager#reconfigure() reloads all the applications from its configuration.
		// Since we loaded one in-memory, we must restore it ourselves.
		this.manager.getAppNameToManagedApplication().put( app.getName(), ma );

		// Force the agent to send a heart beat message.
		this.agentItf.forceHeartbeatSending();
		Thread.sleep( 400 );

		// The agent should now be configured.
		Assert.assertEquals( app.getName(), this.agentItf.getApplicationName());
		Assert.assertNotNull( this.agentItf.getMessagingClient());
		Assert.assertTrue( this.agentItf.getMessagingClient().isConnected());
		Assert.assertNotNull( this.agentItf.getRootInstance());
		Assert.assertEquals( app.getMySqlVm(), this.agentItf.getRootInstance());

		// And the DM should have considered the root instance as started.
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, app.getMySqlVm().getStatus());
	}
}
