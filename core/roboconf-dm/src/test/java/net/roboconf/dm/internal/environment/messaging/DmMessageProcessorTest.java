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

package net.roboconf.dm.internal.environment.messaging;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import net.roboconf.core.Constants;
import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.dm.internal.test.TestManagerWrapper;
import net.roboconf.dm.internal.test.TestTargetResolver;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.management.events.IDmListener;
import net.roboconf.messaging.api.MessagingConstants;
import net.roboconf.messaging.api.internal.client.test.TestClient;
import net.roboconf.messaging.api.messages.Message;
import net.roboconf.messaging.api.messages.from_agent_to_dm.MsgNotifHeartbeat;
import net.roboconf.messaging.api.messages.from_agent_to_dm.MsgNotifInstanceChanged;
import net.roboconf.messaging.api.messages.from_agent_to_dm.MsgNotifInstanceRemoved;
import net.roboconf.messaging.api.messages.from_agent_to_dm.MsgNotifLogs;
import net.roboconf.messaging.api.messages.from_agent_to_dm.MsgNotifMachineDown;
import net.roboconf.messaging.api.messages.from_dm_to_agent.MsgCmdGatherLogs;
import net.roboconf.messaging.api.messages.from_dm_to_dm.MsgEcho;

/**
 * @author Vincent Zurczak - Linagora
 */
public class DmMessageProcessorTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private TestApplication app;
	private DmMessageProcessor processor;
	private Manager manager;
	private TestManagerWrapper managerWrapper;


	@Before
	public void resetManager() throws Exception {

		// Create the manager
		this.manager = new Manager();
		this.manager.setTargetResolver( new TestTargetResolver());
		this.manager.setMessagingType(MessagingConstants.FACTORY_TEST);
		this.manager.configurationMngr().setWorkingDirectory( this.folder.newFolder());
		this.manager.start();

		// Create the wrapper and complete configuration
		this.managerWrapper = new TestManagerWrapper( this.manager );

		// Reset the processor
		if( this.processor != null )
			this.processor.stopProcessor();

		this.processor = (DmMessageProcessor) this.managerWrapper.getMessagingClient().getMessageProcessor();

		// Create an application
		this.app = new TestApplication();
		this.app.setDirectory( this.folder.newFolder());
		this.managerWrapper.clearManagedApplications();
		this.managerWrapper.addManagedApplication( new ManagedApplication( this.app ));
	}


	@After
	public void stopManager() {
		this.manager.stop();
	}


	@Test
	public void testProcessMsgNotifMachineDown_success() {

		this.app.getMySqlVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		MsgNotifMachineDown msg = new MsgNotifMachineDown( this.app.getName(), this.app.getMySqlVm().getName());

		this.processor.processMessage( msg );
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, this.app.getMySqlVm().getStatus());
	}


	@Test
	public void testProcessMsgNotifMachineDown_invalidApplication() {

		this.app.getMySqlVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		MsgNotifMachineDown msg = new MsgNotifMachineDown( "app-51", this.app.getMySqlVm());

		// The application name is invalid, no update should have been performed
		this.processor.processMessage( msg );
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, this.app.getMySqlVm().getStatus());
	}


	@Test
	public void testProcessMsgNotifMachineDown_invalidInstance() {

		this.app.getMySqlVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		MsgNotifMachineDown msg = new MsgNotifMachineDown( this.app.getName(), "invalid-path" );

		// The application name is invalid, no update should have been performed
		this.processor.processMessage( msg );
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, this.app.getMySqlVm().getStatus());
	}


	@Test
	public void testEchoReception() {

		IDmListener listener = Mockito.mock( IDmListener.class );
		this.manager.listenerAppears( listener );
		Mockito.verify( listener, Mockito.never()).raw( Mockito.anyString());

		MsgEcho msg = new MsgEcho( "hey!" );
		this.processor.processMessage( msg );

		Mockito.verify( listener ).raw( "hey!" );
	}


	@Test
	public void testProcessMsgNotifLogs_success() throws Exception {

		this.processor.tmpDir = this.folder.newFolder().getAbsolutePath();
		Map<String,byte[]> map = new HashMap<>( 2 );
		map.put( "karaf.log", new byte[ 0 ]);
		map.put( "roboconf.log", new byte[ 0 ]);

		File output = new File( this.processor.tmpDir, "roboconf-logs/app/si" );
		Assert.assertFalse( output.exists());

		MsgNotifLogs msg = new MsgNotifLogs( "app", "si", map );
		this.processor.processMessage( msg );

		Assert.assertTrue( output.exists());
		Assert.assertEquals( 2, output.listFiles().length );
		Assert.assertTrue( new File( output, "karaf.log" ).exists());
		Assert.assertTrue( new File( output, "roboconf.log" ).exists());
	}


	@Test
	public void testProcessMsgNotifLogs_failure() throws Exception {

		// It will fail because the root is a file (and not a directory)
		this.processor.tmpDir = this.folder.newFile().getAbsolutePath();
		Map<String,byte[]> map = new HashMap<>( 2 );
		map.put( "karaf.log", new byte[ 0 ]);
		map.put( "roboconf.log", new byte[ 0 ]);

		File output = new File( this.processor.tmpDir, "roboconf-logs/app/si/" );
		Assert.assertFalse( output.exists());

		MsgNotifLogs msg = new MsgNotifLogs( "app", "si", map );
		this.processor.processMessage( msg );

		Assert.assertFalse( output.exists());
	}


	@Test
	public void testProcessMsgNotifInstanceChanged_success() {

		this.app.getMySqlVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		MsgNotifInstanceChanged msg = new MsgNotifInstanceChanged( this.app.getName(), this.app.getMySqlVm());
		msg.setNewStatus( InstanceStatus.STOPPING );

		this.processor.processMessage( msg );
		Assert.assertEquals( InstanceStatus.STOPPING, this.app.getMySqlVm().getStatus());
	}


	@Test
	public void testProcessMsgNotifInstanceChanged_invalidApplication() {

		this.app.getMySqlVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		MsgNotifInstanceChanged msg = new MsgNotifInstanceChanged( "app-53", this.app.getMySqlVm());
		msg.setNewStatus( InstanceStatus.STOPPING );

		// The application name is invalid, no update should have been performed
		this.processor.processMessage( msg );
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, this.app.getMySqlVm().getStatus());
	}


	@Test
	public void testProcessMsgNotifInstanceChanged_invalidInstance() throws Exception {

		this.managerWrapper.addManagedApplication( new ManagedApplication( this.app ));
		this.app.getMySqlVm().setStatus( InstanceStatus.DEPLOYED_STARTED );

		MsgNotifInstanceChanged msg = new MsgNotifInstanceChanged( this.app.getName(), new Instance( "invalid instance" ));
		msg.setNewStatus( InstanceStatus.STOPPING );

		// The instance name is invalid, no update should have been performed
		this.processor.processMessage( msg );
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, this.app.getMySqlVm().getStatus());
	}


	@Test
	public void testProcessMsgNotifInstanceChanged_rootIsNotDeployed() throws Exception {

		this.managerWrapper.addManagedApplication( new ManagedApplication( this.app ));
		this.app.getMySqlVm().setStatus( InstanceStatus.NOT_DEPLOYED );

		MsgNotifInstanceChanged msg = new MsgNotifInstanceChanged( this.app.getName(), this.app.getMySql());
		msg.setNewStatus( InstanceStatus.DEPLOYED_STARTED );

		// The root is not deployed, the change should be dismissed
		this.processor.processMessage( msg );
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, this.app.getMySql().getStatus());
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, this.app.getMySqlVm().getStatus());
	}


	@Test
	public void testProcessMsgNotifInstanceRemoved_success() {

		String instancePath = InstanceHelpers.computeInstancePath( this.app.getMySql());
		int instancesCount = InstanceHelpers.getAllInstances( this.app ).size();

		MsgNotifInstanceRemoved msg = new MsgNotifInstanceRemoved( this.app.getName(), this.app.getMySql());
		Assert.assertNotNull( InstanceHelpers.findInstanceByPath( this.app, instancePath ));

		this.processor.processMessage( msg );
		Assert.assertNull( InstanceHelpers.findInstanceByPath( this.app, instancePath ));
		Assert.assertEquals( instancesCount - 1, InstanceHelpers.getAllInstances( this.app ).size());
	}


	@Test
	public void testProcessMsgNotifInstanceRemoved_invalidApplication() {

		String instancePath = InstanceHelpers.computeInstancePath( this.app.getMySql());
		int instancesCount = InstanceHelpers.getAllInstances( this.app ).size();

		MsgNotifInstanceRemoved msg = new MsgNotifInstanceRemoved( "app-98", this.app.getMySql());
		Assert.assertNotNull( InstanceHelpers.findInstanceByPath( this.app, instancePath ));

		this.processor.processMessage( msg );
		Assert.assertNotNull( InstanceHelpers.findInstanceByPath( this.app, instancePath ));
		Assert.assertEquals( instancesCount, InstanceHelpers.getAllInstances( this.app ).size());
	}


	@Test
	public void testProcessMsgNotifInstanceRemoved_invalidInstance() {

		MsgNotifInstanceRemoved msg = new MsgNotifInstanceRemoved( this.app.getName(), new Instance( "whatever" ));
		int instancesCount = InstanceHelpers.getAllInstances( this.app ).size();

		this.processor.processMessage( msg );
		Assert.assertEquals( instancesCount, InstanceHelpers.getAllInstances( this.app ).size());
	}


	@Test
	public void testProcessMsgNotifInstanceRemoved_scopedInstance() {

		this.app.getWar().getComponent().installerName( Constants.TARGET_INSTALLER );

		int instancesCount = InstanceHelpers.getAllInstances( this.app ).size();
		MsgNotifInstanceRemoved msg = new MsgNotifInstanceRemoved( this.app.getName(), this.app.getWar());

		// Nothing removed
		this.processor.processMessage( msg );
		Assert.assertEquals( instancesCount, InstanceHelpers.getAllInstances( this.app ).size());
	}


	@Test
	public void testMsgNotifHeartbeat_success() {

		this.app.getMySqlVm().setStatus( InstanceStatus.PROBLEM );
		MsgNotifHeartbeat msg = new MsgNotifHeartbeat( this.app.getName(), this.app.getMySqlVm(), "192.168.1.45" );

		this.processor.processMessage( msg );
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, this.app.getMySqlVm().getStatus());
	}


	@Test
	public void testMsgNotifHeartbeat_mustSendStoredMessages() throws Exception {

		this.managerWrapper.configureMessagingForTest();
		this.manager.reconfigure();
		TestClient msgClient = (TestClient) this.managerWrapper.getInternalMessagingClient();

		ManagedApplication ma = this.manager.applicationMngr().findManagedApplicationByName( this.app.getName());
		Assert.assertNotNull( ma );
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, this.app.getMySqlVm().getStatus());
		Assert.assertNull( ma.getScopedInstanceToAwaitingMessages().get( this.app.getMySqlVm()));
		Assert.assertEquals( 0, msgClient.allSentMessages.size());

		// Store messages
		this.manager.messagingMngr().sendMessageSafely( ma, this.app.getMySqlVm(), new MsgCmdGatherLogs());
		this.manager.messagingMngr().sendMessageSafely( ma, this.app.getMySqlVm(), new MsgCmdGatherLogs());
		this.manager.messagingMngr().sendMessageSafely( ma, this.app.getMySqlVm(), new MsgCmdGatherLogs());
		Assert.assertEquals( 3, ma.getScopedInstanceToAwaitingMessages().get( this.app.getMySqlVm()).size());
		Assert.assertEquals( 0, msgClient.allSentMessages.size());

		// The ACK should send them
		MsgNotifHeartbeat msg = new MsgNotifHeartbeat( this.app.getName(), this.app.getMySqlVm(), "192.168.1.45" );
		this.processor.processMessage( msg );

		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, this.app.getMySqlVm().getStatus());
		Assert.assertNull( ma.getScopedInstanceToAwaitingMessages().get( this.app.getMySqlVm()));
		Assert.assertEquals( 3, msgClient.allSentMessages.size());
	}


	@Test
	public void testMsgNotifHeartbeat_invalidApplication() {

		this.app.getMySqlVm().setStatus( InstanceStatus.PROBLEM );
		MsgNotifHeartbeat msg = new MsgNotifHeartbeat( "app-98", this.app.getMySqlVm(), "192.168.1.45" );

		this.processor.processMessage( msg );
		Assert.assertEquals( InstanceStatus.PROBLEM, this.app.getMySqlVm().getStatus());
	}


	@Test
	public void testMsgNotifHeartbeat_invalidInstance() {

		this.app.getMySqlVm().setStatus( InstanceStatus.PROBLEM );
		MsgNotifHeartbeat msg = new MsgNotifHeartbeat( this.app.getName(), new Instance( "unknown" ), "192.168.1.45" );

		this.processor.processMessage( msg );
		Assert.assertEquals( InstanceStatus.PROBLEM, this.app.getMySqlVm().getStatus());
	}


	@Test
	public void testMsg_unknownMessage() {

		Message msg = new Message() {
			private static final long serialVersionUID = 5687202567967616823L;
		};

		this.processor.processMessage( msg );
	}
}
