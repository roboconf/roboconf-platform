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

package net.roboconf.dm.rest.api;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import net.roboconf.dm.rest.UrlConstants;

/**
 * The REST API to initialize the DM.
 * <p>
 * Implementing classes have to define the "Path" annotation
 * on the class. Use {@link #PATH}.
 * </p>
 *
 * @author Vincent Zurczak - Linagora
 */
public interface IInitWs {

	String PATH = "/" + UrlConstants.INIT;


	/**
	 * Initializes the DM and indicates the location of the AMQP server.
	 * @param amqpIp the IP address of the AMQP server
	 * @return a response
	 */
	@POST
	@Produces( MediaType.TEXT_PLAIN )
	Response init( String amqpIp );


	/**
	 * @return true if the DM was initialized
	 */
	@GET
	@Produces( MediaType.TEXT_PLAIN )
	Response isInitialized();
}
