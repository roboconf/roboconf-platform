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

import java.util.HashMap;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import org.quartz.SchedulerContext;

import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.runtime.CommandHistoryItem;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.management.api.ICommandsMngr;
import net.roboconf.dm.management.exceptions.CommandException;

/**
 * @author Vincent Zurczak - Linagora
 */
public class CommandExecutionJobTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private JobExecutionContext context;
	private Manager manager;
	private ICommandsMngr commandMngr;


	@Before
	public void prepare() throws Exception {

		JobDataMap map = new JobDataMap( new HashMap<String,String> ());
		map.put( RoboconfScheduler.APP_NAME, "app" );
		map.put( RoboconfScheduler.JOB_NAME, "job" );
		map.put( RoboconfScheduler.CMD_NAME, "cmd" );

		JobDetail jobDetail = Mockito.mock( JobDetail.class );
		Mockito.when( jobDetail.getJobDataMap()).thenReturn( map );

		this.manager = Mockito.spy( new Manager());
		this.manager.configurationMngr().setWorkingDirectory( this.folder.newFolder());

		this.commandMngr = Mockito.mock( ICommandsMngr.class );
		Mockito.when( this.manager.commandsMngr()).thenReturn( this.commandMngr );

		this.context = Mockito.mock( JobExecutionContext.class );
		Mockito.when( this.context.get( RoboconfScheduler.MANAGER )).thenReturn( this.manager );
		Mockito.when( this.context.getJobDetail()).thenReturn( jobDetail );

		Scheduler scheduler = Mockito.mock( Scheduler.class );
		Mockito.when( this.context.getScheduler()).thenReturn( scheduler );

		SchedulerContext schedulerCtx = Mockito.mock( SchedulerContext.class );
		Mockito.when( scheduler.getContext()).thenReturn( schedulerCtx );
		Mockito.when( schedulerCtx.get( RoboconfScheduler.MANAGER )).thenReturn( this.manager );
	}



	@Test
	public void testNormalExecution() throws Exception {

		CommandExecutionJob job = new CommandExecutionJob();
		job.execute( this.context );

		Mockito.verify( this.context, Mockito.times( 1 )).getScheduler();
		Mockito.verify( this.context, Mockito.times( 3 )).getJobDetail();
		Mockito.verify( this.manager, Mockito.times( 1 )).applicationMngr();
		Mockito.verify( this.commandMngr, Mockito.times( 1 )).execute( null, "cmd", CommandHistoryItem.ORIGIN_SCHEDULER, "job" );
	}


	@Test
	public void testExecutionInError() throws Exception {

		Mockito
			.doThrow( new CommandException( "For test" ))
			.when( this.commandMngr ).execute( Mockito.any( Application.class ), Mockito.anyString(), Mockito.anyInt(), Mockito.anyString());

		CommandExecutionJob job = new CommandExecutionJob();
		job.execute( this.context );

		Mockito.verify( this.context, Mockito.times( 1 )).getScheduler();
		Mockito.verify( this.context, Mockito.times( 3 )).getJobDetail();
		Mockito.verify( this.manager, Mockito.times( 1 )).applicationMngr();
		Mockito.verify( this.commandMngr, Mockito.times( 1 )).execute( null, "cmd", CommandHistoryItem.ORIGIN_SCHEDULER, "job" );
	}
}
