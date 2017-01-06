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

package net.roboconf.messaging.api.utils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

import org.junit.Assert;
import org.junit.Test;

import net.roboconf.core.model.beans.Component;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.messaging.api.messages.Message;
import net.roboconf.messaging.api.messages.from_agent_to_agent.MsgCmdAddImport;
import net.roboconf.messaging.api.messages.from_agent_to_agent.MsgCmdRemoveImport;
import net.roboconf.messaging.api.messages.from_agent_to_agent.MsgCmdRequestImport;
import net.roboconf.messaging.api.messages.from_agent_to_dm.MsgNotifAutonomic;
import net.roboconf.messaging.api.messages.from_agent_to_dm.MsgNotifHeartbeat;
import net.roboconf.messaging.api.messages.from_agent_to_dm.MsgNotifInstanceChanged;
import net.roboconf.messaging.api.messages.from_agent_to_dm.MsgNotifInstanceRemoved;
import net.roboconf.messaging.api.messages.from_agent_to_dm.MsgNotifLogs;
import net.roboconf.messaging.api.messages.from_agent_to_dm.MsgNotifMachineDown;
import net.roboconf.messaging.api.messages.from_dm_to_agent.MsgCmdAddInstance;
import net.roboconf.messaging.api.messages.from_dm_to_agent.MsgCmdChangeBinding;
import net.roboconf.messaging.api.messages.from_dm_to_agent.MsgCmdChangeInstanceState;
import net.roboconf.messaging.api.messages.from_dm_to_agent.MsgCmdChangeLogLevel;
import net.roboconf.messaging.api.messages.from_dm_to_agent.MsgCmdGatherLogs;
import net.roboconf.messaging.api.messages.from_dm_to_agent.MsgCmdRemoveInstance;
import net.roboconf.messaging.api.messages.from_dm_to_agent.MsgCmdResynchronize;
import net.roboconf.messaging.api.messages.from_dm_to_agent.MsgCmdSendInstances;
import net.roboconf.messaging.api.messages.from_dm_to_agent.MsgCmdSetScopedInstance;
import net.roboconf.messaging.api.messages.from_dm_to_agent.MsgCmdUpdateProbeConfiguration;
import net.roboconf.messaging.api.messages.from_dm_to_dm.MsgEcho;

/**
 * @author Vincent Zurczak - Linagora
 */
public class SerializationUtilsTest {

	// From agent

	@Test
	public void testMessage_heartbeat() throws Exception {

		MsgNotifHeartbeat msg = new MsgNotifHeartbeat( "app1", "instance1", "127.0.0.1" );
		checkBasics( msg, MsgNotifHeartbeat.class );

		msg = new MsgNotifHeartbeat( "app1", new Instance( "instance2" ), "192.168.0.11" );
		checkBasics( msg, MsgNotifHeartbeat.class );
	}


	@Test
	public void testMessage_autonomic() throws Exception {

		MsgNotifAutonomic msg = new MsgNotifAutonomic( "app1", "instance1", "too high", "oops" );
		checkBasics( msg, MsgNotifAutonomic.class );
	}


	@Test
	public void testMessage_logs() throws Exception {

		MsgNotifLogs msg = new MsgNotifLogs( "app1", "instance1", null );
		checkBasics( msg, MsgNotifLogs.class );

		Map<String,byte[]> map = new HashMap<> ();
		msg = new MsgNotifLogs( "app2", "instance2", map );
		checkBasics( msg, MsgNotifLogs.class );

		map.put( "file1", new byte[ 0 ]);
		map.put( "file2", "test".getBytes( "UTF-8" ));
		msg = new MsgNotifLogs( "app5", "instance4", map );
		checkBasics( msg, MsgNotifLogs.class );
	}


	@Test
	public void testMessage_machineDown() throws Exception {

		MsgNotifMachineDown msg = new MsgNotifMachineDown( "app1", "instance1" );
		checkBasics( msg, MsgNotifMachineDown.class );
	}


	@Test
	public void testMessage_instanceChanged() throws Exception {

		MsgNotifInstanceChanged msg = new MsgNotifInstanceChanged( "app2", new Instance( "instance1" ));
		checkBasics( msg, MsgNotifInstanceChanged.class );
	}


	@Test
	public void testMessage_instanceRemoved() throws Exception {

		MsgNotifInstanceRemoved msg = new MsgNotifInstanceRemoved( "app2", new Instance( "instance1" ));
		checkBasics( msg, MsgNotifInstanceRemoved.class );
	}


	@Test
	public void testMessage_removeImport() throws Exception {

		MsgCmdRemoveImport msg = new MsgCmdRemoveImport( "app", "change-me", "anything" );
		checkBasics( msg, MsgCmdRemoveImport.class );
	}


	@Test
	public void testMessage_addImport() throws Exception {

		Map<String,String> map = new HashMap<> ();
		map.put( "yeah", "value" );

		MsgCmdAddImport msg = new MsgCmdAddImport( "app", "change-me", "anything", map );
		checkBasics( msg, MsgCmdAddImport.class );
	}


	@Test
	public void testMessage_requestImport() throws Exception {

		MsgCmdRequestImport msg = new MsgCmdRequestImport( "app", "dsf" );
		checkBasics( msg, MsgCmdRequestImport.class );
	}


	// From DM


	@Test
	public void testMessage_setRootInstance() throws Exception {

		MsgCmdSetScopedInstance msg = new MsgCmdSetScopedInstance( new Instance( "instance1" ));
		checkBasics( msg, MsgCmdSetScopedInstance.class );

		Map<String,String> map1 = new HashMap<> ();
		map1.put( "test", "t1" );
		map1.put( "another", "t2" );

		Map<String,Set<String>> map2 = new HashMap<> ();
		Set<String> appNames = new LinkedHashSet<> ();
		appNames.add( "app1" );
		appNames.add( "app2" );
		map2.put( "app_prefix", appNames );

		Map<String,byte[]> map3 = new HashMap<> ();
		map3.put("script", "toto".getBytes( "UTF-8" ));

		msg = new MsgCmdSetScopedInstance( new Instance( "instance1" ), map1, map2, map3 );
		checkBasics( msg, MsgCmdSetScopedInstance.class );
	}


	@Test
	public void testMessage_resynchronize() throws Exception {

		MsgCmdResynchronize msg = new MsgCmdResynchronize();
		checkBasics( msg, MsgCmdResynchronize.class );
	}


	@Test
	public void testMessage_changeLogLevel() throws Exception {

		MsgCmdChangeLogLevel msg = new MsgCmdChangeLogLevel( Level.FINER );
		checkBasics( msg, MsgCmdChangeLogLevel.class );
	}


	@Test
	public void testMessage_gatherLogs() throws Exception {

		MsgCmdGatherLogs msg = new MsgCmdGatherLogs();
		checkBasics( msg, MsgCmdGatherLogs.class );
	}


	@Test
	public void testMessage_updateProbeConfiguration() throws Exception {

		MsgCmdUpdateProbeConfiguration msg = new MsgCmdUpdateProbeConfiguration( "/inst", null );
		checkBasics( msg, MsgCmdUpdateProbeConfiguration.class );

		msg = new MsgCmdUpdateProbeConfiguration( new Instance( "inst" ), new HashMap<String,byte[]>( 0 ));
		checkBasics( msg, MsgCmdUpdateProbeConfiguration.class );
	}


	@Test
	public void testMessage_changeBinding() throws Exception {

		MsgCmdChangeBinding msg = new MsgCmdChangeBinding( "tpl", new HashSet<>( Arrays.asList( "app" )));
		checkBasics( msg, MsgCmdChangeBinding.class );

		msg = new MsgCmdChangeBinding( "tpl", new HashSet<>( Arrays.asList( "app1", "app2" )));
		checkBasics( msg, MsgCmdChangeBinding.class );
	}


	@Test
	public void testMessage_removeInstance() throws Exception {

		MsgCmdRemoveInstance msg = new MsgCmdRemoveInstance( "/inst1" );
		checkBasics( msg, MsgCmdRemoveInstance.class );

		msg = new MsgCmdRemoveInstance( new Instance( "root" ));
		checkBasics( msg, MsgCmdRemoveInstance.class );
	}


	@Test
	public void testMessage_addInstance() throws Exception {

		Instance child = new Instance( "child" ).channel( "channel 4" ).status( InstanceStatus.DEPLOYED_STOPPED );
		child.component( new Component( "comp_child" ).installerName( "whatever" ));

		MsgCmdAddInstance msg = new MsgCmdAddInstance( child );
		checkBasics( msg, MsgCmdAddInstance.class );

		Instance root = new Instance( "root" ).status( InstanceStatus.DEPLOYED_STARTED ).channel( "channel1" ).channel( "channel2" );
		root.component( new Component( "comp_root" ).installerName( "whatever" ));
		InstanceHelpers.insertChild( root, child );

		msg = new MsgCmdAddInstance( child );
		checkBasics( msg, MsgCmdAddInstance.class );

		msg = new MsgCmdAddInstance( new Instance( "instance without component" ));
		checkBasics( msg, MsgCmdAddInstance.class );
	}


	@Test
	public void testMessage_restoreInstance() throws Exception {

		MsgCmdSendInstances msg = new MsgCmdSendInstances();
		checkBasics( msg, MsgCmdSendInstances.class );
	}


	@Test
	public void testMessage_changeInstanceState() throws Exception {

		MsgCmdChangeInstanceState msg = new MsgCmdChangeInstanceState( "/o/mp/k", InstanceStatus.DEPLOYED_STARTED );
		checkBasics( msg, MsgCmdChangeInstanceState.class );

		msg = new MsgCmdChangeInstanceState((String) null, InstanceStatus.NOT_DEPLOYED );
		checkBasics( msg, MsgCmdChangeInstanceState.class );

		msg = new MsgCmdChangeInstanceState( new Instance( "test" ), InstanceStatus.NOT_DEPLOYED );
		checkBasics( msg, MsgCmdChangeInstanceState.class );

		Map<String,byte[]> fileNameToFileContent = new HashMap<> ();
		fileNameToFileContent.put( "readme.txt", new byte[ 90 ]);

		msg = new MsgCmdChangeInstanceState( "/oops", InstanceStatus.NOT_DEPLOYED, fileNameToFileContent );
		checkBasics( msg, MsgCmdChangeInstanceState.class );

		msg = new MsgCmdChangeInstanceState((Instance) null, InstanceStatus.NOT_DEPLOYED, fileNameToFileContent );
		checkBasics( msg, MsgCmdChangeInstanceState.class );
	}


	@Test
	public void testMessage_msgEcho() throws Exception {

		MsgEcho msg = new MsgEcho( "coucou", UUID.randomUUID());
		checkBasics( msg, MsgEcho.class );

		msg = new MsgEcho( "hello" );
		checkBasics( msg, MsgEcho.class );
	}


	/**
	 * Serializes, deserializes and compares messages.
	 * @param msg
	 * @param clazz
	 * @return
	 * @throws Exception
	 */
	public static <T extends Message> T checkBasics( Message msg, Class<T> clazz ) throws Exception {

		String prefix = "Class " + clazz.getSimpleName();
		Assert.assertTrue(
				prefix + ": invalid invocation. First parameter must be of type " + clazz.getSimpleName() + ".",
				clazz.isAssignableFrom( msg.getClass()));

		// Write and read
		byte[] bytes = SerializationUtils.serializeObject( msg );
		Message newMsg = SerializationUtils.deserializeObject( bytes );

		// Compare classes
		Assert.assertEquals( prefix, clazz.getName(), newMsg.getClass().getName());
		Assert.assertTrue( prefix, clazz.isAssignableFrom( newMsg.getClass()));

		// Compare internal fields
		for( Method m : clazz.getMethods()) {
			if( ! m.getName().startsWith( "get" )
					|| m.getParameterTypes().length != 0 )
				continue;

			Object expectedValue = m.invoke( msg );
			Object value = m.invoke( newMsg );

			// Maps gets a special treatment
			if( value instanceof Map ) {
				Map<?,?> expectedMap = (Map<?,?>) expectedValue;
				Map<?,?> map = (Map<?,?>) value;

				Assert.assertEquals( prefix, expectedMap.size(), map.size());
				for( Map.Entry<?,?> entry : expectedMap.entrySet()) {
					Assert.assertTrue( prefix + ": key was not found. " + entry.getKey(), map.containsKey( entry.getKey()));
					if( entry.getValue() instanceof String )
						Assert.assertEquals( prefix + ": value did not match. " + entry.getKey(), entry.getValue(), map.get( entry.getKey()));
				}
			}

			// Other objects are compared directly
			else {
				Assert.assertEquals( prefix + ": invalid match for " + m.getName() + ".", expectedValue, value );
			}
		}

		return clazz.cast( newMsg );
	}
}
