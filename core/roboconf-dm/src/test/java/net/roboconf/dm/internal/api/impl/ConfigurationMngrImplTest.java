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

import java.io.File;

import org.junit.Assert;
import net.roboconf.core.Constants;
import net.roboconf.dm.internal.utils.ConfigurationUtils;
import net.roboconf.dm.management.api.IConfigurationMngr;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ConfigurationMngrImplTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();
	private IConfigurationMngr mngr;


	@Before
	public void prepareMngr() {
		this.mngr = new ConfigurationMngrImpl();
	}


	@Test
	public void testDefault_notInKaraf() {

		File defaultConfigurationDirectory = new File( System.getProperty( "java.io.tmpdir" ), "roboconf-dm" );
		Assert.assertEquals( defaultConfigurationDirectory, this.mngr.getWorkingDirectory());
	}


	@Test
	public void testDefault_inKaraf() throws Exception {

		File dir = this.folder.newFolder();
		try {
			System.setProperty( "karaf.data", dir.getAbsolutePath());
			this.mngr = new ConfigurationMngrImpl();
			Assert.assertEquals( new File( dir, "roboconf" ), this.mngr.getWorkingDirectory());

		} finally {
			System.setProperty( "karaf.data", "" );
		}
	}


	@Test
	public void testFindIconFromPath() throws Exception {

		File dir = this.folder.newFolder();
		this.mngr.setWorkingDirectory( dir );
		Assert.assertEquals( dir, this.mngr.getWorkingDirectory());

		File appDir = ConfigurationUtils.findApplicationDirectory( "app", this.mngr.getWorkingDirectory());
		File descDir = new File( appDir, Constants.PROJECT_DIR_DESC );
		Assert.assertTrue( descDir.mkdirs());

		File trickFile = new File( descDir, "directory.jpg" );
		Assert.assertTrue( trickFile.mkdirs());
		Assert.assertNull( this.mngr.findIconFromPath( "/app/whatever.jpg" ));

		File singleJpgFile = new File( descDir, "whatever.jpg" );
		Assert.assertTrue( singleJpgFile.createNewFile());
		Assert.assertEquals( singleJpgFile, this.mngr.findIconFromPath( "/app/whatever.jpg" ));
		Assert.assertEquals( singleJpgFile, this.mngr.findIconFromPath( "/app/whatever.png" ));
		Assert.assertEquals( singleJpgFile, this.mngr.findIconFromPath( "/app/whatever.cool" ));
	}
}
