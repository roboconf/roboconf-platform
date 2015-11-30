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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;

import junit.framework.Assert;
import net.roboconf.core.commands.AbstractCommandInstruction;
import net.roboconf.core.commands.BulkCommandInstructions;
import net.roboconf.core.commands.ChangeStateCommandInstruction;
import net.roboconf.core.commands.CommandsParser;
import net.roboconf.core.commands.DefineVariableCommandInstruction;
import net.roboconf.core.commands.EmailCommandInstruction;
import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.dm.internal.test.TestManagerWrapper;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.management.exceptions.CommandException;
import net.roboconf.messaging.api.MessagingConstants;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author Vincent Zurczak - Linagora
 */
public class CommandsExecutorTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private TestApplication app;
	private Manager manager;


	@Before
	public void startManager() throws Exception {

		this.app = new TestApplication();

		// Prepare the DM
		this.manager = new Manager();
		this.manager.configurationMngr().setWorkingDirectory( this.folder.newFolder());
		this.manager.setMessagingType( MessagingConstants.TEST_FACTORY_TYPE );
		this.manager.start();

		// Reconfigure the manager
		TestManagerWrapper wrapper = new TestManagerWrapper( this.manager );
		wrapper.getNameToManagedApplication().put( this.app.getName(), new ManagedApplication( this.app ));
		wrapper.configureMessagingForTest();
		this.manager.reconfigure();

		// Disable the messages timer for predictability
		TestUtils.getInternalField( this.manager, "timer", Timer.class).cancel();
	}


	@After
	public void stopManager() {
		this.manager.stop();
	}


	@Test
	public void testAllCommands() throws Exception {

		// Before
		File f = TestUtils.findTestFile( "/commands/create-and-delete.commands" );
		CommandsExecutor executor = new CommandsExecutor( this.manager, this.app, f );

		String targetId = this.manager.targetsMngr().createTarget( "" );
		Assert.assertEquals( "1", targetId );

		List<String> instancePaths = new ArrayList<> ();
		for( Instance inst : InstanceHelpers.getAllInstances( this.app ))
			instancePaths.add( InstanceHelpers.computeInstancePath( inst ));

		int instancesCount = InstanceHelpers.getAllInstances( this.app ).size();
		Assert.assertTrue( instancePaths.contains( "/tomcat-vm" ));

		// Execute
		executor.execute();

		// After
		instancePaths.clear();;
		for( Instance inst : InstanceHelpers.getAllInstances( this.app ))
			instancePaths.add( InstanceHelpers.computeInstancePath( inst ));

		Assert.assertEquals( instancesCount + 5, instancePaths.size());
		Assert.assertTrue( instancePaths.contains( "/tomcat 1" ));
		Assert.assertTrue( instancePaths.contains( "/tomcat 1/tomcat-server" ));
		Assert.assertTrue( instancePaths.contains( "/tomcat 1/tomcat-server/hello-world" ));

		Assert.assertTrue( instancePaths.contains( "/tomcat 2" ));
		Assert.assertTrue( instancePaths.contains( "/tomcat 2/tomcat-server" ));
		Assert.assertTrue( instancePaths.contains( "/tomcat 2/tomcat-server/hello-world" ));

		Assert.assertTrue( instancePaths.contains( "/tomcat 3" ));
		Assert.assertTrue( instancePaths.contains( "/tomcat 3/my-tomcat-server" ));

		Assert.assertFalse( instancePaths.contains( "/tomcat 3/tomcat-server/hello-world" ));
		Assert.assertFalse( instancePaths.contains( "/tomcat 3/my-tomcat-server/hello-world" ));
		Assert.assertFalse( instancePaths.contains( "/tomcat-vm" ));

		Instance instance = InstanceHelpers.findInstanceByPath( this.app, "/tomcat 1" );
		Assert.assertNotNull( instance );
		Assert.assertNull( this.manager.targetsMngr().findTargetId( this.app, "/tomcat 1" ));

		instance = InstanceHelpers.findInstanceByPath( this.app, "/tomcat 2" );
		Assert.assertNotNull( instance );
		Assert.assertNull( this.manager.targetsMngr().findTargetId( this.app, "/tomcat 2" ));

		instance = InstanceHelpers.findInstanceByPath( this.app, "/tomcat 3" );
		Assert.assertNotNull( instance );
		Assert.assertEquals( targetId, this.manager.targetsMngr().findTargetId( this.app, "/tomcat 3" ));
	}


	@Test( expected = CommandException.class )
	public void testExecutionFailure1() throws Exception {

		// No command file
		CommandsExecutor executor = new CommandsExecutor( this.manager, this.app, new File( "inexisting" ));
		executor.execute();
	}


	@Test( expected = CommandException.class )
	public void testExecutionFailure2() throws Exception {

		// All the exceptions are caught, even NPE!
		CommandsExecutor executor = new CommandsExecutor( this.manager, null, new File( "inexisting" ));
		executor.execute();
	}


	@Test
	public void testFindExecutor_bulkCommand() {

		CommandsParser parser = new CommandsParser( this.app, "deploy and start all /tomcat-vm" );
		Assert.assertEquals( 0, parser.getParsingErrors().size());
		Assert.assertEquals( 1, parser.getInstructions().size());

		AbstractCommandInstruction instr = parser.getInstructions().get( 0 );
		Assert.assertEquals( BulkCommandInstructions.class, instr.getClass());

		CommandsExecutor executor = new CommandsExecutor( this.manager, null, new File( "whatever" ));
		Assert.assertEquals( BulkCommandExecution.class, executor.findExecutor( instr ).getClass());
	}


	@Test
	public void testFindExecutor_changeStateCommand() {

		CommandsParser parser = new CommandsParser( this.app, "change status of /tomcat-vm to DEPLOYED and STARTED" );
		Assert.assertEquals( 0, parser.getParsingErrors().size());
		Assert.assertEquals( 1, parser.getInstructions().size());

		AbstractCommandInstruction instr = parser.getInstructions().get( 0 );
		Assert.assertEquals( ChangeStateCommandInstruction.class, instr.getClass());

		CommandsExecutor executor = new CommandsExecutor( this.manager, null, new File( "whatever" ));
		Assert.assertEquals( ChangeStateCommandExecution.class, executor.findExecutor( instr ).getClass());
	}


	@Test
	public void testFindExecutor_defineCommand() {

		CommandsParser parser = new CommandsParser( this.app, "define key = value" );
		Assert.assertEquals( 0, parser.getParsingErrors().size());
		Assert.assertEquals( 1, parser.getInstructions().size());

		AbstractCommandInstruction instr = parser.getInstructions().get( 0 );
		Assert.assertEquals( DefineVariableCommandInstruction.class, instr.getClass());

		CommandsExecutor executor = new CommandsExecutor( this.manager, null, new File( "whatever" ));
		Assert.assertNull( executor.findExecutor( instr ));
	}


	@Test
	public void testFindExecutor_emailCommand() {

		CommandsParser parser = new CommandsParser( this.app, "email toto with this message" );
		Assert.assertEquals( 0, parser.getParsingErrors().size());
		Assert.assertEquals( 1, parser.getInstructions().size());

		AbstractCommandInstruction instr = parser.getInstructions().get( 0 );
		Assert.assertEquals( EmailCommandInstruction.class, instr.getClass());

		CommandsExecutor executor = new CommandsExecutor( this.manager, null, new File( "whatever" ));
		Assert.assertEquals( EmailCommandExecution.class, executor.findExecutor( instr ).getClass());
	}
}
