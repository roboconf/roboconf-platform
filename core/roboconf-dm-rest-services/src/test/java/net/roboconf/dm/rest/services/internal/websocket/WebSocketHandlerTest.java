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

package net.roboconf.dm.rest.services.internal.websocket;

import java.io.IOException;

import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.api.Session;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.core.model.beans.Component;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.runtime.EventType;

/**
 * @author Vincent Zurczak - Linagora
 */
public class WebSocketHandlerTest {

	private Session session;
	private RemoteEndpoint remoteEndpoint;


	@After
	@Before
	public void resetSessions() {

		for( Session session : WebSocketHandler.getSessions())
			WebSocketHandler.removeSession( session );
	}


	@Test
	public void testSessionsManagement() {

		Assert.assertEquals( 0, WebSocketHandler.getSessions().size());

		Session session1 = Mockito.mock( Session.class );
		WebSocketHandler.addSession( session1 );
		Assert.assertEquals( 1, WebSocketHandler.getSessions().size());
		Assert.assertEquals( session1, WebSocketHandler.getSessions().iterator().next());

		Session session2 = Mockito.mock( Session.class );
		WebSocketHandler.addSession( session2 );
		Assert.assertEquals( 2, WebSocketHandler.getSessions().size());

		WebSocketHandler.removeSession( session1 );
		Assert.assertEquals( 1, WebSocketHandler.getSessions().size());
		Assert.assertEquals( session2, WebSocketHandler.getSessions().iterator().next());

		WebSocketHandler.removeSession( session2 );
		Assert.assertEquals( 0, WebSocketHandler.getSessions().size());
	}


	@Test
	public void testNotificationsForApplication() throws Exception {

		ApplicationTemplate template = new ApplicationTemplate( "test-tpl" );
		Application app = new Application( "test", template );

		WebSocketHandler handler = configuredHandler();
		handler.enableNotifications();
		handler.application( app, EventType.CREATED );
		Mockito.verify( this.remoteEndpoint )
				.sendString( "{\"event\":\"CREATED\",\"app\":{\"name\":\"test\",\"displayName\":\"test\",\"tplName\":\"test-tpl\"}}" );
	}


	@Test
	public void testNotificationsForApplicationTemplate() throws Exception {

		ApplicationTemplate template = new ApplicationTemplate( "test-tpl" );

		WebSocketHandler handler = configuredHandler();
		handler.enableNotifications();
		handler.applicationTemplate( template, EventType.DELETED );
		Mockito.verify( this.remoteEndpoint )
				.sendString( "{\"event\":\"DELETED\",\"tpl\":{\"name\":\"test-tpl\",\"displayName\":\"test-tpl\",\"apps\":[]}}" );
	}


	@Test
	public void testNotificationsForInstance() throws Exception {

		ApplicationTemplate template = new ApplicationTemplate( "test-tpl" );
		Application app = new Application( "test", template );
		Instance inst = new Instance( "inst" ).component( new Component( "comp" ));

		WebSocketHandler handler = configuredHandler();
		Assert.assertNotNull( handler.getId());

		handler.enableNotifications();
		handler.instance( inst, app, EventType.CHANGED );

		String expected =
				"{\"event\":\"CHANGED\",\"app\":{\"name\":\"test\",\"displayName\":\"test\",\"tplName\":\"test-tpl\"},\"inst\":"
				+ "{\"name\":\"inst\",\"path\":\"/inst\",\"status\":\"NOT_DEPLOYED\",\"component\":{\"name\":\"comp\"}}}";

		Mockito.verify( this.remoteEndpoint ).sendString( expected );
	}


	@Test
	public void testRawNotifications() throws Exception {

		WebSocketHandler handler = configuredHandler();
		handler.enableNotifications();
		handler.raw( "this is a raw notification" );
		Mockito.verify( this.remoteEndpoint ).sendString( "{\"msg\":\"this is a raw notification\"}" );
	}


	@Test
	public void testRawNotification_withNullMessage() throws Exception {

		WebSocketHandler handler = configuredHandler();
		handler.enableNotifications();
		handler.raw( null );
		Mockito.verify( this.remoteEndpoint, Mockito.never()).sendString( Mockito.anyString());
	}


	@Test
	public void testNotifications_whenDisabled() throws Exception {

		WebSocketHandler handler = configuredHandler();
		handler.disableNotifications();
		handler.raw( "this is a raw notification" );
		Mockito.verify( this.remoteEndpoint, Mockito.never()).sendString( Mockito.anyString());
	}


	@Test
	public void testNotifications_sendException_1() throws Exception {

		WebSocketHandler handler = configuredHandler();
		handler.enableNotifications();

		Mockito.doThrow( new IOException()).when( this.remoteEndpoint ).sendString( Mockito.anyString());
		handler.raw( "this is another raw notification" );
		Mockito.verify( this.remoteEndpoint ).sendString( "{\"msg\":\"this is another raw notification\"}" );
	}


	@Test
	public void testNotifications_sendException_2() throws Exception {

		WebSocketHandler handler = configuredHandler();
		handler.enableNotifications();

		Mockito.doThrow( new IOException( "some reason" )).when( this.remoteEndpoint ).sendString( Mockito.anyString());
		handler.raw( "this is another raw notification" );
		Mockito.verify( this.remoteEndpoint ).sendString( "{\"msg\":\"this is another raw notification\"}" );
	}


	private WebSocketHandler configuredHandler() {

		this.session = Mockito.mock( Session.class );
		this.remoteEndpoint = Mockito.mock( RemoteEndpoint.class );
		Mockito.when( this.session.getRemote()).thenReturn( this.remoteEndpoint );

		WebSocketHandler.addSession( this.session );
		WebSocketHandler handler = new WebSocketHandler();

		return handler;
	}
}
