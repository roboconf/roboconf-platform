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

import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;

import net.roboconf.core.model.runtime.Preference;
import net.roboconf.dm.rest.client.WsClient;
import net.roboconf.dm.rest.commons.UrlConstants;

/**
 * @author Vincent Zurczak - Linagora
 */
public class PreferencesWsDelegate {

	private final WebResource resource;
	private final Logger logger;
	private final WsClient wsClient;


	/**
	 * Constructor.
	 * @param resource a web resource
	 * @param the WS client
	 */
	public PreferencesWsDelegate( WebResource resource, WsClient wsClient ) {
		this.resource = resource;
		this.wsClient = wsClient;
		this.logger = Logger.getLogger( getClass().getName());
	}


	/**
	 * Lists all the preferences.
	 */
	public List<Preference> listPreferences() {

		this.logger.finer( "Getting all the preferences."  );

		WebResource path = this.resource.path( UrlConstants.PREFERENCES );
		List<Preference> result = this.wsClient.createBuilder( path )
				.accept( MediaType.APPLICATION_JSON )
				.get( new GenericType<List<Preference>> () {});

		if( result != null )
			this.logger.finer( result.size() + " preferences were found on the DM." );
		else
			this.logger.finer( "No preference was found on the DM." );

		return result != null ? result : new ArrayList<Preference> ();
	}
}
