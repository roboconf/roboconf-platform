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

package net.roboconf.dm.internal.commands;

import java.io.File;

import net.roboconf.core.Constants;
import net.roboconf.core.commands.CommandsParser;
import net.roboconf.core.commands.ExecuteCommandInstruction;
import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.management.exceptions.CommandException;

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

	private static final String FILE = "toto";
	private Manager manager;
	private TestApplication app;
	private File outputFile;


	@Before
	public void initialize() throws Exception {

		this.app = new TestApplication();
		this.app.setDirectory( this.folder.newFolder());

		this.manager = new Manager();
		this.manager.configurationMngr().setWorkingDirectory( this.folder.newFolder());

		this.outputFile = this.folder.newFile();
		Assert.assertTrue( this.outputFile.delete());

		File cmdDir = new File( this.app.getDirectory(), Constants.PROJECT_DIR_COMMANDS );
		Assert.assertTrue( cmdDir.mkdirs());

		File cmdFile = new File( cmdDir, FILE + Constants.FILE_EXT_COMMANDS );
		Utils.writeStringInto( "Write this into " + this.outputFile.getAbsolutePath(), cmdFile );
		Assert.assertTrue( cmdFile.exists());
	}


	@Test
	public void testExecute_success_withFileExtension() throws Exception {

		// The command executes another command that creates a file.
		Assert.assertFalse( this.outputFile.exists());
		ExecuteCommandExecution executor = buildExecutor( "execute " + FILE + Constants.FILE_EXT_COMMANDS, 0 );
		executor.execute();
		Assert.assertTrue( this.outputFile.exists());
	}


	@Test
	public void testExecute_success_withoutFileExtension() throws Exception {

		// The command executes another command that creates a file.
		Assert.assertFalse( this.outputFile.exists());
		ExecuteCommandExecution executor = buildExecutor( "execute " + FILE, 0 );
		executor.execute();
		Assert.assertTrue( this.outputFile.exists());
	}


	@Test( expected = CommandException.class )
	public void testExecute_failure_inexistingCommand() throws Exception {

		ExecuteCommandExecution executor = buildExecutor( "execute inexisting", 1 );
		executor.execute();
	}


	private ExecuteCommandExecution buildExecutor( String command, int validationError ) {

		CommandsParser parser = new CommandsParser( this.app, command );
		Assert.assertEquals( validationError, parser.getParsingErrors().size());
		Assert.assertEquals( 1, parser.getInstructions().size());
		Assert.assertEquals( ExecuteCommandInstruction.class, parser.getInstructions().get( 0 ).getClass());

		ExecuteCommandInstruction instr = (ExecuteCommandInstruction) parser.getInstructions().get( 0 );
		return new ExecuteCommandExecution( instr, this.manager );
	}
}
