/**
 * Copyright 2015-2016 Linagora, Université Joseph Fourier, Floralis
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.model.beans.Application;
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
import net.roboconf.dm.management.events.EventType;
import net.roboconf.messaging.api.business.IDmClient;
import net.roboconf.messaging.api.messages.Message;
import net.roboconf.messaging.api.messages.from_dm_to_agent.MsgCmdSendInstances;
import net.roboconf.target.api.TargetException;
import net.roboconf.target.api.TargetHandler;
import net.roboconf.target.api.TargetHandlerParameters;

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
	public void testNotificationsWhenUndeployingScopedInstances_changeInstanceState() throws Exception {

		// Prepare stuff
		final TestApplication app = new TestApplication();
		final Map<Instance,List<InstanceStatus>> instanceToStatusHistory = new HashMap<> ();
		INotificationMngr notificationMngr = new NotificationMngrImpl() {
			@Override
			public void instance( Instance instance, Application application, EventType eventType ) {

				Assert.assertEquals( EventType.CHANGED, eventType );
				Assert.assertEquals( app, application );

				List<InstanceStatus> status = instanceToStatusHistory.get( instance );
				if( status == null ) {
					status = new ArrayList<> ();
					instanceToStatusHistory.put( instance, status );
				}

				status.add( instance.getStatus());
			}
		};

		ITargetsMngr targetsMngr = Mockito.mock( ITargetsMngr.class );
		IRandomMngr randomMngr = Mockito.mock( IRandomMngr.class );

		IMessagingMngr messagingMngr = Mockito.mock( IMessagingMngr.class );
		Mockito.when( messagingMngr.getMessagingClient()).thenReturn( Mockito.mock( IDmClient.class ));

		IConfigurationMngr configurationMngr = new ConfigurationMngrImpl();
		configurationMngr.setWorkingDirectory( this.folder.newFolder());

		IInstancesMngr mngr = new InstancesMngrImpl( messagingMngr, notificationMngr, targetsMngr, randomMngr );
		((InstancesMngrImpl) mngr).setTargetHandlerResolver( new TestTargetResolver());

		// Make one of our VM being fully deployed
		ManagedApplication ma = new ManagedApplication( app );
		app.getTomcatVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		app.getTomcat().setStatus( InstanceStatus.DEPLOYED_STARTED );
		app.getWar().setStatus( InstanceStatus.DEPLOYED_STARTED );

		// One scoped instance has a machine ID (considered as running somewhere)
		app.getTomcatVm().data.put( Instance.MACHINE_ID, "machine-id" );

		// Stop everything
		mngr.changeInstanceState( ma, app.getTomcatVm(), InstanceStatus.NOT_DEPLOYED );

		// Check notifications
		Assert.assertEquals( 3, instanceToStatusHistory.size());
		List<InstanceStatus> statusHistory = instanceToStatusHistory.get( app.getTomcatVm());
		Assert.assertNotNull( statusHistory );
		Assert.assertEquals( Arrays.asList( InstanceStatus.UNDEPLOYING, InstanceStatus.NOT_DEPLOYED ), statusHistory );

		statusHistory = instanceToStatusHistory.get( app.getTomcat());
		Assert.assertNotNull( statusHistory );
		Assert.assertEquals( Arrays.asList( InstanceStatus.NOT_DEPLOYED ), statusHistory );

		statusHistory = instanceToStatusHistory.get( app.getWar());
		Assert.assertNotNull( statusHistory );
		Assert.assertEquals( Arrays.asList( InstanceStatus.NOT_DEPLOYED ), statusHistory );
	}


	@Test
	public void testNotificationsWhenUndeployingScopedInstances_undeployAll() throws Exception {

		// Prepare stuff
		final TestApplication app = new TestApplication();
		final Map<Instance,List<InstanceStatus>> instanceToStatusHistory = new HashMap<> ();
		INotificationMngr notificationMngr = new NotificationMngrImpl() {
			@Override
			public void instance( Instance instance, Application application, EventType eventType ) {

				Assert.assertEquals( EventType.CHANGED, eventType );
				Assert.assertEquals( app, application );

				List<InstanceStatus> status = instanceToStatusHistory.get( instance );
				if( status == null ) {
					status = new ArrayList<> ();
					instanceToStatusHistory.put( instance, status );
				}

				status.add( instance.getStatus());
			}
		};

		ITargetsMngr targetsMngr = Mockito.mock( ITargetsMngr.class );
		IRandomMngr randomMngr = Mockito.mock( IRandomMngr.class );

		IMessagingMngr messagingMngr = Mockito.mock( IMessagingMngr.class );
		Mockito.when( messagingMngr.getMessagingClient()).thenReturn( Mockito.mock( IDmClient.class ));

		IConfigurationMngr configurationMngr = new ConfigurationMngrImpl();
		configurationMngr.setWorkingDirectory( this.folder.newFolder());

		IInstancesMngr mngr = new InstancesMngrImpl( messagingMngr, notificationMngr, targetsMngr, randomMngr );
		((InstancesMngrImpl) mngr).setTargetHandlerResolver( new TestTargetResolver());

		// Make one of our VM being fully deployed
		ManagedApplication ma = new ManagedApplication( app );
		app.getTomcatVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		app.getTomcat().setStatus( InstanceStatus.DEPLOYED_STARTED );
		app.getWar().setStatus( InstanceStatus.DEPLOYED_STARTED );

		// One scoped instance has a machine ID (considered as running somewhere)
		app.getTomcatVm().data.put( Instance.MACHINE_ID, "machine-id" );

		// Stop everything
		mngr.undeployAll( ma, app.getTomcatVm());

		// Check notifications
		Assert.assertEquals( 3, instanceToStatusHistory.size());
		List<InstanceStatus> statusHistory = instanceToStatusHistory.get( app.getTomcatVm());
		Assert.assertNotNull( statusHistory );
		Assert.assertEquals( Arrays.asList( InstanceStatus.UNDEPLOYING, InstanceStatus.NOT_DEPLOYED ), statusHistory );

		statusHistory = instanceToStatusHistory.get( app.getTomcat());
		Assert.assertNotNull( statusHistory );
		Assert.assertEquals( Arrays.asList( InstanceStatus.NOT_DEPLOYED ), statusHistory );

		statusHistory = instanceToStatusHistory.get( app.getWar());
		Assert.assertNotNull( statusHistory );
		Assert.assertEquals( Arrays.asList( InstanceStatus.NOT_DEPLOYED ), statusHistory );
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
		final Map<Instance,Integer> instancePathToLock = new HashMap<> ();
		Mockito.when( targetsMngr.lockAndGetTarget( app, app.getMySqlVm())).thenAnswer( new Answer<Map<String,String>>() {

			@Override
			public Map<String,String> answer( InvocationOnMock invocation ) throws Throwable {

				Instance inst = invocation.getArgumentAt( 1, Instance.class );
				Integer count = instancePathToLock.get( inst );
				count = count == null ? 1 : count + 1;
				instancePathToLock.put( inst, count );

				Map<String,String> result = new HashMap<>( 0 );
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
		Mockito.when( targetHandler
				.createMachine( Mockito.any( TargetHandlerParameters.class )))
				.thenThrow( new TargetException( "for test" ));

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
				Map<String,String> result = new HashMap<>( 0 );
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


	@Test
	public void testRestoreInstances_nullHandler() throws Exception {

		// Prepare stuff
		INotificationMngr notificationMngr = Mockito.mock( INotificationMngr.class );
		IRandomMngr randomMngr = Mockito.mock( IRandomMngr.class );
		IMessagingMngr messagingMngr = Mockito.mock( IMessagingMngr.class );

		ITargetsMngr targetsMngr = Mockito.mock( ITargetsMngr.class );
		Mockito.when( targetsMngr.findRawTargetProperties(
				Mockito.any( Application.class ),
				Mockito.anyString())).thenReturn( new HashMap<String,String>( 0 ));

		IConfigurationMngr configurationMngr = new ConfigurationMngrImpl();
		configurationMngr.setWorkingDirectory( this.folder.newFolder());

		final TargetHandler targetHandlerArgument = Mockito.mock( TargetHandler.class );
		Mockito.when( targetHandlerArgument.getTargetId()).thenReturn( "some target id" );

		IInstancesMngr mngr = new InstancesMngrImpl( messagingMngr, notificationMngr, targetsMngr, randomMngr );
		((InstancesMngrImpl) mngr).setTargetHandlerResolver( new TestTargetResolver() {
			@Override
			public TargetHandler findTargetHandler( Map<String,String> targetProperties ) throws TargetException {
				throw new TargetException( "No handler for tests" );
			}
		});

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app );

		// One scoped instance has a machine ID (considered as running somewhere)
		app.getMySqlVm().data.put( Instance.MACHINE_ID, "machine-id" );

		// Try to restore instances
		mngr.restoreInstanceStates( ma, targetHandlerArgument );

		// The handler's ID did not match => no restoration and no use of other mocks
		Mockito.verify( targetsMngr, Mockito.only()).findRawTargetProperties( Mockito.eq( app ), Mockito.anyString());

		Mockito.verifyZeroInteractions( targetHandlerArgument );
		Mockito.verifyZeroInteractions( messagingMngr );
		Mockito.verifyZeroInteractions( randomMngr );

		// No notification was sent since there was no change on Tomcat instances
		Mockito.verifyZeroInteractions( notificationMngr );
	}


	@Test
	public void testRestoreInstances_nonNullNonMatchingHandler() throws Exception {

		// Prepare stuff
		INotificationMngr notificationMngr = Mockito.mock( INotificationMngr.class );
		IRandomMngr randomMngr = Mockito.mock( IRandomMngr.class );
		IMessagingMngr messagingMngr = Mockito.mock( IMessagingMngr.class );

		ITargetsMngr targetsMngr = Mockito.mock( ITargetsMngr.class );
		Mockito.when( targetsMngr.findRawTargetProperties(
				Mockito.any( Application.class ),
				Mockito.anyString())).thenReturn( new HashMap<String,String>( 0 ));

		IConfigurationMngr configurationMngr = new ConfigurationMngrImpl();
		configurationMngr.setWorkingDirectory( this.folder.newFolder());

		final TargetHandler targetHandlerArgument = Mockito.mock( TargetHandler.class );
		Mockito.when( targetHandlerArgument.getTargetId()).thenReturn( "some target id" );

		final TargetHandler targetHandler = Mockito.mock( TargetHandler.class );
		Mockito.when( targetHandler.getTargetId()).thenReturn( "some other target id" );

		IInstancesMngr mngr = new InstancesMngrImpl( messagingMngr, notificationMngr, targetsMngr, randomMngr );
		((InstancesMngrImpl) mngr).setTargetHandlerResolver( new TestTargetResolver() {
			@Override
			public TargetHandler findTargetHandler( Map<String,String> targetProperties ) throws TargetException {
				return targetHandler;
			}
		});

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app );

		// One scoped instance has a machine ID (considered as running somewhere)
		app.getMySqlVm().data.put( Instance.MACHINE_ID, "machine-id" );

		// Try to restore instances
		mngr.restoreInstanceStates( ma, targetHandlerArgument );

		// The handler's ID did not match => no restoration and no use of other mocks
		Mockito.verify( targetsMngr, Mockito.only()).findRawTargetProperties( Mockito.eq( app ), Mockito.anyString());
		Mockito.verify( targetHandler, Mockito.only()).getTargetId();
		Mockito.verify( targetHandlerArgument, Mockito.only()).getTargetId();

		Mockito.verifyZeroInteractions( messagingMngr );
		Mockito.verifyZeroInteractions( randomMngr );

		// No notification was sent since there was no change on Tomcat instances
		Mockito.verifyZeroInteractions( notificationMngr );
	}


	@Test
	public void testRestoreInstances_rightHandler_vmRunning() throws Exception {

		// Prepare stuff
		Map<String,String> targetProperties = new HashMap<>( 0 );

		INotificationMngr notificationMngr = Mockito.mock( INotificationMngr.class );
		IRandomMngr randomMngr = Mockito.mock( IRandomMngr.class );
		IMessagingMngr messagingMngr = Mockito.mock( IMessagingMngr.class );

		ITargetsMngr targetsMngr = Mockito.mock( ITargetsMngr.class );
		Mockito.when( targetsMngr.findRawTargetProperties(
				Mockito.any( Application.class ),
				Mockito.anyString())).thenReturn( targetProperties );

		IConfigurationMngr configurationMngr = new ConfigurationMngrImpl();
		configurationMngr.setWorkingDirectory( this.folder.newFolder());

		final TargetHandler targetHandlerArgument = Mockito.mock( TargetHandler.class );
		Mockito.when( targetHandlerArgument.getTargetId()).thenReturn( "some target id" );

		IInstancesMngr mngr = new InstancesMngrImpl( messagingMngr, notificationMngr, targetsMngr, randomMngr );
		((InstancesMngrImpl) mngr).setTargetHandlerResolver( new TestTargetResolver() {
			@Override
			public TargetHandler findTargetHandler( Map<String,String> targetProperties ) throws TargetException {
				return targetHandlerArgument;
			}
		});

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app );

		// One scoped instance has a machine ID (considered as running somewhere)
		app.getMySqlVm().data.put( Instance.MACHINE_ID, "machine-id" );

		// Try to restore instances
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getMySqlVm().getStatus());
		Mockito.when( targetHandlerArgument.isMachineRunning( targetProperties, "machine-id" )).thenReturn( true );
		mngr.restoreInstanceStates( ma, targetHandlerArgument );
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, app.getMySqlVm().getStatus());

		// The handler's ID matched and the VM is running => a message was sent.
		Mockito.verify( targetsMngr, Mockito.only()).findRawTargetProperties( Mockito.eq( app ), Mockito.anyString());
		Mockito.verify( targetHandlerArgument, Mockito.times( 1 )).isMachineRunning( targetProperties, "machine-id" );

		Mockito.verify( messagingMngr, Mockito.only()).sendMessageDirectly(
				Mockito.eq( ma ),
				Mockito.eq( app.getMySqlVm() ),
				Mockito.any( MsgCmdSendInstances.class ));

		// No notification was sent since there was no change on Tomcat instances
		Mockito.verifyZeroInteractions( notificationMngr );
		Mockito.verifyZeroInteractions( randomMngr );
	}


	@Test
	public void testRestoreInstances_rightHandler_vmRunning_withMessagingException() throws Exception {

		// Prepare stuff
		Map<String,String> targetProperties = new HashMap<>( 0 );

		INotificationMngr notificationMngr = Mockito.mock( INotificationMngr.class );
		IRandomMngr randomMngr = Mockito.mock( IRandomMngr.class );

		ITargetsMngr targetsMngr = Mockito.mock( ITargetsMngr.class );
		Mockito.when( targetsMngr.findRawTargetProperties(
				Mockito.any( Application.class ),
				Mockito.anyString())).thenReturn( targetProperties );

		IMessagingMngr messagingMngr = Mockito.mock( IMessagingMngr.class );
		Mockito.doThrow( new IOException( "for test" )).when( messagingMngr ).sendMessageDirectly(
				Mockito.any( ManagedApplication.class ),
				Mockito.any( Instance.class ),
				Mockito.any( Message.class ));

		IConfigurationMngr configurationMngr = new ConfigurationMngrImpl();
		configurationMngr.setWorkingDirectory( this.folder.newFolder());

		final TargetHandler targetHandlerArgument = Mockito.mock( TargetHandler.class );
		Mockito.when( targetHandlerArgument.getTargetId()).thenReturn( "some target id" );

		IInstancesMngr mngr = new InstancesMngrImpl( messagingMngr, notificationMngr, targetsMngr, randomMngr );
		((InstancesMngrImpl) mngr).setTargetHandlerResolver( new TestTargetResolver() {
			@Override
			public TargetHandler findTargetHandler( Map<String,String> targetProperties ) throws TargetException {
				return targetHandlerArgument;
			}
		});

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app );

		// One scoped instance has a machine ID (considered as running somewhere)
		app.getMySqlVm().data.put( Instance.MACHINE_ID, "machine-id" );

		// Try to restore instances
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getMySqlVm().getStatus());
		Mockito.when( targetHandlerArgument.isMachineRunning( targetProperties, "machine-id" )).thenReturn( true );
		mngr.restoreInstanceStates( ma, targetHandlerArgument );
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, app.getMySqlVm().getStatus());

		// The handler's ID matched and the VM is running => a message was sent.
		Mockito.verify( targetsMngr, Mockito.only()).findRawTargetProperties( Mockito.eq( app ), Mockito.anyString());
		Mockito.verify( targetHandlerArgument, Mockito.times( 1 )).isMachineRunning( targetProperties, "machine-id" );

		Mockito.verify( messagingMngr, Mockito.only()).sendMessageDirectly(
				Mockito.eq( ma ),
				Mockito.eq( app.getMySqlVm() ),
				Mockito.any( MsgCmdSendInstances.class ));

		// No notification was sent since there was no change on Tomcat instances
		Mockito.verifyZeroInteractions( notificationMngr );
		Mockito.verifyZeroInteractions( randomMngr );
	}


	@Test
	public void testRestoreInstances_rightHandler_vmNotRunning() throws Exception {

		// Prepare stuff
		Map<String,String> targetProperties = new HashMap<>( 0 );

		INotificationMngr notificationMngr = Mockito.mock( INotificationMngr.class );
		IRandomMngr randomMngr = Mockito.mock( IRandomMngr.class );
		IMessagingMngr messagingMngr = Mockito.mock( IMessagingMngr.class );

		ITargetsMngr targetsMngr = Mockito.mock( ITargetsMngr.class );
		Mockito.when( targetsMngr.findRawTargetProperties(
				Mockito.any( Application.class ),
				Mockito.anyString())).thenReturn( targetProperties );

		IConfigurationMngr configurationMngr = new ConfigurationMngrImpl();
		configurationMngr.setWorkingDirectory( this.folder.newFolder());

		final TargetHandler targetHandlerArgument = Mockito.mock( TargetHandler.class );
		Mockito.when( targetHandlerArgument.getTargetId()).thenReturn( "some target id" );

		IInstancesMngr mngr = new InstancesMngrImpl( messagingMngr, notificationMngr, targetsMngr, randomMngr );
		((InstancesMngrImpl) mngr).setTargetHandlerResolver( new TestTargetResolver() {
			@Override
			public TargetHandler findTargetHandler( Map<String,String> targetProperties ) throws TargetException {
				return targetHandlerArgument;
			}
		});

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app );

		// One scoped instance has a machine ID (considered as running somewhere)
		app.getMySqlVm().data.put( Instance.MACHINE_ID, "machine-id" );

		// Try to restore instances
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getMySqlVm().getStatus());
		app.getMySqlVm().setStatus( InstanceStatus.DEPLOYED_STARTED );

		Mockito.when( targetHandlerArgument.isMachineRunning( targetProperties, "machine-id" )).thenReturn( false );
		mngr.restoreInstanceStates( ma, targetHandlerArgument );
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getMySqlVm().getStatus());

		// The handler's ID matched and the VM is NOT running => no message was sent.
		Mockito.verify( targetsMngr, Mockito.only()).findRawTargetProperties( Mockito.eq( app ), Mockito.anyString());
		Mockito.verify( targetHandlerArgument, Mockito.times( 1 )).isMachineRunning( targetProperties, "machine-id" );

		Mockito.verifyZeroInteractions( messagingMngr );
		Mockito.verifyZeroInteractions( randomMngr );

		// A notification was sent for the instance whose state changed
		Mockito.verify( notificationMngr ).instance(
				Mockito.any( Instance.class ),
				Mockito.eq( app ),
				Mockito.eq( EventType.CHANGED ));
	}


	@Test
	public void testRestoreInstances_noMachineId() throws Exception {

		// Prepare stuff
		IInstancesMngr mngr = new InstancesMngrImpl( null, null, null, null );
		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app );

		TargetHandler targetHandler = Mockito.mock( TargetHandler.class );

		// Try to restore instances
		mngr.restoreInstanceStates( ma, targetHandler );

		// We did not go very far, and the mock was not even checked.
		Mockito.verifyZeroInteractions( targetHandler );
	}
}
