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
import net.roboconf.agent.api.internal.AgentUtils;

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class AgentUtilsTest {

	@Test
	public void testIsValidIP() {

		Assert.assertTrue( AgentUtils.isValidIP( "127.0.0.1" ));
		Assert.assertTrue( AgentUtils.isValidIP( "192.168.1.1" ));
		Assert.assertTrue( AgentUtils.isValidIP( "46.120.105.36" ));
		Assert.assertTrue( AgentUtils.isValidIP( "216.24.131.152" ));

		Assert.assertFalse( AgentUtils.isValidIP( "localhost" ));
		Assert.assertFalse( AgentUtils.isValidIP( "127.0.0." ));
		Assert.assertFalse( AgentUtils.isValidIP( "127.0.0" ));
		Assert.assertFalse( AgentUtils.isValidIP( "192.168.1.-5" ));
		Assert.assertFalse( AgentUtils.isValidIP( "192.168.lol.1" ));
		Assert.assertFalse( AgentUtils.isValidIP( "" ));
		Assert.assertFalse( AgentUtils.isValidIP( null ));
	}
}