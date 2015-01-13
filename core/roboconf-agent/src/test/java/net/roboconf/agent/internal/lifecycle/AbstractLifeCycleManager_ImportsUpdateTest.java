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

package net.roboconf.agent.internal.lifecycle;

import java.io.IOException;
import java.util.Map;

import junit.framework.Assert;
import net.roboconf.agent.internal.misc.PluginMock;
import net.roboconf.core.model.beans.Component;
import net.roboconf.core.model.beans.Import;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.core.model.helpers.ImportHelpers;
import net.roboconf.messaging.internal.client.test.TestClientAgent;
import net.roboconf.plugin.api.PluginException;
import net.roboconf.plugin.api.PluginInterface;

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class AbstractLifeCycleManager_ImportsUpdateTest {

	@Test
	public void testOptionalImports() throws Exception {

		// The model
		Component clusterNodeComponent = new Component( "cluster" ).installerName( "whatever" );
		clusterNodeComponent.importedVariables.put( "cluster.ip", Boolean.TRUE );
		clusterNodeComponent.importedVariables.put( "cluster.port", Boolean.TRUE );
		clusterNodeComponent.exportedVariables.put( "cluster.ip", null );
		clusterNodeComponent.exportedVariables.put( "cluster.port", "9007" );

		Instance i1 = new Instance( "inst 1" ).component( clusterNodeComponent );
		i1.overriddenExports.put( "cluster.ip", "192.168.1.15" );
		i1.setStatus( InstanceStatus.STARTING );

		Instance i2 = new Instance( "inst 2" ).component( clusterNodeComponent );
		i2.overriddenExports.put( "cluster.ip", "192.168.1.28" );

		// The basis
		PluginInterface plugin = new PluginMock();
		AbstractLifeCycleManager lfm = new AbstractLifeCycleManager( "my-app", new TestClientAgent()) {
			@Override
			public void changeInstanceState(
					Instance instance, PluginInterface plugin,
					InstanceStatus newStatus, Map<String,byte[]> fileNameToFileContent )
			throws IOException, PluginException {
				// nothing
			}
		};

		// The cluster node does not know about another node
		Assert.assertEquals( InstanceStatus.STARTING, i1.getStatus());
		lfm.updateStateFromImports( i1, plugin, null, InstanceStatus.DEPLOYED_STARTED );
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, i1.getStatus());

		// The node is now aware of another node
		ImportHelpers.addImport( i1, "cluster", new Import( i2 ));
		i1.setStatus( InstanceStatus.STARTING );
		lfm.updateStateFromImports( i1, plugin, null, InstanceStatus.DEPLOYED_STARTED );
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, i1.getStatus());
		lfm.updateStateFromImports( i1, plugin, null, InstanceStatus.DEPLOYED_STARTED );
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, i1.getStatus());

		i1.getImports().clear();
		lfm.updateStateFromImports( i1, plugin, null, InstanceStatus.DEPLOYED_STARTED );
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, i1.getStatus());

		// Imports update does nothing on a stopped instance
		i1.setStatus( InstanceStatus.DEPLOYED_STOPPED );
		lfm.updateStateFromImports( i1, plugin, null, InstanceStatus.DEPLOYED_STARTED );
		Assert.assertEquals( InstanceStatus.DEPLOYED_STOPPED, i1.getStatus());
	}


	@Test
	public void testNonOptionalImports() throws Exception {

		// The model
		Component dbComponent = new Component( "database" ).installerName( "whatever" );
		dbComponent.exportedVariables.put( "database.ip", null );
		dbComponent.exportedVariables.put( "database.port", "3009" );

		Component appServerComponent = new Component( "app-server" ).installerName( "whatever" );
		appServerComponent.exportedVariables.put( "app-server.ip", null );
		appServerComponent.exportedVariables.put( "app-server.port", "8009" );
		appServerComponent.importedVariables.put( "database.ip", Boolean.FALSE );
		appServerComponent.importedVariables.put( "database.port", Boolean.FALSE );

		Instance appServer = new Instance( "app server" ).component( appServerComponent );
		appServer.overriddenExports.put( "app-server.ip", "192.168.1.15" );
		appServer.setStatus( InstanceStatus.STARTING );

		Instance database = new Instance( "database" ).component( dbComponent );
		database.overriddenExports.put( "database.ip", "192.168.1.28" );

		// The basis
		PluginInterface plugin = new PluginMock();
		AbstractLifeCycleManager lfm = new AbstractLifeCycleManager( "my-app", new TestClientAgent()) {
			@Override
			public void changeInstanceState(
					Instance instance, PluginInterface plugin,
					InstanceStatus newStatus, Map<String,byte[]> fileNameToFileContent )
			throws IOException, PluginException {
				// nothing
			}
		};

		// The application server does not know about the database
		Assert.assertEquals( InstanceStatus.STARTING, appServer.getStatus());
		lfm.updateStateFromImports( appServer, plugin, null, InstanceStatus.DEPLOYED_STARTED );
		Assert.assertEquals( InstanceStatus.STARTING, appServer.getStatus());

		// The application server is now aware of the database
		ImportHelpers.addImport( appServer, "database", new Import( database ));
		appServer.setStatus( InstanceStatus.STARTING );
		lfm.updateStateFromImports( appServer, plugin, null, InstanceStatus.DEPLOYED_STARTED );
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, appServer.getStatus());
		lfm.updateStateFromImports( appServer, plugin, null, InstanceStatus.DEPLOYED_STARTED );
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, appServer.getStatus());

		appServer.getImports().clear();
		lfm.updateStateFromImports( appServer, plugin, null, InstanceStatus.DEPLOYED_STARTED );
		Assert.assertEquals( InstanceStatus.STARTING, appServer.getStatus());
	}
}
