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

package net.roboconf.dm.rest.services.internal.resources.impl;

import java.util.List;
import java.util.logging.Logger;

import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import net.roboconf.core.model.runtime.CommandHistoryItem;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.rest.services.internal.resources.IHistoryResource;

/**
 * @author Vincent Zurczak - Linagora
 */
@Path( IHistoryResource.PATH )
public class HistoryResource implements IHistoryResource {

	private final Logger logger = Logger.getLogger( getClass().getName());
	private final Manager manager;


	/**
	 * Constructor.
	 * @param manager the manager
	 */
	public HistoryResource( Manager manager ) {
		this.manager = manager;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.dm.rest.services.internal.resources.IHistoryResource
	 * #getCommandHistoryNumberOfPages(java.lang.String, int)
	 */
	@Override
	public Response getCommandHistoryNumberOfPages( String applicationName, int itemsPerPage ) {

		this.logger.fine("Request: get the number of pages for commands history.");
		int number = this.manager.commandsMngr().getHistoryNumberOfPages( itemsPerPage, applicationName );
		return Response.ok( String.valueOf( number )).build();
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.dm.rest.services.internal.resources.IHistoryResource
	 * #getCommandHistory(int, java.lang.String, int, java.lang.String, java.lang.String)
	 */
	@Override
	public List<CommandHistoryItem> getCommandHistory(
			int pageNumber,
			String applicationName,
			int itemsPerPage,
			String sortingCriteria,
			String sortingOrder ) {

		if( pageNumber < 1 )
			pageNumber = 1;

		this.logger.fine( "Request: get the history of commands execution (page = " + pageNumber + ")." );
		return this.manager.commandsMngr().getHistory(
				(pageNumber -1) * itemsPerPage,
				itemsPerPage,
				sortingCriteria,
				sortingOrder,
				applicationName );
	}
}
