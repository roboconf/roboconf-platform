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

package net.roboconf.core.commands;

import java.io.File;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import net.roboconf.core.commands.BulkCommandInstructions.ChangeStateInstruction;
import net.roboconf.core.errors.ErrorCode;
import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.model.ParsingError;

/**
 * @author Vincent Zurczak - Linagora
 */
public class BulkCommandInstructionsTest {

	private TestApplication app;
	private Context context;


	@Before
	public void initialize() throws Exception {
		this.app = new TestApplication();
		this.context = new Context( this.app, new File( "whatever" ));
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
		Assert.assertTrue( BulkCommandInstructions.isBulkInstruction( "delete /vm" ));

		Assert.assertTrue( BulkCommandInstructions.isBulkInstruction( "deploy and start all instances of toto" ));
		Assert.assertTrue( BulkCommandInstructions.isBulkInstruction( "Stop all instances of toto" ));
		Assert.assertTrue( BulkCommandInstructions.isBulkInstruction( "undeploy all instances of toto" ));
		Assert.assertTrue( BulkCommandInstructions.isBulkInstruction( "delete all instances of toto" ));

		Assert.assertFalse( BulkCommandInstructions.isBulkInstruction( "deploy /vm" ));
		Assert.assertFalse( BulkCommandInstructions.isBulkInstruction( "" ));
		Assert.assertFalse( BulkCommandInstructions.isBulkInstruction( "deploy all instances of toto" ));
		Assert.assertFalse( BulkCommandInstructions.isBulkInstruction( "deploy and start all toto" ));
	}


	@Test
	public void testValidate_1() {

		String line = "eat and drink everything";
		BulkCommandInstructions instr = new BulkCommandInstructions( this.context, line, 1 );
		List<ParsingError> errors = instr.validate();

		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.CMD_INVALID_SYNTAX, errors.get( 0 ).getErrorCode());
	}


	@Test
	public void testValidate_2() {

		String line = "deploy and start all /vm";
		BulkCommandInstructions instr = new BulkCommandInstructions( this.context, line, 1 );
		Assert.assertEquals( "/vm", instr.getInstancePath());
		Assert.assertNull( instr.getComponentName());

		List<ParsingError> errors = instr.validate();
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.CMD_NO_MATCHING_INSTANCE, errors.get( 0 ).getErrorCode());
	}


	@Test
	public void testValidate_3() {

		String line = "deploy and start all /tomcat-vm";
		BulkCommandInstructions instr = new BulkCommandInstructions( this.context, line, 1 );
		Assert.assertEquals( "/tomcat-vm", instr.getInstancePath());
		Assert.assertNull( instr.getComponentName());

		List<ParsingError> errors = instr.validate();
		Assert.assertEquals( 0, errors.size());
	}


	@Test
	public void testValidate_4() {

		String line = "eat and drink /all";
		BulkCommandInstructions instr = new BulkCommandInstructions( this.context, line, 1 );
		Assert.assertEquals( "/all", instr.getInstancePath());
		Assert.assertNull( instr.getComponentName());

		List<ParsingError> errors = instr.validate();
		Assert.assertEquals( 2, errors.size());
		Assert.assertEquals( ErrorCode.CMD_UNRECOGNIZED_INSTRUCTION, errors.get( 0 ).getErrorCode());
		Assert.assertEquals( ErrorCode.CMD_NO_MATCHING_INSTANCE, errors.get( 1 ).getErrorCode());
	}


	@Test
	public void testValidate_5() {

		String[] lines = {
				"deploy and start all instances of vm",
				"stop all instances of vm",
				"undeploy all instances of vm",
				"delete all instances of vm"
		};

		for( String line : lines ) {
			BulkCommandInstructions instr = new BulkCommandInstructions( this.context, line, 1 );

			Assert.assertEquals( line, "vm", instr.getComponentName());
			Assert.assertNull( line, instr.getInstancePath());

			List<ParsingError> errors = instr.validate();
			Assert.assertEquals( line, 0, errors.size());
		}
	}


	@Test
	public void testValidate_6() {

		String line = "deploy and start all instances of whatever";
		BulkCommandInstructions instr = new BulkCommandInstructions( this.context, line, 1 );

		Assert.assertEquals( "whatever", instr.getComponentName());
		Assert.assertNull( instr.getInstancePath());

		List<ParsingError> errors = instr.validate();
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.CMD_INEXISTING_COMPONENT, errors.get( 0 ).getErrorCode());
	}


	@Test
	public void testUpdateContext_instancePath() {

		String line = "delete /tomcat-vm";
		BulkCommandInstructions instr = new BulkCommandInstructions( this.context, line, 1 );

		Assert.assertEquals( "/tomcat-vm", instr.getInstancePath());
		Assert.assertNull( instr.getComponentName());

		Assert.assertEquals( 0, instr.validate().size());
		Assert.assertTrue( this.context.instancePathToComponentName.containsKey( "/tomcat-vm" ));
		Assert.assertTrue( this.context.instancePathToComponentName.containsKey( "/tomcat-vm/tomcat-server" ));
		Assert.assertTrue( this.context.instancePathToComponentName.containsKey( "/tomcat-vm/tomcat-server/hello-world" ));

		int instancesCount = this.context.instancePathToComponentName.size();
		instr.updateContext();
		Assert.assertEquals( instancesCount - 3, this.context.instancePathToComponentName.size());

		Assert.assertFalse( this.context.instancePathToComponentName.containsKey( "/tomcat-vm" ));
		Assert.assertFalse( this.context.instancePathToComponentName.containsKey( "/tomcat-vm/tomcat-server" ));
		Assert.assertFalse( this.context.instancePathToComponentName.containsKey( "/tomcat-vm/tomcat-server/hello-world" ));
	}


	@Test
	public void testUpdateContext_componentName() {

		// Delete all the instance of "war"
		String line = "delete all instances of war";
		BulkCommandInstructions instr = new BulkCommandInstructions( this.context, line, 1 );

		Assert.assertEquals( "war", instr.getComponentName());
		Assert.assertNull( instr.getInstancePath());

		Assert.assertEquals( 0, instr.validate().size());
		Assert.assertTrue( this.context.instancePathToComponentName.containsKey( "/tomcat-vm" ));
		Assert.assertTrue( this.context.instancePathToComponentName.containsKey( "/tomcat-vm/tomcat-server" ));
		Assert.assertTrue( this.context.instancePathToComponentName.containsKey( "/tomcat-vm/tomcat-server/hello-world" ));

		int instancesCount = this.context.instancePathToComponentName.size();
		instr.updateContext();
		Assert.assertEquals( instancesCount - 1, this.context.instancePathToComponentName.size());

		Assert.assertTrue( this.context.instancePathToComponentName.containsKey( "/tomcat-vm" ));
		Assert.assertTrue( this.context.instancePathToComponentName.containsKey( "/tomcat-vm/tomcat-server" ));
		Assert.assertFalse( this.context.instancePathToComponentName.containsKey( "/tomcat-vm/tomcat-server/hello-world" ));

		// Delete all the "vm"
		line = "delete all instances of vm";
		instr = new BulkCommandInstructions( this.context, line, 1 );

		Assert.assertEquals( "vm", instr.getComponentName());
		Assert.assertNull( instr.getInstancePath());

		Assert.assertEquals( 0, instr.validate().size());
		Assert.assertTrue( this.context.instancePathToComponentName.containsKey( "/tomcat-vm" ));
		Assert.assertTrue( this.context.instancePathToComponentName.containsKey( "/tomcat-vm/tomcat-server" ));
		Assert.assertFalse( this.context.instancePathToComponentName.containsKey( "/tomcat-vm/tomcat-server/hello-world" ));

		instr.updateContext();
		Assert.assertEquals( 0, this.context.instancePathToComponentName.size());
	}
}
