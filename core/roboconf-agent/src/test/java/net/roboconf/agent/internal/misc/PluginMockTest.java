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

package net.roboconf.agent.internal.misc;

import org.junit.Assert;
import net.roboconf.core.model.beans.Instance;

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class PluginMockTest {

	@Test
	public void testPlugin() throws Exception {

		PluginMock pl = new PluginMock();
		Assert.assertEquals( "mock", pl.getPluginName());

		// Make sure we can invoke invoke method in any order
		pl.setNames( "My Agent", null );

		pl.undeploy( null );
		pl.undeploy( new Instance( "inst" ));

		pl.start( null );
		pl.start( new Instance( "inst" ));

		pl.deploy( null );
		pl.deploy( new Instance( "inst" ));

		pl.stop( null );
		pl.stop( new Instance( "inst" ));

		pl.update( null, null, null );
		pl.update( new Instance( "inst" ), null, null );

		pl.initialize( null );
		pl.initialize( new Instance( "inst" ));
	}
}
