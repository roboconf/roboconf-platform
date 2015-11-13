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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;
import net.roboconf.core.ErrorCode;
import net.roboconf.core.RoboconfError;
import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.dm.internal.commands.BulkCommandInstructions.ChangeStateInstruction;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.management.api.IInstancesMngr;
import net.roboconf.dm.management.exceptions.CommandException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * @author Vincent Zurczak - Linagora
 */
public class BulkCommandInstructionsTest {

	private TestApplication app;
	private ManagedApplication ma;
	private Manager manager;
	private IInstancesMngr instancesMngr;


	@Before
	public void initialize() throws Exception {

		this.app = new TestApplication();
		this.ma = new ManagedApplication( this.app );

		this.manager = Mockito.mock( Manager.class );
		this.instancesMngr = Mockito.mock( IInstancesMngr.class );
		Mockito.when( this.manager.instancesMngr()).thenReturn( this.instancesMngr );
	}


	@Test
	public void testExecute_success() throws Exception {

		// Prepare
		String instancePath = InstanceHelpers.computeInstancePath( this.app.getTomcatVm());
		BulkCommandInstructions instr1 = new BulkCommandInstructions(
				this.ma, this.manager,
				ChangeStateInstruction.DEPLOY_AND_START_ALL.toString() + " " + instancePath );

		BulkCommandInstructions instr2 = new BulkCommandInstructions(
				this.ma, this.manager,
				ChangeStateInstruction.STOP_ALL.toString() + " " + instancePath );

		BulkCommandInstructions instr3 = new BulkCommandInstructions(
				this.ma, this.manager,
				ChangeStateInstruction.UNDEPLOY_ALL.toString() + " " + instancePath );

		BulkCommandInstructions instr4 = new BulkCommandInstructions(
				this.ma, this.manager,
				ChangeStateInstruction.DELETE.toString() + " " + instancePath );

		// Deploy and start all
		Mockito.verifyZeroInteractions( this.instancesMngr );
		Assert.assertNull( instr1.validate());
		instr1.execute();
		Mockito.verify( this.instancesMngr, Mockito.times( 1 )).deployAndStartAll( this.ma, this.app.getTomcatVm());
		Mockito.verify( this.instancesMngr, Mockito.only()).deployAndStartAll( this.ma, this.app.getTomcatVm());

		// Stop all
		Mockito.reset( this.instancesMngr );
		Assert.assertNull( instr2.validate());
		instr2.execute();
		Mockito.verify( this.instancesMngr, Mockito.times( 1 )).stopAll( this.ma, this.app.getTomcatVm());
		Mockito.verify( this.instancesMngr, Mockito.only()).stopAll( this.ma, this.app.getTomcatVm());

		// Undeploy all
		Mockito.reset( this.instancesMngr );
		Assert.assertNull( instr3.validate());
		instr3.execute();
		Mockito.verify( this.instancesMngr, Mockito.times( 1 )).undeployAll( this.ma, this.app.getTomcatVm());
		Mockito.verify( this.instancesMngr, Mockito.only()).undeployAll( this.ma, this.app.getTomcatVm());

		// Delete
		Mockito.reset( this.instancesMngr );
		Assert.assertNull( instr4.validate());
		instr4.execute();

		Mockito.verify( this.instancesMngr, Mockito.times( 1 )).removeInstance( this.ma, this.app.getTomcatVm());
		Mockito.verify( this.instancesMngr, Mockito.only()).removeInstance( this.ma, this.app.getTomcatVm());
	}


	@Test( expected = CommandException.class )
	public void testExecute_inexistingInstance() throws Exception {

		String instancePath = InstanceHelpers.computeInstancePath( this.app.getTomcatVm());
		BulkCommandInstructions instr = new BulkCommandInstructions(
				this.ma, this.manager,
				ChangeStateInstruction.DEPLOY_AND_START_ALL + " " + instancePath );

		Mockito.doThrow( new IOException( "for test" )).when( this.instancesMngr ).deployAndStartAll( this.ma, this.app.getTomcatVm());
		Assert.assertNull( instr.validate());
		instr.execute();
	}


	@Test
	public void testWhich() {

		Assert.assertEquals( ChangeStateInstruction.DELETE, ChangeStateInstruction.which( "dELETE" ));
		Assert.assertEquals( ChangeStateInstruction.DEPLOY_AND_START_ALL, ChangeStateInstruction.which( "deploy and start all" ));
		Assert.assertEquals( ChangeStateInstruction.STOP_ALL, ChangeStateInstruction.which( "Stop ALL" ));
		Assert.assertEquals( ChangeStateInstruction.UNDEPLOY_ALL, ChangeStateInstruction.which( "undeploy all" ));
		Assert.assertNull( ChangeStateInstruction.which( "invalid" ));
	}


	@Test
	public void testIsBulkInstruction() {

		Assert.assertTrue( BulkCommandInstructions.isBulkInstruction( "deploy and start all /vm" ));
		Assert.assertTrue( BulkCommandInstructions.isBulkInstruction( "Stop all /vm" ));
		Assert.assertTrue( BulkCommandInstructions.isBulkInstruction( "undeploy all /vm" ));
		Assert.assertFalse( BulkCommandInstructions.isBulkInstruction( "deploy /vm" ));
		Assert.assertFalse( BulkCommandInstructions.isBulkInstruction( "" ));
	}


	@Test
	public void testValidate() {

		Map<String,ErrorCode> instructionToError = new HashMap<> ();
		instructionToError.put( "eat and drink everything", ErrorCode.EXEC_CMD_UNRECOGNIZED_INSTRUCTION );
		instructionToError.put( "deploy and start all /vm", ErrorCode.EXEC_CMD_NO_MATCHING_INSTANCE );
		instructionToError.put( "deploy and start all /tomcat-vm", null );

		for( Map.Entry<String,ErrorCode> entry : instructionToError.entrySet()) {

			BulkCommandInstructions instr = new BulkCommandInstructions( this.ma, this.manager, entry.getKey());
			RoboconfError error = instr.validate();
			ErrorCode value = error == null ? null : error.getErrorCode();

			Assert.assertEquals( entry.getKey(), entry.getValue(), value );
		}
	}
}
