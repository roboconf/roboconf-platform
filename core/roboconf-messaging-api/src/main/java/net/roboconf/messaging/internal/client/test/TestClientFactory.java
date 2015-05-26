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

package net.roboconf.messaging.internal.client.test;

import net.roboconf.messaging.MessagingConstants;
import net.roboconf.messaging.client.IAgentClient;
import net.roboconf.messaging.client.IDmClient;
import net.roboconf.messaging.factory.MessagingClientFactory;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.ServiceProperty;

/**
 * Messaging client factory for tests.
 *
 * @author Pierre Bourret - Université Joseph Fourier
 */
@Component(name = "roboconf-messaging-client-factory-test", publicFactory = false)
@Provides(specifications = MessagingClientFactory.class)
@Instantiate(name = "Roboconf Test Messaging Client Factory")
public class TestClientFactory implements MessagingClientFactory {

	@ServiceProperty(name = MessagingClientFactory.MESSAGING_TYPE_PROPERTY)
	private final String type = MessagingConstants.FACTORY_TEST;

	@Override
	public String getType() {
		return this.type;
	}

	@Override
	public IDmClient createDmClient() {
		return new TestClientDm();
	}

	@Override
	public IAgentClient createAgentClient() {
		return new TestClientAgent();
	}
}