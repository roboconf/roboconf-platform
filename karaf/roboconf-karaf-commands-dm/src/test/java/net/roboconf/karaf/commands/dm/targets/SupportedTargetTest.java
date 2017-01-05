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

package net.roboconf.karaf.commands.dm.targets;

import java.util.List;

import org.junit.Assert;

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class SupportedTargetTest {

	@Test
	public void testFindString() {

		for( SupportedTarget st : SupportedTarget.values())
			Assert.assertTrue( st.toString(), st.findString().size() > 0 );
	}


	@Test
	public void testFindCommands() {

		String roboconfVersion = "#nawak";
		for( SupportedTarget st : SupportedTarget.values()) {
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

		Assert.assertEquals( SupportedTarget.EC2, SupportedTarget.which( "ec2" ));
		Assert.assertEquals( SupportedTarget.EC2, SupportedTarget.which( "aws" ));
		Assert.assertEquals( SupportedTarget.EC2, SupportedTarget.which( "eC2" ));
		Assert.assertEquals( SupportedTarget.OPENSTACK, SupportedTarget.which( "openstack" ));
		Assert.assertNull( SupportedTarget.which( "unknown" ));
		Assert.assertNull( SupportedTarget.which( null ));
	}
}
