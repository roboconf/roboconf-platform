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
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.dm.management.ManagedApplication;

import org.junit.Before;
import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class DefineVariableCommandInstructionTest {

	private TestApplication app;
	private ManagedApplication ma;


	@Before
	public void initialize() throws Exception {
		this.app = new TestApplication();
		this.ma = new ManagedApplication( this.app );
	}


	@Test
	public void testValidate() {

		Map<String,ErrorCode> instructionToError = new HashMap<> ();
		instructionToError.put( "define = toto", ErrorCode.EXEC_CMD_EMPTY_VARIABLE_NAME );
		instructionToError.put( "define =toto", ErrorCode.EXEC_CMD_EMPTY_VARIABLE_NAME );
		instructionToError.put( "define key=toto", null );
		instructionToError.put( "define key = toto", null );
		instructionToError.put( "define key =", null );
		instructionToError.put( "define key = ", null );
		instructionToError.put( "define key = value under /inexisting", ErrorCode.EXEC_CMD_NO_MATCHING_INSTANCE );

		for( Map.Entry<String,ErrorCode> entry : instructionToError.entrySet()) {

			DefineVariableCommandInstruction instr = new DefineVariableCommandInstruction( this.ma, entry.getKey(), new HashMap<String,String>( 0 ));
			RoboconfError error = instr.validate();
			ErrorCode value = error == null ? null : error.getErrorCode();

			Assert.assertEquals( entry.getKey(), entry.getValue(), value );
		}
	}


	@Test
	public void testExecute() throws Exception {

		// Preparation
		Map<String,String> context = new HashMap<> ();
		String instr1 = "define key = toto";
		String instr2 = "define key2 = $(key)2";
		String instr3 = "define key2 = $(key) 2";
		String instr4 = "define nano = " + DefineVariableCommandInstruction.NANO_TIME;
		String instr5 = "define milli = " + DefineVariableCommandInstruction.MILLI_TIME;
		String instr6 = "define uuid = " + DefineVariableCommandInstruction.RANDOM_UUID;
		String instr7 = "define indexWithoutInstance = oops " + DefineVariableCommandInstruction.SMART_INDEX;
		String instr8 = "define index = oops " + DefineVariableCommandInstruction.SMART_INDEX + " under /tomcat-vm";
		String instr9 = "define tt = t " + DefineVariableCommandInstruction.SMART_INDEX + " t under /tomcat-vm";

		// Assertions
		DefineVariableCommandInstruction instr = new DefineVariableCommandInstruction( this.ma, instr1, context );
		Assert.assertNull( instr.validate());
		instr.execute();
		Assert.assertEquals( "toto", context.get( "key" ));

		instr = new DefineVariableCommandInstruction( this.ma, instr2, context );
		Assert.assertNull( instr.validate());
		instr.execute();
		Assert.assertEquals( "toto2", context.get( "key2" ));

		instr = new DefineVariableCommandInstruction( this.ma, instr3, context );
		Assert.assertNull( instr.validate());
		instr.execute();
		Assert.assertEquals( "toto 2", context.get( "key2" ));

		instr = new DefineVariableCommandInstruction( this.ma, instr4, context );
		Assert.assertNull( instr.validate());
		instr.execute();
		Assert.assertNotNull( context.get( "nano" ));
		Assert.assertFalse( DefineVariableCommandInstruction.NANO_TIME.equals( context.get( "nano" )));

		instr = new DefineVariableCommandInstruction( this.ma, instr5, context );
		Assert.assertNull( instr.validate());
		instr.execute();
		Assert.assertNotNull( context.get( "milli" ));
		Assert.assertFalse( DefineVariableCommandInstruction.MILLI_TIME.equals( context.get( "nano" )));

		instr = new DefineVariableCommandInstruction( this.ma, instr6, context );
		Assert.assertNull( instr.validate());
		instr.execute();
		Assert.assertNotNull( context.get( "uuid" ));
		Assert.assertFalse( DefineVariableCommandInstruction.RANDOM_UUID.equals( context.get( "uuid" )));

		instr = new DefineVariableCommandInstruction( this.ma, instr7, context );
		Assert.assertNull( instr.validate());
		instr.execute();
		Assert.assertNotNull( context.get( "indexWithoutInstance" ));
		Assert.assertEquals( "oops 0", context.get( "indexWithoutInstance" ));

		instr = new DefineVariableCommandInstruction( this.ma, instr9, context );
		Assert.assertNull( instr.validate());
		instr.execute();
		Assert.assertNotNull( context.get( "tt" ));
		Assert.assertEquals( "t 1 t", context.get( "tt" ));

		instr = new DefineVariableCommandInstruction( this.ma, instr8, context );
		Assert.assertNull( instr.validate());
		instr.execute();
		Assert.assertNotNull( context.get( "index" ));
		Assert.assertEquals( "oops 1", context.get( "index" ));

		// Verify the smart index works as expected
		instr.execute();
		Assert.assertNotNull( context.get( "index" ));
		Assert.assertEquals( "oops 1", context.get( "index" ));

		InstanceHelpers.insertChild( this.app.getTomcatVm(), new Instance( "oops 1" ));

		instr.execute();
		Assert.assertNotNull( context.get( "index" ));
		Assert.assertEquals( "oops 2", context.get( "index" ));

		InstanceHelpers.insertChild( this.app.getTomcatVm(), new Instance( "oops 2" ));

		instr.execute();
		Assert.assertNotNull( context.get( "index" ));
		Assert.assertEquals( "oops 3", context.get( "index" ));

		instr.execute();
		Assert.assertNotNull( context.get( "index" ));
		Assert.assertEquals( "oops 3", context.get( "index" ));
	}
}
