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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import net.roboconf.core.commands.AssociateTargetCommandInstruction;
import net.roboconf.core.commands.CommandsParser;
import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.dm.internal.test.TestManagerWrapper;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.management.exceptions.CommandException;

/**
 * @author Vincent Zurczak - Linagora
 */
public class AssociateTargetCommandInstructionTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private TestApplication app;
	private Manager manager;
	private TestManagerWrapper managerWrapper;


	@Before
	public void initialize() throws Exception {

		this.manager = new Manager();
		this.manager.configurationMngr().setWorkingDirectory( this.folder.newFolder());

		this.app = new TestApplication();
		this.app.setDirectory( this.folder.newFolder());
		ManagedApplication ma = new ManagedApplication( this.app );

		this.managerWrapper = new TestManagerWrapper( this.manager );
		this.managerWrapper.addManagedApplication( ma );
	}


	@Test
	public void testExecute_success() throws Exception {

		String targetId = this.manager.targetsMngr().createTarget( "id: tid\nhandler: h" );
		String instancePath = InstanceHelpers.computeInstancePath( this.app.getTomcatVm());
		AssociateTargetCommandExecution executor = buildExecutor( "associate " + instancePath + " with " + targetId );

		Assert.assertNull( this.manager.targetsMngr().findTargetId( this.app, instancePath ));
		executor.execute();
		Assert.assertEquals( targetId, this.manager.targetsMngr().findTargetId( this.app, instancePath ));
	}


	@Test( expected = CommandException.class )
	public void testExecute_inexistingTarget() throws Exception {

		String instancePath = InstanceHelpers.computeInstancePath( this.app.getTomcatVm());
		AssociateTargetCommandExecution executor = buildExecutor( "associate " + instancePath + " with 80" );

		Assert.assertNull( this.manager.targetsMngr().findTargetId( this.app, instancePath ));
		executor.execute();
	}


	@Test( expected = CommandException.class )
	public void testExecute_inexistingApplication() throws Exception {

		String targetId = this.manager.targetsMngr().createTarget( "id: tid\nhandler: h" );
		String instancePath = InstanceHelpers.computeInstancePath( this.app.getTomcatVm());
		AssociateTargetCommandExecution executor = buildExecutor( "associate " + instancePath + " with " + targetId );

		this.managerWrapper.clearManagedApplications();
		executor.execute();
	}


	@Test( expected = CommandException.class )
	public void testExecute_inexistingInstance() throws Exception {

		String targetId = this.manager.targetsMngr().createTarget( "id: tid\nhandler: h" );
		AssociateTargetCommandExecution executor = buildExecutor( "associate /inexisting with " + targetId, 1 );
		executor.execute();
	}


	private AssociateTargetCommandExecution buildExecutor( String command ) {
		return buildExecutor( command, 0 );
	}


	private AssociateTargetCommandExecution buildExecutor( String command, int validationError ) {

		CommandsParser parser = new CommandsParser( this.app, command );
		Assert.assertEquals( validationError, parser.getParsingErrors().size());
		Assert.assertEquals( 1, parser.getInstructions().size());
		Assert.assertEquals( AssociateTargetCommandInstruction.class, parser.getInstructions().get( 0 ).getClass());

		AssociateTargetCommandInstruction instr = (AssociateTargetCommandInstruction) parser.getInstructions().get( 0 );
		return new AssociateTargetCommandExecution( instr, this.manager );
	}
}
