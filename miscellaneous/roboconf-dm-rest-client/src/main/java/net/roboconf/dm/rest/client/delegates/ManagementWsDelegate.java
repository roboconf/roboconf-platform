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

package net.roboconf.dm.rest.client.delegates;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status.Family;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.multipart.FormDataMultiPart;
import com.sun.jersey.multipart.file.FileDataBodyPart;

import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.dm.rest.client.WsClient;
import net.roboconf.dm.rest.client.exceptions.ManagementWsException;
import net.roboconf.dm.rest.commons.UrlConstants;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ManagementWsDelegate {

	private final WebResource resource;
	private final Logger logger;
	private final WsClient wsClient;


	/**
	 * Constructor.
	 * @param resource a web resource
	 * @param the WS client
	 */
	public ManagementWsDelegate( WebResource resource, WsClient wsClient ) {
		this.resource = resource;
		this.wsClient = wsClient;
		this.logger = Logger.getLogger( getClass().getName());
	}


	// Templates


	/**
	 * Uploads a ZIP file and loads its application template.
	 * @param applicationFile a ZIP archive file
	 * @throws ManagementWsException if a problem occurred with the applications management
	 * @throws IOException if the file was not found or is invalid
	 */
	public void uploadZippedApplicationTemplate( File applicationFile ) throws ManagementWsException, IOException {

		if( applicationFile == null
				|| ! applicationFile.exists()
				|| ! applicationFile.isFile())
			throw new IOException( "Expected an existing file as parameter." );

		this.logger.finer( "Loading an application from " + applicationFile.getAbsolutePath() + "..." );

		FormDataMultiPart part = new FormDataMultiPart();
		part.bodyPart( new FileDataBodyPart( "file", applicationFile, MediaType.APPLICATION_OCTET_STREAM_TYPE));

		WebResource path = this.resource.path( UrlConstants.APPLICATIONS ).path( "templates" );
		ClientResponse response = this.wsClient.createBuilder( path )
				.type( MediaType.MULTIPART_FORM_DATA_TYPE )
				.post( ClientResponse.class, part );

		if( Family.SUCCESSFUL != response.getStatusInfo().getFamily()) {
			String value = response.getEntity( String.class );
			this.logger.finer( response.getStatusInfo() + ": " + value );
			throw new ManagementWsException( response.getStatusInfo().getStatusCode(), value );
		}

		this.logger.finer( String.valueOf( response.getStatusInfo()));
	}


	/**
	 * Loads an application template from a directory located on the DM's file system.
	 * @param localFilePath the file path of a directory containing the application on the DM's machine
	 * @throws ManagementWsException if a problem occurred with the applications management
	 */
	public void loadUnzippedApplicationTemplate( String localFilePath ) throws ManagementWsException {
		this.logger.finer( "Loading an application from a local directory: " + localFilePath );

		WebResource path = this.resource.path( UrlConstants.APPLICATIONS ).path( "templates" ).path( "local" );
		if( localFilePath != null )
			path = path.queryParam( "local-file-path", localFilePath );

		ClientResponse response = this.wsClient.createBuilder( path )
						.type( MediaType.APPLICATION_JSON )
						.post( ClientResponse.class );

		if( Family.SUCCESSFUL != response.getStatusInfo().getFamily()) {
			String value = response.getEntity( String.class );
			this.logger.finer( response.getStatusInfo() + ": " + value );
			throw new ManagementWsException( response.getStatusInfo().getStatusCode(), value );
		}

		this.logger.finer( String.valueOf( response.getStatusInfo()));
	}


	/**
	 * Loads an application template from an URL.
	 * @param url an URL pointing to a ZIP file
	 * @throws ManagementWsException if a problem occurred with the applications management
	 */
	public void loadZippedApplicationTemplate( String url ) throws ManagementWsException {
		this.logger.finer( "Loading an application from an URL: " + url );

		WebResource path = this.resource.path( UrlConstants.APPLICATIONS ).path( "templates" ).path( "url" );
		if( url != null )
			path = path.queryParam( "url", url );

		ClientResponse response = this.wsClient.createBuilder( path )
						.type( MediaType.APPLICATION_JSON )
						.post( ClientResponse.class );

		if( Family.SUCCESSFUL != response.getStatusInfo().getFamily()) {
			String value = response.getEntity( String.class );
			this.logger.finer( response.getStatusInfo() + ": " + value );
			throw new ManagementWsException( response.getStatusInfo().getStatusCode(), value );
		}

		this.logger.finer( String.valueOf( response.getStatusInfo()));
	}


	/**
	 * Lists application templates.
	 * @param exactName if specified, only the templates with this name will be returned (null to match all)
	 * <p>
	 * We only consider the application name, not the display name.
	 * It means that the parameter should not contain special characters.
	 * </p>
	 *
	 * @param exactQualifier the exact qualifier to search (null to match all)
	 * @return a non-null list of application templates
	 * @throws ManagementWsException if a problem occurred with the applications management
	 */
	public List<ApplicationTemplate> listApplicationTemplates( String exactName, String exactQualifier )
	throws ManagementWsException {

		// Log
		if( this.logger.isLoggable( Level.FINER )) {
			if( exactName == null && exactQualifier == null ) {
				this.logger.finer( "Listing all the application templates." );

			} else {
				StringBuilder sb = new StringBuilder( "Listing/finding the application templates" );
				if( exactName != null ) {
					sb.append( " with name = " );
					sb.append( exactName );
				}

				if( exactQualifier != null ) {
					if( exactName != null )
						sb.append( " and" );

					sb.append( " qualifier = " );
					sb.append( exactQualifier );
				}

				sb.append( "." );
				this.logger.finer( sb.toString());
			}
		}

		// Search
		WebResource path = this.resource.path( UrlConstants.APPLICATIONS ).path( "templates" );
		if( exactName != null )
			path = path.queryParam( "name", exactName );

		if( exactQualifier != null )
			path = path.queryParam( "qualifier", exactQualifier );

		List<ApplicationTemplate> result = this.wsClient.createBuilder( path )
				.accept( MediaType.APPLICATION_JSON )
				.get( new GenericType<List<ApplicationTemplate>> () {});

		if( result != null )
			this.logger.finer( result.size() + " application templates were found on the DM." );
		else
			this.logger.finer( "No application template was found on the DM." );

		return result != null ? result : new ArrayList<ApplicationTemplate> ();
	}


	/**
	 * Lists all the application templates.
	 * <p>
	 * Equivalent to <code>listApplicationTemplates( null, null )</code>.
	 * </p>
	 *
	 * @return a non-null list of application templates
	 * @throws ManagementWsException if a problem occurred with the applications management
	 */
	public List<ApplicationTemplate> listApplicationTemplates() throws ManagementWsException {
		return listApplicationTemplates( null, null );
	}


	/**
	 * Deletes an application.
	 * @param templateName the template name
	 * @param templateQualifier the template qualifier
	 * @throws ManagementWsException if a problem occurred with the applications management
	 */
	public void deleteApplicationTemplate( String templateName, String templateQualifier ) throws ManagementWsException {
		this.logger.finer( "Removing application template " + templateName + "..." );

		WebResource path = this.resource
				.path( UrlConstants.APPLICATIONS ).path( "templates" )
				.path( templateName ).path( templateQualifier );

		ClientResponse response = this.wsClient.createBuilder( path ).delete( ClientResponse.class );
		String text = response.getEntity( String.class );
		this.logger.finer( text );
		if( Family.SUCCESSFUL != response.getStatusInfo().getFamily())
			throw new ManagementWsException( response.getStatusInfo().getStatusCode(), text );
	}


	// Applications


	/**
	 * Creates an application from a template.
	 * @param applicationName the application name
	 * @param templateName the template's name
	 * @param templateQualifier the template's qualifier
	 * @return the created application
	 * @throws ManagementWsException if a problem occurred with the applications management
	 */
	public Application createApplication( String applicationName, String templateName, String templateQualifier )
	throws ManagementWsException {

		this.logger.finer( "Creating application " + applicationName + " from " + templateName + " - " + templateQualifier + "..." );
		ApplicationTemplate tpl = new ApplicationTemplate( templateName ).version( templateQualifier );
		Application app = new Application( applicationName, tpl );

		WebResource path = this.resource.path( UrlConstants.APPLICATIONS );
		ClientResponse response = this.wsClient.createBuilder( path )
				.type( MediaType.APPLICATION_JSON )
				.post( ClientResponse.class, app );

		if( Family.SUCCESSFUL != response.getStatusInfo().getFamily())
			throw new ManagementWsException( response.getStatusInfo().getStatusCode(), "" );

		Application result = response.getEntity( Application.class );
		return result;
	}


	/**
	 * Lists applications.
	 * @param exactName if not null, the result list will contain at most one application (with this name)
	 * @return a non-null list of applications
	 * @throws ManagementWsException if a problem occurred with the applications management
	 */
	public List<Application> listApplications( String exactName ) throws ManagementWsException {

		if( exactName != null )
			this.logger.finer( "List/finding the application named " + exactName + "." );
		else
			this.logger.finer( "Listing all the applications." );

		WebResource path = this.resource.path( UrlConstants.APPLICATIONS );
		if( exactName != null )
			path = path.queryParam( "name", exactName );

		List<Application> result = this.wsClient.createBuilder( path )
				.accept( MediaType.APPLICATION_JSON )
				.get( new GenericType<List<Application>> () {});

		if( result != null )
			this.logger.finer( result.size() + " applications were found on the DM." );
		else
			this.logger.finer( "No application was found on the DM." );

		return result != null ? result : new ArrayList<Application> ();
	}


	/**
	 * Lists all the applications.
	 * <p>
	 * Equivalent to <code>listApplications( null )</code>.
	 * </p>
	 *
	 * @return a non-null list of applications
	 * @throws ManagementWsException if a problem occurred with the applications management
	 */
	public List<Application> listApplications() throws ManagementWsException {
		return listApplications( null );
	}


	/**
	 * Shutdowns an application.
	 * @param applicationName the application name
	 * @throws ManagementWsException if a problem occurred with the applications management
	 */
	public void shutdownApplication( String applicationName ) throws ManagementWsException {
		this.logger.finer( "Removing application " + applicationName + "..." );

		WebResource path = this.resource
					.path( UrlConstants.APPLICATIONS )
					.path( applicationName )
					.path( "shutdown" );

		ClientResponse response = this.wsClient.createBuilder( path ).post( ClientResponse.class );
		String text = response.getEntity( String.class );
		this.logger.finer( text );
		if( Family.SUCCESSFUL != response.getStatusInfo().getFamily())
			throw new ManagementWsException( response.getStatusInfo().getStatusCode(), text );
	}


	/**
	 * Deletes an application.
	 * @param applicationName the application name
	 * @throws ManagementWsException if a problem occurred with the applications management
	 */
	public void deleteApplication( String applicationName ) throws ManagementWsException {
		this.logger.finer( "Removing application " + applicationName + "..." );

		WebResource path = this.resource.path( UrlConstants.APPLICATIONS ).path( applicationName );
		ClientResponse response = this.wsClient.createBuilder( path ).delete( ClientResponse.class );

		String text = response.getEntity( String.class );
		this.logger.finer( text );
		if( Family.SUCCESSFUL != response.getStatusInfo().getFamily())
			throw new ManagementWsException( response.getStatusInfo().getStatusCode(), text );
	}
}
