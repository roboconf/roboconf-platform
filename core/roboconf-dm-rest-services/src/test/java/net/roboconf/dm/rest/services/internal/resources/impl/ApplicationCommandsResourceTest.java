/**
 * Copyright 2014-2016 Linagora, Université Joseph Fourier, Floralis
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

import java.io.IOException;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.dm.internal.test.TestManagerWrapper;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.rest.services.internal.resources.IApplicationCommandsResource;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author Amadou Diarra - Université Joseph Fourier
 */
public class ApplicationCommandsResourceTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private Manager manager;
	private IApplicationCommandsResource resource;
	private TestApplication app;
	private ManagedApplication ma;
	private TestManagerWrapper managerWrapper;


	@Before
	public void before() throws Exception {

		// Create the manager
		this.manager = new Manager();
		this.manager.start();

		// Create the wrapper
		this.managerWrapper = new TestManagerWrapper(this.manager);

		// Create our resource
		this.resource = new ApplicationCommandsResource(this.manager);

		// Load an application
		this.app = new TestApplication();
		this.app.setDirectory(this.folder.newFolder());

		this.ma = new ManagedApplication(this.app);
		this.managerWrapper.getNameToManagedApplication().put(this.app.getName(), this.ma);
	}


	@Test
	public void commandsTest() throws IOException {

		Response resp = this.resource.getCommandInstructions(this.app.getName(), "");
		Assert.assertEquals( Status.NO_CONTENT.getStatusCode(), resp.getStatus());

		resp = this.resource.createOrUpdateCommand(this.app.getName(), "toto", "this is a command");
		Assert.assertEquals( Status.OK.getStatusCode(), resp.getStatus());
		resp = this.resource.getCommandInstructions(this.app.getName(), "toto");
		Assert.assertEquals( "this is a command", resp.getEntity());


		resp = this.resource.createOrUpdateCommand(this.app.getName(), "toto", "Good command");
		Assert.assertEquals( Status.OK.getStatusCode(), resp.getStatus());
		resp = this.resource.getCommandInstructions(this.app.getName(), "toto");
		Assert.assertEquals( "Good command",resp.getEntity());

		resp = this.resource.deleteCommand(this.app.getName(), "toto");
		Assert.assertEquals( Status.OK.getStatusCode(), resp.getStatus());
		resp = this.resource.getCommandInstructions(this.app.getName(), "toto");
		Assert.assertEquals( Status.NO_CONTENT.getStatusCode(), resp.getStatus());
		Assert.assertEquals( null, resp.getEntity());
	}
}
