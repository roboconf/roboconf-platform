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

package net.roboconf.tooling.core;

import static net.roboconf.tooling.core.TextUtils.isLineBreak;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class TextUtilsTest {

	@Test
	public void testRemoveComments() {

		String s = "no comment";
		Assert.assertEquals( s, TextUtils.removeComments( s ));

		s = "# comment";
		Assert.assertEquals( "", TextUtils.removeComments( s ));

		s = " # no comment";
		Assert.assertEquals( "", TextUtils.removeComments( s ));

		s = " # no comment # ";
		Assert.assertEquals( "", TextUtils.removeComments( s ));

		s = " first line #\n # no comment # ";
		Assert.assertEquals( " first line", TextUtils.removeComments( s ));

		s = " first line #\n second line\n # no comment # ";
		Assert.assertEquals( " first line\n second line", TextUtils.removeComments( s ));
	}



	@Test
	public void testIsLineBreak() {

		Assert.assertTrue( isLineBreak( '\n' ));
		Assert.assertTrue( isLineBreak( '\r' ));
		Assert.assertFalse( isLineBreak( 'c' ));
	}


	@Test
	public void testFixSelectionOffset() {

		String text = "something not too long";
		Assert.assertEquals( 0, TextUtils.fixSelectionOffset( text, -2 ));
		Assert.assertEquals( 0, TextUtils.fixSelectionOffset( text, 500 ));
		Assert.assertEquals( 0, TextUtils.fixSelectionOffset( text, 2 ));
		Assert.assertEquals( 0, TextUtils.fixSelectionOffset( text, 0 ));

		text = "  something not too long";
		Assert.assertEquals( 0, TextUtils.fixSelectionOffset( text, 0 ));
		Assert.assertEquals( 0, TextUtils.fixSelectionOffset( text, 1 ));
		Assert.assertEquals( 0, TextUtils.fixSelectionOffset( text, 2 ));
		Assert.assertEquals( 0, TextUtils.fixSelectionOffset( text, 3 ));

		text = "\nline 1\r\nline 2";
		Assert.assertEquals( 9, TextUtils.fixSelectionOffset( text, 9 ));
		Assert.assertEquals( 9, TextUtils.fixSelectionOffset( text, 10 ));
		Assert.assertEquals( 9, TextUtils.fixSelectionOffset( text, 11 ));
		Assert.assertEquals( 9, TextUtils.fixSelectionOffset( text, 12 ));

		Assert.assertEquals( 0, TextUtils.fixSelectionOffset( text, 0 ));
		Assert.assertEquals( 1, TextUtils.fixSelectionOffset( text, 1 ));
		Assert.assertEquals( 1, TextUtils.fixSelectionOffset( text, 2 ));
		Assert.assertEquals( 1, TextUtils.fixSelectionOffset( text, 3 ));
	}


	@Test
	public void testFixSelectionLength() {

		String text = "";
		Assert.assertEquals( 0, TextUtils.fixSelectionLength( text, 0, -1 ));
		Assert.assertEquals( 0, TextUtils.fixSelectionLength( text, 0, 0 ));
		Assert.assertEquals( 0, TextUtils.fixSelectionLength( text, 0, 1 ));
		Assert.assertEquals( 0, TextUtils.fixSelectionLength( text, 0, 100 ));

		text = "a";
		Assert.assertEquals( 1, TextUtils.fixSelectionLength( text, 0, -1 ));
		Assert.assertEquals( 1, TextUtils.fixSelectionLength( text, 0, 0 ));
		Assert.assertEquals( 1, TextUtils.fixSelectionLength( text, 0, 1 ));
		Assert.assertEquals( 1, TextUtils.fixSelectionLength( text, 0, 100 ));

		text = "a\nb\nc";
		Assert.assertEquals( 1, TextUtils.fixSelectionLength( text, 0, -1 ));
		Assert.assertEquals( 1, TextUtils.fixSelectionLength( text, 2, 1 ));
		Assert.assertEquals( 1, TextUtils.fixSelectionLength( text, 4, 1 ));
		Assert.assertEquals( text.length(), TextUtils.fixSelectionLength( text, 0, 100 ));
	}
}
