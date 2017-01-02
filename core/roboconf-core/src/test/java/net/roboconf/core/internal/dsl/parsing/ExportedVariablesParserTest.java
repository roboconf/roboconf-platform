/**
 * Copyright 2016 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.core.internal.dsl.parsing;

import org.junit.Assert;
import org.junit.Test;

import net.roboconf.core.ErrorCode;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ExportedVariablesParserTest {

	@Test
	public void testParserIsResetCorrectly() {

		ExportedVariablesParser parser = new ExportedVariablesParser();
		Assert.assertEquals( 0, parser.errors.size());
		Assert.assertEquals( 0, parser.rawNameToVariables.size());

		parser.parse( "ip", null, 1 );
		Assert.assertEquals( 0, parser.errors.size());
		Assert.assertEquals( 1, parser.rawNameToVariables.size());

		parser.parse( "", null, 1 );
		Assert.assertEquals( 0, parser.errors.size());
		Assert.assertEquals( 0, parser.rawNameToVariables.size());
	}


	@Test
	public void testParser_1() {

		ExportedVariablesParser parser = new ExportedVariablesParser();
		parser.parse( "ip", null, 1 );

		Assert.assertEquals( 0, parser.errors.size());
		Assert.assertEquals( 1, parser.rawNameToVariables.size());
		Assert.assertTrue( parser.rawNameToVariables.containsKey( "ip" ));
		Assert.assertNull( parser.rawNameToVariables.get( "ip" ));
	}


	@Test
	public void testParser_2() {

		ExportedVariablesParser parser = new ExportedVariablesParser();
		parser.parse( "ip1, ip2", null, 1 );

		Assert.assertEquals( 0, parser.errors.size());
		Assert.assertEquals( 2, parser.rawNameToVariables.size());
		Assert.assertTrue( parser.rawNameToVariables.containsKey( "ip1" ));
		Assert.assertNull( parser.rawNameToVariables.get( "ip1" ));
		Assert.assertTrue( parser.rawNameToVariables.containsKey( "ip2" ));
		Assert.assertNull( parser.rawNameToVariables.get( "ip2" ));
	}


	@Test
	public void testParser_3() {

		ExportedVariablesParser parser = new ExportedVariablesParser();
		parser.parse( "port=8080", null, 1 );

		Assert.assertEquals( 0, parser.errors.size());
		Assert.assertEquals( 1, parser.rawNameToVariables.size());
		Assert.assertEquals( "8080", parser.rawNameToVariables.get( "port" ));

		parser = new ExportedVariablesParser();
		parser.parse( "port = 8080", null, 1 );

		Assert.assertEquals( 0, parser.errors.size());
		Assert.assertEquals( 1, parser.rawNameToVariables.size());
		Assert.assertEquals( "8080", parser.rawNameToVariables.get( "port" ));

		parser = new ExportedVariablesParser();
		parser.parse( " port  = 	 8080 ", null, 1 );

		Assert.assertEquals( 0, parser.errors.size());
		Assert.assertEquals( 1, parser.rawNameToVariables.size());
		Assert.assertEquals( "8080", parser.rawNameToVariables.get( "port" ));
	}


	@Test
	public void testParser_4() {

		ExportedVariablesParser parser = new ExportedVariablesParser();
		parser.parse( "ip, port = 8080", null, 1 );

		Assert.assertEquals( 0, parser.errors.size());
		Assert.assertEquals( 2, parser.rawNameToVariables.size());
		Assert.assertTrue( parser.rawNameToVariables.containsKey( "ip" ));
		Assert.assertNull( parser.rawNameToVariables.get( "ip" ));
		Assert.assertEquals( "8080", parser.rawNameToVariables.get( "port" ));
	}


	@Test
	public void testParser_5() {

		ExportedVariablesParser parser = new ExportedVariablesParser();
		parser.parse( "ip, port=8080", null, 1 );

		Assert.assertEquals( 0, parser.errors.size());
		Assert.assertEquals( 2, parser.rawNameToVariables.size());
		Assert.assertTrue( parser.rawNameToVariables.containsKey( "ip" ));
		Assert.assertNull( parser.rawNameToVariables.get( "ip" ));
		Assert.assertEquals( "8080", parser.rawNameToVariables.get( "port" ));
	}


	@Test
	public void testParser_6() {

		ExportedVariablesParser parser = new ExportedVariablesParser();
		parser.parse( "ip  , port =  	8080 ", null, 1 );

		Assert.assertEquals( 0, parser.errors.size());
		Assert.assertEquals( 2, parser.rawNameToVariables.size());
		Assert.assertTrue( parser.rawNameToVariables.containsKey( "ip" ));
		Assert.assertNull( parser.rawNameToVariables.get( "ip" ));
		Assert.assertEquals( "8080", parser.rawNameToVariables.get( "port" ));
	}


	@Test
	public void testParser_7() {

		ExportedVariablesParser parser = new ExportedVariablesParser();
		parser.parse( "  path = \"test with quotes\" ", null, 1 );

		Assert.assertEquals( 0, parser.errors.size());
		Assert.assertEquals( 1, parser.rawNameToVariables.size());
		Assert.assertEquals( "test with quotes", parser.rawNameToVariables.get( "path" ));
	}


	@Test
	public void testParser_8() {

		ExportedVariablesParser parser = new ExportedVariablesParser();
		parser.parse( "ip, port=8080, port-b = \"9000\",  path = \"test with quotes\" ", null, 1 );

		Assert.assertEquals( 0, parser.errors.size());
		Assert.assertEquals( 4, parser.rawNameToVariables.size());
		Assert.assertEquals( "test with quotes", parser.rawNameToVariables.get( "path" ));
		Assert.assertTrue( parser.rawNameToVariables.containsKey( "ip" ));
		Assert.assertNull( parser.rawNameToVariables.get( "ip" ));
		Assert.assertEquals( "8080", parser.rawNameToVariables.get( "port" ));
		Assert.assertEquals( "9000", parser.rawNameToVariables.get( "port-b" ));
	}


	@Test
	public void testParser_9() {

		ExportedVariablesParser parser = new ExportedVariablesParser();
		parser.parse( "ip, random[port] port,  path = \"test with quotes\" ", null, 1 );

		Assert.assertEquals( 0, parser.errors.size());
		Assert.assertEquals( 3, parser.rawNameToVariables.size());
		Assert.assertEquals( "test with quotes", parser.rawNameToVariables.get( "path" ));
		Assert.assertTrue( parser.rawNameToVariables.containsKey( "ip" ));
		Assert.assertNull( parser.rawNameToVariables.get( "ip" ));
		Assert.assertTrue( parser.rawNameToVariables.containsKey( "random[port] port" ));
		Assert.assertNull( parser.rawNameToVariables.get( "random[port] port" ));
	}


	@Test
	public void testParser_10() {

		ExportedVariablesParser parser = new ExportedVariablesParser();
		parser.parse( "ip, path1 = \"this path1, contains, commas('#') and special characters!*.\",  path = \"test with ; a semicolon\" ", null, 1 );

		Assert.assertEquals( 0, parser.errors.size());
		Assert.assertEquals( 3, parser.rawNameToVariables.size());
		Assert.assertEquals( "test with ; a semicolon", parser.rawNameToVariables.get( "path" ));
		Assert.assertEquals( "this path1, contains, commas('#') and special characters!*.", parser.rawNameToVariables.get( "path1" ));
		Assert.assertTrue( parser.rawNameToVariables.containsKey( "ip" ));
		Assert.assertNull( parser.rawNameToVariables.get( "ip" ));
	}


	@Test
	public void testParser_11() {

		ExportedVariablesParser parser = new ExportedVariablesParser();
		parser.parse( "path1 = \"value1, value2\"", null, 1 );

		Assert.assertEquals( 0, parser.errors.size());
		Assert.assertEquals( 1, parser.rawNameToVariables.size());
		Assert.assertEquals( "value1, value2", parser.rawNameToVariables.get( "path1" ));
	}


	@Test
	public void testParser_12() {

		ExportedVariablesParser parser = new ExportedVariablesParser();
		parser.parse( "ip = \"\", path1 = \"value1, value2\"", null, 1 );

		Assert.assertEquals( 0, parser.errors.size());
		Assert.assertEquals( 2, parser.rawNameToVariables.size());
		Assert.assertEquals( "value1, value2", parser.rawNameToVariables.get( "path1" ));
		Assert.assertEquals( "", parser.rawNameToVariables.get( "ip" ));
	}


	@Test
	public void testParser_13() {

		ExportedVariablesParser parser = new ExportedVariablesParser();
		parser.parse( "path1 = \"value1, value2\", ip = \"  \"", null, 1 );

		Assert.assertEquals( 0, parser.errors.size());
		Assert.assertEquals( 2, parser.rawNameToVariables.size());
		Assert.assertEquals( "value1, value2", parser.rawNameToVariables.get( "path1" ));
		Assert.assertEquals( "  ", parser.rawNameToVariables.get( "ip" ));
	}


	@Test
	public void testParser_14() {

		ExportedVariablesParser parser = new ExportedVariablesParser();
		parser.parse( "ip = ", null, 1 );

		Assert.assertEquals( 0, parser.errors.size());
		Assert.assertEquals( 1, parser.rawNameToVariables.size());
		Assert.assertTrue( parser.rawNameToVariables.containsKey( "ip" ));
		Assert.assertNull( parser.rawNameToVariables.get( "ip" ));
	}


	@Test
	public void testParserErrors_1() {

		ExportedVariablesParser parser = new ExportedVariablesParser();
		parser.parse( "path1 = \"value1, value2", null, 1 );

		Assert.assertEquals( 1, parser.errors.size());
		Assert.assertEquals( 0, parser.rawNameToVariables.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_EXPORT_COMPLEX_VALUE, parser.errors.get( 0 ).getErrorCode());
		Assert.assertEquals( "Variable name: path1", parser.errors.get( 0 ).getDetails());
	}


	@Test
	public void testParserErrors_2() {

		ExportedVariablesParser parser = new ExportedVariablesParser();
		parser.parse( "path1 = value1, value2\"", null, 1 );

		Assert.assertEquals( 0, parser.errors.size());
		Assert.assertEquals( 2, parser.rawNameToVariables.size());
		Assert.assertEquals( "value1", parser.rawNameToVariables.get( "path1" ));
		Assert.assertTrue( parser.rawNameToVariables.containsKey( "value2\"" ));
		Assert.assertNull( parser.rawNameToVariables.get( "value2\"" ));
	}


	@Test
	public void testParserErrors_3() {

		ExportedVariablesParser parser = new ExportedVariablesParser();
		parser.parse( "ip, port = 8080, path1 = value1, value2\"", null, 1 );

		Assert.assertEquals( 0, parser.errors.size());
		Assert.assertEquals( 4, parser.rawNameToVariables.size());

		Assert.assertTrue( parser.rawNameToVariables.containsKey( "ip" ));
		Assert.assertNull( parser.rawNameToVariables.get( "ip" ));
		Assert.assertEquals( "8080", parser.rawNameToVariables.get( "port" ));
		Assert.assertEquals( "value1", parser.rawNameToVariables.get( "path1" ));
		Assert.assertTrue( parser.rawNameToVariables.containsKey( "value2\"" ));
		Assert.assertNull( parser.rawNameToVariables.get( "value2\"" ));
	}


	@Test
	public void testParserErrors_4() {

		ExportedVariablesParser parser = new ExportedVariablesParser();
		parser.parse( "ip, port = 8080, path1 = \"value1, value2, path2 = \"value3, value 4\"", null, 1 );

		Assert.assertEquals( 1, parser.errors.size());
		Assert.assertEquals( 4, parser.rawNameToVariables.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_EXPORT_COMPLEX_VALUE, parser.errors.get( 0 ).getErrorCode());
		Assert.assertEquals( "Variable name: path1", parser.errors.get( 0 ).getDetails());

		Assert.assertTrue( parser.rawNameToVariables.containsKey( "ip" ));
		Assert.assertNull( parser.rawNameToVariables.get( "ip" ));
		Assert.assertEquals( "8080", parser.rawNameToVariables.get( "port" ));

		Assert.assertTrue( parser.rawNameToVariables.containsKey( "value3" ));
		Assert.assertNull( parser.rawNameToVariables.get( "value3" ));
		Assert.assertTrue( parser.rawNameToVariables.containsKey( "value 4\"" ));
		Assert.assertNull( parser.rawNameToVariables.get( "value 4\"" ));
	}


	@Test
	public void testParserErrors_5() {

		ExportedVariablesParser parser = new ExportedVariablesParser();
		parser.parse( "ip, port = 8080, path1 = \"value1, value2, path2 = short", null, 1 );

		Assert.assertEquals( 1, parser.errors.size());
		Assert.assertEquals( 2, parser.rawNameToVariables.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_EXPORT_COMPLEX_VALUE, parser.errors.get( 0 ).getErrorCode());
		Assert.assertEquals( "Variable name: path1", parser.errors.get( 0 ).getDetails());

		Assert.assertTrue( parser.rawNameToVariables.containsKey( "ip" ));
		Assert.assertNull( parser.rawNameToVariables.get( "ip" ));
		Assert.assertEquals( "8080", parser.rawNameToVariables.get( "port" ));
	}


	@Test
	public void testParserErrors_6() {

		ExportedVariablesParser parser = new ExportedVariablesParser();
		parser.parse( "path1 = \"value1, value2\", , ip = \"  \"", null, 1 );

		Assert.assertEquals( 1, parser.errors.size());
		Assert.assertEquals( 2, parser.rawNameToVariables.size());
		Assert.assertEquals( "value1, value2", parser.rawNameToVariables.get( "path1" ));
		Assert.assertEquals( "  ", parser.rawNameToVariables.get( "ip" ));

		Assert.assertEquals( ErrorCode.PM_EMPTY_VARIABLE_NAME, parser.errors.get( 0 ).getErrorCode());
	}


	@Test
	public void testParserErrors_7() {

		ExportedVariablesParser parser = new ExportedVariablesParser();
		parser.parse( "var2, var1 =value1\"", null, 1 );

		Assert.assertEquals( 1, parser.errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_EXPORT_COMPLEX_VALUE, parser.errors.get( 0 ).getErrorCode());

		Assert.assertEquals( 1, parser.rawNameToVariables.size());
		Assert.assertTrue( parser.rawNameToVariables.containsKey( "var2" ));
		Assert.assertNull( parser.rawNameToVariables.get( "var2" ));
	}
}
