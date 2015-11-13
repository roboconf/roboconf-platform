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

import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;
import net.roboconf.core.ErrorCode;
import net.roboconf.core.RoboconfError;
import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.management.exceptions.CommandException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author Vincent Zurczak - Linagora
 */
public class AssociateTargetCommandInstructionTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private TestApplication app;
	private Manager manager;
	private ManagedApplication ma;


	@Before
	public void initialize() throws Exception {

		this.app = new TestApplication();
		this.ma = new ManagedApplication( this.app );

		this.manager = new Manager();
		this.manager.configurationMngr().setWorkingDirectory( this.folder.newFolder());
	}


	@Test
	public void testExecute_success() throws Exception {

		String targetId = this.manager.targetsMngr().createTarget( "" );
		String instancePath = InstanceHelpers.computeInstancePath( this.app.getTomcatVm());
		AssociateTargetCommandInstruction instr =
				new AssociateTargetCommandInstruction( this.ma, this.manager, "associate " + instancePath + " with " + targetId );

		Assert.assertNull( this.manager.targetsMngr().findTargetId( this.app, instancePath ));
		Assert.assertNull( instr.validate());
		instr.execute();
		Assert.assertEquals( targetId, this.manager.targetsMngr().findTargetId( this.app, instancePath ));
	}


	@Test( expected = CommandException.class )
	public void testExecute_inexistingTarget() throws Exception {

		String instancePath = InstanceHelpers.computeInstancePath( this.app.getTomcatVm());
		AssociateTargetCommandInstruction instr =
				new AssociateTargetCommandInstruction( this.ma, this.manager, "associate " + instancePath + " with 80" );

		Assert.assertNull( this.manager.targetsMngr().findTargetId( this.app, instancePath ));
		Assert.assertNotNull( instr.validate());
		instr.execute();
	}


	@Test
	public void testValidate() {

		Map<String,ErrorCode> instructionToError = new HashMap<> ();
		instructionToError.put( "associate /tomcat-vm with", ErrorCode.EXEC_CMD_INVALID_TARGET_ID );
		instructionToError.put( "associate /vm with 2", ErrorCode.EXEC_CMD_NO_MATCHING_INSTANCE );
		instructionToError.put( "associate /tomcat-vm/tomcat-server with 2", ErrorCode.EXEC_CMD_NOT_A_SCOPED_INSTANCE );
		instructionToError.put( "associate /tomcat-vm with 2", ErrorCode.EXEC_CMD_TARGET_WAS_NOT_FOUND );

		for( Map.Entry<String,ErrorCode> entry : instructionToError.entrySet()) {

			AssociateTargetCommandInstruction instr = new AssociateTargetCommandInstruction( this.ma, this.manager, entry.getKey());
			RoboconfError error = instr.validate();
			ErrorCode value = error == null ? null : error.getErrorCode();

			Assert.assertEquals( entry.getKey(), entry.getValue(), value );
		}
	}
}
