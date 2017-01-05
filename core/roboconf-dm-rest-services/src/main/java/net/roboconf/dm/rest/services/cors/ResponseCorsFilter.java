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

package net.roboconf.dm.rest.services.cors;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;

import net.roboconf.core.utils.Utils;

/**
 * A filter to handle CORS.
 * <p>
 * CORS means Cross-Origin Domain.<br>
 * We use this filter to allow our REST clients to be served from
 * a different domain than our REST API.
 * </p>
 * <p>
 * As an example, the REST API can be served from http://localhost:8080/rest
 * and our client be served from http://localhost. Without this filter, the REST invocations will not work.
 * </p>
 *
 * @author Vincent Zurczak - Linagora
 */
public class ResponseCorsFilter implements ContainerResponseFilter {

	@Override
	public ContainerResponse filter( ContainerRequest req, ContainerResponse contResp ) {

		ResponseBuilder resp = Response.fromResponse( contResp.getResponse());
		resp
		.header( "Access-Control-Allow-Origin", "*" )
		.header( "Access-Control-Allow-Methods", "GET, DELETE, POST, OPTIONS" );

		String reqHead = req.getHeaderValue( "Access-Control-Request-Headers" );
		if( ! Utils.isEmptyOrWhitespaces( reqHead )) {
			try {
				reqHead = URLEncoder.encode( reqHead, StandardCharsets.UTF_8.displayName());

			} catch( UnsupportedEncodingException e ) {
				Logger logger = Logger.getLogger( getClass().getName());
				Utils.logException( logger, e );
			}

			resp.header( "Access-Control-Allow-Headers", reqHead );
		}

		contResp.setResponse( resp.build());
		return contResp;
	}
}
