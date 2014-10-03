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
import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.io.RuntimeModelIo.InstancesLoadResult;
import net.roboconf.core.model.runtime.Application;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.core.model.runtime.Instance.InstanceStatus;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.internal.management.ManagedApplication;
import net.roboconf.dm.internal.test.TestMessageServerClient;
import net.roboconf.dm.internal.test.TestMessageServerClient.DmMessageServerClientFactory;
import net.roboconf.messaging.client.IDmClient;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ManagerConfigurationTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();
	private ManagerConfiguration conf;
	private File dir;


	@Before
	public void resetConfiguration() throws Exception {

		this.dir = this.folder.newFolder();
		this.conf = new ManagerConfiguration( this.dir );
		this.conf.setMessgingFactory( new DmMessageServerClientFactory());
	}


	@Test
	public void testUpdate_default() throws Exception {

		Assert.assertFalse( this.conf.isValidConfiguration());
		this.conf.update();
		Assert.assertTrue( this.conf.isValidConfiguration());

		Assert.assertEquals( "localhost", this.conf.getMessageServerIp());
		Assert.assertEquals( "guest", this.conf.getMessageServerPassword());
		Assert.assertEquals( "guest", this.conf.getMessageServerUsername());
		Assert.assertEquals( this.dir, this.conf.getConfigurationDirectory());
		Assert.assertEquals( 0, this.conf.findApplicationDirectories().size());

		File appdir = this.conf.findApplicationdirectory( "app" );
		Assert.assertEquals( "app", appdir.getName());
		Assert.assertTrue( Utils.isAncestorFile( this.dir, appdir ));
		Assert.assertTrue( new File( this.dir, ManagerConfiguration.APPLICATIONS ).exists());
		Assert.assertTrue( new File( this.dir, ManagerConfiguration.INSTANCES ).exists());
	}


	@Test
	public void testUpdate_given() throws Exception {

		this.conf = new ManagerConfiguration( "192.168.1.40", "oasis", "wonderwall", this.dir );
		this.conf.setMessgingFactory( new DmMessageServerClientFactory());

		Assert.assertFalse( this.conf.isValidConfiguration());
		this.conf.update();
		Assert.assertTrue( this.conf.isValidConfiguration());

		Assert.assertEquals( "192.168.1.40", this.conf.getMessageServerIp());
		Assert.assertEquals( "wonderwall", this.conf.getMessageServerPassword());
		Assert.assertEquals( "oasis", this.conf.getMessageServerUsername());
		Assert.assertEquals( this.dir, this.conf.getConfigurationDirectory());
		Assert.assertEquals( 0, this.conf.findApplicationDirectories().size());

		File appdir = this.conf.findApplicationdirectory( "app 50" );
		Assert.assertEquals( "app 50", appdir.getName());
		Assert.assertTrue( Utils.isAncestorFile( this.dir, appdir ));
	}


	@Test
	public void testUpdate_withError_fileInsteadOfDirectory() throws Exception {

		// Invalid root directory
		Assert.assertFalse( this.conf.isValidConfiguration());
		this.conf.setConfigurationDirectoryLocation( this.folder.newFile().getAbsolutePath());
		this.conf.update();
		Assert.assertFalse( this.conf.isValidConfiguration());

		this.conf.update();
		Assert.assertFalse( this.conf.isValidConfiguration());

		// Invalid children directories - applications
		File dir = this.folder.newFolder();
		this.conf.setConfigurationDirectoryLocation( dir.getAbsolutePath());
		Assert.assertTrue( new File( dir, ManagerConfiguration.APPLICATIONS ).createNewFile());

		this.conf.update();
		Assert.assertFalse( this.conf.isValidConfiguration());

		// Invalid children directories - instances
		dir = this.folder.newFolder();
		this.conf.setConfigurationDirectoryLocation( dir.getAbsolutePath());
		Assert.assertTrue( new File( dir, ManagerConfiguration.INSTANCES ).createNewFile());

		this.conf.update();
		Assert.assertFalse( this.conf.isValidConfiguration());

		// Let's try a valid one
		this.conf.setConfigurationDirectoryLocation( new File( dir, "inexisting-directory" ).getAbsolutePath());
		this.conf.update();
		Assert.assertTrue( this.conf.isValidConfiguration());
	}


	@Test
	public void testFindApplicationDirectories_inexistingDir() throws Exception {

		this.conf.update();

		File apps = new File( this.dir, ManagerConfiguration.APPLICATIONS );
		Assert.assertTrue( apps.exists());
		Assert.assertTrue( apps.delete());
		Assert.assertFalse( apps.exists());

		Assert.assertTrue( apps.createNewFile());
		Assert.assertTrue( apps.exists());

		Assert.assertEquals( 0, this.conf.findApplicationDirectories().size());
	}


	@Test
	public void testFindApplicationDirectories_twoDirs() throws Exception {

		File f1 = new File( this.dir, ManagerConfiguration.APPLICATIONS + "/app 1" );
		Assert.assertTrue( f1.mkdirs());
		File f2 = new File( this.dir, ManagerConfiguration.APPLICATIONS + "/app-2" );
		Assert.assertTrue( f2.mkdirs());

		this.conf.update();

		Assert.assertEquals( 2, this.conf.findApplicationDirectories().size());
		Assert.assertTrue( this.conf.findApplicationDirectories().contains( f1 ));
		Assert.assertTrue( this.conf.findApplicationDirectories().contains( f2 ));
	}


	@Test
	public void testRestoreInstances_empty() throws Exception {

		this.conf.update();

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app, this.conf.findApplicationdirectory( app.getName()));
		InstancesLoadResult ilr = this.conf.restoreInstances( ma );
		Assert.assertEquals( 0, ilr.getLoadErrors().size());
		Assert.assertEquals( 0, ilr.getRootInstances().size());
	}


	@Test
	public void testSaveAndRestoreInstances() throws Exception {

		// Save...
		this.conf.update();

		TestApplication app = new TestApplication();
		app.getMySqlVm().status( InstanceStatus.DEPLOYED_STARTED );
		app.getMySqlVm().getData().put( Instance.IP_ADDRESS, "192.168.1.12" );
		app.getMySqlVm().getData().put( Instance.MACHINE_ID, "xx45s5s645" );
		app.getMySql().status( InstanceStatus.DEPLOYED_STOPPED );

		app.getMySqlVm().getData().put( Instance.APPLICATION_NAME, app.getName());
		app.getTomcatVm().getData().put( Instance.APPLICATION_NAME, app.getName());

		ManagedApplication ma = new ManagedApplication( app, this.conf.findApplicationdirectory( app.getName()));
		this.conf.saveInstances( ma );

		// ... and restore...
		InstancesLoadResult ilr = this.conf.restoreInstances( ma );
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

		this.conf.update();
		File f = new File( this.dir, ManagerConfiguration.INSTANCES + "/some-app.instances" );

		// Try to delete an inexisting file => OK
		this.conf.deleteInstancesFile( "inexisting-app" );

		// Delete an existing file
		Assert.assertTrue( f.createNewFile());
		Assert.assertTrue( f.exists());
		this.conf.deleteInstancesFile( "some-app" );
		Assert.assertFalse( f.exists());

		// Try to delete the file without success (here, a directory with a file inside).
		// No exception must be thrown.
		Assert.assertTrue( f.mkdir());
		Assert.assertTrue( new File( f, "whatever" ).createNewFile());
		Assert.assertTrue( f.exists());
		this.conf.deleteInstancesFile( "some-app" );
		Assert.assertTrue( f.exists());
	}


	@Test
	public void testCloseConnection() {

		Assert.assertNull( this.conf.messagingClient );
		this.conf.closeConnection();

		Assert.assertNull( this.conf.messagingClient );
		Manager manager = new Manager();
		manager.setConfiguration( this.conf );
		this.conf.setManager( manager );
		this.conf.update();

		Assert.assertTrue( this.conf.messagingClient.isConnected());
		this.conf.closeConnection();
		Assert.assertFalse( this.conf.messagingClient.isConnected());

		this.conf.update();
		Assert.assertTrue( this.conf.messagingClient.isConnected());
	}


	@Test
	public void testCloseConnection_withException() {

		this.conf.setMessgingFactory( new DmMessageServerClientFactory() {
			@Override
			public IDmClient createDmClient() {
				return new TestMessageServerClient() {
					@Override
					public void closeConnection() throws IOException {
						throw new IOException( "For test purpose." );
					}

					@Override
					public boolean isConnected() {
						return true;
					}
				};
			}
		});

		this.conf.update();
		Assert.assertNotNull( this.conf.messagingClient );
		this.conf.closeConnection();
	}
}
