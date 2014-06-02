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

package net.roboconf.iaas.in_memory.internal;

import junit.framework.Assert;
import net.roboconf.agent.AgentLauncher;
import net.roboconf.iaas.in_memory.internal.utils.AgentManager;

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class AgentManagerTest {

	@Test
	public void testManager() {

		AgentLauncher launcher = new AgentLauncher( null );
		AgentManager.INSTANCE.registerMachine( "id-0", launcher );
		Assert.assertNull( AgentManager.INSTANCE.unregisterMachine( "invalid id" ));
		Assert.assertNull( AgentManager.INSTANCE.unregisterMachine( null ));
		Assert.assertEquals( launcher, AgentManager.INSTANCE.unregisterMachine( "id-0" ));
	}
}
