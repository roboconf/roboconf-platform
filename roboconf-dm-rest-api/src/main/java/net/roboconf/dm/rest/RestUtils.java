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

package net.roboconf.dm.rest;

import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.runtime.Application;
import net.roboconf.core.model.runtime.Instance;

/**
 * A set of utilities related to REST.
 * <p>
 * These functions were added because instance paths contain slashes ('/').
 * Since instance paths are used as URL parameters in Jersey, we need to escape them.
 * </p>
 *
 * @author Vincent Zurczak - Linagora
 */
public class RestUtils {

	/**
	 * Creates a restful path for an instance.
	 * <p>
	 * It consists in replacing '/' by '|'.
	 * </p>
	 *
	 * @param inst an instance (not null)
	 * @return a string (not null)
	 */
	public static String toRestfulPath( Instance inst ) {
		return toRestfulPath( InstanceHelpers.computeInstancePath( inst ));
	}


	/**
	 * Creates a restful path for an instance.
	 * <p>
	 * It consists in replacing '/' by '|'.
	 * </p>
	 *
	 * @param instancePath an instance path (not null)
	 * @return a string (not null)
	 */
	public static String toRestfulPath( String instancePath ) {
		return instancePath.replace( '/', '|' );
	}


	/**
	 * Converts a 'restful' instance path to a normal instance path.
	 * @param instancePath an instance path (not null)
	 * @return an instance path (not null)
	 */
	public static String fromRestfulPath( String instancePath ) {
		return instancePath.replace( '|', '/' );
	}


	/**
	 * Finds an instance from a restful instance path.
	 * @param application the application
	 * @param restfulInstancePath a restful instance path (not null)
	 * @return an instance, or null if it was not found
	 */
	public static Instance findInstanceFromRestfulPath( Application application, String restfulInstancePath ) {
		String realInstancePath = fromRestfulPath( restfulInstancePath );
		return InstanceHelpers.findInstanceByPath( application, realInstancePath );
	}
}
