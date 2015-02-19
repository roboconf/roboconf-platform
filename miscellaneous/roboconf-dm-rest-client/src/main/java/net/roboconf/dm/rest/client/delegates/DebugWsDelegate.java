/**
 * Copyright 2015-2015 Linagora, Université Joseph Fourier, Floralis
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

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.Component;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.dm.rest.client.exceptions.ApplicationException;
import net.roboconf.dm.rest.commons.UrlConstants;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status.Family;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author Vincent Zurczak - Linagora
 */
public class DebugWsDelegate {

	private final WebResource resource;
	private final Logger logger;


	/**
	 * Constructor.
	 *
	 * @param resource a web resource
	 */
	public DebugWsDelegate( WebResource resource ) {
		this.resource = resource;
		this.logger = Logger.getLogger( getClass().getName() );
	}


	/**
	 * Checks the DM is correctly connected with the messaging server.
	 *
	 * @param message a customized message content.
	 * @param timeout the timeout in milliseconds (ms) to wait before considering the message is lost.
	 * @return the content of the reponse.
	 */
	public String checkMessagingConnectionForTheDm( String message, long timeout )
			throws ApplicationException {

		this.logger.finer( "Checking messaging connection with the DM: message=" + message + ", timeout=" + timeout );

		WebResource path = this.resource.path( UrlConstants.DEBUG ).path( "check-dm" );
		if ( message != null )
			path = path.queryParam( "message", message );
		path = path.queryParam( "timeout", Long.toString( timeout ) );

		ClientResponse response = path.get( ClientResponse.class );
		if ( Family.SUCCESSFUL != response.getStatusInfo().getFamily() ) {
			String value = response.getEntity( String.class );
			this.logger.finer( response.getStatusInfo() + ": " + value );
			throw new ApplicationException( response.getStatusInfo().getStatusCode(), value );
		}

		this.logger.finer( String.valueOf( response.getStatusInfo() ) );
		return response.getEntity( String.class );
	}

	/**
	 * Checks the DM can correctly exchange with an agent through the messaging server.
	 *
	 * @param applicationName  the name of the application holding the targeted agent.
	 * @param rootInstanceName the identifier of the targeted agent.
	 * @param message          a customized message content.
	 * @param timeout          the timeout in milliseconds (ms) to wait before considering the message is lost.
	 * @return the response to the agent connection check.
	 */
	public String checkMessagingConnectionWithAgent( String applicationName,
	                                                 String rootInstanceName,
	                                                 String message,
	                                                 long timeout )
			throws ApplicationException {
		this.logger.finer( "Checking messaging connection with agent: applicationName=" + applicationName +
				", rootInstanceName=" + rootInstanceName + ", message=" + message + ", timeout=" + timeout );

		WebResource path = this.resource.path( UrlConstants.DEBUG ).path( "check-agent" );
		path = path.queryParam( "application-name", applicationName );
		path = path.queryParam( "root-instance-name", rootInstanceName );
		if ( message != null )
			path = path.queryParam( "message", message );
		path = path.queryParam( "timeout", Long.toString( timeout ) );

		ClientResponse response = path.get( ClientResponse.class );
		if ( Family.SUCCESSFUL != response.getStatusInfo().getFamily() ) {
			String value = response.getEntity( String.class );
			this.logger.finer( response.getStatusInfo() + ": " + value );
			throw new ApplicationException( response.getStatusInfo().getStatusCode(), value );
		}

		this.logger.finer( String.valueOf( response.getStatusInfo() ) );
		return response.getEntity( String.class );
	}

	/**
	 * Runs a diagnostic for a given instance.
	 *
	 * @return the instance
	 */
	public Instance diagnoseInstance( String applicationName,
	                                  String instancePath )
			throws ApplicationException {
		this.logger.finer( "Diagnosing instance: applicationName=" + applicationName + ", instancePath=" + instancePath);

		WebResource path = this.resource.path( UrlConstants.DEBUG ).path( "diagnose-instance" );
		path = path.queryParam( "application-name", applicationName );
		path = path.queryParam( "instance-path", instancePath );

		ClientResponse response = path.accept( MediaType.APPLICATION_JSON ).get( ClientResponse.class );
		if ( Family.SUCCESSFUL != response.getStatusInfo().getFamily() ) {
			String value = response.getEntity( String.class );
			this.logger.finer( response.getStatusInfo() + ": " + value );
			throw new ApplicationException( response.getStatusInfo().getStatusCode(), value );
		}

		this.logger.finer( String.valueOf( response.getStatusInfo() ) );
		return response.getEntity( Instance.class );
	}

	/**
	 * Runs a diagnostic for a given application.
	 *
	 * @return the application
	 */
	public Application diagnoseApplication( String applicationName)
			throws ApplicationException {
		this.logger.finer( "Diagnosing instance: applicationName=" + applicationName );

		WebResource path = this.resource.path( UrlConstants.DEBUG ).path( "diagnose-application" );
		path = path.queryParam( "application-name", applicationName );

		ClientResponse response = path.accept( MediaType.APPLICATION_JSON ).get( ClientResponse.class );
		if ( Family.SUCCESSFUL != response.getStatusInfo().getFamily() ) {
			String value = response.getEntity( String.class );
			this.logger.finer( response.getStatusInfo() + ": " + value );
			throw new ApplicationException( response.getStatusInfo().getStatusCode(), value );
		}

		this.logger.finer( String.valueOf( response.getStatusInfo() ) );
		return response.getEntity( Application.class );
	}



}
