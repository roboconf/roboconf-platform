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

package net.roboconf.dm.internal.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import net.roboconf.core.Constants;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.api.ITargetHandlerResolver;
import net.roboconf.dm.management.api.ITargetsMngr;
import net.roboconf.target.api.TargetException;

/**
 * Utilities related to deployment targetsMngr.
 * @author Vincent Zurczak - Linagora
 */
public final class TargetHelpers {

	/**
	 * Empty private constructor.
	 */
	private TargetHelpers() {
		// nothing
	}


	/**
	 * Verifies that all the target handlers an application needs are installed.
	 * @param targetHandlerResolver the DM's target resolver
	 * @param ma a managed application
	 * @param handlers a non-null list of handlers
	 */
	public static void verifyTargets(
			ITargetHandlerResolver targetHandlerResolver,
			ManagedApplication ma,
			ITargetsMngr targetsMngr ) {

		Logger logger = Logger.getLogger( TargetHelpers.class.getName());
		for( Instance inst : InstanceHelpers.getAllInstances( ma.getApplication())) {
			if( ! InstanceHelpers.isTarget( inst ))
				continue;

			try {
				String path = InstanceHelpers.computeInstancePath( inst );
				Map<String,String> targetProperties = targetsMngr.findTargetProperties( ma.getApplication(), path ).asMap();
				targetHandlerResolver.findTargetHandler( targetProperties );

			} catch( TargetException e ) {
				logger.warning( e.getMessage());
			}
		}
	}


	/**
	 * Expands the properties.
	 * <p>
	 * Property values that contain {{ ip }} will be updated.
	 * {{ ip }} will be replaced by the IP address of the root instance
	 * (which may - or not) be the same than the scoped instance).
	 * </p>
	 *
	 * @param scopedInstance the scoped instance (not null)
	 * @param targetProperties the target properties (not null)
	 * @return a new map equivalent to the input but with expanded properties)
	 */
	public static Map<String,String> expandProperties( Instance scopedInstance, Map<String,String> targetProperties ) {

		Logger logger = Logger.getLogger( TargetHelpers.class.getName());
		String ipAddress = scopedInstance.data.get( Instance.IP_ADDRESS );
		if( ipAddress == null )
			ipAddress = InstanceHelpers.findRootInstance( scopedInstance ).data.get( Instance.IP_ADDRESS );

		if( ipAddress == null )
			ipAddress = "";

		Properties params = new Properties();
		params.setProperty( Constants.SPECIFIC_VARIABLE_IP, ipAddress );

		Map<String,String> newTargetProperties = new HashMap<>( targetProperties.size());
		for( Map.Entry<String,String> entry : targetProperties.entrySet()) {
			String p = Utils.expandTemplate( entry.getValue(), params );
			newTargetProperties.put( entry.getKey(), p );

			if( ! p.equals( entry.getValue()))
				logger.fine( "Target property '" + entry.getKey() + "' was expanded to " + p );
		}

		return newTargetProperties;
	}


	/**
	 * Finds the target handler name in properties.
	 * <p>
	 * This methods checks for the presence of the {@value Constants.TARGET_PROPERTY_HANDLER}
	 * property, or for the old {@value LEGACY_HANDLER_PROPERTY} one.
	 * </p>
	 *
	 * @param props non-null properties
	 * @return a handler name, or null if none was specified
	 */
	public static String findTargetHandlerName( Properties props ) {
		return props.getProperty( Constants.TARGET_PROPERTY_HANDLER );
	}


	/**
	 * Finds the target handler name in properties.
	 * <p>
	 * This methods checks for the presence of the {@value Constants.TARGET_PROPERTY_HANDLER}
	 * property, or for the old {@value LEGACY_HANDLER_PROPERTY} one.
	 * </p>
	 *
	 * @param props non-null map
	 * @return a handler name, or null if none was specified
	 */
	public static String findTargetHandlerName( Map<String,String> props ) {
		return props.get( Constants.TARGET_PROPERTY_HANDLER );
	}
}
