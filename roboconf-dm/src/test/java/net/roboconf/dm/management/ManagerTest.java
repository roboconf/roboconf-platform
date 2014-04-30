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
import net.roboconf.core.model.runtime.Application;
import net.roboconf.dm.management.exceptions.DmWasNotInitializedException;
import net.roboconf.messaging.client.MessageServerClientFactory;

import org.junit.Before;
import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ManagerTest {

	@Before
	public void resetManager() throws Exception {
		Manager.INSTANCE.getAppNameToManagedApplication().clear();
		Manager.INSTANCE.setMessagingClientFactory( new MessageServerClientFactory());
	}


	@Test( expected = DmWasNotInitializedException.class )
	public void testPerformWhenNotConnected() throws Exception {

		Application app = new Application( "app" );
		Manager.INSTANCE.getAppNameToManagedApplication().put( app.getName(), new ManagedApplication( app, null ));

		Assert.assertFalse( Manager.INSTANCE.isConnectedToTheMessagingServer());
		Manager.INSTANCE.perform( app.getName(), ApplicationAction.deploy.toString(), null, true );
	}


	@Test( expected = DmWasNotInitializedException.class )
	public void testDeleteApplicationWhenNotConnected() throws Exception {

		Application app = new Application( "app" );
		Manager.INSTANCE.getAppNameToManagedApplication().put( app.getName(), new ManagedApplication( app, null ));

		Assert.assertFalse( Manager.INSTANCE.isConnectedToTheMessagingServer());
		Manager.INSTANCE.deleteApplication( app.getName());
	}


	@Test( expected = DmWasNotInitializedException.class )
	public void testLoadNewApplicationWhenNotConnected() throws Exception {

		Assert.assertFalse( Manager.INSTANCE.isConnectedToTheMessagingServer());
		Manager.INSTANCE.loadNewApplication( new File( "whatever--we-should-check-the-connection-first" ));
	}


	@Test
	public void testCleanupAllWhenNotConnected() {

		Assert.assertFalse( Manager.INSTANCE.isConnectedToTheMessagingServer());
		Manager.INSTANCE.cleanUpAll();
	}


	@Test
	public void testCleanMessagingServerWhenNotConnected() {

		Assert.assertFalse( Manager.INSTANCE.isConnectedToTheMessagingServer());
		ManagedApplication ma = new ManagedApplication( new Application( "app" ), null );
		Manager.INSTANCE.cleanMessagingServer( ma );
	}


	@Test
	public void testCleanUpWhenNotConnected_notNull() {

		Assert.assertFalse( Manager.INSTANCE.isConnectedToTheMessagingServer());
		ManagedApplication ma = new ManagedApplication( new Application( "app" ), null );
		Manager.INSTANCE.cleanUp( ma );
	}


	@Test
	public void testCleanUpWhenNotConnected_null() {

		Assert.assertFalse( Manager.INSTANCE.isConnectedToTheMessagingServer());
		Manager.INSTANCE.cleanUp( null );
	}
}
