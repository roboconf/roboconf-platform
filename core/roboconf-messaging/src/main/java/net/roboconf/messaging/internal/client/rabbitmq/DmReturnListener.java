/**
 * Copyright 2014 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.messaging.internal.client.rabbitmq;

import java.io.IOException;
import java.util.logging.Logger;

import net.roboconf.core.utils.Utils;
import net.roboconf.messaging.internal.utils.SerializationUtils;
import net.roboconf.messaging.messages.Message;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.ReturnListener;

/**
 * @author Vincent Zurczak - Linagora
 */
public class DmReturnListener implements ReturnListener {

	private final Logger logger = Logger.getLogger( getClass().getName());


	/* (non-Javadoc)
	 * @see com.rabbitmq.client.ReturnListener
	 * #handleReturn(int, java.lang.String, java.lang.String, java.lang.String, com.rabbitmq.client.AMQP.BasicProperties, byte[])
	 */
	@Override
	public void handleReturn(
			int replyCode,
			String replyText,
			String exchange,
			String routingKey,
			BasicProperties properties,
			byte[] body )
	throws IOException {

		String messageType = "undetermined";
		try {
			Message msg = SerializationUtils.deserializeObject( body );
			messageType = msg.getClass().getName();

		} catch( ClassNotFoundException e ) {
			this.logger.severe( "Failed to deserialize a message object." );
			this.logger.finest( Utils.writeException( e ));
		}

		StringBuilder sb = new StringBuilder();
		sb.append( "A message sent by the DM was not received by any agent queue." );
		sb.append( "\nMessage type: " + messageType );
		sb.append( "\nRouting key: " + routingKey );
		sb.append( "\nReason: " + replyText );

		this.logger.warning( sb.toString());
	}
}
