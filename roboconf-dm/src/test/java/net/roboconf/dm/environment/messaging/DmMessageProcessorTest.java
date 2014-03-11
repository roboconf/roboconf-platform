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

package net.roboconf.dm.environment.messaging;

import junit.framework.Assert;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.core.model.runtime.Instance.InstanceStatus;
import net.roboconf.dm.internal.TestApplication;
import net.roboconf.messaging.messages.from_agent_to_dm.MsgNotifInstanceChanged;
import net.roboconf.messaging.messages.from_agent_to_dm.MsgNotifMachineDown;
import net.roboconf.messaging.messages.from_agent_to_dm.MsgNotifMachineUp;

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class DmMessageProcessorTest {

	@Test
	public void testProcessMsgNotifMachineUp_1() {

		final String ip = "192.13.1.23";
		TestApplication app = new TestApplication();
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getMySqlVm().getStatus());

		DmMessageProcessor processor = new DmMessageProcessor( app );
		MsgNotifMachineUp msg = new MsgNotifMachineUp( app.getMySqlVm().getName(), ip );
		processor.processMessage( msg );

		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, app.getMySqlVm().getStatus());
		String value = app.getMySqlVm().getData().get( Instance.IP_ADDRESS );
		Assert.assertNotNull( value );
		Assert.assertEquals( ip, value );
	}


	@Test
	public void testProcessMsgNotifMachineUp_2() {

		DmMessageProcessor processor = new DmMessageProcessor( new TestApplication());
		MsgNotifMachineUp msg = new MsgNotifMachineUp( "invalid name", "some ip" );
		processor.processMessage( msg );

		Assert.assertTrue( "We are supposed to reach here without any error.", true );
	}


	@Test
	public void testProcessMsgNotifMachineDown_1() {

		TestApplication app = new TestApplication();
		app.getMySqlVm().setStatus( InstanceStatus.DEPLOYED_STARTED );

		DmMessageProcessor processor = new DmMessageProcessor( app );
		MsgNotifMachineDown msg = new MsgNotifMachineDown( app.getMySqlVm().getName());
		processor.processMessage( msg );

		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getMySqlVm().getStatus());
	}


	@Test
	public void testProcessMsgNotifMachineDown_2() {

		DmMessageProcessor processor = new DmMessageProcessor( new TestApplication());
		MsgNotifMachineDown msg = new MsgNotifMachineDown( "invalid name" );
		processor.processMessage( msg );

		Assert.assertTrue( "We are supposed to reach here without any error.", true );
	}


	@Test
	public void testProcessMsgNotifInstanceChanged_1() {

		TestApplication app = new TestApplication();
		app.getMySqlVm().setStatus( InstanceStatus.DEPLOYED_STARTED );

		DmMessageProcessor processor = new DmMessageProcessor( app );
		MsgNotifInstanceChanged msg = new MsgNotifInstanceChanged( app.getMySqlVm());
		msg.setNewStatus( InstanceStatus.STOPPING );

		processor.processMessage( msg );
		Assert.assertEquals( InstanceStatus.STOPPING, app.getMySqlVm().getStatus());
	}


	@Test
	public void testProcessMsgNotifInstanceChanged_2() {

		DmMessageProcessor processor = new DmMessageProcessor( new TestApplication());
		MsgNotifInstanceChanged msg = new MsgNotifInstanceChanged( new Instance( "invalid-instance" ));
		processor.processMessage( msg );

		Assert.assertTrue( "We are supposed to reach here without any error.", true );
	}
}
