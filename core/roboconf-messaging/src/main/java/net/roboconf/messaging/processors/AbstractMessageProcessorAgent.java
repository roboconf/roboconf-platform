/**
 * Copyright 2014 Linagora, Université Joseph Fourier
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

package net.roboconf.messaging.processors;

import java.io.IOException;

import net.roboconf.messaging.client.IAgentClient;
import net.roboconf.messaging.internal.client.MessageServerClientFactory;

/**
 * @author Vincent Zurczak - Linagora
 */
public abstract class AbstractMessageProcessorAgent extends AbstractMessageProcessor<IAgentClient> {

	private final String messagingFactoryName;


	/**
	 * Constructor.
	 * @param messagingFactoryName the message factory name
	 * @See {@link MessageServerClientFactory}
	 */
	public AbstractMessageProcessorAgent( String messagingFactoryName ) {
		super( "Roboconf Agent - Message Processor" );
		this.messagingFactoryName = messagingFactoryName;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.messaging.processors.AbstractMessageProcessor
	 * #createNewMessagingClient(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	protected final IAgentClient createNewMessagingClient( String messageServerIp, String messageServerUser, String messageServerPwd )
	throws IOException {

		MessageServerClientFactory factory = new MessageServerClientFactory();
		IAgentClient client = factory.createAgentClient( this.messagingFactoryName );
		client.setParameters( messageServerIp, messageServerUser, messageServerPwd );

		return client;
	}
}