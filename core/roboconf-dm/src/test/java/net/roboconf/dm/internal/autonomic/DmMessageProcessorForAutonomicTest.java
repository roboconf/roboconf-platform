/**
 * Copyright 2014-2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.dm.internal.autonomic;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.dm.internal.environment.messaging.DmMessageProcessor;
import net.roboconf.dm.internal.test.TestManagerWrapper;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.management.api.IAutonomicMngr;
import net.roboconf.messaging.api.messages.from_agent_to_dm.MsgNotifAutonomic;

/**
 * @author Vincent Zurczak - Linagora
 */
public class DmMessageProcessorForAutonomicTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private Manager manager;
	private IAutonomicMngr autonomicMngr;
	private TestApplication app;



	@Before
	public void prepareManager() throws Exception {

		// Prepare the manager
		this.manager = new Manager();
		this.manager.configurationMngr().setWorkingDirectory( this.folder.newFolder());

		// Prepare the mocks
		this.autonomicMngr = Mockito.mock( IAutonomicMngr.class );
		this.manager = Mockito.spy( this.manager );
		Mockito.when( this.manager.autonomicMngr()).thenReturn( this.autonomicMngr );

		// Prepare an application all the tests can use
		this.app = new TestApplication();
		this.app.setDirectory( this.folder.newFolder());
	}


	@Test
	public void testAutonomicMessage_appIsRegistered() throws Exception {

		// Register the application
		ManagedApplication ma = new ManagedApplication( this.app );
		TestManagerWrapper wrapper = new TestManagerWrapper( this.manager );
		wrapper.addManagedApplication( ma );

		// Simulate a first message to delete an instance
		MsgNotifAutonomic msg = new MsgNotifAutonomic( this.app.getName(), this.app.getTomcatVm().getName(), "whatever", "we do not care" );
		DmMessageProcessor processor = new DmMessageProcessor( this.manager );
		processor.processMessage( msg );

		Mockito.verify( this.autonomicMngr, Mockito.times( 1 )).handleEvent( ma, msg );
	}


	@Test
	public void testAutonomicMessage_appIsNotRegistered() throws Exception {

		MsgNotifAutonomic msg = new MsgNotifAutonomic( this.app.getName(), this.app.getTomcatVm().getName(), "whatever", "we do not care" );
		DmMessageProcessor processor = new DmMessageProcessor( this.manager );
		processor.processMessage( msg );

		Mockito.verifyZeroInteractions( this.autonomicMngr );
	}


	@Test
	public void testAutonomicMessage_invalidAgentIdentifier() throws Exception {

		ManagedApplication ma = new ManagedApplication( this.app );
		TestManagerWrapper wrapper = new TestManagerWrapper( this.manager );
		wrapper.addManagedApplication( ma );

		MsgNotifAutonomic msg = new MsgNotifAutonomic( this.app.getName(), "/invalid/id", "whatever", "we do not care" );
		DmMessageProcessor processor = new DmMessageProcessor( this.manager );
		processor.processMessage( msg );

		Mockito.verifyZeroInteractions( this.autonomicMngr );
	}
}
