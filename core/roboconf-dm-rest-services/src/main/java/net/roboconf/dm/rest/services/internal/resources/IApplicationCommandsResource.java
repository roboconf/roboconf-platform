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

package net.roboconf.dm.rest.services.internal.resources;

import java.io.IOException;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import net.roboconf.dm.rest.commons.UrlConstants;

/**
 * The REST API to manipulate commands in DM.
 * @author Amadou Diarra - Université Joseph Fourier
 */
public interface IApplicationCommandsResource {

	String PATH = "/" + UrlConstants.APP + "/{name}/commands";

	/**
	 * Creates or updates a command from its instructions.
	 * @param app the associated application
	 * @param commandName the command name (must be unique)
	 * @param commandText the instructions contained in the command (must be valid)
	 * @throws IOException if something went wrong
	 *
	 * @HTTP 200 everything went fine
	 * @HTTP 404 the application was not found
	 */
	@POST
	Response createOrUpdateCommand(@PathParam("name") String app, @QueryParam("command-name") String commandName, String commandText);


	/**
	 * Deletes a command.
	 * @param app the associated application
	 * @param commandName the command name
	 * @throws IOException if something went wrong
	 *
	 * @HTTP 200 everything went fine
	 * @HTTP 404 the application was not found
	 */
	@DELETE
	@Path( "{command-name}" )
	Response deleteCommand(@PathParam("name") String app, @PathParam("command-name") String commandName);


	/**
	 * Gets the instructions contained by a command.
	 * @param app the associated application
	 * @param commandName the command name
	 * @return the commands content (never null)
	 * @throws IOException if something went wrong
	 *
	 * @HTTP 200 everything went fine
	 * @HTTP 204 no instruction was found
	 */
	@GET
	@Path( "{command-name}" )
	Response getCommandInstructions(@PathParam("name") String app,@PathParam("command-name") String commandName);
}
