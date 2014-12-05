/**
 * Copyright 2014 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.integration.test;

import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;

import net.roboconf.agent.internal.Agent;
import net.roboconf.agent.internal.AgentMessageProcessor;
import net.roboconf.agent.internal.misc.HeartbeatTask;
import net.roboconf.agent.internal.misc.PluginMock;
import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.dm.management.ITargetResolver;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.integration.test.IntegrationTestsUtils.MyMessageProcessor;
import net.roboconf.pax.probe.AbstractTest;
import net.roboconf.pax.probe.DmTest;
import net.roboconf.plugin.api.PluginException;
import net.roboconf.plugin.api.PluginInterface;
import net.roboconf.target.api.TargetException;
import net.roboconf.target.api.TargetHandler;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.OptionUtils;
import org.ops4j.pax.exam.ProbeBuilder;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

/**
 * A set of tests for the agent's initialization.
 * <p>
 * We launch a Karaf installation with an agent in-memory. We load
 * an application and instantiates a root instance. The new agent
 * must send an initial message to the DM to indicate it is alive.
 * It must then receive its model from the DM.
 * </p>
 *
 * @author Vincent Zurczak - Linagora
 */
@ExamReactorStrategy( PerClass.class )
public class AgentInitializationTest extends DmTest {

	private static final String APP_LOCATION = "my.app.location";

	@Inject
	protected Manager manager;

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();


	@ProbeBuilder
	public TestProbeBuilder probeConfiguration( TestProbeBuilder probe ) {

		// We need to specify the classes we need
		// and that come from external modules.
		probe.addTest( AbstractTest.class );
		probe.addTest( DmTest.class );
		probe.addTest( TestUtils.class );
		probe.addTest( TemporaryFolder.class );

		probe.addTest( MyHandler.class );
		probe.addTest( MyTargetResolver.class );
		probe.addTest( IntegrationTestsUtils.class );
		probe.addTest( MyMessageProcessor.class );

		// Classes from the agent
		probe.addTest( Agent.class );
		probe.addTest( PluginInterface.class );
		probe.addTest( PluginException.class );
		probe.addTest( PluginMock.class );
		probe.addTest( HeartbeatTask.class );
		probe.addTest( AgentMessageProcessor.class );

		return probe;
	}


	@Override
	@Configuration
	public Option[] config() {

		String appLocation = null;
		try {
			File resourcesDirectory = TestUtils.findTestFile( "/lamp", getClass());
			appLocation = resourcesDirectory.getAbsolutePath();

		} catch( Exception e ) {
			// nothing
		}

		return OptionUtils.combine(
				super.config(),
				systemProperty( APP_LOCATION ).value( appLocation ));
	}


	@Override
	public void run() throws Exception {
		Assume.assumeTrue( IntegrationTestsUtils.rabbitMqIsRunning());

		// Update the manager
		MyTargetResolver myResolver = new MyTargetResolver();

		this.manager.setConfigurationDirectoryLocation( this.folder.newFolder().getAbsolutePath());
		this.manager.setTargetResolver( myResolver );
		this.manager.reconfigure();

		// Load the application
		String appLocation = System.getProperty( APP_LOCATION );
		ManagedApplication ma = this.manager.loadNewApplication( new File( appLocation ));
		Assert.assertNotNull( ma );
		Assert.assertEquals( 1, this.manager.getAppNameToManagedApplication().size());

		// There is no agent yet (no root instance was deployed)
		Assert.assertEquals( 0, myResolver.handler.agentIdToAgent.size());

		// Instantiate a new root instance
		Instance rootInstance = InstanceHelpers.findInstanceByPath( ma.getApplication(), "/MySQL VM" );
		Assert.assertNotNull( rootInstance );
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, rootInstance.getStatus());

		this.manager.changeInstanceState( ma, rootInstance, InstanceStatus.DEPLOYED_STARTED );
		Thread.sleep( 1000 );
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, rootInstance.getStatus());

		// A new agent must have been created
		Assert.assertEquals( 1, myResolver.handler.agentIdToAgent.size());
		Agent agent = myResolver.handler.agentIdToAgent.values().iterator().next();
		Thread.sleep( 1000 );
		Assert.assertFalse( agent.needsModel());

		// Undeploy
		this.manager.changeInstanceState( ma, rootInstance, InstanceStatus.NOT_DEPLOYED );
		Thread.sleep( 300 );
		Assert.assertEquals( 0, myResolver.handler.agentIdToAgent.size());
	}


	/**
	 * @author Vincent Zurczak - Linagora
	 */
	public static final class MyTargetResolver implements ITargetResolver {
		final MyHandler handler;


		public MyTargetResolver() {
			this.handler = new MyHandler();
		}

		@Override
		public Target findTargetHandler( List<TargetHandler> target, ManagedApplication ma, Instance instance )
		throws TargetException {
			return new Target( this.handler, null );
		}
	}


	/**
	 * @author Vincent Zurczak - Linagora
	 */
	public static class MyHandler implements TargetHandler {
		final Map<String,Agent> agentIdToAgent = new ConcurrentHashMap<String,Agent> ();


		@Override
		public String getTargetId() {
			return "for test";
		}

		@Override
		public String createOrConfigureMachine(
				Map<String,String> targetProperties,
				String messagingIp,
				String messagingUsername,
				String messagingPassword,
				String rootInstanceName,
				String applicationName )
		throws TargetException {

			Agent agent = new Agent();
			agent.setApplicationName( applicationName );
			agent.setRootInstanceName( rootInstanceName );
			agent.setTargetId( "in-memory" );
			agent.setSimulatePlugins( true );
			agent.setIpAddress( "127.0.0.1" );
			agent.setMessageServerIp( messagingIp );
			agent.setMessageServerUsername( messagingUsername );
			agent.setMessageServerPassword( messagingPassword );
			agent.start();

			String key = rootInstanceName + " @ " + applicationName;
			this.agentIdToAgent.put( key, agent );

			return key;
		}

		@Override
		public void terminateMachine( Map<String,String> targetProperties, String machineId ) throws TargetException {

			Agent agent = this.agentIdToAgent.remove( machineId );
			if( agent != null )
				agent.stop();
		}
	}
}
