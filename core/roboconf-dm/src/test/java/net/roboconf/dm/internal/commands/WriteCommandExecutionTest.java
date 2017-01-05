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

import net.roboconf.core.commands.CommandsParser;
import net.roboconf.core.commands.WriteCommandInstruction;
import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.dm.management.exceptions.CommandException;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author Vincent Zurczak - Linagora
 */
public class WriteCommandExecutionTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();
	final TestApplication app = new TestApplication();


	@Test
	public void testExecute_success() throws Exception {

		File tf = this.folder.newFile();
		Assert.assertEquals( 0, tf.length());

		WriteCommandExecution exec = buildExecutor( "write this into " + tf.getAbsolutePath());
		exec.execute();

		Assert.assertNotSame( 0, tf.length());
	}


	@Test( expected = CommandException.class )
	public void testExecute_failure() throws Exception {

		File tf = this.folder.newFolder();
		WriteCommandExecution exec = buildExecutor( "write this into " + tf.getAbsolutePath());
		exec.execute();
	}


	private WriteCommandExecution buildExecutor( String command ) {

		CommandsParser parser = new CommandsParser( this.app, command );
		Assert.assertEquals( 0, parser.getParsingErrors().size());
		Assert.assertEquals( 1, parser.getInstructions().size());
		Assert.assertEquals( WriteCommandInstruction.class, parser.getInstructions().get( 0 ).getClass());

		WriteCommandInstruction instr = (WriteCommandInstruction) parser.getInstructions().get( 0 );
		return new WriteCommandExecution( instr );
	}
}
