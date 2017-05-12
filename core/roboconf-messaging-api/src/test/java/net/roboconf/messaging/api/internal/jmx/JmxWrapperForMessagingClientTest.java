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

package net.roboconf.messaging.api.internal.jmx;

import java.io.IOException;
import java.util.Dictionary;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import net.roboconf.core.model.beans.Application;
import net.roboconf.messaging.api.extensions.IMessagingClient;
import net.roboconf.messaging.api.extensions.MessagingContext;
import net.roboconf.messaging.api.extensions.MessagingContext.RecipientKind;
import net.roboconf.messaging.api.jmx.MessagingApiMBean;
import net.roboconf.messaging.api.jmx.RoboconfMessageQueue;
import net.roboconf.messaging.api.messages.Message;
import net.roboconf.messaging.api.utils.MessagingUtils;
import net.roboconf.messaging.api.utils.OsgiHelper;

/**
 * @author Vincent Zurczak - Linagora
 */
public class JmxWrapperForMessagingClientTest {

	@Test
	public void testSimpleWrappedMethods() throws Exception {

		// We do not run in OSGi, but it should not raise any error
		IMessagingClient messagingClient = Mockito.mock( IMessagingClient.class );
		JmxWrapperForMessagingClient client = new JmxWrapperForMessagingClient( messagingClient );

		// Simple wrapped methods
		client.closeConnection();
		client.deleteMessagingServerArtifacts( Mockito.mock( Application.class ));
		Assert.assertEquals( 0, client.getConfiguration().size());
		client.openConnection();
		client.subscribe( Mockito.mock( MessagingContext.class ));
		client.unsubscribe( Mockito.mock( MessagingContext.class ));

		Mockito.verify( messagingClient ).closeConnection();;
		Mockito.verify( messagingClient ).deleteMessagingServerArtifacts( Mockito.any( Application.class ));;
		Mockito.verify( messagingClient ).getConfiguration();
		Mockito.verify( messagingClient ).openConnection();
		Mockito.verify( messagingClient ).subscribe( Mockito.any( MessagingContext.class ));
		Mockito.verify( messagingClient ).unsubscribe( Mockito.any( MessagingContext.class ));
		Mockito.verifyNoMoreInteractions( messagingClient );
	}


	@Test
	public void testPublish_success() throws Exception {

		// We do not run in OSGi, but it should not raise any error
		IMessagingClient messagingClient = Mockito.mock( IMessagingClient.class );
		JmxWrapperForMessagingClient client = new JmxWrapperForMessagingClient( messagingClient );

		// Publish
		Assert.assertEquals( 0, client.getSentMessagesCount());
		Assert.assertEquals( 0, client.getTimestampOfLastSentMessage());

		Assert.assertEquals( 0, client.getFailedSendingCount());
		Assert.assertEquals( 0, client.getTimestampOfLastSendingFailure());

		Assert.assertEquals( 0, client.getReceivedMessagesCount());
		Assert.assertEquals( 0, client.getTimestampOfLastReceivedMessage());

		Assert.assertEquals( 0, client.getFailedReceptionCount());
		Assert.assertEquals( 0, client.getTimestampOfLastReceptionFailure());

		client.publish( Mockito.mock( MessagingContext.class ), Mockito.mock( Message.class ));
		Mockito.verify( messagingClient ).publish( Mockito.any( MessagingContext.class ), Mockito.any( Message.class ));

		Assert.assertEquals( 1, client.getSentMessagesCount());
		Assert.assertNotEquals( 0, client.getTimestampOfLastSentMessage());

		Assert.assertEquals( 0, client.getFailedSendingCount());
		Assert.assertEquals( 0, client.getTimestampOfLastSendingFailure());

		Assert.assertEquals( 0, client.getReceivedMessagesCount());
		Assert.assertEquals( 0, client.getTimestampOfLastReceivedMessage());

		Assert.assertEquals( 0, client.getFailedReceptionCount());
		Assert.assertEquals( 0, client.getTimestampOfLastReceptionFailure());

		// Reset
		client.reset();
		Assert.assertEquals( 0, client.getSentMessagesCount());
		Assert.assertEquals( 0, client.getTimestampOfLastSentMessage());

		Assert.assertEquals( 0, client.getFailedSendingCount());
		Assert.assertEquals( 0, client.getTimestampOfLastSendingFailure());

		Assert.assertEquals( 0, client.getReceivedMessagesCount());
		Assert.assertEquals( 0, client.getTimestampOfLastReceivedMessage());

		Assert.assertEquals( 0, client.getFailedReceptionCount());
		Assert.assertEquals( 0, client.getTimestampOfLastReceptionFailure());
	}


	@Test
	public void testPublish_sendingError() throws Exception {

		// We do not run in OSGi, but it should not raise any error
		IMessagingClient messagingClient = Mockito.mock( IMessagingClient.class );
		JmxWrapperForMessagingClient client = new JmxWrapperForMessagingClient( messagingClient );
		Mockito.doThrow( new IOException( "for test" )).when( messagingClient ).publish(
				Mockito.any( MessagingContext.class ),
				Mockito.any( Message.class ));

		// Publish
		Assert.assertEquals( 0, client.getSentMessagesCount());
		Assert.assertEquals( 0, client.getTimestampOfLastSentMessage());

		Assert.assertEquals( 0, client.getFailedSendingCount());
		Assert.assertEquals( 0, client.getTimestampOfLastSendingFailure());

		Assert.assertEquals( 0, client.getReceivedMessagesCount());
		Assert.assertEquals( 0, client.getTimestampOfLastReceivedMessage());

		Assert.assertEquals( 0, client.getFailedReceptionCount());
		Assert.assertEquals( 0, client.getTimestampOfLastReceptionFailure());

		try {
			client.publish( Mockito.mock( MessagingContext.class ), Mockito.mock( Message.class ));
			Assert.fail( "An IO exception was expected. It should have been propagated." );

		} catch( IOException e ) {
			// nothing
		}

		Mockito.verify( messagingClient ).publish( Mockito.any( MessagingContext.class ), Mockito.any( Message.class ));

		Assert.assertEquals( 0, client.getSentMessagesCount());
		Assert.assertEquals( 0, client.getTimestampOfLastSentMessage());

		Assert.assertEquals( 1, client.getFailedSendingCount());
		Assert.assertNotEquals( 0, client.getTimestampOfLastSendingFailure());

		Assert.assertEquals( 0, client.getReceivedMessagesCount());
		Assert.assertEquals( 0, client.getTimestampOfLastReceivedMessage());

		Assert.assertEquals( 0, client.getFailedReceptionCount());
		Assert.assertEquals( 0, client.getTimestampOfLastReceptionFailure());

		// Reset
		client.reset();
		Assert.assertEquals( 0, client.getSentMessagesCount());
		Assert.assertEquals( 0, client.getTimestampOfLastSentMessage());

		Assert.assertEquals( 0, client.getFailedSendingCount());
		Assert.assertEquals( 0, client.getTimestampOfLastSendingFailure());

		Assert.assertEquals( 0, client.getReceivedMessagesCount());
		Assert.assertEquals( 0, client.getTimestampOfLastReceivedMessage());

		Assert.assertEquals( 0, client.getFailedReceptionCount());
		Assert.assertEquals( 0, client.getTimestampOfLastReceptionFailure());
	}



	@Test
	public void testSetMessageQueue_notNull() {

		// We do not run in OSGi, but it should not raise any error
		IMessagingClient messagingClient = Mockito.mock( IMessagingClient.class );
		JmxWrapperForMessagingClient client = new JmxWrapperForMessagingClient( messagingClient );

		// Set the message queue
		Assert.assertEquals( 0, client.getSentMessagesCount());
		Assert.assertEquals( 0, client.getTimestampOfLastSentMessage());

		Assert.assertEquals( 0, client.getFailedSendingCount());
		Assert.assertEquals( 0, client.getTimestampOfLastSendingFailure());

		Assert.assertEquals( 0, client.getReceivedMessagesCount());
		Assert.assertEquals( 0, client.getTimestampOfLastReceivedMessage());

		Assert.assertEquals( 0, client.getFailedReceptionCount());
		Assert.assertEquals( 0, client.getTimestampOfLastReceptionFailure());

		RoboconfMessageQueue queue = new RoboconfMessageQueue();
		queue.add( Mockito.mock( Message.class ));
		queue.errorWhileReceivingMessage();

		client.setMessageQueue( queue );
		Mockito.verify( messagingClient ).setMessageQueue( queue );

		Assert.assertEquals( 0, client.getSentMessagesCount());
		Assert.assertEquals( 0, client.getTimestampOfLastSentMessage());

		Assert.assertEquals( 0, client.getFailedSendingCount());
		Assert.assertEquals( 0, client.getTimestampOfLastSendingFailure());

		Assert.assertEquals( queue.getReceivedMessagesCount(), client.getReceivedMessagesCount());
		Assert.assertEquals( queue.getTimestampOfLastReceivedMessage(), client.getTimestampOfLastReceivedMessage());

		Assert.assertEquals( queue.getFailedReceptionCount(), client.getFailedReceptionCount());
		Assert.assertEquals( queue.getTimestampOfLastReceptionFailure(), client.getTimestampOfLastReceptionFailure());

		// Reset
		client.reset();
		Assert.assertEquals( 0, client.getSentMessagesCount());
		Assert.assertEquals( 0, client.getTimestampOfLastSentMessage());

		Assert.assertEquals( 0, client.getFailedSendingCount());
		Assert.assertEquals( 0, client.getTimestampOfLastSendingFailure());

		Assert.assertEquals( 0, client.getReceivedMessagesCount());
		Assert.assertEquals( 0, client.getTimestampOfLastReceivedMessage());

		Assert.assertEquals( 0, client.getFailedReceptionCount());
		Assert.assertEquals( 0, client.getTimestampOfLastReceptionFailure());
	}


	@Test
	public void testSetOwnerProperties() {

		IMessagingClient messagingClient = Mockito.mock( IMessagingClient.class );
		JmxWrapperForMessagingClient client = new JmxWrapperForMessagingClient( messagingClient );

		Assert.assertNull( client.getId());
		client.setOwnerProperties( RecipientKind.DM, "domain", "app", "/root" );

		Mockito.verify( messagingClient ).setOwnerProperties( RecipientKind.DM, "domain", "app", "/root" );
		Assert.assertNotNull( client.getId());
		Assert.assertEquals( MessagingUtils.buildId( RecipientKind.DM, "domain", "app", "/root" ), client.getId());
	}


	@Test
	public void testUnregister_noRegistration() {

		IMessagingClient messagingClient = Mockito.mock( IMessagingClient.class );
		JmxWrapperForMessagingClient client = new JmxWrapperForMessagingClient( messagingClient );

		Assert.assertNull( client.serviceReg );
		client.unregisterService();
		Assert.assertNull( client.serviceReg );
	}


	@Test
	@SuppressWarnings( "unchecked" )
	public void testUnregister_withRegistration() {

		IMessagingClient messagingClient = Mockito.mock( IMessagingClient.class );
		JmxWrapperForMessagingClient client = new JmxWrapperForMessagingClient( messagingClient );
		ServiceRegistration<MessagingApiMBean> serviceReg = Mockito.mock( ServiceRegistration.class );
		client.serviceReg = serviceReg;

		client.unregisterService();
		Assert.assertNull( client.serviceReg );
		Mockito.verify( serviceReg, Mockito.only()).unregister();
	}


	@Test
	@SuppressWarnings( "unchecked" )
	public void testUnregister_withRegistration_withException() {

		IMessagingClient messagingClient = Mockito.mock( IMessagingClient.class );
		JmxWrapperForMessagingClient client = new JmxWrapperForMessagingClient( messagingClient );

		ServiceRegistration<MessagingApiMBean> serviceReg = Mockito.mock( ServiceRegistration.class );
		Mockito.doThrow( new RuntimeException( "for test" )).when( serviceReg ).unregister();
		client.serviceReg = serviceReg;

		client.unregisterService();
		Assert.assertNull( client.serviceReg );
		Mockito.verify( serviceReg, Mockito.only()).unregister();
	}


	@Test
	@SuppressWarnings( "unchecked" )
	public void testConstructor_mockOsgi() {

		ServiceRegistration<MessagingApiMBean> serviceReg = Mockito.mock( ServiceRegistration.class );
		BundleContext bundleCtx = Mockito.mock( BundleContext.class );
		Mockito.when( bundleCtx.registerService(
				Mockito.eq( MessagingApiMBean.class ),
				Mockito.any( JmxWrapperForMessagingClient.class ),
				Mockito.any( Dictionary.class ))).thenReturn( serviceReg );

		OsgiHelper osgiHelper = Mockito.mock( OsgiHelper.class );
		Mockito.when( osgiHelper.findBundleContext()).thenReturn( bundleCtx );

		IMessagingClient messagingClient = Mockito.mock( IMessagingClient.class );
		JmxWrapperForMessagingClient client = new JmxWrapperForMessagingClient( messagingClient, osgiHelper );
		Assert.assertNotNull( client.serviceReg );
		Assert.assertEquals( serviceReg, client.serviceReg );
	}


	@Test
	@SuppressWarnings( "unchecked" )
	public void testConstructor_mockOsgi_exceptionDuringRegistration() {

		BundleContext bundleCtx = Mockito.mock( BundleContext.class );
		Mockito.when( bundleCtx.registerService(
				Mockito.eq( MessagingApiMBean.class ),
				Mockito.any( JmxWrapperForMessagingClient.class ),
				Mockito.any( Dictionary.class ))).thenThrow( new RuntimeException( "for test" ));

		OsgiHelper osgiHelper = Mockito.mock( OsgiHelper.class );
		Mockito.when( osgiHelper.findBundleContext()).thenReturn( bundleCtx );

		IMessagingClient messagingClient = Mockito.mock( IMessagingClient.class );
		JmxWrapperForMessagingClient client = new JmxWrapperForMessagingClient( messagingClient, osgiHelper );
		Assert.assertNull( client.serviceReg );
	}
}
