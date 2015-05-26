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

package net.roboconf.messaging.rabbitmq;

import net.roboconf.messaging.client.IClient;

/**
 * Common RabbitMQ client-related stuffs.
 * @author Pierre Bourret - Université Joseph Fourier
 */
public interface RabbitMqClient extends IClient {

	/**
	 * The default value of the IP address property.
	 * <p>
	 * By default, the client assumes the RabbitMQ server is hosted on {@code localhost}, with the default port.
	 * </p>
	 */
	String DEFAULT_IP = "localhost";

	/**
	 * The default value of the username property.
	 * <p>
	 * By default, the client connects as {@code guest}, the default account only available locally.
	 * </p>
	 */
	String DEFAULT_USERNAME = "guest";

	/**
	 * The default value of the password property.
	 * <p>
	 * By default, the client connects as {@code guest}, the default account only available locally. This property holds
	 * the default password for this default account.
	 * </p>
	 */
	String DEFAULT_PASSWORD = "guest";

	/**
	 * Change the configuration of this RabbitMQ messaging client.
	 * @param messageServerIp the RabbitMQ server IP/hostname.
	 * @param messageServerUsername the username needed to connect to the RabbitMQ server.
	 * @param messageServerPassword the password needed to connect to the RabbitMQ server.
	 */
	void setParameters( String messageServerIp, String messageServerUsername, String messageServerPassword );

}
