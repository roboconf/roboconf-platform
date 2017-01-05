/**
 * Copyright 2014-2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.dm.internal.tasks;

import java.util.Map;

import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.dm.internal.api.IRandomMngr;
import net.roboconf.dm.internal.api.impl.ApplicationMngrImpl;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.api.IApplicationMngr;
import net.roboconf.dm.management.api.IAutonomicMngr;
import net.roboconf.dm.management.api.IConfigurationMngr;
import net.roboconf.dm.management.api.IMessagingMngr;
import net.roboconf.dm.management.api.INotificationMngr;
import net.roboconf.dm.management.api.ITargetsMngr;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * @author Vincent Zurczak - Linagora
 */
public class CheckerForStoredMessagesTaskTest {

	private IApplicationMngr appManager;
	private IMessagingMngr messagingMngr;


	@Before
	public void resetManager() throws Exception {

		// These tests only use the internal map of managed applications...
		// ... as well as the managed applications themselves.
		INotificationMngr notificationMngr = Mockito.mock( INotificationMngr.class );
		IConfigurationMngr configurationMngr = Mockito.mock( IConfigurationMngr.class );
		ITargetsMngr targetsMngr = Mockito.mock( ITargetsMngr.class );
		IRandomMngr randomMngr = Mockito.mock( IRandomMngr.class );
		IAutonomicMngr autonomicMngr = Mockito.mock( IAutonomicMngr.class );

		this.messagingMngr = Mockito.mock( IMessagingMngr.class );
		this.appManager = new ApplicationMngrImpl(
				notificationMngr, configurationMngr,
				targetsMngr, this.messagingMngr,
				randomMngr, autonomicMngr );
	}


	@Test
	public void testRun_noApplication() {

		CheckerForStoredMessagesTask task = new CheckerForStoredMessagesTask( this.appManager, this.messagingMngr );
		task.run();
		Mockito.verifyZeroInteractions( this.messagingMngr );
	}


	@Test
	@SuppressWarnings( "unchecked" )
	public void testRun_appWithAllStates() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app );
		TestUtils.getInternalField( this.appManager, "nameToManagedApplication", Map.class ).put( ma.getName(), ma );

		InstanceStatus[] statuses = new InstanceStatus[] {
				InstanceStatus.NOT_DEPLOYED,
				InstanceStatus.DEPLOYING,
				InstanceStatus.DEPLOYED_STARTED,
				InstanceStatus.DEPLOYED_STOPPED,
				InstanceStatus.PROBLEM
		};

		for( InstanceStatus status : statuses ) {
			Mockito.reset( this.messagingMngr );
			CheckerForStoredMessagesTask task = new CheckerForStoredMessagesTask( this.appManager, this.messagingMngr );
			app.getMySqlVm().setStatus( status );

			Mockito.verifyZeroInteractions( this.messagingMngr );
			task.run();
			Mockito.verify( this.messagingMngr, Mockito.times( 1 )).sendStoredMessages(
					Mockito.eq( ma ),
					Mockito.eq( app.getMySqlVm()));

			Mockito.verify( this.messagingMngr, Mockito.times( 1 )).sendStoredMessages(
					Mockito.eq( ma ),
					Mockito.eq( app.getTomcatVm()));
		}
	}
}
