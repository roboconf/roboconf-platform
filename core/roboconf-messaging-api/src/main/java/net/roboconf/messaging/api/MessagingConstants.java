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

package net.roboconf.messaging.api;

/**
 * Messaging related constants.
 * @author Vincent Zurczak - Linagora
 * @author Pierre Bourret - Université Joseph Fourier
 */
public interface MessagingConstants {

	/**
	 * The prefix for all messaging-related properties.
	 * <p>
	 * This property syntax allows target handlers to automatically recognize such properties, and propagate them to
	 * the agents they create.
	 * </p>
	 */
	String MESSAGING_PROPERTY_PREFIX = "net.roboconf.messaging";

	/**
	 * The name of the property which contains the type of messaging.
	 * <p>
	 * The value of this property <em>must</em> be a {@code String}, and may be {@code null} to indicate the messaging
	 * type is left unconfigured.
	 * </p>
	 * <p>
	 * This property is used in the provider-specific
	 * {@linkplain net.roboconf.messaging.api.business.IClient#getConfiguration() messaging configuration} to ensure a
	 * configuration is applicable to a given messaging client. It is also used by
	 * {@link net.roboconf.messaging.api.factory.IMessagingClientFactory} services to indicate which type of messaging
	 * they
	 * support. In the latter case, the property value <em>must</em> be non-{@code null}.
	 * </p>
	 */
	String MESSAGING_TYPE_PROPERTY = MESSAGING_PROPERTY_PREFIX + ".type";

	/**
	 * The factory's name for test clients.
	 */
	String FACTORY_TEST = "test";

	/**
	 * The factory's name for in-memory clients.
	 */
	String FACTORY_IN_MEMORY = "in-memory";

	/**
	 * The factory's name for idle clients.
	 */
	String FACTORY_IDLE = "idle";
}
