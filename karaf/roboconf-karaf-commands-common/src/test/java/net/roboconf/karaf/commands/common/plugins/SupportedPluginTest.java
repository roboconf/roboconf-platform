/**
 * Copyright 2015-2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.karaf.commands.common.plugins;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class SupportedPluginTest {

	@Test
	public void testFindString() {

		for( SupportedPlugin st : SupportedPlugin.values())
			Assert.assertTrue( st.toString(), st.findString().size() > 0 );
	}


	@Test
	public void testFindCommands() {

		String roboconfVersion = "#nawak";
		for( SupportedPlugin st : SupportedPlugin.values()) {
			List<String> commands = st.findCommands( roboconfVersion );
			Assert.assertTrue( commands.size() > 0 );

			for( String cmd : commands ) {
				boolean isFeature = cmd.startsWith( "feature:install " );
				boolean isBundle = cmd.startsWith( "bundle:install " );
				Assert.assertTrue( isBundle || isFeature );

				if( isBundle ) {
					Assert.assertTrue( cmd.contains( " --start " ));
					Assert.assertTrue( cmd.endsWith( "/" + roboconfVersion ));
				}
			}
		}
	}


	@Test
	public void testWhich() {

		Assert.assertEquals( SupportedPlugin.SCRIPT, SupportedPlugin.which( "script" ));
		Assert.assertEquals( SupportedPlugin.FILE, SupportedPlugin.which( "fiLe" ));
		Assert.assertNull( SupportedPlugin.which( "unknown" ));
		Assert.assertNull( SupportedPlugin.which( null ));
	}
}
