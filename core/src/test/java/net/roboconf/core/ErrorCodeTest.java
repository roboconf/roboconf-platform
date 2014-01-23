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

package net.roboconf.core;

import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;
import net.roboconf.core.ErrorCode.ErrorCategory;

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ErrorCodeTest {

	/**
	 * Every error code must have an unique ID in its category.
	 */
	@Test
	public void testCodesUnicity() {

		Map<ErrorCategory,ErrorCode> categoryToLastCode = new HashMap<ErrorCategory,ErrorCode> ();
		for( ErrorCode code : ErrorCode.values()) {
			ErrorCode lastCode = categoryToLastCode.get( code.getCategory());
			if( lastCode != null )
				Assert.assertTrue( "Error code " + code + " is already used.", code.getErrorId() > lastCode.getErrorId());

			categoryToLastCode.put( code.getCategory(), code );
		}
	}


	/**
	 * Error codes must have a have a given prefix depending on their category.
	 */
	@Test
	public void testCodesPrefixes() {

		Map<ErrorCategory,String> categoryToPrefix = new HashMap<ErrorCategory,String> ();
		categoryToPrefix.put( ErrorCategory.CONVERSION, "CO_" );
		categoryToPrefix.put( ErrorCategory.PARSING, "P_" );
		categoryToPrefix.put( ErrorCategory.PARSING_MODEL, "PM_" );
		categoryToPrefix.put( ErrorCategory.RUNTIME_MODEL, "RM_" );
		categoryToPrefix.put( ErrorCategory.EXECUTION, "EXEC_" );

		for( ErrorCode code : ErrorCode.values()) {
			String prefix = categoryToPrefix.get( code.getCategory());
			Assert.assertNotNull( "No prefix was found for " + code, prefix );
			Assert.assertTrue( "Invalid prefix for " + code + ". " + prefix + " was expected.", code.toString().startsWith( prefix ));
		}
	}


	/**
	 * Error codes must be in upper case.
	 */
	@Test
	public void testCodesUpperCase() {
		for( ErrorCode code : ErrorCode.values()) {
			Assert.assertEquals( code + " should be in upper case.", code.toString(), code.toString().toUpperCase());
		}
	}
}
