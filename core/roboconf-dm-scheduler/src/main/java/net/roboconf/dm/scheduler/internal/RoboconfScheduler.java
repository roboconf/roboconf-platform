/**
 * Copyright 2016 Linagora, Université Joseph Fourier, Floralis
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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Logger;

import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.StdSchedulerFactory;

import net.roboconf.core.Constants;
import net.roboconf.core.model.runtime.ScheduledJob;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.management.events.IDmListener;
import net.roboconf.dm.scheduler.IScheduler;

/**
 * @author Vincent Zurczak - Linagora
 */
public class RoboconfScheduler implements IScheduler {

	static final String JOB_ID = "id";
	static final String JOB_NAME = "job-name";
	static final String APP_NAME = "application-name";
	static final String CMD_NAME = "command-file-name";
	static final String CRON = "cron";
	static final String MANAGER = "manager";
	static final String PROJECT_DIR_SCHEDULER = "scheduler";

	private final Logger logger = Logger.getLogger( getClass().getName());

	IDmListener dmListener;
	Scheduler scheduler;
	Manager manager;


	/**
	 * Starts the scheduler.
	 * <p>
	 * Invoked by iPojo.
	 * </p>
	 */
	public void start() throws Exception {

		// Verify the "scheduler" directory exists
		File schedulerDirectory = getSchedulerDirectory();
		Utils.createDirectory( schedulerDirectory );

		// Disable Quartz update checks
		StringBuilder quartzProperties = new StringBuilder();
		quartzProperties.append( "org.quartz.scheduler.instanceName: Roboconf Quartz Scheduler\n" );
		quartzProperties.append( "org.quartz.threadPool.threadCount = 3\n" );
		quartzProperties.append( "org.quartz.jobStore.class = org.quartz.simpl.RAMJobStore\n" );
		quartzProperties.append( "org.quartz.scheduler.skipUpdateCheck: false\n" );

		StdSchedulerFactory factory = new StdSchedulerFactory();
		factory.initialize( new ByteArrayInputStream( quartzProperties.toString().getBytes( "UTF-8" )));

		// Create a new scheduler
		this.scheduler = factory.getScheduler();
		this.scheduler.start();
		this.scheduler.getContext().put( MANAGER, this.manager );

		// Add a listener to the DM
		this.dmListener = new ManagerListener( this );
		this.manager.listenerAppears( this.dmListener );

		// Reload all the jobs
		loadJobs();
	}


	/**
	 * Stops the scheduler.
	 * <p>
	 * Invoked by iPojo.
	 * </p>
	 */
	public void stop() throws Exception {

		// Remove the DM listener
		if( this.manager != null ) {
			this.manager.listenerDisappears( this.dmListener );
			this.dmListener = null;
		}

		// Shutdown the scheduler
		if( this.scheduler != null ) {
			this.scheduler.shutdown();
			this.scheduler = null;
		}
	}


	@Override
	public void loadJobs() {

		for( File f : Utils.listAllFiles( getSchedulerDirectory(), Constants.FILE_EXT_PROPERTIES )) {
			try {
				Properties props = Utils.readPropertiesFileQuietly( f, this.logger );

				// Inject the ID in the properties
				props.setProperty( JOB_ID, Utils.removeFileExtension( f.getName()));

				// Validate and reload
				if( validProperties( props ))
					scheduleJob( props );
				else
					this.logger.warning( "Skipped schedule for a job. There are invalid or missing job properties in " + f.getName());

			} catch( IOException e ) {
				this.logger.warning( "Failed to load a scheduled job from " + f.getName());
				Utils.logException( this.logger, e );
			}
		}
	}


	@Override
	public List<ScheduledJob> listJobs() {

		List<ScheduledJob> result = new ArrayList<> ();
		for( File f : Utils.listAllFiles( getSchedulerDirectory(), Constants.FILE_EXT_PROPERTIES )) {

			Properties props = Utils.readPropertiesFileQuietly( f, this.logger );
			if( props.isEmpty())
				continue;

			// Inject the ID in the properties
			props.put( JOB_ID, Utils.removeFileExtension( f.getName()));
			ScheduledJob job = from( props );
			result.add( job );
		}

		return result;
	}


	@Override
	public ScheduledJob saveJob( String jobId, String jobName, String cmdName, String cron, String appName )
	throws IOException {

		Properties props = new Properties();
		if( jobId == null )
			jobId = UUID.randomUUID().toString();

		if( jobName != null )
			props.setProperty( JOB_NAME, jobName );

		if( cmdName != null )
			props.setProperty( CMD_NAME, cmdName );

		if( appName != null )
			props.setProperty( APP_NAME, appName );

		if( cron != null )
			props.setProperty( CRON, cron );

		ScheduledJob result = null;
		if( validProperties( props )) {

			// Unschedule the job, if any
			unscheduleJob( jobId );

			// Save the job's information
			Utils.createDirectory( getSchedulerDirectory());
			Utils.writePropertiesFile( props, getJobFile( jobId ));

			// Inject the ID in the properties and schedule the job
			props.setProperty( JOB_ID, jobId );
			scheduleJob( props );
			result = from( props );
		}

		return result;
	}


	@Override
	public void deleteJob( String jobId ) throws IOException {

		try {
			unscheduleJob( jobId );

		} catch( IOException e ) {
			this.logger.warning( "Failed to remove a scheduled job. Job's id: " + jobId );
			throw e;

		} finally {
			File f = getJobFile( jobId );
			Utils.deleteFilesRecursively( f );
		}
	}


	@Override
	public ScheduledJob findJobProperties( String jobId ) {

		ScheduledJob result = null;
		File f = getJobFile( jobId );
		try {
			// Inject the ID in the properties
			Properties props = Utils.readPropertiesFile( f );
			props.put( JOB_ID, jobId );
			result = from( props );

		} catch( IOException e ) {
			// nothing
		}

		return result;
	}


	File getSchedulerDirectory() {
		return new File( this.manager.configurationMngr().getWorkingDirectory(), PROJECT_DIR_SCHEDULER );
	}


	File getJobFile( String jobId ) {
		return new File( getSchedulerDirectory(), jobId + Constants.FILE_EXT_PROPERTIES );
	}


	/**
	 * @param props non-null properties
	 * @return true if the properties are valid
	 */
	boolean validProperties( Properties props ) {

		// We do not consider the job ID here.
		// Job ID = file name. No need to duplicate the information as
		// it would imply coherence checking.
		String jobName = props.getProperty( JOB_NAME, "" );
		String appName = props.getProperty( APP_NAME, "" );
		String cmdName = props.getProperty( CMD_NAME, "" );
		String cron = props.getProperty( CRON, "" );

		return ! Utils.isEmptyOrWhitespaces( cron )
				&& ! Utils.isEmptyOrWhitespaces( jobName )
				&& ! Utils.isEmptyOrWhitespaces( appName )
				&& ! Utils.isEmptyOrWhitespaces( cmdName );
	}


	/**
	 * @param props non-null and VALID properties
	 * @return true if the job properties are correct and the job was successfully scheduled
	 * @throws IOException
	 * @see {@link #validProperties(Properties)}
	 */
	private boolean scheduleJob( Properties props ) throws IOException {

		// 1 file = 1 job = 1 trigger.
		String jobId = props.getProperty( JOB_ID, "" );
		String jobName = props.getProperty( JOB_NAME, "" );
		String appName = props.getProperty( APP_NAME, "" );
		String cmdName = props.getProperty( CMD_NAME, "" );
		String cron = props.getProperty( CRON, "" );

		// Schedule the job
		JobDetail job = JobBuilder.newJob( CommandExecutionJob.class )
				.withIdentity( jobId, appName )
				.usingJobData( JOB_ID, jobId )
				.usingJobData( APP_NAME, appName )
				.usingJobData( JOB_NAME, jobName )
				.usingJobData( CMD_NAME, cmdName )
				.build();

		Trigger trigger = TriggerBuilder
				.newTrigger()
				.withIdentity( jobId, appName )
				.withSchedule( CronScheduleBuilder.cronSchedule( cron ))
				.build();

		try {
			this.scheduler.scheduleJob( job, trigger );

		} catch( Exception e ) {
			// Catch all the exceptions (including the runtime ones)
			throw new IOException( e );
		}

		return true;
	}


	private void unscheduleJob( String jobId ) throws IOException {

		try {
			File f = getJobFile( jobId );
			if( f.exists()) {
				Properties props = Utils.readPropertiesFileQuietly( f, this.logger );
				String appName = props.getProperty( APP_NAME, "" );
				if( ! Utils.isEmptyOrWhitespaces( appName ))
					this.scheduler.unscheduleJob( TriggerKey.triggerKey( jobId, appName ));
			}

		} catch( SchedulerException e ) {
			// Catch all the exceptions (including the runtime ones)
			throw new IOException( e );
		}
	}


	private ScheduledJob from( Properties props ) {

		ScheduledJob job = new ScheduledJob( props.getProperty( JOB_ID ));
		job.setAppName( props.getProperty( APP_NAME ));
		job.setCmdName( props.getProperty( CMD_NAME ));
		job.setJobName( props.getProperty( JOB_NAME ));
		job.setCron( props.getProperty( CRON ));

		return job;
	}
}
