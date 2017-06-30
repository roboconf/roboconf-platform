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

package net.roboconf.core.autonomic;

import java.io.File;

import net.roboconf.core.errors.ErrorCode;
import net.roboconf.core.internal.tests.TestUtils;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class RuleParserTest {

	@Test
	public void testInvalidRule() throws Exception {

		File ruleFile = TestUtils.findTestFile( "/rules.autonomic/invalid-rule.drl" );
		RuleParser parser = new RuleParser( ruleFile );

		Assert.assertNotNull( parser.getRule());
		Assert.assertEquals( 1, parser.getParsingErrors().size());
		Assert.assertEquals( ErrorCode.RULE_INVALID_SYNTAX, parser.getParsingErrors().get( 0 ).getErrorCode());
	}


	@Test
	public void testIncompleteRule() throws Exception {

		File ruleFile = TestUtils.findTestFile( "/rules.autonomic/incomplete-rule.drl" );
		RuleParser parser = new RuleParser( ruleFile );

		Assert.assertNotNull( parser.getRule());
		Assert.assertEquals( 3, parser.getParsingErrors().size());
		Assert.assertEquals( ErrorCode.RULE_EMPTY_NAME, parser.getParsingErrors().get( 0 ).getErrorCode());
		Assert.assertEquals( ErrorCode.RULE_EMPTY_WHEN, parser.getParsingErrors().get( 1 ).getErrorCode());
		Assert.assertEquals( ErrorCode.RULE_EMPTY_THEN, parser.getParsingErrors().get( 2 ).getErrorCode());
	}


	@Test
	public void testInexistingFile() throws Exception {

		File ruleFile = new File( "that.does.not.exist" );
		RuleParser parser = new RuleParser( ruleFile );

		Assert.assertNotNull( parser.getRule());
		Assert.assertEquals( 1, parser.getParsingErrors().size());
		Assert.assertEquals( ErrorCode.RULE_IO_ERROR, parser.getParsingErrors().get( 0 ).getErrorCode());
	}


	@Test
	public void testSimpleRuleFile() throws Exception {

		File ruleFile = TestUtils.findTestFile( "/rules.autonomic/simple-rule.drl" );
		RuleParser parser = new RuleParser( ruleFile );

		Assert.assertNotNull( parser.getRule());
		Assert.assertEquals( 0, parser.getParsingErrors().size());

		Assert.assertEquals( "event-1", parser.getRule().getEventName());
		Assert.assertEquals( "test", parser.getRule().getRuleName());
		Assert.assertEquals( 0L, parser.getRule().getDelayBetweenSucceedingInvocations());
		Assert.assertEquals( -1, parser.getRule().getTimingWindow());
		Assert.assertEquals( 1, parser.getRule().getCommandsToInvoke().size());
		Assert.assertEquals( "cmd1" , parser.getRule().getCommandsToInvoke().get( 0 ));
		Assert.assertEquals( "test", parser.getRule().toString());
	}


	@Test
	public void testRuleFileWithComments_1() throws Exception {

		File ruleFile = TestUtils.findTestFile( "/rules.autonomic/rule-with-comments.drl" );
		RuleParser parser = new RuleParser( ruleFile );

		Assert.assertNotNull( parser.getRule());
		Assert.assertEquals( 0, parser.getParsingErrors().size());

		Assert.assertEquals( "event-1", parser.getRule().getEventName());
		Assert.assertEquals( "test", parser.getRule().getRuleName());
		Assert.assertEquals( 0L, parser.getRule().getDelayBetweenSucceedingInvocations());
		Assert.assertEquals( -1, parser.getRule().getTimingWindow());
		Assert.assertEquals( 4, parser.getRule().getCommandsToInvoke().size());
		Assert.assertEquals( "cmd1" , parser.getRule().getCommandsToInvoke().get( 0 ));
		Assert.assertEquals( "cmd2" , parser.getRule().getCommandsToInvoke().get( 1 ));
		Assert.assertEquals( "cmd3" , parser.getRule().getCommandsToInvoke().get( 2 ));
		Assert.assertEquals( "cmd4" , parser.getRule().getCommandsToInvoke().get( 3 ));
	}


	@Test
	public void testRuleFileWithComments_2() throws Exception {

		File ruleFile = TestUtils.findTestFile( "/rules.autonomic/rule-with-comments-2.drl" );
		RuleParser parser = new RuleParser( ruleFile );

		Assert.assertNotNull( parser.getRule());
		Assert.assertEquals( 0, parser.getParsingErrors().size());

		Assert.assertEquals( "event-1", parser.getRule().getEventName());
		Assert.assertEquals( "test", parser.getRule().getRuleName());
		Assert.assertEquals( 0L, parser.getRule().getDelayBetweenSucceedingInvocations());
		Assert.assertEquals( -1, parser.getRule().getTimingWindow());
		Assert.assertEquals( 4, parser.getRule().getCommandsToInvoke().size());
		Assert.assertEquals( "cmd1" , parser.getRule().getCommandsToInvoke().get( 0 ));
		Assert.assertEquals( "cmd2" , parser.getRule().getCommandsToInvoke().get( 1 ));
		Assert.assertEquals( "cmd3" , parser.getRule().getCommandsToInvoke().get( 2 ));
		Assert.assertEquals( "cmd4" , parser.getRule().getCommandsToInvoke().get( 3 ));
	}


	@Test
	public void testRuleFileWithTimeConstraints() throws Exception {

		File ruleFile = TestUtils.findTestFile( "/rules.autonomic/rule-with-time-constraints.drl" );
		RuleParser parser = new RuleParser( ruleFile );

		Assert.assertNotNull( parser.getRule());
		Assert.assertEquals( 0, parser.getParsingErrors().size());

		Assert.assertEquals( "event-1", parser.getRule().getEventName());
		Assert.assertEquals( "test", parser.getRule().getRuleName());
		Assert.assertEquals( 8, parser.getRule().getDelayBetweenSucceedingInvocations());
		Assert.assertEquals( 60, parser.getRule().getTimingWindow());
		Assert.assertEquals( 1, parser.getRule().getCommandsToInvoke().size());
		Assert.assertEquals( "cmd1" , parser.getRule().getCommandsToInvoke().get( 0 ));
	}
}
