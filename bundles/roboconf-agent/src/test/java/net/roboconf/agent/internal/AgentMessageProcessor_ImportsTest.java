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

package net.roboconf.agent.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;
import net.roboconf.agent.internal.impl.InMemoryAgentImpl;
import net.roboconf.agent.tests.TestAgentMessagingClient;
import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.model.runtime.Component;
import net.roboconf.core.model.runtime.Import;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.core.model.runtime.Instance.InstanceStatus;
import net.roboconf.messaging.messages.from_agent_to_agent.MsgCmdAddImport;
import net.roboconf.messaging.messages.from_agent_to_agent.MsgCmdRemoveImport;
import net.roboconf.messaging.messages.from_agent_to_agent.MsgCmdRequestImport;
import net.roboconf.plugin.api.PluginInterface;

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class AgentMessageProcessor_ImportsTest {

	@Test
	public void testImportsRequest() throws Exception {

		AbstractAgent agent = new InMemoryAgentImpl();
		AgentMessageProcessor processor = new AgentMessageProcessor( agent );
		TestAgentMessagingClient client = new TestAgentMessagingClient();
		processor.newMessagingClient = client;

		TestApplication app = new TestApplication();
		processor.rootInstance = app.getTomcatVm();
		app.getTomcatVm().setStatus( InstanceStatus.DEPLOYED_STARTED );

		// There are as many client invocations as started instances
		processor.processMessage( new MsgCmdRequestImport( "war" ));
		Assert.assertEquals( 1, client.messagesForAgentsCount.get());

		app.getTomcat().setStatus( InstanceStatus.DEPLOYED_STARTED );
		app.getWar().setStatus( InstanceStatus.DEPLOYED_STARTED );
		client.messagesForAgentsCount.set( 0 );

		processor.processMessage( new MsgCmdRequestImport( "war" ));
		Assert.assertEquals( 3, client.messagesForAgentsCount.get());
	}


	@Test
	public void testImports() throws Exception {

		AbstractAgent agent = new InMemoryAgentImpl();
		AgentMessageProcessor processor = new AgentMessageProcessor( agent );
		TestAgentMessagingClient client = new TestAgentMessagingClient();
		processor.newMessagingClient = client;

		TestApplication app = new TestApplication();
		processor.rootInstance = app.getTomcatVm();

		app.getTomcatVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		app.getTomcat().setStatus( InstanceStatus.DEPLOYED_STARTED );
		app.getWar().setStatus( InstanceStatus.DEPLOYED_STARTED );

		// Notify a MySQL instance is available
		Assert.assertEquals( 0, app.getWar().getImports().size());

		Map<String,String> variables1 = new HashMap<String,String> ();
		variables1.put( "mysql.ip", "192.168.0.15" );
		variables1.put( "mysql.port", "3306" );
		processor.processMessage( new MsgCmdAddImport( "mysql", "mysql-vm-1/mysql", variables1 ));

		Assert.assertEquals( 1, app.getWar().getImports().size());
		Collection<Import> imports = app.getWar().getImports().get( "mysql" );
		Assert.assertEquals( 1, imports.size());

		Import imp = imports.iterator().next();
		Assert.assertEquals( 2, imp.getExportedVars().size());
		Assert.assertEquals( "192.168.0.15", imp.getExportedVars().get( "mysql.ip" ));
		Assert.assertEquals( "3306", imp.getExportedVars().get( "mysql.port" ));
		Assert.assertEquals( "mysql-vm-1/mysql", imp.getInstancePath());

		// Another instance is made available
		Map<String,String> variables2 = new HashMap<String,String> ();
		variables2.put( "mysql.ip", "192.168.0.21" );
		variables2.put( "mysql.port", "31306" );
		processor.processMessage( new MsgCmdAddImport( "mysql", "mysql-vm-2/mysql", variables2 ));

		Assert.assertEquals( 1, app.getWar().getImports().size());
		imports = app.getWar().getImports().get( "mysql" );
		Assert.assertEquals( 2, imports.size());

		imp = imports.toArray( new Import[ 0 ])[ 1 ];
		Assert.assertEquals( 2, imp.getExportedVars().size());
		Assert.assertEquals( "192.168.0.21", imp.getExportedVars().get( "mysql.ip" ));
		Assert.assertEquals( "31306", imp.getExportedVars().get( "mysql.port" ));
		Assert.assertEquals( "mysql-vm-2/mysql", imp.getInstancePath());

		// Remove the last one
		processor.processMessage( new MsgCmdRemoveImport( "mysql", "mysql-vm-2/mysql" ));
		Assert.assertEquals( 1, app.getWar().getImports().size());
		imports = app.getWar().getImports().get( "mysql" );
		Assert.assertEquals( 1, imports.size());

		imp = imports.iterator().next();
		Assert.assertEquals( 2, imp.getExportedVars().size());
		Assert.assertEquals( "192.168.0.15", imp.getExportedVars().get( "mysql.ip" ));
		Assert.assertEquals( "3306", imp.getExportedVars().get( "mysql.port" ));
		Assert.assertEquals( "mysql-vm-1/mysql", imp.getInstancePath());

		// Remove an invalid one
		processor.processMessage( new MsgCmdRemoveImport( "mysql", "mysql-vm-54/mysql" ));
		Assert.assertEquals( 1, app.getWar().getImports().size());

		processor.processMessage( new MsgCmdRemoveImport( "something-else", "a-vm/something-else" ));
		Assert.assertEquals( 1, app.getWar().getImports().size());
	}


	@Test
	public void testAddImport_noPlugin() throws Exception {

		AbstractAgent agent = new InMemoryAgentImpl() {
			@Override
			public PluginInterface findPlugin( Instance instance ) {
				return null;
			}
		};

		AgentMessageProcessor processor = new AgentMessageProcessor( agent );
		TestAgentMessagingClient client = new TestAgentMessagingClient();
		processor.newMessagingClient = client;

		TestApplication app = new TestApplication();
		processor.rootInstance = app.getTomcatVm();

		app.getTomcatVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		app.getTomcat().setStatus( InstanceStatus.DEPLOYED_STARTED );
		app.getWar().setStatus( InstanceStatus.DEPLOYED_STARTED );

		// Notify a MySQL instance is available
		Assert.assertEquals( 0, app.getWar().getImports().size());

		Map<String,String> variables1 = new HashMap<String,String> ();
		variables1.put( "mysql.ip", "192.168.0.15" );
		variables1.put( "mysql.port", "3306" );
		processor.processMessage( new MsgCmdAddImport( "mysql", "mysql-vm-1/mysql", variables1 ));

		Assert.assertEquals( 1, app.getWar().getImports().size());
		Collection<Import> imports = app.getWar().getImports().get( "mysql" );
		Assert.assertEquals( 1, imports.size());

		Import imp = imports.iterator().next();
		Assert.assertEquals( 2, imp.getExportedVars().size());
		Assert.assertEquals( "192.168.0.15", imp.getExportedVars().get( "mysql.ip" ));
		Assert.assertEquals( "3306", imp.getExportedVars().get( "mysql.port" ));
		Assert.assertEquals( "mysql-vm-1/mysql", imp.getInstancePath());
		// No plug-in => no error (the exception is caught somewhere)
	}


	@Test
	public void testRemoveImport_noPlugin() throws Exception {

		AbstractAgent agent = new InMemoryAgentImpl() {
			@Override
			public PluginInterface findPlugin( Instance instance ) {
				return null;
			}
		};

		AgentMessageProcessor processor = new AgentMessageProcessor( agent );
		TestAgentMessagingClient client = new TestAgentMessagingClient();
		processor.newMessagingClient = client;

		TestApplication app = new TestApplication();
		processor.rootInstance = app.getTomcatVm();

		app.getTomcatVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		app.getTomcat().setStatus( InstanceStatus.DEPLOYED_STARTED );
		app.getWar().setStatus( InstanceStatus.DEPLOYED_STARTED );

		List<Import> imports = new ArrayList<Import>( Arrays.asList( new Import( "mysql-vm-1/mysql" )));
		app.getWar().getImports().put( "mysql", imports );

		// Remove the MySQL export
		Assert.assertEquals( 1, app.getWar().getImports().size());
		Assert.assertEquals( 1, app.getWar().getImports().get( "mysql" ).size());
		processor.processMessage( new MsgCmdRemoveImport( "mysql", "mysql-vm-1/mysql" ));
		Assert.assertEquals( 1, app.getWar().getImports().size());
		Assert.assertEquals( 0, app.getWar().getImports().get( "mysql" ).size());
		// No plug-in => no error (the exception is caught somewhere)
	}


	@Test
	public void testSelfImport() throws Exception {

		// A node depends on other nodes
		AbstractAgent agent = new InMemoryAgentImpl();
		AgentMessageProcessor processor = new AgentMessageProcessor( agent );
		TestAgentMessagingClient client = new TestAgentMessagingClient();
		processor.newMessagingClient = client;

		Component clusterNodeComponent = new Component( "cluster" ).alias( "a cluster node" ).installerName( "whatever" );
		clusterNodeComponent.getImportedVariables().put( "cluster.ip", Boolean.TRUE );
		clusterNodeComponent.getImportedVariables().put( "cluster.port", Boolean.TRUE );
		clusterNodeComponent.getExportedVariables().put( "cluster.ip", null );
		clusterNodeComponent.getExportedVariables().put( "cluster.port", "9007" );

		Instance i1 = new Instance( "inst 1" ).component( clusterNodeComponent );
		i1.getExports().put( "cluster.ip", "192.168.1.15" );
		processor.rootInstance = i1;

		// Adding itself does not work
		Assert.assertEquals( 0, i1.getImports().size());
		processor.processMessage( new MsgCmdAddImport( "cluster", "/inst 1", i1.getExports()));
		Assert.assertEquals( 0, i1.getImports().size());

		// Adding another node works
		Map<String,String> variables = new HashMap<String,String> ();
		variables.put( "cluster.ip", "192.168.0.45" );
		processor.processMessage( new MsgCmdAddImport( "cluster", "/vm/cluster node", variables ));

		Assert.assertEquals( 1, i1.getImports().size());
		Collection<Import> imports = i1.getImports().get( "cluster" );
		Assert.assertEquals( 1, imports.size());

		Import imp = imports.iterator().next();
		Assert.assertEquals( 1, imp.getExportedVars().size());
		Assert.assertEquals( "192.168.0.45", imp.getExportedVars().get( "cluster.ip" ));
	}
}
