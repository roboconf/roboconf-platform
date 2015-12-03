/**
 * Copyright 2014-2015 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.messaging.http;

import java.util.LinkedHashMap;
import java.util.Map;

import net.roboconf.messaging.api.MessagingConstants;

/**
 * Constants related to the HTTP messaging client factory.
 * @author Pierre Bourret - Université Joseph Fourier
 */
public final class HttpConstants {

	// Prevent instantiation.
	private HttpConstants() {
	}

	/**
	 * The factory's name for HTTP clients.
	 */
	public static final String HTTP_FACTORY_TYPE = "http";

	/**
	 * The prefix for all HTTP-related properties.
	 */
	private static final String HTTP_PROPERTY_PREFIX = MessagingConstants.MESSAGING_PROPERTY_PREFIX + "." + HTTP_FACTORY_TYPE;

	/**
	 * Messaging property holding the HTTP port. Defaults to {@code "8080"}.
	 */
	public static final String HTTP_SERVER_PORT = HTTP_PROPERTY_PREFIX + ".server.port";

	/**
	 * Messaging property holding the HTTP server IP. Defaults to {@code "127.0.0.1"}.
	 */
	public static final String HTTP_SERVER_IP = HTTP_PROPERTY_PREFIX + ".server.ip";

	public static final String DEFAULT_IP = "127.0.0.1";
	public static final String DEFAULT_PORT = "8080";

	/**
	 * Return a HTTP messaging configuration for the given parameters.
	 * @param agentPort the HTTP server port of the agent.. May be {@code null}.
	 * @return the messaging configuration for the given parameters.
	 */
	public static Map<String, String> httpMessagingConfiguration(String ip, String port) {
		final Map<String, String> result = new LinkedHashMap<>();
		result.put(MessagingConstants.MESSAGING_TYPE_PROPERTY, HTTP_FACTORY_TYPE);
		result.put(HTTP_SERVER_IP, (ip == null ? DEFAULT_IP : ip));
		result.put(HTTP_SERVER_PORT, (port == null ? "8080" : port));
		return result;
	}
}
