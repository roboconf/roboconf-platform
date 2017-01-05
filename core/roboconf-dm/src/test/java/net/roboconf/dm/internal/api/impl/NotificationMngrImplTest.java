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

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import net.roboconf.dm.management.events.IDmListener;

/**
 * @author Vincent Zurczak - Linagora
 */
public class NotificationMngrImplTest {

	@Test
	public void testObvious() {

		NotificationMngrImpl mngr = new NotificationMngrImpl();
		mngr.enableNotifications();
		mngr.disableNotifications();

		Assert.assertNotNull( mngr.getId());
		Assert.assertNotNull( mngr.getDmListeners());
	}


	@Test
	public void testNotificationsEnablement() {

		// Basics about notifications
		IDmListener listener1 = Mockito.mock( IDmListener.class );
		IDmListener listener2 = Mockito.mock( IDmListener.class );

		// Notifications are not enabled by default
		NotificationMngrImpl mngr = new NotificationMngrImpl();
		mngr.addListener( listener1 );
		mngr.addListener( listener2 );
		Mockito.verify( listener1, Mockito.atLeast( 1 )).getId();
		Mockito.verify( listener2, Mockito.atLeast( 1 )).getId();
		Mockito.verifyNoMoreInteractions( listener1, listener2 );

		// When they are, existing listeners are enabled
		Mockito.reset( listener1, listener2 );
		mngr.enableNotifications();
		Mockito.verify( listener1, Mockito.only()).enableNotifications();
		Mockito.verify( listener2, Mockito.only()).enableNotifications();

		// When notifications are enabled, new listeners are automatically activated
		Mockito.reset( listener1, listener2 );
		IDmListener listener3 = Mockito.mock( IDmListener.class );
		mngr.addListener( listener3 );

		Mockito.verify( listener3, Mockito.atLeast( 1 )).getId();
		Mockito.verify( listener3, Mockito.times( 1 )).enableNotifications();
		Mockito.verifyNoMoreInteractions( listener3 );

		Mockito.verify( listener1, Mockito.atLeast( 1 )).getId();
		Mockito.verify( listener2, Mockito.atLeast( 1 )).getId();
		Mockito.verifyNoMoreInteractions( listener1, listener2 );

		// Disabling the notifications impacts the listeners
		Mockito.reset( listener1, listener2 );
		mngr.disableNotifications();
		Mockito.verify( listener1, Mockito.only()).disableNotifications();
		Mockito.verify( listener2, Mockito.only()).disableNotifications();
	}
}
