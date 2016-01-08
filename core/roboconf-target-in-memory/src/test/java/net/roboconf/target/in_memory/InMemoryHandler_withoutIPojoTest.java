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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Assert;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.messaging.api.factory.MessagingClientFactoryRegistry;
import net.roboconf.target.api.TargetException;

import org.junit.Before;
import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class InMemoryHandler_withoutIPojoTest {

	private Map<String, String> msgCfg = new LinkedHashMap<>();


	@Before
	public void setMessagingConfiguration() {
		this.msgCfg = new LinkedHashMap<>();
		this.msgCfg.put("net.roboconf.messaging.type", "telepathy");
		this.msgCfg.put("mindControl", "false");
		this.msgCfg.put("psychosisProtection", "active");
	}


	@Test
	public void checkBasics() throws Exception {

		InMemoryHandler target = new InMemoryHandler();
		Assert.assertEquals( InMemoryHandler.TARGET_ID, target.getTargetId());

		target.terminateMachine( null, "whatever" );
	}


	@Test( expected = TargetException.class )
	public void testCreateVm() throws Exception {

		InMemoryHandler target = new InMemoryHandler();
		target.setMessagingFactoryRegistry(new MessagingClientFactoryRegistry());
		target.createMachine(null, this.msgCfg, "vm", "my app");
	}


	@Test( expected = TargetException.class )
	public void testCreateVm_withDelay() throws Exception {

		InMemoryHandler handler = new InMemoryHandler();
		handler.setDefaultDelay( 100L );
		Assert.assertEquals( 100L, handler.getDefaultDelay());

		Map<String,String> targetProperties = new HashMap<>(1);
		targetProperties.put( InMemoryHandler.DELAY, "20L" );

		handler.setMessagingFactoryRegistry(new MessagingClientFactoryRegistry());
		handler.createMachine( targetProperties, this.msgCfg, "vm", "my app" );
	}


	@Test( expected = TargetException.class )
	public void testCreateVm_withDefaultDelay() throws Exception {

		InMemoryHandler handler = new InMemoryHandler();
		handler.setDefaultDelay( 10L );
		Assert.assertEquals( 10L, handler.getDefaultDelay());

		Map<String,String> targetProperties = new HashMap<>( 0 );
		handler.setMessagingFactoryRegistry(new MessagingClientFactoryRegistry());
		handler.createMachine( targetProperties, this.msgCfg, "vm", "my app" );
	}


	@Test
	public void testConfigureAndIsRunning() throws Exception {

		InMemoryHandler handler = new InMemoryHandler();
		handler.configureMachine( null, this.msgCfg, "my app", null, null, new Instance());
		Assert.assertFalse( handler.isMachineRunning( null, "whatever, there is no iPojo factory" ));
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
}
