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
import java.util.Map;

import junit.framework.Assert;
import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.io.RuntimeModelIo.InstancesLoadResult;
import net.roboconf.core.model.runtime.Application;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.core.model.runtime.Instance.InstanceStatus;
import net.roboconf.dm.internal.utils.ConfigurationUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

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
	public void testFindApplicationDirectories_inexistingDir() throws Exception {

		File apps = new File( this.dir, ConfigurationUtils.APPLICATIONS );
		Assert.assertTrue( apps.createNewFile());
		Assert.assertTrue( apps.exists());

		Assert.assertEquals( 0, ConfigurationUtils.findApplicationDirectories( this.dir ).size());
	}


	@Test
	public void testFindApplicationDirectories_twoDirs() throws Exception {

		File f1 = new File( this.dir, ConfigurationUtils.APPLICATIONS + "/app 1" );
		Assert.assertTrue( f1.mkdirs());
		File f2 = new File( this.dir, ConfigurationUtils.APPLICATIONS + "/app-2" );
		Assert.assertTrue( f2.mkdirs());

		Assert.assertEquals( 2, ConfigurationUtils.findApplicationDirectories( this.dir ).size());
		Assert.assertTrue( ConfigurationUtils.findApplicationDirectories( this.dir ).contains( f1 ));
		Assert.assertTrue( ConfigurationUtils.findApplicationDirectories( this.dir ).contains( f2 ));
	}


	@Test
	public void testRestoreInstances_empty() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app, ConfigurationUtils.findApplicationdirectory( app.getName(), this.dir ));
		InstancesLoadResult ilr = ConfigurationUtils.restoreInstances( ma, this.dir );
		Assert.assertEquals( 0, ilr.getLoadErrors().size());
		Assert.assertEquals( 0, ilr.getRootInstances().size());
	}


	@Test
	public void testSaveAndRestoreInstances() throws Exception {

		// Save...
		TestApplication app = new TestApplication();
		app.getMySqlVm().status( InstanceStatus.DEPLOYED_STARTED );
		app.getMySqlVm().getData().put( Instance.IP_ADDRESS, "192.168.1.12" );
		app.getMySqlVm().getData().put( Instance.MACHINE_ID, "xx45s5s645" );
		app.getMySql().status( InstanceStatus.DEPLOYED_STOPPED );

		app.getMySqlVm().getData().put( Instance.APPLICATION_NAME, app.getName());
		app.getTomcatVm().getData().put( Instance.APPLICATION_NAME, app.getName());

		ManagedApplication ma = new ManagedApplication( app, ConfigurationUtils.findApplicationdirectory( app.getName(), this.dir ));
		ConfigurationUtils.saveInstances( ma, this.dir );

		// ... and restore...
		InstancesLoadResult ilr = ConfigurationUtils.restoreInstances( ma, this.dir );
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

		File f = new File( this.dir, ConfigurationUtils.INSTANCES + "/some-app.instances" );

		// Try to delete an inexisting file => OK
		ConfigurationUtils.deleteInstancesFile( "inexisting-app", this.dir );

		// Delete an existing file
		Assert.assertTrue( f.getParentFile().mkdir());
		Assert.assertTrue( f.createNewFile());
		Assert.assertTrue( f.exists());
		ConfigurationUtils.deleteInstancesFile( "some-app", this.dir );
		Assert.assertFalse( f.exists());

		// Try to delete the file without success (here, a directory with a file inside).
		// No exception must be thrown.
		Assert.assertTrue( f.mkdir());
		Assert.assertTrue( new File( f, "whatever" ).createNewFile());
		Assert.assertTrue( f.exists());
		ConfigurationUtils.deleteInstancesFile( "some-app", this.dir );
		Assert.assertTrue( f.exists());
	}
}