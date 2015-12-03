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

package net.roboconf.dm.internal.api.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import org.junit.Assert;
import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.dm.internal.api.IRandomMngr;
import net.roboconf.dm.internal.test.TestTargetResolver;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.api.IConfigurationMngr;
import net.roboconf.dm.management.api.IInstancesMngr;
import net.roboconf.dm.management.api.IMessagingMngr;
import net.roboconf.dm.management.api.INotificationMngr;
import net.roboconf.dm.management.api.ITargetsMngr;
import net.roboconf.messaging.api.business.IDmClient;
import net.roboconf.target.api.TargetException;
import net.roboconf.target.api.TargetHandler;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * @author Vincent Zurczak - Linagora
 */
public class InstancesMngrImplTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();


	@Test
	public void testProcessExceptions_noException() throws Exception {

		Logger logger = Logger.getLogger( getClass().getName());
		List<Exception> exceptions = new ArrayList<>( 0 );
		InstancesMngrImpl.processExceptions( logger, exceptions, "whatever" );
	}


	@Test( expected = IOException.class )
	public void testProcessExceptions_withException() throws Exception {

		Logger logger = Logger.getLogger( getClass().getName());
		List<Exception> exceptions = new ArrayList<>( 1 );
		exceptions.add( new Exception( "oops" ));

		InstancesMngrImpl.processExceptions( logger, exceptions, "whatever" );
	}


	@Test
	public void testTargetsLocking_whenCreatingMachines_noException() throws Exception {

		// Prepare stuff
		INotificationMngr notificationMngr = Mockito.mock( INotificationMngr.class );
		ITargetsMngr targetsMngr = Mockito.mock( ITargetsMngr.class );
		IRandomMngr randomMngr = Mockito.mock( IRandomMngr.class );

		IMessagingMngr messagingMngr = Mockito.mock( IMessagingMngr.class );
		Mockito.when( messagingMngr.getMessagingClient()).thenReturn( Mockito.mock( IDmClient.class ));

		IConfigurationMngr configurationMngr = new ConfigurationMngrImpl();
		configurationMngr.setWorkingDirectory( this.folder.newFolder());

		IInstancesMngr mngr = new InstancesMngrImpl( messagingMngr, notificationMngr, targetsMngr, randomMngr );
		((InstancesMngrImpl) mngr).setTargetHandlerResolver( new TestTargetResolver());

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app );

		// We want to make sure target locking is correctly invoked by the instances manager.
		// So, we store requests to lock or unlock a target for a given instance.
		final Map<Instance,Integer> instancePathToLock = new HashMap<Instance,Integer> ();
		Mockito.when( targetsMngr.lockAndGetTarget( app, app.getMySqlVm())).thenAnswer( new Answer<Map<String,String>>() {

			@Override
			public Map<String,String> answer( InvocationOnMock invocation ) throws Throwable {

				Instance inst = invocation.getArgumentAt( 1, Instance.class );
				Integer count = instancePathToLock.get( inst );
				count = count == null ? 1 : count + 1;
				instancePathToLock.put( inst, count );

				Map<String,String> result = new HashMap<String,String>( 0 );
				return result;
			}
		});

		Mockito.doAnswer( new Answer<Object>() {
			@Override
			public Object answer( InvocationOnMock invocation ) throws Throwable {

				Instance inst = invocation.getArgumentAt( 1, Instance.class );
				Integer count = instancePathToLock.get( inst );
				count = count == null ? 0 : count - 1;
				if( count > 0 )
					instancePathToLock.put( inst, count );
				else
					instancePathToLock.remove( inst );

				return null;
			}
		}).when( targetsMngr ).unlockTarget( app, app.getMySqlVm());

		// Let's run assertions now
		Assert.assertEquals( 0, instancePathToLock.size());
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getMySqlVm().getStatus());
		mngr.changeInstanceState( ma, app.getMySqlVm(), InstanceStatus.DEPLOYED_STARTED );
		Assert.assertEquals( 1, instancePathToLock.size());

		Integer lockCount = instancePathToLock.get( app.getMySqlVm());
		Assert.assertNotNull( lockCount );
		Assert.assertEquals( 1, lockCount.intValue());
		Assert.assertEquals( InstanceStatus.DEPLOYING, app.getMySqlVm().getStatus());

		// Release the machine
		mngr.changeInstanceState( ma, app.getMySqlVm(), InstanceStatus.NOT_DEPLOYED );
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getMySqlVm().getStatus());
		Assert.assertEquals( 0, instancePathToLock.size());
	}


	@Test
	@SuppressWarnings( "unchecked" )
	public void testTargetsLocking_whenCreatingMachines_withExceptionInDeploy() throws Exception {

		// Prepare stuff
		INotificationMngr notificationMngr = Mockito.mock( INotificationMngr.class );
		ITargetsMngr targetsMngr = Mockito.mock( ITargetsMngr.class );
		IRandomMngr randomMngr = Mockito.mock( IRandomMngr.class );

		IMessagingMngr messagingMngr = Mockito.mock( IMessagingMngr.class );
		Mockito.when( messagingMngr.getMessagingClient()).thenReturn( Mockito.mock( IDmClient.class ));

		IConfigurationMngr configurationMngr = new ConfigurationMngrImpl();
		configurationMngr.setWorkingDirectory( this.folder.newFolder());

		final TargetHandler targetHandler = Mockito.mock( TargetHandler.class );
		Mockito.when( targetHandler.createMachine(
				Mockito.any( Map.class ),
				Mockito.any( Map.class ),
				Mockito.anyString(),
				Mockito.anyString())).thenThrow( new TargetException( "for test" ));

		IInstancesMngr mngr = new InstancesMngrImpl( messagingMngr, notificationMngr, targetsMngr, randomMngr );
		((InstancesMngrImpl) mngr).setTargetHandlerResolver( new TestTargetResolver() {
			@Override
			public TargetHandler findTargetHandler( Map<String,String> targetProperties ) throws TargetException {
				return targetHandler;
			}
		});

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app );

		// We will try to create a machine. It will fail.
		// We must be sure the lock was acquired, and that it was released.
		final AtomicBoolean acquired = new AtomicBoolean( false );
		final AtomicBoolean released = new AtomicBoolean( false );

		Mockito.when( targetsMngr.lockAndGetTarget( app, app.getMySqlVm())).thenAnswer( new Answer<Map<String,String>>() {

			@Override
			public Map<String,String> answer( InvocationOnMock invocation ) throws Throwable {
				acquired.set( true );
				Map<String,String> result = new HashMap<String,String>( 0 );
				return result;
			}
		});

		Mockito.doAnswer( new Answer<Object>() {
			@Override
			public Object answer( InvocationOnMock invocation ) throws Throwable {
				released.set( true );
				return null;
			}
		}).when( targetsMngr ).unlockTarget( app, app.getMySqlVm());

		// Let's run assertions now
		Assert.assertFalse( acquired.get());
		Assert.assertFalse( released.get());
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getMySqlVm().getStatus());

		try {
			mngr.changeInstanceState( ma, app.getMySqlVm(), InstanceStatus.DEPLOYED_STARTED );
			Assert.fail( "A target exception was expected." );

		} catch( Exception e ) {
			// nothing
		}

		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getMySqlVm().getStatus());
		Assert.assertTrue( acquired.get());
		Assert.assertTrue( released.get());
	}
}
