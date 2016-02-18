/**
 * Copyright 2014-2016 Linagora, Université Joseph Fourier, Floralis
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.Import;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.helpers.ComponentHelpers;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.rest.commons.Diagnostic;
import net.roboconf.dm.rest.commons.Diagnostic.DependencyInformation;
import net.roboconf.dm.rest.services.internal.resources.IDebugResource;

/**
 * @author Vincent Zurczak - Linagora
 * @author Pierre Bourret - Université Joseph Fourier
 */
@Path( IDebugResource.PATH )
public class DebugResource implements IDebugResource {

	private final Logger logger = Logger.getLogger( getClass().getName());
	private final Manager manager;


	/**
	 * Constructor.
	 * @param manager the manager
	 */
	public DebugResource( Manager manager ) {
		this.manager = manager;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.dm.rest.services.internal.resources.IDebugResource
	 * #checkMessagingConnectionForTheDm(java.lang.String)
	 */
	@Override
	public Response checkMessagingConnectionForTheDm( String message ) {

		this.logger.fine( "Request: check the connection to the message queue. message=" + message );
		Response response;
		if( this.manager.debugMngr().pingMessageQueue( message ))
			response = Response.status( Status.OK ).entity( "An Echo message (" + message + ") was sent. Wait for the echo on websocket." ).build();
		else
			response = Response .status( Status.INTERNAL_SERVER_ERROR ).entity( "An error occured with the messaging, no ECHO message was sent." ).build();

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

		this.logger.fine( "Request: check the connection with agent " + applicationName + " :: " + scopedInstancePath + ". message=" + message );
		final ManagedApplication ma = this.manager.applicationMngr().findManagedApplicationByName( applicationName );
		Response response;
		int pingResult;
		final Instance instance;

		if( ma == null )
			response = Response.status( Status.NOT_FOUND ).entity( "No application called " + applicationName + " was found." ).build();
		else if(( instance = InstanceHelpers.findInstanceByPath( ma.getApplication(), scopedInstancePath )) == null )
			response = Response .status( Status.NOT_FOUND ).entity( "Instance " + scopedInstancePath + " was not found in application " + applicationName ).build();
		else if(( pingResult = this.manager.debugMngr().pingAgent( ma, instance, message )) == 1 )
			response = Response .status( Status.BAD_REQUEST ).entity( "No PING request was sent, the agent is not started." ).build();
		else if( pingResult == 2 )
			response = Response .status( Status.INTERNAL_SERVER_ERROR ).entity( "An error occured with the messaging, no PING request was sent." ).build();
		else
			response = Response.status( Status.OK ).entity( "A PING request (" + message + ") was sent. Wait for the echo on websocket." ).build();

		return response;
	}


	/* (non-Javadoc)
	 * @see net.roboconf.dm.rest.services.internal.resources.IDebugResource
	 * #diagnoseInstance(java.lang.String)
	 */
	@Override
	public Response diagnoseInstance( String applicationName, String instancePath ) {

		this.logger.fine( "Request: create a diagnostic for " + instancePath + " in application " + applicationName );
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

		this.logger.fine( "Request: create a diagnostic for the application called " + applicationName + "." );
		List<Diagnostic> result = new ArrayList<Diagnostic> ();
		final Application application = this.manager.applicationMngr().findApplicationByName( applicationName );
		if( application != null ) {
			for( Instance inst : InstanceHelpers.getAllInstances( application ))
				result.add( createDiagnostic( inst ));
		}

		return result;
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
