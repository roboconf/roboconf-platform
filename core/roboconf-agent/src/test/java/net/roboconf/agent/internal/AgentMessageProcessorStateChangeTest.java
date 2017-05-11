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

package net.roboconf.agent.internal;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import net.roboconf.agent.internal.misc.PluginMock;
import net.roboconf.agent.internal.test.AgentTestUtils;
import net.roboconf.core.internal.tests.TestApplicationTemplate;
import net.roboconf.core.model.beans.Component;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.messaging.api.MessagingConstants;
import net.roboconf.messaging.api.factory.MessagingClientFactoryRegistry;
import net.roboconf.messaging.api.internal.client.test.TestClientFactory;
import net.roboconf.messaging.api.messages.from_dm_to_agent.MsgCmdChangeInstanceState;
import net.roboconf.messaging.api.messages.from_dm_to_agent.MsgCmdSetScopedInstance;
import net.roboconf.plugin.api.PluginException;
import net.roboconf.plugin.api.PluginInterface;

/**
 * @author Vincent Zurczak - Linagora
 */
public class AgentMessageProcessorStateChangeTest {

	private Agent agent;


	@Before
	public void initializeAgent() throws Exception {

		final MessagingClientFactoryRegistry registry = new MessagingClientFactoryRegistry();
		registry.addMessagingClientFactory(new TestClientFactory());
		this.agent = new Agent();

		// We first need to start the agent, so it creates the reconfigurable messaging client.
		this.agent.setMessagingType(MessagingConstants.FACTORY_TEST);
		this.agent.start();

		// We then set the factory registry of the created client, and reconfigure the agent, so the messaging client backend is created.
		this.agent.getMessagingClient().setRegistry(registry);
		this.agent.reconfigure();

		Thread.sleep( 200 );
		AgentTestUtils.getInternalClient( this.agent.getMessagingClient()).clearMessages();
	}


	@After
	public void stopAgent() {
		this.agent.stop();
	}


	@Test
	public void testSetMessagingClient() throws Exception {

		TestApplicationTemplate app = new TestApplicationTemplate();
		AgentMessageProcessor processor = (AgentMessageProcessor) this.agent.getMessagingClient().getMessageProcessor();

		// Initialize the model
		processor.processMessage( new MsgCmdSetScopedInstance( app.getTomcatVm()));
		Assert.assertEquals( app.getTomcatVm(), processor.scopedInstance );

		// Try to deploy sub-child should fail
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, processor.scopedInstance.getStatus());
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getTomcat().getStatus());
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getWar().getStatus());

		processor.processMessage( new MsgCmdChangeInstanceState( app.getWar(), InstanceStatus.DEPLOYED_STOPPED ));
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, processor.scopedInstance.getStatus());
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getTomcat().getStatus());
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getWar().getStatus());

		processor.processMessage( new MsgCmdChangeInstanceState( app.getWar(), InstanceStatus.DEPLOYED_STARTED ));
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, processor.scopedInstance.getStatus());
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getTomcat().getStatus());
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getWar().getStatus());

		// Test the Tomcat life cycle
		processor.processMessage( new MsgCmdChangeInstanceState( app.getTomcat(), InstanceStatus.DEPLOYED_STOPPED ));
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, processor.scopedInstance.getStatus());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STOPPED, app.getTomcat().getStatus());
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getWar().getStatus());

		processor.processMessage( new MsgCmdChangeInstanceState( app.getTomcat(), InstanceStatus.NOT_DEPLOYED ));
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, processor.scopedInstance.getStatus());
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getTomcat().getStatus());
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getWar().getStatus());

		processor.processMessage( new MsgCmdChangeInstanceState( app.getTomcat(), InstanceStatus.DEPLOYED_STARTED ));
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, processor.scopedInstance.getStatus());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, app.getTomcat().getStatus());
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getWar().getStatus());

		processor.processMessage( new MsgCmdChangeInstanceState( app.getTomcat(), InstanceStatus.NOT_DEPLOYED ));
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, processor.scopedInstance.getStatus());
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getTomcat().getStatus());
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getWar().getStatus());

		// Deploy the WAR when Tomcat is stopped => OK
		processor.processMessage( new MsgCmdChangeInstanceState( app.getTomcat(), InstanceStatus.DEPLOYED_STOPPED ));
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, processor.scopedInstance.getStatus());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STOPPED, app.getTomcat().getStatus());
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getWar().getStatus());

		processor.processMessage( new MsgCmdChangeInstanceState( app.getWar(), InstanceStatus.DEPLOYED_STOPPED ));
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, processor.scopedInstance.getStatus());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STOPPED, app.getTomcat().getStatus());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STOPPED, app.getWar().getStatus());

		// Start the WAR when Tomcat is stopped => fail
		processor.processMessage( new MsgCmdChangeInstanceState( app.getWar(), InstanceStatus.DEPLOYED_STARTED ));
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, processor.scopedInstance.getStatus());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STOPPED, app.getTomcat().getStatus());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STOPPED, app.getWar().getStatus());

		// Start the WAR when Tomcat is started => OK
		processor.processMessage( new MsgCmdChangeInstanceState( app.getTomcat(), InstanceStatus.DEPLOYED_STARTED ));
		processor.processMessage( new MsgCmdChangeInstanceState( app.getWar(), InstanceStatus.NOT_DEPLOYED ));
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, processor.scopedInstance.getStatus());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, app.getTomcat().getStatus());
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getWar().getStatus());

		processor.processMessage( new MsgCmdChangeInstanceState( app.getTomcat(), InstanceStatus.DEPLOYED_STARTED ));
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, processor.scopedInstance.getStatus());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, app.getTomcat().getStatus());
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getWar().getStatus());

		processor.processMessage( new MsgCmdChangeInstanceState( app.getWar(), InstanceStatus.DEPLOYED_STARTED ));
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, processor.scopedInstance.getStatus());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, app.getTomcat().getStatus());
		// The WAR needs a MySQL
		Assert.assertEquals( InstanceStatus.UNRESOLVED, app.getWar().getStatus());

		// What happens now if we stop Tomcat? The WAR should be stopped too.
		processor.processMessage( new MsgCmdChangeInstanceState( app.getTomcat(), InstanceStatus.DEPLOYED_STOPPED ));
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, processor.scopedInstance.getStatus());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STOPPED, app.getTomcat().getStatus());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STOPPED, app.getWar().getStatus());

		// Same with undeploy
		processor.processMessage( new MsgCmdChangeInstanceState( app.getTomcat(), InstanceStatus.NOT_DEPLOYED ));
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, processor.scopedInstance.getStatus());
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getTomcat().getStatus());
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getWar().getStatus());

		// Can we undeploy a root instance? No, we should not.
		processor.processMessage( new MsgCmdChangeInstanceState( app.getTomcatVm(), InstanceStatus.NOT_DEPLOYED ));
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, app.getTomcatVm().getStatus());

		// Stop an instance in the "unresolved" state
		app.getTomcat().setStatus( InstanceStatus.UNRESOLVED );
		Assert.assertEquals( InstanceStatus.UNRESOLVED, app.getTomcat().getStatus());

		processor.processMessage( new MsgCmdChangeInstanceState( app.getTomcat(), InstanceStatus.DEPLOYED_STARTED ));
		Assert.assertEquals( InstanceStatus.UNRESOLVED, app.getTomcat().getStatus());

		processor.processMessage( new MsgCmdChangeInstanceState( app.getTomcat(), InstanceStatus.DEPLOYED_STOPPED ));
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, processor.scopedInstance.getStatus());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STOPPED, app.getTomcat().getStatus());
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getWar().getStatus());

		// Undeploy an instance in the "unresolved" state
		app.getTomcat().setStatus( InstanceStatus.UNRESOLVED );
		Assert.assertEquals( InstanceStatus.UNRESOLVED, app.getTomcat().getStatus());

		processor.processMessage( new MsgCmdChangeInstanceState( app.getTomcat(), InstanceStatus.DEPLOYED_STARTED ));
		Assert.assertEquals( InstanceStatus.UNRESOLVED, app.getTomcat().getStatus());

		processor.processMessage( new MsgCmdChangeInstanceState( app.getTomcat(), InstanceStatus.NOT_DEPLOYED ));
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, processor.scopedInstance.getStatus());
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getTomcat().getStatus());
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getWar().getStatus());
	}


	@Test
	public void testStateChangeWithUnknownInstance() {

		AgentMessageProcessor processor = (AgentMessageProcessor) this.agent.getMessagingClient().getMessageProcessor();

		// No local model yet
		Instance inst = new Instance( "inst" ).status( InstanceStatus.DEPLOYED_STOPPED ).component( new Component( "unknown" ));
		processor.processMessage( new MsgCmdChangeInstanceState( inst, InstanceStatus.DEPLOYED_STARTED ));
		Assert.assertEquals( InstanceStatus.DEPLOYED_STOPPED, inst.getStatus());

		// Same thing with a local model
		processor.scopedInstance = new TestApplicationTemplate().getMySqlVm();
		processor.processMessage( new MsgCmdChangeInstanceState( inst, InstanceStatus.DEPLOYED_STARTED ));
		Assert.assertEquals( InstanceStatus.DEPLOYED_STOPPED, inst.getStatus());
	}


	@Test
	public void testStateChangeWithUnknownPlugin() {

		this.agent.stop();
		this.agent = new Agent() {
			@Override
			public PluginInterface findPlugin( Instance instance ) {
				return null;
			}
		};

		this.agent.setMessagingType(MessagingConstants.FACTORY_TEST);
		this.agent.start();
		AgentMessageProcessor processor = (AgentMessageProcessor) this.agent.getMessagingClient().getMessageProcessor();

		TestApplicationTemplate app = new TestApplicationTemplate();
		app.getMySql().getComponent().setInstallerName( "unknown installer" );

		// Initialize the model
		processor.processMessage( new MsgCmdSetScopedInstance( app.getMySqlVm()));
		Assert.assertEquals( app.getMySqlVm(), processor.scopedInstance );
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, app.getMySqlVm().getStatus());
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getMySql().getStatus());

		processor.processMessage( new MsgCmdChangeInstanceState( app.getMySql(), InstanceStatus.DEPLOYED_STOPPED ));
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, app.getMySqlVm().getStatus());
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getMySql().getStatus());
	}


	@Test
	public void testStateChangeWithTransitiveState() {

		AgentMessageProcessor processor = (AgentMessageProcessor) this.agent.getMessagingClient().getMessageProcessor();
		TestApplicationTemplate app = new TestApplicationTemplate();
		processor.scopedInstance = app.getMySqlVm();

		// Unstable (transitive) states => no state change
		for( InstanceStatus status : InstanceStatus.values()) {
			if( status.isStable())
				continue;

			app.getMySql().setStatus( status );
			processor.processMessage( new MsgCmdChangeInstanceState( app.getMySql(), InstanceStatus.DEPLOYED_STARTED ));
			Assert.assertEquals( status, app.getMySql().getStatus());
		}
	}


	@Test
	public void testStateAdvancedChange_startFails() {

		// Initialize all the stuff.
		// The plug-in will fail on "start".
		this.agent.stop();
		this.agent = new Agent() {
			@Override
			public PluginInterface findPlugin( Instance instance ) {
				return new PluginMock() {
					@Override
					public void start( Instance instance ) throws PluginException {
						throw new PluginException( "For tests..." );
					}
				};
			}
		};

		this.agent.setMessagingType(MessagingConstants.FACTORY_TEST);
		this.agent.start();
		AgentMessageProcessor processor = (AgentMessageProcessor) this.agent.getMessagingClient().getMessageProcessor();

		TestApplicationTemplate app = new TestApplicationTemplate();
		processor.processMessage( new MsgCmdSetScopedInstance( app.getTomcatVm()));
		Assert.assertEquals( app.getTomcatVm(), processor.scopedInstance );
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, processor.scopedInstance.getStatus());

		/*
		 * Scenario:
		 * 1. Tomcat is not deployed.
		 * 2. Request it to reach the "started" state. Deploy will work, but start will fail.
		 * 3. Try to stop it.
		 * 4. Try to undeploy it.
		 */
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getTomcat().getStatus());
		processor.processMessage( new MsgCmdChangeInstanceState( app.getTomcat(), InstanceStatus.DEPLOYED_STARTED ));
		Assert.assertEquals( InstanceStatus.DEPLOYED_STOPPED, app.getTomcat().getStatus());

		processor.processMessage( new MsgCmdChangeInstanceState( app.getTomcat(), InstanceStatus.DEPLOYED_STOPPED ));
		Assert.assertEquals( InstanceStatus.DEPLOYED_STOPPED, app.getTomcat().getStatus());

		processor.processMessage( new MsgCmdChangeInstanceState( app.getTomcat(), InstanceStatus.NOT_DEPLOYED ));
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getTomcat().getStatus());
	}


	@Test
	public void testStateAdvancedChange_deployFails() {

		// Initialize all the stuff.
		// The plug-in will fail on "start".
		this.agent.stop();
		this.agent = new Agent() {
			@Override
			public PluginInterface findPlugin( Instance instance ) {
				return new PluginMock() {
					@Override
					public void deploy( Instance instance ) throws PluginException {
						throw new PluginException( "For tests..." );
					}
				};
			}
		};

		this.agent.setMessagingType(MessagingConstants.FACTORY_TEST);
		this.agent.start();
		AgentMessageProcessor processor = (AgentMessageProcessor) this.agent.getMessagingClient().getMessageProcessor();

		TestApplicationTemplate app = new TestApplicationTemplate();
		processor.processMessage( new MsgCmdSetScopedInstance( app.getTomcatVm()));
		Assert.assertEquals( app.getTomcatVm(), processor.scopedInstance );
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, processor.scopedInstance.getStatus());

		/*
		 * Scenario:
		 * 1. Tomcat is not deployed.
		 * 2. Request it to reach the "deployed" state. Deploy will fail.
		 * 3. Request it to reach the "started" state. Deploy will fail.
		 * 4. Try to undeploy it.
		 */
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getTomcat().getStatus());
		processor.processMessage( new MsgCmdChangeInstanceState( app.getTomcat(), InstanceStatus.DEPLOYED_STOPPED ));
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getTomcat().getStatus());

		processor.processMessage( new MsgCmdChangeInstanceState( app.getTomcat(), InstanceStatus.DEPLOYED_STARTED ));
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getTomcat().getStatus());

		processor.processMessage( new MsgCmdChangeInstanceState( app.getTomcat(), InstanceStatus.NOT_DEPLOYED ));
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getTomcat().getStatus());
	}


	@Test
	public void testStateAdvancedChange_stopFails() {

		// Initialize all the stuff.
		// The plug-in will fail on "start".
		this.agent.stop();
		this.agent = new Agent() {
			@Override
			public PluginInterface findPlugin( Instance instance ) {
				return new PluginMock() {
					@Override
					public void stop( Instance instance ) throws PluginException {
						throw new PluginException( "For tests..." );
					}
				};
			}
		};

		this.agent.setMessagingType(MessagingConstants.FACTORY_TEST);
		this.agent.start();
		AgentMessageProcessor processor = (AgentMessageProcessor) this.agent.getMessagingClient().getMessageProcessor();

		TestApplicationTemplate app = new TestApplicationTemplate();
		processor.processMessage( new MsgCmdSetScopedInstance( app.getTomcatVm()));
		Assert.assertEquals( app.getTomcatVm(), processor.scopedInstance );
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, processor.scopedInstance.getStatus());

		/*
		 * Scenario:
		 * 1. Tomcat is not deployed.
		 * 2. Request it to reach the "started" state. OK.
		 * 3. Try to stop it. It will work, even if the plug-in invocation fails.
		 * 4. Try to undeploy it. Same as previously.
		 */
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getTomcat().getStatus());
		processor.processMessage( new MsgCmdChangeInstanceState( app.getTomcat(), InstanceStatus.DEPLOYED_STARTED ));
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, app.getTomcat().getStatus());

		processor.processMessage( new MsgCmdChangeInstanceState( app.getTomcat(), InstanceStatus.DEPLOYED_STOPPED ));
		Assert.assertEquals( InstanceStatus.DEPLOYED_STOPPED, app.getTomcat().getStatus());

		processor.processMessage( new MsgCmdChangeInstanceState( app.getTomcat(), InstanceStatus.DEPLOYED_STARTED ));
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, app.getTomcat().getStatus());

		processor.processMessage( new MsgCmdChangeInstanceState( app.getTomcat(), InstanceStatus.NOT_DEPLOYED ));
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getTomcat().getStatus());
	}


	@Test
	public void testStateAdvancedChange_undeployFails() {

		// Initialize all the stuff.
		// The plug-in will fail on "start".
		this.agent.stop();
		this.agent = new Agent() {
			@Override
			public PluginInterface findPlugin( Instance instance ) {
				return new PluginMock() {
					@Override
					public void undeploy( Instance instance ) throws PluginException {
						throw new PluginException( "For tests..." );
					}
				};
			}
		};

		this.agent.setMessagingType(MessagingConstants.FACTORY_TEST);
		this.agent.start();
		AgentMessageProcessor processor = (AgentMessageProcessor) this.agent.getMessagingClient().getMessageProcessor();

		TestApplicationTemplate app = new TestApplicationTemplate();
		processor.processMessage( new MsgCmdSetScopedInstance( app.getTomcatVm()));
		Assert.assertEquals( app.getTomcatVm(), processor.scopedInstance );
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, processor.scopedInstance.getStatus());

		/*
		 * Scenario:
		 * 1. Tomcat is not deployed.
		 * 2. Request it to reach the "started" state. OK.
		 * 3. Try to stop it. OK.
		 * 4. Try to undeploy it. Failure.
		 */
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getTomcat().getStatus());
		processor.processMessage( new MsgCmdChangeInstanceState( app.getTomcat(), InstanceStatus.DEPLOYED_STARTED ));
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, app.getTomcat().getStatus());

		processor.processMessage( new MsgCmdChangeInstanceState( app.getTomcat(), InstanceStatus.DEPLOYED_STOPPED ));
		Assert.assertEquals( InstanceStatus.DEPLOYED_STOPPED, app.getTomcat().getStatus());

		processor.processMessage( new MsgCmdChangeInstanceState( app.getTomcat(), InstanceStatus.NOT_DEPLOYED ));
		Assert.assertEquals( InstanceStatus.DEPLOYED_STOPPED, app.getTomcat().getStatus());
	}
}
