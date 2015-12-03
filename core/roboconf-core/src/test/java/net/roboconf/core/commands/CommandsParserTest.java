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

import org.junit.Assert;
import net.roboconf.core.ErrorCode;
import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.ParsingError;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author Vincent Zurczak - Linagora
 */
public class CommandsParserTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();
	private TestApplication app;


	@Before
	public void initialize() throws Exception {
		this.app = new TestApplication();
	}


	@Test
	public void testValidate_1() throws Exception {

		File f = TestUtils.findTestFile( "/commands/single-line-commands.txt" );
		CommandsParser parser = new CommandsParser( this.app, f );
		Assert.assertEquals( 0, parser.getParsingErrors().size());
		Assert.assertEquals( 3, parser.instructions.size());
	}


	@Test
	public void testValidate_2() throws Exception {

		File f = TestUtils.findTestFile( "/commands/multi-line-commands.txt" );
		CommandsParser parser = new CommandsParser( this.app, f );
		Assert.assertEquals( 0, parser.getParsingErrors().size());
		Assert.assertEquals( 3, parser.instructions.size());
	}


	@Test
	public void testValidate_3() throws Exception {

		File f = TestUtils.findTestFile( "/commands/commands-in-error.txt" );
		CommandsParser parser = new CommandsParser( this.app, f );
		Assert.assertEquals( 3, parser.instructions.size());

		List<ParsingError> errors = parser.getParsingErrors();
		Assert.assertEquals( 4, errors.size());

		Assert.assertEquals( ErrorCode.CMD_UNRECOGNIZED_INSTRUCTION, errors.get( 0 ).getErrorCode());
		Assert.assertEquals( 2, errors.get( 0 ).getLine());

		Assert.assertEquals( ErrorCode.CMD_UNRESOLVED_VARIABLE, errors.get( 1 ).getErrorCode());
		Assert.assertEquals( 3, errors.get( 1 ).getLine());

		Assert.assertEquals( ErrorCode.CMD_UNRESOLVED_VARIABLE, errors.get( 1 ).getErrorCode());
		Assert.assertEquals( 4, errors.get( 2 ).getLine());

		Assert.assertEquals( ErrorCode.CMD_UNRESOLVED_VARIABLE, errors.get( 2 ).getErrorCode());
		Assert.assertEquals( 7, errors.get( 3 ).getLine());
	}


	@Test
	public void testInexistingFile() {

		CommandsParser parser = new CommandsParser( this.app, new File( "inexisting" ));
		List<ParsingError> errors = parser.getParsingErrors();
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.CMD_NO_INSTRUCTION, errors.get( 0 ).getErrorCode());
	}


	@Test
	public void testEmptyFile() throws Exception {

		CommandsParser parser = new CommandsParser( this.app, this.folder.newFile());
		List<ParsingError> errors = parser.getParsingErrors();
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.CMD_NO_INSTRUCTION, errors.get( 0 ).getErrorCode());
	}


	@Test
	public void testAllCommands() throws Exception {

		File f = TestUtils.findTestFile( "/commands/all-commands.txt" );
		CommandsParser parser = new CommandsParser( this.app, f );

		List<ParsingError> errors = parser.getParsingErrors();
		Assert.assertEquals( 0, errors.size());
	}


	@Test
	public void testEmailComandwithLineBreaks() throws Exception {

		File f = TestUtils.findTestFile( "/commands/email-command-with-line-breaks.txt" );
		CommandsParser parser = new CommandsParser( this.app, f );
		List<ParsingError> errors = parser.getParsingErrors();
		Assert.assertEquals( 0, errors.size());

		Assert.assertEquals( 1, parser.getInstructions().size());
		Assert.assertEquals( EmailCommandInstruction.class, parser.getInstructions().get( 0 ).getClass());

		String expected = "Subject: Alert!\nThis is an alert.";
		Assert.assertEquals( expected, ((EmailCommandInstruction) parser.getInstructions().get( 0 )).getMsg());
	}


	@Test
	public void testConstructorWithString() throws Exception {

		CommandsParser parser = new CommandsParser( this.app, "replicate /tomcat-vm as tomcat-vm-copy" );
		List<ParsingError> errors = parser.getParsingErrors();

		Assert.assertEquals( 0, errors.size());
		Assert.assertEquals( 1, parser.getInstructions().size());
		Assert.assertEquals( ReplicateCommandInstruction.class, parser.getInstructions().get( 0 ).getClass());
	}


	@Test
	public void testConstructorWithNullString() throws Exception {

		CommandsParser parser = new CommandsParser( this.app, (String) null );
		List<ParsingError> errors = parser.getParsingErrors();
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.CMD_NO_INSTRUCTION, errors.get( 0 ).getErrorCode());
	}
}
