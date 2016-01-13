/**
 * Copyright 2015-2016 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.dm.internal.api.impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import net.roboconf.core.model.runtime.Preference;
import net.roboconf.core.model.runtime.Preference.PreferenceKeyCategory;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.management.api.IConfigurationMngr;
import net.roboconf.dm.management.api.IPreferencesMngr;

/**
 * @author Vincent Zurczak - Linagora
 */
public class PreferencesMngrImpl implements IPreferencesMngr {

	static final String FILE_NAME = "preferences.properties";
	static final Defaults DEFAULTS = new Defaults();

	private final Logger logger = Logger.getLogger( getClass().getName());
	private final ConcurrentHashMap<String,String> cache = new ConcurrentHashMap<> ();
	private final IConfigurationMngr configurationMngr;



	/**
	 * Constructor.
	 * @param configurationMngr the configuration manager
	 */
	public PreferencesMngrImpl( IConfigurationMngr configurationMngr ) {
		this.configurationMngr = configurationMngr;
	}


	@Override
	public void loadProperties() {

		// At the beginning, this was done in the constructor.
		// But it made tests complicated as preferences were loaded before
		// we could change the manager's working directory.

		// Build a new map
		Map<String,String> snapshot = new HashMap<> ();
		Properties props = Utils.readPropertiesFileQuietly( getPropertiesFile(), this.logger );
		for( Map.Entry<Object,Object> entry : props.entrySet()) {
			snapshot.put((String) entry.getKey(), (String) entry.getValue());
		}

		if( snapshot.isEmpty())
			snapshot.putAll( DEFAULTS.keyToDefaultValue );

		// Replace the cache by the new map
		this.cache.clear();
		this.cache.putAll( snapshot );
	}


	@Override
	public String get( String key ) {
		return this.cache.get( key );
	}


	@Override
	public String get( String key, String defaultValue ) {
		return this.cache.containsKey( key ) ? this.cache.get( key ) : defaultValue;
	}


	@Override
	public void save( String key, String value ) throws IOException {

		this.cache.put( key, value );
		Properties props = new Properties();
		props.putAll( this.cache );
		Utils.writePropertiesFile( props, getPropertiesFile());
	}


	@Override
	public String delete( String key ) {
		return this.cache.remove( key );
	}


	@Override
	public Properties getJavaxMailProperties() {

		Properties result = new Properties();
		for( Map.Entry<String,String> entry : this.cache.entrySet()) {
			if( ! entry.getKey().startsWith( "mail." ))
				continue;

			result.setProperty( entry.getKey(), entry.getValue());
		}

		return result;
	}


	@Override
	public List<Preference> getAllPreferences() {

		List<Preference> result = new ArrayList<> ();
		for( Map.Entry<String,String> entry : this.cache.entrySet()) {
			PreferenceKeyCategory category = DEFAULTS.keyToCategory.get( entry.getKey());
			Preference p = new Preference( entry.getKey(), entry.getValue(), category );
			result.add( p );
		}

		return result;
	}


	private File getPropertiesFile() {
		return new File( this.configurationMngr.getWorkingDirectory(), FILE_NAME );
	}
}
