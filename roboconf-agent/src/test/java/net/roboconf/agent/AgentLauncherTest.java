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

package net.roboconf.agent;

import junit.framework.Assert;
import net.roboconf.agent.tests.TestAgentMessagingClient;
import net.roboconf.messaging.client.IAgentClient;
import net.roboconf.messaging.client.IDmClient;
import net.roboconf.messaging.client.MessageServerClientFactory;
import net.roboconf.plugin.api.ExecutionLevel;

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class AgentLauncherTest {

	@Test
	public void testStopAgent() {

		AgentData ad = new AgentData();
		AgentLauncher launcher = new AgentLauncher( ad );
		Assert.assertFalse( launcher.isRunning());

		launcher.stopAgent();
		Assert.assertFalse( launcher.isRunning());

		// A second call should not throw any error
		launcher.stopAgent();
		Assert.assertFalse( launcher.isRunning());
	}


	@Test
	public void testStartAndStop() throws Exception {

		AgentData ad = new AgentData();
		AgentLauncher launcher = new AgentLauncher( "I am the agent", ad );
		launcher.setFactory( new MessageServerClientFactory() {
			@Override
			public IAgentClient createAgentClient() {
				return new TestAgentMessagingClient();
			}

			@Override
			public IDmClient createDmClient() {
				return null;
			}
		});

		Assert.assertFalse( launcher.isRunning());
		launcher.launchAgent( ExecutionLevel.LOG, null );
		Assert.assertTrue( launcher.isRunning());

		launcher.stopAgent();
		Assert.assertFalse( launcher.isRunning());
	}
}
