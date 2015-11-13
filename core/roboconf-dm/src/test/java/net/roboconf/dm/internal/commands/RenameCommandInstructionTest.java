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
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.dm.management.ManagedApplication;

import org.junit.Before;
import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class RenameCommandInstructionTest {

	private TestApplication app;
	private ManagedApplication ma;


	@Before
	public void initialize() throws Exception {

		this.app = new TestApplication();
		this.ma = new ManagedApplication( this.app );
	}


	@Test
	public void testExecute_success() throws Exception {

		String instancePath = InstanceHelpers.computeInstancePath( this.app.getTomcatVm());
		RenameCommandInstruction instr = new RenameCommandInstruction( this.ma, "rename " + instancePath + " as toto" );

		Assert.assertEquals( "tomcat-vm", this.app.getTomcatVm().getName());
		Assert.assertNull( instr.validate());
		instr.execute();
		Assert.assertEquals( "toto", this.app.getTomcatVm().getName());
	}


	@Test
	public void testValidate() {

		// Basic checks
		Instance tomcatSibling = new Instance( "tomcat-copy" ).component( this.app.getTomcat().getComponent());
		InstanceHelpers.insertChild( this.app.getTomcatVm(), tomcatSibling );

		Map<String,ErrorCode> instructionToError = new HashMap<> ();
		instructionToError.put( "rename /tomcat-vm as", ErrorCode.EXEC_CMD_MISSING_INSTANCE_NAME );
		instructionToError.put( "rename /tomcat-vm as !boo!", ErrorCode.EXEC_CMD_INVALID_INSTANCE_NAME );
		instructionToError.put( "rename /vm as toto", ErrorCode.EXEC_CMD_NO_MATCHING_INSTANCE );
		instructionToError.put( "rename /tomcat-vm as mysql-vm", ErrorCode.EXEC_CMD_CONFLICTING_INSTANCE_NAME );
		instructionToError.put( "rename /tomcat-vm/tomcat-copy as tomcat-server", ErrorCode.EXEC_CMD_CONFLICTING_INSTANCE_NAME );
		instructionToError.put( "rename /tomcat-vm/tomcat-copy as tomcat-server-copy", null );
		instructionToError.put( "rename /tomcat-vm as vm-for-tomcat", null );

		for( Map.Entry<String,ErrorCode> entry : instructionToError.entrySet()) {

			RenameCommandInstruction instr = new RenameCommandInstruction( this.ma, entry.getKey());
			RoboconfError error = instr.validate();
			ErrorCode value = error == null ? null : error.getErrorCode();

			Assert.assertEquals( entry.getKey(), entry.getValue(), value );
		}

		// Specific check
		String instructionText = "rename /tomcat-vm/tomcat-copy as tomcat-server-2";
		RenameCommandInstruction instr = new RenameCommandInstruction( this.ma, instructionText );
		Assert.assertNull( instr.validate());

		this.app.getTomcatVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		instr = new RenameCommandInstruction( this.ma, instructionText );
		Assert.assertEquals( ErrorCode.EXEC_CMD_APPLIABLE_TO_NOT_DEPLOYED_ONLY, instr.validate().getErrorCode());
	}
}
