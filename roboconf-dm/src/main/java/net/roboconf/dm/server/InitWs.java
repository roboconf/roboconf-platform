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

import java.util.logging.Logger;

import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import net.roboconf.dm.management.Manager;
import net.roboconf.dm.rest.api.IInitWs;

/**
 * @author Vincent Zurczak - Linagora
 */
@Path( IInitWs.PATH )
public class InitWs implements IInitWs {

	private final Logger logger = Logger.getLogger( InitWs.class.getName());



	/* (non-Javadoc)
	 * @see net.roboconf.dm.rest.client.exceptions.server.IInitWs#init(java.lang.String)
	 */
	@Override
	public Response init( String amqpIp ) {

		this.logger.fine( "Request: initialize the DM with message server IP " + amqpIp );
		Response response;
		if( Manager.INSTANCE.tryToChangeMessageServerIp( amqpIp ))
			response = Response.ok().build();
		else
			response = Response.status( Status.FORBIDDEN ).entity( "There must be no application to be able to update the message server IP." ).build();

		return response;
	}


	/* (non-Javadoc)
	 * @see net.roboconf.dm.rest.client.exceptions.server.IInitWs#isInitialized()
	 */
	@Override
	public Response isInitialized() {

		this.logger.fine( "Request: determine whether the DM was already initialized." );
		boolean isInitialized = Manager.INSTANCE.getMessageServerIp() != null;
		String s = String.valueOf( isInitialized );

		return Response.ok( s ).build();
	}
}
