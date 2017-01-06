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

package net.roboconf.dm.internal.api.impl;

import java.io.IOException;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.LinkedHashSet;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import net.roboconf.core.model.runtime.Preference;
import net.roboconf.core.model.runtime.Preference.PreferenceKeyCategory;
import net.roboconf.dm.management.api.IPreferencesMngr;

/**
 * @author Vincent Zurczak - Linagora
 */
public class PreferencesMngrImplTest {

	private IPreferencesMngr mngr;


	@Before
	public void prepareManager() throws IOException {
		this.mngr = new PreferencesMngrImpl();
	}


	@Test
	public void testForCoverage() {
		((PreferencesMngrImpl) this.mngr).start();
		((PreferencesMngrImpl) this.mngr).stop();
	}


	@Test
	public void testStorage() throws Exception {

		int expectedSize = PreferencesMngrImpl.DEFAULTS.keyToDefaultValue.size();
		Assert.assertEquals( expectedSize, this.mngr.getAllPreferences().size());
		Assert.assertEquals( "def", this.mngr.get( "key", "def" ));
		Assert.assertNull( this.mngr.get( "key" ));

		this.mngr.save( IPreferencesMngr.FORBIDDEN_RANDOM_PORTS, "87954" );
		Assert.assertEquals( expectedSize + 1, this.mngr.getAllPreferences().size());
		Assert.assertEquals( "87954", this.mngr.get( IPreferencesMngr.FORBIDDEN_RANDOM_PORTS ));
		Assert.assertEquals( "87954", this.mngr.get( IPreferencesMngr.FORBIDDEN_RANDOM_PORTS, "def" ));

		this.mngr.delete( IPreferencesMngr.FORBIDDEN_RANDOM_PORTS );
		Assert.assertEquals( expectedSize, this.mngr.getAllPreferences().size());
		Assert.assertNull( this.mngr.get( IPreferencesMngr.FORBIDDEN_RANDOM_PORTS ));
		Assert.assertEquals( "def", this.mngr.get( IPreferencesMngr.FORBIDDEN_RANDOM_PORTS, "def" ));

		this.mngr.save( IPreferencesMngr.FORBIDDEN_RANDOM_PORTS, null );
		Assert.assertEquals( "", this.mngr.get( IPreferencesMngr.FORBIDDEN_RANDOM_PORTS ));
	}


	@Test
	@SuppressWarnings( "rawtypes" )
	public void testSaveWithConfigAdmin() throws Exception {

		Dictionary properties = new Hashtable<> ();
		ConfigurationAdmin configAdmin = Mockito.mock( ConfigurationAdmin.class );
		Configuration config = Mockito.mock( Configuration.class );
		Mockito.when( configAdmin.getConfiguration( PreferencesMngrImpl.PID, null )).thenReturn( config );
		Mockito.when( config.getProperties()).thenReturn( properties );

		((PreferencesMngrImpl) this.mngr).setConfigAdmin( configAdmin );
		Mockito.verifyZeroInteractions( configAdmin );
		Mockito.verifyZeroInteractions( config );

		this.mngr.save( "my key", "my value" );
		Mockito.verify( configAdmin, Mockito.only()).getConfiguration( PreferencesMngrImpl.PID, null );
		Mockito.verify( config, Mockito.times( 1 )).getProperties();
		Mockito.verify( config, Mockito.times( 1 )).update( properties );
		Mockito.verifyNoMoreInteractions( config );
		Assert.assertEquals( 1, properties.size());
		Assert.assertEquals( "my value", properties.get( "my key" ));
	}


	@Test
	@SuppressWarnings( { "rawtypes", "unchecked" } )
	public void testCacheUpdate() throws Exception {

		int expectedSize = PreferencesMngrImpl.DEFAULTS.keyToDefaultValue.size();
		Assert.assertEquals( expectedSize, this.mngr.getAllPreferences().size());

		Dictionary properties = new Hashtable();
		properties.put( IPreferencesMngr.FORBIDDEN_RANDOM_PORTS, "8154" );
		properties.put( "something", "" );
		this.mngr.updateProperties( properties );

		Assert.assertEquals( properties.size(), this.mngr.getAllPreferences().size());
		Assert.assertEquals( "8154", this.mngr.get( IPreferencesMngr.FORBIDDEN_RANDOM_PORTS ));
		Assert.assertEquals( "8154", this.mngr.get( IPreferencesMngr.FORBIDDEN_RANDOM_PORTS, "def" ));
		Assert.assertEquals( "", this.mngr.get( "something" ));
	}


	@Test
	public void testMailProperties() throws Exception {

		// Verify only mail properties are retrieved
		this.mngr.save( IPreferencesMngr.FORBIDDEN_RANDOM_PORTS, "8154" );
		for( Object key : this.mngr.getJavaxMailProperties().keySet())
			Assert.assertTrue((String) key, ((String) key).startsWith( "mail." ));

		// Check category associations
		for( Preference pref : this.mngr.getAllPreferences()) {
			String name = pref.getName().toLowerCase();
			boolean match = name.startsWith( "mail." ) || name.startsWith( "email." );
			if( match )
				Assert.assertEquals( name, PreferenceKeyCategory.EMAIL, pref.getCategory());

			if( pref.getCategory() == PreferenceKeyCategory.EMAIL )
				Assert.assertTrue( name, match );
		}
	}


	@Test
	public void testPropertiesAsList() throws Exception {

		final String key = "whatever";

		String value = this.mngr.get( key );
		Assert.assertNull( value );
		Assert.assertEquals( 0, this.mngr.getAsCollection( key ).size());

		this.mngr.addToList( key, "v1" );
		value = this.mngr.get( key );
		Assert.assertEquals( "v1", value );
		Assert.assertEquals(
				new LinkedHashSet<>( Arrays.asList( "v1" )),
				this.mngr.getAsCollection( key ));

		this.mngr.addToList( key, "v3" );
		this.mngr.addToList( key, "v2" );
		value = this.mngr.get( key );
		Assert.assertEquals( "v1, v3, v2", value );
		Assert.assertEquals(
				new LinkedHashSet<>( Arrays.asList( "v1", "v3", "v2" )),
				this.mngr.getAsCollection( key ));

		this.mngr.removeFromList( key, "v3" );
		value = this.mngr.get( key );
		Assert.assertEquals( "v1, v2", value );
		Assert.assertEquals(
				new LinkedHashSet<>( Arrays.asList( "v1", "v2" )),
				this.mngr.getAsCollection( key ));

		this.mngr.removeFromList( key, "v3" );
		this.mngr.removeFromList( key, "v2" );
		this.mngr.removeFromList( key, "v1" );
		value = this.mngr.get( key );
		Assert.assertEquals( "", value );
		Assert.assertEquals( 0, this.mngr.getAsCollection( key ).size());
	}
}
