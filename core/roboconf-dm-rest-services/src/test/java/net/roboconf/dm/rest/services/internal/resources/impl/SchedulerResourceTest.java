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

package net.roboconf.dm.rest.services.internal.resources.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import net.roboconf.core.model.runtime.ScheduledJob;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.rest.commons.json.StringWrapper;
import net.roboconf.dm.scheduler.IScheduler;

/**
 * @author Vincent Zurczak - Linagora
 */
public class SchedulerResourceTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private SchedulerResource resource;
	private IScheduler scheduler;


	@Before
	public void prepare() throws Exception {

		// Simple manager
		Manager manager = new Manager();
		manager.configurationMngr().setWorkingDirectory( this.folder.newFolder());

		// Create the scheduler
		this.resource = new SchedulerResource( manager );
		this.scheduler = Mockito.mock( IScheduler.class );
		this.resource.scheduler = this.scheduler;
	}


	@Test
	public void testSaveJob_noScheduler() {

		this.resource.scheduler = null;
		Response resp = this.resource.saveJob( null, "job", "app", "cmd", "cron" );
		Assert.assertEquals( Status.FORBIDDEN.getStatusCode(), resp.getStatus());
	}


	@Test
	public void testSaveJob_inexistingApplicationOrCommand() throws Exception {

		Mockito
			.doThrow( new IllegalArgumentException( "For test" ))
			.when( this.scheduler ).saveJob( Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());

		Response resp = this.resource.saveJob( null, "job", "app", "cmd", "cron" );
		Assert.assertEquals( Status.NOT_FOUND.getStatusCode(), resp.getStatus());
	}


	@Test
	public void testSaveJob_ok() throws Exception {

		Mockito
			.when( this.scheduler.saveJob( Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
			.thenReturn( "some-id" );

		Response resp = this.resource.saveJob( null, "job", "app", "cmd", "cron" );
		Assert.assertEquals( Status.OK.getStatusCode(), resp.getStatus());
		Assert.assertTrue( resp.getEntity() instanceof StringWrapper );
		Assert.assertEquals( "some-id", ((StringWrapper) resp.getEntity()).toString());
	}


	@Test
	public void testSaveJob_error() throws Exception {

		Mockito
			.doThrow( new IOException( "For test" ))
			.when( this.scheduler ).saveJob( Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());

		Response resp = this.resource.saveJob( null, "job", "app", "cmd", "cron" );
		Assert.assertEquals( Status.BAD_REQUEST.getStatusCode(), resp.getStatus());
	}


	@Test
	public void testDeleteJob_noScheduler() {

		this.resource.scheduler = null;
		Response resp = this.resource.deleteJob( "job" );
		Assert.assertEquals( Status.FORBIDDEN.getStatusCode(), resp.getStatus());
	}


	@Test
	public void testDeleteJob_ok() {

		Response resp = this.resource.deleteJob( "job" );
		Assert.assertEquals( Status.OK.getStatusCode(), resp.getStatus());
	}


	@Test
	public void testDeleteJob_error() throws Exception {

		Mockito
			.doThrow( new IOException( "For test" ))
			.when( this.scheduler ).deleteJob( Mockito.anyString());

		Response resp = this.resource.deleteJob( "job" );
		Assert.assertEquals( Status.INTERNAL_SERVER_ERROR.getStatusCode(), resp.getStatus());
	}


	@Test
	public void testGetJobDetails_noScheduler() {

		this.resource.scheduler = null;
		Response resp = this.resource.findJobProperties( "job" );
		Assert.assertEquals( Status.FORBIDDEN.getStatusCode(), resp.getStatus());
	}


	@Test
	public void testGetJobDetails_notFound() {

		Response resp = this.resource.findJobProperties( "job" );
		Assert.assertEquals( Status.NOT_FOUND.getStatusCode(), resp.getStatus());
	}


	@Test
	public void testGetJobDetails_ok() {

		Mockito
			.when( this.scheduler.findJobProperties( Mockito.anyString()))
			.thenReturn( new ScheduledJob( "job-id" ));

		Response resp = this.resource.findJobProperties( "job-id" );
		Assert.assertEquals( Status.OK.getStatusCode(), resp.getStatus());
		Assert.assertTrue( resp.getEntity() instanceof ScheduledJob );
	}


	@Test
	public void testListJobs_noScheduler() {

		this.resource.scheduler = null;
		Assert.assertEquals( 0, this.resource.listJobs( null, null ).size());
	}


	@Test
	public void testListJobs_ok() {

		// Prepare the mock
		List<ScheduledJob> result = new ArrayList<> ();
		for( int i=0; i<3; i++ ) {
			for( int j=0; j<4; j++ ) {

				ScheduledJob job = new ScheduledJob( "job-id-" + i + j );
				job.setJobName( "Job " + i + j );
				job.setCron( "some cron that we will not validate since we mock the scheduler" );
				job.setAppName( "App-" + i );
				job.setCmdName( "cmd-" + j );
				result.add( job );
			}
		}

		// And a duplicate job (but with a different name for readability)
		ScheduledJob customJob = new ScheduledJob( "new-job-id" );
		customJob.setJobName( "Custom Job" );
		customJob.setCron( "some cron that we will not validate since we mock the scheduler" );
		customJob.setAppName( "App-2" );
		customJob.setCmdName( "cmd-2" );
		result.add( customJob );

		Mockito
			.when( this.scheduler.listJobs())
			.thenReturn( result );

		// Get all the jobs
		Assert.assertEquals( result, this.resource.listJobs( null, null ));

		// Get only the jobs from "App-2"
		List<ScheduledJob> got = this.resource.listJobs( "App-2", null );
		Set<String> jobNames = new HashSet<> ();
		for( ScheduledJob gotJob : got ) {
			Assert.assertEquals( "App-2", gotJob.getAppName());
			jobNames.add( gotJob.getJobName());
		}

		Assert.assertEquals( 5, got.size());
		Assert.assertEquals( 5, jobNames.size());
		Assert.assertTrue( jobNames.contains( "Custom Job" ));

		// Get only the jobs from "App-2" and "cmd-2"
		got = this.resource.listJobs( "App-2", "cmd-2" );
		jobNames.clear();
		for( ScheduledJob gotJob : got ) {
			Assert.assertEquals( "App-2", gotJob.getAppName());
			jobNames.add( gotJob.getJobName());
		}

		Assert.assertEquals( 2, got.size());
		Assert.assertEquals( 2, jobNames.size());
		Assert.assertTrue( jobNames.contains( "Custom Job" ));

		// Get only the jobs from "App-2" and "cmd-1"
		Assert.assertEquals( 1, this.resource.listJobs( "App-2", "cmd-1" ).size());

		// Get only the jobs from "App-2" and "cmd-that-does-not-exist"
		Assert.assertEquals( 0, this.resource.listJobs( "App-2", "cmd-that-does-not-exist" ).size());

		// Get only the jobs with "cmd-2"
		Assert.assertEquals( 0, this.resource.listJobs( null, "cmd-2" ).size());

		// Get only the jobs from "app-that-does-not-exist"
		Assert.assertEquals( 0, this.resource.listJobs( "app-that-does-not-exist", null ).size());
	}
}
