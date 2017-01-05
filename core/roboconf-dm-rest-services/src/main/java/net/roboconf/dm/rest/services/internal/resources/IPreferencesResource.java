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

package net.roboconf.dm.rest.services.internal.resources;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import net.roboconf.core.model.runtime.Preference;
import net.roboconf.dm.rest.commons.UrlConstants;

/**
 * The REST API to set global preferences.
 * <p>
 * Implementing classes have to define the "Path" annotation
 * on the class. Use {@link #PATH}.
 * </p>
 *
 * @author Vincent Zurczak - Linagora
 */
public interface IPreferencesResource {

	String PATH = "/" + UrlConstants.PREFERENCES;


	/**
	 * Get a specific or all the preferences.
	 * @param key a specific key, or null to get all the properties
	 * @return a non-null list of preferences
	 *
	 * @HTTP 200 Everything went fine.
	 */
	@GET
	@Produces( MediaType.APPLICATION_JSON )
	List<Preference> getPreferences( @QueryParam("key") String key );


	/**
	 * Changes a preference value.
	 * <p>
	 * Notice that preferences cannot be deleted (and created) through
	 * the REST API.
	 * </p>
	 *
	 * @param key the preference name
	 * @param value the preference value
	 * @return a response
	 *
	 * @HTTP 200 Everything went fine.
	 */
	@POST
	Response savePreference( @QueryParam("key") String key, @QueryParam("value") String value );
}
