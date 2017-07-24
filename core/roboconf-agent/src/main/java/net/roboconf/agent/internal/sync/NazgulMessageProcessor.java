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

import net.roboconf.agent.AgentCoordinator;
import net.roboconf.agent.internal.AgentMessageProcessor;
import net.roboconf.messaging.api.messages.Message;

/**
 * An agent that forwards all its messages to Sauron.
 * <p>
 * Sauron will tell it when to process it for real.
 * </p>
 *
 * @author Vincent Zurczak - Linagora
 */
public class NazgulMessageProcessor extends AgentMessageProcessor {

	private final AgentCoordinator sauron;


	/**
	 * Constructor.
	 * @param nazgulAgent a Nazgul agent
	 * @param sauron <strong>the</strong> Sauron agent
	 */
	public NazgulMessageProcessor( NazgulAgent nazgulAgent, AgentCoordinator sauron ) {
		super( nazgulAgent );
		this.sauron = sauron;
	}


	@Override
	protected void processMessage( Message message ) {
		// In this order! Map first, queue then.
		SauronMessageProcessor.THE_SINGLE_MQ.put( message, this );
		this.sauron.processMessageInSequence( message );
	}


	public void processMessageForReal( Message message ) {
		super.processMessage( message );
	}


	public String getAgentId() {
		return this.agent.getAgentId();
	}
}
