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

import net.roboconf.core.Constants;
import net.roboconf.core.errors.ErrorCode;
import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.model.ParsingError;
import net.roboconf.core.utils.Utils;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ExecuteCommandInstructionTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private static final String INVOKED_NAME = "to invoke";
	private static final String INVOKER_NAME = "invokes";

	private File cmdToExecute;
	private Context context;


	@Before
	public void initialize() throws Exception {

		TestApplication app = new TestApplication();
		app.setDirectory( this.folder.newFolder());

		File cmdDirectory = new File( app.getDirectory(), Constants.PROJECT_DIR_COMMANDS );
		Assert.assertTrue( cmdDirectory.mkdirs());
		this.cmdToExecute = new File( cmdDirectory, INVOKED_NAME + Constants.FILE_EXT_COMMANDS );

		this.context = new Context( app, new File( cmdDirectory, INVOKER_NAME + Constants.FILE_EXT_COMMANDS ));
	}


	@Test
	public void testValidate_1() {

		String line = "execute  ";
		ExecuteCommandInstruction instr = new ExecuteCommandInstruction( this.context, line, 1 );
		List<ParsingError> errors = instr.validate();

		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.CMD_MISSING_COMMAND_NAME, errors.get( 0 ).getErrorCode());
	}


	@Test
	public void testValidate_2() {

		String line = "execute inexisting";
		ExecuteCommandInstruction instr = new ExecuteCommandInstruction( this.context, line, 1 );
		List<ParsingError> errors = instr.validate();

		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.CMD_INEXISTING_COMMAND, errors.get( 0 ).getErrorCode());
	}


	@Test
	public void testValidate_3() {

		String line = "execute " + this.context.getCommandFile().getName();
		ExecuteCommandInstruction instr = new ExecuteCommandInstruction( this.context, line, 1 );
		List<ParsingError> errors = instr.validate();

		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.CMD_LOOPING_COMMAND, errors.get( 0 ).getErrorCode());
	}


	@Test
	public void testValidate_4() {

		String line = "execute " + INVOKER_NAME + "  ";
		ExecuteCommandInstruction instr = new ExecuteCommandInstruction( this.context, line, 1 );
		List<ParsingError> errors = instr.validate();

		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.CMD_LOOPING_COMMAND, errors.get( 0 ).getErrorCode());
	}


	@Test
	public void testValidate_5() {

		String line = "execute " + this.cmdToExecute.getName();
		ExecuteCommandInstruction instr = new ExecuteCommandInstruction( this.context, line, 1 );
		List<ParsingError> errors = instr.validate();

		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.CMD_INEXISTING_COMMAND, errors.get( 0 ).getErrorCode());
	}


	@Test
	public void testValidate_6() throws Exception {

		Assert.assertTrue( this.cmdToExecute.createNewFile());
		String line = "execute " + this.cmdToExecute.getName();
		ExecuteCommandInstruction instr = new ExecuteCommandInstruction( this.context, line, 1 );
		List<ParsingError> errors = instr.validate();

		Assert.assertEquals( 0, errors.size());
	}


	@Test
	public void testValidate_7() throws Exception {

		Assert.assertTrue( this.cmdToExecute.createNewFile());
		String line = "execute " + INVOKED_NAME;
		ExecuteCommandInstruction instr = new ExecuteCommandInstruction( this.context, line, 1 );
		List<ParsingError> errors = instr.validate();

		Assert.assertEquals( 0, errors.size());
	}


	@Test
	public void testValidate_8() throws Exception {

		Assert.assertTrue( this.cmdToExecute.createNewFile());
		String line = "excuse " + this.cmdToExecute.getName();
		ExecuteCommandInstruction instr = new ExecuteCommandInstruction( this.context, line, 1 );
		List<ParsingError> errors = instr.validate();

		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.CMD_INVALID_SYNTAX, errors.get( 0 ).getErrorCode());
	}


	@Test
	public void testValidate_noContextFile_noRecursiveCommand() throws Exception {

		// No context file
		Assert.assertTrue( this.context.getCommandFile().createNewFile());
		this.context = new Context( this.context.getApp(), null );

		// Few lines before, the same test gives a LOOPING_COMMAND error.
		// However, commands may not always be loaded from a file.
		String line = "execute " + INVOKER_NAME;
		ExecuteCommandInstruction instr = new ExecuteCommandInstruction( this.context, line, 1 );
		List<ParsingError> errors = instr.validate();

		Assert.assertEquals( 0, errors.size());
	}


	@Test
	public void testValidate_noContextFile_withRecursiveCommand() throws Exception {

		// The commands file to execute contains the exact same instruction
		Utils.writeStringInto( "  eXecuTe   " + INVOKER_NAME + ".commands  ", this.context.getCommandFile());

		// No context file
		this.context = new Context( this.context.getApp(), null );

		// Commands may not always be loaded from a file.
		String line = "execute " + INVOKER_NAME;
		ExecuteCommandInstruction instr = new ExecuteCommandInstruction( this.context, line, 1 );
		List<ParsingError> errors = instr.validate();

		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.CMD_NASTY_LOOPING_COMMAND, errors.get( 0 ).getErrorCode());
	}


	@Test
	public void testValidate_noContextFile_noRecursiveCommand_2() throws Exception {

		// The commands file to execute contains the exact same instruction
		Utils.writeStringInto( "  eXecuTe  " + INVOKED_NAME + "\n# " + INVOKER_NAME + ".commands  ", this.context.getCommandFile());

		// No context file
		this.context = new Context( this.context.getApp(), null );
		Assert.assertTrue( this.cmdToExecute.createNewFile());

		// Commands may not always be loaded from a file.
		String line = "execute " + INVOKER_NAME;
		ExecuteCommandInstruction instr = new ExecuteCommandInstruction( this.context, line, 1 );
		List<ParsingError> errors = instr.validate();

		Assert.assertEquals( 0, errors.size());
	}
}
