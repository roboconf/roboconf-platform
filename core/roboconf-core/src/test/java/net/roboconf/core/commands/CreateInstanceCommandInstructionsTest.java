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

import java.io.File;
import java.util.List;

import org.junit.Assert;

import net.roboconf.core.errors.ErrorCode;
import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.model.ParsingError;

import org.junit.Before;
import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class CreateInstanceCommandInstructionsTest {

	private TestApplication app;
	private Context context;


	@Before
	public void initialize() throws Exception {
		this.app = new TestApplication();
		this.context = new Context( this.app, new File( "whatever" ));
	}


	@Test
	public void testValidate_1() {

		String line = "create tomcat as";
		CreateInstanceCommandInstruction instr = new CreateInstanceCommandInstruction( this.context, line, 1 );
		List<ParsingError> errors = instr.validate();

		Assert.assertEquals( this.app, instr.getApplication());
		Assert.assertEquals( 2, errors.size());
		Assert.assertEquals( ErrorCode.CMD_MISSING_PARENT_INSTANCE, errors.get( 0 ).getErrorCode());
		Assert.assertEquals( ErrorCode.CMD_MISSING_INSTANCE_NAME, errors.get( 1 ).getErrorCode());
	}


	@Test
	public void testValidate_2() {

		String line = "create tomcat as";
		CreateInstanceCommandInstruction instr = new CreateInstanceCommandInstruction( this.context, line, 1 );
		List<ParsingError> errors = instr.validate();

		Assert.assertEquals( 2, errors.size());
		Assert.assertEquals( ErrorCode.CMD_MISSING_PARENT_INSTANCE, errors.get( 0 ).getErrorCode());
		Assert.assertEquals( ErrorCode.CMD_MISSING_INSTANCE_NAME, errors.get( 1 ).getErrorCode());
	}


	@Test
	public void testValidate_3() {

		String line = "create tomcat as !boo!";
		CreateInstanceCommandInstruction instr = new CreateInstanceCommandInstruction( this.context, line, 1 );
		List<ParsingError> errors = instr.validate();

		Assert.assertEquals( 2, errors.size());
		Assert.assertEquals( ErrorCode.CMD_MISSING_PARENT_INSTANCE, errors.get( 0 ).getErrorCode());
		Assert.assertEquals( ErrorCode.CMD_INVALID_INSTANCE_NAME, errors.get( 1 ).getErrorCode());
	}


	@Test
	public void testValidate_4() {

		String line = "create as toto";
		CreateInstanceCommandInstruction instr = new CreateInstanceCommandInstruction( this.context, line, 1 );
		List<ParsingError> errors = instr.validate();

		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.CMD_MISSING_COMPONENT_NAME, errors.get( 0 ).getErrorCode());
	}


	@Test
	public void testValidate_5() {

		String line = "create     as toto";
		CreateInstanceCommandInstruction instr = new CreateInstanceCommandInstruction( this.context, line, 1 );
		List<ParsingError> errors = instr.validate();

		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.CMD_MISSING_COMPONENT_NAME, errors.get( 0 ).getErrorCode());
	}


	@Test
	public void testValidate_6() {

		String line = "create invalid as toto";
		CreateInstanceCommandInstruction instr = new CreateInstanceCommandInstruction( this.context, line, 1 );
		List<ParsingError> errors = instr.validate();

		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.CMD_INEXISTING_COMPONENT, errors.get( 0 ).getErrorCode());
	}


	@Test
	public void testValidate_7() {

		String line = "create tomcat as toto";
		CreateInstanceCommandInstruction instr = new CreateInstanceCommandInstruction( this.context, line, 1 );
		List<ParsingError> errors = instr.validate();

		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.CMD_MISSING_PARENT_INSTANCE, errors.get( 0 ).getErrorCode());
	}


	@Test
	public void testValidate_8() {

		String line = "create vm as toto";
		CreateInstanceCommandInstruction instr = new CreateInstanceCommandInstruction( this.context, line, 1 );
		List<ParsingError> errors = instr.validate();

		Assert.assertEquals( 0, errors.size());
	}


	@Test
	public void testValidate_9() {

		String line = "create tomcat as instance name with spaces under /tomcat-vm";
		CreateInstanceCommandInstruction instr = new CreateInstanceCommandInstruction( this.context, line, 1 );
		List<ParsingError> errors = instr.validate();

		Assert.assertEquals( 0, errors.size());
	}


	@Test
	public void testValidate_10() {

		String line = "create vm as my-vm under /tomcat-vm";
		CreateInstanceCommandInstruction instr = new CreateInstanceCommandInstruction( this.context, line, 1 );
		List<ParsingError> errors = instr.validate();

		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.CMD_CANNOT_HAVE_ANY_PARENT, errors.get( 0 ).getErrorCode());
	}


	@Test
	public void testValidate_11() {

		String line = "create tomcat as my-tomcat under /tomcat-vm/tomcat-server";
		CreateInstanceCommandInstruction instr = new CreateInstanceCommandInstruction( this.context, line, 1 );
		List<ParsingError> errors = instr.validate();

		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.CMD_NOT_AN_ACCEPTABLE_PARENT, errors.get( 0 ).getErrorCode());
	}


	@Test
	public void testValidate_12() {

		String line = "create tomcat as instance name with spaces under /inexisting";
		CreateInstanceCommandInstruction instr = new CreateInstanceCommandInstruction( this.context, line, 1 );
		List<ParsingError> errors = instr.validate();

		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.CMD_NO_MATCHING_INSTANCE, errors.get( 0 ).getErrorCode());
	}


	@Test
	public void testValidate_13() {

		String line = "create vm as tomcat-vm";
		CreateInstanceCommandInstruction instr = new CreateInstanceCommandInstruction( this.context, line, 1 );
		List<ParsingError> errors = instr.validate();

		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.CMD_CONFLICTING_INSTANCE_NAME, errors.get( 0 ).getErrorCode());
	}


	@Test
	public void testValidate_14() {

		String line = "create invalid syntax";
		CreateInstanceCommandInstruction instr = new CreateInstanceCommandInstruction( this.context, line, 1 );
		List<ParsingError> errors = instr.validate();

		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.CMD_INVALID_SYNTAX, errors.get( 0 ).getErrorCode());
	}


	@Test
	public void testUpdateContext_1() {

		String line = "create vm as my-vm";
		CreateInstanceCommandInstruction instr = new CreateInstanceCommandInstruction( this.context, line, 1 );

		Assert.assertEquals( 0, instr.validate().size());
		Assert.assertFalse( this.context.instancePathToComponentName.containsKey( "/my-vm" ));

		int instancesCount = this.context.instancePathToComponentName.size();
		instr.updateContext();
		Assert.assertEquals( instancesCount + 1, this.context.instancePathToComponentName.size());
		Assert.assertTrue( this.context.instancePathToComponentName.containsKey( "/my-vm" ));
	}


	@Test
	public void testUpdateContext_2() {

		String line = "create tomcat as tomcat2 under /tomcat-vm";
		CreateInstanceCommandInstruction instr = new CreateInstanceCommandInstruction( this.context, line, 1 );

		Assert.assertEquals( 0, instr.validate().size());
		Assert.assertFalse( this.context.instancePathToComponentName.containsKey( "/tomcat-vm/tomcat2" ));

		int instancesCount = this.context.instancePathToComponentName.size();
		instr.updateContext();
		Assert.assertEquals( instancesCount + 1, this.context.instancePathToComponentName.size());
		Assert.assertTrue( this.context.instancePathToComponentName.containsKey( "/tomcat-vm/tomcat2" ));
	}
}
