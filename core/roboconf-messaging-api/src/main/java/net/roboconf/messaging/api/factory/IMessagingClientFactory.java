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

package net.roboconf.messaging.api.factory;

import java.util.Map;

import net.roboconf.messaging.api.MessagingConstants;
import net.roboconf.messaging.api.client.IAgentClient;
import net.roboconf.messaging.api.client.IDmClient;
import net.roboconf.messaging.api.reconfigurables.ReconfigurableClientAgent;
import net.roboconf.messaging.api.reconfigurables.ReconfigurableClientDm;

/**
 * A service that allows to create Roboconf messaging clients.
 * @author Pierre Bourret - Université Joseph Fourier
 */
public interface IMessagingClientFactory {

	/**
	 * Service property indicating the type of messaging this factory provides.
	 * <p>
	 * This property is <em>mandatory</em>, its value is <em>immutable</em> and must be an non-null {@code String}.
	 * </p>
	 * @see #getType()
	 * @see MessagingConstants#MESSAGING_TYPE_PROPERTY
	 */
	String MESSAGING_TYPE_PROPERTY = MessagingConstants.MESSAGING_TYPE_PROPERTY;

	/**
	 * Get the type of messaging this factory supports.
	 * @return the type of messaging this factory supports.
	 * @see #MESSAGING_TYPE_PROPERTY
	 */
	String getType();

	/**
	 * Creates a messaging client for the DM.
	 * @return the created DM messaging client.
	 * @param parent the parent client.
	 */
	IDmClient createDmClient( ReconfigurableClientDm parent );


	/**
	 * Creates a messaging client for an agent.
	 * @return the created agent messaging client.
	 * @param parent the parent client.
	 */
	IAgentClient createAgentClient( ReconfigurableClientAgent parent );

	/**
	 * Attempts to apply the given provider-specific messaging configuration to this factory.
	 * @param configuration the configuration to apply.
	 * @return {@code true} if the configuration has been successfully applied, {@code false} otherwise.
	 */
	boolean setConfiguration(Map<String, String> configuration);
}
