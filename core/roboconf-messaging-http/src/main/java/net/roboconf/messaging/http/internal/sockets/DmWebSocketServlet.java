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

package net.roboconf.messaging.http.internal.sockets;

import net.roboconf.messaging.http.internal.HttpClientFactory;

import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

/**
 * @author Pierre-Yves Gibello - Linagora
 */
public class DmWebSocketServlet extends WebSocketServlet {

	private static final long serialVersionUID = 1359420439902184795L;
	private final transient HttpClientFactory httpClientFactory;


	/**
	 * Constructor.
	 * @param httpClientFactory
	 */
	public DmWebSocketServlet( HttpClientFactory httpClientFactory ) {
		this.httpClientFactory = httpClientFactory;
	}


	@Override
	public void configure( WebSocketServletFactory factory ) {
		factory.setCreator( new DmWebSocketCreator( this.httpClientFactory ));
	}


	/**
	 * @author Vincent Zurczak - Linagora
	 */
	private static class DmWebSocketCreator implements WebSocketCreator {
		private final HttpClientFactory httpClientFactory;

		/**
		 * Constructor.
		 * @param httpClientFactory
		 */
		public DmWebSocketCreator( HttpClientFactory httpClientFactory ) {
			this.httpClientFactory = httpClientFactory;
		}


		@Override
		public Object createWebSocket( ServletUpgradeRequest req, ServletUpgradeResponse resp ) {
			return new DmWebSocket( this.httpClientFactory );
		}
	}
}
