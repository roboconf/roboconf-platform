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

package net.roboconf.dm.rest.services.internal.resources;

import java.io.InputStream;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;

import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.dm.rest.commons.UrlConstants;

/**
 * The REST API to manipulate applications on the DM.
 * <p>
 * Implementing classes have to define the "Path" annotation
 * on the class. Use {@link #PATH}.
 * </p>
 *
 * @author Vincent Zurczak - Linagora
 * @author Pierre Bourret - Université Joseph Fourier
 */
public interface IManagementResource {

	String PATH = "/" + UrlConstants.APPLICATIONS;


	// Application Templates


	/**
	 * Loads an application template from an uploaded ZIP file.
	 * @param uploadedInputStream the uploaded archive
	 * @param fileDetail the file details
	 * @return a response
	 *
	 * @HTTP 200 Everything went fine.
	 * @HTTP 409 Conflicts occurred with deployment targets.
	 * @HTTP 406 Invalid application template.
	 * @HTTP 403 An application template with this name already exists.
	 * @HTTP 401 The application template could not be registered (e.g. DM not ready).
	 */
	@POST
	@Path("/templates")
	@Consumes( MediaType.MULTIPART_FORM_DATA )
	@Produces( MediaType.APPLICATION_JSON )
	Response loadUploadedZippedApplicationTemplate(
			@FormDataParam("file") InputStream uploadedInputStream,
			@FormDataParam("file") FormDataContentDisposition fileDetail );


	/**
	 * Loads a zipped application template from an URL.
	 * <p>
	 * Local (file:/) and standard remote (http:/) URLs are supported.
	 * Maven URLs are also supported. Please, refer to PAX URL's web site for more details:
	 * https://ops4j1.jira.com/wiki/display/paxurl/Mvn+Protocol
	 * </p>
	 * <p>
	 * This operation can be invoked as an example in the scope of continuous deployments.
	 * </p>
	 *
	 * @param url the URL of an application template archive (ZIP file)
	 * @return a response
	 *
	 * @HTTP 200 Everything went fine.
	 * @HTTP 409 Conflicts occurred with deployment targets.
	 * @HTTP 406 Invalid application template or invalid URL.
	 * @HTTP 403 An application template with this name already exists.
	 * @HTTP 401 The application template could not be registered (e.g. DM not ready).
	 */
	@POST
	@Path("/templates/url")
	@Consumes( MediaType.APPLICATION_JSON )
	@Produces( MediaType.APPLICATION_JSON )
	Response loadZippedApplicationTemplate( @QueryParam( "url" ) String url );


	/**
	 * Loads an application template from a local ZIP file.
	 * <p>
	 * A directory containing a Roboconf application (model + resources)
	 * could be fairly heavy (several hundreds of megabytes). We must
	 * consider the possibility someone uses a (S)FTP server (or some
	 * equivalent solution) to upload a file on the DM's machine.
	 * </p>
	 * <p>
	 * This operation covers this use case.<br>
	 * It will load an application from a file which was already uploaded
	 * on the DM's machine.
	 * </p>
	 *
	 * @param localFilePath the local file path of the application directory
	 * @return a response
	 *
	 * @HTTP 200 Everything went fine.
	 * @HTTP 409 Conflicts occurred with deployment targets.
	 * @HTTP 406 Invalid application template.
	 * @HTTP 403 An application template with this name already exists.
	 * @HTTP 401 The application template could not be registered (e.g. DM not ready).
	 */
	@POST
	@Path("/templates/local")
	@Consumes( MediaType.APPLICATION_JSON )
	@Produces( MediaType.APPLICATION_JSON )
	Response loadUnzippedApplicationTemplate( @QueryParam( "local-file-path" ) String localFilePath );


	/**
	 * Lists the application templates.
	 * @param exactName if specified, only the templates with this name will be returned (null to match all)
	 * <p>
	 * We only consider the application name, not the display name.
	 * It means that the parameter should not contain special characters.
	 * </p>
	 *
	 * @param exactQualifier the exact qualifier to search (null to match all)
	 * @param tag a tag to filter results (null to match all)
	 * @return a non-null list of application templates (with at most one element if "exactName" was set)
	 * @HTTP 200 Everything went fine.
	 */
	@GET
	@Path("/templates")
	@Produces( MediaType.APPLICATION_JSON )
	List<ApplicationTemplate> listApplicationTemplates(
			@QueryParam( "name" ) String exactName,
			@QueryParam( "qualifier" ) String exactQualifier,
			@QueryParam( "tag" ) String tag );


	/**
	 * Lists all the application templates.
	 * <p>
	 * Java utility, equivalent to <code>listApplicationTemplates( null, null )</code>.
	 * We do not annotate it for Jersey as listApplicationTemplates( String, String ) already is.
	 * </p>
	 *
	 * @return a non-null list of application templates
	 * @HTTP 200 Everything went fine.
	 */
	List<ApplicationTemplate> listApplicationTemplates();


	/**
	 * Deletes an application template.
	 * @param tplName the application template's name
	 * @param tplQualifier the application template's qualifier
	 * @return a response
	 *
	 * @HTTP 200 Everything went fine.
	 * @HTTP 404 The application template was not found.
	 * @HTTP 403 There are still applications attached to this template.
	 */
	@DELETE
	@Path("/templates/{name}/{qualifier}")
	@Produces( MediaType.APPLICATION_JSON )
	Response deleteApplicationTemplate( @PathParam("name") String tplName, @PathParam("qualifier") String tplQualifier );


	// Applications


	/**
	 * Creates a new application.
	 * @param app an application
	 * @return a response
	 *
	 * @HTTP 200 Everything went fine.
	 * @HTTP 404 The application's template was not found.
	 * @HTTP 403 An application with this name already exists.
	 * @HTTP 401 The application could not be created (e.g. DM not ready).
	 */
	@POST
	@Consumes( MediaType.APPLICATION_JSON )
	@Produces( MediaType.APPLICATION_JSON )
	Response createApplication( Application app );


	/**
	 * Lists applications.
	 * @param exactName if specified, only the application with this name will be returned in the list (null to match all)
	 * <p>
	 * We only consider the application name, not the display name.
	 * It means that the parameter should not contain special characters.
	 * </p>
	 *
	 * @return a non-null list of applications (with at most one element if "exactName" was set)
	 * @HTTP 200 Everything went fine.
	 */
	@GET
	@Produces( MediaType.APPLICATION_JSON )
	List<Application> listApplications( @QueryParam("name") String exactName );


	/**
	 * Lists all the applications.
	 * <p>
	 * Java utility, equivalent to <code>listApplications( null )</code>.
	 * We do not annotate it for Jersey as listApplications( String ) already is.
	 * </p>
	 *
	 * @return a non-null list of applications
	 * @HTTP 200 Everything went fine.
	 */
	List<Application> listApplications();


	/**
	 * Shutdowns an application.
	 * <p>
	 * Shutdown means removing everything Roboconf deployed or created.
	 * This operation does not delete the application from the model. It only destroys
	 * runtime resources.
	 * </p>
	 *
	 * @param applicationName the application name
	 * @return a response
	 *
	 * @HTTP 200 Everything went fine.
	 * @HTTP 404 The application was not found.
	 * @HTTP 403 The application could not be shutdown (e.g. messaging error).
	 */
	@POST
	@Path("/{name}/shutdown")
	@Produces( MediaType.APPLICATION_JSON )
	Response shutdownApplication( @PathParam("name") String applicationName );


	/**
	 * Deletes an application.
	 * @param applicationName the application name
	 * @return a response
	 *
	 * @HTTP 200 Everything went fine.
	 * @HTTP 404 The application was not found.
	 * @HTTP 403 The application could not be deleted (e.g. messaging error).
	 */
	@DELETE
	@Path("/{name}")
	@Produces( MediaType.APPLICATION_JSON )
	Response deleteApplication( @PathParam("name") String applicationName );



	// Images


	/**
	 * Uploads an image for a template/application.
	 * <p>
	 * If an image was already set, it is overridden by the new one.
	 * </p>
	 * @param name the name of the template/application.
	 * @param qualifier the qualifier of the template, or {@code null} for an application.
	 * @param image the uploaded image.
	 * @param fileDetail the image details.
	 * @return a response.
	 *
	 * @HTTP 200 Everything went fine.
	 * @HTTP 400 The application was not found or the image type or extension is invalid.
	 */
	@POST
	@Path("/image")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	Response setImage(	@QueryParam("name") String name,
						@QueryParam("qualifier") String qualifier,
						@FormDataParam("file") InputStream image,
						@FormDataParam("file") FormDataContentDisposition fileDetail);
}
