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

package net.roboconf.dm.rest.services.internal;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import net.roboconf.dm.rest.services.internal.resources.IApplicationResource;
import net.roboconf.dm.rest.services.internal.resources.IManagementResource;

/**
 * @author Vincent Zurczak - Linagora
 */
public class RestApplication extends Application {

	private final IApplicationResource applicationResource;
	private final IManagementResource managementResource;


	/**
	 * Constructor.
	 * @param applicationResource
	 * @param managementResource
	 */
	public RestApplication( IApplicationResource applicationResource, IManagementResource managementResource ) {
		this.applicationResource = applicationResource;
		this.managementResource = managementResource;
	}


	@Override
	public Set<Object> getSingletons() {

		HashSet<Object> set = new HashSet<Object> ();
		set.add( this.applicationResource );
		set.add( this.managementResource );

		return set;
	}
}
