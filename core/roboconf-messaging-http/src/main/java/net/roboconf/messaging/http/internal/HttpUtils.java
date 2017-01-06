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

package net.roboconf.messaging.http.internal;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import org.eclipse.jetty.websocket.api.RemoteEndpoint;

import net.roboconf.messaging.api.MessagingConstants;
import net.roboconf.messaging.api.messages.Message;
import net.roboconf.messaging.api.utils.SerializationUtils;
import net.roboconf.messaging.http.HttpConstants;

/**
 * @author Vincent Zurczak - Linagora
 */
public final class HttpUtils {

	/**
	 * Private empty constructor.
	 */
	private HttpUtils() {
		// nothing
	}


	/**
	 * Return a HTTP messaging configuration for the given parameters.
	 * @param agentPort the HTTP server port of the agent.. May be {@code null}.
	 * @return the messaging configuration for the given parameters.
	 */
	public static Map<String,String> httpMessagingConfiguration( String ip, int port ) {

		final Map<String,String> result = new LinkedHashMap<>();
		result.put( MessagingConstants.MESSAGING_TYPE_PROPERTY, HttpConstants.FACTORY_HTTP );
		result.put( HttpConstants.HTTP_SERVER_IP, ip == null ? HttpConstants.DEFAULT_IP : ip );
		result.put( HttpConstants.HTTP_SERVER_PORT, "" + (port <= 0 ? HttpConstants.DEFAULT_PORT : port));

		return result;
	}


	/**
	 * Sends a message asynchronously to a remote end point.
	 * <p>
	 * Asynchronous sending is necessary since we may have several threads that
	 * use the same messaging client. Blocking sending may result in issues such
	 * as #598.
	 * </p>
	 *
	 * @param message the message to send
	 * @param remoteEndpoint the remote end-point
	 * @throws IOException if something went wrong
	 */
	public static void sendAsynchronously( Message message, RemoteEndpoint remoteEndpoint )
			throws IOException {

		Future<Void> fut = null;
		Exception exception = null;
		try {
			byte[] rawData = SerializationUtils.serializeObject( message );
			ByteBuffer data = ByteBuffer.wrap( rawData );
			fut = remoteEndpoint.sendBytesByFuture( data );

			// Wait for completion (timeout)
			fut.get( 8, TimeUnit.SECONDS );

		} catch( ExecutionException | InterruptedException e ) {
			exception = e;

		} catch( TimeoutException e ) {
			exception = e;
			final Logger logger = Logger.getLogger( HttpUtils.class.getName());
			logger.warning( "A message could not be sent to a remote end-point (HTTP messaging). Sending timed out." );

			// FIXME: should we send the message once again?
			if( fut != null )
				fut.cancel(true);

		} finally {
			if( exception != null )
				throw new IOException( exception );
		}
	}
}
