/**
 * Copyright 2014 Linagora, Université Joseph Fourier, Floralis
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

import java.io.InputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import net.roboconf.dm.rest.commons.UrlConstants;

import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;

/**
 * The REST API to debug various things.
 * <p>
 * Implementing classes have to define the "Path" annotation
 * on the class. Use {@link #PATH}.
 * </p>
 *
 * @author Vincent Zurczak - Linagora
 */
public interface IDebugResource {

	String PATH = "/" + UrlConstants.DEBUG;
	String FAKE_APP_NAME = "Fake Application";


	/**
	 * Loads a target.properties file and creates a fake application to test it.
	 * @param uploadedInputStream the uploaded file
	 * @param fileDetail the file details
	 * @return a response
	 */
	@POST
	@Path("/test-target")
	@Consumes( MediaType.MULTIPART_FORM_DATA )
	Response createTestForTargetProperties(
			@FormDataParam("file") InputStream uploadedInputStream,
			@FormDataParam("file") FormDataContentDisposition fileDetail );


	/**
	 * Checks the DM is correctly connected with the messaging server.
	 * @return a response
	 */
	@GET
	@Path("/check-dm")
	Response checkMessagingConnectionForTheDm();


	/**
	 * Checks the DM can correctly exchange with an agent through the messaging server.
	 * @return a response
	 */
	@GET
	@Path("/check-agent")
	Response checkMessagingConnectionWithAgent( @QueryParam("root-instance-name") String rootInstanceName );


	/**
	 * Runs a diagnostic for a given instance.
	 * <p>
	 * The diagnostic is based on the information hold by the DM, and not by the agent.
	 * </p>
	 *
	 * @return a response
	 */
	@GET
	@Path("/diagnostic-instance")
	Response diagnosticInstance( @QueryParam("instance-path") String instancePath );


	/**
	 * Runs a diagnostic for a given instance.
	 * <p>
	 * The diagnostic is based on the information hold by the DM, and not by the agent.
	 * </p>
	 *
	 * @return a response
	 */
	@GET
	@Path("/diagnostic-application")
	Response diagnosticApplication( @QueryParam("application-name") String applicationName );
}
