/**
 * Copyright 2017 Linagora, Université Joseph Fourier, Floralis
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
import org.junit.Test;
import org.mockito.Mockito;

import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.messaging.api.business.IAgentClient;

/**
 * @author Vincent Zurczak - Linagora
 */
public class WaitingForAncestorTest {

	@Test
	public void testUnstableState() throws Exception {

		IAgentClient messagingClient = Mockito.mock( IAgentClient.class );
		WaitingForAncestor wfa = new WaitingForAncestor( "app", messagingClient );
		Instance inst = Mockito.mock( Instance.class );

		wfa.changeInstanceState( inst, null, InstanceStatus.DEPLOYING, null );
		Mockito.verifyZeroInteractions( messagingClient );
		Mockito.verifyZeroInteractions( inst );
	}


	@Test
	public void test_stopIsPropagatedToChildren_notDeployed() throws Exception {

		IAgentClient messagingClient = Mockito.mock( IAgentClient.class );
		WaitingForAncestor wfa = new WaitingForAncestor( "app", messagingClient );
		TestApplication app = new TestApplication();
		app.getTomcatVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		app.getTomcat().setStatus( InstanceStatus.WAITING_FOR_ANCESTOR );

		wfa.changeInstanceState( app.getTomcat(), null, InstanceStatus.DEPLOYED_STOPPED, null );
		Mockito.verifyZeroInteractions( messagingClient );
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, app.getTomcatVm().getStatus());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STOPPED, app.getTomcat().getStatus());
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getWar().getStatus());
	}


	@Test
	public void test_stopIsPropagatedToChildren_stopped() throws Exception {

		IAgentClient messagingClient = Mockito.mock( IAgentClient.class );
		WaitingForAncestor wfa = new WaitingForAncestor( "app", messagingClient );
		TestApplication app = new TestApplication();
		app.getTomcatVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		app.getTomcat().setStatus( InstanceStatus.WAITING_FOR_ANCESTOR );
		app.getWar().setStatus( InstanceStatus.DEPLOYED_STOPPED );

		wfa.changeInstanceState( app.getTomcat(), null, InstanceStatus.DEPLOYED_STOPPED, null );
		Mockito.verifyZeroInteractions( messagingClient );
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, app.getTomcatVm().getStatus());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STOPPED, app.getTomcat().getStatus());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STOPPED, app.getWar().getStatus());
	}


	@Test
	public void test_stopIsPropagatedToChildren_waitingForAncestor() throws Exception {

		IAgentClient messagingClient = Mockito.mock( IAgentClient.class );
		WaitingForAncestor wfa = new WaitingForAncestor( "app", messagingClient );
		TestApplication app = new TestApplication();
		app.getTomcatVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		app.getTomcat().setStatus( InstanceStatus.WAITING_FOR_ANCESTOR );
		app.getWar().setStatus( InstanceStatus.WAITING_FOR_ANCESTOR );

		wfa.changeInstanceState( app.getTomcat(), null, InstanceStatus.DEPLOYED_STOPPED, null );
		Mockito.verifyZeroInteractions( messagingClient );
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, app.getTomcatVm().getStatus());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STOPPED, app.getTomcat().getStatus());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STOPPED, app.getWar().getStatus());
	}
}
