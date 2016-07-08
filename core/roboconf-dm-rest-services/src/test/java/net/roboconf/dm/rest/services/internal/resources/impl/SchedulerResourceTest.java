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

package net.roboconf.dm.rest.services.internal.resources.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import net.roboconf.core.model.runtime.ScheduledJob;
import net.roboconf.dm.scheduler.IScheduler;

/**
 * @author Vincent Zurczak - Linagora
 */
public class SchedulerResourceTest {

	private SchedulerResource resource;
	private IScheduler scheduler;


	@Before
	public void prepare() {
		this.resource = new SchedulerResource();
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
	public void testSaveJob_ok() throws Exception {

		Mockito
			.when( this.scheduler.saveJob( Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
			.thenReturn( new ScheduledJob( "some-id" ));

		Response resp = this.resource.saveJob( null, "job", "app", "cmd", "cron" );
		Assert.assertEquals( Status.OK.getStatusCode(), resp.getStatus());
		Assert.assertTrue( resp.getEntity() instanceof ScheduledJob );
		Assert.assertEquals( "some-id", ((ScheduledJob) resp.getEntity()).getJobId());
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
		Assert.assertEquals( 0, this.resource.listJobs().size());
	}


	@Test
	public void testListJobs_ok() {

		List<ScheduledJob> result = new ArrayList<>( 2 );
		result.add( new ScheduledJob( "id1" ));
		result.add( new ScheduledJob( "id2" ));

		Mockito
			.when( this.scheduler.listJobs())
			.thenReturn( result );

		Assert.assertEquals( 2, this.resource.listJobs().size());
		Assert.assertEquals( result, this.resource.listJobs());
	}
}
