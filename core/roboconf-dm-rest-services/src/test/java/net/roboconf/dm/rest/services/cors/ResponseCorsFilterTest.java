/**
 * Copyright 2015-2017 Linagora, Université Joseph Fourier, Floralis
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

import static net.roboconf.dm.rest.services.cors.ResponseCorsFilter.CORS_ALLOW_CREDENTIALS;
import static net.roboconf.dm.rest.services.cors.ResponseCorsFilter.CORS_ALLOW_HEADERS;
import static net.roboconf.dm.rest.services.cors.ResponseCorsFilter.CORS_ALLOW_METHODS;
import static net.roboconf.dm.rest.services.cors.ResponseCorsFilter.CORS_ALLOW_ORIGIN;
import static net.roboconf.dm.rest.services.cors.ResponseCorsFilter.CORS_REQ_HEADERS;
import static net.roboconf.dm.rest.services.cors.ResponseCorsFilter.ORIGIN;
import static net.roboconf.dm.rest.services.cors.ResponseCorsFilter.VALUE_ALLOWED_METHODS;
import static net.roboconf.dm.rest.services.cors.ResponseCorsFilter.VALUE_ALLOW_CREDENTIALS;

import java.util.Arrays;

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
	public void testHeadersUpdate() {

		final String origin = "http://something";
		final String reqHeader = "POST";

		ResponseCorsFilter filter = new ResponseCorsFilter();
		ContainerRequest req = Mockito.mock( ContainerRequest.class );

		// There are request headers
		ContainerResponse resp = new ContainerResponse( null, null, null );
		Mockito.verifyZeroInteractions( req );
		Mockito.when( req.getHeaderValue( ORIGIN )).thenReturn( origin );
		Mockito.when( req.getHeaderValue( CORS_REQ_HEADERS )).thenReturn( reqHeader );

		ContainerResponse result = filter.filter( req, resp );

		Assert.assertNotNull( result );
		Assert.assertEquals( 4, result.getHttpHeaders().size());
		Assert.assertEquals( Arrays.asList( VALUE_ALLOW_CREDENTIALS ), result.getHttpHeaders().get( CORS_ALLOW_CREDENTIALS ));
		Assert.assertEquals( Arrays.asList( reqHeader ), result.getHttpHeaders().get( CORS_ALLOW_HEADERS ));
		Assert.assertEquals( Arrays.asList( VALUE_ALLOWED_METHODS ), result.getHttpHeaders().get( CORS_ALLOW_METHODS ));
		Assert.assertEquals( Arrays.asList( origin ), result.getHttpHeaders().get( CORS_ALLOW_ORIGIN ));

		Mockito.verify( req ).getHeaderValue( CORS_REQ_HEADERS );
		Mockito.verify( req ).getHeaderValue( ORIGIN );
		Mockito.verifyNoMoreInteractions( req );

		// No request header
		resp = new ContainerResponse( null, null, null );
		Mockito.reset( req );
		Mockito.when( req.getHeaderValue( ORIGIN )).thenReturn( "http://something" );

		result = filter.filter( req, resp );

		Assert.assertNotNull( result );
		Assert.assertEquals( 3, result.getHttpHeaders().size());
		Assert.assertEquals( Arrays.asList( VALUE_ALLOW_CREDENTIALS ), result.getHttpHeaders().get( CORS_ALLOW_CREDENTIALS ));
		Assert.assertEquals( Arrays.asList( VALUE_ALLOWED_METHODS ), result.getHttpHeaders().get( CORS_ALLOW_METHODS ));
		Assert.assertEquals( Arrays.asList( origin ), result.getHttpHeaders().get( CORS_ALLOW_ORIGIN ));

		Mockito.verify( req ).getHeaderValue( ResponseCorsFilter.CORS_REQ_HEADERS );
		Mockito.verify( req ).getHeaderValue( ResponseCorsFilter.ORIGIN );
		Mockito.verifyNoMoreInteractions( req );
	}
}
