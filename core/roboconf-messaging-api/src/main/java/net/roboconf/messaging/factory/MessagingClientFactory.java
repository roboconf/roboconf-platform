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

package net.roboconf.messaging.factory;

import net.roboconf.messaging.client.IAgentClient;
import net.roboconf.messaging.client.IClient;
import net.roboconf.messaging.client.IDmClient;

/**
 * A service that allows to create Roboconf messaging clients.
 *
 * @author Pierre Bourret - Université Joseph Fourier
 */
public interface MessagingClientFactory {

	/**
	 * When exposed as a service, this property indicates the type of messaging this factory provides.
	 * <p>
	 * This property is <em>mandatory</em>, its value is <em>immutable</em> and must be an non-null {@code String}.
	 * </p>
	 * @see #getType()
	 */
	String MESSAGING_TYPE_PROPERTY = IClient.MESSAGING_TYPE_PROPERTY;

	/**
	 * Get the type of messaging this factory supports.
	 * @return the type of messaging this factory supports.
	 * @see #MESSAGING_TYPE_PROPERTY
	 */
	String getType();

	/**
	 * Creates a messaging client for the DM.
	 * @return the created DM messaging client.
	 */
	IDmClient createDmClient();


	/**
	 * Creates a messaging client for an agent.
	 * @return the created agent messaging client.
	 */
	IAgentClient createAgentClient();

}