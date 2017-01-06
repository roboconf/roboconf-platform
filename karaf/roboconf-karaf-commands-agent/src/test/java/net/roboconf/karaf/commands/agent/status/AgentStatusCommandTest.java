/**
 * Copyright 2016-2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.karaf.commands.agent.status;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import net.roboconf.agent.AgentMessagingInterface;

/**
 * @author Amadou Diarra - UGA
 */
public class AgentStatusCommandTest {

	@Test
	public void testExecute_noAgentStatus() throws Exception {

		AgentStatusCommand cmd = new AgentStatusCommand();
		Assert.assertNull( cmd.execute());
	}

	@Test
	public void testExecute_emptyAgentList() throws Exception {

		AgentStatusCommand cmd = new AgentStatusCommand();
		cmd.agents = new ArrayList<>( 0 );
		Assert.assertNull( cmd.execute());
	}

	@Test
	public void testExecute_twoAgentStatus() throws Exception {

		AgentStatusCommand cmd = new AgentStatusCommand();

		AgentMessagingInterface am1 = Mockito.mock( AgentMessagingInterface.class );
		AgentMessagingInterface am2 = Mockito.mock( AgentMessagingInterface.class );
		cmd.agents = Arrays.asList( am1, am2 );

		Assert.assertNull( cmd.execute());
		Mockito.verify( am1, Mockito.only()).agentStatus();
		Mockito.verify( am2, Mockito.only()).agentStatus();
	}

	@Test
	public void testExecute_testException() throws Exception {

		AgentStatusCommand cmd = new AgentStatusCommand();

		AgentMessagingInterface am1 = Mockito.mock( AgentMessagingInterface.class );
		Mockito.doThrow( new RuntimeException( "for test" )).when( am1 ).agentStatus();
		cmd.agents = Arrays.asList( am1 );

		Assert.assertNull( cmd.execute());
		Mockito.verify( am1, Mockito.only()).agentStatus();
	}
}
