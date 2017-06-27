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

package net.roboconf.core.errors;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import net.roboconf.core.errors.ErrorCode.ErrorCategory;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ErrorCodeTest {

	/**
	 * Every error code must have an unique ID in its category.
	 */
	@Test
	public void testCodesUnicity() {

		Map<ErrorCategory,ErrorCode> categoryToLastCode = new HashMap<> ();
		for( ErrorCode code : ErrorCode.values()) {
			ErrorCode lastCode = categoryToLastCode.get( code.getCategory());
			if( lastCode != null )
				Assert.assertTrue( "RoboconfError code " + code + " is already used.", code.getErrorId() > lastCode.getErrorId());

			categoryToLastCode.put( code.getCategory(), code );
		}
	}


	/**
	 * RoboconfError codes must have a have a given prefix depending on their category.
	 */
	@Test
	public void testCodesPrefixes() {

		Map<ErrorCategory,String> categoryToPrefix = new HashMap<> ();
		categoryToPrefix.put( ErrorCategory.CONVERSION, "CO_" );
		categoryToPrefix.put( ErrorCategory.PARSING, "P_" );
		categoryToPrefix.put( ErrorCategory.PARSING_MODEL, "PM_" );
		categoryToPrefix.put( ErrorCategory.RUNTIME_MODEL, "RM_" );
		categoryToPrefix.put( ErrorCategory.EXECUTION, "EXEC_" );
		categoryToPrefix.put( ErrorCategory.PROJECT, "PROJ_" );
		categoryToPrefix.put( ErrorCategory.RECIPES, "REC_" );
		categoryToPrefix.put( ErrorCategory.COMMANDS, "CMD_" );
		categoryToPrefix.put( ErrorCategory.RULES, "RULE_" );
		categoryToPrefix.put( ErrorCategory.REST, "REST_" );

		for( ErrorCode code : ErrorCode.values()) {
			String prefix = categoryToPrefix.get( code.getCategory());
			Assert.assertNotNull( "No prefix was found for " + code, prefix );
			Assert.assertTrue( "Invalid prefix for " + code + ". " + prefix + " was expected.", code.toString().startsWith( prefix ));
			Assert.assertNotNull( code.getI18nProperties());
		}
	}


	@Test
	public void verifyCodesAreInUpperCase() {
		for( ErrorCode code : ErrorCode.values()) {
			Assert.assertEquals( code + " should be in upper case.", code.toString(), code.toString().toUpperCase());
		}
	}


	@Test
	public void testToString() {

		for( ErrorCode code : ErrorCode.values()) {
			RoboconfError re = new RoboconfError( code );
			Assert.assertEquals( code, re.getErrorCode());
			Assert.assertNotNull( re.toString());
		}
	}


	@Test
	public void testGetI18nProperties() {

		Assert.assertEquals( 1, ErrorCode.CMD_INVALID_INSTANCE_NAME.getI18nProperties().length );
		Assert.assertEquals( 0, ErrorCode.RULE_EMPTY_NAME.getI18nProperties().length );
	}
}
