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

package net.roboconf.dm.rest.services.internal.utils;

import static net.roboconf.core.errors.ErrorCode.CMD_CANNOT_HAVE_ANY_PARENT;
import static net.roboconf.core.errors.ErrorCode.REST_APP_EXEC_ERROR;
import static net.roboconf.core.errors.ErrorDetails.name;
import static net.roboconf.core.errors.ErrorDetails.value;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.junit.Assert;
import org.junit.Test;

import net.roboconf.core.errors.RoboconfError;
import net.roboconf.core.errors.i18n.TranslationBundle;
import net.roboconf.dm.management.exceptions.InvalidApplicationException;
import net.roboconf.dm.rest.services.internal.errors.RestError;

/**
 * @author Vincent Zurczak - Linagora
 */
public class RestServicesUtilsTest {

	private static TranslationBundle tb = new TranslationBundle( null );


	@Test
	public void testHandleError_noException() {

		Response response = RestServicesUtils.handleError(
				Status.FORBIDDEN,
				new RestError( REST_APP_EXEC_ERROR ),
				null ).build();

		Assert.assertEquals( Status.FORBIDDEN.getStatusCode(), response.getStatus());
		Assert.assertEquals(
				"{\"reason\":\"" + tb.getString( REST_APP_EXEC_ERROR.toString()) + "\"}",
				response.getEntity());
	}


	@Test
	public void testHandleError_noException_withDetails() {

		Response response = RestServicesUtils.handleError(
				Status.FORBIDDEN,
				new RestError( REST_APP_EXEC_ERROR, name( "toto" )),
				null ).build();

		Assert.assertEquals( Status.FORBIDDEN.getStatusCode(), response.getStatus());
		Assert.assertEquals(
				"{\"reason\":\"" + tb.getString( REST_APP_EXEC_ERROR.toString()) + " Name: toto\"}",
				response.getEntity());
	}


	@Test
	public void testHandleError_withException() {

		Exception e = new RuntimeException( "hi" );
		Response response = RestServicesUtils.handleError(
				Status.FORBIDDEN,
				new RestError( REST_APP_EXEC_ERROR, e ),
				null ).build();

		StringBuilder expected = new StringBuilder();
		expected.append( "{\"reason\":\"" );
		expected.append( tb.getString( REST_APP_EXEC_ERROR.toString()) );
		expected.append( " Exception Type: java.lang.RuntimeException. Log Reference: " );

		Assert.assertEquals( Status.FORBIDDEN.getStatusCode(), response.getStatus());
		Assert.assertTrue( response.getEntity().toString().startsWith( expected.toString()));
		Assert.assertTrue( response.getEntity().toString().endsWith( "\"}" ));
	}


	@Test
	public void testHandleError_withException_withDetails() {

		Exception e = new RuntimeException( "hi" );
		Response response = RestServicesUtils.handleError(
				Status.FORBIDDEN,
				new RestError( REST_APP_EXEC_ERROR, e, name( "titi" )),
				null ).build();

		StringBuilder expected = new StringBuilder();
		expected.append( "{\"reason\":\"" );
		expected.append( tb.getString( REST_APP_EXEC_ERROR.toString()));
		expected.append( " Name: titi. Exception Type: java.lang.RuntimeException. Log Reference: " );

		Assert.assertEquals( Status.FORBIDDEN.getStatusCode(), response.getStatus());
		Assert.assertTrue( response.getEntity().toString().startsWith( expected.toString()));
		Assert.assertTrue( response.getEntity().toString().endsWith( "\"}" ));
	}


	@Test
	public void testHandleError_withInvalidApplicationException_withDetails() {

		RoboconfError error = new RoboconfError( CMD_CANNOT_HAVE_ANY_PARENT, value( "t" ));
		Exception e = new InvalidApplicationException( error );

		Response response = RestServicesUtils.handleError(
				Status.FORBIDDEN,
				new RestError( REST_APP_EXEC_ERROR, e, name( "titi" )),
				null ).build();

		Assert.assertEquals( Status.FORBIDDEN.getStatusCode(), response.getStatus());

		StringBuilder expected = new StringBuilder();
		expected.append( "{\"reason\":\"" );
		expected.append( tb.getString( REST_APP_EXEC_ERROR.toString()));
		expected.append( " Name: titi. " );
		expected.append( "Exception Type: net.roboconf.dm.management.exceptions.InvalidApplicationException." );
		expected.append( " Log Reference: " );

		Assert.assertTrue( response.getEntity().toString().startsWith( expected.toString()));

		expected = new StringBuilder();
		expected.append( "\\n\\n" );
		expected.append( tb.getString( CMD_CANNOT_HAVE_ANY_PARENT.toString()));
		expected.append( " Value: t\"}" );

		Assert.assertTrue( response.getEntity().toString().endsWith( expected.toString()));
	}
}
