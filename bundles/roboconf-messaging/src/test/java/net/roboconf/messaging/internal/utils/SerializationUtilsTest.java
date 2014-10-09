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

package net.roboconf.messaging.internal.utils;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.runtime.Component;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.core.model.runtime.Instance.InstanceStatus;
import net.roboconf.messaging.messages.Message;
import net.roboconf.messaging.messages.from_agent_to_agent.MsgCmdAddImport;
import net.roboconf.messaging.messages.from_agent_to_agent.MsgCmdRemoveImport;
import net.roboconf.messaging.messages.from_agent_to_agent.MsgCmdRequestImport;
import net.roboconf.messaging.messages.from_agent_to_dm.MsgNotifHeartbeat;
import net.roboconf.messaging.messages.from_agent_to_dm.MsgNotifInstanceChanged;
import net.roboconf.messaging.messages.from_agent_to_dm.MsgNotifInstanceRemoved;
import net.roboconf.messaging.messages.from_agent_to_dm.MsgNotifMachineDown;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdAddInstance;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdChangeInstanceState;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdRemoveInstance;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdResynchronize;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdSendInstances;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdSetRootInstance;

import org.junit.Test;

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

		MsgCmdRemoveImport msg = new MsgCmdRemoveImport( "change-me", "anything" );
		checkBasics( msg, MsgCmdRemoveImport.class );
	}


	@Test
	public void testMessage_addImport() throws Exception {

		Map<String,String> map = new HashMap<String,String> ();
		map.put( "yeah", "value" );

		MsgCmdAddImport msg = new MsgCmdAddImport( "change-me", "anything", map );
		checkBasics( msg, MsgCmdAddImport.class );
	}


	@Test
	public void testMessage_requestImport() throws Exception {

		MsgCmdRequestImport msg = new MsgCmdRequestImport( "dsf" );
		checkBasics( msg, MsgCmdRequestImport.class );
	}


	// From DM


	@Test
	public void testMessage_setRootInstance() throws Exception {

		MsgCmdSetRootInstance msg = new MsgCmdSetRootInstance( new Instance( "instance1" ));
		checkBasics( msg, MsgCmdSetRootInstance.class );
	}


	@Test
	public void testMessage_resynchronize() throws Exception {

		MsgCmdResynchronize msg = new MsgCmdResynchronize();
		checkBasics( msg, MsgCmdResynchronize.class );
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
		child.component( new Component( "comp_child" ).alias( "component" ).installerName( "whatever" ));

		MsgCmdAddInstance msg = new MsgCmdAddInstance( child );
		checkBasics( msg, MsgCmdAddInstance.class );

		Instance root = new Instance( "root" ).status( InstanceStatus.DEPLOYED_STARTED );
		root.component( new Component( "comp_root" ).alias( "component" ).installerName( "whatever" ));
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

		Map<String,byte[]> fileNameToFileContent = new HashMap<String,byte[]> ();
		fileNameToFileContent.put( "readme.txt", new byte[ 90 ]);

		msg = new MsgCmdChangeInstanceState( "/oops", InstanceStatus.NOT_DEPLOYED, fileNameToFileContent );
		checkBasics( msg, MsgCmdChangeInstanceState.class );

		msg = new MsgCmdChangeInstanceState((Instance) null, InstanceStatus.NOT_DEPLOYED, fileNameToFileContent );
		checkBasics( msg, MsgCmdChangeInstanceState.class );
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
