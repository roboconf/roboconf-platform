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

package net.roboconf.agent.internal.lifecycle;

import junit.framework.Assert;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.core.model.runtime.Instance.InstanceStatus;
import net.roboconf.messaging.client.IAgentClient;
import net.roboconf.messaging.internal.client.test.TestClientAgent;

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class AbstractLifeCycleManager_BasicTest {

	@Test
	public void testFactory() {

		IAgentClient messagingClient = new TestClientAgent();
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

		for( InstanceStatus status : InstanceStatus.values()) {
			if( status.isStable())
				continue;

			instance.setStatus( status );
			Assert.assertEquals(
					TransitiveStates.class,
					AbstractLifeCycleManager.build( instance, appName, messagingClient ).getClass());
		}
	}
}
