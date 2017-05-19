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

package net.roboconf.dm.rest.services.cors;

import java.util.LinkedHashMap;
import java.util.Map;

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

	public static final String CORS_REQ_HEADERS = "Access-Control-Request-Headers";
	public static final String ORIGIN = "Origin";

	static final String CORS_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
	static final String CORS_ALLOW_METHODS = "Access-Control-Allow-Methods";
	static final String CORS_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";
	static final String CORS_ALLOW_HEADERS = "Access-Control-Allow-Headers";

	static final String VALUE_ALLOWED_METHODS = "GET, DELETE, POST, PUT, OPTIONS";
	static final String VALUE_ALLOW_CREDENTIALS = "true";


	@Override
	public ContainerResponse filter( ContainerRequest req, ContainerResponse contResp ) {

		ResponseBuilder resp = Response.fromResponse( contResp.getResponse());
		Map<String,String> headers = buildHeaders(
				req.getHeaderValue( CORS_REQ_HEADERS ),
				req.getHeaderValue( ORIGIN ));

		for( Map.Entry<String,String> h : headers.entrySet())
			resp.header( h.getKey(), h.getValue());

		contResp.setResponse( resp.build());
		return contResp;
	}


	/**
	 * Finds the right headers to set on the response to prevent CORS issues.
	 * @param reqCorsHeader the headers related to CORS
	 * @return a non-null map
	 */
	public static Map<String,String> buildHeaders( String reqCorsHeader, String requestUri ) {

		Map<String,String> result = new LinkedHashMap<> ();
		result.put( CORS_ALLOW_ORIGIN, requestUri );
		result.put( CORS_ALLOW_METHODS, VALUE_ALLOWED_METHODS );
		result.put( CORS_ALLOW_CREDENTIALS, VALUE_ALLOW_CREDENTIALS );

		if( ! Utils.isEmptyOrWhitespaces( reqCorsHeader ))
			result.put( CORS_ALLOW_HEADERS, reqCorsHeader );

		return result;
	}
}
