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

package net.roboconf.dm.internal.api.impl.beans;

import java.util.List;

import net.roboconf.core.autonomic.Rule;
import net.roboconf.core.internal.tests.TestApplication;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class AutonomicApplicationContextTest {

	@Test
	public void testBasics() {

		TestApplication app = new TestApplication();
		AutonomicApplicationContext ctx = new AutonomicApplicationContext( app );
		Assert.assertEquals( app.getName(), ctx.toString());

		Assert.assertEquals( 0, ctx.ruleNameToRule.size());
		Assert.assertEquals( 0, ctx.eventNameToLastRecordTime.size());
		Assert.assertEquals( 0, ctx.ruleNameToLastExecution.size());
		Assert.assertEquals( 0, ctx.ruleNameToLastTrigger.size());
		Assert.assertEquals( 0, ctx.vmCount.get());

		ctx.recordPreExecution( "rule1" );

		Assert.assertEquals( 0, ctx.ruleNameToRule.size());
		Assert.assertEquals( 0, ctx.eventNameToLastRecordTime.size());
		Assert.assertEquals( 1, ctx.ruleNameToLastExecution.size());
		Assert.assertNotNull( ctx.ruleNameToLastExecution.get( "rule1" ));
		Assert.assertEquals( 0, ctx.ruleNameToLastTrigger.size());
		Assert.assertEquals( 0, ctx.vmCount.get());
	}


	@Test
	public void testFindRulesToExecute_noRule() throws Exception {

		TestApplication app = new TestApplication();
		AutonomicApplicationContext ctx = new AutonomicApplicationContext( app );

		Assert.assertEquals( 0, ctx.ruleNameToRule.size());
		Assert.assertEquals( 0, ctx.eventNameToLastRecordTime.size());
		Assert.assertEquals( 0, ctx.ruleNameToLastExecution.size());
		Assert.assertEquals( 0, ctx.ruleNameToLastTrigger.size());
		Assert.assertEquals( 0, ctx.vmCount.get());

		Assert.assertEquals( 0, ctx.findRulesToExecute().size());

		Assert.assertEquals( 0, ctx.ruleNameToRule.size());
		Assert.assertEquals( 0, ctx.eventNameToLastRecordTime.size());
		Assert.assertEquals( 0, ctx.ruleNameToLastExecution.size());
		Assert.assertEquals( 0, ctx.ruleNameToLastTrigger.size());
		Assert.assertEquals( 0, ctx.vmCount.get());
	}


	@Test
	public void testFindRulesToExecute_oneRuleButNoMatchingEvent() throws Exception {

		// Setup
		TestApplication app = new TestApplication();
		AutonomicApplicationContext ctx = new AutonomicApplicationContext( app );

		Rule rule = new Rule();
		rule.setEventName( "event" );
		rule.setRuleName( "r" );
		ctx.ruleNameToRule.put( rule.getRuleName(), rule );

		ctx.registerEvent( "other-event-1" );
		ctx.registerEvent( "other-event-2" );

		// Check
		Assert.assertEquals( 1, ctx.ruleNameToRule.size());
		Assert.assertEquals( 2, ctx.eventNameToLastRecordTime.size());
		Assert.assertEquals( 0, ctx.ruleNameToLastExecution.size());
		Assert.assertEquals( 0, ctx.ruleNameToLastTrigger.size());
		Assert.assertEquals( 0, ctx.vmCount.get());

		Assert.assertEquals( 0, ctx.findRulesToExecute().size());

		Assert.assertEquals( 1, ctx.ruleNameToRule.size());
		Assert.assertEquals( 2, ctx.eventNameToLastRecordTime.size());
		Assert.assertEquals( 0, ctx.ruleNameToLastExecution.size());
		Assert.assertEquals( 0, ctx.ruleNameToLastTrigger.size());
		Assert.assertEquals( 0, ctx.vmCount.get());
	}


	@Test
	public void testFindRulesToExecute_matchingRule() throws Exception {

		// Setup
		TestApplication app = new TestApplication();
		AutonomicApplicationContext ctx = new AutonomicApplicationContext( app );

		Rule rule1 = new Rule();
		rule1.setEventName( "event1" );
		rule1.setRuleName( "r1" );
		ctx.ruleNameToRule.put( rule1.getRuleName(), rule1 );

		Rule rule2 = new Rule();
		rule2.setEventName( "event2" );
		rule2.setRuleName( "r2" );
		ctx.ruleNameToRule.put( rule2.getRuleName(), rule2 );

		ctx.registerEvent( "other-event-1" );
		ctx.registerEvent( "other-event-2" );
		ctx.registerEvent( "event1" );

		// Check
		Assert.assertEquals( 2, ctx.ruleNameToRule.size());
		Assert.assertEquals( 3, ctx.eventNameToLastRecordTime.size());
		Assert.assertEquals( 0, ctx.ruleNameToLastExecution.size());
		Assert.assertEquals( 0, ctx.ruleNameToLastTrigger.size());
		Assert.assertEquals( 0, ctx.vmCount.get());

		List<Rule> rules = ctx.findRulesToExecute();
		Assert.assertEquals( 1, rules.size());
		Assert.assertEquals( rule1, rules.get( 0 ));

		Assert.assertEquals( 2, ctx.ruleNameToRule.size());
		Assert.assertEquals( 3, ctx.eventNameToLastRecordTime.size());
		Assert.assertEquals( 0, ctx.ruleNameToLastExecution.size());
		Assert.assertEquals( 1, ctx.ruleNameToLastTrigger.size());
		Assert.assertNotNull( ctx.ruleNameToLastTrigger.get( rule1.getRuleName()));
		Assert.assertEquals( 0, ctx.vmCount.get());

		// If we find the rules to execute, we should not have any rule
		// since the event was already read.
		Assert.assertEquals( 0, ctx.findRulesToExecute().size());

		Assert.assertEquals( 2, ctx.ruleNameToRule.size());
		Assert.assertEquals( 3, ctx.eventNameToLastRecordTime.size());
		Assert.assertEquals( 0, ctx.ruleNameToLastExecution.size());
		Assert.assertEquals( 1, ctx.ruleNameToLastTrigger.size());
		Assert.assertNotNull( ctx.ruleNameToLastTrigger.get( rule1.getRuleName()));
		Assert.assertEquals( 0, ctx.vmCount.get());

		// If we register the event with another time stamp, the rule should be listed again.
		// We also add a new event for another rule.
		ctx.registerEvent( "event1" );
		ctx.registerEvent( "event2" );

		rules = ctx.findRulesToExecute();
		Assert.assertEquals( 2, rules.size());
		Assert.assertTrue( rules.contains( rule1 ));
		Assert.assertTrue( rules.contains( rule2 ));

		Assert.assertEquals( 2, ctx.ruleNameToRule.size());
		Assert.assertEquals( 4, ctx.eventNameToLastRecordTime.size());
		Assert.assertEquals( 0, ctx.ruleNameToLastExecution.size());
		Assert.assertEquals( 2, ctx.ruleNameToLastTrigger.size());
		Assert.assertNotNull( ctx.ruleNameToLastTrigger.get( rule1.getRuleName()));
		Assert.assertNotNull( ctx.ruleNameToLastTrigger.get( rule2.getRuleName()));
		Assert.assertEquals( 0, ctx.vmCount.get());

		// No new event => no rule found.
		Assert.assertEquals( 0, ctx.findRulesToExecute().size());
	}


	@Test
	public void testFindRulesToExecute_matchingRule_withDelayBetweenExecutions() throws Exception {

		// Setup
		TestApplication app = new TestApplication();
		AutonomicApplicationContext ctx = new AutonomicApplicationContext( app );

		Rule rule1 = new Rule();
		rule1.setEventName( "event1" );
		rule1.setRuleName( "r1" );
		rule1.setDelayBetweenSucceedingInvocations( 1 );
		ctx.ruleNameToRule.put( rule1.getRuleName(), rule1 );

		ctx.recordPreExecution( rule1.getRuleName());
		ctx.registerEvent( "event1" );

		// Check: the delay is not over, the rule is ignored
		Assert.assertEquals( 1, ctx.ruleNameToRule.size());
		Assert.assertEquals( 1, ctx.eventNameToLastRecordTime.size());
		Assert.assertEquals( 1, ctx.ruleNameToLastExecution.size());
		Assert.assertEquals( 0, ctx.ruleNameToLastTrigger.size());
		Assert.assertEquals( 0, ctx.vmCount.get());

		List<Rule> rules = ctx.findRulesToExecute();
		Assert.assertEquals( 0, rules.size());

		Assert.assertEquals( 1, ctx.ruleNameToRule.size());
		Assert.assertEquals( 1, ctx.eventNameToLastRecordTime.size());
		Assert.assertEquals( 1, ctx.ruleNameToLastExecution.size());
		Assert.assertEquals( 0, ctx.ruleNameToLastTrigger.size());
		Assert.assertEquals( 0, ctx.vmCount.get());

		// Wait the delay
		Thread.sleep( 1010 );

		// Now, the rule should be found
		rules = ctx.findRulesToExecute();
		Assert.assertEquals( 1, rules.size());
		Assert.assertEquals( rule1, rules.get( 0 ));

		Assert.assertEquals( 1, ctx.ruleNameToRule.size());
		Assert.assertEquals( 1, ctx.eventNameToLastRecordTime.size());
		Assert.assertEquals( 1, ctx.ruleNameToLastExecution.size());
		Assert.assertEquals( 1, ctx.ruleNameToLastTrigger.size());
		Assert.assertNotNull( ctx.ruleNameToLastTrigger.get( rule1.getRuleName()));
		Assert.assertEquals( 0, ctx.vmCount.get());

		// If we execute it again, it will be skipped because the original event was already processed for this rule.
		Assert.assertEquals( 0, ctx.findRulesToExecute().size());
	}


	@Test
	public void testFindRulesToExecute_matchingRule_withTimingWindow() throws Exception {

		// Setup
		TestApplication app = new TestApplication();
		AutonomicApplicationContext ctx = new AutonomicApplicationContext( app );

		Rule rule1 = new Rule();
		rule1.setEventName( "event1" );
		rule1.setRuleName( "r1" );
		rule1.setTimingWindow( 1 );
		ctx.ruleNameToRule.put( rule1.getRuleName(), rule1 );

		ctx.registerEvent( "event1" );

		// Check: no registered time stamp, the rule is found
		Assert.assertEquals( 1, ctx.ruleNameToRule.size());
		Assert.assertEquals( 1, ctx.eventNameToLastRecordTime.size());
		Assert.assertEquals( 0, ctx.ruleNameToLastExecution.size());
		Assert.assertEquals( 0, ctx.ruleNameToLastTrigger.size());
		Assert.assertEquals( 0, ctx.vmCount.get());

		List<Rule> rules = ctx.findRulesToExecute();
		Assert.assertEquals( 1, rules.size());
		Assert.assertEquals( rule1, rules.get( 0 ));

		Assert.assertEquals( 1, ctx.ruleNameToRule.size());
		Assert.assertEquals( 1, ctx.eventNameToLastRecordTime.size());
		Assert.assertEquals( 0, ctx.ruleNameToLastExecution.size());
		Assert.assertEquals( 1, ctx.ruleNameToLastTrigger.size());
		Assert.assertNotNull( ctx.ruleNameToLastTrigger.get( rule1.getRuleName()));
		Assert.assertEquals( 0, ctx.vmCount.get());

		// Create a new similar event, but after the timing window.
		// The time window is not over, the rule will be ignored
		ctx.registerEvent( "event1" );
		Thread.sleep( 1010 );
		Assert.assertEquals( 0, ctx.findRulesToExecute().size());

		// Register a more recent event, it should be picked up.
		ctx.registerEvent( "event1" );
		rules = ctx.findRulesToExecute();
		Assert.assertEquals( 1, rules.size());
		Assert.assertEquals( rule1, rules.get( 0 ));

		Assert.assertEquals( 1, ctx.ruleNameToRule.size());
		Assert.assertEquals( 1, ctx.eventNameToLastRecordTime.size());
		Assert.assertEquals( 0, ctx.ruleNameToLastExecution.size());
		Assert.assertEquals( 1, ctx.ruleNameToLastTrigger.size());
		Assert.assertNotNull( ctx.ruleNameToLastTrigger.get( rule1.getRuleName()));
		Assert.assertEquals( 0, ctx.vmCount.get());

		// If we execute it again, it will be skipped because the original event was already processed for this rule.
		Assert.assertEquals( 0, ctx.findRulesToExecute().size());
	}
}
