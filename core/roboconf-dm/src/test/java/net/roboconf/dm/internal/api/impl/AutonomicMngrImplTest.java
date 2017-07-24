/**
 * Copyright 2016-2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.dm.internal.api.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import net.roboconf.core.Constants;
import net.roboconf.core.autonomic.Rule;
import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.internal.tests.TestApplicationTemplate;
import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.internal.api.impl.beans.AutonomicApplicationContext;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.api.ICommandsMngr;
import net.roboconf.dm.management.api.ICommandsMngr.CommandExecutionContext;
import net.roboconf.dm.management.api.IPreferencesMngr;
import net.roboconf.dm.management.exceptions.CommandException;
import net.roboconf.messaging.api.messages.from_agent_to_dm.MsgNotifAutonomic;

/**
 * @author Vincent Zurczak - Linagora
 */
public class AutonomicMngrImplTest {

	@org.junit.Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private AutonomicMngrImpl autonomicMngr;
	private ICommandsMngr commandsMngr;
	private IPreferencesMngr preferencesMngr;



	@Before
	public void setup() {

		this.commandsMngr = Mockito.mock( ICommandsMngr.class );
		this.preferencesMngr = Mockito.mock( IPreferencesMngr.class );
		this.autonomicMngr = new AutonomicMngrImpl( this.commandsMngr );
		this.autonomicMngr.setPreferencesMngr( this.preferencesMngr );
	}


	@Test
	public void testNotifyVmWasDeletedByHand() throws Exception {

		AtomicInteger vmCount = this.autonomicMngr.autonomicVmCount;
		Assert.assertEquals( 0, vmCount.get());

		vmCount.set( 5 );
		Instance rootInstance = new Instance( "inst" );;
		this.autonomicMngr.notifyVmWasDeletedByHand( rootInstance );
		Assert.assertEquals( 5, vmCount.get());

		rootInstance.data.put( AutonomicMngrImpl.AUTONOMIC_MARKER, "whatever" );
		this.autonomicMngr.notifyVmWasDeletedByHand( rootInstance );
		Assert.assertEquals( 4, vmCount.get());

		// The marker was already removed
		this.autonomicMngr.notifyVmWasDeletedByHand( rootInstance );
		Assert.assertEquals( 4, vmCount.get());
	}


	@Test
	public void testHandleEvent_noContext() {

		Application app = new Application( "app", new TestApplicationTemplate());
		ManagedApplication ma = new ManagedApplication( app );
		this.autonomicMngr.handleEvent( ma, new MsgNotifAutonomic( "app", "/root", "event", null ));

		Mockito.verifyZeroInteractions( this.commandsMngr );
		Mockito.verifyZeroInteractions( this.preferencesMngr );
		Assert.assertEquals( 0, this.autonomicMngr.getAutonomicInstancesCount());
	}


	@Test
	public void testHandleEvent_withContext_noRule() throws Exception {

		List<Rule> rules = new ArrayList<>( 0 );
		Application app = new Application( "app", new TestApplicationTemplate());

		AutonomicApplicationContext ctx = Mockito.spy( new AutonomicApplicationContext( app ));
		Mockito.when( ctx.findRulesToExecute()).thenReturn( rules );
		this.autonomicMngr.appNameToContext.put( "app", ctx );

		ManagedApplication ma = new ManagedApplication( app );
		this.autonomicMngr.handleEvent( ma, new MsgNotifAutonomic( "app", "/root", "event", null ));

		Mockito.verifyZeroInteractions( this.commandsMngr );
		Mockito.verifyZeroInteractions( this.preferencesMngr );

		Map<?,?> eventNameToLastRecordTime = TestUtils.getInternalField( ctx, "eventNameToLastRecordTime", Map.class );
		Assert.assertEquals( 1, eventNameToLastRecordTime.size());
		Assert.assertNotNull( eventNameToLastRecordTime.get( "event" ));
	}


	@Test
	public void testHandleEvent_withContext_withRule() throws Exception {

		Mockito.when( this.preferencesMngr.get( Mockito.anyString(), Mockito.anyString())).thenReturn( "5" );

		Rule rule1 = Mockito.mock( Rule.class );
		Mockito.when( rule1.getCommandsToInvoke()).thenReturn( Arrays.asList( "cmd1" ));

		Rule rule2 = Mockito.mock( Rule.class );
		Mockito.when( rule2.getCommandsToInvoke()).thenReturn( Arrays.asList( "cmd2", "cmd3" ));

		Application app = new Application( "app", new TestApplicationTemplate());
		AutonomicApplicationContext ctx = Mockito.spy( new AutonomicApplicationContext( app ));
		Mockito.when( ctx.findRulesToExecute()).thenReturn( Arrays.asList( rule1, rule2 ));
		this.autonomicMngr.appNameToContext.put( "app", ctx );

		ManagedApplication ma = new ManagedApplication( app );
		this.autonomicMngr.handleEvent( ma, new MsgNotifAutonomic( "app", "/root", "event", null ));

		Mockito.verify( this.preferencesMngr, Mockito.times( 2 )).get( Mockito.anyString(), Mockito.anyString());

		Map<?,?> eventNameToLastRecordTime = TestUtils.getInternalField( ctx, "eventNameToLastRecordTime", Map.class );
		Assert.assertEquals( 1, eventNameToLastRecordTime.size());
		Assert.assertNotNull( eventNameToLastRecordTime.get( "event" ));

		Mockito.verify( rule1, Mockito.times( 1 )).getCommandsToInvoke();
		Mockito.verify( rule2, Mockito.times( 1 )).getCommandsToInvoke();

		ArgumentCaptor<CommandExecutionContext> execCtx = ArgumentCaptor.forClass( CommandExecutionContext.class );
		Mockito.verify( this.commandsMngr, Mockito.times( 3 )).execute(
				Mockito.any( Application.class ),
				Mockito.anyString(),
				Mockito.any( CommandExecutionContext.class ),
				Mockito.anyInt(),
				Mockito.anyString());

		for( CommandExecutionContext c : execCtx.getAllValues()) {
			Assert.assertEquals( 5, c.getMaxVm());
			Assert.assertFalse( c.isStrictMaxVm());
		}
	}


	@Test
	public void testHandleEvent_withContext_withRule_negativeMaxVmNumber() throws Exception {

		Mockito.when( this.preferencesMngr.get( Mockito.anyString(), Mockito.anyString())).thenReturn( "-1" );

		ManagedApplication ma = factorizeConfiguration();
		this.autonomicMngr.handleEvent( ma, new MsgNotifAutonomic( "app", "/root", "event", null ));

		ArgumentCaptor<CommandExecutionContext> execCtx = ArgumentCaptor.forClass( CommandExecutionContext.class );
		Mockito.verify( this.commandsMngr, Mockito.times( 3 )).execute(
				Mockito.any( Application.class ),
				Mockito.anyString(),
				execCtx.capture(),
				Mockito.anyInt(),
				Mockito.anyString());

		for( CommandExecutionContext c : execCtx.getAllValues()) {
			Assert.assertEquals( Integer.MAX_VALUE, c.getMaxVm());
			Assert.assertFalse( c.isStrictMaxVm());
		}
	}


	@Test
	public void testHandleEvent_withContext_withRule_strictMaximum() throws Exception {

		Mockito.when( this.preferencesMngr.get( IPreferencesMngr.AUTONOMIC_MAX_VM_NUMBER, "" + Integer.MAX_VALUE )).thenReturn( "4" );
		Mockito.when( this.preferencesMngr.get( IPreferencesMngr.AUTONOMIC_STRICT_MAX_VM_NUMBER, "true" )).thenReturn( "true" );

		ManagedApplication ma = factorizeConfiguration();
		this.autonomicMngr.handleEvent( ma, new MsgNotifAutonomic( "app", "/root", "event", null ));

		Mockito.verify( this.preferencesMngr, Mockito.times( 1 )).get( IPreferencesMngr.AUTONOMIC_MAX_VM_NUMBER, "" + Integer.MAX_VALUE );
		Mockito.verify( this.preferencesMngr, Mockito.times( 1 )).get( IPreferencesMngr.AUTONOMIC_STRICT_MAX_VM_NUMBER, "true" );

		ArgumentCaptor<CommandExecutionContext> execCtx = ArgumentCaptor.forClass( CommandExecutionContext.class );
		Mockito.verify( this.commandsMngr, Mockito.times( 3 )).execute(
				Mockito.any( Application.class ),
				Mockito.anyString(),
				execCtx.capture(),
				Mockito.anyInt(),
				Mockito.anyString());

		for( CommandExecutionContext c : execCtx.getAllValues()) {
			Assert.assertEquals( 4, c.getMaxVm());
			Assert.assertTrue( c.isStrictMaxVm());
		}
	}


	@Test
	public void testHandleEvent_withContext_withRule_strictMaximumEnabledAutomatically() throws Exception {

		// More VM than what the preferences allow => the "strict" parameter will be set automatically.
		TestUtils.getInternalField( this.autonomicMngr, "autonomicVmCount", AtomicInteger.class ).set( 10 );
		Mockito.when( this.preferencesMngr.get( Mockito.anyString(), Mockito.anyString())).thenReturn( "5" );

		ManagedApplication ma = factorizeConfiguration();
		this.autonomicMngr.handleEvent( ma, new MsgNotifAutonomic( "app", "/root", "event", null ));

		ArgumentCaptor<CommandExecutionContext> execCtx = ArgumentCaptor.forClass( CommandExecutionContext.class );
		Mockito.verify( this.commandsMngr, Mockito.times( 3 )).execute(
				Mockito.any( Application.class ),
				Mockito.anyString(),
				execCtx.capture(),
				Mockito.anyInt(),
				Mockito.anyString());

		for( CommandExecutionContext c : execCtx.getAllValues()) {
			Assert.assertEquals( 5, c.getMaxVm());
			Assert.assertTrue( c.isStrictMaxVm());
		}
	}


	@Test
	public void testHandleEvent_execption() throws Exception {

		Mockito.doThrow( new CommandException( "for test" )).when( this.commandsMngr ).execute(
				Mockito.any( Application.class ),
				Mockito.anyString(),
				Mockito.any( CommandExecutionContext.class ),
				Mockito.anyInt(),
				Mockito.anyString());

		ManagedApplication ma = factorizeConfiguration();
		this.autonomicMngr.handleEvent( ma, new MsgNotifAutonomic( "app", "/root", "event", null ));

		Mockito.verifyZeroInteractions( this.commandsMngr );
	}



	@Test
	public void testLoadUpdateAndRemove() throws Exception {

		// Create an application
		TestApplication app = new TestApplication();
		app.setDirectory( this.folder.newFolder());

		// Create rule files
		File autonomicRulesDir = new File( app.getDirectory(), Constants.PROJECT_DIR_RULES_AUTONOMIC );
		Assert.assertTrue( autonomicRulesDir.mkdir());

		File f = new File( autonomicRulesDir, "rule1.invalid-ext" );
		Assert.assertTrue( f.createNewFile());

		f = new File( autonomicRulesDir, "rule2" + Constants.FILE_EXT_RULE );
		Utils.writeStringInto( "rule \"test1\"\nwhen event1 then cmd1 end", f );

		f = new File( autonomicRulesDir, "rule3" + Constants.FILE_EXT_RULE );
		Utils.writeStringInto( "rule \"test2\"\nwhen event2 then invalid syntax", f );

		// Load the rules
		Assert.assertEquals( 0, this.autonomicMngr.appNameToContext.size());
		this.autonomicMngr.loadApplicationRules( app );
		Assert.assertEquals( 1, this.autonomicMngr.appNameToContext.size());

		AutonomicApplicationContext ctx = this.autonomicMngr.appNameToContext.get( app.getName());
		Assert.assertNotNull( ctx );
		Assert.assertEquals( 1, ctx.ruleNameToRule.size());
		Assert.assertNotNull( ctx.ruleNameToRule.get( "test1" ));

		// Update the invalid rule
		Utils.writeStringInto( "rule \"test2\"\nwhen event2 then cmd2 end", f );
		this.autonomicMngr.refreshApplicationRules( app, "rule3" );
		Assert.assertEquals( 1, this.autonomicMngr.appNameToContext.size());

		Assert.assertEquals( 2, ctx.ruleNameToRule.size());
		Assert.assertNotNull( ctx.ruleNameToRule.get( "test1" ));
		Assert.assertNotNull( ctx.ruleNameToRule.get( "test2" ));

		// Reload the first one
		Rule oldRule1 = ctx.ruleNameToRule.remove( "test1" );
		this.autonomicMngr.refreshApplicationRules( app, "rule2" + Constants.FILE_EXT_RULE );

		Assert.assertEquals( 1, this.autonomicMngr.appNameToContext.size());
		Assert.assertEquals( 2, ctx.ruleNameToRule.size());
		Assert.assertNotNull( ctx.ruleNameToRule.get( "test1" ));
		Assert.assertNotNull( ctx.ruleNameToRule.get( "test2" ));
		Assert.assertNotSame( oldRule1, ctx.ruleNameToRule.get( "test1" ));

		// Unload the rules
		this.autonomicMngr.unloadApplicationRules( app );
		Assert.assertEquals( 0, this.autonomicMngr.appNameToContext.size());
	}


	@Test
	public void testRefreshRules_unknownApp() throws Exception {

		TestApplication app = new TestApplication();
		Assert.assertEquals( 0, this.autonomicMngr.appNameToContext.size());
		this.autonomicMngr.refreshApplicationRules( app, null );
		Assert.assertEquals( 0, this.autonomicMngr.appNameToContext.size());
	}


	@Test
	public void testRefreshRules_unknownRule() throws Exception {

		// Create an application
		TestApplication app = new TestApplication();
		app.setDirectory( this.folder.newFolder());

		// Create the autonomic directory
		File autonomicRulesDir = new File( app.getDirectory(), Constants.PROJECT_DIR_RULES_AUTONOMIC );
		Assert.assertTrue( autonomicRulesDir.mkdir());

		// Load the rules
		Assert.assertEquals( 0, this.autonomicMngr.appNameToContext.size());
		this.autonomicMngr.loadApplicationRules( app );
		Assert.assertEquals( 1, this.autonomicMngr.appNameToContext.size());

		AutonomicApplicationContext ctx = this.autonomicMngr.appNameToContext.get( app.getName());
		Assert.assertNotNull( ctx );
		Assert.assertEquals( 0, ctx.ruleNameToRule.size());

		// Try to refresh an invalid rule
		this.autonomicMngr.refreshApplicationRules( app, "unknown" );
		Assert.assertEquals( 0, ctx.ruleNameToRule.size());
	}


	@Test
	public void testLoadRules_noAutonomicDir() throws Exception {

		// Create an application
		TestApplication app = new TestApplication();
		app.setDirectory( this.folder.newFolder());

		// Load the rules
		Assert.assertEquals( 0, this.autonomicMngr.appNameToContext.size());
		this.autonomicMngr.loadApplicationRules( app );
		Assert.assertEquals( 1, this.autonomicMngr.appNameToContext.size());

		AutonomicApplicationContext ctx = this.autonomicMngr.appNameToContext.get( app.getName());
		Assert.assertNotNull( ctx );
		Assert.assertEquals( 0, ctx.ruleNameToRule.size());
	}


	private ManagedApplication factorizeConfiguration() {

		Rule rule1 = Mockito.mock( Rule.class );
		Mockito.when( rule1.getCommandsToInvoke()).thenReturn( Arrays.asList( "cmd1" ));

		Rule rule2 = Mockito.mock( Rule.class );
		Mockito.when( rule2.getCommandsToInvoke()).thenReturn( Arrays.asList( "cmd2", "cmd3" ));

		Application app = new Application( "app", new TestApplicationTemplate());
		AutonomicApplicationContext ctx = Mockito.spy( new AutonomicApplicationContext( app ));
		Mockito.when( ctx.findRulesToExecute()).thenReturn( Arrays.asList( rule1, rule2 ));
		this.autonomicMngr.appNameToContext.put( "app", ctx );

		return new ManagedApplication( app );
	}
}
