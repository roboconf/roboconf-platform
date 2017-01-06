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

import org.junit.Assert;

import org.eclipse.jetty.websocket.api.Session;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * @author Vincent Zurczak - Linagora
 */
public class RoboconfWebSocketTest {

	@Test
	public void testUnusedMethods() {

		// Mainly for code coverage...
		RoboconfWebSocket ws = new RoboconfWebSocket();
		ws.session = Mockito.mock( Session.class );

		ws.onWebSocketError( new Exception());
		ws.onWebSocketText( "oops" );
		ws.onWebSocketBinary( new byte[ 0 ], 0, 0 );
	}


	@Test
	public void testSessionsManagement() {

		Assert.assertEquals( 0, WebSocketHandler.getSessions().size());

		RoboconfWebSocket ws1 = new RoboconfWebSocket();
		Session session1 = Mockito.mock( Session.class );
		ws1.onWebSocketConnect( session1 );
		Assert.assertEquals( 1, WebSocketHandler.getSessions().size());
		Assert.assertEquals( session1, WebSocketHandler.getSessions().iterator().next());

		RoboconfWebSocket ws2 = new RoboconfWebSocket();
		Session session2 = Mockito.mock( Session.class );
		ws2.onWebSocketConnect( session2 );
		Assert.assertEquals( 2, WebSocketHandler.getSessions().size());

		ws1.onWebSocketClose( 0, "whatever" );
		Assert.assertEquals( 1, WebSocketHandler.getSessions().size());
		Assert.assertEquals( session2, WebSocketHandler.getSessions().iterator().next());

		ws2.onWebSocketClose( 1, "whatever" );
		Assert.assertEquals( 0, WebSocketHandler.getSessions().size());
	}
}
