/**
 * Copyright 2015 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.dm.management;

import java.io.File;
import java.util.List;

import junit.framework.Assert;
import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.dm.internal.api.impl.TargetHandlerResolverImpl;
import net.roboconf.dm.internal.test.TargetHandlerMock;
import net.roboconf.dm.internal.test.TestManagerWrapper;
import net.roboconf.dm.internal.test.TestTargetResolver;
import net.roboconf.dm.management.api.ITargetHandlerResolver;
import net.roboconf.dm.management.events.IDmListener;
import net.roboconf.messaging.api.MessagingConstants;
import net.roboconf.target.api.TargetHandler;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ManagerTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();
	private Manager manager;


	@Before
	public void resetManager() throws Exception {
		this.manager = new Manager();
	}


	@Test
	@SuppressWarnings( "unchecked" )
	public void testExtensibilityNotifications_targets() throws Exception {

		ITargetHandlerResolver dthr = TestUtils.getInternalField( this.manager, "defaultTargetHandlerResolver", ITargetHandlerResolver.class );
		List<TargetHandler> handlers = TestUtils.getInternalField( dthr, "targetHandlers", List.class );
		handlers.clear();

		Assert.assertEquals( 0, handlers.size());
		this.manager.targetAppears( null );
		Assert.assertEquals( 0, handlers.size());
		this.manager.targetAppears( new TargetHandlerMock( "hey" ));
		Assert.assertEquals( 1, handlers.size());
		this.manager.targetDisappears( new TargetHandlerMock( "hey" ));
		Assert.assertEquals( 0, handlers.size());

		this.manager.targetDisappears( new TargetHandlerMock( "ho" ));
		Assert.assertEquals( 0, handlers.size());

		this.manager.targetDisappears( null );
		Assert.assertEquals( 0, handlers.size());

		this.manager.targetAppears( new TargetHandlerMock( "oops" ));
		Assert.assertEquals( 1, handlers.size());

		this.manager.targetAppears( new TargetHandlerMock( "new_oops" ));
		Assert.assertEquals( 2, handlers.size());
	}


	@Test
	@SuppressWarnings( "unchecked" )
	public void testExtensibilityNotifications_listeners() throws Exception {

		List<IDmListener> listeners = TestUtils.getInternalField( this.manager.notificationMngr(), "dmListeners", List.class );
		listeners.clear();

		Assert.assertEquals( 0, listeners.size());
		this.manager.listenerAppears( null );
		Assert.assertEquals( 0, listeners.size());

		IDmListener mock1 = Mockito.mock( IDmListener.class );
		Mockito.when( mock1.getId()).thenReturn( "mock1" );

		this.manager.listenerAppears( mock1 );
		Assert.assertEquals( 1, listeners.size());
		this.manager.listenerDisappears( mock1 );
		Assert.assertEquals( 0, listeners.size());

		this.manager.listenerDisappears( Mockito.mock( IDmListener.class ));
		Assert.assertEquals( 0, listeners.size());

		this.manager.listenerDisappears( null );
		Assert.assertEquals( 0, listeners.size());

		this.manager.listenerAppears( Mockito.mock( IDmListener.class ));
		Assert.assertEquals( 1, listeners.size());

		this.manager.listenerAppears( Mockito.mock( IDmListener.class ));
		Assert.assertEquals( 2, listeners.size());
	}


	@Test
	public void verifyApis() {

		Assert.assertNotNull( this.manager.applicationMngr());
		Assert.assertNotNull( this.manager.applicationTemplateMngr());
		Assert.assertNotNull( this.manager.configurationMngr());
		Assert.assertNotNull( this.manager.debugMngr());
		Assert.assertNotNull( this.manager.instancesMngr());
		Assert.assertNotNull( this.manager.messagingMngr());
		Assert.assertNotNull( this.manager.notificationMngr());
		Assert.assertNotNull( this.manager.targetsMngr());
	}


	@Test
	public void verifyDefaultTargerResolverIsInjected() throws Exception {

		ITargetHandlerResolver resolver = TestUtils.getInternalField( this.manager.instancesMngr(), "targetHandlerResolver", ITargetHandlerResolver.class );
		Assert.assertEquals( TargetHandlerResolverImpl.class, resolver.getClass());

		this.manager.setTargetResolver( new TestTargetResolver());
		resolver = TestUtils.getInternalField( this.manager.instancesMngr(), "targetHandlerResolver", ITargetHandlerResolver.class );
		Assert.assertEquals( TestTargetResolver.class, resolver.getClass());

		this.manager.setTargetResolver( null );
		resolver = TestUtils.getInternalField( this.manager.instancesMngr(), "targetHandlerResolver", ITargetHandlerResolver.class );
		Assert.assertEquals( TargetHandlerResolverImpl.class, resolver.getClass());
	}


	@Test
	public void testApplicationBindingsAreCorrectlySaved() throws Exception {

		this.manager.configurationMngr().setWorkingDirectory( this.folder.newFolder());
		this.manager.setMessagingType( MessagingConstants.TEST_FACTORY_TYPE );
		try {
			this.manager.start();
			TestManagerWrapper managerWrapper = new TestManagerWrapper( this.manager );
			managerWrapper.configureMessagingForTest();
			this.manager.reconfigure();

			File dir = TestUtils.findApplicationDirectory( "lamp" );
			Assert.assertTrue( dir.exists());

			ApplicationTemplate tpl = this.manager.applicationTemplateMngr().loadApplicationTemplate( dir );
			Assert.assertNotNull( tpl );

			ManagedApplication ma = this.manager.applicationMngr().createApplication( "test", null, tpl );
			Assert.assertNotNull( ma );

			// Create a binding between this application and itself.
			// It does not make sense, but this is for test.
			Assert.assertEquals( 0, ma.getApplication().applicationBindings.size());
			this.manager.applicationMngr().bindApplication( ma, tpl.getName(), ma.getName());
			Assert.assertEquals( 1, ma.getApplication().applicationBindings.size());

			// Bindings must have been saved.
			// Remove the application from the cache and restore it.
			managerWrapper.getNameToManagedApplication().remove( ma.getName());
			Assert.assertNull( this.manager.applicationMngr().findApplicationByName( ma.getName()));

			this.manager.applicationMngr().restoreApplications();
			ma = this.manager.applicationMngr().findManagedApplicationByName( ma.getName());
			Assert.assertNotNull( ma );

			Assert.assertEquals( 1, ma.getApplication().applicationBindings.size());
			Assert.assertEquals( ma.getName(), ma.getApplication().applicationBindings.get( tpl.getName()));

		} finally {
			this.manager.stop();
		}
	}


	@Test
	public void testApplicationInstancesAreCorrectlyRestored() throws Exception {

		// To prevent #454 from happening again
		this.manager.configurationMngr().setWorkingDirectory( this.folder.newFolder());
		this.manager.setMessagingType( MessagingConstants.TEST_FACTORY_TYPE );
		try {
			this.manager.start();
			TestManagerWrapper managerWrapper = new TestManagerWrapper( this.manager );
			managerWrapper.configureMessagingForTest();
			this.manager.reconfigure();

			File dir = TestUtils.findApplicationDirectory( "lamp" );
			Assert.assertTrue( dir.exists());

			ApplicationTemplate tpl = this.manager.applicationTemplateMngr().loadApplicationTemplate( dir );
			Assert.assertNotNull( tpl );

			ManagedApplication ma = this.manager.applicationMngr().createApplication( "test", null, tpl );
			Assert.assertNotNull( ma );

			// Clear the applications and restore them.
			// Even with no life cycle action, instances were saved and restored.
			Assert.assertEquals( 3, ma.getApplication().getRootInstances().size());
			managerWrapper.getNameToManagedApplication().clear();
			this.manager.applicationMngr().restoreApplications();

			ma = this.manager.applicationMngr().findManagedApplicationByName( "test" );
			Assert.assertNotNull( ma );
			Assert.assertEquals( 3, ma.getApplication().getRootInstances().size());

		} finally {
			this.manager.stop();
		}
	}
}
