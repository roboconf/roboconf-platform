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

import net.roboconf.core.internal.utils.Utils;
import net.roboconf.core.model.runtime.Application;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.management.exceptions.AlreadyExistingException;
import net.roboconf.dm.management.exceptions.BulkActionException;
import net.roboconf.dm.management.exceptions.InexistingException;
import net.roboconf.dm.management.exceptions.InvalidApplicationException;
import net.roboconf.dm.management.exceptions.UnauthorizedActionException;
import net.roboconf.dm.rest.api.IManagementWs;

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
			response = loadApplication( dir.getAbsolutePath());

		} catch( IOException e ) {
			response = Response.status( Status.INTERNAL_SERVER_ERROR ).entity( e.getMessage()).build();

		} finally {
			Utils.closeQuietly( uploadedInputStream );
			if( ! tempZipFile.delete())
				tempZipFile.deleteOnExit();
		}

		return response;
	}

	/*
	 * (non-Javadoc)
	 * @see net.roboconf.dm.rest.client.exceptions.server.IApplicationWs
	 * #loadApplication(java.lang.String)
	 */
	@Override
	public Response loadApplication( String localFilePath ) {

		this.logger.fine( "Request: load application from " + localFilePath + "." );
		Response response;
		try {
			Manager.INSTANCE.loadNewApplication( new File( localFilePath ));
			response = Response.ok().build();

		} catch( AlreadyExistingException e ) {
			response = Response.status( Status.FORBIDDEN ).entity( e.getMessage()).build();

		} catch( InvalidApplicationException e ) {
			response = Response.status( Status.NOT_ACCEPTABLE ).entity( e.getMessage()).build();
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
	 * #downloadApplicationModelData(java.lang.String)
	 */
	@Override
	public Response downloadApplicationModelData( String applicationName ) {

		this.logger.fine( "Request: download model data for " + applicationName + "." );

		// Get the ZIP file to return.
		// Here, we will created an arbitrary one.
//		Map<String,String> entryToContent = TestUtils.buildZipContent();
//		File zipFile = new File( System.getProperty( "java.io.tmpdir" ), UUID.randomUUID().toString() + ".zip" );
//		zipFile.deleteOnExit();
//
//		// Create the ZIP file to transfer
//		ZipOutputStream zos = null;
//		try {
//			zos = new ZipOutputStream( new FileOutputStream( zipFile ));
//			for( Map.Entry<String,String> entry : entryToContent.entrySet()) {
//				zos.putNextEntry( new ZipEntry( entry.getKey()));
//				ByteArrayInputStream is = new ByteArrayInputStream( entry.getValue().getBytes( "UTF-8" ));
//				Utils.copyStream( is, zos );
//			}
//
//		} catch( IOException e ) {
//			Assert.fail( "Failed to create the ZIP. " + e.getMessage());
//
//		} finally {
//			Utils.closeQuietly( zos );
//		}

		// Return it
		// return Response.ok( zipFile ).header( "content-disposition","attachment; filename = " + zipFile.getName()).build();
		// TODO: review once the app manager has been refactored.
		return Response.status( Status.FORBIDDEN ).build();
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
			Manager.INSTANCE.deleteApplication( applicationName );

		} catch( InexistingException e ) {
			result = Response.status( Status.NOT_FOUND ).entity( "Application " + applicationName + " was not found." ).build();

		} catch( UnauthorizedActionException e ) {
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
			Manager.INSTANCE.shutdownApplication( applicationName );

		} catch( InexistingException e ) {
			result = Response.status( Status.NOT_FOUND ).entity( "Application " + applicationName + " was not found." ).build();

		} catch( BulkActionException e ) {
			result = Response.status( Status.ACCEPTED ).entity( e.getMessage()).build();
		}

		return result;
	}
}
