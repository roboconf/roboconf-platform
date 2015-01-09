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

package net.roboconf.agent.monitoring.internal;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.Properties;

import junit.framework.Assert;
import net.roboconf.agent.monitoring.internal.file.FileHandler;
import net.roboconf.agent.monitoring.internal.nagios.NagiosHandler;
import net.roboconf.agent.monitoring.internal.tests.MyAgentInterface;
import net.roboconf.core.Constants;
import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.runtime.Component;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.core.utils.Utils;
import net.roboconf.messaging.MessagingConstants;
import net.roboconf.messaging.internal.client.MessageServerClientFactory;
import net.roboconf.messaging.internal.client.test.TestClientAgent;
import net.roboconf.messaging.messages.from_agent_to_dm.MsgNotifAutonomic;

import org.junit.Before;
import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class MonitoringTaskTest {

	private TestClientAgent messagingClient;
	private MyAgentInterface agentInterface;


	@Before
	public void initializeMessagingClient() {
		MessageServerClientFactory factory = new MessageServerClientFactory();
		this.messagingClient = (TestClientAgent) factory.createAgentClient( MessagingConstants.FACTORY_TEST );
		this.agentInterface = new MyAgentInterface( this.messagingClient );
	}

	@Test
	public void testExpandString() {
		Properties params = new Properties();
		params.setProperty("firstname", "James");
		params.setProperty("lastname", "Bond");
		String tmpl = "My name is {{lastname}}, {{ firstname }} {{ lastname }}!";
		Assert.assertEquals(
				"My name is Bond, James Bond!",
				(new MonitoringTask(null)).expandString(tmpl, params));

		tmpl = "This is an {{ unknown }} parameter";
		Assert.assertEquals(tmpl, (new MonitoringTask(null)).expandString(tmpl, params));
	}

	@Test
	public void testExtractRuleSections_file() throws Exception {

		File f = TestUtils.findTestFile( "/file-events.conf" );
		String fileContent = Utils.readFileContent( f );
		MonitoringTask task = new MonitoringTask( this.agentInterface );
		this.agentInterface.setRootInstance( new Instance( "root" ));

		List<MonitoringHandler> handlers = task.extractRuleSections( f, fileContent, null );
		Assert.assertEquals( 3, handlers.size());

		MonitoringHandler handler = handlers.get( 0 );
		Assert.assertEquals( FileHandler.class, handler.getClass());
		Assert.assertEquals( "myRuleName-1", handler.getEventId());
		Assert.assertEquals( "/tmp/ta-daaaaa", ((FileHandler) handler).getFileLocation());
		Assert.assertEquals( true, ((FileHandler) handler).isDeleteIfExists());
		Assert.assertEquals( false, ((FileHandler) handler).isNotifyIfNotExists());

		handler = handlers.get( 1 );
		Assert.assertEquals( FileHandler.class, handler.getClass());
		Assert.assertEquals( "myRuleName-2", handler.getEventId());
		Assert.assertEquals( "/tmp/ta-daaaaa-2", ((FileHandler) handler).getFileLocation());
		Assert.assertEquals( false, ((FileHandler) handler).isDeleteIfExists());
		Assert.assertEquals( false, ((FileHandler) handler).isNotifyIfNotExists());

		handler = handlers.get( 2 );
		Assert.assertEquals( FileHandler.class, handler.getClass());
		Assert.assertEquals( "myRuleName", handler.getEventId());
		Assert.assertEquals( "/tmp/a-directory-to-not-delete", ((FileHandler) handler).getFileLocation());
		Assert.assertEquals( false, ((FileHandler) handler).isDeleteIfExists());
		Assert.assertEquals( true, ((FileHandler) handler).isNotifyIfNotExists());
	}


	@Test
	public void testExtractRuleSections_nagios() throws Exception {

		File f = TestUtils.findTestFile( "/nagios-events.conf" );
		String fileContent = Utils.readFileContent( f );
		MonitoringTask task = new MonitoringTask( this.agentInterface );
		this.agentInterface.setRootInstance( new Instance( "root" ));

		List<MonitoringHandler> handlers = task.extractRuleSections( f, fileContent, null );
		Assert.assertEquals( 3, handlers.size());

		MonitoringHandler handler = handlers.get( 0 );
		Assert.assertEquals( NagiosHandler.class, handler.getClass());
		Assert.assertEquals( "myRuleName-80", handler.getEventId());
		Assert.assertNull(((NagiosHandler) handler).getHost());
		Assert.assertEquals( -1, ((NagiosHandler) handler).getPort());

		handler = handlers.get( 1 );
		Assert.assertEquals( NagiosHandler.class, handler.getClass());
		Assert.assertEquals( "myRuleName-2", handler.getEventId());
		Assert.assertEquals( "http://192.168.15.4", ((NagiosHandler) handler).getHost());
		Assert.assertEquals( -1, ((NagiosHandler) handler).getPort());

		handler = handlers.get( 2 );
		Assert.assertEquals( NagiosHandler.class, handler.getClass());
		Assert.assertEquals( "myRuleName-3", handler.getEventId());
		Assert.assertEquals( "http://192.168.15.4", ((NagiosHandler) handler).getHost());
		Assert.assertEquals( 50001, ((NagiosHandler) handler).getPort());
	}


	@Test
	public void testExtractRuleSections_mixed() throws Exception {

		File f = TestUtils.findTestFile( "/mixed-events.conf" );
		String fileContent = Utils.readFileContent( f );
		MonitoringTask task = new MonitoringTask( this.agentInterface );
		this.agentInterface.setRootInstance( new Instance( "root" ));

		List<MonitoringHandler> handlers = task.extractRuleSections( f, fileContent, null );
		Assert.assertEquals( 2, handlers.size());

		MonitoringHandler handler = handlers.get( 0 );
		Assert.assertEquals( NagiosHandler.class, handler.getClass());
		Assert.assertEquals( "myRuleName-nagios", handler.getEventId());
		Assert.assertNull(((NagiosHandler) handler).getHost());
		Assert.assertEquals( -1, ((NagiosHandler) handler).getPort());

		handler = handlers.get( 1 );
		Assert.assertEquals( FileHandler.class, handler.getClass());
		Assert.assertEquals( "myRuleName-for-file", handler.getEventId());
		Assert.assertEquals( "/tmp/a-directory-to-not-delete", ((FileHandler) handler).getFileLocation());
		Assert.assertEquals( false, ((FileHandler) handler).isDeleteIfExists());
		Assert.assertEquals( true, ((FileHandler) handler).isNotifyIfNotExists());
	}

	@Test
	public void testExtractRuleSections_mixedTemplating() throws Exception {

		File f = TestUtils.findTestFile( "/mixed-events-templating.conf" );
		String fileContent = Utils.readFileContent( f );
		MonitoringTask task = new MonitoringTask( this.agentInterface );
		this.agentInterface.setRootInstance( new Instance( "root" ));

		Properties params = new Properties();
		params.load(new FileInputStream(TestUtils.findTestFile( "/mixed-events-templating.properties")));
		List<MonitoringHandler> handlers = task.extractRuleSections( f, fileContent, params );
		Assert.assertEquals( 2, handlers.size());

		MonitoringHandler handler = handlers.get( 0 );
		Assert.assertEquals( NagiosHandler.class, handler.getClass());
		// Test template expansion of "= {{ accept_passive_checks }}"
		// with "accept_passive_checks" property set to 1
		Assert.assertEquals(true,
			((NagiosHandler) handler).getNagiosInstructions().endsWith("= 1"));

		handler = handlers.get( 1 );
		Assert.assertEquals( FileHandler.class, handler.getClass());
		// Test template expansion of "/tmp/{{ a-directory-to-not-delete }}"
		// with "a-directory-to-not-delete" property set to "roboconf"
		Assert.assertEquals( "/tmp/roboconf", ((FileHandler) handler).getFileLocation());
	}

	@Test
	public void testExtractRuleSections_incompleteRule() throws Exception {

		String fileContent = MonitoringTask.RULE_BEGINNING;
		MonitoringTask task = new MonitoringTask( this.agentInterface );
		this.agentInterface.setRootInstance( new Instance( "root" ));

		List<MonitoringHandler> handlers = task.extractRuleSections( new File( "test" ), fileContent, null );
		Assert.assertEquals( 0, handlers.size());
	}


	@Test
	public void testExtractRuleSections_invalidRule() throws Exception {

		String fileContent = MonitoringTask.RULE_BEGINNING + " file event id with spaces]";
		MonitoringTask task = new MonitoringTask( this.agentInterface );
		this.agentInterface.setRootInstance( new Instance( "root" ));

		List<MonitoringHandler> handlers = task.extractRuleSections( new File( "test" ), fileContent, null );
		Assert.assertEquals( 0, handlers.size());
	}


	@Test
	public void testExtractRuleSections_unknownParser() throws Exception {

		String fileContent = MonitoringTask.RULE_BEGINNING + " unknown event-id]\nok";
		MonitoringTask task = new MonitoringTask( this.agentInterface );
		this.agentInterface.setRootInstance( new Instance( "root" ));

		List<MonitoringHandler> handlers = task.extractRuleSections( new File( "test" ), fileContent, null );
		Assert.assertEquals( 0, handlers.size());
	}


	@Test
	public void testWholeChain() throws Exception {

		testTheCommonChain();

		Assert.assertEquals( 1, this.messagingClient.messagesForTheDm.size());
		Assert.assertEquals( MsgNotifAutonomic.class, this.messagingClient.messagesForTheDm.get( 0 ).getClass());

		MsgNotifAutonomic msg = (MsgNotifAutonomic) this.messagingClient.messagesForTheDm.get( 0 );
		Assert.assertEquals( this.agentInterface.getApplicationName(), msg.getApplicationName());
		Assert.assertEquals( "myRuleName", msg.getEventName());
		Assert.assertEquals( this.agentInterface.getRootInstance().getName(), msg.getRootInstanceName());
		Assert.assertTrue( msg.getEventInfo().toLowerCase().contains( "does not exist" ));
	}


	@Test
	public void testWholeChain_messagingError() throws Exception {

		this.messagingClient.failMessageSending.set( true );
		testTheCommonChain();
		Assert.assertEquals( 0, this.messagingClient.messagesForTheDm.size());
	}


	@Test
	public void testWholeChain_noModelYet() throws Exception {

		Assert.assertEquals( 0, this.messagingClient.messagesForTheDm.size());
		MonitoringTask task = new MonitoringTask( this.agentInterface );
		task.run();
		Assert.assertEquals( 0, this.messagingClient.messagesForTheDm.size());
	}


	private void testTheCommonChain() throws Exception {

		// Create a model
		Instance rootInstance = new Instance( "root" ).component( new Component( "Root" ).installerName( Constants.TARGET_INSTALLER ));
		Instance childInstance = new Instance( "child" ).component( new Component( "Child" ).installerName( "whatever" ));
		InstanceHelpers.insertChild( rootInstance, childInstance );
		this.agentInterface.setRootInstance( rootInstance );

		// Create the resources
		File dir = InstanceHelpers.findInstanceDirectoryOnAgent( childInstance, childInstance.getComponent().getInstallerName());
		Utils.deleteFilesRecursively( dir );
		Assert.assertTrue( dir.mkdirs());

		File f = TestUtils.findTestFile( "/file-events.conf" );
		File measureFile = new File( dir, childInstance.getComponent().getName() + ".measures" );

		Assert.assertFalse( measureFile.exists());
		Utils.copyStream( f, measureFile );
		Assert.assertTrue( measureFile.exists());

		// Run the task
		Assert.assertEquals( 0, this.messagingClient.messagesForTheDm.size());
		MonitoringTask task = new MonitoringTask( this.agentInterface );
		task.run();

		Utils.deleteFilesRecursively( dir );
	}
}
