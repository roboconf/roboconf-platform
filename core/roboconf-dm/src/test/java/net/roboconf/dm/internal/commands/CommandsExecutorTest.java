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

package net.roboconf.dm.internal.commands;

import java.io.File;
import java.util.List;

import junit.framework.Assert;
import net.roboconf.core.ErrorCode;
import net.roboconf.core.RoboconfError;
import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author Vincent Zurczak - Linagora
 */
public class CommandsExecutorTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private TestApplication app;
	private ManagedApplication ma;
	private Manager manager;


	@Before
	public void initialize() throws Exception {

		this.app = new TestApplication();
		this.ma = new ManagedApplication( this.app );

		this.manager = new Manager();
		this.manager.configurationMngr().setWorkingDirectory( this.folder.newFolder());
	}


	@Test
	public void testValidate() throws Exception {

		File f = TestUtils.findTestFile( "/commands/single-line-commands.txt" );
		CommandsExecutor executor = new CommandsExecutor( this.manager, this.ma, f );
		Assert.assertEquals( 0, executor.validate().size());
		Assert.assertEquals( 1, executor.instructions.size());
		Assert.assertEquals( 3, executor.foundInstructions );

		f = TestUtils.findTestFile( "/commands/multi-line-commands.txt" );
		executor = new CommandsExecutor( this.manager, this.ma, f );
		Assert.assertEquals( 0, executor.validate().size());
		Assert.assertEquals( 1, executor.instructions.size());
		Assert.assertEquals( 3, executor.foundInstructions );

		f = TestUtils.findTestFile( "/commands/commands-in-error.txt" );
		executor = new CommandsExecutor( this.manager, this.ma, f );
		Assert.assertEquals( 2, executor.instructions.size());
		Assert.assertEquals( 3, executor.foundInstructions );

		List<RoboconfError> errors = executor.validate();
		Assert.assertEquals( 4, errors.size());
		Assert.assertEquals( ErrorCode.EXEC_CMD_UNRECOGNIZED_INSTRUCTION, errors.get( 0 ).getErrorCode());
		Assert.assertEquals( ErrorCode.EXEC_CMD_UNRESOLVED_VARIABLE, errors.get( 1 ).getErrorCode());
		Assert.assertEquals( ErrorCode.EXEC_CMD_INVALID_INSTANCE_NAME, errors.get( 2 ).getErrorCode());
		Assert.assertEquals( ErrorCode.EXEC_CMD_INEXISTING_COMPONENT, errors.get( 3 ).getErrorCode());
	}


	@Test
	public void testInexistingFile() {

		CommandsExecutor executor = new CommandsExecutor( this.manager, this.ma, new File( "inexisting" ));
		List<RoboconfError> errors = executor.validate();
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.EXEC_CMD_NO_INSTRUCTION, errors.get( 0 ).getErrorCode());
	}


	@Test
	public void testEmptyFile() throws Exception {

		CommandsExecutor executor = new CommandsExecutor( this.manager, this.ma, this.folder.newFile());
		List<RoboconfError> errors = executor.validate();
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.EXEC_CMD_NO_INSTRUCTION, errors.get( 0 ).getErrorCode());
	}


	@Test
	public void testAllCommands() throws Exception {

		File f = TestUtils.findTestFile( "/commands/all-commands.txt" );
		CommandsExecutor executor = new CommandsExecutor( this.manager, this.ma, f );

		String targetId = this.manager.targetsMngr().createTarget( "" );
		Assert.assertEquals( "1", targetId );

		List<RoboconfError> errors = executor.validate();
		Assert.assertEquals( 0, errors.size());
	}
}
