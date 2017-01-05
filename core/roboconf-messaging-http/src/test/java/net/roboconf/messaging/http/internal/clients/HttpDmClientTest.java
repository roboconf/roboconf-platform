/**
 * Copyright 2016-2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.messaging.http.internal.clients;

import java.nio.ByteBuffer;
import java.util.concurrent.Future;

import net.roboconf.messaging.api.messages.Message;
import net.roboconf.messaging.http.internal.HttpClientFactory.HttpRoutingContext;

import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.api.Session;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * @author Vincent Zurczak - Linagora
 */
public class HttpDmClientTest {

	@Test
	public void testProcess() throws Exception {

		HttpRoutingContext routingContext = new HttpRoutingContext();
		HttpDmClient httpDmClient = new HttpDmClient( routingContext );

		// Not connected => no processing
		Message message = Mockito.mock( Message.class );
		Session session = Mockito.mock( Session.class );
		Mockito.when( session.isOpen()).thenReturn( false );

		httpDmClient.process( session, message );
		Mockito.verifyZeroInteractions( message );
		Mockito.verify( session, Mockito.only()).isOpen();

		// Connected => processing...
		Mockito.reset( session );
		Future<Void> future = Mockito.mock( Future.class );
		RemoteEndpoint remote = Mockito.mock( RemoteEndpoint.class );
		Mockito.when( remote.sendBytesByFuture( Mockito.any( ByteBuffer.class ) )).thenReturn( future );

		Mockito.when( session.getRemote()).thenReturn( remote );
		Mockito.when( session.isOpen()).thenReturn( true );

		httpDmClient.process( session, message );

		Mockito.verifyZeroInteractions( message );
		Mockito.verify( session, Mockito.times( 1 )).isOpen();
		Mockito.verify( session, Mockito.times( 1 )).getRemote();
		Mockito.verifyNoMoreInteractions( session );
	}
}
