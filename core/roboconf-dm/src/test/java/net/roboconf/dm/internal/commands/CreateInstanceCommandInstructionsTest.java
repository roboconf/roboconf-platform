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
import net.roboconf.core.model.beans.Instance;
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
public class CreateInstanceCommandInstructionsTest {

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
	public void testExecute_rootInstance_success() throws Exception {

		String txt = "create vm as tomcat-vm-2";
		CreateInstanceCommandInstruction instr = new CreateInstanceCommandInstruction( this.ma, this.manager, txt );

		Assert.assertNull( instr.validate());
		instr.execute();

		Mockito.verify( this.instancesMngr, Mockito.times( 1 )).addInstance( this.ma, null, new Instance( "tomcat-vm-2" ));
	}


	@Test
	public void testExecute_childInstance_success() throws Exception {

		String txt = "create tomcat as tomcat-server-2 under /tomcat-vm";
		CreateInstanceCommandInstruction instr = new CreateInstanceCommandInstruction( this.ma, this.manager, txt );

		Assert.assertNull( instr.validate());
		instr.execute();

		Mockito.verify( this.instancesMngr, Mockito.times( 1 )).addInstance(
				this.ma,
				this.app.getTomcatVm(),
				new Instance( "tomcat-server-2" ));
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

		// Prepare the arguments
		String txt = "create tomcat as tomcat-server-2";
		CreateInstanceCommandInstruction instr = new CreateInstanceCommandInstruction( this.ma, this.manager, txt );

		// Execute the command
		Assert.assertNotNull( instr.validate());
		instr.execute();
	}


	@Test
	public void testValidate() {

		Map<String,ErrorCode> instructionToError = new HashMap<> ();
		instructionToError.put( "create tomcat as", ErrorCode.EXEC_CMD_MISSING_INSTANCE_NAME );
		instructionToError.put( "create tomcat as ", ErrorCode.EXEC_CMD_MISSING_INSTANCE_NAME );
		instructionToError.put( "create tomcat as !boo!", ErrorCode.EXEC_CMD_INVALID_INSTANCE_NAME );
		instructionToError.put( "create as toto", ErrorCode.EXEC_CMD_MISSING_COMPONENT_NAME );
		instructionToError.put( "create     as toto", ErrorCode.EXEC_CMD_MISSING_COMPONENT_NAME );
		instructionToError.put( "create invalid as toto", ErrorCode.EXEC_CMD_INEXISTING_COMPONENT );
		instructionToError.put( "create tomcat as toto", ErrorCode.EXEC_CMD_MISSING_PARENT_INSTANCE );
		instructionToError.put( "create vm as toto", null );
		instructionToError.put( "create tomcat as instance name with spaces under /tomcat-vm", null );

		for( Map.Entry<String,ErrorCode> entry : instructionToError.entrySet()) {

			CreateInstanceCommandInstruction instr = new CreateInstanceCommandInstruction( this.ma, this.manager, entry.getKey());
			RoboconfError error = instr.validate();
			ErrorCode value = error == null ? null : error.getErrorCode();

			Assert.assertEquals( entry.getKey(), entry.getValue(), value );
		}
	}
}
