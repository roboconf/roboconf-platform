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

package net.roboconf.dm.persistence;

import junit.framework.Assert;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.persistence.IDmStorage.DmStorageApplicationBean;
import net.roboconf.dm.persistence.IDmStorage.DmStorageBean;
import net.roboconf.dm.persistence.IDmStorage.DmStorageRootInstanceBean;

import org.junit.Before;
import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class PropertiesFileStorageTest {

	@Before
	public void deleteStateFile() throws Exception {
		Utils.deleteFilesRecursively( PropertiesFileStorage.STATE_FILE );
	}


	@Test
	public void testRestorationAndPersistence_full() throws Exception {

		// Create the bean
		DmStorageBean managerState = new DmStorageBean();
		managerState.messagingServerIp( "192.168.1.19" );

		DmStorageApplicationBean app1 = new DmStorageApplicationBean();
		app1.applicationDirectoryPath( "/whatever" ).applicationName( "app 1" );
		app1.getRootInstances().add( new DmStorageRootInstanceBean().componentName( "mysql" ).ipAddress( "192.168.1.2" ).machineId( "machine-1" ).rootInstanceName( "mysql" ));
		app1.getRootInstances().add( new DmStorageRootInstanceBean().componentName( "tomcat" ).ipAddress( "192.168.1.96" ).machineId( "machine-2" ).rootInstanceName( "tomcat 1" ));
		app1.getRootInstances().add( new DmStorageRootInstanceBean().componentName( "tomcat" ).ipAddress( "192.168.1.94" ).rootInstanceName( "tomcat 2" ));
		app1.getRootInstances().add( new DmStorageRootInstanceBean().componentName( "tomcat" ).machineId( "machine-4" ).rootInstanceName( "tomcat 3" ));

		DmStorageApplicationBean app2 = new DmStorageApplicationBean();
		app2.applicationDirectoryPath( "/whatever/2" ).applicationName( "app 2" );

		DmStorageApplicationBean app3 = new DmStorageApplicationBean();
		app3.applicationDirectoryPath( "/whatever/oops" ).applicationName( "app 3" );
		app3.getRootInstances().add( new DmStorageRootInstanceBean().componentName( "vm mongo" ).ipAddress( "192.168.1.12" ).machineId( "machine 1" ).rootInstanceName( "vm mongo 1" ));
		app3.getRootInstances().add( new DmStorageRootInstanceBean().componentName( "vm redis" ).ipAddress( "192.168.1.15" ).machineId( "machine 2" ).rootInstanceName( "vm Redis" ));

		managerState.getApplications().add( app1 );
		managerState.getApplications().add( app2 );
		managerState.getApplications().add( app3 );

		// Store and restore
		Assert.assertFalse( PropertiesFileStorage.STATE_FILE.exists());

		IDmStorage storage = new PropertiesFileStorage();
		storage.saveManagerState( managerState );
		DmStorageBean restoredBean = storage.restoreManagerState();

		Assert.assertTrue( PropertiesFileStorage.STATE_FILE.exists());

		// Compare
		Assert.assertEquals( managerState.getMessagingServerIp(), restoredBean.getMessagingServerIp());
		Assert.assertEquals( managerState.getApplications().size(), restoredBean.getApplications().size());

		DmStorageApplicationBean[] apps = managerState.getApplications().toArray( new DmStorageApplicationBean[ 0 ]);
		Assert.assertEquals( app1.getApplicationName(), apps[ 0 ].getApplicationName());
		Assert.assertEquals( app1.getApplicationDirectoryPath(), apps[ 0 ].getApplicationDirectoryPath());
		Assert.assertEquals( app1.getRootInstances().size(), apps[ 0 ].getRootInstances().size());

		for( int i=0; i<app1.getRootInstances().size(); i++ ) {
			Assert.assertEquals( app1.getRootInstances().get( i ).getComponentName(), apps[ 0 ].getRootInstances().get( i ).getComponentName());
			Assert.assertEquals( app1.getRootInstances().get( i ).getIpAddress(), apps[ 0 ].getRootInstances().get( i ).getIpAddress());
			Assert.assertEquals( app1.getRootInstances().get( i ).getMachineId(), apps[ 0 ].getRootInstances().get( i ).getMachineId());
			Assert.assertEquals( app1.getRootInstances().get( i ).getRootInstanceName(), apps[ 0 ].getRootInstances().get( i ).getRootInstanceName());
		}

		Assert.assertEquals( app2.getApplicationName(), apps[ 1 ].getApplicationName());
		Assert.assertEquals( app2.getApplicationDirectoryPath(), apps[ 1 ].getApplicationDirectoryPath());
		Assert.assertEquals( 0, apps[ 1 ].getRootInstances().size());

		Assert.assertEquals( app3.getApplicationName(), apps[ 2 ].getApplicationName());
		Assert.assertEquals( app3.getApplicationDirectoryPath(), apps[ 2 ].getApplicationDirectoryPath());
		Assert.assertEquals( app3.getRootInstances().size(), apps[ 2 ].getRootInstances().size());

		for( int i=0; i<app3.getRootInstances().size(); i++ ) {
			Assert.assertEquals( app3.getRootInstances().get( i ).getComponentName(), apps[ 2 ].getRootInstances().get( i ).getComponentName());
			Assert.assertEquals( app3.getRootInstances().get( i ).getIpAddress(), apps[ 2 ].getRootInstances().get( i ).getIpAddress());
			Assert.assertEquals( app3.getRootInstances().get( i ).getMachineId(), apps[ 2 ].getRootInstances().get( i ).getMachineId());
			Assert.assertEquals( app3.getRootInstances().get( i ).getRootInstanceName(), apps[ 2 ].getRootInstances().get( i ).getRootInstanceName());
		}
	}


	@Test
	public void testRestorationAndPersistence_minimalist() throws Exception {

		Assert.assertFalse( PropertiesFileStorage.STATE_FILE.exists());
		DmStorageBean managerState = new DmStorageBean();
		IDmStorage storage = new PropertiesFileStorage();
		storage.saveManagerState( managerState );

		Assert.assertTrue( PropertiesFileStorage.STATE_FILE.exists());
		DmStorageBean restoredBean = storage.restoreManagerState();
		Assert.assertNotNull( restoredBean );
		Assert.assertNull( restoredBean.getMessagingServerIp());
		Assert.assertEquals( 0, restoredBean.getApplications().size());
	}


	@Test
	public void testRestorationAndPersistence_nonExistingFile() throws Exception {

		Assert.assertFalse( PropertiesFileStorage.STATE_FILE.exists());
		IDmStorage storage = new PropertiesFileStorage();

		DmStorageBean restoredBean = storage.restoreManagerState();
		Assert.assertNotNull( restoredBean );
		Assert.assertNull( restoredBean.getMessagingServerIp());
		Assert.assertEquals( 0, restoredBean.getApplications().size());
	}
}
