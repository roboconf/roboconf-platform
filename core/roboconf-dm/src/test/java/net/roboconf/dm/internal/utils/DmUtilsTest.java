/**
 * Copyright 2017 Linagora, Université Joseph Fourier, Floralis
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

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.api.IInstancesMngr;
import net.roboconf.dm.management.api.INotificationMngr;

/**
 * @author Vincent Zurczak - Linagora
 */
public class DmUtilsTest {

	@Test
	public void testDeletionOfInstances_noInstanceMarked() throws Exception {

		INotificationMngr notificationMngr = Mockito.mock( INotificationMngr.class );
		IInstancesMngr instanceMngr = Mockito.mock( IInstancesMngr.class );
		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app );

		app.getMySqlVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		DmUtils.markScopedInstanceAsNotDeployed( app.getMySqlVm(), ma, notificationMngr, instanceMngr );
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getMySqlVm().getStatus());
		Mockito.verifyZeroInteractions( instanceMngr );
	}


	@Test
	public void testDeletionOfInstances_withInstanceMarked() throws Exception {

		INotificationMngr notificationMngr = Mockito.mock( INotificationMngr.class );
		IInstancesMngr instanceMngr = Mockito.mock( IInstancesMngr.class );
		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app );

		app.getMySqlVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		app.getMySqlVm().data.put( Instance.DELETE_WHEN_NOT_DEPLOYED, "whatever" );

		DmUtils.markScopedInstanceAsNotDeployed( app.getMySqlVm(), ma, notificationMngr, instanceMngr );
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getMySqlVm().getStatus());
		Mockito.verify( instanceMngr ).removeInstance( ma, app.getMySqlVm(), true );
	}


	@Test
	public void testDeletionOfInstances_withInstanceMarked_andExceptions() throws Exception {

		INotificationMngr notificationMngr = Mockito.mock( INotificationMngr.class );
		IInstancesMngr instanceMngr = Mockito.mock( IInstancesMngr.class );

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app );

		Mockito.doThrow( new RuntimeException( "for test" )).when( instanceMngr ).removeInstance( ma, app.getMySqlVm(), true );

		app.getMySqlVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		app.getMySqlVm().data.put( Instance.DELETE_WHEN_NOT_DEPLOYED, "whatever" );

		DmUtils.markScopedInstanceAsNotDeployed( app.getMySqlVm(), ma, notificationMngr, instanceMngr );
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getMySqlVm().getStatus());
		Mockito.verify( instanceMngr ).removeInstance( ma, app.getMySqlVm(), true );
	}
}
