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

import org.junit.Assert;
import net.roboconf.core.commands.CommandsParser;
import net.roboconf.core.commands.RenameCommandInstruction;
import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.dm.management.exceptions.CommandException;

import org.junit.Before;
import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class RenameCommandInstructionTest {

	private TestApplication app;


	@Before
	public void initialize() throws Exception {
		this.app = new TestApplication();
	}


	@Test
	public void testExecute_success() throws Exception {

		String instancePath = InstanceHelpers.computeInstancePath( this.app.getTomcatVm());
		RenameCommandExecution executor = buildExecutor( "rename " + instancePath + " as toto" );

		Assert.assertEquals( "tomcat-vm", this.app.getTomcatVm().getName());
		executor.execute();
		Assert.assertEquals( "toto", this.app.getTomcatVm().getName());
	}


	@Test( expected = CommandException.class )
	public void testExecute_falure_instanceIsStarted() throws Exception {

		String instancePath = InstanceHelpers.computeInstancePath( this.app.getTomcatVm());
		this.app.getTomcatVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		RenameCommandExecution executor = buildExecutor( "rename " + instancePath + " as toto" );

		Assert.assertEquals( "tomcat-vm", this.app.getTomcatVm().getName());
		executor.execute();
	}


	@Test( expected = CommandException.class )
	public void testExecute_failure_inexistingInstance() throws Exception {

		RenameCommandExecution executor = buildExecutor( "rename /inexisting as toto", 1 );
		executor.execute();
	}


	private RenameCommandExecution buildExecutor( String command ) {
		return buildExecutor( command, 0 );
	}


	private RenameCommandExecution buildExecutor( String command, int validationError ) {

		CommandsParser parser = new CommandsParser( this.app, command );
		Assert.assertEquals( validationError, parser.getParsingErrors().size());
		Assert.assertEquals( 1, parser.getInstructions().size());
		Assert.assertEquals( RenameCommandInstruction.class, parser.getInstructions().get( 0 ).getClass());

		RenameCommandInstruction instr = (RenameCommandInstruction) parser.getInstructions().get( 0 );
		return new RenameCommandExecution( instr );
	}
}
