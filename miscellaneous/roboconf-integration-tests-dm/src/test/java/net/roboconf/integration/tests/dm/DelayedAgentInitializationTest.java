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

package net.roboconf.integration.tests.dm;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.ProbeBuilder;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;

import net.roboconf.agent.AgentMessagingInterface;
import net.roboconf.core.Constants;
import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.internal.tests.TestApplicationTemplate;
import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.dm.internal.test.TestManagerWrapper;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.integration.tests.commons.AbstractIntegrationTest;
import net.roboconf.integration.tests.commons.ItConfigurationBean;
import net.roboconf.integration.tests.commons.internal.ItUtils;
import net.roboconf.integration.tests.commons.internal.parameterized.IMessagingConfiguration;
import net.roboconf.integration.tests.commons.internal.runners.RoboconfPaxRunner;
import net.roboconf.integration.tests.dm.probes.DmTest;
import net.roboconf.messaging.rabbitmq.RabbitMqConstants;

/**
 * Checks delayed initialization.
 * <p>
 * Configure an agent correctly and the DM incorrectly.<br>
 * Wait a little bit and reconfigure the DM with the right messaging
 * credentials. Make sure the agent's model is initialized correctly.
 * </p>
 *
 * <p>
 * Note: this test raised a lot of problems as it used to fail randomly.
 * When it failed, it was because AgentMessagingInterface.class was exported
 * by the probe while the implementation (*.internal.agent) came from the agent's
 * bundle. When it happened, it threw an IllegalArgumentException (which in fact
 * hid a ClassCastException - both classes were incompatible because they came from
 * different class loaders).
 * </p>
 * <p>
 * We (hopefully) solved this issue by configuring the probe, and by customizing it too
 * (we only embed the current test class and not the others from the same package),
 * and we add an OSGi import-package declaration in the probe. I am not sure at all
 * which element solves the problem. Just for history, this problem occurred with two
 * versions of PAX-exam 4.x.
 * </p>
 *
 * @author Vincent Zurczak - Linagora
 */
@RunWith( RoboconfPaxRunner.class )
@ExamReactorStrategy( PerMethod.class )
public class DelayedAgentInitializationTest extends DmTest {

	@Inject
	protected Manager manager;

	@Inject
	protected AgentMessagingInterface agentItf;


	@ProbeBuilder
	public TestProbeBuilder probeConfiguration( TestProbeBuilder probe ) {

		// We need to specify the classes we need
		// and that come from external modules.
		probe.addTest( DmTest.class );
		probe.addTest( TestApplicationTemplate.class );
		probe.addTest( TestApplication.class );
		probe.addTest( TestManagerWrapper.class );
		probe.addTest( TestUtils.class );

		probe.addTest( AbstractIntegrationTest.class );
		probe.addTest( IMessagingConfiguration.class );
		probe.addTest( ItConfigurationBean.class );

		return probe;
	}


	@Override
	@Configuration
	public Option[] config() throws Exception {

		List<Option> options = new ArrayList<> ();
		options.addAll( Arrays.asList( super.config()));

		// Generic messaging update note:
		//
		// Messaging configuration is now externalized to the RabbitMQ-specific configuration file:
		//         net.roboconf.messaging.rabbitmq.cfg
		// Because the DM and the agent run on the same platform (in this test), this messaging configuration is common
		// to both of them. As a workaround, we keep the default (valid) messaging configuration, and artificially
		// close the DM client's connection.

		// Add the configuration for the agent
		TestApplication app = new TestApplication();
		options.add( editConfigurationFilePut(
				"etc/" + Constants.KARAF_CFG_FILE_AGENT,
				"application-name",
				app.getName()));

		options.add( editConfigurationFilePut(
				"etc/" + Constants.KARAF_CFG_FILE_AGENT,
				"scoped-instance-path",
				InstanceHelpers.computeInstancePath( app.getMySqlVm())));

		options.add( editConfigurationFilePut(
				"etc/" + Constants.KARAF_CFG_FILE_AGENT,
				Constants.MESSAGING_TYPE,
				RabbitMqConstants.FACTORY_RABBITMQ));

		// Add an invalid configuration for the DM
		options.add( editConfigurationFilePut(
				"etc/net.roboconf.dm.configuration.cfg",
				"message-server-username",
				"invalid" ));

		// Deploy the agent's bundles
		String roboconfVersion = ItUtils.findRoboconfVersion();
		options.add( mavenBundle()
				.groupId( "net.roboconf" )
				.artifactId( "roboconf-plugin-api" )
				.version( roboconfVersion )
				.start());

		options.add( mavenBundle()
				.groupId( "net.roboconf" )
				.artifactId( "roboconf-agent" )
				.version( roboconfVersion )
				.start());

		options.add( mavenBundle()
				.groupId( "net.roboconf" )
				.artifactId( "roboconf-agent-default" )
				.version( roboconfVersion )
				.start());

		return options.toArray( new Option[ options.size()]);
	}


	@Test
	public void run() throws Exception {

		// Sleep for a while, to let the RabbitMQ client factory arrive.
		Thread.sleep(2000);

		// Prepare the manager's wrapper.
		TestManagerWrapper managerWrapper = new TestManagerWrapper( this.manager );

		// Artificially closes the DM-side client, to prevent Agent <-> DM exchanges.
		managerWrapper.getMessagingClient().closeConnection();

		// Make like if the DM had already deployed an application's part
		TestApplication app = new TestApplication();
		app.setDirectory( new File( this.manager.configurationMngr().getWorkingDirectory(), "tmp-test" ));
		ManagedApplication ma = new ManagedApplication( app );
		managerWrapper.addManagedApplication( ma );

		// Check the DM
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getMySqlVm().getStatus());
		Assert.assertNotNull( managerWrapper.getMessagingClient());
		Assert.assertFalse( managerWrapper.getMessagingClient().isConnected());

		// Check the agent
		Assert.assertEquals( app.getName(), this.agentItf.getApplicationName());
		Assert.assertNull( this.agentItf.getScopedInstance());
		Assert.assertNotNull( this.agentItf.getMessagingClient());
		Assert.assertTrue( this.agentItf.getMessagingClient().isConnected());

		// Both cannot communicate.
		// Let's wait a little bit and let's reconfigure the DM with the right credentials.
		// DM reconfiguration should now use the common RabbitMQ configuration (which *is* correct).
		this.manager.reconfigure();

		// Manager#reconfigure() reloads all the applications from its configuration.
		// Since we loaded one in-memory, we must restore it ourselves.
		managerWrapper.addManagedApplication( ma );

		// Force the agent to send a heart beat message.
		this.agentItf.forceHeartbeatSending();
		Thread.sleep( 400 );

		// Travis containers are sometimes very slow
		if( this.agentItf.getScopedInstance() == null )
			Thread.sleep( 400 );

		// The agent should now be configured.
		Assert.assertEquals( app.getName(), this.agentItf.getApplicationName());
		Assert.assertNotNull( this.agentItf.getMessagingClient());
		Assert.assertTrue( this.agentItf.getMessagingClient().isConnected());
		Assert.assertNotNull( this.agentItf.getScopedInstance());
		Assert.assertEquals( app.getMySqlVm(), this.agentItf.getScopedInstance());

		// And the DM should have considered the root instance as started.
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, app.getMySqlVm().getStatus());
	}
}
