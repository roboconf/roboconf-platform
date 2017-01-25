/**
 * Copyright 2015-2017 Linagora, Université Joseph Fourier, Floralis
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

import org.junit.Before;
import org.junit.Test;

import net.roboconf.messaging.api.MessagingConstants;
import net.roboconf.messaging.api.factory.MessagingClientFactoryRegistry;
import net.roboconf.messaging.api.internal.client.AbstractMessagingTest;

/**
 * @author Vincent Zurczak - Linagora
 */
public class InMemoryMessagingTest extends AbstractMessagingTest {


	@Before
	public void registerRabbitMqFactory() {

		this.registry = new MessagingClientFactoryRegistry();
		this.registry.addMessagingClientFactory( new InMemoryClientFactory());
	}


	@Override
	protected long getDelay() {
		return 10;
	}


	@Test
	@Override
	public void testDmDebug() throws Exception {
		super.testDmDebug();
	}


	@Test
	@Override
	public void testExchangesBetweenTheDmAndOneAgent() throws Exception {
		super.testExchangesBetweenTheDmAndOneAgent();
	}


	@Test
	@Override
	public void testExchangesBetweenTheDmAndThreeAgents() throws Exception {
		super.testExchangesBetweenTheDmAndThreeAgents();
	}


	@Test
	@Override
	public void testExportsBetweenAgents() throws Exception {
		super.testExportsBetweenAgents();
	}


	@Test
	@Override
	public void testExportsBetweenSiblingAgents() throws Exception {
		super.testExportsBetweenSiblingAgents();
	}


	@Test
	@Override
	public void testExportsRequestsBetweenAgents() throws Exception {
		super.testExportsRequestsBetweenAgents();
	}


	@Test
	@Override
	public void testExternalExports_withTwoApplications() throws Exception {
		super.testExternalExports_withTwoApplications();
	}


	@Test
	@Override
	public void testPropagateAgentTermination() throws Exception {
		super.testPropagateAgentTermination();
	}


	@Test
	@Override
	public void testExternalExports_twoApplicationsAndTheDm_verifyAgentTerminationPropagation()
	throws Exception {
		super.testExternalExports_twoApplicationsAndTheDm_verifyAgentTerminationPropagation();
	}


	@Test
	@Override
	public void test_applicationRegeneration() throws Exception {
		super.test_applicationRegeneration();
	}


	@Override
	protected String getMessagingType() {
		return MessagingConstants.FACTORY_IN_MEMORY;
	}
}
