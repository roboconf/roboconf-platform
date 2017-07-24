/**
 * Copyright 2016-2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.core.utils;

import java.util.HashMap;
import java.util.Map;

import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.helpers.InstanceHelpers;

/**
 * @author Vincent Zurczak - Linagora
 */
public final class DockerAndScriptUtils {

	public static final String ROBOCONF_INSTANCE_NAME = "ROBOCONF_INSTANCE_NAME";
	public static final String ROBOCONF_INSTANCE_PATH = "ROBOCONF_INSTANCE_PATH";
	public static final String ROBOCONF_CLEAN_INSTANCE_PATH = "ROBOCONF_CLEAN_INSTANCE_PATH";
	public static final String ROBOCONF_CLEAN_REVERSED_INSTANCE_PATH = "ROBOCONF_CLEAN_REVERSED_INSTANCE_PATH";
	public static final String ROBOCONF_COMPONENT_NAME = "ROBOCONF_COMPONENT_NAME";


	/**
	 * Private empty constructor.
	 */
	private DockerAndScriptUtils() {
		// nothing
	}


	/**
	 * Builds a map with the variables defined by this class.
	 * @param instance a non-null instance
	 * @return a non-null map where all the properties here are mapped to their values for this instance
	 */
	public static Map<String,String> buildReferenceMap( Instance instance ) {

		Map<String,String> result = new HashMap<> ();
		String instancePath = InstanceHelpers.computeInstancePath( instance );

		result.put( ROBOCONF_INSTANCE_NAME, instance.getName());
		result.put( ROBOCONF_INSTANCE_PATH, instancePath );
		result.put( ROBOCONF_COMPONENT_NAME, instance.getComponent().getName());
		result.put( ROBOCONF_CLEAN_INSTANCE_PATH, cleanInstancePath( instancePath ));
		result.put( ROBOCONF_CLEAN_REVERSED_INSTANCE_PATH, cleanReversedInstancePath( instancePath ));

		return result;
	}


	/**
	 * Cleans an instance path.
	 * <p>
	 * In fact, this method creates a string from an instance path
	 * and replaces all the non-alphanumeric characters by an underscore.
	 * This can be used to create proper IDs to use in scripts (e.g. to name
	 * a Docker container - see #506).
	 * </p>
	 *
	 * @param instancePath a non-null instance path
	 * @return a non-null string
	 */
	public static String cleanInstancePath( String instancePath ) {
		return instancePath.replaceFirst( "^/", "" ).replaceAll( "[\\W]", "_" );
	}


	/**
	 * Reverses and cleans an instance path.
	 * <p>
	 * In fact, this method creates a string from an instance path
	 * that is reverted (the root becomes the leaf and the lead the root).
	 * Then, it cleans it with {@link #cleanInstancePath(String)}.
	 * This can be used to create proper (and readable) IDs to use in scripts (e.g. to name
	 * a Docker container - see #506).
	 * </p>
	 *
	 * @param instancePath a non-null instance path
	 * @return a non-null string
	 */
	public static String cleanReversedInstancePath( String instancePath ) {

		StringBuilder sb = new StringBuilder();
		for( String s : instancePath.split( "/" )) {
			if( Utils.isEmptyOrWhitespaces( s ))
				continue;

			sb.insert( 0, s );
			sb.insert( 0, "/" );
		}

		return cleanInstancePath( sb.toString());
	}
}
