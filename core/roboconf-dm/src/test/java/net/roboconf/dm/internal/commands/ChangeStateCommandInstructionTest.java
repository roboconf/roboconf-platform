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
import net.roboconf.core.commands.ChangeStateCommandInstruction;
import net.roboconf.core.commands.CommandsParser;
import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.management.api.IApplicationMngr;
import net.roboconf.dm.management.api.IInstancesMngr;
import net.roboconf.dm.management.exceptions.CommandException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ChangeStateCommandInstructionTest {

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
	public void testExecute_success() throws Exception {

		String txt = "Change status of /tomcat-vm/tomcat-server to DEPLOYED and STARTED";
		ChangeStateCommandExecution executor = buildExecutor( txt );

		Mockito.verifyZeroInteractions( this.instancesMngr );
		executor.execute();
		Mockito.verify( this.instancesMngr, Mockito.times( 1 )).changeInstanceState( this.ma, this.app.getTomcat(), InstanceStatus.DEPLOYED_STARTED );
	}


	@Test( expected = CommandException.class )
	public void testExecute_failure() throws Exception {

		String txt = "Change status of /tomcat-vm/tomcat-server to DEPLOYED and STARTED";
		ChangeStateCommandExecution executor = buildExecutor( txt );

		Mockito.doThrow( new IOException( "For test" )).when( this.instancesMngr ).changeInstanceState( this.ma, this.app.getTomcat(), InstanceStatus.DEPLOYED_STARTED );
		Mockito.verifyZeroInteractions( this.instancesMngr );
		executor.execute();
	}


	@Test( expected = CommandException.class )
	public void testExecute_failure_inexistingInstance() throws Exception {

		ChangeStateCommandExecution executor = buildExecutor( "Change status of /inexisting to DEPLOYED and STARTED", 1 );
		executor.execute();
	}


	private ChangeStateCommandExecution buildExecutor( String command ) {
		return buildExecutor( command, 0 );
	}


	private ChangeStateCommandExecution buildExecutor( String command, int validationError ) {

		CommandsParser parser = new CommandsParser( this.app, command );
		Assert.assertEquals( validationError, parser.getParsingErrors().size());
		Assert.assertEquals( 1, parser.getInstructions().size());
		Assert.assertEquals( ChangeStateCommandInstruction.class, parser.getInstructions().get( 0 ).getClass());

		ChangeStateCommandInstruction instr = (ChangeStateCommandInstruction) parser.getInstructions().get( 0 );
		return new ChangeStateCommandExecution( instr, this.manager );
	}
}
