/**
 * Copyright 2014 Linagora, Université Joseph Fourier, Floralis
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

import junit.framework.Assert;
import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.dm.internal.test.TestTargetResolver;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.messaging.MessagingConstants;
import net.roboconf.messaging.messages.Message;
import net.roboconf.messaging.messages.from_agent_to_dm.MsgNotifHeartbeat;
import net.roboconf.messaging.messages.from_agent_to_dm.MsgNotifInstanceChanged;
import net.roboconf.messaging.messages.from_agent_to_dm.MsgNotifInstanceRemoved;
import net.roboconf.messaging.messages.from_agent_to_dm.MsgNotifMachineDown;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author Vincent Zurczak - Linagora
 */
public class DmMessageProcessorTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private TestApplication app;
	private DmMessageProcessor processor;
	private Manager manager;


	@Before
	public void resetManager() throws Exception {

		this.manager = new Manager();
		this.manager.setTargetResolver( new TestTargetResolver());
		this.manager.setMessagingFactoryType( MessagingConstants.FACTORY_TEST );
		this.manager.setConfigurationDirectoryLocation( this.folder.newFolder().getAbsolutePath());
		this.manager.start();

		this.app = new TestApplication();
		if( this.processor != null )
			this.processor.stopProcessor();

		this.processor = (DmMessageProcessor) this.manager.getMessagingClient().getMessageProcessor();
		this.manager.getAppNameToManagedApplication().clear();
		this.manager.getAppNameToManagedApplication().put( this.app.getName(), new ManagedApplication( this.app, null ));
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
	public void testProcessMsgNotifInstanceChanged_invalidInstance() {

		this.manager.getAppNameToManagedApplication().put( this.app.getName(), new ManagedApplication( this.app, null ));
		this.app.getMySqlVm().setStatus( InstanceStatus.DEPLOYED_STARTED );

		MsgNotifInstanceChanged msg = new MsgNotifInstanceChanged( this.app.getName(), new Instance( "invalid instance" ));
		msg.setNewStatus( InstanceStatus.STOPPING );

		// The instance name is invalid, no update should have been performed
		this.processor.processMessage( msg );
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, this.app.getMySqlVm().getStatus());
	}


	@Test
	public void testProcessMsgNotifInstanceChanged_rootIsNotDeployed() {

		this.manager.getAppNameToManagedApplication().put( this.app.getName(), new ManagedApplication( this.app, null ));
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
	public void testMsgNotifHeartbeat_success() {

		this.app.getMySqlVm().setStatus( InstanceStatus.PROBLEM );
		MsgNotifHeartbeat msg = new MsgNotifHeartbeat( this.app.getName(), this.app.getMySqlVm(), "192.168.1.45" );

		this.processor.processMessage( msg );
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, this.app.getMySqlVm().getStatus());
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
