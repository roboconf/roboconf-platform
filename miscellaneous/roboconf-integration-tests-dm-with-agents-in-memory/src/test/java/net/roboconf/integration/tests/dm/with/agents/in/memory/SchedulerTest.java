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

package net.roboconf.integration.tests.dm.with.agents.in.memory;

import java.io.File;
import java.util.List;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.ops4j.pax.exam.ExamSystem;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.TestContainer;
import org.ops4j.pax.exam.karaf.container.internal.KarafTestContainer;
import org.ops4j.pax.exam.spi.PaxExamRuntime;

import net.roboconf.core.Constants;
import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.core.model.runtime.CommandHistoryItem;
import net.roboconf.core.model.runtime.ScheduledJob;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.rest.client.WsClient;
import net.roboconf.integration.tests.commons.internal.ItUtils;
import net.roboconf.integration.tests.dm.with.agents.in.memory.probes.DmWithAgentInMemoryTest;
import net.roboconf.messaging.rabbitmq.internal.utils.RabbitMqTestUtils;

/**
 * This test verifies that job scheduling (and its REST API) work.
 * @author Vincent Zurczak - Linagora
 */
public class SchedulerTest extends DmWithAgentInMemoryTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();
	private File karafDirectory, tmpFile;


	@Test
	public void run() throws Exception {

		// We copy an application template and we add it a command
		Assume.assumeTrue( RabbitMqTestUtils.checkRabbitMqIsRunning());
		File sourceAppDirectory = TestUtils.findApplicationDirectory( "lamp" );
		File appDirectory = this.folder.newFolder();
		Utils.copyDirectory( sourceAppDirectory, appDirectory );

		// Create a command that writes into a file
		this.tmpFile = File.createTempFile( "roboconf-it-", "scheduler" );
		Utils.deleteFilesRecursively( this.tmpFile );
		Assert.assertFalse( this.tmpFile.exists());

		File cmdFile = new File( appDirectory, Constants.PROJECT_DIR_COMMANDS + "/append something" + Constants.FILE_EXT_COMMANDS );
		Utils.deleteFilesRecursively( cmdFile.getParentFile());
		Utils.deleteFilesRecursively( new File( appDirectory, Constants.PROJECT_DIR_RULES_AUTONOMIC ));

		Assert.assertTrue( cmdFile.getParentFile().mkdirs());
		Utils.writeStringInto( "append this into " + this.tmpFile.getAbsolutePath(), cmdFile );

		// Prepare to run an agent distribution
		Option[] options = config();
		ExamSystem system = PaxExamRuntime.createServerSystem( options );
		TestContainer container = PaxExamRuntime.createContainer( system );
		Assert.assertEquals( KarafTestContainer.class, container.getClass());

		WsClient client = null;
		try {
			// Start the DM's distribution... and wait... :(
			container.start();
			ItUtils.waitForDmRestServices( getCurrentPort());

			// Find the Karaf directory
			this.karafDirectory = TestUtils.getInternalField( container, "targetFolder", File.class );
			Assert.assertNotNull( this.karafDirectory );

			// Build a REST client
			String rootUrl = "http://localhost:" + getCurrentPort() + "/roboconf-dm";
			client = new WsClient( rootUrl );

			// Perform the checks
			testScheduler( appDirectory.getAbsolutePath(), client );

		} finally {
			container.stop();
			if( client != null )
				client.destroy();
		}
	}


	private void testScheduler( String appLocation, WsClient client  ) throws Exception {

		// Load an application template
		Assert.assertEquals( 0, client.getManagementDelegate().listApplicationTemplates().size());
		client.getManagementDelegate().loadUnzippedApplicationTemplate( appLocation );
		List<ApplicationTemplate> templates = client.getManagementDelegate().listApplicationTemplates();
		Assert.assertEquals( 1, templates.size());

		ApplicationTemplate tpl = templates.get( 0 );
		Assert.assertEquals( "Legacy LAMP", tpl.getName());
		Assert.assertEquals( "1.0.1-SNAPSHOT", tpl.getVersion());

		// Create an application
		Assert.assertEquals( 0, client.getManagementDelegate().listApplications().size());
		client.getManagementDelegate().createApplication( "app1", tpl.getName(), tpl.getVersion());
		List<Application> apps = client.getManagementDelegate().listApplications();
		Assert.assertEquals( 1, apps.size());

		Application receivedApp = apps.get( 0 );
		Assert.assertEquals( "app1", receivedApp.getName());
		Assert.assertEquals( "Legacy LAMP", receivedApp.getTemplate().getName());
		Assert.assertEquals( "1.0.1-SNAPSHOT", receivedApp.getTemplate().getVersion());

		// Verify there is a command
		List<String> cmdNames = client.getApplicationDelegate().listAllCommands( receivedApp.getName());
		Assert.assertEquals( 1, cmdNames.size());
		Assert.assertEquals( "append something", cmdNames.get( 0 ));

		// Verify no command was listed in the history
		String historyUrl = "http://localhost:" + getCurrentPort() + "/roboconf-dm/history/commands";
		String historyContent = Utils.readUrlContent( historyUrl );
		Assert.assertEquals( "[]", historyContent );

		// Create a scheduled job (every second)
		Assert.assertFalse( this.tmpFile.exists());
		Assert.assertEquals( 0, client.getSchedulerDelegate().listAllJobs( null, null ).size());
		client.getSchedulerDelegate().createOrUpdateJob( null, "job1", receivedApp.getName(), cmdNames.get( 0 ), "* * * * * ?" );

		// Verify the listing
		List<ScheduledJob> jobs = client.getSchedulerDelegate().listAllJobs( null, null );
		Assert.assertEquals( 1, jobs.size());
		Assert.assertEquals( receivedApp.getName(), jobs.get( 0 ).getAppName());
		Assert.assertEquals( cmdNames.get( 0 ), jobs.get( 0 ).getCmdName());
		Assert.assertEquals( "job1", jobs.get( 0 ).getJobName());
		Assert.assertEquals( "* * * * * ?", jobs.get( 0 ).getCron());

		jobs = client.getSchedulerDelegate().listAllJobs( receivedApp.getName(), null );
		Assert.assertEquals( 1, jobs.size());

		jobs = client.getSchedulerDelegate().listAllJobs( receivedApp.getName(), cmdNames.get( 0 ));
		Assert.assertEquals( 1, jobs.size());

		// Usually, crons do not allow seconds precision...
		// But Quartz does.
		Thread.sleep( 2000 );
		Assert.assertTrue( this.tmpFile.exists());

		// Delete the job
		jobs = client.getSchedulerDelegate().listAllJobs( null, null );
		Assert.assertEquals( 1, jobs.size());

		client.getSchedulerDelegate().deleteJob( jobs.get( 0 ).getJobId());
		jobs = client.getSchedulerDelegate().listAllJobs( null, null );
		Assert.assertEquals( 0, jobs.size());

		// Deleting a job does not delete the command or the application
		apps = client.getManagementDelegate().listApplications();
		Assert.assertEquals( 1, apps.size());

		cmdNames = client.getApplicationDelegate().listAllCommands( receivedApp.getName());
		Assert.assertEquals( 1, cmdNames.size());

		// Verify the execution appears in the history
		historyContent = Utils.readUrlContent( historyUrl );
		Assert.assertNotEquals( "[]", historyContent );
		Assert.assertTrue( historyContent.startsWith( "[{" ));
		Assert.assertTrue( historyContent.endsWith( "}]" ));
		Assert.assertTrue( historyContent.contains( ",\"details\":\"job1\"," ));
		Assert.assertTrue( historyContent.contains( "\"origin\":" + CommandHistoryItem.ORIGIN_SCHEDULER ));
	}
}
