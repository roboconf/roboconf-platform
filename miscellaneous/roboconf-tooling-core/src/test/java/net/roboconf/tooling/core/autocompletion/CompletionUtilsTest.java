/**
 * Copyright 2016-2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.tooling.core.autocompletion;

import static net.roboconf.tooling.core.autocompletion.CompletionUtils.DEFAULT_VALUE;
import static net.roboconf.tooling.core.autocompletion.CompletionUtils.SET_BY_ROBOCONF;
import static net.roboconf.tooling.core.autocompletion.CompletionUtils.findAllTypes;
import static net.roboconf.tooling.core.autocompletion.CompletionUtils.findFilesToImport;
import static net.roboconf.tooling.core.autocompletion.CompletionUtils.resolveStringDescription;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;

import net.roboconf.core.Constants;

/**
 * @author Vincent Zurczak - Linagora
 */
public class CompletionUtilsTest {

	@Test
	public void testResolveStringDescription() {

		Assert.assertEquals( SET_BY_ROBOCONF, resolveStringDescription( Constants.SPECIFIC_VARIABLE_IP, "" ));
		Assert.assertEquals( SET_BY_ROBOCONF, resolveStringDescription( "toto." + Constants.SPECIFIC_VARIABLE_IP, "" ));

		Assert.assertNull( resolveStringDescription( "", "" ));
		Assert.assertNull( resolveStringDescription( "", null ));

		Assert.assertEquals( DEFAULT_VALUE + "val", resolveStringDescription( "", "val" ));
		Assert.assertEquals( DEFAULT_VALUE + "val", CompletionUtils.resolveStringDescription( "toto" + Constants.SPECIFIC_VARIABLE_IP, "val" ));
	}


	@Test
	public void testFindFilesToImport_inexistingSearchDirectory() {
		Assert.assertEquals( 0, findFilesToImport( new File( "whatever" ), null, null, "" ).size());
	}


	@Test
	public void testFindAllTypes_noDirectory() {
		Assert.assertEquals( 0, findAllTypes( new File( "whatever" )).size());
		Assert.assertEquals( 0, findAllTypes( null ).size());
	}
}
