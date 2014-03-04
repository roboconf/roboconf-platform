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

package net.roboconf.core.model.helpers;

import java.util.Map;

import junit.framework.Assert;

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class VariableHelpersTest {

	@Test
	public void testParseVariableName() {

		String[][] values = {
				{ "facet", "variable" },
				{ "FacetName", "complex.variable.name" },
				{ "facet-Name", "" },
				{ "", "some_variable" }
		};

		for( String[] value : values ) {
			String s = value[ 0 ] + "." + value[ 1 ];
			Map.Entry<String,String> entry = VariableHelpers.parseVariableName( s );
			Assert.assertEquals( "Invalid component or facet name for " + s, value[ 0 ], entry.getKey());
			Assert.assertEquals( "Invalid simple name for " + s, value[ 1 ], entry.getValue());
		}
	}


	@Test
	public void testParseExportedVariable() {

		String[][] values = {
				{ "variableName", "" },
				{ "variableName", "default value" },
				{ "complex.variable.name", "51" },
				{ "", "oops" }
		};

		for( String[] value : values ) {
			String s = value[ 0 ] + " = " + value[ 1 ];
			Map.Entry<String,String> entry = VariableHelpers.parseExportedVariable( s );
			Assert.assertEquals( "Invalid variable name for " + s, value[ 0 ], entry.getKey());
			Assert.assertEquals( "Invalid default value for " + s, value[ 1 ], entry.getValue());
		}
	}
}
