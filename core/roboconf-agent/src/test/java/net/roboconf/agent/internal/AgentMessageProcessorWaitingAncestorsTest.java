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

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import net.roboconf.core.Constants;
import net.roboconf.core.model.beans.Component;
import net.roboconf.core.model.beans.ExportedVariable;
import net.roboconf.core.model.beans.ImportedVariable;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.messaging.api.MessagingConstants;
import net.roboconf.messaging.api.factory.MessagingClientFactoryRegistry;
import net.roboconf.messaging.api.internal.client.test.TestClientFactory;
import net.roboconf.messaging.api.messages.from_agent_to_agent.MsgCmdAddImport;
import net.roboconf.messaging.api.messages.from_agent_to_agent.MsgCmdRemoveImport;
import net.roboconf.messaging.api.messages.from_dm_to_agent.MsgCmdChangeInstanceState;
import net.roboconf.messaging.api.messages.from_dm_to_agent.MsgCmdSetScopedInstance;

/**
 * @author Vincent Zurczak - Linagora
 */
public class AgentMessageProcessorWaitingAncestorsTest {

	private Agent agent;
	private AgentMessageProcessor processor;


	@Before
	public void initializeAgent() throws Exception {

		final MessagingClientFactoryRegistry registry = new MessagingClientFactoryRegistry();
		registry.addMessagingClientFactory(new TestClientFactory());
		this.agent = new Agent();
		this.agent.setApplicationName( "my-app" );
		this.agent.setScopedInstancePath( "app server" );

		// We first need to start the agent, so it creates the reconfigurable messaging client.
		this.agent.setMessagingType( MessagingConstants.FACTORY_TEST );
		this.agent.start();

		// We then set the factory registry of the created client, and reconfigure the agent, so the messaging client backend is created.
		this.agent.getMessagingClient().setRegistry(registry);
		this.agent.reconfigure();
		this.agent.setSimulatePlugins( true );

		Thread.sleep( 200 );
		this.processor = (AgentMessageProcessor) this.agent.getMessagingClient().getMessageProcessor();
	}


	@After
	public void stopAgent() {
		this.agent.stop();
	}


	@Test
	public void testScenario_1() throws Exception {

		// Hierarchy: 3 levels.
		// The intermediate level has dependencies.

		// Model
		Component dbComponent = new Component( "database" ).installerName( "whatever" );
		dbComponent.addExportedVariable( new ExportedVariable( "database.ip", null ));
		dbComponent.addExportedVariable( new ExportedVariable( "database.port", "3009" ));

		Component appServerComponent = new Component( "app-server" ).installerName( "whatever" );
		appServerComponent.addExportedVariable( new ExportedVariable( "app-server.ip", null ));
		appServerComponent.addExportedVariable( new ExportedVariable( "app-server.port", "8009" ));
		appServerComponent.addImportedVariable( new ImportedVariable( "database.ip", false, false ));
		appServerComponent.addImportedVariable( new ImportedVariable( "database.port", false, false ));

		Component appComponent = new Component( "app1" ).installerName( "we do not care" );
		appServerComponent.addChild( appComponent );

		Component vmComponent = new Component( "vm" ).installerName( Constants.TARGET_INSTALLER );
		vmComponent.addChild( dbComponent );
		vmComponent.addChild( appServerComponent );

		Instance appServer = new Instance( "app server" ).component( appServerComponent );
		appServer.overriddenExports.put( "app-server.ip", "192.168.1.15" );

		Instance app1 = new Instance( "app1" ).component( appComponent );
		Instance app2 = new Instance( "app2" ).component( appComponent );
		InstanceHelpers.insertChild( appServer, app1 );
		InstanceHelpers.insertChild( appServer, app2 );

		Instance vm1 = new Instance( "vm1" ).component( vmComponent );
		InstanceHelpers.insertChild( vm1, appServer );

		final String database = "database";
		final Map<String,String> vars = new HashMap<String,String>( 1 );
		vars.put( "database.ip", "192.168.1.18" );

		// Register the model
		this.processor.processMessage( new MsgCmdSetScopedInstance( vm1 ));
		Assert.assertEquals( vm1, this.agent.getScopedInstance());

		// Deploy and start all
		for( Instance i : InstanceHelpers.buildHierarchicalList( appServer )) {
			this.processor.processMessage( new MsgCmdChangeInstanceState( i, InstanceStatus.DEPLOYED_STARTED ));
		}

		Assert.assertEquals( InstanceStatus.UNRESOLVED, appServer.getStatus());
		Assert.assertEquals( InstanceStatus.WAITING_FOR_ANCESTOR, app1.getStatus());
		Assert.assertEquals( InstanceStatus.WAITING_FOR_ANCESTOR, app2.getStatus());

		// We should not be able to start a "waiting..." explicitely
		this.processor.processMessage( new MsgCmdChangeInstanceState( app2, InstanceStatus.DEPLOYED_STARTED ));

		Assert.assertEquals( InstanceStatus.UNRESOLVED, appServer.getStatus());
		Assert.assertEquals( InstanceStatus.WAITING_FOR_ANCESTOR, app1.getStatus());
		Assert.assertEquals( InstanceStatus.WAITING_FOR_ANCESTOR, app2.getStatus());

		// Resolve the missing dependency
		this.processor.processMessage( new MsgCmdAddImport( "my-app", database, "/vm/db", vars ));

		// The states should all be resolved
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, appServer.getStatus());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, app1.getStatus());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, app2.getStatus());

		// Remove the dependency
		this.processor.processMessage( new MsgCmdRemoveImport( "my-app", database, "/vm/db" ));

		// Verify the states
		Assert.assertEquals( InstanceStatus.UNRESOLVED, appServer.getStatus());
		Assert.assertEquals( InstanceStatus.WAITING_FOR_ANCESTOR, app1.getStatus());
		Assert.assertEquals( InstanceStatus.WAITING_FOR_ANCESTOR, app2.getStatus());

		// Stop an instance
		this.processor.processMessage( new MsgCmdChangeInstanceState( app2, InstanceStatus.DEPLOYED_STOPPED ));

		Assert.assertEquals( InstanceStatus.UNRESOLVED, appServer.getStatus());
		Assert.assertEquals( InstanceStatus.WAITING_FOR_ANCESTOR, app1.getStatus());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STOPPED, app2.getStatus());

		// Add it again (as another instance)
		this.processor.processMessage( new MsgCmdAddImport( "my-app", database, "/other-vm/db", vars ));

		// The states should all be resolved
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, appServer.getStatus());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, app1.getStatus());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STOPPED, app2.getStatus());
	}


	@Test
	public void testScenario_2() throws Exception {

		// Hierarchy: 4 levels.
		// The 2 intermediate levels have dependencies.

		// Model
		Component dbComponent = new Component( "database" ).installerName( "whatever" );
		dbComponent.addExportedVariable( new ExportedVariable( "database.ip", null ));
		dbComponent.addExportedVariable( new ExportedVariable( "database.port", "3009" ));

		Component appServerComponent = new Component( "app-server" ).installerName( "whatever" );
		appServerComponent.addExportedVariable( new ExportedVariable( "app-server.ip", null ));
		appServerComponent.addExportedVariable( new ExportedVariable( "app-server.port", "8009" ));
		appServerComponent.addImportedVariable( new ImportedVariable( "database.ip", false, false ));
		appServerComponent.addImportedVariable( new ImportedVariable( "database.port", false, false ));

		Component appComponent1 = new Component( "app1" ).installerName( "we do not care" );
		appServerComponent.addChild( appComponent1 );

		Component appComponent2 = new Component( "app1" ).installerName( "we do not care" );
		appComponent2.addImportedVariable( new ImportedVariable( "something.else", false, false ));
		appServerComponent.addChild( appComponent2 );

		Component appConfigComponent = new Component( "config" ).installerName( "we do not care" );
		appComponent2.addChild( appConfigComponent );

		Component vmComponent = new Component( "vm" ).installerName( Constants.TARGET_INSTALLER );
		vmComponent.addChild( dbComponent );
		vmComponent.addChild( appServerComponent );

		Instance appServer = new Instance( "app server" ).component( appServerComponent );
		appServer.overriddenExports.put( "app-server.ip", "192.168.1.15" );

		Instance app1 = new Instance( "app1" ).component( appComponent1 );
		Instance app2 = new Instance( "app2" ).component( appComponent2 );
		InstanceHelpers.insertChild( appServer, app1 );
		InstanceHelpers.insertChild( appServer, app2 );

		Instance app2Config = new Instance( "config" ).component( appConfigComponent );
		InstanceHelpers.insertChild( app2, app2Config );

		Instance vm1 = new Instance( "vm1" ).component( vmComponent );
		InstanceHelpers.insertChild( vm1, appServer );

		final String database = "database";
		final Map<String,String> vars = new HashMap<String,String>( 1 );
		vars.put( "database.ip", "192.168.1.18" );

		final String something = "something";
		final Map<String,String> somethingVars = new HashMap<String,String>( 1 );
		somethingVars.put( "something.else", "hi!" );

		// Register the model
		this.processor.processMessage( new MsgCmdSetScopedInstance( vm1 ));
		Assert.assertEquals( vm1, this.agent.getScopedInstance());

		// Deploy and start all
		for( Instance i : InstanceHelpers.buildHierarchicalList( appServer )) {
			this.processor.processMessage( new MsgCmdChangeInstanceState( i, InstanceStatus.DEPLOYED_STARTED ));
		}

		Assert.assertEquals( InstanceStatus.UNRESOLVED, appServer.getStatus());
		Assert.assertEquals( InstanceStatus.WAITING_FOR_ANCESTOR, app1.getStatus());
		Assert.assertEquals( InstanceStatus.WAITING_FOR_ANCESTOR, app2.getStatus());
		Assert.assertEquals( InstanceStatus.WAITING_FOR_ANCESTOR, app2Config.getStatus());

		// Resolve the missing dependency
		this.processor.processMessage( new MsgCmdAddImport( "my-app", database, "/vm/db", vars ));

		// The states should all be resolved
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, appServer.getStatus());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, app1.getStatus());
		Assert.assertEquals( InstanceStatus.UNRESOLVED, app2.getStatus());
		Assert.assertEquals( InstanceStatus.WAITING_FOR_ANCESTOR, app2Config.getStatus());

		// Stop the 2nd app
		this.processor.processMessage( new MsgCmdChangeInstanceState( app2, InstanceStatus.DEPLOYED_STOPPED ));

		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, appServer.getStatus());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, app1.getStatus());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STOPPED, app2.getStatus());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STOPPED, app2Config.getStatus());

		// Start it - does not impact the child instance
		this.processor.processMessage( new MsgCmdChangeInstanceState( app2, InstanceStatus.DEPLOYED_STARTED ));

		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, appServer.getStatus());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, app1.getStatus());
		Assert.assertEquals( InstanceStatus.UNRESOLVED, app2.getStatus());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STOPPED, app2Config.getStatus());

		// Undeploy all
		for( Instance i : InstanceHelpers.buildHierarchicalList( appServer )) {
			this.processor.processMessage( new MsgCmdChangeInstanceState( i, InstanceStatus.NOT_DEPLOYED ));
		}

		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, appServer.getStatus());
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app1.getStatus());
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app2.getStatus());
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app2Config.getStatus());

		// Deploy and start all, again - imports were kept
		for( Instance i : InstanceHelpers.buildHierarchicalList( appServer )) {
			this.processor.processMessage( new MsgCmdChangeInstanceState( i, InstanceStatus.DEPLOYED_STARTED ));
		}

		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, appServer.getStatus());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, app1.getStatus());
		Assert.assertEquals( InstanceStatus.UNRESOLVED, app2.getStatus());
		Assert.assertEquals( InstanceStatus.WAITING_FOR_ANCESTOR, app2Config.getStatus());

		// Resolve the missing dependencIES
		this.processor.processMessage( new MsgCmdAddImport( "my-app", something, "/vm/something", somethingVars ));

		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, appServer.getStatus());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, app1.getStatus());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, app2.getStatus());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, app2Config.getStatus());

		// Remove a top dependency
		this.processor.processMessage( new MsgCmdRemoveImport( "my-app", database, "/vm/db" ));
		Assert.assertEquals( InstanceStatus.UNRESOLVED, appServer.getStatus());
		Assert.assertEquals( InstanceStatus.WAITING_FOR_ANCESTOR, app1.getStatus());
		Assert.assertEquals( InstanceStatus.WAITING_FOR_ANCESTOR, app2.getStatus());
		Assert.assertEquals( InstanceStatus.WAITING_FOR_ANCESTOR, app2Config.getStatus());

		// Add it again
		this.processor.processMessage( new MsgCmdAddImport( "my-app", database, "/vm/db", vars ));

		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, appServer.getStatus());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, app1.getStatus());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, app2.getStatus());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, app2Config.getStatus());

		// Remove the second dependency
		this.processor.processMessage( new MsgCmdRemoveImport( "my-app", something, "/vm/something" ));

		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, appServer.getStatus());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, app1.getStatus());
		Assert.assertEquals( InstanceStatus.UNRESOLVED, app2.getStatus());
		Assert.assertEquals( InstanceStatus.WAITING_FOR_ANCESTOR, app2Config.getStatus());

		// Add it again
		this.processor.processMessage( new MsgCmdAddImport( "my-app", something, "/vm/something", somethingVars ));

		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, appServer.getStatus());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, app1.getStatus());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, app2.getStatus());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, app2Config.getStatus());

		// Test undeploy when a dependency is missing
		this.processor.processMessage( new MsgCmdRemoveImport( "my-app", something, "/vm/something" ));

		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, appServer.getStatus());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, app1.getStatus());
		Assert.assertEquals( InstanceStatus.UNRESOLVED, app2.getStatus());
		Assert.assertEquals( InstanceStatus.WAITING_FOR_ANCESTOR, app2Config.getStatus());

		this.processor.processMessage( new MsgCmdChangeInstanceState( app2Config, InstanceStatus.NOT_DEPLOYED ));

		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, appServer.getStatus());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, app1.getStatus());
		Assert.assertEquals( InstanceStatus.UNRESOLVED, app2.getStatus());
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app2Config.getStatus());

		// Add the missing dependency, the last instance remain not deployed
		this.processor.processMessage( new MsgCmdAddImport( "my-app", something, "/vm/something", somethingVars ));

		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, appServer.getStatus());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, app1.getStatus());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, app2.getStatus());
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app2Config.getStatus());
	}
}
