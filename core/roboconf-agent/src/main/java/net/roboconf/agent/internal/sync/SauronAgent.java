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

import java.io.IOException;
import java.util.logging.Logger;

import net.roboconf.agent.AgentCoordinator;
import net.roboconf.agent.internal.Agent;
import net.roboconf.agent.internal.AgentMessageProcessor;
import net.roboconf.messaging.api.client.idle.IdleClient;
import net.roboconf.messaging.api.extensions.IMessagingClient;
import net.roboconf.messaging.api.messages.Message;
import net.roboconf.messaging.api.reconfigurables.ReconfigurableClientAgent;

/**
 * A super agent to prevent chaos.
 * <p>
 * When multiple agents run in memory and execute real recipes, there may
 * be race conditions on system resources. To prevent random failures, we
 * introduce Sauron and Nazgul agents. Instead of launching a real agent,
 * we run a Nazgul one. When such an agent receives a message to process,
 * it forwards it to Sauron that will decide when to process it.
 * </p>
 * <p>
 * When a message is ready to be processed, Sauron sends it back to the
 * right Nazgul. Sauron is only here to make concurrent processing of
 * messages a simple flow.
 * </p>
 * <p>
 * This agent does not need to send or receive messages. So, we replace the
 * way it creates messaging clients.
 * </p>
 * <p>
 * There must be only one Sauron, that manages agents across all the
 * applications. This is really for local tests.
 * </p>
 *
 * @author Vincent Zurczak - Linagora
 */
public class SauronAgent extends Agent implements AgentCoordinator {

	private SauronMessageProcessor processor;


	@Override
	protected AgentMessageProcessor newMessageProcessor() {
		Logger.getLogger( getClass().getName()).finer( "Sauron is starting..." );
		this.processor = new SauronMessageProcessor( this );
		return this.processor;
	}

	/**
	 * @param message
	 */
	@Override
	public void processMessageInSequence( Message message ) {
		if( this.processor != null )
			this.processor.storeMessage( message );
	}


	@Override
	protected ReconfigurableClientAgent newReconfigurableClientAgent() {
		return new SauronAgentClient();
	}


	@Override
	public String getAgentId() {
		return "Sauron";
	}


	// To keep since iPojo cannot introspect super classes!
	@Override
	public void reconfigure() {
		super.reconfigure();
	}


	/**
	 * @author Vincent Zurczak - Linagora
	 */
	static class SauronAgentClient extends ReconfigurableClientAgent {

		@Override
		protected IMessagingClient createMessagingClient( String factoryName )
		throws IOException {
			return new IdleClient();
		}
	}
}
