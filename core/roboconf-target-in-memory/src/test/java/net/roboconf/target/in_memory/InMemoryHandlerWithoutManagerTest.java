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

import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.Factory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import net.roboconf.core.Constants;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.userdata.UserDataHelpers;
import net.roboconf.core.utils.Utils;
import net.roboconf.messaging.api.factory.MessagingClientFactoryRegistry;
import net.roboconf.target.api.TargetException;
import net.roboconf.target.api.TargetHandlerParameters;

/**
 * @author Vincent Zurczak - Linagora
 */
public class InMemoryHandlerWithoutManagerTest {

	private Map<String, String> msgCfg = new LinkedHashMap<>();
	private InMemoryHandler target;
	private ComponentInstance ipojoInstance;


	@Before
	public void setMessagingConfiguration() throws Exception {

		this.msgCfg = new LinkedHashMap<>();
		this.msgCfg.put("net.roboconf.messaging.type", "telepathy");
		this.msgCfg.put("mindControl", "false");
		this.msgCfg.put("psychosisProtection", "active");

		this.target = new InMemoryHandler();
		this.target.standardAgentFactory = Mockito.mock( Factory.class );
		this.ipojoInstance = Mockito.mock( ComponentInstance.class );
		Mockito
			.when( this.target.standardAgentFactory.createComponentInstance( Mockito.any( Dictionary.class )))
			.thenReturn( this.ipojoInstance );
	}


	@Test
	public void checkBasics() throws Exception {

		Assert.assertEquals( InMemoryHandler.TARGET_ID, this.target.getTargetId());
		this.target.terminateMachine( new TargetHandlerParameters(), "whatever" );
	}


	@Test
	@SuppressWarnings( "rawtypes" )
	public void testCreateVm() throws Exception {

		TargetHandlerParameters parameters = new TargetHandlerParameters()
				.messagingProperties( this.msgCfg )
				.scopedInstancePath( "vm" )
				.applicationName( "my app" )
				.domain( "domain" );

		this.target.setMessagingFactoryRegistry( new MessagingClientFactoryRegistry());
		this.target.createMachine( parameters );

		ArgumentCaptor<Dictionary> arg = ArgumentCaptor.forClass( Dictionary.class );
		Mockito.verify( this.target.standardAgentFactory, Mockito.only()).createComponentInstance( arg.capture());

		Dictionary dico = arg.getValue();
		Assert.assertEquals( 4 + this.msgCfg.size(), dico.size());
		Assert.assertEquals( "domain", dico.get( "domain" ));
		Assert.assertEquals( "my app", dico.get( "application-name" ));
		Assert.assertEquals( "vm", dico.get( "scoped-instance-path" ));
		Assert.assertEquals( "telepathy", dico.get( Constants.MESSAGING_TYPE ));
	}


	@Test( expected = TargetException.class )
	public void testCreateVm_startFails() throws Exception {

		TargetHandlerParameters parameters = new TargetHandlerParameters()
				.messagingProperties( this.msgCfg )
				.scopedInstancePath( "vm" )
				.applicationName( "my app" )
				.domain( "domain" );

		Mockito.doThrow( new RuntimeException( "for test" )).when( this.ipojoInstance ).start();
		this.target.setMessagingFactoryRegistry( new MessagingClientFactoryRegistry());
		this.target.createMachine( parameters );
	}


	@Test
	@SuppressWarnings( "rawtypes" )
	public void testCreateVm_withDelay() throws Exception {

		this.target.setDefaultDelay( 100L );
		Assert.assertEquals( 100L, this.target.getDefaultDelay());

		Map<String,String> targetProperties = new HashMap<>( 1 );
		targetProperties.put( InMemoryHandler.DELAY, "20L" );

		TargetHandlerParameters parameters = new TargetHandlerParameters()
				.targetProperties( targetProperties )
				.messagingProperties( this.msgCfg )
				.scopedInstancePath( "vm" )
				.applicationName( "my app" )
				.domain( "domain" );

		this.target.setMessagingFactoryRegistry( new MessagingClientFactoryRegistry());
		this.target.createMachine( parameters );

		ArgumentCaptor<Dictionary> arg = ArgumentCaptor.forClass( Dictionary.class );
		Mockito.verify( this.target.standardAgentFactory, Mockito.only()).createComponentInstance( arg.capture());

		Dictionary dico = arg.getValue();
		Assert.assertEquals( 4 + this.msgCfg.size(), dico.size());
		Assert.assertEquals( "domain", dico.get( "domain" ));
		Assert.assertEquals( "my app", dico.get( "application-name" ));
		Assert.assertEquals( "vm", dico.get( "scoped-instance-path" ));
		Assert.assertEquals( "telepathy", dico.get( Constants.MESSAGING_TYPE ));
	}


	@Test
	@SuppressWarnings( "rawtypes" )
	public void testCreateVm_withDefaultDelay() throws Exception {

		this.target.setDefaultDelay( 10L );
		Assert.assertEquals( 10L, this.target.getDefaultDelay());

		Map<String,String> targetProperties = new HashMap<>( 0 );
		TargetHandlerParameters parameters = new TargetHandlerParameters()
				.targetProperties( targetProperties )
				.messagingProperties( this.msgCfg )
				.scopedInstancePath( "vm" )
				.applicationName( "my app" )
				.domain( "domain" );

		this.target.setMessagingFactoryRegistry( new MessagingClientFactoryRegistry());
		this.target.createMachine( parameters );

		ArgumentCaptor<Dictionary> arg = ArgumentCaptor.forClass( Dictionary.class );
		Mockito.verify( this.target.standardAgentFactory, Mockito.only()).createComponentInstance( arg.capture());

		Dictionary dico = arg.getValue();
		Assert.assertEquals( 4 + this.msgCfg.size(), dico.size());
		Assert.assertEquals( "domain", dico.get( "domain" ));
		Assert.assertEquals( "my app", dico.get( "application-name" ));
		Assert.assertEquals( "vm", dico.get( "scoped-instance-path" ));
		Assert.assertEquals( "telepathy", dico.get( Constants.MESSAGING_TYPE ));
	}


	@Test
	@SuppressWarnings( "rawtypes" )
	public void testCreateAndTerminateVm_withUserData() throws Exception {

		// Prepare
		Map<String,String> targetProperties = new HashMap<>( 1 );
		targetProperties.put( InMemoryHandler.WRITE_USER_DATA, "true" );

		TargetHandlerParameters parameters = new TargetHandlerParameters()
				.targetProperties( targetProperties )
				.messagingProperties( this.msgCfg )
				.scopedInstancePath( "vm" )
				.applicationName( "my app" )
				.domain( "domain" );

		String expectedMachineId = parameters.getScopedInstancePath() + " @ " + parameters.getApplicationName();
		Mockito.when( this.ipojoInstance.getInstanceName()).thenReturn( expectedMachineId );

		// Create
		this.target.setMessagingFactoryRegistry( new MessagingClientFactoryRegistry());
		String machineId = this.target.createMachine( parameters );
		Assert.assertEquals( expectedMachineId, machineId );

		ArgumentCaptor<Dictionary> arg = ArgumentCaptor.forClass( Dictionary.class );
		Mockito.verify( this.target.standardAgentFactory, Mockito.only()).createComponentInstance( arg.capture());

		Dictionary dico = arg.getValue();
		Assert.assertEquals( 6 + this.msgCfg.size(), dico.size());
		Assert.assertEquals( "domain", dico.get( "domain" ));
		Assert.assertEquals( "my app", dico.get( "application-name" ));
		Assert.assertEquals( "vm", dico.get( "scoped-instance-path" ));
		Assert.assertEquals( "telepathy", dico.get( Constants.MESSAGING_TYPE ));
		Assert.assertEquals( "true", dico.get( "override-properties-with-user-data" ));

		String userDataFilePath = (String) dico.get( "parameters" );
		Assert.assertNotNull( userDataFilePath );

		File userDataFile = new File( new URI( userDataFilePath ));
		Assert.assertTrue( userDataFile.isFile());

		Properties userDataProps = UserDataHelpers.readUserData( Utils.readFileContent( userDataFile ), null );
		Assert.assertEquals( 3 + this.msgCfg.size(), userDataProps.size());
		Assert.assertEquals( "domain", userDataProps.get( "domain" ));
		Assert.assertEquals( "my app", userDataProps.get( "application.name" ));
		Assert.assertEquals( "vm", userDataProps.get( "scoped.instance.path" ));

		for( Map.Entry<String,String> entry : this.msgCfg.entrySet())
			Assert.assertEquals( entry.getKey(), entry.getValue(), userDataProps.get( entry.getKey()));

		// Terminate (verify the user data file was deleted)
		Mockito.when( this.target.standardAgentFactory.getInstances()).thenReturn( Arrays.asList( this.ipojoInstance ));
		Mockito.when( this.ipojoInstance.getInstanceName()).thenReturn( machineId );

		this.target.terminateMachine( parameters, machineId );
		Assert.assertFalse( userDataFile.isFile());
	}


	@Test
	public void testConfigureAndIsRunning() throws Exception {

		Instance scopedInstance = new Instance();
		TargetHandlerParameters parameters = new TargetHandlerParameters()
				.messagingProperties( this.msgCfg )
				.scopedInstancePath( "vm" )
				.scopedInstance( scopedInstance )
				.applicationName( "my app" )
				.domain( "domain" );

		Assert.assertEquals( 0, scopedInstance.data.size());
		this.target.configureMachine( parameters, null );
		Assert.assertTrue( scopedInstance.data.containsKey( Instance.READY_FOR_CFG_MARKER ));
		Assert.assertFalse( this.target.isMachineRunning( new TargetHandlerParameters(), "whatever, there is no iPojo factory" ));
	}


	@Test
	public void testPreventNull() {
		Assert.assertEquals( 0, InMemoryHandler.preventNull( null ).size());

		Map<String,String> targetProperties = new HashMap<> ();
		Assert.assertEquals( 0, InMemoryHandler.preventNull( targetProperties ).size());

		targetProperties.put( "val 1", "test" );
		targetProperties.put( "val 2", "test" );
		Assert.assertEquals( 2, InMemoryHandler.preventNull( targetProperties ).size());
	}


	@Test
	public void testSimulatePlugins() {

		Map<String,String> targetProperties = new HashMap<> ();
		Assert.assertTrue( InMemoryHandler.simulatePlugins( targetProperties ));

		targetProperties.put( InMemoryHandler.EXECUTE_REAL_RECIPES, "false" );
		Assert.assertTrue( InMemoryHandler.simulatePlugins( targetProperties ));

		targetProperties.put( InMemoryHandler.EXECUTE_REAL_RECIPES, "True" );
		Assert.assertFalse( InMemoryHandler.simulatePlugins( targetProperties ));
	}


	@Test
	public void testParseMachineId() throws Exception {

		Map.Entry<String,String> entry = InMemoryHandler.parseMachineId( "/VM @ App" );
		Assert.assertEquals( "/VM", entry.getKey());
		Assert.assertEquals( "App", entry.getValue());

		entry = InMemoryHandler.parseMachineId( " /VM/server@App 2   " );
		Assert.assertEquals( "/VM/server", entry.getKey());
		Assert.assertEquals( "App 2", entry.getValue());
	}


	@Test
	public void testRetrievePublicIpAddress() throws Exception {
		Assert.assertNull( this.target.retrievePublicIpAddress( null, null ));
	}


	@Test
	public void testIsMachineRunning_noFactory() throws Exception {

		this.target.standardAgentFactory = null;
		Map<String,String> targetProperties = new HashMap<>( 0 );
		TargetHandlerParameters parameters = new TargetHandlerParameters()
				.targetProperties( targetProperties );

		Assert.assertFalse( this.target.isMachineRunning( parameters, null ));
	}


	@Test
	public void testStop() throws Exception {

		this.target.standardAgentFactory = Mockito.mock( Factory.class );
		this.target.nazgulAgentFactory = Mockito.mock( Factory.class );

		ComponentInstance agentMock = Mockito.mock( ComponentInstance.class );
		Mockito.when( this.target.standardAgentFactory .getInstances()).thenReturn( Arrays.asList( agentMock ));

		ComponentInstance nazgulMock = Mockito.mock( ComponentInstance.class );
		Mockito.when( this.target.nazgulAgentFactory .getInstances()).thenReturn( Arrays.asList( nazgulMock ));

		this.target.stop();
		Mockito.verify( this.target.standardAgentFactory, Mockito.only()).getInstances();
		Mockito.verify( this.target.nazgulAgentFactory, Mockito.only()).getInstances();

		Mockito.verify( agentMock ).dispose();
		Mockito.verify( nazgulMock ).dispose();
	}


	@Test
	public void testStop_facotiriesAreNull() throws Exception {

		this.target.standardAgentFactory = null;
		this.target.nazgulAgentFactory = null;
		this.target.stop();
		// No error
	}
}
