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

package net.roboconf.tooling.core.textactions;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;

import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.utils.Utils;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ToggleCommentsActionTest {

	@Test
	public void testBlockComment() throws Exception {

		File f = TestUtils.findTestFile( "/textactions/non-commented.txt" );
		String notCommented = Utils.readFileContent( f );

		f = TestUtils.findTestFile( "/textactions/commented2.txt" );
		String commented = Utils.readFileContent( f );

		ToggleCommentsAction action = new ToggleCommentsAction();
		String result = action.update( notCommented, 58, 51 );
		Assert.assertEquals( commented, result );

		action = new ToggleCommentsAction();
		result = action.update( commented, 58, 54 );
		Assert.assertEquals( notCommented, result );
	}


	@Test
	public void testEmptyLine() {

		ToggleCommentsAction action = new ToggleCommentsAction();
		String result = action.update( "", 58, 51 );
		Assert.assertEquals( "", result );
	}
}
