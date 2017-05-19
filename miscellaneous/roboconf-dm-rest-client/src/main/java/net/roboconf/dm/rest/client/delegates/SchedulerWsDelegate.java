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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status.Family;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;

import net.roboconf.core.model.runtime.ScheduledJob;
import net.roboconf.dm.rest.client.WsClient;
import net.roboconf.dm.rest.client.exceptions.SchedulerWsException;
import net.roboconf.dm.rest.commons.UrlConstants;
import net.roboconf.dm.rest.commons.json.StringWrapper;

/**
 * @author Vincent Zurczak - Linagora
 */
public class SchedulerWsDelegate {

	private final WebResource resource;
	private final Logger logger;
	private final WsClient wsClient;


	/**
	 * Constructor.
	 * @param resource a web resource
	 * @param the WS client
	 */
	public SchedulerWsDelegate( WebResource resource, WsClient wsClient ) {
		this.resource = resource;
		this.wsClient = wsClient;
		this.logger = Logger.getLogger( getClass().getName());
	}


	/**
	 * Lists all the jobs.
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
	 * @return a non-null list of scheduled jobs
	 */
	public List<ScheduledJob> listAllJobs( String appName, String cmdName ) {

		StringBuilder sb = new StringBuilder( "Listing all the jobs." );
		if( appName != null ) {
			sb.append( " Application name: " );
			sb.append( appName );
			if( cmdName != null ) {
				sb.append( ". Command name: " );
				sb.append( cmdName );
			}

			sb.append( "." );
		}

		this.logger.finer( sb.toString());
		WebResource path = this.resource.path( UrlConstants.SCHEDULER );
		if( appName != null )
			path = path.queryParam( "app-name", appName );

		if( cmdName != null )
			path = path.queryParam( "cmd-name", cmdName );

		List<ScheduledJob> result =
				this.wsClient.createBuilder( path )
				.accept( MediaType.APPLICATION_JSON )
				.type( MediaType.APPLICATION_JSON )
				.get( new GenericType<List<ScheduledJob>> () {});

		if( result != null ) {
			this.logger.finer( result.size() + " jobs were found." );
		} else {
			this.logger.finer( "No scheduled job was found." );
			result = new ArrayList<>( 0 );
		}

		return result;
	}


	/**
	 * Creates or updates a	scheduled job.
	 * @param jobId the job's ID (null to create a new job)
	 * @param jobName the job's name
	 * @param appName the application's name
	 * @param cmdName the name of the commands file to execute
	 * @param cron the CRON expression to trigger the job
	 * @return the created (or updated) job
	 * @throws SchedulerWsException if the creation or the update failed
	 */
	public String createOrUpdateJob( String jobId, String jobName, String appName, String cmdName, String cron )
	throws SchedulerWsException {

		if( jobId == null )
			this.logger.finer( "Creating a new scheduled job."  );
		else
			this.logger.finer( "Updating the following scheduled job: " + jobId );

		WebResource path = this.resource.path( UrlConstants.SCHEDULER );
		if( jobId != null )
			path = path.queryParam( "job-id", jobId );

		path = path.queryParam( "job-name", jobName );
		path = path.queryParam( "app-name", appName );
		path = path.queryParam( "cmd-name", cmdName );
		path = path.queryParam( "cron", cron );

		ClientResponse response = this.wsClient.createBuilder( path )
						.accept( MediaType.APPLICATION_JSON )
						.post( ClientResponse.class );

		handleResponse( response );
		StringWrapper wrapper = response.getEntity( StringWrapper.class );
		return wrapper.toString();
	}


	/**
	 * Deletes a scheduled job.
	 * @param jobId the job's ID
	 * @throws SchedulerWsException if the deletion failed
	 */
	public void deleteJob( String jobId ) throws SchedulerWsException {

		this.logger.finer( "Deleting scheduled job: " + jobId  );

		WebResource path = this.resource.path( UrlConstants.SCHEDULER ).path( jobId );
		ClientResponse response = this.wsClient.createBuilder( path )
						.accept( MediaType.APPLICATION_JSON )
						.delete( ClientResponse.class );

		handleResponse( response );
	}


	/**
	 * Gets the properties of a scheduled job.
	 * @param jobId the job's ID
	 * @throws SchedulerWsException if the retrieving failed
	 */
	public ScheduledJob getJobProperties( String jobId ) throws SchedulerWsException {

		this.logger.finer( "Getting the properties of a scheduled job: " + jobId  );

		WebResource path = this.resource.path( UrlConstants.SCHEDULER ).path( jobId );
		ClientResponse response = this.wsClient.createBuilder( path )
						.accept( MediaType.APPLICATION_JSON )
						.get( ClientResponse.class );

		handleResponse( response );
		return response.getEntity( ScheduledJob.class );
	}


	private void handleResponse( ClientResponse response ) throws SchedulerWsException {

		if( Family.SUCCESSFUL != response.getStatusInfo().getFamily()) {
			String value = response.getEntity( String.class );
			this.logger.finer( response.getStatusInfo() + ": " + value );
			throw new SchedulerWsException( response.getStatusInfo().getStatusCode(), value );
		}
	}
}
