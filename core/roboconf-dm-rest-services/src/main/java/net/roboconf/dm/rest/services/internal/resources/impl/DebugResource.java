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

package net.roboconf.dm.rest.services.internal.resources.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import net.roboconf.core.Constants;
import net.roboconf.core.dsl.ParsingModelIo;
import net.roboconf.core.dsl.converters.FromGraphs;
import net.roboconf.core.dsl.parsing.FileDefinition;
import net.roboconf.core.model.ApplicationTemplateDescriptor;
import net.roboconf.core.model.RuntimeModelIo;
import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.core.model.beans.Component;
import net.roboconf.core.model.beans.Graphs;
import net.roboconf.core.model.beans.Import;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.core.model.helpers.ComponentHelpers;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.rest.commons.Diagnostic;
import net.roboconf.dm.rest.commons.Diagnostic.DependencyInformation;
import net.roboconf.dm.rest.services.internal.RestServicesUtils;
import net.roboconf.dm.rest.services.internal.resources.IDebugResource;

import com.sun.jersey.core.header.FormDataContentDisposition;

/**
 * @author Vincent Zurczak - Linagora
 * @author Pierre Bourret - Université Joseph Fourier
 */
@Path( IDebugResource.PATH )
public class DebugResource implements IDebugResource {

	static final String ROOT_COMPONENT_NAME = "Machine";

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
		ManagedApplication ma = this.manager.applicationMngr().findManagedApplicationByName( FAKE_APP_NAME );
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
				ApplicationTemplate tpl = this.manager.applicationTemplateMngr().loadApplicationTemplate( tmpDir );
				this.manager.applicationMngr().createApplication( FAKE_APP_NAME, tpl.getDescription(), tpl );
			}

			// Is this our application?
			else if( ma.getGraphs().getRootComponents().size() == 1
					&& ROOT_COMPONENT_NAME.equals( ma.getGraphs().getRootComponents().iterator().next().getName())) {

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
							ma.getTemplateDirectory(),
							Constants.PROJECT_DIR_GRAPH + "/" + ROOT_COMPONENT_NAME + "/" + Constants.TARGET_PROPERTIES_FILE_NAME ));
			}

			// An application with the same name exist, and we did not create it
			else {
				response = Response.status( Status.CONFLICT ).entity( "Another application already exists with the name '" + FAKE_APP_NAME + "'." ).build();
			}

		} catch( Exception e ) {
			response = RestServicesUtils.handleException( this.logger, Status.FORBIDDEN, null, e ).build();

		} finally {
			Utils.deleteFilesRecursivelyAndQuitely( tmpDir );
		}

		// Build the response for successful cases
		if( response == null )
			response = Response.ok( fileContent ).build();

		return response;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.dm.rest.services.internal.resources.IDebugResource
	 * #checkMessagingConnectionForTheDm(java.lang.String)
	 */
	@Override
	public Response checkMessagingConnectionForTheDm( String message ) {

		this.logger.fine( "Checking connection to the message queue. message=" + message );
		Response response;
		try {
			this.manager.debugMngr().pingMessageQueue( message );
			response = Response.status( Status.OK ).entity( "An Echo message (" + message + ") was sent. Wait for the echo on websocket." ).build();

		} catch ( IOException e ) {
			String responseMessage = "Unable to send Echo message " + message;
			response = RestServicesUtils.handleException( this.logger, Status.INTERNAL_SERVER_ERROR, responseMessage, e ).build();
		}

		return response;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.dm.rest.services.internal.resources.IDebugResource
	 * #checkMessagingConnectionWithAgent(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public Response checkMessagingConnectionWithAgent(
			String applicationName,
			String scopedInstancePath,
			String message ) {

		Response response = Response.status( Status.OK ).entity( "An Echo message (" + message + ") was sent. Wait for the echo on websocket." ).build();
		String responseMessage;
		try {
			final ManagedApplication ma = this.manager.applicationMngr().findManagedApplicationByName( applicationName );
			final Instance instance;
			if( ma == null )
				response = Response.status( Status.NOT_FOUND ).entity( "No application called " + applicationName + " was found." ).build();
			else if(( instance = InstanceHelpers.findInstanceByPath( ma.getApplication(), scopedInstancePath )) == null )
				response = Response .status( Status.NOT_FOUND ).entity( "Instance " + scopedInstancePath + " was not found in application " + applicationName ).build();
			else
				this.manager.debugMngr().pingAgent( ma, instance, message );

		} catch ( IOException e ) {
			responseMessage = "Unable to ping agent " + scopedInstancePath + " with message " + message;
			response = RestServicesUtils.handleException( this.logger, Status.INTERNAL_SERVER_ERROR, responseMessage, e ).build();
		}

		return response;
	}


	/* (non-Javadoc)
	 * @see net.roboconf.dm.rest.services.internal.resources.IDebugResource
	 * #diagnoseInstance(java.lang.String)
	 */
	@Override
	public Response diagnoseInstance( String applicationName, String instancePath ) {

		this.logger.fine( "Creating a diagnostic for " + instancePath + " in application " + applicationName );
		final Application application = this.manager.applicationMngr().findApplicationByName( applicationName );
		final Instance instance;
		final Response response;

		if( application == null )
			response = Response.status( Status.NOT_FOUND ).entity( "No application called " + applicationName + " was found." ).build();
		else if(( instance = InstanceHelpers.findInstanceByPath( application, instancePath )) == null )
			response = Response .status( Status.NOT_FOUND ).entity( "Instance " + instancePath + " was not found in application " + applicationName ).build();
		else
			response = Response.status( Status.OK ).entity( createDiagnostic( instance )).build();

		return response;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.dm.rest.services.internal.resources.IDebugResource
	 * #diagnoseApplication(java.lang.String)
	 */
	@Override
	public List<Diagnostic> diagnoseApplication( String applicationName ) {

		this.logger.fine( "Creating a diagnostic for the application called " + applicationName + "." );
		List<Diagnostic> result = new ArrayList<Diagnostic> ();
		final Application application = this.manager.applicationMngr().findApplicationByName( applicationName );
		if( application != null ) {
			for( Instance inst : InstanceHelpers.getAllInstances( application ))
				result.add( createDiagnostic( inst ));
		}

		return result;
	}


	/**
	 * Creates an application template.
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

		ApplicationTemplateDescriptor descriptor = new ApplicationTemplateDescriptor();
		descriptor.setName( FAKE_APP_NAME );
		descriptor.setDescription( "An application to test a deployment target (debug purpose)." );
		descriptor.setGraphEntryPoint( "whole.graph" );
		descriptor.setInstanceEntryPoint( "model.instances" );
		descriptor.setQualifier( "DEBUG" );

		ApplicationTemplateDescriptor.save( new File( metaDir, Constants.PROJECT_FILE_DESCRIPTOR ), descriptor );
	}


	/**
	 * Creates a diagnostic for an instance.
	 * @param instance a non-null instance
	 * @return a non-null diagnostic
	 */
	Diagnostic createDiagnostic( Instance instance ) {

		Diagnostic result = new Diagnostic( InstanceHelpers.computeInstancePath( instance ));
		for( Map.Entry<String,Boolean> entry : ComponentHelpers.findComponentDependenciesFor( instance.getComponent()).entrySet()) {

			String facetOrComponentName = entry.getKey();
			Collection<Import> imports = instance.getImports().get( facetOrComponentName );
			boolean resolved = imports != null && ! imports.isEmpty();
			boolean optional = entry.getValue();

			result.getDependenciesInformation().add( new DependencyInformation( facetOrComponentName, optional, resolved ));
		}

		return result;
	}
}
