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

package net.roboconf.dm.management;

import java.io.File;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.runtime.EventType;
import net.roboconf.dm.management.api.INotificationMngr;
import net.roboconf.messaging.api.messages.Message;
import net.roboconf.messaging.api.messages.from_dm_to_agent.MsgCmdAddInstance;
import net.roboconf.messaging.api.messages.from_dm_to_agent.MsgCmdRemoveInstance;
import net.roboconf.messaging.api.messages.from_dm_to_agent.MsgCmdSendInstances;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ManagedApplicationTest {

	@Rule
	public final TemporaryFolder folder = new TemporaryFolder();

	private ManagedApplication ma;
	private TestApplication app;


	@Before
	public void initializeMa() throws Exception {

		File f = this.folder.newFolder( "Roboconf_test" );
		this.app = new TestApplication();
		this.app.setDirectory( f );
		this.ma = new ManagedApplication( this.app );
	}


	@Test
	public void testConstructor() throws Exception {
		Assert.assertEquals( this.app, this.ma.getApplication());
	}


	@Test
	public void testShortcuts() {

		Assert.assertEquals( this.app.getName(), this.ma.getName());
		Assert.assertEquals( this.ma.getApplication().getTemplate().getGraphs(), this.ma.getGraphs());
		Assert.assertEquals( this.ma.getApplication().getTemplate().getDirectory(), this.ma.getTemplateDirectory());
	}


	@Test
	public void testStoreAwaitingMessage() {

		Instance rootInstance = new Instance( "root" );
		Assert.assertEquals( 0, this.ma.getScopedInstanceToAwaitingMessages().size());
		this.ma.storeAwaitingMessage( rootInstance, new MsgCmdSendInstances());
		Assert.assertEquals( 1, this.ma.getScopedInstanceToAwaitingMessages().size());

		List<Message> messages = this.ma.getScopedInstanceToAwaitingMessages().get( rootInstance );
		Assert.assertEquals( 1, messages.size());
		Assert.assertEquals( MsgCmdSendInstances.class, messages.get( 0 ).getClass());

		Instance childInstance = new Instance( "child" );
		InstanceHelpers.insertChild( rootInstance, childInstance );
		this.ma.storeAwaitingMessage( childInstance, new MsgCmdAddInstance( childInstance ));
		Assert.assertEquals( 1, this.ma.getScopedInstanceToAwaitingMessages().size());

		Assert.assertEquals( 2, messages.size());
		Assert.assertEquals( MsgCmdSendInstances.class, messages.get( 0 ).getClass());
		Assert.assertEquals( MsgCmdAddInstance.class, messages.get( 1 ).getClass());

		childInstance.setParent( null );
		this.ma.storeAwaitingMessage( childInstance, new MsgCmdRemoveInstance( childInstance ));
		Assert.assertEquals( 2, this.ma.getScopedInstanceToAwaitingMessages().size());

		messages = this.ma.getScopedInstanceToAwaitingMessages().get( childInstance );
		Assert.assertEquals( 1, messages.size());
		Assert.assertEquals( MsgCmdRemoveInstance.class, messages.get( 0 ).getClass());
	}


	@Test
	public void testRemoveAwaitingMessages() {

		Assert.assertEquals( 0, this.ma.getScopedInstanceToAwaitingMessages().size());
		Assert.assertEquals( 0, this.ma.removeAwaitingMessages( new Instance( "whatever" )).size());

		Instance rootInstance = new Instance( "root" );
		this.ma.storeAwaitingMessage( rootInstance, new MsgCmdSendInstances());
		Assert.assertEquals( 1, this.ma.getScopedInstanceToAwaitingMessages().size());
		Assert.assertEquals( 1, this.ma.removeAwaitingMessages( rootInstance ).size());
		Assert.assertEquals( 0, this.ma.getScopedInstanceToAwaitingMessages().size());
		Assert.assertEquals( 0, this.ma.removeAwaitingMessages( rootInstance ).size());
	}


	@Test
	public void testStoreAndRemove() {

		Instance rootInstance = new Instance( "root" );
		Assert.assertEquals( 0, this.ma.getScopedInstanceToAwaitingMessages().size());
		this.ma.storeAwaitingMessage( rootInstance, new MsgCmdSendInstances());
		this.ma.storeAwaitingMessage( rootInstance, new MsgCmdSendInstances());
		Assert.assertEquals( 1, this.ma.getScopedInstanceToAwaitingMessages().size());

		List<Message> messages = this.ma.getScopedInstanceToAwaitingMessages().get( rootInstance );
		Assert.assertEquals( 2, messages.size());
		Assert.assertEquals( MsgCmdSendInstances.class, messages.get( 0 ).getClass());
		Assert.assertEquals( MsgCmdSendInstances.class, messages.get( 1 ).getClass());

		messages = this.ma.removeAwaitingMessages( rootInstance );
		Assert.assertEquals( 2, messages.size());
		Assert.assertEquals( MsgCmdSendInstances.class, messages.get( 0 ).getClass());
		Assert.assertEquals( MsgCmdSendInstances.class, messages.get( 1 ).getClass());
		Assert.assertEquals( 0, this.ma.getScopedInstanceToAwaitingMessages().size());

		this.ma.storeAwaitingMessage( rootInstance, new MsgCmdRemoveInstance( rootInstance ));
		messages = this.ma.getScopedInstanceToAwaitingMessages().get( rootInstance );
		Assert.assertEquals( 1, messages.size());
		Assert.assertEquals( MsgCmdRemoveInstance.class, messages.get( 0 ).getClass());
	}


	@Test
	public void testAcknowledgeHeartBeat() {

		Assert.assertNull( this.app.getMySqlVm().data.get( Instance.RUNNING_FROM ));
		Assert.assertNull( this.app.getMySqlVm().data.get( ManagedApplication.MISSED_HEARTBEATS ));
		this.ma.acknowledgeHeartBeat( this.app.getMySqlVm());
		Assert.assertNull( this.app.getMySqlVm().data.get( ManagedApplication.MISSED_HEARTBEATS ));

		String time = this.app.getMySqlVm().data.get( Instance.RUNNING_FROM );
		Assert.assertNotNull( time );

		this.app.getMySqlVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		this.ma.acknowledgeHeartBeat( this.app.getMySqlVm());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, this.app.getMySqlVm().getStatus());
		Assert.assertNull( this.app.getMySqlVm().data.get( ManagedApplication.MISSED_HEARTBEATS ));

		String otherTime = this.app.getMySqlVm().data.get( Instance.RUNNING_FROM );
		Assert.assertNotNull( otherTime );
		Assert.assertEquals( time, otherTime );

		this.app.getMySqlVm().setStatus( InstanceStatus.PROBLEM );
		this.ma.acknowledgeHeartBeat( this.app.getMySqlVm());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, this.app.getMySqlVm().getStatus());
		Assert.assertNull( this.app.getMySqlVm().data.get( ManagedApplication.MISSED_HEARTBEATS ));

		this.app.getMySqlVm().setStatus( InstanceStatus.PROBLEM );
		this.app.getMySqlVm().data.put( ManagedApplication.MISSED_HEARTBEATS, "5" );
		this.ma.acknowledgeHeartBeat( this.app.getMySqlVm());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, this.app.getMySqlVm().getStatus());
		Assert.assertNull( this.app.getMySqlVm().data.get( ManagedApplication.MISSED_HEARTBEATS ));
	}


	@Test
	public void testCheckStates() {

		INotificationMngr notificationMngr = Mockito.mock( INotificationMngr.class );

		Assert.assertNull( this.app.getMySqlVm().data.get( ManagedApplication.MISSED_HEARTBEATS ));
		this.app.getMySqlVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		this.ma.checkStates( notificationMngr );
		Assert.assertEquals( "1", this.app.getMySqlVm().data.get( ManagedApplication.MISSED_HEARTBEATS ));
		Mockito.verifyZeroInteractions( notificationMngr );

		this.ma.acknowledgeHeartBeat( this.app.getMySqlVm());
		Assert.assertNull( this.app.getMySqlVm().data.get( ManagedApplication.MISSED_HEARTBEATS ));

		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, this.app.getMySqlVm().getStatus());
		for( int i=0; i<=ManagedApplication.THRESHOLD; i++ ) {
			this.ma.checkStates( notificationMngr );
			Assert.assertEquals(
					String.valueOf( i ),
					String.valueOf( i+1 ),
					this.app.getMySqlVm().data.get( ManagedApplication.MISSED_HEARTBEATS ));
		}

		Assert.assertEquals( InstanceStatus.PROBLEM, this.app.getMySqlVm().getStatus());
		Mockito.verify( notificationMngr ).instance( this.app.getMySqlVm(), this.app, EventType.CHANGED );
		Mockito.reset( notificationMngr );

		this.app.getMySqlVm().setStatus( InstanceStatus.UNDEPLOYING );
		this.ma.checkStates( notificationMngr );
		Assert.assertNull( this.app.getMySqlVm().data.get( ManagedApplication.MISSED_HEARTBEATS ));
		Mockito.verifyZeroInteractions( notificationMngr );

		this.app.getMySqlVm().setStatus( InstanceStatus.DEPLOYING );
		this.ma.checkStates( notificationMngr );
		Assert.assertNull( this.app.getMySqlVm().data.get( ManagedApplication.MISSED_HEARTBEATS ));
		Mockito.verifyZeroInteractions( notificationMngr );

		this.app.getMySqlVm().setStatus( InstanceStatus.NOT_DEPLOYED );
		this.ma.checkStates( notificationMngr );
		Assert.assertNull( this.app.getMySqlVm().data.get( ManagedApplication.MISSED_HEARTBEATS ));
		Mockito.verifyZeroInteractions( notificationMngr );
	}
}
