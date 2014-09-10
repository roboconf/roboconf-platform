/**
 * Copyright 2014 Linagora, Université Joseph Fourier
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

package net.roboconf.dm.management;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import junit.framework.Assert;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.io.RuntimeModelIo.InstancesLoadResult;
import net.roboconf.core.model.runtime.Application;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.core.model.runtime.Instance.InstanceStatus;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.internal.TestApplication;
import net.roboconf.dm.management.ManagerConfiguration.EnvResolver;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ManagerConfigurationTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();


	@Test
	public void testCreateConfiguration_default() throws Exception {

		File dir = this.folder.newFolder();
		ManagerConfiguration conf = ManagerConfiguration.createConfiguration( dir );
		Assert.assertEquals( "localhost", conf.getMessageServerIp());
		Assert.assertEquals( "guest", conf.getMessageServerPassword());
		Assert.assertEquals( "guest", conf.getMessageServerUsername());
		Assert.assertEquals( dir, conf.getConfigurationDirectory());
		Assert.assertEquals( 0, conf.findApplicationDirectories().size());

		File appdir = conf.findApplicationdirectory( "app" );
		Assert.assertEquals( "app", appdir.getName());
		Assert.assertTrue( Utils.isAncestorFile( dir, appdir ));
		Assert.assertTrue( new File( dir, ManagerConfiguration.APPLICATIONS ).exists());
		Assert.assertTrue( new File( dir, ManagerConfiguration.INSTANCES ).exists());
		Assert.assertTrue( new File( dir, ManagerConfiguration.CONF ).exists());
		Assert.assertTrue( new File( dir, ManagerConfiguration.CONF + "/" + ManagerConfiguration.CONF_PROPERTIES ).exists());
	}


	@Test
	public void testCreateConfiguration_given() throws Exception {

		File dir = this.folder.newFolder();
		ManagerConfiguration conf = ManagerConfiguration.createConfiguration( dir, "192.168.1.40", "oasis", "wonderwall" );
		Assert.assertEquals( "192.168.1.40", conf.getMessageServerIp());
		Assert.assertEquals( "wonderwall", conf.getMessageServerPassword());
		Assert.assertEquals( "oasis", conf.getMessageServerUsername());
		Assert.assertEquals( dir, conf.getConfigurationDirectory());
		Assert.assertEquals( 0, conf.findApplicationDirectories().size());

		File appdir = conf.findApplicationdirectory( "app 50" );
		Assert.assertEquals( "app 50", appdir.getName());
		Assert.assertTrue( Utils.isAncestorFile( dir, appdir ));
	}


	@Test
	public void testFindConfigurationDirectory_default() {

		EnvResolver envResolver = new EnvResolver() {
			@Override
			String findEnvironmentVariable( String name ) {
				return null;
			}
		};

		File defaultFile = new File( System.getProperty( "user.home" ), "roboconf_dm" );
		Assert.assertEquals( defaultFile, ManagerConfiguration.findConfigurationDirectory( envResolver ));
	}


	@Test
	public void testLoadConfiguration() throws Exception {

		File dir = this.folder.newFolder();
		ManagerConfiguration conf = ManagerConfiguration.createConfiguration( dir );
		conf = ManagerConfiguration.loadConfiguration( dir );

		Assert.assertEquals( "localhost", conf.getMessageServerIp());
		Assert.assertEquals( dir, conf.getConfigurationDirectory());
	}


	@Test( expected = IOException.class )
	public void testCreateConfiguration_withError() throws Exception {

		File file = this.folder.newFile();
		ManagerConfiguration.createConfiguration( file );
	}


	@Test( expected = IOException.class )
	public void testLoadConfiguration_withError() throws Exception {

		File file = this.folder.newFile();
		ManagerConfiguration.loadConfiguration( file );
	}


	@Test
	public void testFindApplicationDirectories_inexistingDir() throws Exception {

		File dir = this.folder.newFolder();
		ManagerConfiguration conf = ManagerConfiguration.createConfiguration( dir );

		File apps = new File( dir, ManagerConfiguration.APPLICATIONS );
		Assert.assertTrue( apps.exists());
		Assert.assertTrue( apps.delete());
		Assert.assertFalse( apps.exists());

		Assert.assertTrue( apps.createNewFile());
		Assert.assertTrue( apps.exists());

		Assert.assertEquals( 0, conf.findApplicationDirectories().size());
	}


	@Test
	public void testFindApplicationDirectories_twoDirs() throws Exception {

		File dir = this.folder.newFolder();
		File f1 = new File( dir, ManagerConfiguration.APPLICATIONS + "/app 1" );
		Assert.assertTrue( f1.mkdirs());
		File f2 = new File( dir, ManagerConfiguration.APPLICATIONS + "/app-2" );
		Assert.assertTrue( f2.mkdirs());

		ManagerConfiguration conf = ManagerConfiguration.createConfiguration( dir );
		Assert.assertEquals( 2, conf.findApplicationDirectories().size());
		Assert.assertTrue( conf.findApplicationDirectories().contains( f1 ));
		Assert.assertTrue( conf.findApplicationDirectories().contains( f2 ));
	}


	@Test
	public void testFindConfigurationDirectory_env() {

		final File envFile = new File( System.getProperty( "java.io.tmpdir" ), "somewhere/on/the/disk" );
		EnvResolver envResolver = new EnvResolver() {
			@Override
			String findEnvironmentVariable( String name ) {
				return envFile.getAbsolutePath();
			}
		};

		Assert.assertEquals( envFile, ManagerConfiguration.findConfigurationDirectory( envResolver ));
	}


	@Test
	public void testRestoreInstances_empty() throws Exception {

		File dir = this.folder.newFolder();
		ManagerConfiguration conf = ManagerConfiguration.createConfiguration( dir );
		TestApplication app = new TestApplication();

		ManagedApplication ma = new ManagedApplication( app, conf.findApplicationdirectory( app.getName()));
		InstancesLoadResult ilr = conf.restoreInstances( ma );
		Assert.assertEquals( 0, ilr.getLoadErrors().size());
		Assert.assertEquals( 0, ilr.getRootInstances().size());
	}


	@Test
	public void testEnvResolver() {

		String javaHome = new EnvResolver().findEnvironmentVariable( "JAVA_HOME" );
		Assert.assertNotNull( javaHome );
	}


	@Test
	public void testSaveAndRestoreInstances() throws Exception {

		// Save...
		File dir = this.folder.newFolder();
		ManagerConfiguration conf = ManagerConfiguration.createConfiguration( dir );
		TestApplication app = new TestApplication();
		app.getMySqlVm().status( InstanceStatus.DEPLOYED_STARTED );
		app.getMySqlVm().getData().put( Instance.IP_ADDRESS, "192.168.1.12" );
		app.getMySqlVm().getData().put( Instance.MACHINE_ID, "xx45s5s645" );
		app.getMySql().status( InstanceStatus.DEPLOYED_STOPPED );
		
		app.getMySqlVm().getData().put( Instance.APPLICATION_NAME, app.getName());
		app.getTomcatVm().getData().put( Instance.APPLICATION_NAME, app.getName());

		ManagedApplication ma = new ManagedApplication( app, conf.findApplicationdirectory( app.getName()));
		conf.saveInstances( ma );

		// ... and restore...
		InstancesLoadResult ilr = conf.restoreInstances( ma );
		Assert.assertEquals( 0, ilr.getLoadErrors().size());

		Application restoredApp = new Application();
		restoredApp.getRootInstances().addAll( ilr.getRootInstances());

		// ... before comparing...
		Assert.assertEquals( app.getRootInstances().size(), restoredApp.getRootInstances().size());
		Assert.assertEquals( InstanceHelpers.getAllInstances( app ).size(), InstanceHelpers.getAllInstances( restoredApp ).size());
		for( Instance restoredInst : InstanceHelpers.getAllInstances( restoredApp )) {

			Instance inst = InstanceHelpers.findInstanceByPath( app, InstanceHelpers.computeInstancePath( restoredInst ));
			Assert.assertNotNull( inst );
			Assert.assertEquals( inst.getName(), restoredInst.getName());
			Assert.assertEquals( inst.getChannel(), restoredInst.getChannel());
			Assert.assertEquals( inst.getStatus(), restoredInst.getStatus());
			Assert.assertEquals( inst.getComponent(), restoredInst.getComponent());
			Assert.assertEquals( inst.getOverriddenExports().size(), restoredInst.getOverriddenExports().size());
			Assert.assertEquals( inst.getData().size(), restoredInst.getData().size());

			for( Map.Entry<String,String> entry : restoredInst.getData().entrySet()) {
				Assert.assertTrue( inst.getData().containsKey( entry.getKey()));
				Assert.assertEquals( inst.getData().get( entry.getKey()), entry.getValue());
			}
		}
	}


	@Test
	public void testDeleteInstancesFile() throws Exception {

		File dir = this.folder.newFolder();
		ManagerConfiguration conf = ManagerConfiguration.createConfiguration( dir );
		conf.deleteInstancesFile( "inexisting-app" );

		File f = new File( dir, ManagerConfiguration.INSTANCES + "/some-app.instances" );
		Assert.assertTrue( f.createNewFile());
		Assert.assertTrue( f.exists());

		conf.deleteInstancesFile( "some-app" );
		Assert.assertFalse( f.exists());
	}
}
