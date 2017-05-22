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

package net.roboconf.dm.internal.utils;

import java.io.File;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import net.roboconf.core.Constants;
import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.model.RuntimeModelIo.InstancesLoadResult;
import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.dm.management.ManagedApplication;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ConfigurationUtilsTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();
	private File dir;


	@Before
	public void resetConfiguration() throws Exception {
		this.dir = this.folder.newFolder();
	}


	@Test
	public void testRestoreInstances_empty() throws Exception {

		TestApplication app = new TestApplication();
		app.setDirectory( ConfigurationUtils.findApplicationDirectory( app.getName(), this.dir ));

		ManagedApplication ma = new ManagedApplication( app );
		InstancesLoadResult ilr = ConfigurationUtils.restoreInstances( ma );
		Assert.assertEquals( 0, ilr.getLoadErrors().size());
		Assert.assertEquals( 0, ilr.getRootInstances().size());
	}


	@Test
	public void testSaveAndRestoreInstances() throws Exception {

		// Save...
		TestApplication app = new TestApplication();
		app.getMySqlVm().status( InstanceStatus.DEPLOYED_STARTED );
		app.getMySqlVm().data.put( Instance.IP_ADDRESS, "192.168.1.12" );
		app.getMySqlVm().data.put( Instance.MACHINE_ID, "xx45s5s645" );
		app.getMySql().status( InstanceStatus.DEPLOYED_STOPPED );

		app.getMySqlVm().data.put( Instance.APPLICATION_NAME, app.getName());
		app.getTomcatVm().data.put( Instance.APPLICATION_NAME, app.getName());

		app.setDirectory( ConfigurationUtils.findApplicationDirectory( app.getName(), this.dir ));
		ManagedApplication ma = new ManagedApplication( app );
		ConfigurationUtils.saveInstances( ma );

		// ... and restore...
		InstancesLoadResult ilr = ConfigurationUtils.restoreInstances( ma );
		Assert.assertEquals( 0, ilr.getLoadErrors().size());

		Application restoredApp = new Application( "test", null );
		restoredApp.getRootInstances().addAll( ilr.getRootInstances());

		// ... before comparing...
		Assert.assertEquals( app.getRootInstances().size(), restoredApp.getRootInstances().size());
		Assert.assertEquals( InstanceHelpers.getAllInstances( app ).size(), InstanceHelpers.getAllInstances( restoredApp ).size());
		for( Instance restoredInst : InstanceHelpers.getAllInstances( restoredApp )) {

			Instance inst = InstanceHelpers.findInstanceByPath( app, InstanceHelpers.computeInstancePath( restoredInst ));
			Assert.assertNotNull( inst );
			Assert.assertEquals( inst.getName(), restoredInst.getName());
			Assert.assertEquals( inst.channels, restoredInst.channels );
			Assert.assertEquals( inst.getStatus(), restoredInst.getStatus());
			Assert.assertEquals( inst.getComponent(), restoredInst.getComponent());
			Assert.assertEquals( inst.overriddenExports.size(), restoredInst.overriddenExports.size());
			Assert.assertEquals( inst.data.size(), restoredInst.data.size());

			for( Map.Entry<String,String> entry : restoredInst.data.entrySet()) {
				Assert.assertTrue( inst.data.containsKey( entry.getKey()));
				Assert.assertEquals( inst.data.get( entry.getKey()), entry.getValue());
			}
		}
	}


	@Test
	public void testFindTemplateDirectory() {

		File configurationDirectory = new File( "somewhere" );

		ApplicationTemplate tpl = new ApplicationTemplate( "test" );
		Assert.assertEquals( "test", ConfigurationUtils.findTemplateDirectory( tpl, configurationDirectory ).getName());

		tpl.setVersion( "v2" );
		Assert.assertEquals( "test - v2", ConfigurationUtils.findTemplateDirectory( tpl, configurationDirectory ).getName());
	}


	@Test
	public void testFindIcon_app() throws Exception {

		File configDir = this.folder.newFolder();
		File appDir = ConfigurationUtils.findApplicationDirectory( "app", configDir );
		File descDir = new File( appDir, Constants.PROJECT_DIR_DESC );
		Assert.assertTrue( descDir.mkdirs());

		File trickFile = new File( descDir, "directory.jpg" );
		Assert.assertTrue( trickFile.mkdirs());
		Assert.assertNull( ConfigurationUtils.findIcon( "app", null, configDir ));

		File singleJpgFile = new File( descDir, "whatever.jpg" );
		Assert.assertTrue( singleJpgFile.createNewFile());
		Assert.assertEquals( singleJpgFile, ConfigurationUtils.findIcon( "app", null, configDir ));

		File defaultFile = new File( descDir, "application.sVg" );
		Assert.assertTrue( defaultFile.createNewFile());
		Assert.assertEquals( defaultFile, ConfigurationUtils.findIcon( "app", null, configDir ));
		Assert.assertEquals( defaultFile, ConfigurationUtils.findIcon( "app", "", configDir ));
	}


	@Test
	public void testFindIcon_tpl() throws Exception {

		File configDir = this.folder.newFolder();
		ApplicationTemplate tpl = new ApplicationTemplate( "app" ).version( "v1" );

		File appDir = ConfigurationUtils.findTemplateDirectory( tpl, configDir );
		File descDir = new File( appDir, Constants.PROJECT_DIR_DESC );
		Assert.assertTrue( descDir.mkdirs());

		File trickFile = new File( descDir, "file.txt" );
		Assert.assertTrue( trickFile.createNewFile());
		Assert.assertNull( ConfigurationUtils.findIcon( "app", "v1", configDir ));

		File singleJpgFile = new File( descDir, "whatever.jpg" );
		Assert.assertTrue( singleJpgFile.createNewFile());
		Assert.assertEquals( singleJpgFile, ConfigurationUtils.findIcon( "app", "v1", configDir ));

		File defaultFile = new File( descDir, "application.sVg" );
		Assert.assertTrue( defaultFile.createNewFile());
		Assert.assertEquals( defaultFile, ConfigurationUtils.findIcon( "app", "v1", configDir ));

		Assert.assertNull( ConfigurationUtils.findIcon( "app", "", configDir ));
		Assert.assertNull( ConfigurationUtils.findIcon( "app", "v2", configDir ));
	}


	@Test
	public void testFindIcon_nullConfigDirectory() throws Exception {

		// In case we try to get an icon while the DM is reconfigured
		Assert.assertNull( ConfigurationUtils.findIcon( "app", "v1", null ));
	}


	@Test
	public void testLoadAndSaveApplicationBindings() throws Exception {

		File dir = this.folder.newFolder();
		Application app = new TestApplication();
		app.setDirectory( dir );

		Assert.assertEquals( 0, app.getApplicationBindings().size());
		ConfigurationUtils.loadApplicationBindings( app );
		Assert.assertEquals( 0, app.getApplicationBindings().size());

		app.bindWithApplication( "a1", "v11" );
		app.bindWithApplication( "a1", "v11" );
		app.bindWithApplication( "a1", "v12" );
		app.bindWithApplication( "a2", "v2" );
		ConfigurationUtils.saveApplicationBindings( app );

		app = new TestApplication();
		app.setDirectory( dir );

		ConfigurationUtils.loadApplicationBindings( app );
		Assert.assertEquals( 2, app.getApplicationBindings().size());
		Assert.assertEquals( 2, app.getApplicationBindings().get( "a1" ).size());
		Assert.assertEquals( 1, app.getApplicationBindings().get( "a2" ).size());

		Assert.assertTrue( app.getApplicationBindings().get( "a1" ).contains( "v11" ));
		Assert.assertTrue( app.getApplicationBindings().get( "a1" ).contains( "v12" ));
		Assert.assertTrue( app.getApplicationBindings().get( "a2" ).contains( "v2" ));
	}
}
