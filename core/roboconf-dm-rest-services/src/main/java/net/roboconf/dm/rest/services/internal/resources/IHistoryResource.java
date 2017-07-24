/**
 * Copyright 2017 Linagora, Université Joseph Fourier, Floralis
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

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import net.roboconf.core.model.runtime.CommandHistoryItem;
import net.roboconf.dm.rest.commons.UrlConstants;

/**
 * Operations related to the history.
 * @author Vincent Zurczak - Linagora
 */
public interface IHistoryResource {

	String PATH = "/" + UrlConstants.HISTORY;


	/**
	 * Gets the paged history of commands execution.
	 * @param applicationName the expected application (can be null for all applications)
	 * @param pageNumber the page number
	 * @param itemsPerPage the number of items per page
	 * @param sortingCriteria the sort criteria (start / application / command / result / origin)
	 * @param sortingOrder the sorting order ("asc" or "desc")
	 * @return the commands history (never null)
	 *
	 * @HTTP 200 Everything went fine.
	 */
	@GET
	@Path( "/commands" )
	@Produces( MediaType.APPLICATION_JSON )
	List<CommandHistoryItem> getCommandHistory(
			@QueryParam("page") int pageNumber,
			@QueryParam("name") String applicationName,
			@QueryParam("itemsPerPage") int itemsPerPage,
			@QueryParam("sortingCriteria") String sortingCriteria,
			@QueryParam("sortingOrder") String sortingOrder );


	/**
	 * Gets the total number of pages for the history of commands execution.
	 * @param applicationName the expected application (can be null for all applications)
	 * @param itemsPerPage the number of items per page
	 * @return the commands history (never null)
	 *
	 * @HTTP 200 Everything went fine.
	 */
	@GET
	@Path( "/size/commands" )
	@Produces( MediaType.TEXT_PLAIN )
	Response getCommandHistoryNumberOfPages(
			@QueryParam("name") String applicationName,
			@QueryParam("itemsPerPage") int itemsPerPage );
}
