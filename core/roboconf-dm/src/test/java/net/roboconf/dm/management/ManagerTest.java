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

package net.roboconf.dm.management;

import java.io.File;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.dm.internal.api.impl.PreferencesMngrImpl;
import net.roboconf.dm.internal.api.impl.TargetHandlerResolverImpl;
import net.roboconf.dm.internal.test.TargetHandlerMock;
import net.roboconf.dm.internal.test.TestManagerWrapper;
import net.roboconf.dm.internal.test.TestTargetResolver;
import net.roboconf.dm.management.api.IInstancesMngr;
import net.roboconf.dm.management.api.IPreferencesMngr;
import net.roboconf.dm.management.api.ITargetHandlerResolver;
import net.roboconf.dm.management.events.IDmListener;
import net.roboconf.messaging.api.MessagingConstants;
import net.roboconf.messaging.api.factory.IMessagingClientFactory;
import net.roboconf.messaging.api.factory.MessagingClientFactoryRegistry;
import net.roboconf.messaging.api.reconfigurables.ReconfigurableClientDm;
import net.roboconf.target.api.TargetException;
import net.roboconf.target.api.TargetHandler;

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
		Assert.assertNotNull( this.manager.commandsMngr());

		// The preferences are injected by iPojo
		IPreferencesMngr impl1 = this.manager.preferencesMngr();
		Assert.assertNotNull( impl1 );

		IPreferencesMngr impl2 = new PreferencesMngrImpl();
		this.manager.setPreferencesMngr( impl2 );
		Assert.assertNotNull( this.manager.preferencesMngr());

		Assert.assertSame( impl2, this.manager.preferencesMngr());
		Assert.assertNotSame( impl1, this.manager.preferencesMngr());

		// Only public API are exported by the manager
		for( Method m : Manager.class.getMethods()) {
			if( ! m.getName().startsWith( "get" ))
				continue;

			// Ignore primitive types
			if( m.getReturnType().isPrimitive())
				continue;

			String packageName = m.getReturnType().getPackage().getName();
			Assert.assertFalse( m.getReturnType().getName() + " is internal!", packageName.toLowerCase().contains( "internal" ));
		}
	}


	@Test
	public void verifyAddAndRemoveMessaging_nonOsgiEnvironment() throws Exception {

		// Initial checks
		ReconfigurableClientDm mc;
		try {
			this.manager.start();
			mc = TestUtils.getInternalField( this.manager, "messagingClient", ReconfigurableClientDm.class );
			Assert.assertNotNull( mc );
			Assert.assertNull( mc.getRegistry());

		} finally {
			this.manager.stop();
		}

		// After "stop"...
		mc = TestUtils.getInternalField( this.manager, "messagingClient", ReconfigurableClientDm.class );
		Assert.assertNotNull( mc );
		Assert.assertNull( mc.getRegistry());

		// No error if we try to remove a factory that does not exist
		IMessagingClientFactory f1 = Mockito.mock( IMessagingClientFactory.class );
		Mockito.when( f1.getType()).thenReturn( "f1" );

		this.manager.removeMessagingFactory( f1 );
		Assert.assertNull( mc.getRegistry());

		// Set new messaging factories
		this.manager.addMessagingFactory( f1 );

		MessagingClientFactoryRegistry registry = mc.getRegistry();
		Assert.assertNotNull( registry );
		Assert.assertSame( f1, registry.getMessagingClientFactory( "f1" ));

		// Add another one
		IMessagingClientFactory f2 = Mockito.mock( IMessagingClientFactory.class );
		Mockito.when( f2.getType()).thenReturn( "f2" );
		this.manager.addMessagingFactory( f2 );

		Assert.assertSame( registry, mc.getRegistry());
		Assert.assertSame( f2, registry.getMessagingClientFactory( "f2" ));
		Assert.assertSame( f1, registry.getMessagingClientFactory( "f1" ));

		// Remove one
		this.manager.removeMessagingFactory( f2 );
		Assert.assertSame( f1, registry.getMessagingClientFactory( "f1" ));
		Assert.assertNull( registry.getMessagingClientFactory( "f2" ));

		// Remove the other one
		this.manager.removeMessagingFactory( f2 );
		this.manager.removeMessagingFactory( f1 );
		Assert.assertNull( registry.getMessagingClientFactory( "f1" ));
		Assert.assertNull( registry.getMessagingClientFactory( "f2" ));
		Assert.assertNotNull( mc.getRegistry());
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
		this.manager.setMessagingType( MessagingConstants.FACTORY_TEST );
		try {
			this.manager.start();
			TestManagerWrapper managerWrapper = new TestManagerWrapper( this.manager );
			managerWrapper.configureMessagingForTest();
			this.manager.reconfigure();

			File dir = TestUtils.findApplicationDirectory( "lamp" );
			Assert.assertTrue( dir.exists());

			ApplicationTemplate tpl = this.manager.applicationTemplateMngr().loadApplicationTemplate( dir );
			tpl.setExternalExportsPrefix( "prefix" );
			Assert.assertNotNull( tpl );

			ManagedApplication ma = this.manager.applicationMngr().createApplication( "test", null, tpl );
			Assert.assertNotNull( ma );

			// Create a binding between this application and itself.
			// It does not make sense, but this is for test.
			Assert.assertEquals( 0, ma.getApplication().getApplicationBindings().size());
			this.manager.applicationMngr().bindOrUnbindApplication( ma, tpl.getExternalExportsPrefix(), ma.getName(), true );
			Assert.assertEquals( 1, ma.getApplication().getApplicationBindings().size());

			// Bindings must have been saved.
			// Remove the application from the cache and restore it.
			managerWrapper.removeManagedApplication( ma.getName());
			Assert.assertNull( this.manager.applicationMngr().findApplicationByName( ma.getName()));

			this.manager.applicationMngr().restoreApplications();
			ma = this.manager.applicationMngr().findManagedApplicationByName( ma.getName());
			Assert.assertNotNull( ma );

			Assert.assertEquals( 1, ma.getApplication().getApplicationBindings().size());
			Assert.assertTrue( ma.getApplication().getApplicationBindings().get( tpl.getExternalExportsPrefix()).contains( ma.getName()));

			// Unbind and verify
			this.manager.applicationMngr().bindOrUnbindApplication( ma, tpl.getExternalExportsPrefix(), ma.getName(), false );
			Assert.assertEquals( 0, ma.getApplication().getApplicationBindings().size());

			// Bindings must have been saved.
			// Remove the application from the cache and restore it.
			managerWrapper.removeManagedApplication( ma.getName());
			Assert.assertNull( this.manager.applicationMngr().findApplicationByName( ma.getName()));

			this.manager.applicationMngr().restoreApplications();
			ma = this.manager.applicationMngr().findManagedApplicationByName( ma.getName());
			Assert.assertNotNull( ma );
			Assert.assertEquals( 0, ma.getApplication().getApplicationBindings().size());

		} finally {
			this.manager.stop();
		}
	}


	@Test
	public void testApplicationInstancesAreCorrectlyRestored() throws Exception {

		// To prevent #454 from happening again
		this.manager.configurationMngr().setWorkingDirectory( this.folder.newFolder());
		this.manager.setMessagingType( MessagingConstants.FACTORY_TEST );
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
			managerWrapper.clearManagedApplications();
			this.manager.applicationMngr().restoreApplications();

			ma = this.manager.applicationMngr().findManagedApplicationByName( "test" );
			Assert.assertNotNull( ma );
			Assert.assertEquals( 3, ma.getApplication().getRootInstances().size());

		} finally {
			this.manager.stop();
		}
	}


	@Test
	public void testApplicationInstancesStatesAreCorrectlyRestored() throws Exception {

		// Verify instances states are restored correctly.
		// Instances states are restored when the DM starts and when new
		// target handlers appears AFTER the DM has started.

		final String targetId = "for-test";
		final TargetHandler targetHandler = Mockito.mock( TargetHandler.class );
		Mockito.when( targetHandler.getTargetId()).thenReturn( targetId );

		this.manager.configurationMngr().setWorkingDirectory( this.folder.newFolder());
		this.manager.setMessagingType( MessagingConstants.FACTORY_TEST );
		this.manager.setTargetResolver( new ITargetHandlerResolver() {

			@Override
			public TargetHandler findTargetHandler( Map<String,String> targetProperties )
			throws TargetException {
				return targetHandler;
			}
		});

		try {
			// Add the target handler before starting the DM.
			// Verify no restoration was attempted.
			this.manager = Mockito.spy( this.manager );
			IInstancesMngr instancesMngr = Mockito.mock( IInstancesMngr.class );
			Mockito.when( this.manager.instancesMngr()).thenReturn( instancesMngr );

			this.manager.targetAppears( targetHandler );
			Mockito.verify( instancesMngr, Mockito.times( 0 )).restoreInstanceStates(
					Mockito.any( ManagedApplication.class ),
					Mockito.eq( targetHandler ));

			// Start the DM. Still no restoration.
			this.manager.start();
			Mockito.verify( instancesMngr, Mockito.times( 0 )).restoreInstanceStates(
					Mockito.any( ManagedApplication.class ),
					Mockito.eq( targetHandler ));

			// Register an application.
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
			managerWrapper.clearManagedApplications();

			// Restart the DM then.
			this.manager.stop();
			this.manager.start();

			// Verify instances were restored
			ma = this.manager.applicationMngr().findManagedApplicationByName( "test" );
			Assert.assertNotNull( ma );

			// Verify invocations
			Mockito.verify( instancesMngr, Mockito.times( 1 )).restoreInstanceStates( ma, targetHandler );
			Mockito.verify( instancesMngr, Mockito.times( 1 )).restoreInstanceStates(
					Mockito.eq( ma ),
					Mockito.any( TargetHandler.class ));

			// Add a new target handler and verify restoration was also invoked.
			TargetHandler newTargetHandler = Mockito.mock( TargetHandler.class );
			this.manager.targetAppears( newTargetHandler );

			Mockito.verify( instancesMngr, Mockito.times( 1 )).restoreInstanceStates( ma, targetHandler );
			Mockito.verify( instancesMngr, Mockito.times( 1 )).restoreInstanceStates( ma, newTargetHandler );
			Mockito.verify( instancesMngr, Mockito.times( 2 )).restoreInstanceStates(
					Mockito.eq( ma ),
					Mockito.any( TargetHandler.class ));

			// Stop the DM again
			this.manager.stop();

			// Verify adding a new target handler does nothing
			newTargetHandler = Mockito.mock( TargetHandler.class );
			this.manager.targetAppears( newTargetHandler );
			Mockito.verify( instancesMngr, Mockito.times( 2 )).restoreInstanceStates(
					Mockito.eq( ma ),
					Mockito.any( TargetHandler.class ));

		} finally {
			// Manager#stop() is idem-potent.
			this.manager.stop();
		}
	}


	@Test
	public void testSetMessagingType_ignoresSameValue() {

		this.manager = Mockito.spy( this.manager );

		Assert.assertNull( this.manager.messagingType );

		this.manager.setMessagingType( "test" );
		Assert.assertEquals( "test", this.manager.messagingType );
		Mockito.verify( this.manager, Mockito.times( 1 )).reconfigure();

		this.manager.setMessagingType( "test" );
		Assert.assertEquals( "test", this.manager.messagingType );
		Mockito.verify( this.manager, Mockito.times( 1 )).reconfigure();

		this.manager.setMessagingType( "toto" );
		Assert.assertEquals( "toto", this.manager.messagingType );
		Mockito.verify( this.manager, Mockito.times( 2 )).reconfigure();
	}
}
