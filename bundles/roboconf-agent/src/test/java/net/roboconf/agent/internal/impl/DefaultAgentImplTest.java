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

package net.roboconf.agent.internal.impl;

import junit.framework.Assert;
import net.roboconf.agent.internal.misc.PluginMock;
import net.roboconf.core.model.runtime.Component;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.plugin.api.PluginInterface;

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class DefaultAgentImplTest {

	@Test
	public void testFindPlugin() {

		Instance inst = new Instance( "my inst" );
		DefaultAgentImpl agent = new DefaultAgentImpl();

		// No component and no installer
		Assert.assertNull( agent.findPlugin( inst ));

		// With an installer
		inst.setComponent( new Component( "comp" ).installerName( "inst" ));
		Assert.assertNull( agent.findPlugin( inst ));

		// With some plug-ins
		PluginMock plugin = new PluginMock();
		agent.setPlugins( new PluginInterface[]{ plugin });
		Assert.assertNull( agent.findPlugin( inst ));

		inst.getComponent().setInstallerName( plugin.getPluginName());
		Assert.assertEquals( plugin, agent.findPlugin( inst ));
	}
}
