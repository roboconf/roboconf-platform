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

import java.io.IOException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import net.roboconf.core.commands.BulkCommandInstructions;
import net.roboconf.core.commands.BulkCommandInstructions.ChangeStateInstruction;
import net.roboconf.core.commands.CommandsParser;
import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.management.api.IApplicationMngr;
import net.roboconf.dm.management.api.IInstancesMngr;
import net.roboconf.dm.management.exceptions.CommandException;

/**
 * @author Vincent Zurczak - Linagora
 */
public class BulkCommandInstructionsTest {

	private TestApplication app;
	private ManagedApplication ma;

	private Manager manager;
	private IInstancesMngr instancesMngr;
	private IApplicationMngr applicationsMngr;


	@Before
	public void initialize() throws Exception {

		this.app = new TestApplication();
		this.ma = new ManagedApplication( this.app );

		this.manager = Mockito.mock( Manager.class );
		this.instancesMngr = Mockito.mock( IInstancesMngr.class );
		this.applicationsMngr = Mockito.mock( IApplicationMngr.class );

		Mockito.when( this.manager.instancesMngr()).thenReturn( this.instancesMngr );
		Mockito.when( this.manager.applicationMngr()).thenReturn( this.applicationsMngr );
		Mockito.when( this.applicationsMngr.findManagedApplicationByName( this.app.getName())).thenReturn( this.ma );
	}


	@Test
	public void testExecute_success_byInstancePath() throws Exception {

		// Prepare
		String instancePath = InstanceHelpers.computeInstancePath( this.app.getTomcatVm());
		BulkCommandExecution executor1 = buildExecutor( ChangeStateInstruction.DEPLOY_AND_START_ALL.toString() + " " + instancePath );
		BulkCommandExecution executor2 = buildExecutor( ChangeStateInstruction.STOP_ALL.toString() + " " + instancePath );
		BulkCommandExecution executor3 = buildExecutor( ChangeStateInstruction.UNDEPLOY_ALL.toString() + " " + instancePath );
		BulkCommandExecution executor4 = buildExecutor( ChangeStateInstruction.DELETE.toString() + " " + instancePath );

		// Deploy and start all
		Mockito.verifyZeroInteractions( this.instancesMngr );
		executor1.execute();

		Mockito.verify( this.instancesMngr, Mockito.times( 1 )).deployAndStartAll( this.ma, this.app.getTomcatVm());
		Mockito.verify( this.instancesMngr, Mockito.only()).deployAndStartAll( this.ma, this.app.getTomcatVm());

		// Stop all
		Mockito.reset( this.instancesMngr );
		executor2.execute();

		Mockito.verify( this.instancesMngr, Mockito.times( 1 )).stopAll( this.ma, this.app.getTomcatVm());
		Mockito.verify( this.instancesMngr, Mockito.only()).stopAll( this.ma, this.app.getTomcatVm());

		// Undeploy all
		Mockito.reset( this.instancesMngr );
		executor3.execute();

		Mockito.verify( this.instancesMngr, Mockito.times( 1 )).undeployAll( this.ma, this.app.getTomcatVm());
		Mockito.verify( this.instancesMngr, Mockito.only()).undeployAll( this.ma, this.app.getTomcatVm());

		// Delete
		Mockito.reset( this.instancesMngr );
		executor4.execute();

		Mockito.verify( this.instancesMngr, Mockito.times( 1 )).removeInstance( this.ma, this.app.getTomcatVm(), false );
		Mockito.verify( this.instancesMngr, Mockito.only()).removeInstance( this.ma, this.app.getTomcatVm(), false );
	}


	@Test
	public void testExecute_success_byComponentName() throws Exception {

		// Prepare
		String suffix = " instances of vm";
		BulkCommandExecution executor1 = buildExecutor( ChangeStateInstruction.DEPLOY_AND_START_ALL.toString() + suffix );
		BulkCommandExecution executor2 = buildExecutor( ChangeStateInstruction.STOP_ALL.toString() + suffix );
		BulkCommandExecution executor3 = buildExecutor( ChangeStateInstruction.UNDEPLOY_ALL.toString() + suffix );
		BulkCommandExecution executor4 = buildExecutor( ChangeStateInstruction.DELETE.toString() + " all " + suffix );

		// Deploy and start all
		Mockito.verifyZeroInteractions( this.instancesMngr );
		executor1.execute();

		Mockito.verify( this.instancesMngr, Mockito.times( 1 )).deployAndStartAll( this.ma, this.app.getTomcatVm());
		Mockito.verify( this.instancesMngr, Mockito.times( 1 )).deployAndStartAll( this.ma, this.app.getMySqlVm());
		Mockito.verifyNoMoreInteractions( this.instancesMngr );

		// Stop all
		Mockito.reset( this.instancesMngr );
		executor2.execute();

		Mockito.verify( this.instancesMngr, Mockito.times( 1 )).stopAll( this.ma, this.app.getTomcatVm());
		Mockito.verify( this.instancesMngr, Mockito.times( 1 )).stopAll( this.ma, this.app.getMySqlVm());
		Mockito.verifyNoMoreInteractions( this.instancesMngr );

		// Undeploy all
		Mockito.reset( this.instancesMngr );
		executor3.execute();

		Mockito.verify( this.instancesMngr, Mockito.times( 1 )).undeployAll( this.ma, this.app.getTomcatVm());
		Mockito.verify( this.instancesMngr, Mockito.times( 1 )).undeployAll( this.ma, this.app.getMySqlVm());
		Mockito.verifyNoMoreInteractions( this.instancesMngr );

		// Delete
		Mockito.reset( this.instancesMngr );
		executor4.execute();

		Mockito.verify( this.instancesMngr, Mockito.times( 1 )).removeInstance( this.ma, this.app.getTomcatVm(), false );
		Mockito.verify( this.instancesMngr, Mockito.times( 1 )).removeInstance( this.ma, this.app.getMySqlVm(), false );
		Mockito.verifyNoMoreInteractions( this.instancesMngr );
	}


	@Test( expected = CommandException.class )
	public void testExecute_inexistingInstance() throws Exception {

		String instancePath = InstanceHelpers.computeInstancePath( this.app.getTomcatVm());
		BulkCommandExecution executor = buildExecutor( ChangeStateInstruction.DEPLOY_AND_START_ALL + " " + instancePath );

		Mockito.doThrow( new IOException( "for test" )).when( this.instancesMngr ).deployAndStartAll( this.ma, this.app.getTomcatVm());
		executor.execute();
	}


	@Test( expected = CommandException.class )
	public void testExecute_applicationNotFound() throws Exception {

		Mockito.reset( this.applicationsMngr );

		String instancePath = InstanceHelpers.computeInstancePath( this.app.getTomcatVm());
		BulkCommandExecution executor = buildExecutor( ChangeStateInstruction.DEPLOY_AND_START_ALL + " " + instancePath );

		Mockito.doThrow( new IOException( "for test" )).when( this.instancesMngr ).deployAndStartAll( this.ma, this.app.getTomcatVm());
		executor.execute();
	}


	@Test( expected = CommandException.class )
	public void testExecute_failure_inexistingInstance() throws Exception {

		BulkCommandExecution executor = buildExecutor( ChangeStateInstruction.DEPLOY_AND_START_ALL + "/inexisting", 1 );
		executor.execute();
	}


	private BulkCommandExecution buildExecutor( String command ) {
		return buildExecutor( command, 0 );
	}


	private BulkCommandExecution buildExecutor( String command, int validationError ) {

		CommandsParser parser = new CommandsParser( this.app, command );
		Assert.assertEquals( validationError, parser.getParsingErrors().size());
		Assert.assertEquals( 1, parser.getInstructions().size());
		Assert.assertEquals( BulkCommandInstructions.class, parser.getInstructions().get( 0 ).getClass());

		BulkCommandInstructions instr = (BulkCommandInstructions) parser.getInstructions().get( 0 );
		return new BulkCommandExecution( instr, this.manager );
	}
}
