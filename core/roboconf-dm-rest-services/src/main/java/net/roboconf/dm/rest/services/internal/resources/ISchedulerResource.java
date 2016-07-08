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

package net.roboconf.dm.rest.services.internal.resources;

import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import net.roboconf.core.model.runtime.ScheduledJob;
import net.roboconf.dm.rest.commons.UrlConstants;

/**
 * The REST API to scheduler jobs.
 * <p>
 * Implementing classes may have to redefine the "Path" annotation
 * on the class. This is not required on methods.
 * </p>
 *
 * @author Vincent Zurczak - Linagora
 */
public interface ISchedulerResource {

	String PATH = "/" + UrlConstants.SCHEDULER;


	/**
	 * Saves a job.
	 * @param jobName the job's name
	 * @param appName the application's name
	 * @param cmdName the name of the commands file to execute
	 * @param cron the CRON expression to trigger the job
	 * @return a response
	 *
	 * @HTTP 200 everything went fine
	 * @HTTP 403 if Roboconf's scheduler is not available
	 * @HTTP 400 if a problem arose with the parameters
	 */
	@POST
	Response saveJob(
			@QueryParam("job-id") String jobId,
			@QueryParam("job-name") String jobName,
			@QueryParam("app-name") String appName,
			@QueryParam("cmd-name") String cmdName,
			@QueryParam("cron") String cron );


	/**
	 * Deletes a job.
	 * @param jobName the job's name
	 * @return a response
	 *
	 * @HTTP 200 everything went fine
	 * @HTTP 403 if Roboconf's scheduler is not available
	 */
	@DELETE
	Response deleteJob( String jobName );


	/**
	 * Lists all the jobs.
	 * @param jobName the job's name
	 * @return a non-null list of jobs
	 *
	 * @HTTP 200 everything went fine
	 */
	@GET
	List<ScheduledJob> listJobs();


	/**
	 * Gets the properties of a given job.
	 * @param jobName the job's name
	 * @return a response
	 *
	 * @HTTP 200 everything went fine
	 * @HTTP 403 if Roboconf's scheduler is not available
	 * @HTTP 404 if the job was not found
	 */
	@GET
	@Path( "job" )
	Response findJobProperties( @QueryParam( "job-name" ) String jobName );
}
