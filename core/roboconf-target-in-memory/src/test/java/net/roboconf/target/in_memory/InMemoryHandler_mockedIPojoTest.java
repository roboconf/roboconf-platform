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

package net.roboconf.target.in_memory;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;
import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.management.api.IApplicationMngr;
import net.roboconf.dm.management.api.IInstancesMngr;
import net.roboconf.messaging.api.factory.MessagingClientFactoryRegistry;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.Factory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * @author Vincent Zurczak - Linagora
 */
public class InMemoryHandler_mockedIPojoTest {

	private Map<String, String> msgCfg = new LinkedHashMap<> ();

	private Manager manager;
	private Factory agentFactory;
	private InMemoryHandler handler;


	@Before
	public void setMessagingConfiguration() {

		this.msgCfg = new LinkedHashMap<>();
		this.msgCfg.put("net.roboconf.messaging.type", "telepathy");
		this.msgCfg.put("mindControl", "false");
		this.msgCfg.put("psychosisProtection", "active");

		this.manager = Mockito.mock( Manager.class );
		this.agentFactory = Mockito.mock( Factory.class );

		this.handler = new InMemoryHandler();
		this.handler.setMessagingFactoryRegistry( new MessagingClientFactoryRegistry());

		this.handler.agentFactory = this.agentFactory;
		this.handler.manager = this.manager;
	}


	@Test
	public void testCreateMachine() throws Exception {

		// Configure the mocks
		ComponentInstance componentInstance = Mockito.mock( ComponentInstance.class );
		Mockito.when( this.agentFactory.createComponentInstance( Mockito.any( Dictionary.class ))).thenReturn( componentInstance );

		// Create a machine and verify assertions
		Assert.assertEquals( 0, this.handler.machineIdToCtx.size());
		Mockito.verifyZeroInteractions( this.agentFactory );
		Mockito.verifyZeroInteractions( componentInstance );

		String machineId = this.handler.createMachine( new HashMap<String,String>( 0 ), this.msgCfg, "/VM", "my-app" );
		Assert.assertEquals( "/VM @ my-app", machineId );

		Assert.assertEquals( 1, this.handler.machineIdToCtx.size());
		Assert.assertEquals( "my-app", this.handler.machineIdToCtx.get( machineId ).getValue());
		Assert.assertEquals( "/VM", this.handler.machineIdToCtx.get( machineId ).getKey());
		Mockito.verify( this.agentFactory, Mockito.times( 1 )).createComponentInstance( Mockito.any( Dictionary.class ));
		Mockito.verify( componentInstance, Mockito.times( 1 )).start();
	}


	@Test
	public void testMachineIsRunning() throws Exception {

		List<String> result = new ArrayList<> ();
		Mockito.when( this.agentFactory.getInstancesNames()).thenReturn( result );
		Assert.assertFalse( this.handler.isMachineRunning( null, "test" ));

		result.add( "test" );
		Assert.assertTrue( this.handler.isMachineRunning( null, "test" ));
	}


	@Test
	public void testTerminateMachine_simulatePlugins_noPojoFound() throws Exception {

		List<ComponentInstance> instances = new ArrayList<> ();
		Mockito.when( this.agentFactory.getInstances()).thenReturn( instances );
		this.handler.machineIdToCtx.put( "test @ test", new AbstractMap.SimpleEntry<String,String>( "test", "test" ));

		Assert.assertEquals( 1, this.handler.machineIdToCtx.size());
		this.handler.terminateMachine( null, "test @ test" );
		Assert.assertEquals( 0, this.handler.machineIdToCtx.size());
		// No error, even if no iPojo instance was found (e.g. if it was killed from the Ipojo console)

		Mockito.verifyZeroInteractions( this.manager );
	}


	@Test
	public void testTerminateMachine_simulatePlugins_noPojoWithTheRightName() throws Exception {

		ComponentInstance componentInstance = Mockito.mock( ComponentInstance.class );
		Mockito.when( componentInstance.getInstanceName()).thenReturn( "NOT test @ test" );

		List<ComponentInstance> instances = new ArrayList<> ();
		instances.add( componentInstance );

		Mockito.when( this.agentFactory.getInstances()).thenReturn( instances );
		this.handler.machineIdToCtx.put( "test @ test", new AbstractMap.SimpleEntry<String,String>( "test", "test" ));

		Assert.assertEquals( 1, this.handler.machineIdToCtx.size());
		this.handler.terminateMachine( null, "test @ test" );
		Assert.assertEquals( 0, this.handler.machineIdToCtx.size());

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

		Mockito.when( this.agentFactory.getInstances()).thenReturn( instances );
		this.handler.machineIdToCtx.put( "test @ test", new AbstractMap.SimpleEntry<String,String>( "test", "test" ));

		// Execute and check
		Assert.assertEquals( 1, this.handler.machineIdToCtx.size());
		this.handler.terminateMachine( null, "test @ test" );
		Assert.assertEquals( 0, this.handler.machineIdToCtx.size());

		Mockito.verify( componentInstance, Mockito.times( 1 )).getInstanceName();
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

		Mockito.when( this.agentFactory.getInstances()).thenReturn( instances );
		this.handler.machineIdToCtx.put(
				componentInstance.getInstanceName(),
				new AbstractMap.SimpleEntry<String,String>( "/" + app.getTomcatVm().getName(), app.getName()));

		Mockito.when( this.manager.instancesMngr()).thenReturn( Mockito.mock( IInstancesMngr.class ));
		IApplicationMngr applicationMngr = Mockito.mock( IApplicationMngr.class );
		Mockito.when( applicationMngr.findManagedApplicationByName( Mockito.anyString())).thenReturn( new ManagedApplication( app ));
		Mockito.when( this.manager.applicationMngr()).thenReturn( applicationMngr );

		// Execute and check
		Map<String,String> targetProperties = new HashMap<>( 1 );
		targetProperties.put( InMemoryHandler.EXECUTE_REAL_RECIPES, "true" );

		Assert.assertEquals( 1, this.handler.machineIdToCtx.size());
		this.handler.terminateMachine( targetProperties, componentInstance.getInstanceName());
		Assert.assertEquals( 0, this.handler.machineIdToCtx.size());

		Mockito.verify( componentInstance, Mockito.times( 1 )).dispose();
		Mockito.verify( this.manager, Mockito.times( 1 )).applicationMngr();
		Mockito.verify( this.manager, Mockito.times( 1 )).instancesMngr();
	}
}
