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

package net.roboconf.dm.utils;

import java.io.File;
import java.util.Arrays;

import junit.framework.Assert;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.core.model.runtime.Instance.InstanceStatus;
import net.roboconf.dm.internal.TestApplication;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.persistence.IDmStorage.DmStorageApplicationBean;
import net.roboconf.dm.persistence.IDmStorage.DmStorageBean;
import net.roboconf.dm.persistence.IDmStorage.DmStorageRootInstanceBean;

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class PersistenceUtilsTest {

	@Test
	public void testRetrieveManagerState() {

		// Current application
		TestApplication testApp = new TestApplication();
		File testDir = new File( "test-app" );
		ManagedApplication testMa = new ManagedApplication( testApp, testDir );

		// Old app
		TestApplication oldApp = new TestApplication();
		oldApp.setName( "old app" );
		for( Instance inst : InstanceHelpers.getAllInstances( oldApp ))
			inst.setStatus( InstanceStatus.DEPLOYED_STARTED );

		for( Instance inst : oldApp.getRootInstances()) {
			inst.getData().put( Instance.IP_ADDRESS, inst.getName() + "-host" );
			inst.getData().put( Instance.MACHINE_ID, inst.getName() + "-id" );
		}

		File oldDir = new File( "old app" );
		ManagedApplication oldMa = new ManagedApplication( oldApp, oldDir );

		// Generate a persistence bean
		DmStorageBean bean = PersistenceUtils.retrieveManagerState( Arrays.asList( oldMa ), testMa );

		// Check the result
		Assert.assertNotNull( bean );
		Assert.assertEquals( 2, bean.getApplications().size());
		DmStorageApplicationBean[] apps = bean.getApplications().toArray( new DmStorageApplicationBean[ 0 ]);

		// Check the old application
		DmStorageApplicationBean currentAppBean = apps[ 0 ];
		Assert.assertEquals( oldApp.getName(), currentAppBean.getApplicationName());
		Assert.assertEquals( oldDir.getAbsolutePath(), currentAppBean.getApplicationDirectoryPath());
		Assert.assertEquals( 2, currentAppBean.getRootInstances().size());

		for( DmStorageRootInstanceBean rootInstanceBean : currentAppBean.getRootInstances()) {
			Assert.assertEquals( rootInstanceBean.getRootInstanceName() + "-host", rootInstanceBean.getIpAddress());
			Assert.assertEquals( rootInstanceBean.getRootInstanceName() + "-id", rootInstanceBean.getMachineId());

			Instance inst = InstanceHelpers.findInstanceByPath( oldApp, "/" + rootInstanceBean.getRootInstanceName());
			Assert.assertNotNull( inst );
			Assert.assertEquals( inst.getComponent().getName(), rootInstanceBean.getComponentName());
			Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED.toString(), rootInstanceBean.getStatus());
		}

		// Check the new application
		currentAppBean = apps[ 1 ];
		Assert.assertEquals( testApp.getName(), currentAppBean.getApplicationName());
		Assert.assertEquals( testDir.getAbsolutePath(), currentAppBean.getApplicationDirectoryPath());
		Assert.assertEquals( 2, currentAppBean.getRootInstances().size());

		for( DmStorageRootInstanceBean rootInstanceBean : currentAppBean.getRootInstances())
			Assert.assertEquals( InstanceStatus.NOT_DEPLOYED.toString(), rootInstanceBean.getStatus());
	}
}
