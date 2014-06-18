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

import java.util.logging.Logger;

import junit.framework.Assert;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.runtime.Component;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.plugin.api.ExecutionLevel;
import net.roboconf.plugin.api.PluginException;
import net.roboconf.plugin.api.PluginInterface;
import net.roboconf.plugin.bash.PluginBash;
import net.roboconf.plugin.logger.PluginLogger;
import net.roboconf.plugin.puppet.PluginPuppet;

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class PluginManagerTest {

	@Test
	public void testFindplugin() {

		// No plugin
		Component comp = new Component( "comp" ).alias( "alias" ).installerName( "iaas" );
		Instance instance = new Instance( "inst" ).component( comp );

		PluginManager pm = new PluginManager();
		PluginInterface pi = pm.findPlugin( instance, Logger.getAnonymousLogger());
		Assert.assertNull( pi );

		// Puppet
		comp.setInstallerName( "puppet" );
		pi = pm.findPlugin( instance, Logger.getAnonymousLogger());
		Assert.assertEquals( "puppet", pi.getPluginName());
		Assert.assertEquals( PluginPuppet.class, pi.getClass());

		// Logger
		comp.setInstallerName( "loGGer" );
		pi = pm.findPlugin( instance, Logger.getAnonymousLogger());
		Assert.assertEquals( "logger", pi.getPluginName());
		Assert.assertEquals( PluginLogger.class, pi.getClass());

		// Bash
		comp.setInstallerName( "bash" );
		pi = pm.findPlugin( instance, Logger.getAnonymousLogger());
		Assert.assertEquals( "bash", pi.getPluginName());
		Assert.assertEquals( PluginBash.class, pi.getClass());
	}


	@Test( expected = PluginException.class )
	public void testInitializePluginForInstance_noPlugin() throws Exception {

		Component comp = new Component( "comp" ).alias( "alias" ).installerName( "iaas" );
		Instance instance = new Instance( "inst" ).component( comp );

		PluginManager.initializePluginForInstance( instance, ExecutionLevel.LOG );
	}


	@Test
	public void testInitializePluginForInstance() throws Exception {

		Component comp = new Component( "comp" ).alias( "alias" ).installerName( "bash" );
		Instance parentInstance = new Instance( "inst parent" ).component( comp );
		Instance childInstance = new Instance( "inst child" ).component( comp );
		InstanceHelpers.insertChild( parentInstance, childInstance );

		PluginManager.initializePluginForInstance( parentInstance, ExecutionLevel.LOG );
		// No error should occur
	}
}
