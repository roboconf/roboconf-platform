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

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import net.roboconf.agent.AgentCoordinator;
import net.roboconf.messaging.api.messages.Message;
import net.roboconf.messaging.api.messages.from_dm_to_dm.MsgEcho;

/**
 * @author Vincent Zurczak - Linagora
 */
public class NazgulMessageProcessorTest {

	@Test
	public void testStoreMessage() {

		NazgulAgent nazgulAgent = Mockito.mock( NazgulAgent.class );
		AgentCoordinator sauron = Mockito.mock( AgentCoordinator.class );
		NazgulMessageProcessor processor = new NazgulMessageProcessor( nazgulAgent, sauron );

		Message msg = new MsgEcho( "oo" );
		Assert.assertEquals( 0, SauronMessageProcessor.THE_SINGLE_MQ.size());
		processor.processMessage( msg );

		Assert.assertEquals( 1, SauronMessageProcessor.THE_SINGLE_MQ.size());
		Assert.assertEquals( processor, SauronMessageProcessor.THE_SINGLE_MQ.get( msg ));
		Mockito.verify( sauron, Mockito.only()).processMessageInSequence( msg );
	}


	@Test
	public void testGetAgentId() {

		NazgulAgent nazgulAgent = Mockito.mock( NazgulAgent.class );
		AgentCoordinator sauron = Mockito.mock( AgentCoordinator.class );
		NazgulMessageProcessor processor = new NazgulMessageProcessor( nazgulAgent, sauron );

		final String name = "toto";
		Mockito.when( nazgulAgent.getAgentId()).thenReturn( name );
		Assert.assertEquals( name, processor.getAgentId());
	}


	@Test
	public void testProcessMessageForReal() {

		NazgulAgent nazgulAgent = Mockito.mock( NazgulAgent.class );
		AgentCoordinator sauron = Mockito.mock( AgentCoordinator.class );
		NazgulMessageProcessor processor = new NazgulMessageProcessor( nazgulAgent, sauron );

		Message msg = Mockito.mock( Message.class );
		processor.processMessageForReal( msg );

		// No error
	}
}
