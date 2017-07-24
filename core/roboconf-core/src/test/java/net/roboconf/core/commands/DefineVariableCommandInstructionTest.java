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

package net.roboconf.core.commands;

import static net.roboconf.core.commands.DefineVariableCommandInstruction.EXISTING_INDEX_PREFIX;
import static net.roboconf.core.commands.DefineVariableCommandInstruction.FORMATTED_TIME_PREFIX;
import static net.roboconf.core.commands.DefineVariableCommandInstruction.MILLI_TIME;
import static net.roboconf.core.commands.DefineVariableCommandInstruction.NANO_TIME;
import static net.roboconf.core.commands.DefineVariableCommandInstruction.RANDOM_UUID;
import static net.roboconf.core.commands.DefineVariableCommandInstruction.SMART_INDEX;

import java.io.File;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import net.roboconf.core.errors.ErrorCode;
import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.model.ParsingError;

/**
 * @author Vincent Zurczak - Linagora
 */
public class DefineVariableCommandInstructionTest {

	private TestApplication app;
	private Context context;


	@Before
	public void initialize() throws Exception {
		this.app = new TestApplication();
		this.context = new Context( this.app, new File( "whatever" ));
	}


	@Test
	public void testValidate_1() {

		String line = "define = toto";
		DefineVariableCommandInstruction instr = new DefineVariableCommandInstruction( this.context, line, 1 );
		List<ParsingError> errors = instr.validate();

		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.CMD_EMPTY_VARIABLE_NAME, errors.get( 0 ).getErrorCode());
	}


	@Test
	public void testValidate_2() {

		String line = "define =toto";
		DefineVariableCommandInstruction instr = new DefineVariableCommandInstruction( this.context, line, 1 );
		List<ParsingError> errors = instr.validate();

		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.CMD_EMPTY_VARIABLE_NAME, errors.get( 0 ).getErrorCode());
	}


	@Test
	public void testValidate_3() {

		String line = "define key=toto";
		DefineVariableCommandInstruction instr = new DefineVariableCommandInstruction( this.context, line, 1 );
		List<ParsingError> errors = instr.validate();

		Assert.assertEquals( 0, errors.size());
	}


	@Test
	public void testValidate_4() {

		String line = "define key = toto";
		DefineVariableCommandInstruction instr = new DefineVariableCommandInstruction( this.context, line, 1 );
		List<ParsingError> errors = instr.validate();

		Assert.assertEquals( 0, errors.size());
	}


	@Test
	public void testValidate_5() {

		String line = "define key =";
		DefineVariableCommandInstruction instr = new DefineVariableCommandInstruction( this.context, line, 1 );
		List<ParsingError> errors = instr.validate();

		Assert.assertEquals( 0, errors.size());
	}


	@Test
	public void testValidate_6() {

		String line = "define key = ";
		DefineVariableCommandInstruction instr = new DefineVariableCommandInstruction( this.context, line, 1 );
		List<ParsingError> errors = instr.validate();

		Assert.assertEquals( 0, errors.size());
	}


	@Test
	public void testValidate_7() {

		String line = "define key = value under /inexisting";
		DefineVariableCommandInstruction instr = new DefineVariableCommandInstruction( this.context, line, 1 );
		List<ParsingError> errors = instr.validate();

		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.CMD_NO_MATCHING_INSTANCE, errors.get( 0 ).getErrorCode());
	}


	@Test
	public void testValidate_8() {

		String line = "define invalid syntax";
		DefineVariableCommandInstruction instr = new DefineVariableCommandInstruction( this.context, line, 1 );
		List<ParsingError> errors = instr.validate();

		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.CMD_INVALID_SYNTAX, errors.get( 0 ).getErrorCode());
	}


	@Test
	public void testValidate_9() {

		String line = "define vm id = " + SMART_INDEX;
		DefineVariableCommandInstruction instr = new DefineVariableCommandInstruction( this.context, line, 1 );
		List<ParsingError> errors = instr.validate();

		Assert.assertEquals( 0, errors.size());
	}


	@Test
	public void testValidate_10() {

		String line = "define vm id = " + SMART_INDEX + " under /tomcat-vm";
		DefineVariableCommandInstruction instr = new DefineVariableCommandInstruction( this.context, line, 1 );
		List<ParsingError> errors = instr.validate();

		Assert.assertEquals( 0, errors.size());
	}


	@Test
	public void testValidate_11() {

		String line = "define ftime = at " + FORMATTED_TIME_PREFIX + "HH:mm:ss )";
		DefineVariableCommandInstruction instr = new DefineVariableCommandInstruction( this.context, line, 1 );
		List<ParsingError> errors = instr.validate();

		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.CMD_NO_MIX_FOR_PATTERNS, errors.get( 0 ).getErrorCode());
	}


	@Test
	public void testValidate_12() {

		String line = "define m = " + MILLI_TIME + " " + FORMATTED_TIME_PREFIX + "HH:mm:ss )";
		DefineVariableCommandInstruction instr = new DefineVariableCommandInstruction( this.context, line, 1 );
		List<ParsingError> errors = instr.validate();

		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.CMD_NO_MIX_FOR_PATTERNS, errors.get( 0 ).getErrorCode());
	}


	@Test
	public void testValidate_13() {

		String line = "define m = " + FORMATTED_TIME_PREFIX + "HH:mm:ss )" + " " + MILLI_TIME;
		DefineVariableCommandInstruction instr = new DefineVariableCommandInstruction( this.context, line, 1 );
		List<ParsingError> errors = instr.validate();

		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.CMD_NO_MIX_FOR_PATTERNS, errors.get( 0 ).getErrorCode());
	}


	@Test
	public void testValidate_14() {

		String line = "define m = " + FORMATTED_TIME_PREFIX + "HH:mm:ss )" + " oops ";
		DefineVariableCommandInstruction instr = new DefineVariableCommandInstruction( this.context, line, 1 );
		List<ParsingError> errors = instr.validate();

		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.CMD_NO_MIX_FOR_PATTERNS, errors.get( 0 ).getErrorCode());
	}


	@Test
	public void testValidate_15() {

		String line = "define m = " + FORMATTED_TIME_PREFIX + "invalid pattern )";
		DefineVariableCommandInstruction instr = new DefineVariableCommandInstruction( this.context, line, 1 );
		List<ParsingError> errors = instr.validate();

		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.CMD_INVALID_DATE_PATTERN, errors.get( 0 ).getErrorCode());
	}


	@Test
	public void testValidate_16() {

		String line = "define m = " + EXISTING_INDEX_PREFIX + " MIN)";
		DefineVariableCommandInstruction instr = new DefineVariableCommandInstruction( this.context, line, 1 );
		List<ParsingError> errors = instr.validate();
		Assert.assertEquals( 0, errors.size());

		line = "define m = " + EXISTING_INDEX_PREFIX + " MIN )";
		instr = new DefineVariableCommandInstruction( this.context, line, 1 );
		errors = instr.validate();
		Assert.assertEquals( 0, errors.size());

		line = "define m = " + EXISTING_INDEX_PREFIX + " MAX )";
		instr = new DefineVariableCommandInstruction( this.context, line, 1 );
		errors = instr.validate();
		Assert.assertEquals( 0, errors.size());

		line = "define m = " + EXISTING_INDEX_PREFIX + " MIN>2 )";
		instr = new DefineVariableCommandInstruction( this.context, line, 1 );
		errors = instr.validate();
		Assert.assertEquals( 0, errors.size());

		line = "define m = " + EXISTING_INDEX_PREFIX + " MIN> 4 )";
		instr = new DefineVariableCommandInstruction( this.context, line, 1 );
		errors = instr.validate();
		Assert.assertEquals( 0, errors.size());

		line = "define m = " + EXISTING_INDEX_PREFIX + " MIN > 4 )";
		instr = new DefineVariableCommandInstruction( this.context, line, 1 );
		errors = instr.validate();
		Assert.assertEquals( 0, errors.size());

		line = "define m = " + EXISTING_INDEX_PREFIX + " MIN <415 )";
		instr = new DefineVariableCommandInstruction( this.context, line, 1 );
		errors = instr.validate();
		Assert.assertEquals( 0, errors.size());


		line = "define m = " + EXISTING_INDEX_PREFIX + " MAX   > 4)";
		instr = new DefineVariableCommandInstruction( this.context, line, 1 );
		errors = instr.validate();
		Assert.assertEquals( 0, errors.size());

		line = "define m = " + EXISTING_INDEX_PREFIX + " MAX<18)";
		instr = new DefineVariableCommandInstruction( this.context, line, 1 );
		errors = instr.validate();
		Assert.assertEquals( 0, errors.size());

		line = "define m = " + EXISTING_INDEX_PREFIX + " MAXIMUM)";
		instr = new DefineVariableCommandInstruction( this.context, line, 1 );
		errors = instr.validate();

		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.CMD_INVALID_INDEX_PATTERN, errors.get( 0 ).getErrorCode());

		line = "define m = instance " + EXISTING_INDEX_PREFIX + " MAX<18) t under /tomcat-vm";
		instr = new DefineVariableCommandInstruction( this.context, line, 1 );
		errors = instr.validate();
		Assert.assertEquals( 0, errors.size());
	}


	@Test
	public void testUpdateContext() {

		// Preparation
		String instr1 = "define key = toto";
		String instr2 = "define key2 = $(key)2";
		String instr3 = "define key2 = $(key) 2";
		String instr4 = "define nano = " + NANO_TIME;
		String instr5 = "define milli = " + MILLI_TIME;
		String instr6 = "define uuid = " + RANDOM_UUID;
		String instr7 = "define indexWithoutInstance = oops " + SMART_INDEX;
		String instr8 = "define index = oops " + SMART_INDEX + " under /tomcat-vm";
		String instr9 = "define tt = t " + SMART_INDEX + " t under /tomcat-vm";
		String instr10 = "define ftime1 = " + FORMATTED_TIME_PREFIX + "HH:mm:ss )";
		String instr11 = "define ftime2 = " + FORMATTED_TIME_PREFIX + "MMMM)";
		String instr12 = "define ftime3 = something happenned at $(ftime1)";
		String instr13 = "define ftime4 = " + FORMATTED_TIME_PREFIX + "'today at' HH:mm:ss )";

		// Assertions
		DefineVariableCommandInstruction instr = new DefineVariableCommandInstruction( this.context, instr1, 1 );
		Assert.assertFalse( instr.isDisabled());
		Assert.assertEquals( 0, instr.validate().size());
		instr.updateContext();
		Assert.assertEquals( "toto", this.context.variables.get( "key" ));

		instr = new DefineVariableCommandInstruction( this.context, instr2, 1 );
		Assert.assertFalse( instr.isDisabled());
		Assert.assertEquals( 0, instr.validate().size());
		instr.updateContext();
		Assert.assertEquals( "toto2", this.context.variables.get( "key2" ));

		instr = new DefineVariableCommandInstruction( this.context, instr3, 1 );
		Assert.assertFalse( instr.isDisabled());
		Assert.assertEquals( 0, instr.validate().size());
		instr.updateContext();
		Assert.assertEquals( "toto 2", this.context.variables.get( "key2" ));

		instr = new DefineVariableCommandInstruction( this.context, instr4, 1 );
		Assert.assertFalse( instr.isDisabled());
		Assert.assertEquals( 0, instr.validate().size());
		instr.updateContext();
		Assert.assertNotNull( this.context.variables.get( "nano" ));
		Assert.assertFalse( NANO_TIME.equals( this.context.variables.get( "nano" )));

		instr = new DefineVariableCommandInstruction( this.context, instr5, 1 );
		Assert.assertFalse( instr.isDisabled());
		Assert.assertEquals( 0, instr.validate().size());
		instr.updateContext();
		Assert.assertNotNull( this.context.variables.get( "milli" ));
		Assert.assertFalse( MILLI_TIME.equals( this.context.variables.get( "milli" )));

		instr = new DefineVariableCommandInstruction( this.context, instr6, 1 );
		Assert.assertFalse( instr.isDisabled());
		Assert.assertEquals( 0, instr.validate().size());
		instr.updateContext();
		Assert.assertNotNull( this.context.variables.get( "uuid" ));
		Assert.assertFalse( RANDOM_UUID.equals( this.context.variables.get( "uuid" )));

		instr = new DefineVariableCommandInstruction( this.context, instr7, 1 );
		Assert.assertFalse( instr.isDisabled());
		Assert.assertEquals( 0, instr.validate().size());
		instr.updateContext();
		Assert.assertNotNull( this.context.variables.get( "indexWithoutInstance" ));
		Assert.assertEquals( "oops 1", this.context.variables.get( "indexWithoutInstance" ));

		instr = new DefineVariableCommandInstruction( this.context, instr9, 1 );
		Assert.assertFalse( instr.isDisabled());
		Assert.assertEquals( 0, instr.validate().size());
		instr.updateContext();
		Assert.assertNotNull( this.context.variables.get( "tt" ));
		Assert.assertEquals( "t 1 t", this.context.variables.get( "tt" ));

		instr = new DefineVariableCommandInstruction( this.context, instr10, 1 );
		Assert.assertFalse( instr.isDisabled());
		Assert.assertEquals( 0, instr.validate().size());
		instr.updateContext();
		Assert.assertNotNull( this.context.variables.get( "ftime1" ));
		Assert.assertNotEquals( "HH:mm:ss", this.context.variables.get( "ftime1" ));
		Assert.assertTrue( this.context.variables.get( "ftime1" ).matches( "\\d\\d:\\d\\d:\\d\\d" ));

		instr = new DefineVariableCommandInstruction( this.context, instr11, 1 );
		Assert.assertFalse( instr.isDisabled());
		Assert.assertEquals( 0, instr.validate().size());
		instr.updateContext();
		Assert.assertNotNull( this.context.variables.get( "ftime2" ));
		Assert.assertNotEquals( "MMM", this.context.variables.get( "ftime2" ));
		Assert.assertTrue( this.context.variables.get( "ftime2" ).matches( "[a-zA-Z]+.*" ));

		instr = new DefineVariableCommandInstruction( this.context, instr12, 1 );
		Assert.assertFalse( instr.isDisabled());
		Assert.assertEquals( 0, instr.validate().size());
		instr.updateContext();
		Assert.assertEquals(
				"something happenned at " + this.context.variables.get( "ftime1" ),
				this.context.variables.get( "ftime3" ));

		instr = new DefineVariableCommandInstruction( this.context, instr13, 1 );
		Assert.assertFalse( instr.isDisabled());
		Assert.assertEquals( 0, instr.validate().size());
		instr.updateContext();
		Assert.assertNotNull( this.context.variables.get( "ftime4" ));
		Assert.assertTrue( this.context.variables.get( "ftime4" ).matches( "today at \\d\\d:\\d\\d:\\d\\d" ));

		// Verify the smart index works as expected
		instr = new DefineVariableCommandInstruction( this.context, instr8, 1 );
		Assert.assertFalse( instr.isDisabled());
		Assert.assertEquals( 0, instr.validate().size());
		instr.updateContext();
		Assert.assertNotNull( this.context.variables.get( "index" ));
		Assert.assertEquals( "oops 1", this.context.variables.get( "index" ));

		instr.updateContext();
		Assert.assertNotNull( this.context.variables.get( "index" ));
		Assert.assertEquals( "oops 1", this.context.variables.get( "index" ));

		this.context.instancePathToComponentName.put( "/tomcat-vm/oops 1", this.app.getTomcatVm().getComponent().getName());

		instr.updateContext();
		Assert.assertNotNull( this.context.variables.get( "index" ));
		Assert.assertEquals( "oops 2", this.context.variables.get( "index" ));

		this.context.instancePathToComponentName.put( "/tomcat-vm/oops 2", this.app.getTomcatVm().getComponent().getName());

		instr.updateContext();
		Assert.assertNotNull( this.context.variables.get( "index" ));
		Assert.assertEquals( "oops 3", this.context.variables.get( "index" ));

		instr.updateContext();
		Assert.assertNotNull( this.context.variables.get( "index" ));
		Assert.assertEquals( "oops 3", this.context.variables.get( "index" ));
	}



	@Test
	public void testUpdateContext_queryIndexes() {

		this.context.instancePathToComponentName.put( "/tomcat-vm/oops 1", this.app.getTomcatVm().getComponent().getName());
		this.context.instancePathToComponentName.put( "/tomcat-vm/oops 2", this.app.getTomcatVm().getComponent().getName());
		this.context.instancePathToComponentName.put( "/tomcat-vm/oops 3", this.app.getTomcatVm().getComponent().getName());

		String instr14 = "define exi = oops " + EXISTING_INDEX_PREFIX + " MIN ) under /tomcat-vm";
		AbstractCommandInstruction instr = new DefineVariableCommandInstruction( this.context, instr14, 1 );
		Assert.assertEquals( 0, instr.validate().size());
		instr.updateContext();
		Assert.assertFalse( instr.isDisabled());
		Assert.assertNotNull( this.context.variables.get( "exi" ));
		Assert.assertEquals( "oops 1", this.context.variables.get( "exi" ));

		String instr15 = "define exi = oops " + EXISTING_INDEX_PREFIX + " MIN > 1 ) under /tomcat-vm";
		instr = new DefineVariableCommandInstruction( this.context, instr15, 1 );
		Assert.assertEquals( 0, instr.validate().size());
		instr.updateContext();
		Assert.assertFalse( instr.isDisabled());
		Assert.assertNotNull( this.context.variables.get( "exi" ));
		Assert.assertEquals( "oops 2", this.context.variables.get( "exi" ));

		String instr16 = "define exi = oops " + EXISTING_INDEX_PREFIX + " MIN > 2 ) under /tomcat-vm";
		instr = new DefineVariableCommandInstruction( this.context, instr16, 1 );
		Assert.assertEquals( 0, instr.validate().size());
		instr.updateContext();
		Assert.assertFalse( instr.isDisabled());
		Assert.assertNotNull( this.context.variables.get( "exi" ));
		Assert.assertEquals( "oops 3", this.context.variables.get( "exi" ));

		String instr17 = "define exi = oops " + EXISTING_INDEX_PREFIX + " MIN > 3 ) under /tomcat-vm";
		instr = new DefineVariableCommandInstruction( this.context, instr17, 1 );
		Assert.assertEquals( 0, instr.validate().size());
		instr.updateContext();
		Assert.assertTrue( instr.isDisabled());
		Assert.assertNotNull( this.context.variables.get( "exi" ));
		Assert.assertEquals( "fake", this.context.variables.get( "exi" ));

		String instr18 = "define exi = oops " + EXISTING_INDEX_PREFIX + " MAX ) under /tomcat-vm";
		instr = new DefineVariableCommandInstruction( this.context, instr18, 1 );
		Assert.assertEquals( 0, instr.validate().size());
		instr.updateContext();
		Assert.assertFalse( instr.isDisabled());
		Assert.assertNotNull( this.context.variables.get( "exi" ));
		Assert.assertEquals( "oops 3", this.context.variables.get( "exi" ));

		String instr19 = "define exi = oops " + EXISTING_INDEX_PREFIX + " MAX > 1 ) under /tomcat-vm";
		instr = new DefineVariableCommandInstruction( this.context, instr19, 1 );
		Assert.assertEquals( 0, instr.validate().size());
		instr.updateContext();
		Assert.assertFalse( instr.isDisabled());
		Assert.assertNotNull( this.context.variables.get( "exi" ));
		Assert.assertEquals( "oops 3", this.context.variables.get( "exi" ));

		String instr20 = "define exi = oops " + EXISTING_INDEX_PREFIX + " MAX < 3 ) under /tomcat-vm";
		instr = new DefineVariableCommandInstruction( this.context, instr20, 1 );
		Assert.assertEquals( 0, instr.validate().size());
		instr.updateContext();
		Assert.assertFalse( instr.isDisabled());
		Assert.assertNotNull( this.context.variables.get( "exi" ));
		Assert.assertEquals( "oops 2", this.context.variables.get( "exi" ));

		String instr21 = "define exi = oops " + EXISTING_INDEX_PREFIX + " MAX < 2 ) under /tomcat-vm";
		instr = new DefineVariableCommandInstruction( this.context, instr21, 1 );
		Assert.assertEquals( 0, instr.validate().size());
		instr.updateContext();
		Assert.assertFalse( instr.isDisabled());
		Assert.assertNotNull( this.context.variables.get( "exi" ));
		Assert.assertEquals( "oops 1", this.context.variables.get( "exi" ));

		String instr22 = "define exi = oops " + EXISTING_INDEX_PREFIX + " MAX > 3 ) under /tomcat-vm";
		instr = new DefineVariableCommandInstruction( this.context, instr22, 1 );
		Assert.assertEquals( 0, instr.validate().size());
		instr.updateContext();
		Assert.assertTrue( instr.isDisabled());
		Assert.assertNotNull( this.context.variables.get( "exi" ));
		Assert.assertEquals( "fake", this.context.variables.get( "exi" ));

		Assert.assertEquals( 1, this.context.disabledVariables.size());
		Assert.assertEquals( "exi", this.context.disabledVariables.iterator().next());
	}


	@Test
	public void testUpdateContext_forSmartIndex_1() {

		String line = "define vm id = " + SMART_INDEX;
		DefineVariableCommandInstruction instr = new DefineVariableCommandInstruction( this.context, line, 1 );
		Assert.assertEquals( 0, instr.validate().size());

		instr.updateContext();
		Assert.assertEquals( "1", this.context.variables.get( "vm id" ));
	}


	@Test
	public void testUpdateContext_forSmartIndex_2() {

		String line = "define vm id = " + SMART_INDEX + " under /tomcat-vm";
		DefineVariableCommandInstruction instr = new DefineVariableCommandInstruction( this.context, line, 1 );
		Assert.assertEquals( 0, instr.validate().size());

		instr.updateContext();
		Assert.assertEquals( "1", this.context.variables.get( "vm id" ));
	}


	@Test
	public void testUpdateContext_forSmartIndex_3() {

		// Define a variable once
		String line = "define war id = t" + SMART_INDEX + " under /tomcat-vm";
		DefineVariableCommandInstruction instr = new DefineVariableCommandInstruction( this.context, line, 1 );
		Assert.assertEquals( 0, instr.validate().size());

		instr.updateContext();
		Assert.assertEquals( "t1", this.context.variables.get( "war id" ));

		// Create this instance
		line = "Create tomcat as t1 under /tomcat-vm";
		CreateInstanceCommandInstruction ci = new CreateInstanceCommandInstruction( this.context, line, 1 );
		Assert.assertEquals( 0, ci.validate().size());
		ci.updateContext();

		// Define a new smart variable
		line = "define war id = t" + SMART_INDEX + " under /tomcat-vm";
		instr = new DefineVariableCommandInstruction( this.context, line, 1 );
		Assert.assertEquals( 0, instr.validate().size());

		instr.updateContext();
		Assert.assertEquals( "t2", this.context.variables.get( "war id" ));
	}
}
