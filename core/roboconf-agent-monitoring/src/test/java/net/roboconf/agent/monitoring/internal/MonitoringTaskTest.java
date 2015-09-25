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

package net.roboconf.agent.monitoring.internal;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import junit.framework.Assert;
import net.roboconf.agent.monitoring.api.IMonitoringHandler;
import net.roboconf.agent.monitoring.internal.MonitoringTask.MonitoringHandlerRun;
import net.roboconf.agent.monitoring.internal.file.FileHandler;
import net.roboconf.agent.monitoring.internal.nagios.NagiosHandler;
import net.roboconf.agent.monitoring.internal.rest.RestHandler;
import net.roboconf.agent.monitoring.internal.tests.MyAgentInterface;
import net.roboconf.core.Constants;
import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.beans.Component;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.utils.Utils;
import net.roboconf.messaging.api.internal.client.test.TestClientAgent;
import net.roboconf.messaging.api.internal.client.test.TestClientFactory;
import net.roboconf.messaging.api.messages.from_agent_to_dm.MsgNotifAutonomic;

import org.junit.Before;
import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class MonitoringTaskTest {

	private static final List<IMonitoringHandler> HANDLERS = new ArrayList<> ();
	static {
		HANDLERS.add(  new FileHandler());
		HANDLERS.add(  new NagiosHandler());
		HANDLERS.add(  new RestHandler());
	}

	private TestClientAgent messagingClient;
	private MyAgentInterface agentInterface;


	@Before
	public void initializeMessagingClient() {
		this.messagingClient = (TestClientAgent) new TestClientFactory().createAgentClient(null);
		this.agentInterface = new MyAgentInterface( this.messagingClient );
	}


	@Test
	public void testExtractRuleSections_noSection() throws Exception {

		String fileContent = "";
		MonitoringTask task = new MonitoringTask( this.agentInterface, HANDLERS );
		this.agentInterface.setScopedInstance( new Instance( "root" ));

		List<MonitoringHandlerRun> handlers = task.extractRuleSections( new File( "whatever" ), fileContent, null );
		Assert.assertEquals( 0, handlers.size());
	}


	@Test
	public void testExtractRuleSections_file() throws Exception {

		File f = TestUtils.findTestFile( "/file-events.conf" );
		String fileContent = Utils.readFileContent( f );
		MonitoringTask task = new MonitoringTask( this.agentInterface, HANDLERS );
		this.agentInterface.setScopedInstance( new Instance( "root" ));

		List<MonitoringHandlerRun> handlers = task.extractRuleSections( f, fileContent, null );
		Assert.assertEquals( 3, handlers.size());

		MonitoringHandlerRun bean = handlers.get( 0 );
		Assert.assertEquals( "file", bean.handlerName );
		Assert.assertEquals( "myRuleName-1", bean.eventId );
		Assert.assertTrue( bean.rawRulesText.contains( "/tmp/ta-daaaaa" ));

		bean = handlers.get( 1 );
		Assert.assertEquals( "file", bean.handlerName );
		Assert.assertEquals( "myRuleName-2", bean.eventId );
		Assert.assertTrue( bean.rawRulesText.contains( "/tmp/ta-daaaaa-2" ));

		bean = handlers.get( 2 );
		Assert.assertEquals( "file", bean.handlerName );
		Assert.assertEquals( "myRuleName", bean.eventId );
		Assert.assertTrue( bean.rawRulesText.contains( "/tmp/a-directory-to-not-delete" ));
	}


	@Test
	public void testExtractRuleSections_nagios() throws Exception {

		File f = TestUtils.findTestFile( "/nagios-events.conf" );
		String fileContent = Utils.readFileContent( f );
		MonitoringTask task = new MonitoringTask( this.agentInterface, HANDLERS );
		this.agentInterface.setScopedInstance( new Instance( "root" ));

		List<MonitoringHandlerRun> handlers = task.extractRuleSections( f, fileContent, null );
		Assert.assertEquals( 3, handlers.size());

		MonitoringHandlerRun bean = handlers.get( 0 );
		Assert.assertEquals( "nagios", bean.handlerName );
		Assert.assertEquals( "myRuleName-80", bean.eventId );
		Assert.assertTrue( bean.rawRulesText.contains( "host_name accept_passive_checks acknowledged" ));

		bean = handlers.get( 1 );
		Assert.assertEquals( "nagios", bean.handlerName );
		Assert.assertEquals( "myRuleName-2", bean.eventId );
		Assert.assertTrue( bean.rawRulesText.contains( "http://192.168.15.4" ));

		bean = handlers.get( 2 );
		Assert.assertEquals( "nagios", bean.handlerName );
		Assert.assertEquals( "myRuleName-3", bean.eventId );
		Assert.assertTrue( bean.rawRulesText.contains( "http://192.168.15.4:50001" ));
	}


	@Test
	public void testExtractRuleSections_rest() throws Exception {

		File f = TestUtils.findTestFile( "/rest-events.conf" );
		String fileContent = Utils.readFileContent( f );
		MonitoringTask task = new MonitoringTask( this.agentInterface, HANDLERS );
		this.agentInterface.setScopedInstance( new Instance( "root" ));

		List<MonitoringHandlerRun> handlers = task.extractRuleSections( f, fileContent, null );
		Assert.assertEquals( 1, handlers.size());

		MonitoringHandlerRun bean = handlers.get( 0 );
		Assert.assertEquals( "rest", bean.handlerName );
		Assert.assertEquals( "myRuleName-1", bean.eventId );
		Assert.assertTrue( bean.rawRulesText.contains( "http://google.fr" ));
	}


	@Test
	public void testExtractRuleSections_mixed() throws Exception {

		File f = TestUtils.findTestFile( "/mixed-events.conf" );
		String fileContent = Utils.readFileContent( f );
		MonitoringTask task = new MonitoringTask( this.agentInterface, HANDLERS );
		this.agentInterface.setScopedInstance( new Instance( "root" ));

		List<MonitoringHandlerRun> handlers = task.extractRuleSections( f, fileContent, null );
		Assert.assertEquals( 2, handlers.size());

		MonitoringHandlerRun bean = handlers.get( 0 );
		Assert.assertEquals( "nagios", bean.handlerName );
		Assert.assertEquals( "myRuleName-nagios", bean.eventId );
		Assert.assertTrue( bean.rawRulesText.contains( "Columns: host_name accept_passive_checks acknowledged" ));

		bean = handlers.get( 1 );
		Assert.assertEquals( "file", bean.handlerName );
		Assert.assertEquals( "myRuleName-for-file", bean.eventId );
		Assert.assertTrue( bean.rawRulesText.contains( "/tmp/a-directory-to-not-delete" ));
	}


	@Test
	public void testExtractRuleSections_mixedTemplating() throws Exception {

		File f = TestUtils.findTestFile( "/mixed-events-templating.conf" );
		String fileContent = Utils.readFileContent( f );
		MonitoringTask task = new MonitoringTask( this.agentInterface, HANDLERS );
		this.agentInterface.setScopedInstance( new Instance( "root" ));

		File propertiesFile = TestUtils.findTestFile( "/mixed-events-templating.properties" );
		Assert.assertTrue( propertiesFile.exists());
		Properties params = Utils.readPropertiesFile( propertiesFile );

		List<MonitoringHandlerRun> handlers = task.extractRuleSections( f, fileContent, params );
		Assert.assertEquals( 2, handlers.size());

		// Test template expansion of "= {{ accept_passive_checks }}"
		// with "accept_passive_checks" property set to 1
		MonitoringHandlerRun bean = handlers.get( 0 );
		Assert.assertEquals( "nagios", bean.handlerName );
		Assert.assertTrue( bean.rawRulesText.endsWith( "= 1" ));

		// Test template expansion of "/tmp/{{ a-directory-to-not-delete }}"
		// with "a-directory-to-not-delete" property set to "roboconf"
		bean = handlers.get( 1 );
		Assert.assertEquals( "file", bean.handlerName );
		Assert.assertTrue( bean.rawRulesText.contains( "/tmp/roboconf" ));
	}


	@Test
	public void testExtractRuleSections_incompleteRule() throws Exception {

		String fileContent = MonitoringTask.RULE_BEGINNING;
		MonitoringTask task = new MonitoringTask( this.agentInterface, HANDLERS );
		this.agentInterface.setScopedInstance( new Instance( "root" ));

		List<MonitoringHandlerRun> handlers = task.extractRuleSections( new File( "test" ), fileContent, null );
		Assert.assertEquals( 0, handlers.size());
	}


	@Test
	public void testExtractRuleSections_invalidRule() throws Exception {

		String fileContent = MonitoringTask.RULE_BEGINNING + " file event id with spaces]";
		MonitoringTask task = new MonitoringTask( this.agentInterface, HANDLERS );
		this.agentInterface.setScopedInstance( new Instance( "root" ));

		List<MonitoringHandlerRun> handlers = task.extractRuleSections( new File( "test" ), fileContent, null );
		Assert.assertEquals( 0, handlers.size());
	}


	@Test
	public void testExtractRuleSections_unknownParser() throws Exception {

		String fileContent = MonitoringTask.RULE_BEGINNING + " unknown event-id]\nok";
		MonitoringTask task = new MonitoringTask( this.agentInterface, HANDLERS );
		this.agentInterface.setScopedInstance( new Instance( "root" ));

		List<MonitoringHandlerRun> handlers = task.extractRuleSections( new File( "test" ), fileContent, null );
		Assert.assertEquals( 1, handlers.size());

		MonitoringHandlerRun bean = handlers.get( 0 );
		Assert.assertEquals( "unknown", bean.handlerName );
		Assert.assertEquals( "event-id", bean.eventId );
		Assert.assertEquals( "ok", bean.rawRulesText );
	}


	@Test
	public void testWholeChain_instancesStarted() throws Exception {

		testTheCommonChain( InstanceStatus.DEPLOYED_STARTED, "/file-events.conf" );

		Assert.assertEquals( 1, this.messagingClient.messagesForTheDm.size());
		Assert.assertEquals( MsgNotifAutonomic.class, this.messagingClient.messagesForTheDm.get( 0 ).getClass());

		MsgNotifAutonomic msg = (MsgNotifAutonomic) this.messagingClient.messagesForTheDm.get( 0 );
		Assert.assertEquals( this.agentInterface.getApplicationName(), msg.getApplicationName());
		Assert.assertEquals( "myRuleName", msg.getEventId());
		Assert.assertEquals( "/" + this.agentInterface.getScopedInstance().getName(), msg.getScopedInstancePath());
		Assert.assertTrue( msg.getEventInfo().toLowerCase().contains( "does not exist" ));
	}


	@Test
	public void testWholeChain_instancesNotStarted() throws Exception {

		testTheCommonChain( InstanceStatus.DEPLOYED_STOPPED, "/file-events.conf" );
		Assert.assertEquals( 0, this.messagingClient.messagesForTheDm.size());
	}


	@Test
	public void testWholeChain_noHandler() throws Exception {

		testTheCommonChain( InstanceStatus.DEPLOYED_STARTED, "/unknown-events.conf" );
		Assert.assertEquals( 0, this.messagingClient.messagesForTheDm.size());
	}


	@Test
	public void testWholeChain_messagingError() throws Exception {

		this.messagingClient.failMessageSending.set( true );
		testTheCommonChain( InstanceStatus.DEPLOYED_STARTED, "/file-events.conf" );
		Assert.assertEquals( 0, this.messagingClient.messagesForTheDm.size());
	}


	@Test
	public void testWholeChain_noModelYet() throws Exception {

		Assert.assertEquals( 0, this.messagingClient.messagesForTheDm.size());
		MonitoringTask task = new MonitoringTask( this.agentInterface, HANDLERS );
		task.run();
		Assert.assertEquals( 0, this.messagingClient.messagesForTheDm.size());
	}


	private void testTheCommonChain( InstanceStatus status, String file ) throws Exception {

		// Create a model
		Instance rootInstance = new Instance( "root" ).component( new Component( "Root" ).installerName( Constants.TARGET_INSTALLER ));
		Instance childInstance = new Instance( "child" ).component( new Component( "Child" ).installerName( "whatever" ));
		InstanceHelpers.insertChild( rootInstance, childInstance );
		this.agentInterface.setScopedInstance( rootInstance );

		rootInstance.setStatus( status );
		childInstance.setStatus( status );

		// Create the resources
		File dir = InstanceHelpers.findInstanceDirectoryOnAgent( childInstance );
		Utils.deleteFilesRecursively( dir );
		Assert.assertTrue( dir.mkdirs());

		File f = TestUtils.findTestFile( file );
		File measureFile = new File( dir, childInstance.getComponent().getName() + ".measures" );

		Assert.assertFalse( measureFile.exists());
		Utils.copyStream( f, measureFile );
		Assert.assertTrue( measureFile.exists());

		// Run the task
		Assert.assertEquals( 0, this.messagingClient.messagesForTheDm.size());
		MonitoringTask task = new MonitoringTask( this.agentInterface, HANDLERS );
		task.run();

		Utils.deleteFilesRecursively( dir );
	}
}
