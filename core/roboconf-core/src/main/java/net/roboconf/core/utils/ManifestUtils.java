/**
 * Copyright 2015-2017 Linagora, Université Joseph Fourier, Floralis
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

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Vincent Zurczak - Linagora
 */
public final class ManifestUtils {

	static final String BUNDLE_VERSION = "bundle-version";


	/**
	 * Private empty constructor.
	 */
	private ManifestUtils() {
		// nothing
	}


	/**
	 * Finds the Roboconf version from the bundle version.
	 * <p>
	 * If the third digit in the version is 0, then we remove it.
	 * </p>
	 * <p>
	 * Bundle-version = 0.4.0 =&gt; 0.4
	 * <br />
	 * Bundle-version = 0.4.0-SNAPSHOT =&gt; 0.4-SNAPSHOT
	 * <br />
	 * Bundle-version = 0.4.1 =&gt; 0.4.1
	 * </p>
	 *
	 * @param bundleVersion the bundle version (can be null)
	 * @return null if the bundle version was null, or the updated version otherwise
	 */
	public static String findMavenVersion( String bundleVersion ) {

		String result = bundleVersion;
		if( bundleVersion != null ) {
			result = bundleVersion.replaceAll( "(?i:(\\.|-)SNAPSHOT$)", "-SNAPSHOT" );

			Pattern pattern = Pattern.compile( "(\\d+\\.\\d+)\\.0(-SNAPSHOT)?$" );
			Matcher m = pattern.matcher( result );
			if( m.find())
				result = m.replaceFirst( "$1$2" );
		}

		return result;
	}


	/**
	 * Finds the bundle version by reading the manifest file.
	 * @return a string, that can be null
	 */
	public static String findBundleVersion() {
		return findManifestProperty( BUNDLE_VERSION );
	}


	/**
	 * Finds a property in the MANIFEST file.
	 * @param propertyName the property's name
	 * @return the property's value, or null if it was not found
	 */
	public static String findManifestProperty( String propertyName ) {

		String result = null;
		InputStream is = null;
		try {
			is = ManifestUtils.class.getResourceAsStream( "/META-INF/MANIFEST.MF" );
			Properties props = new Properties();
			props.load( is );
			result = findManifestProperty( props, propertyName );

		} catch( IOException e ) {
			Logger logger = Logger.getLogger( ManifestUtils.class.getName());
			logger.warning( "Could not read the bundle manifest. " + e.getMessage());

		} finally {
			Utils.closeQuietly( is );
		}

		return result;
	}


	/**
	 * Finds a property in the MANIFEST properties.
	 * @param props the properties
	 * @param propertyName the property's name
	 * @return the property's value, or null if it was not found
	 */
	public static String findManifestProperty( Properties props, String propertyName ) {

		String result = null;
		for( Map.Entry<Object,Object> entry : props.entrySet()) {
			if( propertyName.equalsIgnoreCase( String.valueOf( entry.getKey()))) {
				result = String.valueOf( entry.getValue());
				break;
			}
		}

		return result;
	}
}
