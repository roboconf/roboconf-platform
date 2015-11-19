/**
 * Copyright 2015 Linagora, Université Joseph Fourier, Floralis
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

import java.io.File;
import java.util.List;

import junit.framework.Assert;
import net.roboconf.core.ErrorCode;
import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.model.ParsingError;

import org.junit.Before;
import org.junit.Test;

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

		String line = "define vm id = " + DefineVariableCommandInstruction.SMART_INDEX;
		DefineVariableCommandInstruction instr = new DefineVariableCommandInstruction( this.context, line, 1 );
		List<ParsingError> errors = instr.validate();

		Assert.assertEquals( 0, errors.size());
	}


	@Test
	public void testValidate_10() {

		String line = "define vm id = " + DefineVariableCommandInstruction.SMART_INDEX + " under /tomcat-vm";
		DefineVariableCommandInstruction instr = new DefineVariableCommandInstruction( this.context, line, 1 );
		List<ParsingError> errors = instr.validate();

		Assert.assertEquals( 0, errors.size());
	}


	@Test
	public void testUpdateContext() {

		// Preparation
		String instr1 = "define key = toto";
		String instr2 = "define key2 = $(key)2";
		String instr3 = "define key2 = $(key) 2";
		String instr4 = "define nano = " + DefineVariableCommandInstruction.NANO_TIME;
		String instr5 = "define milli = " + DefineVariableCommandInstruction.MILLI_TIME;
		String instr6 = "define uuid = " + DefineVariableCommandInstruction.RANDOM_UUID;
		String instr7 = "define indexWithoutInstance = oops " + DefineVariableCommandInstruction.SMART_INDEX;
		String instr8 = "define index = oops " + DefineVariableCommandInstruction.SMART_INDEX + " under /tomcat-vm";
		String instr9 = "define tt = t " + DefineVariableCommandInstruction.SMART_INDEX + " t under /tomcat-vm";

		// Assertions
		DefineVariableCommandInstruction instr = new DefineVariableCommandInstruction( this.context, instr1, 1 );
		Assert.assertEquals( 0, instr.validate().size());
		instr.updateContext();
		Assert.assertEquals( "toto", this.context.variables.get( "key" ));

		instr = new DefineVariableCommandInstruction( this.context, instr2, 1 );
		Assert.assertEquals( 0, instr.validate().size());
		instr.updateContext();
		Assert.assertEquals( "toto2", this.context.variables.get( "key2" ));

		instr = new DefineVariableCommandInstruction( this.context, instr3, 1 );
		Assert.assertEquals( 0, instr.validate().size());
		instr.updateContext();
		Assert.assertEquals( "toto 2", this.context.variables.get( "key2" ));

		instr = new DefineVariableCommandInstruction( this.context, instr4, 1 );
		Assert.assertEquals( 0, instr.validate().size());
		instr.updateContext();
		Assert.assertNotNull( this.context.variables.get( "nano" ));
		Assert.assertFalse( DefineVariableCommandInstruction.NANO_TIME.equals( this.context.variables.get( "nano" )));

		instr = new DefineVariableCommandInstruction( this.context, instr5, 1 );
		Assert.assertEquals( 0, instr.validate().size());
		instr.updateContext();
		Assert.assertNotNull( this.context.variables.get( "milli" ));
		Assert.assertFalse( DefineVariableCommandInstruction.MILLI_TIME.equals( this.context.variables.get( "milli" )));

		instr = new DefineVariableCommandInstruction( this.context, instr6, 1 );
		Assert.assertEquals( 0, instr.validate().size());
		instr.updateContext();
		Assert.assertNotNull( this.context.variables.get( "uuid" ));
		Assert.assertFalse( DefineVariableCommandInstruction.RANDOM_UUID.equals( this.context.variables.get( "uuid" )));

		instr = new DefineVariableCommandInstruction( this.context, instr7, 1 );
		Assert.assertEquals( 0, instr.validate().size());
		instr.updateContext();
		Assert.assertNotNull( this.context.variables.get( "indexWithoutInstance" ));
		Assert.assertEquals( "oops 1", this.context.variables.get( "indexWithoutInstance" ));

		instr = new DefineVariableCommandInstruction( this.context, instr9, 1 );
		Assert.assertEquals( 0, instr.validate().size());
		instr.updateContext();
		Assert.assertNotNull( this.context.variables.get( "tt" ));
		Assert.assertEquals( "t 1 t", this.context.variables.get( "tt" ));

		instr = new DefineVariableCommandInstruction( this.context, instr8, 1 );
		Assert.assertEquals( 0, instr.validate().size());
		instr.updateContext();
		Assert.assertNotNull( this.context.variables.get( "index" ));
		Assert.assertEquals( "oops 1", this.context.variables.get( "index" ));

		// Verify the smart index works as expected
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
	public void testUpdateContext_forSmartIndex_1() {

		String line = "define vm id = " + DefineVariableCommandInstruction.SMART_INDEX;
		DefineVariableCommandInstruction instr = new DefineVariableCommandInstruction( this.context, line, 1 );
		Assert.assertEquals( 0, instr.validate().size());

		instr.updateContext();
		Assert.assertEquals( "1", this.context.variables.get( "vm id" ));
	}


	@Test
	public void testUpdateContext_forSmartIndex_2() {

		String line = "define vm id = " + DefineVariableCommandInstruction.SMART_INDEX + " under /tomcat-vm";
		DefineVariableCommandInstruction instr = new DefineVariableCommandInstruction( this.context, line, 1 );
		Assert.assertEquals( 0, instr.validate().size());

		instr.updateContext();
		Assert.assertEquals( "1", this.context.variables.get( "vm id" ));
	}


	@Test
	public void testUpdateContext_forSmartIndex_3() {

		// Define a variable once
		String line = "define war id = t" + DefineVariableCommandInstruction.SMART_INDEX + " under /tomcat-vm";
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
		line = "define war id = t" + DefineVariableCommandInstruction.SMART_INDEX + " under /tomcat-vm";
		instr = new DefineVariableCommandInstruction( this.context, line, 1 );
		Assert.assertEquals( 0, instr.validate().size());

		instr.updateContext();
		Assert.assertEquals( "t2", this.context.variables.get( "war id" ));
	}
}
