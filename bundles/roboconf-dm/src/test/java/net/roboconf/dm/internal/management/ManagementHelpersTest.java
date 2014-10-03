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

package net.roboconf.dm.internal.management;

import junit.framework.Assert;
import net.roboconf.dm.management.Manager;

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ManagementHelpersTest {

	@Test
	public void testCreateConfiguredManager() {

		Manager manager = ManagementHelpers.createConfiguredManager();
		Assert.assertEquals( manager, manager.getConfiguration().getManager());

		manager = ManagementHelpers.createConfiguredManager( "my ip", "my user", "my pwd", "my directory" );
		Assert.assertEquals( manager, manager.getConfiguration().getManager());
		Assert.assertEquals( "my ip", manager.getConfiguration().getMessageServerIp());
		Assert.assertEquals( "my user", manager.getConfiguration().getMessageServerUsername());
		Assert.assertEquals( "my pwd", manager.getConfiguration().getMessageServerPassword());
		Assert.assertEquals( "my directory", manager.getConfiguration().getConfigurationDirectoryLocation());
	}
}
