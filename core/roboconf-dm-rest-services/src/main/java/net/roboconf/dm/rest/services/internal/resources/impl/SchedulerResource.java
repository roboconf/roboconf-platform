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
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import net.roboconf.core.model.runtime.ScheduledJob;
import net.roboconf.dm.rest.services.internal.RestServicesUtils;
import net.roboconf.dm.rest.services.internal.resources.ISchedulerResource;
import net.roboconf.dm.scheduler.IScheduler;

/**
 * @author Vincent Zurczak - Linagora
 */
@Path( ISchedulerResource.PATH )
public class SchedulerResource implements ISchedulerResource {

	private static final String MSG = " Roboconf's scheduler is not available.";
	private final Logger logger = Logger.getLogger( getClass().getName());
	IScheduler scheduler;



	@Override
	public Response saveJob( String jobId, String jobName, String appName, String cmdName, String cron ) {

		this.logger.fine( "Request: save a new scheduled job as " + jobName + "." );
		Response result;
		String msg = "Job " + jobName + " could not be saved.";
		try {
			ScheduledJob job = this.scheduler.saveJob( jobId, jobName, cmdName, cron, appName );
			result = Response.ok( job ).build();

		} catch( IllegalArgumentException e ) {
			result = RestServicesUtils.handleException( this.logger, Status.NOT_FOUND, msg, e ).build();

		} catch( IOException e ) {
			result = RestServicesUtils.handleException( this.logger, Status.BAD_REQUEST, msg, e ).build();

		} catch( NullPointerException e ) {
			// Catch NPEs because it is more simple to deal with multi-threading issues.
			result = RestServicesUtils.handleException( this.logger, Status.FORBIDDEN, msg + MSG ).build();
		}

		return result;
	}


	@Override
	public Response deleteJob( String jobName ) {

		this.logger.fine( "Request: delete the scheduled job " + jobName + "." );
		Response result = Response.ok().build();
		String msg = "Job " + jobName + " could not be deleted.";
		try {
			this.scheduler.deleteJob( jobName );

		} catch( IOException e ) {
			result = RestServicesUtils.handleException( this.logger, Status.INTERNAL_SERVER_ERROR, msg, e ).build();

		} catch( NullPointerException e ) {
			// Catch NPEs because it is more simple to deal with multi-threading issues.
			result = RestServicesUtils.handleException( this.logger, Status.FORBIDDEN, msg + MSG ).build();
		}

		return result;
	}


	@Override
	public Response findJobProperties( String jobName ) {

		Response result;
		this.logger.fine( "Request: get the details of job " + jobName + "." );
		try {
			ScheduledJob job = this.scheduler.findJobProperties( jobName );
			if( job == null )
				result = RestServicesUtils.handleException( this.logger, Status.NOT_FOUND, "Job " + jobName + " does not exist." ).build();
			else
				result = Response.ok( job ).build();

		} catch( NullPointerException e ) {
			// Catch NPEs because it is more simple to deal with multi-threading issues.
			result = RestServicesUtils.handleException( this.logger, Status.FORBIDDEN, MSG.trim()).build();
		}

		return result;
	}


	@Override
	public List<ScheduledJob> listJobs( String appName, String cmdName ) {

		List<ScheduledJob> result = new ArrayList<> ();
		this.logger.fine( "Request: get all the scheduled jobs." );
		try {
			if( appName == null && cmdName == null ) {
				result = this.scheduler.listJobs();

			} else if( appName != null ) {
				for( ScheduledJob job : this.scheduler.listJobs()) {
					boolean cmdMatch = cmdName == null || cmdName.equals( job.getCmdName());
					if( cmdMatch
							&& appName.equals( job.getAppName()))
						result.add( job );
				}
			}

		} catch( NullPointerException e ) {
			// Catch NPEs because it is more simple to deal with multi-threading issues.
			this.logger.warning( MSG.trim());
		}

		return result;
	}


	/**
	 * @param scheduler the scheduler to set
	 */
	public void setScheduler( IScheduler scheduler ) {
		this.scheduler = scheduler;
	}
}
