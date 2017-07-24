/**
 * Copyright 2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.dm.rest.services.internal.resources.impl;

import java.util.List;
import java.util.Timer;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.runtime.CommandHistoryItem;
import net.roboconf.dm.internal.test.TestManagerWrapper;
import net.roboconf.dm.internal.test.TestTargetResolver;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.management.api.ICommandsMngr;
import net.roboconf.dm.rest.services.internal.resources.IHistoryResource;
import net.roboconf.messaging.api.MessagingConstants;

/**
 * @author Vincent Zurczak - Linagora
 */
public class HistoryResourceTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private IHistoryResource resource;
	private Manager manager;


	@After
	public void after() {
		this.manager.stop();
	}


	@Before
	public void before() throws Exception {

		// Create the manager
		this.manager = new Manager();
		this.manager.setMessagingType( MessagingConstants.FACTORY_TEST );
		this.manager.setTargetResolver( new TestTargetResolver());
		this.manager.configurationMngr().setWorkingDirectory( this.folder.newFolder());
		this.manager.start();

		// Create the wrapper and complete configuration
		TestManagerWrapper managerWrapper = new TestManagerWrapper( this.manager );
		managerWrapper.configureMessagingForTest();
		this.manager.reconfigure();

		// Disable the messages timer for predictability
		TestUtils.getInternalField( this.manager, "timer", Timer.class).cancel();

		// Create the resource
		this.resource = new HistoryResource( this.manager );
	}


	@Test
	public void testGetCommandHistoryNumberOfPages() {

		// There is no data source
		Response resp = this.resource.getCommandHistoryNumberOfPages( null, 10 );
		Assert.assertEquals( Status.OK.getStatusCode(), resp.getStatus());
		Assert.assertEquals( "0", resp.getEntity());
	}


	@Test
	public void testGetCommandHistory() {

		// There is no data source
		List<CommandHistoryItem> items = this.resource.getCommandHistory( 10, null, 10, "start", null );
		Assert.assertEquals( 0, items.size());

		items = this.resource.getCommandHistory( -2, null, 10, "start", "asc" );
		Assert.assertEquals( 0, items.size());
	}


	@Test
	public void testGetCommandHistory_pagination() {

		Manager manager = Mockito.mock( Manager.class );
		ICommandsMngr commandsMngr = Mockito.mock( ICommandsMngr.class );
		Mockito.when( manager.commandsMngr()).thenReturn( commandsMngr );
		this.resource = new HistoryResource( manager );

		// There is no data source
		List<CommandHistoryItem> items = this.resource.getCommandHistory( 10, null, 10, "start", null );
		Assert.assertEquals( 0, items.size());
		Mockito.verify( commandsMngr ).getHistory( 90, 10, "start", null, null );

		Mockito.reset( commandsMngr );
		items = this.resource.getCommandHistory( -2, null, 10, null, "asc" );
		Assert.assertEquals( 0, items.size());
		Mockito.verify( commandsMngr ).getHistory( 0, 10, null, "asc", null );

		Mockito.reset( commandsMngr );
		items = this.resource.getCommandHistory( 1, null, 10, null, "asc" );
		Assert.assertEquals( 0, items.size());
		Mockito.verify( commandsMngr ).getHistory( 0, 10, null, "asc", null );

		Mockito.reset( commandsMngr );
		items = this.resource.getCommandHistory( 2, "app", 10, null, "asc" );
		Assert.assertEquals( 0, items.size());
		Mockito.verify( commandsMngr ).getHistory( 10, 10, null, "asc", "app" );
	}
}
