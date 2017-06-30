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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status.Family;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;

import net.roboconf.core.model.beans.AbstractApplication;
import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.core.model.runtime.TargetWrapperDescriptor;
import net.roboconf.dm.rest.client.WsClient;
import net.roboconf.dm.rest.client.exceptions.TargetWsException;
import net.roboconf.dm.rest.commons.UrlConstants;

/**
 * FIXME: we only defined operations used in integration tests.
 * @author Vincent Zurczak - Linagora
 */
public class TargetWsDelegate {

	private final WebResource resource;
	private final Logger logger;
	private final WsClient wsClient;


	/**
	 * Constructor.
	 * @param resource a web resource
	 * @param the WS client
	 */
	public TargetWsDelegate( WebResource resource, WsClient wsClient ) {
		this.resource = resource;
		this.wsClient = wsClient;
		this.logger = Logger.getLogger( getClass().getName());
	}


	/**
	 * Lists all the targets.
	 * @return a non-null list of target descriptors
	 */
	public List<TargetWrapperDescriptor> listAllTargets() {

		this.logger.finer( "Listing all the available targets." );
		WebResource path = this.resource.path( UrlConstants.TARGETS );
		List<TargetWrapperDescriptor> result =
				this.wsClient.createBuilder( path )
				.accept( MediaType.APPLICATION_JSON )
				.type( MediaType.APPLICATION_JSON )
				.get( new GenericType<List<TargetWrapperDescriptor>> () {});

		if( result != null )
			this.logger.finer( result.size() + " target descriptors were found." );
		else
			this.logger.finer( "No target descriptor was found." );

		return result != null ? result : new ArrayList<TargetWrapperDescriptor>( 0 );
	}


	/**
	 * Creates a new target.
	 * @param targetContent non-null target properties
	 * @return the ID of the newly created target
	 * @throws TargetWsException if the creation failed
	 */
	public String createTarget( String targetContent ) throws TargetWsException {

		this.logger.finer( "Creating a new target."  );

		WebResource path = this.resource.path( UrlConstants.TARGETS );
		ClientResponse response = this.wsClient.createBuilder( path ).post( ClientResponse.class, targetContent );
		handleResponse( response );

		return response.getEntity( String.class );
	}


	/**
	 * Deletes a target.
	 * @param targetId a target ID
	 * @throws TargetWsException if the creation failed
	 */
	public void deleteTarget( String targetId ) throws TargetWsException {

		this.logger.finer( "Deleting target " + targetId  );

		WebResource path = this.resource.path( UrlConstants.TARGETS ).path( targetId );
		ClientResponse response = this.wsClient.createBuilder( path )
						.accept( MediaType.APPLICATION_JSON )
						.delete( ClientResponse.class );

		handleResponse( response );
	}


	/**
	 * Associates or dissociates an application and a target.
	 * @param app an application or application template
	 * @param targetId a target ID
	 * @param instancePathOrComponentName an instance path or a component name (can be null)
	 * @param bind true to create a binding, false to delete one
	 * @throws TargetWsException
	 */
	public void associateTarget( AbstractApplication app, String instancePathOrComponentName, String targetId, boolean bind )
	throws TargetWsException {

		if( bind )
			this.logger.finer( "Associating " + app + " :: " + instancePathOrComponentName + " with " + targetId );
		else
			this.logger.finer( "Dissociating " + app + " :: " + instancePathOrComponentName + " from " + targetId );

		WebResource path = this.resource.path( UrlConstants.TARGETS )
				.path( targetId ).path( "associations" )
				.queryParam( "bind", Boolean.toString( bind ))
				.queryParam( "name", app.getName());

		if( instancePathOrComponentName != null )
			path = path.queryParam( "elt", instancePathOrComponentName );

		if( app instanceof ApplicationTemplate )
			path = path.queryParam( "qualifier", ((ApplicationTemplate) app).getVersion());

		ClientResponse response = this.wsClient.createBuilder( path )
						.accept( MediaType.APPLICATION_JSON )
						.post( ClientResponse.class );

		handleResponse( response );
	}


	private void handleResponse( ClientResponse response ) throws TargetWsException {

		if( Family.SUCCESSFUL != response.getStatusInfo().getFamily()) {
			String value = response.getEntity( String.class );
			this.logger.finer( response.getStatusInfo() + ": " + value );
			throw new TargetWsException( response.getStatusInfo().getStatusCode(), value );
		}
	}
}
