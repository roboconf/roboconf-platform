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
import net.roboconf.core.commands.ReplicateCommandInstruction;
import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.management.api.IApplicationMngr;
import net.roboconf.dm.management.api.ICommandsMngr.CommandExecutionContext;
import net.roboconf.dm.management.api.IInstancesMngr;
import net.roboconf.dm.management.api.ITargetsMngr;
import net.roboconf.dm.management.exceptions.CommandException;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ReplicateCommandExecutionTest {

	private TestApplication app;
	private ManagedApplication ma;

	private Manager manager;

	private IInstancesMngr instancesMngr;
	private ITargetsMngr targetsMngr;
	private IApplicationMngr applicationsMngr;


	@Before
	public void initialize() throws Exception {

		this.app = new TestApplication();
		this.ma = new ManagedApplication( this.app );

		this.instancesMngr = Mockito.mock( IInstancesMngr.class );
		this.targetsMngr = Mockito.mock( ITargetsMngr.class );
		this.applicationsMngr = Mockito.mock( IApplicationMngr.class );
		this.manager = Mockito.mock( Manager.class );

		Mockito.when( this.manager.instancesMngr()).thenReturn( this.instancesMngr );
		Mockito.when( this.manager.targetsMngr()).thenReturn( this.targetsMngr );
		Mockito.when( this.manager.applicationMngr()).thenReturn( this.applicationsMngr );

		Mockito.when( this.applicationsMngr.findManagedApplicationByName( this.app.getName())).thenReturn( this.ma );
	}


	@Test
	public void testExecute_success_noDefaultTarget() throws Exception {

		String instancePath = InstanceHelpers.computeInstancePath( this.app.getTomcatVm());
		String line = "replicate /" + this.app.getTomcatVm().getName() + " as toto";
		ReplicateCommandExecution executor = buildExecutor( line );

		Mockito.verifyZeroInteractions( this.instancesMngr );
		Mockito.verifyZeroInteractions( this.targetsMngr );

		executor.execute();

		Mockito.verify( this.instancesMngr, Mockito.times( 1 )).addInstance( this.ma, null, new Instance( "toto" ));
		Mockito.verify( this.targetsMngr, Mockito.times( 1 )).findTargetId( this.app, instancePath, true );
		Mockito.verify( this.targetsMngr, Mockito.times( 0 )).findTargetId( this.app, null );
		Mockito.verify( this.targetsMngr, Mockito.times( 0 )).associateTargetWith(
				Mockito.anyString(),
				Mockito.any( Application.class ),
				Mockito.anyString());
	}


	@Test
	public void testExecute_noDefaultTarget_withAutonomicContext_success() throws Exception {

		String instancePath = InstanceHelpers.computeInstancePath( this.app.getTomcatVm());
		String line = "replicate /" + this.app.getTomcatVm().getName() + " as toto";
		ReplicateCommandExecution executor = buildExecutor( line );

		executor.setExecutionContext( new CommandExecutionContext(
				new AtomicInteger( 4 ),
				new AtomicInteger( 3 ),
				2, false,
				"marker", null
		));

		Mockito.verifyZeroInteractions( this.instancesMngr );
		Mockito.verifyZeroInteractions( this.targetsMngr );

		executor.execute();

		Mockito.verify( this.instancesMngr, Mockito.times( 1 )).addInstance( this.ma, null, new Instance( "toto" ));
		Mockito.verify( this.targetsMngr, Mockito.times( 1 )).findTargetId( this.app, instancePath, true );
		Mockito.verify( this.targetsMngr, Mockito.times( 0 )).findTargetId( this.app, null );
		Mockito.verify( this.targetsMngr, Mockito.times( 0 )).associateTargetWith(
				Mockito.anyString(),
				Mockito.any( Application.class ),
				Mockito.anyString());
	}


	@Test( expected = CommandException.class )
	public void testExecute_noDefaultTarget_withAutonomicContext_failure() throws Exception {

		String line = "replicate /" + this.app.getTomcatVm().getName() + " as toto";
		ReplicateCommandExecution executor = buildExecutor( line );

		executor.setExecutionContext( new CommandExecutionContext(
				new AtomicInteger( 6 ),
				new AtomicInteger( 3 ),
				5, true,
				null, null
		));

		Mockito.verifyZeroInteractions( this.instancesMngr );
		Mockito.verifyZeroInteractions( this.targetsMngr );

		executor.execute();
	}


	@Test
	public void testExecute_success_nonDefaultTargetIsCopied() throws Exception {

		// Simulate a non-default target ID
		String instancePath = InstanceHelpers.computeInstancePath( this.app.getTomcatVm());
		Mockito.when( this.targetsMngr.findTargetId( this.app, null )).thenReturn( null );
		Mockito.when( this.targetsMngr.findTargetId( this.app, instancePath, true )).thenReturn( "my-target-id" );

		// Replicate it
		String line = "replicate /" + this.app.getTomcatVm().getName() + " as toto";
		ReplicateCommandExecution executor = buildExecutor( line );

		Mockito.verifyZeroInteractions( this.instancesMngr );
		Mockito.verifyZeroInteractions( this.targetsMngr );

		executor.execute();

		Mockito.verify( this.instancesMngr, Mockito.times( 1 )).addInstance( this.ma, null, new Instance( "toto" ));
		Mockito.verify( this.targetsMngr, Mockito.times( 1 )).findTargetId( this.app, instancePath, true );
		Mockito.verify( this.targetsMngr, Mockito.times( 0 )).findTargetId( this.app, null );
		Mockito.verify( this.targetsMngr, Mockito.times( 1 )).associateTargetWith( "my-target-id", this.app, "/toto" );
	}


	@Test
	public void testExecute_success_defaultTargetIsNotCopied() throws Exception {

		// Simulate a default target ID
		String instancePath = InstanceHelpers.computeInstancePath( this.app.getTomcatVm());
		Mockito.when( this.targetsMngr.findTargetId( this.app, null )).thenReturn( "my-target-id" );
		Mockito.when( this.targetsMngr.findTargetId( this.app, instancePath, true )).thenReturn( null );

		// Replicate it
		String line = "replicate /" + this.app.getTomcatVm().getName() + " as toto";
		ReplicateCommandExecution executor = buildExecutor( line );

		Mockito.verifyZeroInteractions( this.instancesMngr );
		Mockito.verifyZeroInteractions( this.targetsMngr );

		executor.execute();

		Mockito.verify( this.instancesMngr, Mockito.times( 1 )).addInstance( this.ma, null, new Instance( "toto" ));
		Mockito.verify( this.targetsMngr, Mockito.times( 1 )).findTargetId( this.app, instancePath, true );
		Mockito.verify( this.targetsMngr, Mockito.times( 0 )).findTargetId( this.app, null );
		Mockito.verify( this.targetsMngr, Mockito.times( 0 )).associateTargetWith(
				Mockito.anyString(),
				Mockito.any( Application.class ),
				Mockito.anyString());
	}


	@Test( expected = CommandException.class )
	public void testExecute_failure() throws Exception {

		// Simulate an exception
		Mockito
			.doThrow( new IOException( "for test" ))
			.when( this.instancesMngr )
			.addInstance(
					Mockito.any( ManagedApplication.class ),
					Mockito.any( Instance.class ),
					Mockito.any( Instance.class ));

		// Execute the command
		String line = "replicate /" + this.app.getTomcatVm().getName() + " as toto";
		ReplicateCommandExecution executor = buildExecutor( line );

		executor.execute();
	}


	@Test( expected = CommandException.class )
	public void testExecute_failure_applicationNotFound() throws Exception {

		// The application will not be resolved
		Mockito.reset( this.applicationsMngr );

		// Execute the command
		String line = "replicate /" + this.app.getTomcatVm().getName() + " as toto";
		ReplicateCommandExecution executor = buildExecutor( line );

		executor.execute();
	}


	@Test( expected = CommandException.class )
	public void testExecute_failure_inexistingInstance() throws Exception {

		ReplicateCommandExecution executor = buildExecutor( "replicate /inexisting as toto", 1 );
		executor.execute();
	}


	private ReplicateCommandExecution buildExecutor( String command ) {
		return buildExecutor( command, 0 );
	}


	private ReplicateCommandExecution buildExecutor( String command, int validationError ) {

		CommandsParser parser = new CommandsParser( this.app, command );
		Assert.assertEquals( validationError, parser.getParsingErrors().size());
		Assert.assertEquals( 1, parser.getInstructions().size());
		Assert.assertEquals( ReplicateCommandInstruction.class, parser.getInstructions().get( 0 ).getClass());

		ReplicateCommandInstruction instr = (ReplicateCommandInstruction) parser.getInstructions().get( 0 );
		return new ReplicateCommandExecution( instr, this.manager );
	}
}
