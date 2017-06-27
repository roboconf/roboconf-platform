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
public class RenameCommandInstructionTest {

	private TestApplication app;
	private Context context;


	@Before
	public void initialize() throws Exception {
		this.app = new TestApplication();
		this.context = new Context( this.app, new File( "whatever" ));
	}


	@Test
	public void testValidate_1() {

		String line = "rename /tomcat-vm as";
		RenameCommandInstruction instr = new RenameCommandInstruction( this.context, line, 1 );
		List<ParsingError> errors = instr.validate();

		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.CMD_MISSING_INSTANCE_NAME, errors.get( 0 ).getErrorCode());
	}


	@Test
	public void testValidate_2() {

		String line = "rename /tomcat-vm as   ";
		RenameCommandInstruction instr = new RenameCommandInstruction( this.context, line, 1 );
		List<ParsingError> errors = instr.validate();

		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.CMD_MISSING_INSTANCE_NAME, errors.get( 0 ).getErrorCode());
	}


	@Test
	public void testValidate_3() {

		String line = "rename /tomcat-vm as !boo!";
		RenameCommandInstruction instr = new RenameCommandInstruction( this.context, line, 1 );
		List<ParsingError> errors = instr.validate();

		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.CMD_INVALID_INSTANCE_NAME, errors.get( 0 ).getErrorCode());
	}


	@Test
	public void testValidate_4() {

		String line = "rename /vm as toto";
		RenameCommandInstruction instr = new RenameCommandInstruction( this.context, line, 1 );
		List<ParsingError> errors = instr.validate();

		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.CMD_NO_MATCHING_INSTANCE, errors.get( 0 ).getErrorCode());
	}


	@Test
	public void testValidate_5() {

		String line = "rename /tomcat-vm as mysql-vm";
		RenameCommandInstruction instr = new RenameCommandInstruction( this.context, line, 1 );
		List<ParsingError> errors = instr.validate();

		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.CMD_CONFLICTING_INSTANCE_NAME, errors.get( 0 ).getErrorCode());
	}


	@Test
	public void testValidate_6() {

		String line = "rename /tomcat-vm/tomcat-server as tomcat-server";
		RenameCommandInstruction instr = new RenameCommandInstruction( this.context, line, 1 );
		List<ParsingError> errors = instr.validate();

		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.CMD_CONFLICTING_INSTANCE_NAME, errors.get( 0 ).getErrorCode());
	}


	@Test
	public void testValidate_7() {

		String line = "rename /tomcat-vm/tomcat-server as tomcat-server-copy";
		RenameCommandInstruction instr = new RenameCommandInstruction( this.context, line, 1 );
		List<ParsingError> errors = instr.validate();

		Assert.assertEquals( 0, errors.size());
	}


	@Test
	public void testValidate_8() {

		String line = "rename /tomcat-vm as vm-for-tomcat";
		RenameCommandInstruction instr = new RenameCommandInstruction( this.context, line, 1 );
		List<ParsingError> errors = instr.validate();

		Assert.assertEquals( 0, errors.size());
	}


	@Test
	public void testValidate_9() {

		String line = "rename invalid syntax";
		RenameCommandInstruction instr = new RenameCommandInstruction( this.context, line, 1 );
		List<ParsingError> errors = instr.validate();

		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.CMD_INVALID_SYNTAX, errors.get( 0 ).getErrorCode());
	}


	@Test
	public void testValidate_10() {

		String line = "rename as toto";
		RenameCommandInstruction instr = new RenameCommandInstruction( this.context, line, 1 );
		List<ParsingError> errors = instr.validate();

		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.CMD_INVALID_SYNTAX, errors.get( 0 ).getErrorCode());
	}


	@Test
	public void testUpdateContext() {

		String line = "rename /tomcat-vm as vm-for-tomcat";
		RenameCommandInstruction instr = new RenameCommandInstruction( this.context, line, 1 );
		Assert.assertEquals( 0, instr.validate().size());

		Assert.assertTrue( this.context.instancePathToComponentName.containsKey( "/tomcat-vm" ));
		Assert.assertTrue( this.context.instancePathToComponentName.containsKey( "/tomcat-vm/tomcat-server" ));
		Assert.assertTrue( this.context.instancePathToComponentName.containsKey( "/tomcat-vm/tomcat-server/hello-world" ));

		int instancesCount = this.context.instancePathToComponentName.size();
		instr.updateContext();
		Assert.assertEquals( instancesCount, this.context.instancePathToComponentName.size());

		Assert.assertFalse( this.context.instancePathToComponentName.containsKey( "/tomcat-vm" ));
		Assert.assertFalse( this.context.instancePathToComponentName.containsKey( "/tomcat-vm/tomcat-server" ));
		Assert.assertFalse( this.context.instancePathToComponentName.containsKey( "/tomcat-vm/tomcat-server/hello-world" ));

		Assert.assertTrue( this.context.instancePathToComponentName.containsKey( "/vm-for-tomcat" ));
		Assert.assertTrue( this.context.instancePathToComponentName.containsKey( "/vm-for-tomcat/tomcat-server" ));
		Assert.assertTrue( this.context.instancePathToComponentName.containsKey( "/vm-for-tomcat/tomcat-server/hello-world" ));
	}
}
