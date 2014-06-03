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

import junit.framework.Assert;
import net.roboconf.core.actions.ApplicationAction;
import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.helpers.ComponentHelpers;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.runtime.Application;
import net.roboconf.core.model.runtime.Component;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.core.model.runtime.Instance.InstanceStatus;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.internal.TestApplication;
import net.roboconf.dm.internal.TestIaasResolver;
import net.roboconf.dm.internal.TestMessageServerClient.DmMessageServerClientFactory;
import net.roboconf.dm.management.exceptions.ImpossibleRestorationException;
import net.roboconf.dm.persistence.PropertiesFileStorage;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class RestorationTest {

	@Before
	public void resetManager() throws Exception {

		Manager.INSTANCE.cleanUpAll();
		Utils.deleteFilesRecursively( PropertiesFileStorage.STATE_FILE );
		Manager.INSTANCE.getAppNameToManagedApplication().clear();

		Manager.INSTANCE.setStorage( new PropertiesFileStorage());
		Manager.INSTANCE.setMessagingClientFactory( new DmMessageServerClientFactory());
		Manager.INSTANCE.setIaasResolver( new TestIaasResolver());
	}


	@After
	public void deleteStateFile() throws Exception {
		Utils.deleteFilesRecursively( PropertiesFileStorage.STATE_FILE );
	}


	@Test
	public void testRestorationWhenNoState() throws Exception {

		Assert.assertFalse( PropertiesFileStorage.STATE_FILE.exists());
		Assert.assertEquals( 0, Manager.INSTANCE.getAppNameToManagedApplication().size());
		Assert.assertNull( Manager.INSTANCE.messageServerIp );

		Manager.INSTANCE.restoreManagerState();
		Assert.assertEquals( 0, Manager.INSTANCE.getAppNameToManagedApplication().size());
		Assert.assertNull( Manager.INSTANCE.messageServerIp );
	}


	@Test
	public void testRestorationWhenDirectoryDoesNotExist() throws Exception {

		TestApplication app = new TestApplication();
		File dir = new File( "whatever" );
		Manager.INSTANCE.getAppNameToManagedApplication().put( app.getName(), new ManagedApplication( app, dir ));
		Manager.INSTANCE.saveManagerState();

		Manager.INSTANCE.cleanUpAll();
		Manager.INSTANCE.getAppNameToManagedApplication().clear();

		Manager.INSTANCE.restoreManagerState();
		Assert.assertEquals( 0, Manager.INSTANCE.getAppNameToManagedApplication().size());
	}


	@Test
	public void testBackupAndRestoration() throws Exception {

		Assert.assertEquals( 0, Manager.INSTANCE.getAppNameToManagedApplication().size());
		File directory = TestUtils.findTestFile( "/lamp" );
		ManagedApplication ma = Manager.INSTANCE.loadNewApplication( directory );
		Assert.assertEquals( 1, Manager.INSTANCE.getAppNameToManagedApplication().size());

		Manager.INSTANCE.cleanUpAll();
		Manager.INSTANCE.getAppNameToManagedApplication().clear();

		Assert.assertEquals( 0, Manager.INSTANCE.getAppNameToManagedApplication().size());
		Manager.INSTANCE.restoreManagerState();
		Assert.assertEquals( 1, Manager.INSTANCE.getAppNameToManagedApplication().size());

		Application loadedApp = Manager.INSTANCE.getAppNameToManagedApplication().values().iterator().next().getApplication();
		Assert.assertEquals( ma.getApplication(), loadedApp );
		Assert.assertEquals( directory, ma.getApplicationFilesDirectory());
		Assert.assertEquals( ma.getApplication().getRootInstances().size(), loadedApp.getRootInstances().size());
		Assert.assertEquals( InstanceHelpers.getAllInstances( ma.getApplication()).size(), InstanceHelpers.getAllInstances( loadedApp ).size());
	}


	@Test
	public void testBackupOnRootInstanceChange() throws Exception {

		Assert.assertEquals( 0, Manager.INSTANCE.getAppNameToManagedApplication().size());
		File directory = TestUtils.findTestFile( "/lamp" );
		ManagedApplication ma = Manager.INSTANCE.loadNewApplication( directory );
		Assert.assertEquals( 1, Manager.INSTANCE.getAppNameToManagedApplication().size());

		Manager.INSTANCE.perform( ma.getApplication().getName(), ApplicationAction.deploy.toString(), "/Apache VM", false );
		Manager.INSTANCE.perform( ma.getApplication().getName(), ApplicationAction.deploy.toString(), "/Tomcat VM 1", false );

		Manager.INSTANCE.cleanUpAll();
		Manager.INSTANCE.getAppNameToManagedApplication().clear();

		Assert.assertEquals( 0, Manager.INSTANCE.getAppNameToManagedApplication().size());
		Manager.INSTANCE.restoreManagerState();
		Assert.assertEquals( 1, Manager.INSTANCE.getAppNameToManagedApplication().size());

		Application loadedApp = Manager.INSTANCE.getAppNameToManagedApplication().values().iterator().next().getApplication();
		Assert.assertEquals( ma.getApplication(), loadedApp );
		Assert.assertEquals( directory, ma.getApplicationFilesDirectory());
		Assert.assertEquals( ma.getApplication().getRootInstances().size(), loadedApp.getRootInstances().size());
		Assert.assertEquals( InstanceHelpers.getAllInstances( ma.getApplication()).size(), InstanceHelpers.getAllInstances( loadedApp ).size());

		Instance apacheVm = InstanceHelpers.findInstanceByPath( loadedApp, "/Apache VM" );
		Assert.assertEquals( InstanceStatus.RESTORING, apacheVm.getStatus());

		Instance tomcatVm = InstanceHelpers.findInstanceByPath( loadedApp, "/Tomcat VM 1" );
		Assert.assertEquals( InstanceStatus.RESTORING, tomcatVm.getStatus());

		Instance mySqlVm = InstanceHelpers.findInstanceByPath( loadedApp, "/MySQL VM" );
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, mySqlVm.getStatus());
	}


	@Test( expected=ImpossibleRestorationException.class )
	public void testImpossibleRestoration() throws Exception {

		Assert.assertNull( Manager.INSTANCE.messageServerIp );
		Assert.assertTrue( Manager.INSTANCE.tryToChangeMessageServerIp( "localhost" ));
		Assert.assertEquals( "localhost", Manager.INSTANCE.messageServerIp );
		Assert.assertEquals( 0, Manager.INSTANCE.getAppNameToManagedApplication().size());

		Manager.INSTANCE.saveManagerState();
		Manager.INSTANCE.cleanUpAll();
		Manager.INSTANCE.getAppNameToManagedApplication().clear();
		Assert.assertNull( Manager.INSTANCE.messageServerIp );

		TestApplication app = new TestApplication();
		Manager.INSTANCE.getAppNameToManagedApplication().put( app.getName(), new ManagedApplication( app, new File( "whatever" )));

		Manager.INSTANCE.restoreManagerState();
	}


	@Test
	public void testRestorationWithNewInstance() throws Exception {

		Assert.assertEquals( 0, Manager.INSTANCE.getAppNameToManagedApplication().size());
		File directory = TestUtils.findTestFile( "/lamp" );
		ManagedApplication ma = Manager.INSTANCE.loadNewApplication( directory );

		Component tomcatComponent = ComponentHelpers.findComponent( ma.getApplication().getGraphs(), "VM" );
		Assert.assertNotNull( tomcatComponent );
		Instance newTomcatInstance = new Instance( "Tomcat VM 2" ).component( tomcatComponent ).status( InstanceStatus.DEPLOYING );

		Assert.assertEquals( 3, ma.getApplication().getRootInstances().size());
		Manager.INSTANCE.addInstance( ma.getApplication().getName(), null, newTomcatInstance );
		Assert.assertEquals( 4, ma.getApplication().getRootInstances().size());
		Assert.assertEquals( 1, Manager.INSTANCE.getAppNameToManagedApplication().size());

		Manager.INSTANCE.cleanUpAll();
		Manager.INSTANCE.getAppNameToManagedApplication().clear();

		Assert.assertEquals( 0, Manager.INSTANCE.getAppNameToManagedApplication().size());
		Manager.INSTANCE.restoreManagerState();
		Assert.assertEquals( 1, Manager.INSTANCE.getAppNameToManagedApplication().size());

		Application loadedApp = Manager.INSTANCE.getAppNameToManagedApplication().values().iterator().next().getApplication();
		Assert.assertEquals( ma.getApplication(), loadedApp );
		Assert.assertEquals( directory, ma.getApplicationFilesDirectory());
		Assert.assertEquals( ma.getApplication().getRootInstances().size(), loadedApp.getRootInstances().size());
		Assert.assertEquals( InstanceHelpers.getAllInstances( ma.getApplication()).size(), InstanceHelpers.getAllInstances( loadedApp ).size());

		Instance apacheVm = InstanceHelpers.findInstanceByPath( loadedApp, "/Apache VM" );
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, apacheVm.getStatus());

		Instance tomcatVm = InstanceHelpers.findInstanceByPath( loadedApp, "/Tomcat VM 1" );
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, tomcatVm.getStatus());

		Instance tomcatVm2 = InstanceHelpers.findInstanceByPath( loadedApp, "/Tomcat VM 2" );
		Assert.assertEquals( InstanceStatus.RESTORING, tomcatVm2.getStatus());

		Instance mySqlVm = InstanceHelpers.findInstanceByPath( loadedApp, "/MySQL VM" );
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, mySqlVm.getStatus());
	}
}
