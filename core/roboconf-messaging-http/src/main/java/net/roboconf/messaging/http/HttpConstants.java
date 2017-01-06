/**
 * Copyright 2014-2017 Linagora, Université Joseph Fourier, Floralis
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

import net.roboconf.messaging.api.MessagingConstants;

/**
 * Constants related to the HTTP messaging client factory.
 * @author Pierre-Yves Gibello - Linagora
 */
public interface HttpConstants {

	/**
	 * The factory type.
	 */
	String FACTORY_HTTP = "http";

	/**
	 * The prefix for HTTP properties.
	 */
	String HTTP_PROPERTY_PREFIX = MessagingConstants.MESSAGING_PROPERTY_PREFIX + "." + FACTORY_HTTP;

	/**
	 * The HTTP port (to create a client).
	 */
	String HTTP_SERVER_PORT = HTTP_PROPERTY_PREFIX + ".server.port";

	/**
	 * The HTTP server IP (to create a client).
	 */
	String HTTP_SERVER_IP = HTTP_PROPERTY_PREFIX + ".server.ip";


	/**
	 * The default IP address.
	 */
	String DEFAULT_IP = "127.0.0.1";

	/**
	 * The default port (Karaf's one).
	 */
	int DEFAULT_PORT = 8181;

	/**
	 * The path of the socket registered by the DM.
	 */
	String DM_SOCKET_PATH = "/roboconf-messaging-http";
}
