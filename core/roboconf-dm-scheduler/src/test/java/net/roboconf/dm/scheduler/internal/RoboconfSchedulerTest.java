/**
 * Copyright 2016-2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.dm.scheduler.internal;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.impl.matchers.GroupMatcher;

import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.runtime.ScheduledJob;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.internal.test.TestManagerWrapper;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.management.events.IDmListener;

/**
 * @author Vincent Zurczak - Linagora
 */
public class RoboconfSchedulerTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();
	private RoboconfScheduler scheduler;
	private Manager manager;


	@Before
	public void prepare() throws Exception {

		// Prepare the manager
		this.manager = Mockito.spy( new Manager());
		this.manager.configurationMngr().setWorkingDirectory( this.folder.newFolder());

		this.scheduler = new RoboconfScheduler();
		this.scheduler.manager = this.manager;

		// Configure applications and commands
		TestManagerWrapper wrapper = new TestManagerWrapper( this.manager );
		Application app = new TestApplication().name( "app" );
		app.setDirectory( this.folder.newFolder());
		wrapper.addManagedApplication( new ManagedApplication( app ));

		// Register commands
		this.manager.commandsMngr().createOrUpdateCommand( app, "cmd", "" );

		// Ignore all the manager's invocations until now
		Mockito.reset( this.manager );
	}


	@After
	public void clean() throws Exception {
		this.scheduler.stop();
	}


	@Test
	public void testStartAndStop() throws Exception {

		Assert.assertNull( this.scheduler.dmListener );
		Assert.assertNull( this.scheduler.scheduler );

		Mockito.verifyZeroInteractions( this.manager );
		this.scheduler.start();

		Assert.assertNotNull( this.scheduler.dmListener );
		Assert.assertNotNull( this.scheduler.scheduler );

		Assert.assertTrue( this.scheduler.scheduler.isStarted());
		Mockito.verify( this.manager, Mockito.times( 1 )).listenerAppears( this.scheduler.dmListener );
		Mockito.verify( this.manager, Mockito.atLeast( 1 )).configurationMngr();

		String dmPath = this.manager.configurationMngr().getWorkingDirectory().getAbsolutePath();
		String schedulerPath = this.scheduler.getSchedulerDirectory().getAbsolutePath();
		Assert.assertTrue( schedulerPath.startsWith( dmPath ));

		this.scheduler.stop();
		Mockito.verify( this.manager, Mockito.times( 1 )).listenerAppears( Mockito.any( IDmListener.class ));
		Mockito.verify( this.manager, Mockito.times( 1 )).listenerDisappears( Mockito.any( IDmListener.class ));

		Assert.assertNull( this.scheduler.dmListener );
		Assert.assertNull( this.scheduler.scheduler );
	}


	@Test
	public void testStop_noManager() throws Exception {

		this.scheduler.manager = null;
		this.scheduler.stop();

		Assert.assertNull( this.scheduler.dmListener );
		Assert.assertNull( this.scheduler.scheduler );
	}


	@Test
	public void testSaveAndLoadJobs() throws Exception {

		// Start the scheduler and halts triggers
		this.scheduler.start();
		this.scheduler.scheduler.standby();
		File schedulerDirectory = this.scheduler.getSchedulerDirectory();

		Set<JobKey> jobKeys = this.scheduler.scheduler.getJobKeys( GroupMatcher.anyJobGroup());
		Assert.assertEquals( 0, jobKeys.size());
		Assert.assertEquals( 0, Utils.listAllFiles( schedulerDirectory ).size());

		// Create several jobs
		final int max = 5;
		String[] jobIds = new String[ max ];
		for( int i=0; i<max; i++ ) {
			jobIds[ i ] = this.scheduler.saveJob( null, "job " + i, "cmd", "0 0 0 ? 1 *", "app" );
		}

		jobKeys = this.scheduler.scheduler.getJobKeys( GroupMatcher.anyJobGroup());
		Assert.assertEquals( max, jobKeys.size());
		Assert.assertEquals( max, Utils.listAllFiles( schedulerDirectory ).size());

		// Delete a job
		this.scheduler.deleteJob( jobIds[ 3 ]);
		jobKeys = this.scheduler.scheduler.getJobKeys( GroupMatcher.anyJobGroup());
		Assert.assertEquals( max - 1, jobKeys.size());
		Assert.assertEquals( max - 1, Utils.listAllFiles( schedulerDirectory ).size());

		// Find job properties
		ScheduledJob job = this.scheduler.findJobProperties( jobIds[ 1 ]);
		Assert.assertNotNull( job );
		Assert.assertEquals( "app", job.getAppName());
		Assert.assertEquals( "cmd", job.getCmdName());
		Assert.assertEquals( "0 0 0 ? 1 *", job.getCron());
		Assert.assertEquals( "job 1", job.getJobName());

		// Find the properties of a non-existing job
		job = this.scheduler.findJobProperties( "job M" );
		Assert.assertNull( job );
	}


	@Test
	public void testSaveAndLoadJobs_invalidCron() throws Exception {

		// Start the scheduler and halts triggers
		this.scheduler.start();
		this.scheduler.scheduler.standby();
		File schedulerDirectory = this.scheduler.getSchedulerDirectory();

		Assert.assertEquals( 0, this.scheduler.listJobs().size());
		Set<JobKey> jobKeys = this.scheduler.scheduler.getJobKeys( GroupMatcher.anyJobGroup());
		Assert.assertEquals( 0, jobKeys.size());
		Assert.assertEquals( 0, Utils.listAllFiles( schedulerDirectory ).size());

		// Create an invalid job
		try {
			this.scheduler.saveJob( null, "job", "cmd", "0 0 0 ? invalid *", "app" );
			Assert.fail( "An I/O exception was expected." );

		} catch( IOException e ) {
			// nothing
		}

		// There should not be any job: in the scheduler...
		jobKeys = this.scheduler.scheduler.getJobKeys( GroupMatcher.anyJobGroup());
		Assert.assertEquals( 0, jobKeys.size());
		Assert.assertEquals( 0, Utils.listAllFiles( schedulerDirectory ).size());

		// ... and listed by Roboconf
		Assert.assertEquals( 0, this.scheduler.listJobs().size());
	}


	@Test
	public void testDeleteJob_inexisting() throws Exception {

		// Start the scheduler and halts triggers
		this.scheduler.start();
		this.scheduler.scheduler.standby();
		File schedulerDirectory = this.scheduler.getSchedulerDirectory();

		Set<JobKey> jobKeys = this.scheduler.scheduler.getJobKeys( GroupMatcher.anyJobGroup());
		Assert.assertEquals( 0, jobKeys.size());
		Assert.assertEquals( 0, Utils.listAllFiles( schedulerDirectory ).size());

		// Delete a job
		this.scheduler.deleteJob( "job 3" );
		jobKeys = this.scheduler.scheduler.getJobKeys( GroupMatcher.anyJobGroup());
		Assert.assertEquals( 0, jobKeys.size());
		Assert.assertEquals( 0, Utils.listAllFiles( schedulerDirectory ).size());
	}


	@Test( expected = IllegalArgumentException.class )
	public void testSaveJob_invalidApplicationName() throws Exception {

		// Start the scheduler and halts triggers
		this.scheduler.start();
		this.scheduler.scheduler.standby();

		// Create a job
		this.scheduler.saveJob( null, "my job", "cmd", "0 0 0 ? 1 *", "app that does nto exist" );
	}


	@Test( expected = IllegalArgumentException.class )
	public void testSaveJob_invalidCommandName() throws Exception {

		// Start the scheduler and halts triggers
		this.scheduler.start();
		this.scheduler.scheduler.standby();

		// Create a job
		try {
			Assert.assertNotNull( this.scheduler.saveJob( null, "my job", "cmd", "0 0 0 ? 1 *", "app" ));

		} catch( Exception e ) {
			Assert.fail( "No failure was expected here, the application was registered in the setup method." );
		}

		// This one will fail
		this.scheduler.saveJob( null, "my job", "cmd that does not exist", "0 0 0 ? 1 *", "app" );
	}


	@Test
	public void testSaveJobAndUpdateJob() throws Exception {

		// Start the scheduler and halts triggers
		this.scheduler.start();
		this.scheduler.scheduler.standby();
		File schedulerDirectory = this.scheduler.getSchedulerDirectory();

		Set<JobKey> jobKeys = this.scheduler.scheduler.getJobKeys( GroupMatcher.anyJobGroup());
		Assert.assertEquals( 0, jobKeys.size());
		Assert.assertEquals( 0, Utils.listAllFiles( schedulerDirectory ).size());

		// Create a job
		String jobId = this.scheduler.saveJob( null, "my job", "cmd", "0 0 0 ? 1 *", "app" );

		jobKeys = this.scheduler.scheduler.getJobKeys( GroupMatcher.anyJobGroup());
		Assert.assertEquals( 1, jobKeys.size());
		Assert.assertEquals( 1, Utils.listAllFiles( schedulerDirectory ).size());

		// Find job properties
		ScheduledJob readJob = this.scheduler.findJobProperties( jobId );
		Assert.assertNotNull( readJob );
		Assert.assertEquals( "app", readJob.getAppName());
		Assert.assertEquals( "cmd", readJob.getCmdName());
		Assert.assertEquals( "0 0 0 ? 1 *", readJob.getCron());
		Assert.assertEquals( "my job", readJob.getJobName());

		// Rename the initial job and the application's name.
		// We also need to make sure the application exists...
		TestManagerWrapper wrapper = new TestManagerWrapper( this.manager );
		Application app = new TestApplication().name( "new app" );
		app.setDirectory( this.folder.newFolder());
		wrapper.addManagedApplication( new ManagedApplication( app ));

		// ... as well as the command.
		this.manager.commandsMngr().createOrUpdateCommand( app, "cmd", "" );

		// Update the job
		jobId = this.scheduler.saveJob( jobId, "my new job", "cmd", "0 0 0 ? 1 *", app.getName());

		// Find job properties
		readJob = this.scheduler.findJobProperties( jobId );
		Assert.assertNotNull( readJob );
		Assert.assertEquals( app.getName(), readJob.getAppName());
		Assert.assertEquals( "cmd", readJob.getCmdName());
		Assert.assertEquals( "0 0 0 ? 1 *", readJob.getCron());
		Assert.assertEquals( "my new job", readJob.getJobName());

		// Delete the job
		this.scheduler.deleteJob( jobId );
		jobKeys = this.scheduler.scheduler.getJobKeys( GroupMatcher.anyJobGroup());
		Assert.assertEquals( 0, jobKeys.size());
		Assert.assertEquals( 0, Utils.listAllFiles( schedulerDirectory ).size());
	}


	@Test
	public void testDeleteJob_noAppNameInJobProperties() throws Exception {

		this.scheduler.scheduler = Mockito.mock( Scheduler.class );

		Properties props = new Properties();
		props.put( RoboconfScheduler.JOB_NAME, "job" );
		props.put( RoboconfScheduler.CMD_NAME, "cmd" );
		props.put( RoboconfScheduler.CRON, "0 0 0 ? 1 *" );

		try {
			File f = this.scheduler.getJobFile( "job" );
			Assert.assertTrue( f.getParentFile().mkdirs());
			Utils.writePropertiesFile( props, f );

		} catch( Exception e ) {
			Assert.fail( "No exception was expected here." );
		}

		this.scheduler.deleteJob( "job" );
		Mockito.verifyZeroInteractions( this.scheduler.scheduler );
	}


	@Test
	public void testSaveJob_invalidProperties_noCron() throws Exception {

		// Start the scheduler and halts triggers
		this.scheduler.start();
		this.scheduler.scheduler.standby();
		File schedulerDirectory = this.scheduler.getSchedulerDirectory();

		Set<JobKey> jobKeys = this.scheduler.scheduler.getJobKeys( GroupMatcher.anyJobGroup());
		Assert.assertEquals( 0, jobKeys.size());
		Assert.assertEquals( 0, Utils.listAllFiles( schedulerDirectory ).size());

		// Save an invalid job (no cron)
		this.scheduler.saveJob( null, "job", "cmd", null, "app" );

		jobKeys = this.scheduler.scheduler.getJobKeys( GroupMatcher.anyJobGroup());
		Assert.assertEquals( 0, jobKeys.size());
		Assert.assertEquals( 0, Utils.listAllFiles( schedulerDirectory ).size());

		// Save an invalid job (no job name)
		this.scheduler.saveJob( null, null, "cmd", "0 0 0 ? 1 *", "app" );

		jobKeys = this.scheduler.scheduler.getJobKeys( GroupMatcher.anyJobGroup());
		Assert.assertEquals( 0, jobKeys.size());
		Assert.assertEquals( 0, Utils.listAllFiles( schedulerDirectory ).size());

		// Save an invalid job (no command name)
		this.scheduler.saveJob( null, "job", null, "0 0 0 ? 1 *", "app" );

		jobKeys = this.scheduler.scheduler.getJobKeys( GroupMatcher.anyJobGroup());
		Assert.assertEquals( 0, jobKeys.size());
		Assert.assertEquals( 0, Utils.listAllFiles( schedulerDirectory ).size());

		// Save an invalid job (no application name)
		this.scheduler.saveJob( null, "job", "cmd", "0 0 0 ? 1 *", null );

		jobKeys = this.scheduler.scheduler.getJobKeys( GroupMatcher.anyJobGroup());
		Assert.assertEquals( 0, jobKeys.size());
		Assert.assertEquals( 0, Utils.listAllFiles( schedulerDirectory ).size());

		// Save an invalid job (nothing at all)
		this.scheduler.saveJob( null, null, null, null, null );

		jobKeys = this.scheduler.scheduler.getJobKeys( GroupMatcher.anyJobGroup());
		Assert.assertEquals( 0, jobKeys.size());
		Assert.assertEquals( 0, Utils.listAllFiles( schedulerDirectory ).size());

		// Save an invalid job (an ID but nothing else)
		this.scheduler.saveJob( "some-id", null, null, null, null );

		jobKeys = this.scheduler.scheduler.getJobKeys( GroupMatcher.anyJobGroup());
		Assert.assertEquals( 0, jobKeys.size());
		Assert.assertEquals( 0, Utils.listAllFiles( schedulerDirectory ).size());
	}


	@Test
	public void testLoadAll_and_ListJobs() throws Exception {

		Utils.createDirectory( this.scheduler.getSchedulerDirectory());

		// Create several job FILES
		final int max = 5;
		for( int i=0; i<max; i++ ) {

			Properties props = new Properties();
			props.put( RoboconfScheduler.JOB_NAME, "same job name" );
			props.put( RoboconfScheduler.APP_NAME, "app" );
			props.put( RoboconfScheduler.CMD_NAME, "cmd" );
			props.put( RoboconfScheduler.CRON, "0 0 0 ? 1 *" );

			Utils.writePropertiesFile( props, this.scheduler.getJobFile( "my-id-" + i ));
		}

		// Start the scheduler and halts triggers
		this.scheduler.start();
		this.scheduler.scheduler.standby();
		File schedulerDirectory = this.scheduler.getSchedulerDirectory();

		Set<JobKey> jobKeys = this.scheduler.scheduler.getJobKeys( GroupMatcher.anyJobGroup());
		Assert.assertEquals( max, jobKeys.size());
		Assert.assertEquals( max, Utils.listAllFiles( schedulerDirectory ).size());

		// List the jobs
		List<ScheduledJob> jobs = this.scheduler.listJobs();
		Assert.assertEquals( max, jobs.size());

		Set<String> jobIds = new HashSet<> ();
		for( ScheduledJob job : jobs ) {
			Assert.assertEquals( "same job name", job.getJobName());
			Assert.assertEquals( "app", job.getAppName());
			Assert.assertEquals( "cmd", job.getCmdName());
			Assert.assertEquals( "0 0 0 ? 1 *", job.getCron());
			jobIds.add( job.getJobId());
		}

		Assert.assertEquals( max, jobIds.size());
	}


	@Test( expected = IOException.class )
	public void testSaveJob_exception() throws Exception {

		this.scheduler.scheduler = Mockito.mock( Scheduler.class );
		Mockito.when( this.scheduler.scheduler.scheduleJob(
				Mockito.any( JobDetail.class ),
				Mockito.any( Trigger.class ))).thenThrow( new SchedulerException( "For test" ));

		this.scheduler.saveJob( null, "job", "cmd", "0 0 0 ? 1 *", "app" );
	}


	@Test( expected = IOException.class )
	public void testDeleteJob_exception() throws Exception {

		this.scheduler.scheduler = Mockito.mock( Scheduler.class );
		Mockito
			.when( this.scheduler.scheduler.unscheduleJob( Mockito.any( TriggerKey.class )))
			.thenThrow( new SchedulerException( "For test" ));

		Properties props = new Properties();
		props.put( RoboconfScheduler.JOB_NAME, "job" );
		props.put( RoboconfScheduler.APP_NAME, "app" );
		props.put( RoboconfScheduler.CMD_NAME, "cmd" );
		props.put( RoboconfScheduler.CRON, "0 0 0 ? 1 *" );

		try {
			File f = this.scheduler.getJobFile( "job-id" );
			Assert.assertTrue( f.getParentFile().mkdirs());
			Utils.writePropertiesFile( props, f );

		} catch( Exception e ) {
			Assert.fail( "No exception was expected here." );
		}

		this.scheduler.deleteJob( "job-id" );
	}


	@Test
	public void testLoadAllJobs_invalidProperties() throws Exception {

		Utils.createDirectory( this.scheduler.getSchedulerDirectory());

		// Create several job FILES
		final int max = 5;
		for( int i=0; i<max; i++ ) {

			Properties props = new Properties();
			props.put( RoboconfScheduler.JOB_NAME, "same job name" );
			props.put( RoboconfScheduler.APP_NAME, "app" );
			props.put( RoboconfScheduler.CMD_NAME, "cmd" );

			// One job will have invalid properties
			if( i != 3 )
				props.put( RoboconfScheduler.CRON, "0 0 0 ? 1 *" );

			Utils.writePropertiesFile( props, this.scheduler.getJobFile( "job-id-" + i ));
		}

		// Start the scheduler and halts triggers
		this.scheduler.start();
		this.scheduler.scheduler.standby();
		File schedulerDirectory = this.scheduler.getSchedulerDirectory();

		Set<JobKey> jobKeys = this.scheduler.scheduler.getJobKeys( GroupMatcher.anyJobGroup());
		Assert.assertEquals( max - 1, jobKeys.size());
		Assert.assertEquals( max, Utils.listAllFiles( schedulerDirectory ).size());

		// List the jobs
		List<ScheduledJob> jobs = this.scheduler.listJobs();
		Assert.assertEquals( max, jobs.size());

		Set<String> jobIds = new HashSet<> ();
		for( ScheduledJob job : jobs ) {
			Assert.assertEquals( "app", job.getAppName());
			Assert.assertEquals( "cmd", job.getCmdName());
			Assert.assertEquals( "same job name", job.getJobName());
			jobIds.add( job.getJobId());
		}

		Assert.assertEquals( max, jobIds.size());
	}


	@Test
	public void testLoadAllJobs_exception() throws Exception {

		Utils.createDirectory( this.scheduler.getSchedulerDirectory());

		// Create several job FILES
		final int max = 5;
		for( int i=0; i<max; i++ ) {

			Properties props = new Properties();
			props.put( RoboconfScheduler.JOB_NAME, "job " + i );
			props.put( RoboconfScheduler.APP_NAME, "app" );
			props.put( RoboconfScheduler.CMD_NAME, "cmd" );
			props.put( RoboconfScheduler.CRON, "0 0 0 ? 1 *" );

			Utils.writePropertiesFile( props, this.scheduler.getJobFile( "job-id-" + i ));
		}

		// Throw an exception when a job is scheduled
		this.scheduler.scheduler = Mockito.mock( Scheduler.class );
		Mockito.when( this.scheduler.scheduler.scheduleJob(
				Mockito.any( JobDetail.class ),
				Mockito.any( Trigger.class ))).thenThrow( new SchedulerException( "For test" ));

		// Load the jobs...
		this.scheduler.loadJobs();
		File schedulerDirectory = this.scheduler.getSchedulerDirectory();

		Set<JobKey> jobKeys = this.scheduler.scheduler.getJobKeys( GroupMatcher.anyJobGroup());
		Assert.assertEquals( 0, jobKeys.size());
		Assert.assertEquals( max, Utils.listAllFiles( schedulerDirectory ).size());
	}


	@Test
	public void testListAllJobs_withEmptyProperties() throws Exception {

		Utils.createDirectory( this.scheduler.getSchedulerDirectory());

		Properties props = new Properties();
		props.put( RoboconfScheduler.JOB_NAME, "job1" );
		props.put( RoboconfScheduler.CMD_NAME, "cmd" );
		props.put( RoboconfScheduler.CRON, "0 0 0 ? 1 *" );

		Utils.writePropertiesFile( props, this.scheduler.getJobFile( "job1" ));
		Utils.writePropertiesFile( new Properties(), this.scheduler.getJobFile( "job2" ));

		List<ScheduledJob> jobs = this.scheduler.listJobs();
		Assert.assertEquals( 1, jobs.size());
		Assert.assertEquals( "job1", jobs.get( 0 ).getJobName());
	}


	@Test
	public void testFindJobProperties_inexitingJob() throws Exception {

		ScheduledJob job = this.scheduler.findJobProperties( "inexisting" );
		Assert.assertNull( job );
	}
}
