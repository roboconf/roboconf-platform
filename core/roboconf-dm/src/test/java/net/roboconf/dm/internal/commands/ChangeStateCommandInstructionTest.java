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
import net.roboconf.core.model.beans.Instance.InstanceStatus;
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
public class ChangeStateCommandInstructionTest {

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
	public void testValidate() {

		Map<String,ErrorCode> instructionToError = new HashMap<> ();
		instructionToError.put( "Change status of /tomcat-vm/tomcat-server", ErrorCode.EXEC_CMD_INVALID_INSTANCE_STATUS );
		instructionToError.put( "Change status of /tomcat-vm/tomcat-server to started", ErrorCode.EXEC_CMD_INVALID_INSTANCE_STATUS );
		instructionToError.put( "Change status of /tomcat-vm/tomcat-server to DEPLOYING", ErrorCode.EXEC_CMD_INSTABLE_INSTANCE_STATUS );
		instructionToError.put( "Change status of /tomcat-vm/invalid to DEPLOYED_AND_STARTED", ErrorCode.EXEC_CMD_NO_MATCHING_INSTANCE );
		instructionToError.put( "Change status of /tomcat-vm/tomcat-server to DEPLOYED_STARTED", null );
		instructionToError.put( "Change status of /tomcat-vm/tomcat-server to DEPLOYED STARTED", null );
		instructionToError.put( "Change status of /tomcat-vm/tomcat-server to DEPLOYED_AND_STARTED", null );
		instructionToError.put( "Change status of /tomcat-vm/tomcat-server to DEPLOYED and STARTED", null );

		for( Map.Entry<String,ErrorCode> entry : instructionToError.entrySet()) {

			ChangeStateCommandInstruction instr = new ChangeStateCommandInstruction( this.ma, this.manager, entry.getKey());
			RoboconfError error = instr.validate();
			ErrorCode value = error == null ? null : error.getErrorCode();

			Assert.assertEquals( entry.getKey(), entry.getValue(), value );
		}
	}


	@Test
	public void testExecute_success() throws Exception {

		String txt = "Change status of /tomcat-vm/tomcat-server to DEPLOYED and STARTED";
		ChangeStateCommandInstruction instr = new ChangeStateCommandInstruction( this.ma, this.manager, txt );
		Assert.assertNull( instr.validate());

		Mockito.verifyZeroInteractions( this.instancesMngr );
		instr.execute();
		Mockito.verify( this.instancesMngr, Mockito.times( 1 )).changeInstanceState( this.ma, this.app.getTomcat(), InstanceStatus.DEPLOYED_STARTED );
	}


	@Test( expected = CommandException.class )
	public void testExecute_failure() throws Exception {

		String txt = "Change status of /tomcat-vm/tomcat-server to DEPLOYED and STARTED";
		ChangeStateCommandInstruction instr = new ChangeStateCommandInstruction( this.ma, this.manager, txt );
		Assert.assertNull( instr.validate());

		Mockito.doThrow( new IOException( "For test" )).when( this.instancesMngr ).changeInstanceState( this.ma, this.app.getTomcat(), InstanceStatus.DEPLOYED_STARTED );
		Mockito.verifyZeroInteractions( this.instancesMngr );
		instr.execute();
	}
}
