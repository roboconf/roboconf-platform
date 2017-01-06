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

package net.roboconf.agent.internal.lifecycle;

import org.junit.Assert;
import net.roboconf.agent.internal.misc.PluginMock;
import net.roboconf.core.model.beans.Import;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.core.model.helpers.ImportHelpers;
import net.roboconf.messaging.api.business.IAgentClient;

import org.junit.Test;
import org.mockito.Mockito;

/**
 * @author Vincent Zurczak - Linagora
 */
public class AbstractLifeCycleManagerBasicsTest {

	@Test
	public void testFactory() {

		IAgentClient messagingClient = Mockito.mock( IAgentClient.class );
		String appName = "my app";

		Instance instance = new Instance( "inst" );
		Assert.assertEquals(
				NotDeployed.class,
				AbstractLifeCycleManager.build( instance, appName, messagingClient ).getClass());

		instance.setStatus( InstanceStatus.DEPLOYED_STARTED );
		Assert.assertEquals(
				DeployedStarted.class,
				AbstractLifeCycleManager.build( instance, appName, messagingClient ).getClass());

		instance.setStatus( InstanceStatus.DEPLOYED_STOPPED );
		Assert.assertEquals(
				DeployedStopped.class,
				AbstractLifeCycleManager.build( instance, appName, messagingClient ).getClass());

		instance.setStatus( InstanceStatus.UNRESOLVED );
		Assert.assertEquals(
				Unresolved.class,
				AbstractLifeCycleManager.build( instance, appName, messagingClient ).getClass());

		for( InstanceStatus status : InstanceStatus.values()) {
			if( status.isStable())
				continue;

			instance.setStatus( status );
			Assert.assertEquals(
					TransitiveStates.class,
					AbstractLifeCycleManager.build( instance, appName, messagingClient ).getClass());
		}
	}


	@Test
	public void undeployingShouldNotModifyImportsList() throws Exception {

		IAgentClient messagingClient = Mockito.mock( IAgentClient.class );
		Instance instance = new Instance( "inst" );
		Assert.assertTrue( instance.getImports().isEmpty());

		instance.setStatus( InstanceStatus.DEPLOYED_STARTED );
		ImportHelpers.addImport( instance, "test", new Import( "/dep", "depComponent" ));
		Assert.assertFalse( instance.getImports().isEmpty());

		AbstractLifeCycleManager.build( instance, "app", messagingClient ).changeInstanceState(
				instance,
				new PluginMock(),
				InstanceStatus.NOT_DEPLOYED,
				null );

		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, instance.getStatus());
		Assert.assertFalse( instance.getImports().isEmpty());
	}
}
