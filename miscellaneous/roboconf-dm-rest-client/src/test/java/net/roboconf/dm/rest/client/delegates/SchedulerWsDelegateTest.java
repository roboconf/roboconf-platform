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

package net.roboconf.dm.rest.client.delegates;

import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.Timer;

import javax.ws.rs.core.UriBuilder;

import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.sun.jersey.api.container.grizzly2.GrizzlyServerFactory;

import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.runtime.ScheduledJob;
import net.roboconf.dm.internal.test.TestManagerWrapper;
import net.roboconf.dm.internal.test.TestTargetResolver;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.rest.client.WsClient;
import net.roboconf.dm.rest.client.exceptions.SchedulerWsException;
import net.roboconf.dm.rest.services.internal.RestApplication;
import net.roboconf.dm.scheduler.internal.RoboconfScheduler;
import net.roboconf.messaging.api.MessagingConstants;

/**
 * @author Vincent Zurczak - Linagora
 */
public class SchedulerWsDelegateTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();
	private static final String REST_URI = "http://localhost:8090";

	private Manager manager;
	private RoboconfScheduler scheduler;
	private TestManagerWrapper managerWrapper;

	private TestApplication app;
	private ManagedApplication ma;
	private File targetFile;

	private WsClient client;
	private HttpServer httpServer;


	@After
	public void after() throws Exception {

		this.manager.stop();
		if( this.httpServer != null )
			this.httpServer.stop();

		if( this.client != null )
			this.client.destroy();

		if( this.scheduler != null )
			this.scheduler.stop();
	}


	@Before
	public void before() throws Exception {
		this.targetFile = this.folder.newFile();

		// Create the manager
		this.manager = new Manager();
		this.manager.setMessagingType(MessagingConstants.FACTORY_TEST);
		this.manager.setTargetResolver( new TestTargetResolver());
		this.manager.configurationMngr().setWorkingDirectory( this.folder.newFolder());
		this.manager.start();

		// Create the wrapper and complete configuration
		this.managerWrapper = new TestManagerWrapper( this.manager );
		this.managerWrapper.configureMessagingForTest();
		this.manager.reconfigure();

		// Disable the messages timer for predictability
		TestUtils.getInternalField( this.manager, "timer", Timer.class).cancel();

		URI uri = UriBuilder.fromUri( REST_URI ).build();
		RestApplication restApp = new RestApplication( this.manager );
		this.httpServer = GrizzlyServerFactory.createHttpServer( uri, restApp );

		// Load an application
		this.app = new TestApplication();
		this.app.setDirectory( this.folder.newFolder());

		this.ma = new ManagedApplication( this.app );
		this.managerWrapper.addManagedApplication( this.ma );

		// Create a command
		this.manager.commandsMngr().createOrUpdateCommand( this.app, "write 1", "Write this into " + this.targetFile.getAbsolutePath());
		this.manager.commandsMngr().createOrUpdateCommand( this.app, "write 2", "Write this into " + this.targetFile.getAbsolutePath());

		// Initialize the scheduler
		this.scheduler = new RoboconfScheduler();
		this.scheduler.setManager( this.manager );
		this.scheduler.start();
		restApp.setScheduler( this.scheduler );

		this.client = new WsClient( REST_URI );
	}


	@Test
	public void testListCreateAndDelete() throws Exception {

		// Nothing at the beginning
		Assert.assertEquals( 0, this.client.getSchedulerDelegate().listAllJobs( null, null ).size());

		// Create a job
		String jobId1 = this.client.getSchedulerDelegate().createOrUpdateJob( null, "my job", this.app.getName(), "write 1", "0 0 0 ? 1 *" );

		Assert.assertNotNull( jobId1 );
		Assert.assertEquals( 1, this.client.getSchedulerDelegate().listAllJobs( null, null ).size());

		// Create another job
		String jobId2 = this.client.getSchedulerDelegate().createOrUpdateJob( null, "my job", this.app.getName(), "write 2", "0 0 0 ? 1 *" );

		Assert.assertNotNull( jobId2 );
		Assert.assertNotEquals( jobId1, jobId2 );
		Assert.assertEquals( 2, this.client.getSchedulerDelegate().listAllJobs( null, null ).size());
		Assert.assertEquals( 2, this.client.getSchedulerDelegate().listAllJobs( this.app.getName(), null ).size());

		// Test filtering
		List<ScheduledJob> receivedJobs = this.client.getSchedulerDelegate().listAllJobs( this.app.getName(), "write 2" );
		Assert.assertEquals( 1, receivedJobs.size());
		Assert.assertEquals( jobId2, receivedJobs.get( 0 ).getJobId());

		// Test getting the properties
		ScheduledJob expectedJob = this.client.getSchedulerDelegate().getJobProperties( jobId2 );
		Assert.assertNotNull( expectedJob );
		Assert.assertEquals( jobId2, expectedJob.getJobId());
		Assert.assertEquals( "my job", expectedJob.getJobName());
		Assert.assertEquals( this.app.getName(), expectedJob.getAppName());
		Assert.assertEquals( "write 2", expectedJob.getCmdName());
		Assert.assertEquals( "0 0 0 ? 1 *", expectedJob.getCron());

		// Try to update an existing job
		String receivedId = this.client.getSchedulerDelegate().createOrUpdateJob( jobId2, "my job", this.app.getName(), "write 1", "0 0 0 ? 1 5" );
		Assert.assertEquals( jobId2, receivedId );

		expectedJob = this.client.getSchedulerDelegate().getJobProperties( jobId2 );
		Assert.assertNotNull( expectedJob );
		Assert.assertEquals( jobId2, expectedJob.getJobId());
		Assert.assertEquals( "my job", expectedJob.getJobName());
		Assert.assertEquals( this.app.getName(), expectedJob.getAppName());
		Assert.assertEquals( "write 1", expectedJob.getCmdName());
		Assert.assertEquals( "0 0 0 ? 1 5", expectedJob.getCron());

		// Listing should not have changed
		Assert.assertEquals( 2, this.client.getSchedulerDelegate().listAllJobs( null, null ).size());

		// Try deleting a job
		this.client.getSchedulerDelegate().deleteJob( jobId1 );
		Assert.assertEquals( 1, this.client.getSchedulerDelegate().listAllJobs( null, null ).size());
	}


	@Test( expected = SchedulerWsException.class )
	public void testGetJob_inexitsing() throws Exception {

		this.client.getSchedulerDelegate().getJobProperties( "invalid id" );
	}
}
