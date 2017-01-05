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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.dm.internal.environment.messaging.RCDm;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.api.IMessagingMngr;
import net.roboconf.messaging.api.messages.Message;
import net.roboconf.messaging.api.messages.from_dm_to_agent.MsgCmdRemoveInstance;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * @author Vincent Zurczak - Linagora
 */
public class MessagingMngrImplTest {

	private IMessagingMngr mngr;
	private RCDm msgClient;


	@Before
	public void prepareMngr() {

		this.msgClient = Mockito.mock( RCDm.class );
		this.mngr = new MessagingMngrImpl();
		((MessagingMngrImpl) this.mngr).setMessagingClient( this.msgClient );
	}


	@Test
	public void testSendMessageToTheDm_normal() throws Exception {

		Mockito.verifyZeroInteractions( this.msgClient );
		this.mngr.sendMessageToTheDm( new MsgCmdRemoveInstance( "/" ));
		Mockito.verify( this.msgClient, Mockito.times( 1 )).sendMessageToTheDm( Mockito.any( MsgCmdRemoveInstance.class ));
	}


	@Test( expected = IOException.class )
	public void testSendMessageToTheDm_messagingError() throws Exception {

		Mockito.verifyZeroInteractions( this.msgClient );
		Mockito.doThrow( new IOException( "for test" )).when( this.msgClient ).sendMessageToTheDm( Mockito.any( MsgCmdRemoveInstance.class ));
		this.mngr.sendMessageToTheDm( new MsgCmdRemoveInstance( "/" ));
	}


	@Test
	public void testgetMessagingClient() {
		Assert.assertEquals( this.msgClient, this.mngr.getMessagingClient());
	}


	@Test
	public void testSendMessageDirectly_normal() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app );
		Message msg = new MsgCmdRemoveInstance( "/" );

		for( InstanceStatus status : InstanceStatus.values()) {
			Mockito.reset( this.msgClient );
			app.getMySqlVm().setStatus( status );

			Mockito.verifyZeroInteractions( this.msgClient );
			this.mngr.sendMessageDirectly( ma, app.getMySqlVm(), msg );
			Mockito.verify( this.msgClient, Mockito.times( 1 )).sendMessageToAgent(
					Mockito.eq( app ),
					Mockito.eq( app.getMySqlVm()),
					Mockito.eq( msg ));
		}
	}


	@Test( expected = IOException.class )
	public void testSendMessageDirectly_messagingError() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app );
		Message msg = new MsgCmdRemoveInstance( "/" );

		Mockito.doThrow( new IOException( "for test" )).when( this.msgClient ).sendMessageToAgent(
				Mockito.any( Application.class ),
				Mockito.any( Instance.class ),
				Mockito.any( Message.class ));

		Mockito.verifyZeroInteractions( this.msgClient );
		this.mngr.sendMessageDirectly( ma, app.getMySqlVm(), msg );
	}


	@Test
	public void testSendMessageSafely_theMessageIsSentWhenTheMachineIsThere() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app );
		Message msg = new MsgCmdRemoveInstance( "/" );

		app.getMySqlVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		Mockito.when( this.msgClient.isConnected()).thenReturn( true );

		Mockito.verifyZeroInteractions( this.msgClient );
		this.mngr.sendMessageSafely( ma, app.getMySqlVm(), msg );

		Mockito.verify( this.msgClient, Mockito.times( 1 )).isConnected();
		Mockito.verify( this.msgClient, Mockito.times( 1 )).sendMessageToAgent(
				Mockito.eq( app ),
				Mockito.eq( app.getMySqlVm()),
				Mockito.eq( msg ));

		Assert.assertEquals( 0, ma.removeAwaitingMessages( app.getMySqlVm()).size());
	}


	@Test
	public void testSendMessageSafely_theMessageIsSentWhenTheMachineIsThere_butMessagingIsNotReady() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app );
		Message msg = new MsgCmdRemoveInstance( "/" );

		app.getMySqlVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		Mockito.when( this.msgClient.isConnected()).thenReturn( false );

		Mockito.verifyZeroInteractions( this.msgClient );
		this.mngr.sendMessageSafely( ma, app.getMySqlVm(), msg );

		Mockito.verify( this.msgClient, Mockito.times( 1 )).isConnected();
		Mockito.verify( this.msgClient, Mockito.times( 0 )).sendMessageToAgent(
				Mockito.any( Application.class ),
				Mockito.any( Instance.class ),
				Mockito.any( Message.class ));

		List<Message> messages = ma.removeAwaitingMessages( app.getMySqlVm());
		Assert.assertEquals( 1, messages.size());
		Assert.assertEquals( msg, messages.iterator().next());
	}


	@Test
	public void testSendMessageSafely_theMessageIsStoredWhenTheMachineIsNotThere() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app );
		Message msg = new MsgCmdRemoveInstance( "/" );

		List<InstanceStatus> statuses = new ArrayList<>( Arrays.asList( InstanceStatus.values()));
		statuses.remove( InstanceStatus.DEPLOYED_STARTED );
		Mockito.when( this.msgClient.isConnected()).thenReturn( true );

		for( InstanceStatus status : statuses ) {
			Mockito.reset( this.msgClient );
			ma.removeAwaitingMessages( app.getMySqlVm());
			app.getMySqlVm().setStatus( status );

			Mockito.verifyZeroInteractions( this.msgClient );
			this.mngr.sendMessageSafely( ma, app.getMySqlVm(), msg );

			Mockito.verify( this.msgClient, Mockito.times( 1 )).isConnected();
			Mockito.verify( this.msgClient, Mockito.times( 0 )).sendMessageToAgent(
					Mockito.any( Application.class ),
					Mockito.any( Instance.class ),
					Mockito.any( Message.class ));

			List<Message> messages = ma.removeAwaitingMessages( app.getMySqlVm());
			Assert.assertEquals( 1, messages.size());
			Assert.assertEquals( msg, messages.iterator().next());
		}
	}


	@Test
	public void testSendMessageSafely_theMessageIsStoredWhenTheMachineIsThere_andThatTheMessagingFails() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app );
		Message msg = new MsgCmdRemoveInstance( "/" );

		app.getMySqlVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		Mockito.when( this.msgClient.isConnected()).thenReturn( true );

		Mockito.doThrow( new IOException( "for test" )).when( this.msgClient ).sendMessageToAgent(
				Mockito.any( Application.class ),
				Mockito.any( Instance.class ),
				Mockito.any( Message.class ));

		Mockito.verifyZeroInteractions( this.msgClient );
		this.mngr.sendMessageSafely( ma, app.getMySqlVm(), msg );

		Mockito.verify( this.msgClient, Mockito.times( 1 )).isConnected();
		Mockito.verify( this.msgClient, Mockito.times( 1 )).sendMessageToAgent(
				Mockito.eq( app ),
				Mockito.eq( app.getMySqlVm()),
				Mockito.eq( msg ));

		List<Message> messages = ma.removeAwaitingMessages( app.getMySqlVm());
		Assert.assertEquals( 1, messages.size());
		Assert.assertEquals( msg, messages.iterator().next());
	}


	@Test
	public void testSendStoredMessages_NoMessageToSend() throws Exception {

		// sendStoredMessages is widely used in other tests.
		// We just complete the cases that are not covered.

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app );

		app.getMySqlVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		Mockito.when( this.msgClient.isConnected()).thenReturn( true );

		Mockito.verifyZeroInteractions( this.msgClient );
		this.mngr.sendStoredMessages( ma, app.getMySqlVm());

		Mockito.verify( this.msgClient, Mockito.times( 1 )).isConnected();
		Mockito.verify( this.msgClient, Mockito.times( 0 )).sendMessageToAgent(
				Mockito.any( Application.class ),
				Mockito.any( Instance.class ),
				Mockito.any( Message.class ));
	}


	@Test( expected = IOException.class )
	public void testCheckMessagingConfiguration_noConfiguration() throws Exception {

		this.mngr = new MessagingMngrImpl();
		this.mngr.checkMessagingConfiguration();
	}


	@Test( expected = IOException.class )
	public void testCheckMessagingConfiguration_invalidConfiguration() throws Exception {

		Mockito.when( this.msgClient.hasValidClient()).thenReturn( false );
		this.mngr.checkMessagingConfiguration();
	}


	@Test
	public void testCheckMessagingConfiguration() throws Exception {

		Mockito.when( this.msgClient.hasValidClient()).thenReturn( true );
		this.mngr.checkMessagingConfiguration();
		// No exception
	}
}
