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

package net.roboconf.dm.internal.management;

import java.io.IOException;
import java.util.List;

import junit.framework.Assert;
import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.model.runtime.Application;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.core.model.runtime.Instance.InstanceStatus;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.messaging.MessagingConstants;
import net.roboconf.messaging.internal.client.test.TestClientDm;
import net.roboconf.messaging.messages.Message;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdRemoveInstance;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdSendInstances;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class CheckerMessagesTaskTest {

	private Manager manager;


	@Before
	public void resetManager() {
		this.manager = new Manager();
		this.manager.setMessagingFactoryType( MessagingConstants.FACTORY_TEST );
		this.manager.start();
	}


	@After
	public void cleanManager() {
		this.manager.stop();
	}


	@Test
	public void testRun_noApplication() {

		TestClientDm client = new TestClientDm();
		CheckerMessagesTask task = new CheckerMessagesTask( this.manager, client );

		Assert.assertEquals( 0, this.manager.getAppNameToManagedApplication().size());
		Assert.assertEquals( 0, client.sentMessages.size());
		task.run();
		Assert.assertEquals( 0, client.sentMessages.size());
	}


	@Test
	public void testRun_ioException() {

		TestApplication app = new TestApplication();
		app.getMySqlVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		app.getTomcatVm().setStatus( InstanceStatus.DEPLOYED_STARTED );

		ManagedApplication ma = new ManagedApplication( app, null );
		this.manager.getAppNameToManagedApplication().put( app.getName(), ma );

		TestClientDm client = new TestClientDm() {
			@Override
			public void sendMessageToAgent( Application application, Instance instance, Message message ) throws IOException {
				throw new IOException( "For test purpose..." );
			}
		};

		CheckerMessagesTask task = new CheckerMessagesTask( this.manager, client );

		Assert.assertEquals( 0, ma.getRootInstanceToAwaitingMessages().size());
		ma.storeAwaitingMessage( app.getMySqlVm(), new MsgCmdSendInstances());
		ma.storeAwaitingMessage( app.getMySqlVm(), new MsgCmdRemoveInstance( "/whatever" ));

		Assert.assertEquals( 1, ma.getRootInstanceToAwaitingMessages().size());
		Assert.assertEquals( 2, ma.getRootInstanceToAwaitingMessages().get( app.getMySqlVm()).size());
		task.run();
		Assert.assertEquals( 1, ma.getRootInstanceToAwaitingMessages().size());

		List<Message> messages = ma.getRootInstanceToAwaitingMessages().get( app.getMySqlVm());
		Assert.assertEquals( 2, messages.size());
		Assert.assertEquals( MsgCmdSendInstances.class, messages.get( 0 ).getClass());
		Assert.assertEquals( MsgCmdRemoveInstance.class, messages.get( 1 ).getClass());
	}


	@Test
	public void testRun_normal_rootIsStarted() {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app, null );
		this.manager.getAppNameToManagedApplication().put( app.getName(), ma );

		TestClientDm client = new TestClientDm();
		CheckerMessagesTask task = new CheckerMessagesTask( this.manager, client );

		app.getMySqlVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		Assert.assertEquals( 0, ma.getRootInstanceToAwaitingMessages().size());
		ma.storeAwaitingMessage( app.getMySqlVm(), new MsgCmdSendInstances());
		ma.storeAwaitingMessage( app.getMySqlVm(), new MsgCmdRemoveInstance( "/whatever" ));

		Assert.assertEquals( 1, ma.getRootInstanceToAwaitingMessages().size());
		Assert.assertEquals( 2, ma.getRootInstanceToAwaitingMessages().get( app.getMySqlVm()).size());
		task.run();
		Assert.assertNull( ma.getRootInstanceToAwaitingMessages().get( app.getMySqlVm()));
		Assert.assertEquals( 0, ma.getRootInstanceToAwaitingMessages().size());
	}


	@Test
	public void testRun_rootDeploying() {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app, null );
		this.manager.getAppNameToManagedApplication().put( app.getName(), ma );

		TestClientDm client = new TestClientDm();
		CheckerMessagesTask task = new CheckerMessagesTask( this.manager, client );

		app.getMySqlVm().setStatus( InstanceStatus.DEPLOYING );
		Assert.assertEquals( 0, ma.getRootInstanceToAwaitingMessages().size());
		ma.storeAwaitingMessage( app.getMySqlVm(), new MsgCmdSendInstances());
		ma.storeAwaitingMessage( app.getMySqlVm(), new MsgCmdRemoveInstance( "/whatever" ));

		// Messages are still there
		Assert.assertEquals( 1, ma.getRootInstanceToAwaitingMessages().size());
		Assert.assertEquals( 2, ma.getRootInstanceToAwaitingMessages().get( app.getMySqlVm()).size());
		task.run();
		Assert.assertEquals( 1, ma.getRootInstanceToAwaitingMessages().size());
		Assert.assertEquals( 2, ma.getRootInstanceToAwaitingMessages().get( app.getMySqlVm()).size());
	}


	@Test
	public void testRun_rootNotStarted() {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app, null );
		this.manager.getAppNameToManagedApplication().put( app.getName(), ma );

		TestClientDm client = new TestClientDm();
		CheckerMessagesTask task = new CheckerMessagesTask( this.manager, client );

		app.getMySqlVm().setStatus( InstanceStatus.DEPLOYING );
		Assert.assertEquals( 0, ma.getRootInstanceToAwaitingMessages().size());
		ma.storeAwaitingMessage( app.getMySqlVm(), new MsgCmdSendInstances());
		ma.storeAwaitingMessage( app.getMySqlVm(), new MsgCmdRemoveInstance( "/whatever" ));

		// Messages are still there
		Assert.assertEquals( 1, ma.getRootInstanceToAwaitingMessages().size());
		Assert.assertEquals( 2, ma.getRootInstanceToAwaitingMessages().get( app.getMySqlVm()).size());
		task.run();
		Assert.assertEquals( 1, ma.getRootInstanceToAwaitingMessages().size());
		Assert.assertEquals( 2, ma.getRootInstanceToAwaitingMessages().get( app.getMySqlVm()).size());
	}
}
