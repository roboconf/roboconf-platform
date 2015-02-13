/**
 * Copyright 2014-2015 Linagora, Université Joseph Fourier, Floralis
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import net.roboconf.core.Constants;
import net.roboconf.core.dsl.ParsingModelIo;
import net.roboconf.core.dsl.converters.FromGraphs;
import net.roboconf.core.dsl.parsing.FileDefinition;
import net.roboconf.core.model.ApplicationDescriptor;
import net.roboconf.core.model.RuntimeModelIo;
import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.Component;
import net.roboconf.core.model.beans.Graphs;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;

import com.sun.jersey.core.header.FormDataContentDisposition;

/**
 * @author Vincent Zurczak - Linagora
 * @author Pierre Bourret - Université Joseph Fourier
 */
@Path( IDebugResource.PATH )
public class DebugResource implements IDebugResource {

	final static String ROOT_COMPONENT_NAME = "Machine";
	final static long MAXIMUM_TIMEOUT = 10000L;

	private final Logger logger = Logger.getLogger( getClass().getName());
	private final Manager manager;

	/**
	 * Constructor.
	 * @param manager
	 */
	public DebugResource( Manager manager ) {
		this.manager = manager;
	}


	/* (non-Javadoc)
	 * @see net.roboconf.dm.rest.services.internal.resources.IDebugResource
	 * #createTestForTargetProperties(java.io.InputStream, com.sun.jersey.core.header.FormDataContentDisposition)
	 */
	@Override
	public Response createTestForTargetProperties( InputStream uploadedInputStream, FormDataContentDisposition fileDetail ) {

		this.logger.fine( "Creating a fake application to test a target.properties file." );
		ManagedApplication ma = this.manager.getAppNameToManagedApplication().get( FAKE_APP_NAME );
		Response response = null;
		File tmpDir = null;
		String fileContent = null;

		try {
			// Copy the file content
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			Utils.copyStream( uploadedInputStream, os );
			fileContent = os.toString( "UTF-8" );

			// Create a new application and load it
			if( ma == null ) {
				tmpDir = new File( System.getProperty( "java.io.tmpdir" ), UUID.randomUUID().toString());
				Utils.createDirectory( tmpDir );
				createApplication( tmpDir, fileContent );
				this.manager.loadNewApplication( tmpDir );
			}

			// Is this our application?
			else if( ma.getApplication().getGraphs().getRootComponents().size() == 1
					&& ROOT_COMPONENT_NAME.equals( ma.getApplication().getGraphs().getRootComponents().iterator().next().getName())) {

				boolean allStopped = true;
				for( Instance rootInstance : ma.getApplication().getRootInstances()) {
					if( ! InstanceStatus.NOT_DEPLOYED.equals( rootInstance.getStatus())) {
						allStopped = false;
						break;
					}
				}

				if( ! allStopped )
					response = Response.status( Status.CONFLICT ).entity( "Some machines are still running. Undeploy them all first." ).build();
				else
					Utils.writeStringInto( fileContent, new File(
							ma.getApplicationFilesDirectory(),
							Constants.PROJECT_DIR_GRAPH + "/" + ROOT_COMPONENT_NAME + "/" + Constants.TARGET_PROPERTIES_FILE_NAME ));
			}

			// An application with the same name exist, and we did not create it
			else {
				response = Response.status( Status.CONFLICT ).entity( "Another application already exists with the name '" + FAKE_APP_NAME + "'." ).build();
			}

		} catch( Exception e ) {
			this.logger.warning( e.getMessage());
			Utils.logException( this.logger, e );
			response = Response.status( Status.FORBIDDEN ).entity( e.getMessage()).build();

		} finally {
			try {
				Utils.deleteFilesRecursively( tmpDir );

			} catch( IOException e ) {
				this.logger.warning( "A temporary directory could not be deleted." );
				Utils.logException( this.logger, e );
			}
		}

		// Build the response for successful cases
		if( response == null )
			response = Response.ok( fileContent ).build();

		return response;
	}


	/* (non-Javadoc)
	 * @see net.roboconf.dm.rest.services.internal.resources.IDebugResource
	 * #checkMessagingConnectionForTheDm()
	 */
	@Override
	public Response checkMessagingConnectionForTheDm( String message, long timeout ) {

		if ( message == null ) {
			// Generates an unique message.
			message = "ECHO " + UUID.randomUUID();
		}

		// Max timeout
		if ( timeout > MAXIMUM_TIMEOUT ) {
			this.logger.warning( "Timeout " + timeout + "ms is above maximum limit " + MAXIMUM_TIMEOUT + "ms. Normalizing" );
			timeout = MAXIMUM_TIMEOUT;
		}

		this.logger.fine( "Checking connection to the message queue. message=" + message
				+ ", timeout=" + timeout + "ms" );

		boolean hasReceived;
		try {
			// Send the Echo request!
			hasReceived = this.manager.pingMessageQueue( message, timeout );

		} catch ( IOException e ) {
			// Something bad has happened!
			final String eMessage = "Unable to send Echo message " + message;
			logger.log( Level.SEVERE, eMessage, e );
			return Response.status( Status.INTERNAL_SERVER_ERROR ).entity( eMessage ).build();
		} catch ( InterruptedException e ) {
			// Something unexpected has happened!
			final String eMessage = "Interrupted while waiting for Echo message " + message;
			logger.log( Level.WARNING, eMessage, e );
			return Response.status( Status.INTERNAL_SERVER_ERROR ).entity( eMessage ).build();
		}

		// Return result.
		if ( hasReceived ) {
			final String rMessage = "Has received Echo message " + message;
			logger.fine( rMessage );
			return Response.status( Status.OK ).entity( rMessage ).build();
		}
		else {
			final String rMessage = "Did not receive Echo message " + message + " before timeout ("
					+ timeout + "ms)";
			logger.warning( rMessage );
			return Response.status( 408 ).entity( rMessage ).build();
		}
	}


	/* (non-Javadoc)
	 * @see net.roboconf.dm.rest.services.internal.resources.IDebugResource
	 * #checkMessagingConnectionWithAgent(java.lang.String)
	 */
	@Override
	public Response checkMessagingConnectionWithAgent( String applicationName,
	                                                   String rootInstanceName,
	                                                   String message,
	                                                   long timeout ) {

		if ( message == null ) {
			// Generates an unique message.
			message = UUID.randomUUID().toString();
		}

		// Max timeout
		if ( timeout > MAXIMUM_TIMEOUT ) {
			this.logger.warning( "Timeout " + timeout + "ms is above maximum limit " + MAXIMUM_TIMEOUT + "ms. Normalizing" );
			timeout = MAXIMUM_TIMEOUT;
		}

		boolean hasReceived;
		try {
			// Ping the agent!
			hasReceived = this.manager.pingAgent( applicationName, rootInstanceName, message, timeout );

		} catch ( IOException e ) {
			// Something bad has happened!
			final String eMessage = "Unable to ping agent " + rootInstanceName + " with message " + message;
			logger.log( Level.SEVERE, eMessage, e );
			return Response.status( Status.INTERNAL_SERVER_ERROR ).entity( eMessage ).build();
		} catch ( InterruptedException e ) {
			// Something unexpected has happened!
			final String eMessage = "Interrupted while waiting for ping response from agent " + rootInstanceName
					+ " message " + message;
			logger.log( Level.WARNING, eMessage, e );
			return Response.status( Status.INTERNAL_SERVER_ERROR ).entity( eMessage ).build();
		}

		// Return result.
		if ( hasReceived ) {
			final String rMessage = "Has received ping response " + message + " from agent " + rootInstanceName;
			logger.fine( rMessage );
			return Response.status( Status.OK ).entity( rMessage ).build();
		}
		else {
			final String rMessage = "Did not receive ping response " + message + " from agent " + rootInstanceName
					+ " before timeout (" + timeout + "ms)";
			logger.warning( rMessage );
			return Response.status( 408 ).entity( rMessage ).build();
		}

	}


	/* (non-Javadoc)
	 * @see net.roboconf.dm.rest.services.internal.resources.IDebugResource
	 * #diagnoseInstance(java.lang.String)
	 */
	@Override
	public Response diagnoseInstance( String applicationName, String instancePath ) {
		final Application application = this.manager.findApplicationByName( applicationName );
		if (application == null) {
			return Response
					.status( Status.BAD_REQUEST )
					.entity( "No application with name " + applicationName )
					.build();
		}
		final Instance instance = InstanceHelpers.findInstanceByPath( application, instancePath );
		if (instance == null) {
			return Response
					.status( Status.BAD_REQUEST )
					.entity( "No instance with path " + instancePath + " in application " + applicationName )
					.build();
		}
		return Response.status( Status.OK ).entity( instance ).build();
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.dm.rest.services.internal.resources.IDebugResource
	 * #diagnoseApplication(java.lang.String)
	 */
	@Override
	public Response diagnoseApplication( String applicationName ) {
		final Application application = this.manager.findApplicationByName( applicationName );
		if (application == null) {
			return Response
					.status( Status.BAD_REQUEST )
					.entity( "No application with name " + applicationName )
					.build();
		}
		return Response.status( Status.OK ).entity( application ).build();
	}


	/**
	 *
	 * @param appDirectory
	 * @param targetPropertiesContent
	 * @throws IOException
	 */
	void createApplication( File appDirectory, String targetPropertiesContent ) throws IOException {

		// Write the graph
		File graphDir = new File( appDirectory, Constants.PROJECT_DIR_GRAPH );
		Utils.createDirectory( graphDir );

		Component rootComponent = new Component( ROOT_COMPONENT_NAME ).installerName( Constants.TARGET_INSTALLER );
		Graphs graphs = new Graphs();
		graphs.getRootComponents().add( rootComponent );

		FileDefinition def = new FromGraphs().buildFileDefinition( graphs, new File( graphDir, "whole.graph" ), false );
		ParsingModelIo.saveRelationsFile( def, false, "\n" );

		// Write the target.properties file
		File rootComponentDir = new File( graphDir, ROOT_COMPONENT_NAME );
		Utils.createDirectory( rootComponentDir );

		InputStream in = new ByteArrayInputStream( targetPropertiesContent.getBytes( "UTF-8" ));
		Utils.copyStream( in, new File( rootComponentDir, Constants.TARGET_PROPERTIES_FILE_NAME ));

		// Write instances
		File instDir = new File( appDirectory, Constants.PROJECT_DIR_INSTANCES );
		Utils.createDirectory( instDir );

		Instance rootInstance = new Instance( "root" ).component( rootComponent );
		RuntimeModelIo.writeInstances( new File( instDir, "model.instances" ), Arrays.asList( rootInstance ));

		// Create the meta-data
		File metaDir = new File( appDirectory, Constants.PROJECT_DIR_DESC );
		Utils.createDirectory( metaDir );

		ApplicationDescriptor descriptor = new ApplicationDescriptor();
		descriptor.setName( FAKE_APP_NAME );
		descriptor.setDescription( "An application to test a deployment target (debug purpose)." );
		descriptor.setGraphEntryPoint( "whole.graph" );
		descriptor.setInstanceEntryPoint( "model.instances" );
		descriptor.setNamespace( "net.roboconf" );
		descriptor.setQualifier( "DEBUG" );

		ApplicationDescriptor.save( new File( metaDir, Constants.PROJECT_FILE_DESCRIPTOR ), descriptor );
	}
}
