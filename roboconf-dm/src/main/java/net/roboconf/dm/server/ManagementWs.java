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

package net.roboconf.dm.server;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Logger;

import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import net.roboconf.core.model.runtime.Application;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.management.exceptions.AlreadyExistingException;
import net.roboconf.dm.management.exceptions.InvalidApplicationException;
import net.roboconf.dm.management.exceptions.UnauthorizedActionException;
import net.roboconf.dm.rest.api.IManagementWs;
import net.roboconf.dm.rest.json.MapHolder;
import net.roboconf.iaas.api.IaasException;

import com.sun.jersey.core.header.FormDataContentDisposition;

/**
 * @author Vincent Zurczak - Linagora
 */
@Path( IManagementWs.PATH )
public class ManagementWs implements IManagementWs {

	private final Logger logger = Logger.getLogger( ManagementWs.class.getName());


	/* (non-Javadoc)
	 * @see net.roboconf.dm.rest.client.exceptions.server.IApplicationWs
	 * #loadApplication(java.io.InputStream, com.sun.jersey.core.header.FormDataContentDisposition)
	 */
	@Override
	public Response loadApplication( InputStream uploadedInputStream, FormDataContentDisposition fileDetail ) {

		this.logger.fine( "Request: load application from uploaded ZIP file (" + fileDetail.getFileName() + ")." );
		Response response;
		File tempZipFile = new File( System.getProperty( "java.io.tmpdir" ), fileDetail.getFileName());
		try {
			// Copy the uploaded ZIP file on the disk
			Utils.copyStream( uploadedInputStream, tempZipFile );

			// Extract the ZIP content
			String appName = fileDetail.getFileName().replace( ".zip", "" );
			File dir = new File( System.getProperty( "java.io.tmpdir" ), "roboconf/" + appName );
			Utils.extractZipArchive( tempZipFile, dir );

			// Load the application
			MapHolder mapHolder = new MapHolder();
			mapHolder.getMap().put( MapHolder.FILE_LOCAL_PATH, dir.getAbsolutePath());
			response = loadApplication( mapHolder );

		} catch( IOException e ) {
			response = Response.status( Status.NOT_ACCEPTABLE ).entity( "A ZIP file was expected. " + e.getMessage()).build();

		} finally {
			Utils.closeQuietly( uploadedInputStream );
			if( ! tempZipFile.delete())
				tempZipFile.deleteOnExit();
		}

		return response;
	}

	/*
	 * (non-Javadoc)
	 * @see net.roboconf.dm.rest.api.IManagementWs
	 * #loadApplication(net.roboconf.dm.rest.json.MapHolder)
	 */
	@Override
	public Response loadApplication( MapHolder mapHolder ) {

		String localFilePath = mapHolder.getMap().get( MapHolder.FILE_LOCAL_PATH );
		if( localFilePath == null )
			localFilePath = "null";

		this.logger.fine( "Request: load application from " + localFilePath + "." );
		Response response;
		try {
			Manager.INSTANCE.loadNewApplication( new File( localFilePath ));
			response = Response.ok().build();

		} catch( AlreadyExistingException e ) {
			response = Response.status( Status.FORBIDDEN ).entity( e.getMessage()).build();

		} catch( InvalidApplicationException e ) {
			response = Response.status( Status.NOT_ACCEPTABLE ).entity( e.getMessage()).build();

		} catch( IOException e ) {
			response = Response.status( Status.UNAUTHORIZED ).entity( e.getMessage()).build();
		}

		return response;
	}


	/* (non-Javadoc)
	 * @see net.roboconf.dm.rest.client.exceptions.server.IApplicationWs
	 * #listApplications()
	 */
	@Override
	public List<Application> listApplications() {
		this.logger.fine( "Request: list all the applications." );
		return Manager.INSTANCE.listApplications();
	}


	/* (non-Javadoc)
	 * @see net.roboconf.dm.rest.client.exceptions.server.IApplicationWs
	 * #deleteApplication(java.lang.String)
	 */
	@Override
	public Response deleteApplication( String applicationName ) {

		this.logger.fine( "Request: delete application " + applicationName + "." );
		Response result = Response.ok().build();
		try {
			ManagedApplication ma = Manager.INSTANCE.getAppNameToManagedApplication().get( applicationName );
			if( ma == null )
				result = Response.status( Status.NOT_FOUND ).entity( "Application " + applicationName + " was not found." ).build();
			else
				Manager.INSTANCE.deleteApplication( ma );

		} catch( UnauthorizedActionException e ) {
			result = Response.status( Status.FORBIDDEN ).entity( e.getMessage()).build();

		} catch( IOException e ) {
			result = Response.status( Status.FORBIDDEN ).entity( e.getMessage()).build();
		}

		return result;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.dm.rest.api.IManagementWs
	 * #shutdownApplication(java.lang.String)
	 */
	@Override
	public Response shutdownApplication( String applicationName ) {

		this.logger.fine( "Request: shutdown application " + applicationName + "." );
		Response result = Response.ok().build();
		try {
			ManagedApplication ma = Manager.INSTANCE.getAppNameToManagedApplication().get( applicationName );
			if( ma == null )
				result = Response.status( Status.NOT_FOUND ).entity( "Application " + applicationName + " was not found." ).build();
			else
				Manager.INSTANCE.shutdownApplication( ma );

			// FIXME: invalid return code
		} catch( IOException e ) {
			result = Response.status( Status.ACCEPTED ).entity( e.getMessage()).build();

		} catch( IaasException e ) {
			result = Response.status( Status.ACCEPTED ).entity( e.getMessage()).build();
		}

		return result;
	}
}
