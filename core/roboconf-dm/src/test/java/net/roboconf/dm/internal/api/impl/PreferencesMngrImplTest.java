/**
 * Copyright 2016 Linagora, Université Joseph Fourier, Floralis
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

import net.roboconf.core.model.runtime.Preference;
import net.roboconf.core.model.runtime.Preference.PreferenceKeyCategory;
import net.roboconf.dm.management.api.IConfigurationMngr;
import net.roboconf.dm.management.api.IPreferencesMngr;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author Vincent Zurczak - Linagora
 */
public class PreferencesMngrImplTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private IConfigurationMngr configurationMngr;
	private IPreferencesMngr mngr;


	@Before
	public void prepareManager() throws IOException {

		this.configurationMngr = new ConfigurationMngrImpl();
		this.configurationMngr.setWorkingDirectory( this.folder.newFolder());
		this.mngr = new PreferencesMngrImpl( this.configurationMngr );
		this.mngr.loadProperties();
	}


	@Test
	public void testFromInexistingCache() throws Exception {

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
	}


	@Test
	public void testFromExistingCache() throws Exception {

		int expectedSize = PreferencesMngrImpl.DEFAULTS.keyToDefaultValue.size();
		Assert.assertEquals( expectedSize, this.mngr.getAllPreferences().size());
		this.mngr.save( IPreferencesMngr.FORBIDDEN_RANDOM_PORTS, "8154" );

		IPreferencesMngr newMngr = new PreferencesMngrImpl( this.configurationMngr );
		newMngr.loadProperties();

		Assert.assertEquals( expectedSize + 1, newMngr.getAllPreferences().size());
		Assert.assertEquals( "8154", newMngr.get( IPreferencesMngr.FORBIDDEN_RANDOM_PORTS ));
		Assert.assertEquals( "8154", newMngr.get( IPreferencesMngr.FORBIDDEN_RANDOM_PORTS, "def" ));
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
}
