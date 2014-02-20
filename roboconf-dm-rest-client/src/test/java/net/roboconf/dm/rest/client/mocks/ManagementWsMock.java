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

package net.roboconf.dm.rest.client.mocks;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.internal.utils.Utils;
import net.roboconf.core.model.runtime.Application;
import net.roboconf.core.model.runtime.Application.ApplicationStatus;
import net.roboconf.dm.rest.api.IManagementWs;
import net.roboconf.dm.rest.client.mocks.helper.PropertyManager;

import com.sun.jersey.core.header.FormDataContentDisposition;

/**
 * @author Vincent Zurczak - Linagora
 */
@Path( IManagementWs.PATH )
public class ManagementWsMock implements IManagementWs {

	/* (non-Javadoc)
	 * @see net.roboconf.dm.rest.client.exceptions.server.IApplicationWs
	 * #loadApplication(java.io.InputStream, com.sun.jersey.core.header.FormDataContentDisposition)
	 */
	@Override
	public Response loadApplication( InputStream uploadedInputStream, FormDataContentDisposition fileDetail ) {

		Response response;
		try {
			File f = new File( System.getProperty( "java.io.tmpdir" ), "_" + fileDetail.getFileName());
			f.deleteOnExit();

			Utils.copyStream( uploadedInputStream, f );
			PropertyManager.INSTANCE.remoteFiles.add( f.getAbsolutePath());
			System.out.println( "Added " + f.getAbsolutePath());
			response = Response.ok().build();

		} catch( IOException e ) {
			response = Response.status( Status.INTERNAL_SERVER_ERROR ).entity( e.getMessage()).build();

		} finally {
			Utils.closeQuietly( uploadedInputStream );
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
		PropertyManager.INSTANCE.remoteFiles.add( localFilePath );
		return Response.ok().build();
	}


	/* (non-Javadoc)
	 * @see net.roboconf.dm.rest.client.exceptions.server.IApplicationWs
	 * #listApplications()
	 */
	@Override
	public List<Application> listApplications() {
		return PropertyManager.INSTANCE.apps;
	}


	/* (non-Javadoc)
	 * @see net.roboconf.dm.rest.client.exceptions.server.IApplicationWs
	 * #downloadApplicationModelData(java.lang.String)
	 */
	@Override
	public Response downloadApplicationModelData( String applicationName ) {

		// Prepare the ZIP file
		File zipFile = new File( System.getProperty( "java.io.tmpdir" ), UUID.randomUUID().toString() + ".zip" );
		zipFile.deleteOnExit();

		Map<String,String> entryToContent = TestUtils.buildZipContent();
		TestUtils.createZipFile( entryToContent, zipFile );

		// Return it
		return Response.ok( zipFile ).header( "content-disposition","attachment; filename = " + zipFile.getName()).build();
	}


	/* (non-Javadoc)
	 * @see net.roboconf.dm.rest.client.exceptions.server.IApplicationWs
	 * #deleteApplication(java.lang.String)
	 */
	@Override
	public Response deleteApplication( String applicationName ) {

		Response response;
		if( PropertyManager.APP_1.equals( applicationName )) {
			Application app = getApplicationByName( applicationName );
			PropertyManager.INSTANCE.apps.remove( app );
			response = Response.ok().build();
		} else
			response = Response.status( Status.UNAUTHORIZED ).build();

		return response;
	}


	/* (non-Javadoc)
	 * @see net.roboconf.dm.rest.client.exceptions.server.IApplicationWs
	 * #startApplication(java.lang.String)
	 */
	@Override
	public Response startApplication( String applicationName ) {

		Response response;
		if( PropertyManager.APP_1.equals( applicationName )) {
			getApplicationByName( applicationName ).setStatus( ApplicationStatus.STARTED );
			response = Response.ok().build();
		} else
			response = Response.status( Status.UNAUTHORIZED ).build();

		return response;
	}


	/* (non-Javadoc)
	 * @see net.roboconf.dm.rest.client.exceptions.server.IApplicationWs
	 * #stopApplication(java.lang.String)
	 */
	@Override
	public Response stopApplication( String applicationName ) {

		Response response;
		if( PropertyManager.APP_1.equals( applicationName )) {
			getApplicationByName( applicationName ).setStatus( ApplicationStatus.STOPPED );
			response = Response.ok().build();
		} else
			response = Response.status( Status.UNAUTHORIZED ).build();

		return response;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.dm.rest.client.exceptions.server.IApplicationWs
	 * #getApplicationByName(java.lang.String)
	 */
	@Override
	public Application getApplicationByName( String applicationName ) {

		Application result = null;
		for( Application app : listApplications()) {
			if( applicationName.equals( app.getName())) {
				result = app;
				break;
			}
		}

		return result;
	}
}
