/**
 * Copyright 2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.agent.internal.sync;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import net.roboconf.agent.internal.AgentMessageProcessor;
import net.roboconf.messaging.api.messages.Message;

/**
 * @author Vincent Zurczak - Linagora
 */
public class SauronMessageProcessor extends AgentMessageProcessor {

	static final Map<Message,NazgulMessageProcessor> THE_SINGLE_MQ = new ConcurrentHashMap<> ();
	private final Logger logger = Logger.getLogger( getClass().getName());


	/**
	 * Constructor.
	 * @param sauronAgent a Sauron agent
	 */
	public SauronMessageProcessor( SauronAgent sauronAgent ) {
		super( sauronAgent );
	}


	@Override
	protected void processMessage( Message message ) {

		NazgulMessageProcessor nazgul = THE_SINGLE_MQ.get( message );
		if( nazgul == null ) {
			this.logger.warning( "No Nazgul was found for message " + message.getClass().getSimpleName() + ". Message is dropped." );
		} else {
			this.logger.finer( "Delegating message " + message.getClass().getSimpleName() + " to Nazgul " + nazgul.getAgentId());
			nazgul.processMessageForReal( message );
		}
	}
}
