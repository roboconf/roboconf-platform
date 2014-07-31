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

package net.roboconf.agent.api.internal;

import junit.framework.Assert;
import net.roboconf.agent.api.AbstractAgent;
import net.roboconf.agent.api.tests.TestAgentMessagingClient;
import net.roboconf.core.model.helpers.ImportHelpers;
import net.roboconf.core.model.runtime.Component;
import net.roboconf.core.model.runtime.Import;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.core.model.runtime.Instance.InstanceStatus;

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ImportsUpdateTest {

	@Test
	public void testOptionalImports() throws Exception {

		Component clusterNodeComponent = new Component( "cluster" ).alias( "a cluster node" ).installerName( "whatever" );
		clusterNodeComponent.getImportedVariables().put( "cluster.ip", Boolean.TRUE );
		clusterNodeComponent.getImportedVariables().put( "cluster.port", Boolean.TRUE );
		clusterNodeComponent.getExportedVariables().put( "cluster.ip", null );
		clusterNodeComponent.getExportedVariables().put( "cluster.port", "9007" );

		Instance i1 = new Instance( "inst 1" ).component( clusterNodeComponent );
		i1.getExports().put( "cluster.ip", "192.168.1.15" );
		i1.setStatus( InstanceStatus.STARTING );

		Instance i2 = new Instance( "inst 2" ).component( clusterNodeComponent );
		i2.getExports().put( "cluster.ip", "192.168.1.28" );

		AgentMessageProcessor processor = new AgentMessageProcessor( new AbstractAgent() {});
		processor.setMessagingClient( new TestAgentMessagingClient());

		// The cluster node does not know about another node
		Assert.assertEquals( InstanceStatus.STARTING, i1.getStatus());
		processor.updateStateFromImports( i1, new PluginMock(), null, InstanceStatus.DEPLOYED_STARTED );
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, i1.getStatus());

		// The node is now aware of another node
		ImportHelpers.addImport( i1, "cluster", new Import( i2 ));
		i1.setStatus( InstanceStatus.STARTING );
		processor.updateStateFromImports( i1, new PluginMock(), null, InstanceStatus.DEPLOYED_STARTED );
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, i1.getStatus());
		processor.updateStateFromImports( i1, new PluginMock(), null, InstanceStatus.DEPLOYED_STARTED );
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, i1.getStatus());

		i1.getImports().clear();
		processor.updateStateFromImports( i1, new PluginMock(), null, InstanceStatus.DEPLOYED_STARTED );
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, i1.getStatus());
	}


	@Test
	public void testNonOptionalImports() throws Exception {

		Component dbComponent = new Component( "database" ).alias( "a database" ).installerName( "whatever" );
		dbComponent.getExportedVariables().put( "database.ip", null );
		dbComponent.getExportedVariables().put( "database.port", "3009" );

		Component appServerComponent = new Component( "app-server" ).alias( "an application server" ).installerName( "whatever" );
		appServerComponent.getExportedVariables().put( "app-server.ip", null );
		appServerComponent.getExportedVariables().put( "app-server.port", "8009" );
		appServerComponent.getImportedVariables().put( "database.ip", Boolean.FALSE );
		appServerComponent.getImportedVariables().put( "database.port", Boolean.FALSE );

		Instance appServer = new Instance( "app server" ).component( appServerComponent );
		appServer.getExports().put( "app-server.ip", "192.168.1.15" );
		appServer.setStatus( InstanceStatus.STARTING );

		Instance database = new Instance( "database" ).component( dbComponent );
		database.getExports().put( "database.ip", "192.168.1.28" );

		AgentMessageProcessor processor = new AgentMessageProcessor( new AbstractAgent() {});
		processor.setMessagingClient( new TestAgentMessagingClient());

		// The application server does not know about the database
		Assert.assertEquals( InstanceStatus.STARTING, appServer.getStatus());
		processor.updateStateFromImports( appServer, new PluginMock(), null, InstanceStatus.DEPLOYED_STARTED );
		Assert.assertEquals( InstanceStatus.STARTING, appServer.getStatus());

		// The application server is now aware of the database
		ImportHelpers.addImport( appServer, "database", new Import( database ));
		appServer.setStatus( InstanceStatus.STARTING );
		processor.updateStateFromImports( appServer, new PluginMock(), null, InstanceStatus.DEPLOYED_STARTED );
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, appServer.getStatus());
		processor.updateStateFromImports( appServer, new PluginMock(), null, InstanceStatus.DEPLOYED_STARTED );
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, appServer.getStatus());

		appServer.getImports().clear();
		processor.updateStateFromImports( appServer, new PluginMock(), null, InstanceStatus.DEPLOYED_STARTED );
		Assert.assertEquals( InstanceStatus.STARTING, appServer.getStatus());
	}
}
