/**
 * Copyright 2014 Linagora, Université Joseph Fourier
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

package net.roboconf.dm.rest.api;

import java.io.InputStream;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import net.roboconf.core.model.runtime.Application;
import net.roboconf.dm.rest.UrlConstants;

import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;

/**
 * The REST API to manipulate applications on the DM.
 * <p>
 * Implementing classes have to define the "Path" annotation
 * on the class. Use {@link #PATH}.
 * </p>
 *
 * @author Vincent Zurczak - Linagora
 */
public interface IManagementWs {

	String PATH = "/" + UrlConstants.APPLICATIONS;


	/**
	 * Loads an application from a ZIP file.
	 * @param uploadedInputStream the uploaded archive
	 * @param fileDetail the file details
	 * @return a response
	 */
	@POST
	@Consumes( MediaType.MULTIPART_FORM_DATA )
	Response loadApplication(
			@FormDataParam("file") InputStream uploadedInputStream,
			@FormDataParam("file") FormDataContentDisposition fileDetail );

	/**
	 * Loads an application from a local ZIP file.
	 * <p>
	 * A ZIP file containing a Roboconf application (model + resources)
	 * could be fairly heavy (several hundreds of megabytes). We must
	 * consider the possibility someone uses a (S)FTP server (or some
	 * equivalent solution) to upload a file on the DM's machine.
	 * </p>
	 * <p>
	 * This operation covers this use case.<br />
	 * It will load an application from a file which was already uploaded
	 * on the DM's machine.
	 * </p>
	 *
	 * @param localFilePath the file path of a file located on the DM's machine
	 * @return a response
	 */
	@POST
	@Path("/local")
	Response loadApplication( String localFilePath );

	/**
	 * Lists the applications.
	 * @return a non-null list
	 */
	@GET
	@Produces( MediaType.APPLICATION_JSON )
	List<Application> listApplications();

	/**
	 * Gets the application.
	 * @param applicationName the application name
	 * @return the application object
	 */
	@GET
	@Path("/{name}")
	@Produces( MediaType.APPLICATION_JSON )
	Application getApplicationByName( @PathParam("name") String applicationName );

	/**
	 * Gets the model of an application (model files in a ZIP archive).
	 * @param applicationName the application name
	 * @return a response with the ZIP archive
	 */
	@GET
	@Path("/{name}/download")
	@Produces( "application/zip" )
	Response downloadApplicationModelData( @PathParam("name") String applicationName );

	/**
	 * Deletes an application.
	 * @param applicationName the application name
	 * @return a response
	 */
	@DELETE
	@Path("/{name}")
	@Produces( MediaType.APPLICATION_JSON )
	Response deleteApplication( @PathParam("name") String applicationName );

	/**
	 * Starts an application.
	 * @param applicationName the application name
	 * @return a response
	 */
	@POST
	@Path("/{name}/start")
	@Produces( MediaType.APPLICATION_JSON )
	Response startApplication( @PathParam("name") String applicationName );

	/**
	 * Stops an application.
	 * @param applicationName the application name
	 * @return a response
	 */
	@POST
	@Path("/{name}/stop")
	@Produces( MediaType.APPLICATION_JSON )
	Response stopApplication( @PathParam("name") String applicationName );
}
