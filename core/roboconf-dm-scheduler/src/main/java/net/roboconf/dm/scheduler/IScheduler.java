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

package net.roboconf.dm.scheduler;

import java.io.IOException;
import java.util.List;

import net.roboconf.core.model.runtime.ScheduledJob;

/**
 * An interface to schedule jobs in Roboconf.
 * <p>
 * The principle is that jobs are saved as properties files
 * in the DM's directory. All the scheduled jobs are available
 * as files, and vice-versa.
 * </p>
 * <p>
 * So, manipulating job files is equivalent to updating their current registration.
 * </p>
 *
 * @author Vincent Zurczak - Linagora
 */
public interface IScheduler {

	/**
	 * Loads all the saved jobs in the scheduler.
	 */
	void loadJobs();

	/**
	 * Saves a job.
	 * @param jobId the job's ID (will be generated if null, non-null means editing)
	 * @param jobName the job's name
	 * @param cmdName the name of the commands file to execute (the extension is optional)
	 * @param cron a CRON expression to schedule the command execution
	 * @param appName the application's name
	 * @return the ID of the scheduled job, or null if saving failed
	 * @throws IOException if the job could not be scheduled or if the CRON expression is invalid
	 * @throws IllegalArgumentException if the application or the command does not exist
	 */
	String saveJob( String jobId, String jobName, String cmdName, String cron, String appName )
	throws IOException, IllegalArgumentException;

	/**
	 * Deletes a job.
	 * @param jobId the job's ID
	 * @throws IOException if something went wrong
	 */
	void deleteJob( String jobId ) throws IOException;

	/**
	 * @return a non-null list of jobs
	 */
	List<ScheduledJob> listJobs();

	/**
	 * Finds the properties of a given job.
	 * @param jobId the job's ID
	 * @return the properties, or null if this job was not found
	 */
	ScheduledJob findJobProperties( String jobId );
}
