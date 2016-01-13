/**
 * Copyright 2015-2016 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.dm.rest.services.cors;

import javax.ws.rs.core.Response;

import org.junit.Assert;

import org.junit.Test;
import org.mockito.Mockito;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ResponseCorsFilterTest {

	@Test
	public void testHeadersUpdate_noMaj() {

		ResponseCorsFilter filter = new ResponseCorsFilter();
		ContainerRequest req = Mockito.mock( ContainerRequest.class );
		ContainerResponse resp = Mockito.mock( ContainerResponse.class );

		Mockito.when( resp.getResponse()).thenReturn( Response.ok().build());
		Mockito.verifyZeroInteractions( req );
		ContainerResponse result = filter.filter( req, resp );

		Assert.assertNotNull( result );
		Mockito.verify( req, Mockito.times( 1 )).getHeaderValue( Mockito.anyString());
	}


	@Test
	public void testHeadersUpdate_headersAppended() {

		ResponseCorsFilter filter = new ResponseCorsFilter();
		ContainerRequest req = Mockito.mock( ContainerRequest.class );
		ContainerResponse resp = Mockito.mock( ContainerResponse.class );

		Mockito.when( resp.getResponse()).thenReturn( Response.ok().build());
		Mockito.when( req.getHeaderValue( Mockito.anyString())).thenReturn( "hello" );

		Mockito.verifyZeroInteractions( req );
		ContainerResponse result = filter.filter( req, resp );

		Assert.assertNotNull( result );
		Mockito.verify( req, Mockito.times( 1 )).getHeaderValue( Mockito.anyString());
	}
}
