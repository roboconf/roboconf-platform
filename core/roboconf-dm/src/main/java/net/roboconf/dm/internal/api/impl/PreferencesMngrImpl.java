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

package net.roboconf.dm.internal.api.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import net.roboconf.core.model.runtime.Preference;
import net.roboconf.core.model.runtime.Preference.PreferenceKeyCategory;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.management.api.IPreferencesMngr;

/**
 * Unlike other APIs, this one is managed by iPojo.
 * <p>
 * This allows administrators to define preferences through usual
 * CFG files. It also provides a way for the REST API to access and retrieve preferences.
 * </p>
 *
 * @author Vincent Zurczak - Linagora
 */
public class PreferencesMngrImpl implements IPreferencesMngr {

	// Constants
	static final String PID = "net.roboconf.dm.preferences";
	static final Defaults DEFAULTS = new Defaults();

	// Injected by iPojo
	private ConfigurationAdmin configAdmin;

	// Internal fields
	private final Logger logger = Logger.getLogger( getClass().getName());
	private final ConcurrentHashMap<String,String> cache = new ConcurrentHashMap<> ();



	/**
	 * Constructor.
	 */
	public PreferencesMngrImpl() {
		this.cache.putAll( DEFAULTS.keyToDefaultValue );
	}


	/**
	 * Invoked by iPojo when one or several properties were updated from Config Admin.
	 * @param properties
	 */
	@Override
	public void updateProperties( Dictionary<?,?> properties ) {

		// Ignore iPojo properties
		final List<String> propertiesToSkip = Arrays.asList(
				"component",
				"felix.fileinstall.filename" );

		Map<String,String> map = new HashMap<> ();
		for( Enumeration<?> en = properties.keys(); en.hasMoreElements(); ) {

			Object key = en.nextElement();
			String keyAsString = String.valueOf( key );
			if( propertiesToSkip.contains( keyAsString ))
				continue;

			// "null" are not acceptable values in dictionaries
			// (OSGi often use Hash tables)
			Object value = properties.get( key );
			map.put( keyAsString, String.valueOf( value ));
		}

		this.cache.clear();
		this.cache.putAll( map );
		this.logger.fine( "Preferences were updated in bulk mode." );
	}


	/**
	 * Invoked by iPojo when the component is started.
	 */
	public void start() {
		this.logger.info( "The DM preferences were started." );
	}


	/**
	 * Invoked by iPojo when the component is stopped.
	 */
	public void stop() {
		this.logger.info( "The DM preferences were stopped." );
	}


	/**
	 * @param configAdmin the configAdmin to set
	 */
	public void setConfigAdmin( ConfigurationAdmin configAdmin ) {
		this.configAdmin = configAdmin;
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
	@SuppressWarnings( "unchecked" )
	public void save( String key, String value ) throws IOException {

		// Log
		this.logger.fine( "Preference with key '" + key + "' is being updated." );
		this.logger.finest( "New preference value: " + key + " = " + value );

		// Update the cache right away
		String realValue = value == null ? "" : value;
		this.cache.put( key, realValue );

		// Update the config admin property.
		// This will invoke (indirectly) the "updateProperties" method.
		// It will simply update the cache, it does not cost a lot.
		if( this.configAdmin != null ) {
			Configuration config = this.configAdmin.getConfiguration( PID, null );

			// Do NOT be too smart here.
			// Felix's implementation returns a new object every time
			// one invoke "config.getProperties()". So, we do not have a direct
			// access to the properties and "config.update()" does not work.
			//
			// So, we store the result of "config.getProperties()" in a variable.
			// We update this temporary map. And then, we pass the map as a parameter
			// to "config.update()". Manual tests with Karaf have shown it does not work
			// otherwise.
			Dictionary<Object,Object> props = config.getProperties();
			props.put( key, realValue );
			config.update( props );

		} else {
			this.logger.warning( "Config Admin is not available." );
		}

		// We need to mix both approaches here so that this API works in
		// both OSGi and non-OSGi environments.
	}


	@Override
	public void addToList( String key, String value ) throws IOException {

		Collection<String> values = getPreferencesAsCollection( key );
		values.add( value );
		save( key, Utils.format( values, ", " ));
	}


	@Override
	public void removeFromList( String key, String value ) throws IOException {

		Collection<String> values = getPreferencesAsCollection( key );
		values.remove( value );
		save( key, Utils.format( values, ", " ));
	}


	@Override
	public Collection<String> getAsCollection( String key ) {
		return getPreferencesAsCollection( key );
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


	private Set<String> getPreferencesAsCollection( String key ) {

		String listAsString = get( key, "" );
		List<String> list = Utils.filterEmptyValues( Utils.splitNicely( listAsString, "," ));
		return new LinkedHashSet<>( list );
	}
}
