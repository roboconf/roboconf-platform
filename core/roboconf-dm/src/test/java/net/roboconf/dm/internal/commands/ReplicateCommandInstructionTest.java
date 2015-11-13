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
import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.management.api.IInstancesMngr;
import net.roboconf.dm.management.api.ITargetsMngr;
import net.roboconf.dm.management.exceptions.CommandException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ReplicateCommandInstructionTest {

	private TestApplication app;
	private ManagedApplication ma;

	private Manager manager;
	private IInstancesMngr instancesMngr;
	private ITargetsMngr targetsMngr;


	@Before
	public void initialize() throws Exception {

		this.app = new TestApplication();
		this.ma = new ManagedApplication( this.app );

		this.instancesMngr = Mockito.mock( IInstancesMngr.class );
		this.targetsMngr = Mockito.mock( ITargetsMngr.class );
		this.manager = Mockito.mock( Manager.class );

		Mockito.when( this.manager.instancesMngr()).thenReturn( this.instancesMngr );
		Mockito.when( this.manager.targetsMngr()).thenReturn( this.targetsMngr );
	}


	@Test
	public void testExecute_success_noDefaultTarget() throws Exception {

		String instancePath = InstanceHelpers.computeInstancePath( this.app.getTomcatVm());
		ReplicateCommandInstruction instr =
				new ReplicateCommandInstruction( this.ma, this.manager, "replicate " + this.app.getTomcatVm().getName() + " as toto" );

		Mockito.verifyZeroInteractions( this.instancesMngr );
		Mockito.verifyZeroInteractions( this.targetsMngr );

		Assert.assertNull( instr.validate());
		instr.execute();

		Mockito.verify( this.instancesMngr, Mockito.times( 1 )).addInstance( this.ma, null, new Instance( "toto" ));
		Mockito.verify( this.targetsMngr, Mockito.times( 1 )).findTargetId( this.app, instancePath );
		Mockito.verify( this.targetsMngr, Mockito.times( 1 )).findTargetId( this.app, null );
		Mockito.verify( this.targetsMngr, Mockito.times( 0 )).associateTargetWithScopedInstance(
				Mockito.anyString(),
				Mockito.any( Application.class ),
				Mockito.anyString());
	}


	@Test
	public void testExecute_success_nonDefaultTargetIsCopied() throws Exception {

		// Simulate a non-default target ID
		String instancePath = InstanceHelpers.computeInstancePath( this.app.getTomcatVm());
		Mockito.when( this.targetsMngr.findTargetId( this.app, null )).thenReturn( null );
		Mockito.when( this.targetsMngr.findTargetId( this.app, instancePath )).thenReturn( "my-target-id" );

		// Replicate it
		ReplicateCommandInstruction instr =
				new ReplicateCommandInstruction( this.ma, this.manager, "replicate /" + this.app.getTomcatVm().getName() + " as toto" );

		Mockito.verifyZeroInteractions( this.instancesMngr );
		Mockito.verifyZeroInteractions( this.targetsMngr );

		Assert.assertNull( instr.validate());
		instr.execute();

		Mockito.verify( this.instancesMngr, Mockito.times( 1 )).addInstance( this.ma, null, new Instance( "toto" ));
		Mockito.verify( this.targetsMngr, Mockito.times( 1 )).findTargetId( this.app, instancePath );
		Mockito.verify( this.targetsMngr, Mockito.times( 1 )).findTargetId( this.app, null );
		Mockito.verify( this.targetsMngr, Mockito.times( 1 )).associateTargetWithScopedInstance( "my-target-id", this.app, "/toto" );
	}


	@Test
	public void testExecute_success_defaultTargetIsNotCopied() throws Exception {

		// Simulate a default target ID
		String instancePath = InstanceHelpers.computeInstancePath( this.app.getTomcatVm());
		Mockito.when( this.targetsMngr.findTargetId( this.app, null )).thenReturn( "my-target-id" );
		Mockito.when( this.targetsMngr.findTargetId( this.app, instancePath )).thenReturn( "my-target-id" );

		// Replicate it
		ReplicateCommandInstruction instr =
				new ReplicateCommandInstruction( this.ma, this.manager, "replicate " + this.app.getTomcatVm().getName() + " as toto" );

		Mockito.verifyZeroInteractions( this.instancesMngr );
		Mockito.verifyZeroInteractions( this.targetsMngr );

		Assert.assertNull( instr.validate());
		instr.execute();

		Mockito.verify( this.instancesMngr, Mockito.times( 1 )).addInstance( this.ma, null, new Instance( "toto" ));
		Mockito.verify( this.targetsMngr, Mockito.times( 1 )).findTargetId( this.app, instancePath );
		Mockito.verify( this.targetsMngr, Mockito.times( 1 )).findTargetId( this.app, null );
		Mockito.verify( this.targetsMngr, Mockito.times( 0 )).associateTargetWithScopedInstance(
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
		ReplicateCommandInstruction instr =
				new ReplicateCommandInstruction( this.ma, this.manager, "replicate " + this.app.getTomcatVm().getName() + " as toto" );

		Assert.assertNull( instr.validate());
		instr.execute();
	}


	@Test
	public void testValidate() {

		Map<String,ErrorCode> instructionToError = new HashMap<> ();
		instructionToError.put( "replicate invalid as toto", ErrorCode.EXEC_CMD_NO_MATCHING_INSTANCE );
		instructionToError.put( "replicate /tomcat-vm as", ErrorCode.EXEC_CMD_MISSING_INSTANCE_NAME );
		instructionToError.put( "replicate /tomcat-vm as !boo!", ErrorCode.EXEC_CMD_INVALID_INSTANCE_NAME );
		instructionToError.put( "replicate /tomcat-vm/tomcat-server as toto", ErrorCode.EXEC_CMD_NOT_A_ROOT_INSTANCE );
		instructionToError.put( "replicate /tomcat-vm as toto", null );

		for( Map.Entry<String,ErrorCode> entry : instructionToError.entrySet()) {

			ReplicateCommandInstruction instr = new ReplicateCommandInstruction( this.ma, this.manager, entry.getKey());
			RoboconfError error = instr.validate();
			ErrorCode value = error == null ? null : error.getErrorCode();

			Assert.assertEquals( entry.getKey(), entry.getValue(), value );
		}
	}
}
