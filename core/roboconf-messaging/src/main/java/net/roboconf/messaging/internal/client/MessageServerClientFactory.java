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

package net.roboconf.messaging.internal.client;

import net.roboconf.messaging.MessagingConstants;
import net.roboconf.messaging.client.IAgentClient;
import net.roboconf.messaging.client.IDmClient;
import net.roboconf.messaging.internal.client.rabbitmq.RabbitMqClientAgent;
import net.roboconf.messaging.internal.client.rabbitmq.RabbitMqClientDm;
import net.roboconf.messaging.internal.client.test.TestClientAgent;
import net.roboconf.messaging.internal.client.test.TestClientDm;

/**
 * @author Vincent Zurczak - Linagora
 */
public class MessageServerClientFactory {

	/**
	 * @param factoryName the factory name (one of this class's constants)
	 * @return a new instance of message server client, null if the factory name is unknown
	 */
	public IDmClient createDmClient( String factoryName ) {

		IDmClient result = null;
		if( MessagingConstants.FACTORY_RABBIT_MQ.equalsIgnoreCase( factoryName ))
			result = new RabbitMqClientDm();
		else if( MessagingConstants.FACTORY_TEST.equals( factoryName ))
			result = new TestClientDm();

		return result;
	}


	/**
	 * @param factoryName the factory name (one of this class's constants)
	 * @return a new instance of message server client, null if the factory name is unknown
	 */
	public IAgentClient createAgentClient( String factoryName ) {

		IAgentClient result = null;
		if( MessagingConstants.FACTORY_RABBIT_MQ.equalsIgnoreCase( factoryName ))
			result = new RabbitMqClientAgent();
		else if( MessagingConstants.FACTORY_TEST.equals( factoryName ))
			result = new TestClientAgent();

		return result;
	}
}
