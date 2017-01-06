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
public class CorrectIndentationActionTest {

	@Test
	public void testBasics() {

		CorrectIndentationAction action = new CorrectIndentationAction();
		Assert.assertEquals( "", action.update( "", 0, 1 ));
		Assert.assertEquals( "", action.update( "", 0, 0 ));
		Assert.assertEquals( "", action.update( "", 0, -11 ));
		Assert.assertEquals( "", action.update( "", 0, 10 ));
		Assert.assertEquals( "whatever", action.update( "whatever", 0, 1 ));
		Assert.assertEquals( "whatever", action.update( "		whatever", 0, 1 ));
	}


	@Test
	public void testMoreComplexCase_noBracket() {

		CorrectIndentationAction action = new CorrectIndentationAction();
		String p1 = "	indented\n";
		String p2 = "not indented";
		Assert.assertEquals( p1 + "	" + p2, action.update( p1 + p2, p1.length(), 1 ));
		Assert.assertEquals( p1 + "	" + p2, action.update( p1 + p2, p1.length(), p2.length()));
		Assert.assertEquals( p1 + "	" + p2, action.update( p1 + p2, p1.length(), -5 ));
	}


	@Test
	public void testRealFiles() throws Exception {

		// Load the resources
		File f = TestUtils.findTestFile( "/textactions/indentation-correct.txt" );
		String correct = Utils.readFileContent( f );

		f = TestUtils.findTestFile( "/textactions/indentation-incorrect.txt" );
		String incorrect = Utils.readFileContent( f );

		// "incorrect" should be "correct" after fixing the indentation
		CorrectIndentationAction action = new CorrectIndentationAction();
		Assert.assertEquals( correct, action.update( incorrect, 0, incorrect.length()));

		// Fixing the indentation should be idem-potent
		Assert.assertEquals( correct, action.update( correct, 0, correct.length()));

		// Format the "c1" component only
		Assert.assertEquals(
				// The same section has not the same length in both files
				correct.substring( 0, 44 ) + incorrect.substring( 45 ),
				action.update( incorrect, 0, 45 ));

		// Format all the instances
		Assert.assertEquals(
				incorrect.substring( 0, 45 ) + correct.substring( 44 ),
				action.update( incorrect, 45, 5000 ));

		// Format the beginning of the "c2" instance
		Assert.assertEquals(
				incorrect.substring( 0, 65 ) + correct.substring( 64, 64 + 50 ) + incorrect.substring( 117 ),
				action.update( incorrect, 65, 51 ));
	}
}
