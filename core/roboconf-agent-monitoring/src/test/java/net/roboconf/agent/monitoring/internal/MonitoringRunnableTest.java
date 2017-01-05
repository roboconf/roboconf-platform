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

package net.roboconf.agent.monitoring.internal;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import net.roboconf.agent.monitoring.api.IMonitoringHandler;
import net.roboconf.agent.monitoring.internal.MonitoringRunnable.MonitoringHandlerRun;
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
import net.roboconf.messaging.api.business.IAgentClient;
import net.roboconf.messaging.api.messages.Message;
import net.roboconf.messaging.api.messages.from_agent_to_dm.MsgNotifAutonomic;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * @author Vincent Zurczak - Linagora
 */
public class MonitoringRunnableTest {

	private static final List<IMonitoringHandler> HANDLERS = new ArrayList<> ();
	static {
		HANDLERS.add( new FileHandler());
		HANDLERS.add( new NagiosHandler());
		HANDLERS.add( new RestHandler());
	}

	private IAgentClient messagingClient;
	private MyAgentInterface agentInterface;


	@Before
	public void initializeMessagingClient() throws Exception {

		this.messagingClient = Mockito.mock( IAgentClient.class );
		this.agentInterface = new MyAgentInterface( this.messagingClient );
	}


	@Test
	public void testExtractRuleSections_noSection() throws Exception {

		String fileContent = "";
		MonitoringRunnable task = new MonitoringRunnable( this.agentInterface, HANDLERS );
		this.agentInterface.setScopedInstance( new Instance( "root" ));

		List<MonitoringHandlerRun> handlers = task.extractRuleSections( new File( "whatever" ), fileContent, null );
		Assert.assertEquals( 0, handlers.size());
	}


	@Test
	public void testExtractRuleSections_file() throws Exception {

		File f = TestUtils.findTestFile( "/file-events.conf" );
		String fileContent = Utils.readFileContent( f );
		MonitoringRunnable task = new MonitoringRunnable( this.agentInterface, HANDLERS );
		this.agentInterface.setScopedInstance( new Instance( "root" ));

		List<MonitoringHandlerRun> handlers = task.extractRuleSections( f, fileContent, null );
		Assert.assertEquals( 3, handlers.size());

		MonitoringHandlerRun bean = handlers.get( 0 );
		Assert.assertEquals( "file", bean.handlerName );
		Assert.assertEquals( "myRuleName-1", bean.eventId );
		Assert.assertTrue( bean.rawRulesText.contains( "%TMP%/rbcf-test/ta-daaaaa" ));

		bean = handlers.get( 1 );
		Assert.assertEquals( "file", bean.handlerName );
		Assert.assertEquals( "myRuleName-2", bean.eventId );
		Assert.assertTrue( bean.rawRulesText.contains( "%TMP%/rbcf-test/ta-daaaaa-2" ));

		bean = handlers.get( 2 );
		Assert.assertEquals( "file", bean.handlerName );
		Assert.assertEquals( "myRuleName", bean.eventId );
		Assert.assertTrue( bean.rawRulesText.contains( "%TMP%/rbcf-test/a-directory-to-not-delete" ));
	}


	@Test
	public void testExtractRuleSections_nagios() throws Exception {

		File f = TestUtils.findTestFile( "/nagios-events.conf" );
		String fileContent = Utils.readFileContent( f );
		MonitoringRunnable task = new MonitoringRunnable( this.agentInterface, HANDLERS );
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
		MonitoringRunnable task = new MonitoringRunnable( this.agentInterface, HANDLERS );
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
		MonitoringRunnable task = new MonitoringRunnable( this.agentInterface, HANDLERS );
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
		MonitoringRunnable task = new MonitoringRunnable( this.agentInterface, HANDLERS );
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

		String fileContent = MonitoringRunnable.RULE_BEGINNING;
		MonitoringRunnable task = new MonitoringRunnable( this.agentInterface, HANDLERS );
		this.agentInterface.setScopedInstance( new Instance( "root" ));

		List<MonitoringHandlerRun> handlers = task.extractRuleSections( new File( "test" ), fileContent, null );
		Assert.assertEquals( 0, handlers.size());
	}


	@Test
	public void testExtractRuleSections_invalidRule() throws Exception {

		String fileContent = MonitoringRunnable.RULE_BEGINNING + " file event id with spaces]";
		MonitoringRunnable task = new MonitoringRunnable( this.agentInterface, HANDLERS );
		this.agentInterface.setScopedInstance( new Instance( "root" ));

		List<MonitoringHandlerRun> handlers = task.extractRuleSections( new File( "test" ), fileContent, null );
		Assert.assertEquals( 0, handlers.size());
	}


	@Test
	public void testExtractRuleSections_unknownParser() throws Exception {

		String fileContent = MonitoringRunnable.RULE_BEGINNING + " unknown event-id]\nok";
		MonitoringRunnable task = new MonitoringRunnable( this.agentInterface, HANDLERS );
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
		ArgumentCaptor<Message> msgCapture = ArgumentCaptor.forClass( Message.class );
		Mockito.verify( this.messagingClient, Mockito.times( 1 )).sendMessageToTheDm( msgCapture.capture());
		Assert.assertEquals( MsgNotifAutonomic.class, msgCapture.getValue().getClass());

		MsgNotifAutonomic msg = (MsgNotifAutonomic) msgCapture.getValue();
		Assert.assertEquals( this.agentInterface.getApplicationName(), msg.getApplicationName());
		Assert.assertEquals( "myRuleName", msg.getEventName());
		Assert.assertEquals( "/" + this.agentInterface.getScopedInstance().getName(), msg.getScopedInstancePath());
		Assert.assertTrue( msg.getEventInfo().toLowerCase().contains( "does not exist" ));
	}


	@Test
	public void testWholeChain_instancesNotStarted() throws Exception {

		testTheCommonChain( InstanceStatus.DEPLOYED_STOPPED, "/file-events.conf" );
		Mockito.verify( this.messagingClient, Mockito.times( 0 )).sendMessageToTheDm( Mockito.any( Message.class ));
	}


	@Test
	public void testWholeChain_noHandler() throws Exception {

		testTheCommonChain( InstanceStatus.DEPLOYED_STARTED, "/unknown-events.conf" );
		Mockito.verify( this.messagingClient, Mockito.times( 0 )).sendMessageToTheDm( Mockito.any( Message.class ));
	}


	@Test
	public void testWholeChain_messagingError() throws Exception {

		Mockito.doThrow( new IOException( "For test" )).when( this.messagingClient ).sendMessageToTheDm( Mockito.any( Message.class ));
		testTheCommonChain( InstanceStatus.DEPLOYED_STARTED, "/file-events.conf" );
		// No exception is thrown
	}


	@Test
	public void testWholeChain_noModelYet() throws Exception {

		Mockito.verify( this.messagingClient, Mockito.times( 0 )).sendMessageToTheDm( Mockito.any( Message.class ));
		MonitoringRunnable task = new MonitoringRunnable( this.agentInterface, HANDLERS );
		task.run();
		Mockito.verify( this.messagingClient, Mockito.times( 0 )).sendMessageToTheDm( Mockito.any( Message.class ));
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
		String tmpDirLocation = System.getProperty( "java.io.tmpdir" ).replace( "\\", "/" ).replaceAll( "/?$", "" );
		String updatedContent = Utils.readFileContent( f ).replace( "%TMP%", tmpDirLocation );

		File measureFile = new File( dir, childInstance.getComponent().getName() + ".measures" );
		Utils.writeStringInto( updatedContent, measureFile );
		Assert.assertTrue( measureFile.exists());

		// Run the task
		Mockito.verify( this.messagingClient, Mockito.times( 0 )).sendMessageToTheDm( Mockito.any( Message.class ));
		MonitoringRunnable task = new MonitoringRunnable( this.agentInterface, HANDLERS );
		task.run();

		Utils.deleteFilesRecursively( dir );
		Assert.assertFalse( dir.exists());

		File temporaryDirectory = new File( tmpDirLocation, "rbcf-test" );
		Utils.deleteFilesRecursively( temporaryDirectory );
	}
}
