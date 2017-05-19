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

package net.roboconf.messaging.api.internal.client.in_memory;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import net.roboconf.messaging.api.MessagingConstants;
import net.roboconf.messaging.api.extensions.AbstractRoutingClient;
import net.roboconf.messaging.api.extensions.MessagingContext.RecipientKind;
import net.roboconf.messaging.api.jmx.RoboconfMessageQueue;
import net.roboconf.messaging.api.messages.Message;

/**
 * A class to dispatch messages directly into message queues.
 * <p>
 * This solution only works when the DM and ALL the agents run in the same JVM.
 * So, it should only work with in-memory agents, and maybe with locally "embedded" agents.
 * </p>
 *
 * @author Vincent Zurczak - Linagora
 */
public class InMemoryClient extends AbstractRoutingClient<LinkedBlockingQueue<Message>> {

	/**
	 * @author Vincent Zurczak - Linagora
	 */
	public static class InMemoryRoutingContext extends RoutingContext {
		public final Map<String,LinkedBlockingQueue<Message>> ctxToQueue = new ConcurrentHashMap<> ();
	}

	// Internal field (for a convenient access).
	private final Map<String,LinkedBlockingQueue<Message>> ctxToQueue;


	/**
	 * Constructor.
	 * @param routingContext
	 * @param ownerKind
	 */
	public InMemoryClient( InMemoryRoutingContext routingContext, RecipientKind ownerKind ) {
		super( routingContext, ownerKind );
		this.ctxToQueue = routingContext.ctxToQueue;
	}


	@Override
	public void setMessageQueue( RoboconfMessageQueue messageQueue ) {
		this.ctxToQueue.put( this.ownerId, messageQueue );
	}


	@Override
	protected Map<String,LinkedBlockingQueue<Message>> getStaticContextToObject() {
		return this.ctxToQueue;
	}


	@Override
	public String getMessagingType() {
		return MessagingConstants.FACTORY_IN_MEMORY;
	}


	@Override
	protected void process( LinkedBlockingQueue<Message> queue, Message message ) throws IOException {
		queue.add( message );
	}
}
