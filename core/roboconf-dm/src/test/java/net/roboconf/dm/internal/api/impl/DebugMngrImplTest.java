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

import java.io.IOException;
import java.util.List;

import org.junit.Assert;
import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.api.IDebugMngr;
import net.roboconf.dm.management.api.IMessagingMngr;
import net.roboconf.dm.management.api.INotificationMngr;
import net.roboconf.messaging.api.messages.Message;
import net.roboconf.messaging.api.messages.from_dm_to_dm.MsgEcho;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * @author Vincent Zurczak - Linagora
 */
public class DebugMngrImplTest {

	private IDebugMngr mngr;
	private IMessagingMngr messagingMngr;
	private INotificationMngr notificationMngr;


	@Before
	public void prepareMngr() {

		this.messagingMngr = Mockito.mock( IMessagingMngr.class );
		this.notificationMngr = Mockito.mock( INotificationMngr.class );
		this.mngr = new DebugMngrImpl( this.messagingMngr, this.notificationMngr );
	}


	@Test
	public void testSendPingMessageQueue_normal() throws Exception {

		Mockito.verifyZeroInteractions( this.messagingMngr );
		Assert.assertTrue( this.mngr.pingMessageQueue( "TEST" ));

		ArgumentCaptor<Message> argument = ArgumentCaptor.forClass( Message.class );
		Mockito.verify( this.messagingMngr, Mockito.times( 1 )).sendMessageToTheDm( argument.capture());

		Message message = argument.getValue();
		Assert.assertTrue( message instanceof MsgEcho );

		MsgEcho echo = (MsgEcho) message;
		Assert.assertEquals( "TEST", echo.getContent());
	}


	@Test
	public void testSendPingMessageQueue_messagingException() throws Exception {

		Mockito.doThrow( new IOException( "for test" ) ).when( this.messagingMngr ).sendMessageToTheDm( Mockito.any( Message.class  ));
		Mockito.verifyZeroInteractions( this.messagingMngr );
		Assert.assertFalse( this.mngr.pingMessageQueue( "TEST" ));
		Mockito.verify( this.messagingMngr, Mockito.times( 1 )).sendMessageToTheDm( Mockito.any( Message.class ));
	}


	@Test
	public void testSendPingAgent_normal_rootsAreDeployed() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app );
		Mockito.verifyZeroInteractions( this.messagingMngr );

		// Ping all the root instances.
		// None of them is not deployed.
		app.getMySqlVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		app.getTomcatVm().setStatus( InstanceStatus.PROBLEM );

		for( Instance i : app.getRootInstances())
			Assert.assertEquals( 0, this.mngr.pingAgent( ma, i, "TEST " + i.getName()));

		// Now check the DM has sent a ping to every agent.
		int inv = app.getRootInstances().size();
		ArgumentCaptor<Message> argMsg = ArgumentCaptor.forClass( Message.class );
		ArgumentCaptor<ManagedApplication> argApp = ArgumentCaptor.forClass( ManagedApplication.class );
		Mockito.verify( this.messagingMngr, Mockito.times( inv )).sendMessageDirectly(
				argApp.capture(),
				Mockito.any( Instance.class ),
				argMsg.capture());

		List<Message> sentMessages = argMsg.getAllValues();
		Assert.assertEquals( app.getRootInstances().size(), sentMessages.size());
		int index = 0;
		for( Instance i : app.getRootInstances()) {
			Message message = sentMessages.get( index++ );
			Assert.assertTrue( message instanceof MsgEcho );
			MsgEcho echo = (MsgEcho) message;
			Assert.assertEquals( "PING:TEST " + i.getName(), echo.getContent());
		}

		for( ManagedApplication t : argApp.getAllValues()) {
			Assert.assertEquals( ma, t );
		}
	}


	@Test
	public void testSendPingAgent_normal_rootsAreNotDeployed() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app );
		Mockito.verifyZeroInteractions( this.messagingMngr );

		// Ping all the root instances.
		for (Instance i : app.getRootInstances()) {
			i.setStatus( InstanceStatus.NOT_DEPLOYED );
			Assert.assertEquals( 1, this.mngr.pingAgent( ma, i, "TEST " + i.getName()));
		}

		// Now check the DM has tried to send a ping to every agent.
		Mockito.verify( this.messagingMngr, Mockito.times( 0 )).sendMessageDirectly(
				Mockito.any( ManagedApplication.class ),
				Mockito.any( Instance.class ),
				Mockito.any( Message.class ));
	}


	@Test
	public void testSendPingAgent_normal_rootsAreDeployed_messagingError() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app );
		Mockito.verifyZeroInteractions( this.messagingMngr );

		Mockito.doThrow( new IOException( "for test" ) ).when( this.messagingMngr ).sendMessageDirectly(
				Mockito.any( ManagedApplication.class ),
				Mockito.any( Instance.class ),
				Mockito.any( Message.class ));

		// Ping all the root instances.
		for( Instance i : app.getRootInstances()) {
			i.setStatus( InstanceStatus.DEPLOYED_STARTED );
			Assert.assertEquals( 2, this.mngr.pingAgent( ma, i, "TEST " + i.getName()));
		}

		// Now check the DM has tried to send a ping to every agent.
		int inv = app.getRootInstances().size();
		Mockito.verify( this.messagingMngr, Mockito.times( inv )).sendMessageDirectly(
				Mockito.any( ManagedApplication.class ),
				Mockito.any( Instance.class ),
				Mockito.any( Message.class ));
	}
}
