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

package net.roboconf.dm.rest.services.internal.resources;

import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
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
	 * @param jobId the job's ID (null to create a new job)
	 * @param jobName the job's name
	 * @param appName the application's name
	 * @param cmdName the name of the commands file to execute
	 * @param cron the CRON expression to trigger the job
	 * @return a response
	 *
	 * @HTTP 200 Everything went fine.
	 * @HTTP 403 If Roboconf's scheduler is not available.
	 * @HTTP 404 If the application or the command was not found.
	 * @HTTP 400 If a problem arose with the parameters.
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
	 * @param jobId the job's ID
	 * @return a response
	 *
	 * @HTTP 200 Everything went fine.
	 * @HTTP 403 If Roboconf's scheduler is not available.
	 */
	@DELETE
	@Path( "{job-id}" )
	Response deleteJob( @PathParam("job-id") String jobId );


	/**
	 * Lists jobs.
	 * <p>
	 * When no parameter is supplied, all the jobs are returned.
	 * When <code>appName</code> is supplied, only the jobs associated with
	 * this application are returned.  When <code>cmdName</code> is also supplied,
	 * only the jobs associated with this application and this command are returned.
	 * </p>
	 * <p>
	 * When <code>cmdName</code> is supplied but not <code>appName</code>, then no job
	 * is returned.
	 * </p>
	 *
	 * @param appName an application name (optional)
	 * @param cmdName a command name (optional, only makes sense when the application name is given)
	 * @return a non-null list of jobs
	 *
	 * @HTTP 200 Everything went fine.
	 */
	@GET
	List<ScheduledJob> listJobs(
			@QueryParam("app-name") String appName,
			@QueryParam("cmd-name") String cmdName );


	/**
	 * Gets the properties of a given job.
	 * @param jobId the job's ID
	 * @return a response
	 *
	 * @HTTP 200 Everything went fine.
	 * @HTTP 403 If Roboconf's scheduler is not available.
	 * @HTTP 404 If the job was not found.
	 */
	@GET
	@Path( "{job-id}" )
	Response findJobProperties( @PathParam("job-id") String jobId );
}
