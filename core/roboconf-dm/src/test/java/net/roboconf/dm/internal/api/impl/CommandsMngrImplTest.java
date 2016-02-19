/**
 * Copyright 2015-2016 Linagora, Université Joseph Fourier, Floralis
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
package net.roboconf.dm.internal.api.impl;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.List;

import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.model.ParsingError;
import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.dm.management.Manager;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

/**
 * @author Amadou Diarra - Université Joseph Fourier
 */
public class CommandsMngrImplTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private Application app;
	private CommandsMngrImpl cmdMngr;

	private Manager manager;


	@Before
	public void createMockObject() throws IOException {

		this.app = new TestApplication();
		this.app.setDirectory( this.folder.newFolder());
		this.manager = Mockito.mock( Manager.class );

		this.cmdMngr = new CommandsMngrImpl( this.manager );
		Assert.assertEquals( "", this.cmdMngr.getCommandInstructions( this.app, "" ));
	}


	@Test
	public void testBasics() throws IOException {

		this.cmdMngr.createOrUpdateCommand(this.app, "toto","This is a command");
		Assert.assertEquals("This is a command", this.cmdMngr.getCommandInstructions(this.app, "toto"));

		this.cmdMngr.createOrUpdateCommand( this.app, "toto", "Good command");
		Assert.assertEquals( "Good command", this.cmdMngr.getCommandInstructions(this.app, "toto"));
		this.cmdMngr.deleteCommand( this.app, "tata");
		Assert.assertEquals( "", this.cmdMngr.getCommandInstructions(this.app, "tata"));
	}


	@Test
	public void testValidate() {

		List<ParsingError> errors = this.cmdMngr.validate( this.app, "deploy and start all /tomcat-vm" );
		Assert.assertEquals( 0, errors.size());

		errors = this.cmdMngr.validate( this.app, "This is not a command..." );
		Assert.assertEquals( 2, errors.size());
	}


	@Test
	public void testListCommands() throws Exception {

		Application inexisting = new Application( "inexisting", this.app.getTemplate());
		Assert.assertEquals( 0, this.cmdMngr.listCommands( inexisting ).size());
		Assert.assertEquals( 0, this.cmdMngr.listCommands( this.app ).size());

		this.cmdMngr.createOrUpdateCommand( this.app, "toto", "Good command");
		List<String> list = this.cmdMngr.listCommands( this.app );

		Assert.assertEquals( 1, list.size());
		Assert.assertEquals( "toto", list.get( 0 ));

		this.cmdMngr.createOrUpdateCommand( this.app, "before", "Good command");
		this.cmdMngr.createOrUpdateCommand( this.app, "toto2", "Good command");
		list = this.cmdMngr.listCommands( this.app );

		Assert.assertEquals( 3, list.size());
		Assert.assertEquals( "before", list.get( 0 ));
		Assert.assertEquals( "toto", list.get( 1 ));
		Assert.assertEquals( "toto2", list.get( 2 ));

		this.cmdMngr.deleteCommand( this.app, "toto" );
		list = this.cmdMngr.listCommands( this.app );

		Assert.assertEquals( 2, list.size());
		Assert.assertEquals( "before", list.get( 0 ));
		Assert.assertEquals( "toto2", list.get( 1 ));
	}


	@Test
	public void testExecute() throws Exception {

		String line = "rename /tomcat-vm as tomcat-vm-copy";
		Assert.assertEquals( 0, this.cmdMngr.validate( this.app, line ).size());

		String cmdName = "my-command";
		this.cmdMngr.createOrUpdateCommand( this.app, cmdName, line );

		Assert.assertNotNull( InstanceHelpers.findInstanceByPath( this.app, "/tomcat-vm" ));
		Assert.assertNull( InstanceHelpers.findInstanceByPath( this.app, "/tomcat-vm-copy" ));
		this.cmdMngr.execute( this.app, cmdName );
		Assert.assertNull( InstanceHelpers.findInstanceByPath( this.app, "/tomcat-vm" ));
		Assert.assertNotNull( InstanceHelpers.findInstanceByPath( this.app, "/tomcat-vm-copy" ));
	}


	@Test( expected = NoSuchFileException.class )
	public void testExecute_noSuchCommand() throws Exception {

		this.cmdMngr.execute( this.app, "my-command" );
	}
}
