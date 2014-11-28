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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.UUID;
import java.util.logging.Logger;

import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import net.roboconf.core.Constants;
import net.roboconf.core.model.ApplicationDescriptor;
import net.roboconf.core.model.converters.FromGraphs;
import net.roboconf.core.model.io.ParsingModelIo;
import net.roboconf.core.model.io.RuntimeModelIo;
import net.roboconf.core.model.parsing.FileDefinition;
import net.roboconf.core.model.runtime.Component;
import net.roboconf.core.model.runtime.Graphs;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.core.model.runtime.Instance.InstanceStatus;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;

import com.sun.jersey.core.header.FormDataContentDisposition;

/**
 * @author Vincent Zurczak - Linagora
 */
@Path( IDebugResource.PATH )
public class DebugResource implements IDebugResource {

	final static String ROOT_COMPONENT_NAME = "Machine";

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
				if( ! tmpDir.mkdirs())
					throw new IOException( "Failed to create directory " + tmpDir );

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
	public Response checkMessagingConnectionForTheDm() {
		return Response.status( Status.SERVICE_UNAVAILABLE ).entity( "Not yet implemented." ).build();
	}


	/* (non-Javadoc)
	 * @see net.roboconf.dm.rest.services.internal.resources.IDebugResource
	 * #checkMessagingConnectionWithAgent(java.lang.String)
	 */
	@Override
	public Response checkMessagingConnectionWithAgent( String rootInstanceName ) {
		return Response.status( Status.SERVICE_UNAVAILABLE ).entity( "Not yet implemented." ).build();
	}


	/* (non-Javadoc)
	 * @see net.roboconf.dm.rest.services.internal.resources.IDebugResource
	 * #diagnosticInstance(java.lang.String)
	 */
	@Override
	public Response diagnosticInstance( String instancePath ) {
		return Response.status( Status.SERVICE_UNAVAILABLE ).entity( "Not yet implemented." ).build();
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.dm.rest.services.internal.resources.IDebugResource
	 * #diagnosticApplication(java.lang.String)
	 */
	@Override
	public Response diagnosticApplication( String applicationName ) {
		return Response.status( Status.SERVICE_UNAVAILABLE ).entity( "Not yet implemented." ).build();
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
		if( ! graphDir.mkdirs())
			throw new IOException( "Failed to create directory " + graphDir );

		Component rootComponent = new Component( ROOT_COMPONENT_NAME ).alias( "Machine" ).installerName( Constants.TARGET_INSTALLER );
		Graphs graphs = new Graphs();
		graphs.getRootComponents().add( rootComponent );

		FileDefinition def = new FromGraphs().buildFileDefinition( graphs, new File( graphDir, "whole.graph" ), false );
		ParsingModelIo.saveRelationsFile( def, false, "\n" );

		// Write the target.properties file
		File rootComponentDir = new File( graphDir, ROOT_COMPONENT_NAME );
		if( ! rootComponentDir.mkdirs())
			throw new IOException( "Failed to create directory " + rootComponentDir );

		InputStream in = new ByteArrayInputStream( targetPropertiesContent.getBytes( "UTF-8" ));
		Utils.copyStream( in, new File( rootComponentDir, Constants.TARGET_PROPERTIES_FILE_NAME ));

		// Write instances
		File instDir = new File( appDirectory, Constants.PROJECT_DIR_INSTANCES );
		if( ! instDir.mkdirs())
			throw new IOException( "Failed to create directory " + instDir );

		Instance rootInstance = new Instance( "root" ).component( rootComponent );
		RuntimeModelIo.writeInstances( new File( instDir, "model.instances" ), Arrays.asList( rootInstance ));

		// Create the meta-data
		File metaDir = new File( appDirectory, Constants.PROJECT_DIR_DESC );
		if( ! metaDir.mkdirs())
			throw new IOException( "Failed to create directory " + metaDir );

		ApplicationDescriptor descriptor = new ApplicationDescriptor();
		descriptor.setName( FAKE_APP_NAME );
		descriptor.setDescription( "An application to test a deployment target (debug purpose)." );
		descriptor.setGraphEntryPoint( "whole.graph" );
		descriptor.setInstanceEntryPoint( "model.instances" );
		descriptor.setQualifier( "DEBUG" );

		ApplicationDescriptor.save( new File( metaDir, Constants.PROJECT_FILE_DESCRIPTOR ), descriptor );
	}
}
