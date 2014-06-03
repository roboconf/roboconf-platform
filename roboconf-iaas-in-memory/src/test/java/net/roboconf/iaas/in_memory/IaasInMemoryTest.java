/**
 * Copyright 2014 Linagora, Université Joseph Fourier
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

package net.roboconf.iaas.in_memory;

import junit.framework.Assert;
import net.roboconf.agent.AgentLauncher;
import net.roboconf.iaas.api.IaasException;
import net.roboconf.iaas.in_memory.internal.utils.AgentManager;

import org.junit.Before;
import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class IaasInMemoryTest {

	@Before
	public void cleanAgentManager() throws Exception {

		for( AgentLauncher launcher : AgentManager.INSTANCE.getMachineIdToAgentLauncher().values())
			launcher.stopAgent();

		AgentManager.INSTANCE.getMachineIdToAgentLauncher().clear();
	}


	@Test
	public void testIaasInMemory() throws Exception {

		Assert.assertEquals( 0, AgentManager.INSTANCE.getMachineIdToAgentLauncher().size());
		IaasInMemory iaas = new IaasInMemory();

		String machineId = iaas.createVM( "machine-image-id", "192.168.1.14", "my root", "my app" );
		Assert.assertNotNull( machineId );
		Assert.assertEquals( 1, AgentManager.INSTANCE.getMachineIdToAgentLauncher().size());

		AgentLauncher launcher = AgentManager.INSTANCE.getMachineIdToAgentLauncher().values().iterator().next();
		Assert.assertEquals( "my app", launcher.getAgentData().getApplicationName());
		Assert.assertEquals( "localhost", launcher.getAgentData().getIpAddress());
		Assert.assertEquals( "192.168.1.14", launcher.getAgentData().getMessageServerIp());
		Assert.assertEquals( "my root", launcher.getAgentData().getRootInstanceName());

		for( int i=0; i<3; i++ ) {
			if( ! launcher.isRunning())
				Thread.sleep( 500 );
		}

		Assert.assertTrue( launcher.isRunning());
		iaas.terminateVM( "invalid-id" );
		Assert.assertEquals( 1, AgentManager.INSTANCE.getMachineIdToAgentLauncher().size());

		iaas.terminateVM( machineId );
		Assert.assertEquals( 0, AgentManager.INSTANCE.getMachineIdToAgentLauncher().size());
		Assert.assertFalse( launcher.isRunning());

		iaas.terminateVM( machineId );
		Assert.assertEquals( 0, AgentManager.INSTANCE.getMachineIdToAgentLauncher().size());
	}


	@Test( expected = IaasException.class )
	public void testDuplicateAgent() throws Exception {

		IaasInMemory iaas = new IaasInMemory();
		iaas.createVM( "machine-image-id", "192.168.1.14", "my root", "my app" );
		iaas.createVM( "machine-image-id", "192.168.1.14", "my root", "my app" );
	}
}
