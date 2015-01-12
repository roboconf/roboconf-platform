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

package net.roboconf.dm.internal.autonomic;

import java.io.File;
import java.util.Collection;

import junit.framework.Assert;
import net.roboconf.core.Constants;
import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.internal.environment.messaging.DmMessageProcessor;
import net.roboconf.dm.internal.test.TestTargetResolver;
import net.roboconf.dm.internal.utils.ConfigurationUtils;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.messaging.MessagingConstants;
import net.roboconf.messaging.messages.Message;
import net.roboconf.messaging.messages.from_agent_to_dm.MsgNotifAutonomic;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author Vincent Zurczak - Linagora
 */
public class DmMessageProcessorForAutonomicTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private TestApplication app;
	private DmMessageProcessor processor;
	private Manager manager;


	@Before
	public void resetManager() throws Exception {
		File dir = this.folder.newFolder();

		this.manager = new Manager();
		this.manager.setTargetResolver( new TestTargetResolver());
		this.manager.setMessagingFactoryType( MessagingConstants.FACTORY_TEST );
		this.manager.setConfigurationDirectoryLocation( dir.getAbsolutePath());
		this.manager.start();

		this.app = new TestApplication();
		if( this.processor != null )
			this.processor.stopProcessor();

		this.processor = (DmMessageProcessor) this.manager.getMessagingClient().getMessageProcessor();
		this.manager.getAppNameToManagedApplication().clear();

		File appDirectory = ConfigurationUtils.findApplicationdirectory( this.app.getName(), dir );
		Assert.assertTrue( appDirectory.mkdirs());
		this.manager.getAppNameToManagedApplication().put( this.app.getName(), new ManagedApplication( this.app, appDirectory ));
	}


	@After
	public void stopManager() {
		this.manager.stop();
	}


	@Test
	public void testAutonomic() throws Exception {

		// Copy resources
		ManagedApplication ma = this.manager.getAppNameToManagedApplication().get( this.app.getName());
		Assert.assertNotNull( ma );

		File dir = new File( ma.getApplicationFilesDirectory(), Constants.PROJECT_DIR_AUTONOMIC );
		Assert.assertTrue( dir.mkdirs());

		File targetFile = new File( dir, Constants.FILE_RULES );
		File sourceFile = TestUtils.findTestFile( "/autonomic/rules.cfg" );
		Utils.copyStream( sourceFile, targetFile );
		Assert.assertTrue( targetFile.exists());

		// Get some information about the application
		int instanceCount = InstanceHelpers.getAllInstances( this.app ).size();

		// Simulate a first message to delete an instance
		this.processor.processMessage( newMessage( "peaceful" ));
		Assert.assertEquals( instanceCount, InstanceHelpers.getAllInstances( this.app ).size());

		// Now, send an email
		this.processor.processMessage( newMessage( "up" ));
		Assert.assertEquals( instanceCount, InstanceHelpers.getAllInstances( this.app ).size());

		// Add a WAR
		this.processor.processMessage( newMessage( "loaded" ));
		Assert.assertEquals( instanceCount + 3, InstanceHelpers.getAllInstances( this.app ).size());

		this.processor.processMessage( newMessage( "loaded" ));
		Assert.assertEquals( instanceCount + 6, InstanceHelpers.getAllInstances( this.app ).size());

		this.processor.processMessage( newMessage( "peaceful" ));
		Assert.assertEquals( instanceCount + 3, InstanceHelpers.getAllInstances( this.app ).size());

		this.processor.processMessage( newMessage( "loaded" ));
		Assert.assertEquals( instanceCount + 6, InstanceHelpers.getAllInstances( this.app ).size());

		this.processor.processMessage( newMessage( "loaded" ));
		Assert.assertEquals( instanceCount + 9, InstanceHelpers.getAllInstances( this.app ).size());

		// Log something
		this.processor.processMessage( newMessage( "log" ));
		Assert.assertEquals( instanceCount + 9, InstanceHelpers.getAllInstances( this.app ).size());

		this.processor.processMessage( newMessage( "unknown event" ));
		Assert.assertEquals( instanceCount + 9, InstanceHelpers.getAllInstances( this.app ).size());

		// Reduce the number of instances
		this.processor.processMessage( newMessage( "peaceful" ));
		Assert.assertEquals( instanceCount + 6, InstanceHelpers.getAllInstances( this.app ).size());

		this.processor.processMessage( newMessage( "peaceful" ));
		Assert.assertEquals( instanceCount + 3, InstanceHelpers.getAllInstances( this.app ).size());

		this.processor.processMessage( newMessage( "peaceful" ));
		Assert.assertEquals( instanceCount, InstanceHelpers.getAllInstances( this.app ).size());

		this.processor.processMessage( newMessage( "peaceful" ));
		Assert.assertEquals( instanceCount, InstanceHelpers.getAllInstances( this.app ).size());

		// Process a message that does not come from a valid instance
		Message msg = new MsgNotifAutonomic( this.app.getName(), "invalid", "up", "we do not care" );
		this.processor.processMessage( msg );
		Assert.assertEquals( instanceCount, InstanceHelpers.getAllInstances( this.app ).size());

		// We must have the initial instances
		Collection<Instance> allInstances = InstanceHelpers.getAllInstances( this.app );
		Assert.assertTrue( allInstances.contains( this.app.getMySqlVm()));
		Assert.assertTrue( allInstances.contains( this.app.getMySql()));
		Assert.assertTrue( allInstances.contains( this.app.getTomcatVm()));
		Assert.assertTrue( allInstances.contains( this.app.getTomcat()));
		Assert.assertTrue( allInstances.contains( this.app.getWar()));
	}


	private Message newMessage( String eventId ) {
		return new MsgNotifAutonomic( this.app.getName(), this.app.getTomcatVm().getName(), eventId, "we do not care" );
	}
}
