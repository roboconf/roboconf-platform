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
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.messaging.messages.Message;
import net.roboconf.messaging.messages.from_agent_to_agent.MsgCmdImportAdd;
import net.roboconf.messaging.messages.from_agent_to_agent.MsgCmdImportRemove;
import net.roboconf.messaging.messages.from_agent_to_agent.MsgCmdImportRequest;
import net.roboconf.messaging.messages.from_agent_to_dm.MsgNotifHeartbeat;
import net.roboconf.messaging.messages.from_agent_to_dm.MsgNotifInstanceChanged;
import net.roboconf.messaging.messages.from_agent_to_dm.MsgNotifInstanceRemoved;
import net.roboconf.messaging.messages.from_agent_to_dm.MsgNotifInstanceRestoration;
import net.roboconf.messaging.messages.from_agent_to_dm.MsgNotifMachineDown;
import net.roboconf.messaging.messages.from_agent_to_dm.MsgNotifMachineUp;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdInstanceAdd;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdInstanceDeploy;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdInstanceRemove;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdInstanceRestore;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdInstanceStart;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdInstanceStop;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdInstanceUndeploy;

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class SerializationUtilsTest {

	// From agent

	@Test
	public void testMessage_heartbeat() throws Exception {

		MsgNotifHeartbeat msg = new MsgNotifHeartbeat( "app1", "instance1" );
		checkBasics( msg, MsgNotifHeartbeat.class );
	}


	@Test
	public void testMessage_machineDown() throws Exception {

		MsgNotifMachineDown msg = new MsgNotifMachineDown( "app1", "instance1" );
		checkBasics( msg, MsgNotifMachineDown.class );
	}


	@Test
	public void testMessage_machineUp() throws Exception {

		MsgNotifMachineUp msg = new MsgNotifMachineUp( "app1", "instance1", "127.0.0.1" );
		checkBasics( msg, MsgNotifMachineUp.class );

		msg = new MsgNotifMachineUp( "app1", new Instance( "instance2" ), "192.168.1.2" );
		checkBasics( msg, MsgNotifMachineUp.class );
	}


	@Test
	public void testMessage_instanceRestoration() throws Exception {

		MsgNotifInstanceRestoration msg = new MsgNotifInstanceRestoration( "app2", new Instance( "root" ));
		checkBasics( msg, MsgNotifInstanceRestoration.class );
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
	public void testMessage_importRemove() throws Exception {

		MsgCmdImportRemove msg = new MsgCmdImportRemove( "change-me", "anything" );
		checkBasics( msg, MsgCmdImportRemove.class );
	}


	@Test
	public void testMessage_importAdd() throws Exception {

		Map<String,String> map = new HashMap<String,String> ();
		map.put( "yeah", "value" );

		MsgCmdImportAdd msg = new MsgCmdImportAdd( "change-me", "anything", map );
		checkBasics( msg, MsgCmdImportAdd.class );
	}


	@Test
	public void testMessage_importRequest() throws Exception {

		MsgCmdImportRequest msg = new MsgCmdImportRequest( "dsf" );
		checkBasics( msg, MsgCmdImportRequest.class );
	}


	// From DM


	@Test
	public void testMessage_instanceAdd() throws Exception {

		MsgCmdInstanceAdd msg = new MsgCmdInstanceAdd( "/parent", new Instance( "instance1" ));
		checkBasics( msg, MsgCmdInstanceAdd.class );

		msg = new MsgCmdInstanceAdd( new Instance( "root" ), new Instance( "instance2" ));
		checkBasics( msg, MsgCmdInstanceAdd.class );
	}


	@Test
	public void testMessage_instanceRemove() throws Exception {

		MsgCmdInstanceRemove msg = new MsgCmdInstanceRemove( "/inst1" );
		checkBasics( msg, MsgCmdInstanceRemove.class );

		msg = new MsgCmdInstanceRemove( new Instance( "root" ));
		checkBasics( msg, MsgCmdInstanceRemove.class );
	}


	@Test
	public void testMessage_instanceRestore() throws Exception {

		MsgCmdInstanceRestore msg = new MsgCmdInstanceRestore();
		checkBasics( msg, MsgCmdInstanceRestore.class );
	}


	@Test
	public void testMessage_instanceStart() throws Exception {

		MsgCmdInstanceStart msg = new MsgCmdInstanceStart( "/o/mp/k" );
		checkBasics( msg, MsgCmdInstanceStart.class );
	}


	@Test
	public void testMessage_instanceStop() throws Exception {

		MsgCmdInstanceStop msg = new MsgCmdInstanceStop( "/o/m/k" );
		checkBasics( msg, MsgCmdInstanceStop.class );

		msg = new MsgCmdInstanceStop( new Instance( "root" ));
		checkBasics( msg, MsgCmdInstanceStop.class );
	}


	@Test
	public void testMessage_instanceUndeploy() throws Exception {

		MsgCmdInstanceUndeploy msg = new MsgCmdInstanceUndeploy( "/o/mp/k" );
		checkBasics( msg, MsgCmdInstanceUndeploy.class );

		msg = new MsgCmdInstanceUndeploy( new Instance( "root" ));
		checkBasics( msg, MsgCmdInstanceUndeploy.class );
	}


	@Test
	public void testMessage_instanceDeploy() throws Exception {

		Map<String,byte[]> fileNameToFileContent = new HashMap<String,byte[]> ();
		fileNameToFileContent.put( "readme.txt", new byte[ 90 ]);

		MsgCmdInstanceDeploy msg = new MsgCmdInstanceDeploy( "/o/mp/k", fileNameToFileContent );
		checkBasics( msg, MsgCmdInstanceDeploy.class );

		msg = new MsgCmdInstanceDeploy( new Instance( "root" ), fileNameToFileContent );
		checkBasics( msg, MsgCmdInstanceDeploy.class );
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
