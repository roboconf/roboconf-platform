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
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import net.roboconf.core.commands.CommandsParser;
import net.roboconf.core.commands.CreateInstanceCommandInstruction;
import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.management.api.IApplicationMngr;
import net.roboconf.dm.management.api.ICommandsMngr.CommandExecutionContext;
import net.roboconf.dm.management.api.IInstancesMngr;
import net.roboconf.dm.management.exceptions.CommandException;

/**
 * @author Vincent Zurczak - Linagora
 */
public class CreateInstanceCommandInstructionsTest {

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
	public void testExecute_rootInstance_success() throws Exception {

		String txt = "create vm as tomcat-vm-2";
		CreateInstanceCommandExecution executor = buildExecutor( txt );

		executor.execute();
		Mockito.verify( this.instancesMngr, Mockito.times( 1 )).addInstance( this.ma, null, new Instance( "tomcat-vm-2" ));
	}


	@Test
	public void testExecute_rootInstance_withAutonomicContext_success() throws Exception {

		String txt = "create vm as tomcat-vm-2";
		CreateInstanceCommandExecution executor = buildExecutor( txt );

		executor.setExecutionContext( new CommandExecutionContext(
				new AtomicInteger( 4 ),
				new AtomicInteger( 3 ),
				2, false,
				"marker", null
		));

		executor.execute();
		Mockito.verify( this.instancesMngr, Mockito.times( 1 )).addInstance( this.ma, null, new Instance( "tomcat-vm-2" ));
	}


	@Test( expected = CommandException.class )
	public void testExecute_rootInstance_withAutonomicContext_failure() throws Exception {

		String txt = "create vm as tomcat-vm-2";
		CreateInstanceCommandExecution executor = buildExecutor( txt );

		executor.setExecutionContext( new CommandExecutionContext(
				new AtomicInteger( 6 ),
				new AtomicInteger( 3 ),
				5, true,
				null, null
		));

		executor.execute();
	}


	@Test
	public void testExecute_childInstance_success() throws Exception {

		String txt = "create tomcat as tomcat-server-2 under /tomcat-vm";
		CreateInstanceCommandExecution executor = buildExecutor( txt );

		executor.setExecutionContext( new CommandExecutionContext(
				new AtomicInteger( 4 ),
				new AtomicInteger( 3 ),
				2, true,
				"marker", null
		));

		executor.execute();
		Mockito.verify( this.instancesMngr, Mockito.times( 1 )).addInstance(
				this.ma,
				this.app.getTomcatVm(),
				new Instance( "tomcat-server-2" ));
	}


	@Test
	public void testExecute_childInstance_withAutonomicContext_success() throws Exception {

		String txt = "create tomcat as tomcat-server-2 under /tomcat-vm";
		CreateInstanceCommandExecution executor = buildExecutor( txt );

		executor.execute();
		Mockito.verify( this.instancesMngr, Mockito.times( 1 )).addInstance(
				this.ma,
				this.app.getTomcatVm(),
				new Instance( "tomcat-server-2" ));
	}


	@Test( expected = CommandException.class )
	public void testExecute_failure_randomException() throws Exception {

		// Simulate an exception
		Mockito
			.doThrow( new IOException( "for test" ))
			.when( this.instancesMngr )
			.addInstance(
					Mockito.any( ManagedApplication.class ),
					Mockito.any( Instance.class ),
					Mockito.any( Instance.class ));

		// Prepare the arguments
		String txt = "create vm as tomcat-vm3";
		CreateInstanceCommandExecution executor = buildExecutor( txt );

		// Execute the command
		executor.execute();
	}


	@Test( expected = CommandException.class )
	public void testExecute_failure_applicationNotFound() throws Exception {

		// Reset the mock
		Mockito.reset( this.applicationsMngr );

		// Prepare the arguments
		String txt = "create vm as tomcat-vm3";
		CreateInstanceCommandExecution executor = buildExecutor( txt );

		// Execute the command
		executor.execute();
	}


	@Test( expected = CommandException.class )
	public void testExecute_childInstance_failure_inexistingParent() throws Exception {

		String txt = "create tomcat as tomcat-server-2 under /inexisting";
		CreateInstanceCommandExecution executor = buildExecutor( txt, 1 );
		executor.execute();
	}


	@Test
	public void testVerify_exception_rootNoMax() throws Exception {

		CommandExecutionContext executionContext = new CommandExecutionContext(
				new AtomicInteger( 4 ),
				new AtomicInteger( 1 ),
				-1, true,	// no max
				null, null
		);

		CreateInstanceCommandExecution.verify( executionContext, this.app.getMySqlVm().getComponent());
	}


	@Test( expected = CommandException.class )
	public void testVerify_exception_rootWithMax_andStrictCheck() throws Exception {

		CommandExecutionContext executionContext = new CommandExecutionContext(
				new AtomicInteger( 4 ),
				new AtomicInteger( 3 ),
				2, true,
				null, null
		);

		CreateInstanceCommandExecution.verify( executionContext, this.app.getMySqlVm().getComponent());
	}


	@Test
	public void testVerify_exception_rootWithMax_butNoStrictCheck() throws Exception {

		CommandExecutionContext executionContext = new CommandExecutionContext(
				new AtomicInteger( 4 ),
				new AtomicInteger( 3 ),
				2, false,
				null, null
		);

		CreateInstanceCommandExecution.verify( executionContext, this.app.getMySqlVm().getComponent());
	}


	@Test
	public void testVerify_exception_nonRoot() throws Exception {

		CommandExecutionContext executionContext = new CommandExecutionContext(
				new AtomicInteger( 4 ),
				new AtomicInteger( 3 ),
				2, false,
				null, null
		);

		CreateInstanceCommandExecution.verify( executionContext, this.app.getMySql().getComponent());
	}


	@Test
	public void testVerify_noException() throws Exception {
		CreateInstanceCommandExecution.verify( null, null );
	}


	@Test
	public void testUpdate_nullContext() {
		CreateInstanceCommandExecution.update( null, null );
	}


	@Test
	public void testUpdate_nonNullContext() {

		CommandExecutionContext executionContext = new CommandExecutionContext(
				new AtomicInteger( 4 ),
				new AtomicInteger( 3 ),
				2, false,
				"xcv", "vbn"
		);

		Instance inst = new Instance( "inst" );
		Assert.assertEquals( 0, inst.data.size());

		CreateInstanceCommandExecution.update( executionContext, inst );

		Assert.assertEquals( 5, executionContext.getGlobalVmNumber().get());
		Assert.assertEquals( 4, executionContext.getAppVmNumber().get());
		Assert.assertEquals( 1, inst.data.size());
		Assert.assertEquals( "vbn", inst.data.get( "xcv" ));
	}


	private CreateInstanceCommandExecution buildExecutor( String command ) {
		return buildExecutor( command, 0 );
	}


	private CreateInstanceCommandExecution buildExecutor( String command, int validationError ) {

		CommandsParser parser = new CommandsParser( this.app, command );
		Assert.assertEquals( validationError, parser.getParsingErrors().size());
		Assert.assertEquals( 1, parser.getInstructions().size());
		Assert.assertEquals( CreateInstanceCommandInstruction.class, parser.getInstructions().get( 0 ).getClass());

		CreateInstanceCommandInstruction instr = (CreateInstanceCommandInstruction) parser.getInstructions().get( 0 );
		return new CreateInstanceCommandExecution( instr, this.manager );
	}
}
