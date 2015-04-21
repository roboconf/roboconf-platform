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

package net.roboconf.dm.internal.tasks;

import java.io.IOException;
import java.util.List;

import junit.framework.Assert;
import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.dm.internal.delegates.ApplicationMngrDelegate;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.messaging.internal.client.test.TestClientDm;
import net.roboconf.messaging.messages.Message;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdRemoveInstance;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdSendInstances;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author Vincent Zurczak - Linagora
 */
public class CheckerMessagesTaskTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private ApplicationMngrDelegate appManager;
	private TestApplication app;
	private ManagedApplication ma;


	@Before
	public void resetManager() throws Exception {

		this.app = new TestApplication();
		this.ma = new ManagedApplication( this.app );

		this.appManager = new ApplicationMngrDelegate();
		this.appManager.getNameToManagedApplication().put( this.app.getName(), this.ma );
	}


	@Test
	public void testRun_noApplication() {

		TestClientDm client = new TestClientDm();
		CheckerMessagesTask task = new CheckerMessagesTask( this.appManager, client );
		this.appManager.getNameToManagedApplication().clear();

		Assert.assertEquals( 0, this.appManager.getNameToManagedApplication().size());
		Assert.assertEquals( 0, client.sentMessages.size());
		task.run();
		Assert.assertEquals( 0, client.sentMessages.size());
	}


	@Test
	public void testRun_ioException() {

		this.app.getMySqlVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		this.app.getTomcatVm().setStatus( InstanceStatus.DEPLOYED_STARTED );

		TestClientDm client = new TestClientDm() {
			@Override
			public void sendMessageToAgent( Application application, Instance instance, Message message ) throws IOException {
				throw new IOException( "For test purpose..." );
			}
		};

		CheckerMessagesTask task = new CheckerMessagesTask( this.appManager, client );

		Assert.assertEquals( 0, this.ma.getScopedInstanceToAwaitingMessages().size());
		this.ma.storeAwaitingMessage( this.app.getMySqlVm(), new MsgCmdSendInstances());
		this.ma.storeAwaitingMessage( this.app.getMySqlVm(), new MsgCmdRemoveInstance( "/whatever" ));

		Assert.assertEquals( 1, this.ma.getScopedInstanceToAwaitingMessages().size());
		Assert.assertEquals( 2, this.ma.getScopedInstanceToAwaitingMessages().get( this.app.getMySqlVm()).size());
		task.run();
		Assert.assertEquals( 1, this.ma.getScopedInstanceToAwaitingMessages().size());

		List<Message> messages = this.ma.getScopedInstanceToAwaitingMessages().get( this.app.getMySqlVm());
		Assert.assertEquals( 2, messages.size());
		Assert.assertEquals( MsgCmdSendInstances.class, messages.get( 0 ).getClass());
		Assert.assertEquals( MsgCmdRemoveInstance.class, messages.get( 1 ).getClass());
	}


	@Test
	public void testRun_normal_rootIsStarted() {

		TestClientDm client = new TestClientDm();
		CheckerMessagesTask task = new CheckerMessagesTask( this.appManager, client );

		this.app.getMySqlVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		Assert.assertEquals( 0, this.ma.getScopedInstanceToAwaitingMessages().size());
		this.ma.storeAwaitingMessage( this.app.getMySqlVm(), new MsgCmdSendInstances());
		this.ma.storeAwaitingMessage( this.app.getMySqlVm(), new MsgCmdRemoveInstance( "/whatever" ));

		Assert.assertEquals( 1, this.ma.getScopedInstanceToAwaitingMessages().size());
		Assert.assertEquals( 2, this.ma.getScopedInstanceToAwaitingMessages().get( this.app.getMySqlVm()).size());
		task.run();
		Assert.assertNull( this.ma.getScopedInstanceToAwaitingMessages().get( this.app.getMySqlVm()));
		Assert.assertEquals( 0, this.ma.getScopedInstanceToAwaitingMessages().size());
	}


	@Test
	public void testRun_rootDeploying() {

		TestClientDm client = new TestClientDm();
		CheckerMessagesTask task = new CheckerMessagesTask( this.appManager, client );

		this.app.getMySqlVm().setStatus( InstanceStatus.DEPLOYING );
		Assert.assertEquals( 0, this.ma.getScopedInstanceToAwaitingMessages().size());
		this.ma.storeAwaitingMessage( this.app.getMySqlVm(), new MsgCmdSendInstances());
		this.ma.storeAwaitingMessage( this.app.getMySqlVm(), new MsgCmdRemoveInstance( "/whatever" ));

		// Messages are still there
		Assert.assertEquals( 1, this.ma.getScopedInstanceToAwaitingMessages().size());
		Assert.assertEquals( 2, this.ma.getScopedInstanceToAwaitingMessages().get( this.app.getMySqlVm()).size());
		task.run();
		Assert.assertEquals( 1, this.ma.getScopedInstanceToAwaitingMessages().size());
		Assert.assertEquals( 2, this.ma.getScopedInstanceToAwaitingMessages().get( this.app.getMySqlVm()).size());
	}


	@Test
	public void testRun_rootNotStarted() {

		TestClientDm client = new TestClientDm();
		CheckerMessagesTask task = new CheckerMessagesTask( this.appManager, client );

		this.app.getMySqlVm().setStatus( InstanceStatus.DEPLOYING );
		Assert.assertEquals( 0, this.ma.getScopedInstanceToAwaitingMessages().size());
		this.ma.storeAwaitingMessage( this.app.getMySqlVm(), new MsgCmdSendInstances());
		this.ma.storeAwaitingMessage( this.app.getMySqlVm(), new MsgCmdRemoveInstance( "/whatever" ));

		// Messages are still there
		Assert.assertEquals( 1, this.ma.getScopedInstanceToAwaitingMessages().size());
		Assert.assertEquals( 2, this.ma.getScopedInstanceToAwaitingMessages().get( this.app.getMySqlVm()).size());
		task.run();
		Assert.assertEquals( 1, this.ma.getScopedInstanceToAwaitingMessages().size());
		Assert.assertEquals( 2, this.ma.getScopedInstanceToAwaitingMessages().get( this.app.getMySqlVm()).size());
	}
}
