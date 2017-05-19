/**
 * Copyright 2015-2017 Linagora, Université Joseph Fourier, Floralis
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

import java.util.List;
import java.util.logging.Logger;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status.Family;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;

import net.roboconf.dm.rest.client.WsClient;
import net.roboconf.dm.rest.client.exceptions.DebugWsException;
import net.roboconf.dm.rest.commons.Diagnostic;
import net.roboconf.dm.rest.commons.UrlConstants;

/**
 * @author Vincent Zurczak - Linagora
 */
public class DebugWsDelegate {

	private final WebResource resource;
	private final Logger logger;
	private final WsClient wsClient;


	/**
	 * Constructor.
	 * @param resource a web resource
	 * @param the WS client
	 */
	public DebugWsDelegate( WebResource resource, WsClient wsClient ) {
		this.resource = resource;
		this.wsClient = wsClient;
		this.logger = Logger.getLogger( getClass().getName());
	}


	/**
	 * Checks the DM is correctly connected with the messaging server.
	 * @param message a customized message content
	 * @return the content of the response
	 */
	public String checkMessagingConnectionForTheDm( String message )
	throws DebugWsException {

		this.logger.finer( "Checking messaging connection with the DM: message=" + message );

		WebResource path = this.resource.path( UrlConstants.DEBUG ).path( "check-dm" );
		if( message != null )
			path = path.queryParam( "message", message );

		ClientResponse response = this.wsClient.createBuilder( path ).get( ClientResponse.class );
		if( Family.SUCCESSFUL != response.getStatusInfo().getFamily()) {
			String value = response.getEntity( String.class );
			this.logger.finer( response.getStatusInfo() + ": " + value );
			throw new DebugWsException( response.getStatusInfo().getStatusCode(), value );
		}

		this.logger.finer( String.valueOf( response.getStatusInfo()));
		return response.getEntity( String.class );
	}


	/**
	 * Checks the DM can correctly exchange with an agent through the messaging server.
	 *
	 * @param applicationName  the name of the application holding the targeted agent
	 * @param scopedInstancePath the identifier of the targeted agent
	 * @param message          a customized message content
	 * @return the response to the agent connection check
	 */
	public String checkMessagingConnectionWithAgent( String applicationName, String scopedInstancePath, String message )
	throws DebugWsException {

		this.logger.finer( "Checking messaging connection with agent: applicationName=" + applicationName +
				", scoped instance path=" + scopedInstancePath + ", message=" + message );

		WebResource path = this.resource.path( UrlConstants.DEBUG ).path( "check-agent" );
		path = path.queryParam( "application-name", applicationName );
		path = path.queryParam( "scoped-instance-path", scopedInstancePath );
		if( message != null )
			path = path.queryParam( "message", message );

		ClientResponse response = this.wsClient.createBuilder( path ).get( ClientResponse.class );
		if( Family.SUCCESSFUL != response.getStatusInfo().getFamily()) {
			String value = response.getEntity( String.class );
			this.logger.finer( response.getStatusInfo() + ": " + value );
			throw new DebugWsException( response.getStatusInfo().getStatusCode(), value );
		}

		this.logger.finer( String.valueOf( response.getStatusInfo()));
		return response.getEntity( String.class );
	}


	/**
	 * Runs a diagnostic for a given instance.
	 * @return the instance
	 * @throws DebugWsException
	 */
	public Diagnostic diagnoseInstance( String applicationName, String instancePath )
	throws DebugWsException {

		this.logger.finer( "Diagnosing instance " + instancePath + " in application " + applicationName );

		WebResource path = this.resource.path( UrlConstants.DEBUG ).path( "diagnose-instance" );
		path = path.queryParam( "application-name", applicationName );
		path = path.queryParam( "instance-path", instancePath );

		ClientResponse response = this.wsClient.createBuilder( path )
						.accept( MediaType.APPLICATION_JSON )
						.get( ClientResponse.class );

		if( Family.SUCCESSFUL != response.getStatusInfo().getFamily()) {
			String value = response.getEntity( String.class );
			this.logger.finer( response.getStatusInfo() + ": " + value );
			throw new DebugWsException( response.getStatusInfo().getStatusCode(), value );
		}

		this.logger.finer( String.valueOf( response.getStatusInfo()));
		return response.getEntity( Diagnostic.class );
	}


	/**
	 * Runs a diagnostic for a given application.
	 * @return the diagnostic
	 */
	public List<Diagnostic> diagnoseApplication( String applicationName ) {

		this.logger.finer( "Diagnosing application " + applicationName );
		WebResource path = this.resource.path( UrlConstants.DEBUG ).path( "diagnose-application" );
		path = path.queryParam( "application-name", applicationName );

		List<Diagnostic> result = this.wsClient.createBuilder( path )
						.accept( MediaType.APPLICATION_JSON )
						.get( new GenericType<List<Diagnostic>> () {});

		if( result == null )
			this.logger.finer( "No diagnostic was returned for application " + applicationName );

		return result;
	}
}
