/**
 * Copyright 2015 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.target.in_memory;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Timer;

import junit.framework.Assert;
import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.dm.internal.test.TestManagerWrapper;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.messaging.api.MessagingConstants;
import net.roboconf.messaging.api.factory.MessagingClientFactoryRegistry;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.Factory;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

/**
 * @author Vincent Zurczak - Linagora
 */
public class InMemoryHandler_realManagerTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private Map<String,String> msgCfg = new LinkedHashMap<> ();
	private Manager manager;
	private Factory agentFactory;
	private InMemoryHandler handler;
	private TestApplication app;


	@Before
	public void before() throws Exception {

		// Messaging configuration
		this.msgCfg = new LinkedHashMap<>();
		this.msgCfg.put("net.roboconf.messaging.type", "telepathy");
		this.msgCfg.put("mindControl", "false");
		this.msgCfg.put("psychosisProtection", "active");

		// Prepare the handler
		this.agentFactory = Mockito.mock( Factory.class );
		this.manager = new Manager();

		this.handler = new InMemoryHandler();
		this.handler.setMessagingFactoryRegistry( new MessagingClientFactoryRegistry());

		this.handler.agentFactory = this.agentFactory;
		this.handler.manager = this.manager;

		// Configure the manager
		this.manager.setMessagingType( MessagingConstants.TEST_FACTORY_TYPE );
		this.manager.configurationMngr().setWorkingDirectory( this.folder.newFolder());
		this.manager.start();

		TestManagerWrapper managerWrapper = new TestManagerWrapper( this.manager );
		managerWrapper.configureMessagingForTest();
		this.manager.reconfigure();

		// Disable the messages timer for predictability
		TestUtils.getInternalField( this.manager, "timer", Timer.class).cancel();

		// Load an application
		this.app = new TestApplication();
		this.app.setDirectory( this.folder.newFolder());

		ManagedApplication ma = new ManagedApplication( this.app );
		managerWrapper.getNameToManagedApplication().put( this.app.getName(), ma );
	}


	@After
	public void after() {
		this.manager.stop();
	}


	@Test
	public void testMachineIsRunning_noSimulation_notRunning() throws Exception {

		// Configure the mocks
		Mockito.when( this.agentFactory.getInstancesNames()).thenReturn( new ArrayList<String>( 0 ));

		// Prepare...
		Map<String,String> targetProperties = new HashMap<> ();
		targetProperties.put( InMemoryHandler.EXECUTE_REAL_RECIPES, "true" );

		this.app.getTomcatVm().setStatus( InstanceStatus.NOT_DEPLOYED );
		String machineId = InstanceHelpers.computeInstancePath( this.app.getTomcatVm()) + " @ " + this.app.getName();

		// Run and verify
		Assert.assertFalse( this.handler.isMachineRunning( targetProperties, machineId ));
		Mockito.verify( this.agentFactory, Mockito.times( 0 )).createComponentInstance( Mockito.any( Dictionary.class ));
	}


	@Test
	public void testMachineIsRunning_noSimulation_deployedAndStarted() throws Exception {

		// Configure the mocks
		ComponentInstance componentInstance = Mockito.mock( ComponentInstance.class );
		Mockito.when( this.agentFactory.createComponentInstance( Mockito.any( Dictionary.class ))).thenReturn( componentInstance );
		Mockito.when( this.agentFactory.getInstancesNames()).thenReturn( new ArrayList<String>( 0 ));

		// Prepare
		Map<String,String> targetProperties = new HashMap<> ();
		targetProperties.put( InMemoryHandler.EXECUTE_REAL_RECIPES, "true" );

		this.app.getTomcatVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		String machineId = InstanceHelpers.computeInstancePath( this.app.getTomcatVm()) + " @ " + this.app.getName();

		// Run and verify
		Assert.assertTrue( this.handler.isMachineRunning( targetProperties, machineId ));
		Mockito.verify( this.agentFactory, Mockito.times( 1 )).createComponentInstance( Mockito.any( Dictionary.class ));
		Mockito.verify( componentInstance, Mockito.times( 1 )).start();
	}
}
