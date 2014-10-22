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

package net.roboconf.dm.rest.client.delegates;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status.Family;

import net.roboconf.core.model.runtime.Application;
import net.roboconf.dm.rest.client.exceptions.ManagementException;
import net.roboconf.dm.rest.commons.UrlConstants;

import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.multipart.FormDataMultiPart;
import com.sun.jersey.multipart.file.FileDataBodyPart;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ManagementWsDelegate {

	private final WebResource resource;
	private final Logger logger;


	/**
	 * Constructor.
	 * @param resource a web resource
	 */
	public ManagementWsDelegate( WebResource resource ) {
		this.resource = resource;
		this.logger = Logger.getLogger( getClass().getName());
	}


	/**
	 * Uploads a ZIP file and loads its application.
	 * @param applicationFile a ZIP archive file
	 * @throws UniformInterfaceException if something went wrong
	 * @throws ClientHandlerException if something went wrong
	 * @throws ManagementException if a problem occurred with the applications management
	 * @throws IOException if the file was not found or is invalid
	 */
	public void loadApplication( File applicationFile ) throws ManagementException, IOException {

		if( applicationFile == null
				|| ! applicationFile.exists()
				|| ! applicationFile.isFile())
			throw new IOException( "Expected an existing file as parameter." );

		this.logger.finer( "Loading an application from " + applicationFile.getAbsolutePath() + "..." );

		FormDataMultiPart part = new FormDataMultiPart();
		part.bodyPart( new FileDataBodyPart( "file", applicationFile, MediaType.APPLICATION_OCTET_STREAM_TYPE));

		ClientResponse response = this.resource
				.path( UrlConstants.APPLICATIONS )
				.type( MediaType.MULTIPART_FORM_DATA_TYPE )
				.post( ClientResponse.class, part );

		if( Family.SUCCESSFUL != response.getStatusInfo().getFamily()) {
			String value = response.getEntity( String.class );
			this.logger.finer( response.getStatusInfo() + ": " + value );
			throw new ManagementException( response.getStatusInfo().getStatusCode(), value );
		}

		this.logger.finer( String.valueOf( response.getStatusInfo()));
	}


	/**
	 * Loads an application from a file which was already uploaded on the DM's machine.
	 * @param applicationFile a ZIP archive file
	 * @throws UniformInterfaceException if something went wrong
	 * @throws ClientHandlerException if something went wrong
	 * @throws ManagementException if a problem occurred with the applications management
	 */
	public void loadApplication( String remoteFilePath ) throws ManagementException {
		this.logger.finer( "Loading an already-uploaded application. " + remoteFilePath );

		WebResource path = this.resource.path( UrlConstants.APPLICATIONS ).path( "local" );
		if( remoteFilePath != null )
			path = path.queryParam( "local-file-path", remoteFilePath );

		ClientResponse response = path.type( MediaType.APPLICATION_JSON ).post( ClientResponse.class );
		if( Family.SUCCESSFUL != response.getStatusInfo().getFamily()) {
			String value = response.getEntity( String.class );
			this.logger.finer( response.getStatusInfo() + ": " + value );
			throw new ManagementException( response.getStatusInfo().getStatusCode(), value );
		}

		this.logger.finer( String.valueOf( response.getStatusInfo()));
	}


	/**
	 * Lists applications.
	 * @return a non-null list of applications
	 * @throws UniformInterfaceException if something went wrong
	 * @throws ClientHandlerException if something went wrong
	 * @throws ManagementException if a problem occurred with the applications management
	 */
	public List<Application> listApplications() throws ManagementException {
		this.logger.finer( "Listing applications..." );

		List<Application> result = this.resource
				.path( UrlConstants.APPLICATIONS )
				.accept( MediaType.APPLICATION_JSON )
				.get( new GenericType<List<Application>> () {});

		if( result != null )
			this.logger.finer( result.size() + " applications were found on the DM." );
		else
			this.logger.finer( "No application was found on the DM." );

		return result != null ? result : new ArrayList<Application> ();
	}


	/**
	 * Shutdowns an application.
	 * @param applicationName the application name
	 * @throws UniformInterfaceException if something went wrong
	 * @throws ClientHandlerException if something went wrong
	 * @throws ManagementException if a problem occurred with the applications management
	 */
	public void shutdownApplication( String applicationName ) throws ManagementException {
		this.logger.finer( "Removing application " + applicationName + "..." );

		ClientResponse response = this.resource
				.path( UrlConstants.APPLICATIONS ).path( applicationName ).path( "shutdown" )
				.post( ClientResponse.class );

		String text = response.getEntity( String.class );
		this.logger.finer( text );
		if( Family.SUCCESSFUL != response.getStatusInfo().getFamily())
			throw new ManagementException( response.getStatusInfo().getStatusCode(), text );
	}


	/**
	 * Deletes an application.
	 * @param applicationName the application name
	 * @throws UniformInterfaceException if something went wrong
	 * @throws ClientHandlerException if something went wrong
	 * @throws ManagementException if a problem occurred with the applications management
	 */
	public void deleteApplication( String applicationName ) throws ManagementException {
		this.logger.finer( "Removing application " + applicationName + "..." );

		ClientResponse response = this.resource
				.path( UrlConstants.APPLICATIONS ).path( applicationName ).path( "delete" )
				.delete( ClientResponse.class );

		String text = response.getEntity( String.class );
		this.logger.finer( text );
		if( Family.SUCCESSFUL != response.getStatusInfo().getFamily())
			throw new ManagementException( response.getStatusInfo().getStatusCode(), text );
	}
}
