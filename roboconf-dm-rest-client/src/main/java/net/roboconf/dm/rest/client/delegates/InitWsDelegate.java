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

package net.roboconf.dm.rest.client.delegates;

import java.util.logging.Logger;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status.Family;

import net.roboconf.core.internal.utils.Utils;
import net.roboconf.dm.rest.UrlConstants;
import net.roboconf.dm.rest.client.exceptions.InitializationException;

import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;

/**
 * @author Vincent Zurczak - Linagora
 */
public class InitWsDelegate {

	private final WebResource resource;
	private final Logger logger;


	/**
	 * Constructor.
	 * @param resource a web resource
	 */
	public InitWsDelegate( WebResource resource ) {
		this.resource = resource;
		this.logger = Logger.getLogger( getClass().getName());
	}


	/**
	 * Initializes the deployment manager.
	 * @param amqpIp the IP address of the AMQP server
	 * <p>
	 * Null or empty will be changed to "localhost".
	 * </p>
	 *
	 * @throws UniformInterfaceException if something went wrong
	 * @throws ClientHandlerException if something went wrong
	 * @throws InitializationException if a problem occurred with the initialization
	 */
	public void initializeDeploymentManager( String amqpIp ) throws InitializationException {
		this.logger.finer( "Initializing the deployment manager..." );

		ClientResponse response;
		if( Utils.isEmptyOrWhitespaces( amqpIp ))
			amqpIp = "localhost";

		response = this.resource.path( UrlConstants.INIT ).accept( MediaType.TEXT_PLAIN ).post( ClientResponse.class, amqpIp );
		if( Family.SUCCESSFUL != response.getStatusInfo().getFamily()) {
			String value = response.getEntity( String.class );
			this.logger.finer( response.getStatusInfo() + ": " + value );
			throw new InitializationException( response.getStatusInfo().getStatusCode(), value );
		}

		this.logger.finer( String.valueOf( response.getStatusInfo()));
	}


	/**
	 * @return true if the DM was already initialized, false otherwise
	 * @throws UniformInterfaceException if something went wrong
	 * @throws ClientHandlerException if something went wrong
	 * @throws InitializationException if a problem occurred with the initialization
	 */
	public boolean isDeploymentManagerInitialized() throws InitializationException {
		this.logger.finer( "Checking if the deployment manager is initialized..." );

		ClientResponse response = this.resource.path( UrlConstants.INIT ).accept( MediaType.TEXT_PLAIN ).get( ClientResponse.class );
		String value = response.getEntity( String.class );
		this.logger.finer( response.getStatusInfo() + ": " + value );

		if( Family.SUCCESSFUL != response.getStatusInfo().getFamily())
			throw new InitializationException( response.getStatusInfo().getStatusCode(), value );

		return Boolean.valueOf( value );
	}
}
