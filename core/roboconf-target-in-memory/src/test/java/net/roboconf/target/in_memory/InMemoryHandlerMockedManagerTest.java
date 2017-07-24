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

package net.roboconf.target.in_memory;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.Factory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.management.api.IApplicationMngr;
import net.roboconf.dm.management.api.IInstancesMngr;
import net.roboconf.messaging.api.factory.MessagingClientFactoryRegistry;
import net.roboconf.target.api.TargetHandlerParameters;

/**
 * @author Vincent Zurczak - Linagora
 */
public class InMemoryHandlerMockedManagerTest {

	private Map<String, String> msgCfg = new LinkedHashMap<> ();

	private Manager manager;
	private Factory standardAgentFactory, nazgulAgentFactory;
	private InMemoryHandler handler;


	@Before
	public void setMessagingConfiguration() {

		this.msgCfg = new LinkedHashMap<>();
		this.msgCfg.put("net.roboconf.messaging.type", "telepathy");
		this.msgCfg.put("mindControl", "false");
		this.msgCfg.put("psychosisProtection", "active");

		this.manager = Mockito.mock( Manager.class );
		this.standardAgentFactory = Mockito.mock( Factory.class );
		this.nazgulAgentFactory = Mockito.mock( Factory.class );

		this.handler = new InMemoryHandler();
		this.handler.setMessagingFactoryRegistry( new MessagingClientFactoryRegistry());

		this.handler.standardAgentFactory = this.standardAgentFactory;
		this.handler.nazgulAgentFactory = this.nazgulAgentFactory;
		this.handler.manager = this.manager;
	}


	@Test
	public void testCreateMachine() throws Exception {

		// Configure the mocks
		ComponentInstance componentInstance = Mockito.mock( ComponentInstance.class );
		Mockito.when( this.standardAgentFactory.createComponentInstance( Mockito.any( Dictionary.class ))).thenReturn( componentInstance );

		// Create a machine and verify assertions
		Mockito.verifyZeroInteractions( this.standardAgentFactory );
		Mockito.verifyZeroInteractions( componentInstance );

		Map<String,String> targetProperties = new HashMap<> ();
		targetProperties.put( InMemoryHandler.AGENT_IP_ADDRESS, "127.0.0.1" );

		TargetHandlerParameters parameters = new TargetHandlerParameters()
				.targetProperties( targetProperties )
				.messagingProperties( this.msgCfg )
				.scopedInstancePath( "/VM" )
				.applicationName( "my-app" )
				.domain( "default-domain" );

		String machineId = this.handler.createMachine( parameters );
		Assert.assertEquals( "/VM @ my-app", machineId );
		Mockito.verify( this.standardAgentFactory, Mockito.times( 1 )).createComponentInstance( Mockito.any( Dictionary.class ));
		Mockito.verify( componentInstance, Mockito.times( 1 )).start();
	}


	@Test
	public void testMachineIsRunning() throws Exception {

		List<String> result = new ArrayList<> ();
		Mockito.when( this.standardAgentFactory.getInstancesNames()).thenReturn( result );
		Assert.assertFalse( this.handler.isMachineRunning( new TargetHandlerParameters(), "test" ));

		result.add( "test" );
		Assert.assertTrue( this.handler.isMachineRunning( new TargetHandlerParameters(), "test" ));
	}


	@Test
	public void testTerminateMachine_simulatePlugins_noPojoFound() throws Exception {

		List<ComponentInstance> instances = new ArrayList<> ();
		Mockito.when( this.standardAgentFactory.getInstances()).thenReturn( instances );

		TargetHandlerParameters parameters = new TargetHandlerParameters().targetProperties( new HashMap<String,String>( 0 ));
		this.handler.terminateMachine( parameters, "test @ test" );
		// No error, even if no iPojo instance was found (e.g. if it was killed from the Ipojo console)

		Mockito.verifyZeroInteractions( this.manager );
	}


	@Test
	public void testTerminateMachine_simulatePlugins_noFactory() throws Exception {

		this.handler.standardAgentFactory = null;
		TargetHandlerParameters parameters = new TargetHandlerParameters().targetProperties( new HashMap<String,String>( 0 ));
		this.handler.terminateMachine( parameters, "test @ test" );
		// No error, even if no iPojo instance was found (e.g. if it was killed from the Ipojo console)

		Mockito.verifyZeroInteractions( this.manager );
	}


	@Test
	public void testTerminateMachine_simulatePlugins_noPojoWithTheRightName() throws Exception {

		ComponentInstance componentInstance = Mockito.mock( ComponentInstance.class );
		Mockito.when( componentInstance.getInstanceName()).thenReturn( "NOT test @ test" );

		List<ComponentInstance> instances = new ArrayList<> ();
		instances.add( componentInstance );

		Mockito.when( this.standardAgentFactory.getInstances()).thenReturn( instances );
		TargetHandlerParameters parameters = new TargetHandlerParameters().targetProperties( new HashMap<String,String>( 0 ));
		this.handler.terminateMachine( parameters, "test @ test" );

		Mockito.verify( componentInstance, Mockito.only()).getInstanceName();
		Mockito.verifyZeroInteractions( this.manager );
		// No error, even if no matching iPojo instance was found
	}


	@Test
	public void testTerminateMachine_simulatePlugins() throws Exception {

		// Configure the mocks
		ComponentInstance componentInstance = Mockito.mock( ComponentInstance.class );
		Mockito.when( componentInstance.getInstanceName()).thenReturn( "test @ test" );

		List<ComponentInstance> instances = new ArrayList<> ();
		instances.add( componentInstance );

		Mockito.when( this.standardAgentFactory.getInstances()).thenReturn( instances );

		// Execute and check
		TargetHandlerParameters parameters = new TargetHandlerParameters().targetProperties( new HashMap<String,String>( 0 ));
		this.handler.terminateMachine( parameters, "test @ test" );

		Mockito.verify( componentInstance, Mockito.times( 2 )).getInstanceName();
		Mockito.verify( componentInstance, Mockito.times( 1 )).dispose();
		Mockito.verifyZeroInteractions( this.manager );
	}


	@Test
	public void testTerminateMachine_executeRealRecipes() throws Exception {

		// Configure the mocks
		TestApplication app = new TestApplication();
		ComponentInstance componentInstance = Mockito.mock( ComponentInstance.class );
		Mockito.when( componentInstance.getInstanceName()).thenReturn( "/" + app.getTomcatVm().getName() + " @ " + app.getName());

		List<ComponentInstance> instances = new ArrayList<> ();
		instances.add( componentInstance );

		Mockito.when( this.nazgulAgentFactory.getInstances()).thenReturn( instances );
		Mockito.when( this.manager.instancesMngr()).thenReturn( Mockito.mock( IInstancesMngr.class ));

		IApplicationMngr applicationMngr = Mockito.mock( IApplicationMngr.class );
		Mockito.when( applicationMngr.findManagedApplicationByName( Mockito.anyString())).thenReturn( new ManagedApplication( app ));
		Mockito.when( this.manager.applicationMngr()).thenReturn( applicationMngr );

		// Execute and check
		Map<String,String> targetProperties = new HashMap<>( 1 );
		targetProperties.put( InMemoryHandler.EXECUTE_REAL_RECIPES, "true" );
		TargetHandlerParameters parameters = new TargetHandlerParameters();
		parameters.targetProperties( targetProperties );

		this.handler.terminateMachine( parameters, componentInstance.getInstanceName());

		Mockito.verify( componentInstance, Mockito.times( 1 )).dispose();
		Mockito.verify( this.manager, Mockito.times( 1 )).applicationMngr();
		Mockito.verify( this.manager, Mockito.times( 1 )).instancesMngr();
		Mockito.verifyZeroInteractions( this.standardAgentFactory );
	}
}
