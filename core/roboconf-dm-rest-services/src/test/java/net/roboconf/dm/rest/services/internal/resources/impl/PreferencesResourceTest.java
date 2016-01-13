/**
 * Copyright 2015 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.dm.rest.services.internal.resources.impl;

import java.io.IOException;
import java.util.List;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import net.roboconf.core.model.runtime.Preference;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.rest.services.internal.resources.IPreferencesResource;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author Vincent Zurczak - Linagora
 */
public class PreferencesResourceTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private Manager manager;
	private IPreferencesResource resource;



	@Before
	public void resetManager() throws IOException {

		this.manager = new Manager();
		this.manager.configurationMngr().setWorkingDirectory( this.folder.newFolder());
		this.resource = new PreferencesResource( this.manager );
		this.manager.preferencesMngr().loadProperties();
	}


	@Test
	public void testGetAndSave() {

		List<Preference> prefs = this.resource.getAllPreferences();
		Assert.assertNotSame( 0, prefs.size());

		int size = prefs.size();
		Response resp = this.resource.savePreference( "my-key", "my-value" );
		Assert.assertEquals( Status.OK.getStatusCode(), resp.getStatus());

		prefs = this.resource.getAllPreferences();
		Assert.assertEquals( size + 1, prefs.size());

		boolean found = false;
		for( Preference pref : prefs ) {
			if( "my-key".equals( pref.getName())) {
				found = true;
				break;
			}
		}

		Assert.assertTrue( found );
	}


	@Test
	public void testSaveException() throws Exception {

		this.manager.configurationMngr().setWorkingDirectory( this.folder.newFile());
		Response resp = this.resource.savePreference( "my-key", "my-value" );
		Assert.assertEquals( Status.INTERNAL_SERVER_ERROR.getStatusCode(), resp.getStatus());
	}
}
