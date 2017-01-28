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
import java.util.Set;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerKey;
import org.quartz.impl.matchers.GroupMatcher;

import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.core.model.runtime.EventType;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.internal.test.TestManagerWrapper;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ManagerListenerTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();


	@Test
	public void testBasics() {

		ManagerListener listener = new ManagerListener( null );
		Assert.assertEquals( ManagerListener.ID, listener.getId());

		listener.application( null, EventType.CHANGED );
		listener.applicationTemplate( null, EventType.DELETED );
		listener.disableNotifications();
		listener.enableNotifications();
		listener.instance( null, null, EventType.CREATED );
		listener.raw( null );
	}


	@Test
	public void testApplicationDeletion() throws Exception {

		// As we are not very familiar with Quartz behavior,
		// we will use a real Quartz scheduler for tests.
		// We do not mock it.

		Manager manager = Mockito.spy( new Manager());
		manager.configurationMngr().setWorkingDirectory( this.folder.newFolder());

		// Register applications
		final String cmdName = "cmd";
		final Application app1 = new TestApplication().name( "app1" ).directory( this.folder.newFolder());
		final Application app2 = new TestApplication().name( "app2" ).directory( this.folder.newFolder());

		TestManagerWrapper wrapper = new TestManagerWrapper( manager );
		wrapper.addManagedApplication( new ManagedApplication( app1 ));
		wrapper.addManagedApplication( new ManagedApplication( app2 ));

		// Register commands
		manager.commandsMngr().createOrUpdateCommand( app1, cmdName, "" );
		manager.commandsMngr().createOrUpdateCommand( app2, cmdName, "" );

		// Prepare the scheduler
		RoboconfScheduler scheduler = new RoboconfScheduler();
		scheduler.manager = manager;
		Mockito.reset( manager );

		try {
			scheduler.start();

			// Register job properties
			Assert.assertNotNull( scheduler.saveJob( null, "job11", cmdName, "0 0 0 ? 1 *", app1.getName()));
			Assert.assertNotNull( scheduler.saveJob( null, "job12", cmdName, "0 0 0 ? 1 *", app1.getName()));
			Assert.assertNotNull( scheduler.saveJob( null, "job13", cmdName, "0 0 0 ? 1 *", app1.getName()));

			String jobId21 = scheduler.saveJob( null, "job21", cmdName, "0 0 0 ? 1 *", app2.getName());
			Assert.assertNotNull( jobId21  );

			String jobId22 = scheduler.saveJob( null, "job22", cmdName, "0 0 0 ? 1 *", app2.getName());
			Assert.assertNotNull( jobId22  );

			// Now, send signals to the manager listener.
			// Idle ones...
			File schedulerDirectory = scheduler.getSchedulerDirectory();
			Set<JobKey> jobKeys = scheduler.scheduler.getJobKeys( GroupMatcher.anyJobGroup());

			Assert.assertEquals( 5, jobKeys.size());
			Assert.assertEquals( 5, Utils.listAllFiles( schedulerDirectory ).size());

			scheduler.dmListener.application( app1, EventType.CREATED );

			jobKeys = scheduler.scheduler.getJobKeys( GroupMatcher.anyJobGroup());
			Assert.assertEquals( 5, jobKeys.size());
			Assert.assertEquals( 5, Utils.listAllFiles( schedulerDirectory ).size());

			scheduler.dmListener.application( app1, EventType.CHANGED );

			jobKeys = scheduler.scheduler.getJobKeys( GroupMatcher.anyJobGroup());
			Assert.assertEquals( 5, jobKeys.size());
			Assert.assertEquals( 5, Utils.listAllFiles( schedulerDirectory ).size());

			// "app1" was deleted.
			scheduler.dmListener.application( app1, EventType.DELETED );

			jobKeys = scheduler.scheduler.getJobKeys( GroupMatcher.anyJobGroup());
			Assert.assertEquals( 2, jobKeys.size());
			Assert.assertEquals( 2, Utils.listAllFiles( schedulerDirectory ).size());
			Assert.assertTrue( jobKeys.contains( new JobKey( jobId21, "app2" )));
			Assert.assertTrue( jobKeys.contains( new JobKey( jobId22, "app2" )));

		} finally {
			scheduler.stop();
		}
	}



	@Test
	@SuppressWarnings( "unchecked" )
	public void testApplicationDeleted_exceptionWhileListingJobs() throws Exception {

		RoboconfScheduler roboconfScheduler = Mockito.mock( RoboconfScheduler.class );
		roboconfScheduler.scheduler = Mockito.mock( Scheduler.class );
		Mockito
			.when( roboconfScheduler.scheduler.getTriggerKeys( Mockito.any( GroupMatcher.class )))
			.thenThrow( new SchedulerException( "For test" ));

		ManagerListener listener = new ManagerListener( roboconfScheduler );
		listener.application(
				new Application( "app", Mockito.mock( ApplicationTemplate.class )),
				EventType.DELETED );

		Mockito
			.verify( roboconfScheduler.scheduler, Mockito.times( 1 ))
			.getTriggerKeys( Mockito.any( GroupMatcher.class ));
	}


	@Test
	@SuppressWarnings( "unchecked" )
	public void testApplicationDeleted_exceptionWhileDeletingJobs() throws Exception {

		RoboconfScheduler roboconfScheduler = Mockito.mock( RoboconfScheduler.class );
		Mockito
			.doThrow( new IOException( "For test" ))
			.when( roboconfScheduler ).deleteJob( Mockito.anyString());

		Set<TriggerKey> triggerKeys = new HashSet<> ();
		triggerKeys.add( new TriggerKey( "job", "app" ));
		roboconfScheduler.scheduler = Mockito.mock( Scheduler.class );
		Mockito
			.when( roboconfScheduler.scheduler.getTriggerKeys( Mockito.any( GroupMatcher.class )))
			.thenReturn( triggerKeys );

		ManagerListener listener = new ManagerListener( roboconfScheduler );
		listener.application(
				new Application( "app", Mockito.mock( ApplicationTemplate.class )),
				EventType.DELETED );

		Mockito
			.verify( roboconfScheduler.scheduler, Mockito.times( 1 ))
			.getTriggerKeys( Mockito.any( GroupMatcher.class ));

		Mockito
			.verify( roboconfScheduler, Mockito.times( 1 ))
			.deleteJob( Mockito.anyString());
	}
}
