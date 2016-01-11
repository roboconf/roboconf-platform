/**
 * Copyright 2014-2016 Linagora, Université Joseph Fourier, Floralis
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
import java.util.Map;

import org.junit.Assert;
import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.beans.Application;
import net.roboconf.dm.management.ManagedApplication;

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class RulesParserTest {

	@Test
	public void testParsing() throws Exception {

		File f = TestUtils.findTestFile( "/autonomic/my-app.rules" );
		Assert.assertTrue( f.exists());

		Map<String,AutonomicRule> map = RulesParser.parseRules( f );
		Assert.assertEquals( 4, map.size());

		AutonomicRule rule = map.get( "event-1" );
		Assert.assertEquals( RuleBasedEventHandler.REPLICATE_SERVICE, rule.getReactionId().toLowerCase());
		Assert.assertEquals( "/vm/tomcat/war1", rule.getReactionInfo());
		Assert.assertEquals( 0, rule.getDelay());

		rule = map.get( "event-2" );
		Assert.assertEquals( RuleBasedEventHandler.DELETE_SERVICE, rule.getReactionId().toLowerCase());
		Assert.assertEquals( "war1", rule.getReactionInfo());
		Assert.assertEquals( 0, rule.getDelay());

		rule = map.get( "event-3" );
		Assert.assertEquals( RuleBasedEventHandler.MAIL, rule.getReactionId().toLowerCase());
		Assert.assertEquals( "admin@company.com", rule.getReactionInfo());
		Assert.assertEquals( 0, rule.getDelay());

		rule = map.get( "event-4" );
		Assert.assertEquals( "log", rule.getReactionId().toLowerCase());
		Assert.assertEquals( "", rule.getReactionInfo());
		Assert.assertEquals( 27, rule.getDelay());
	}


	@Test
	public void testParsing_invalid() throws Exception {

		File f = TestUtils.findTestFile( "/autonomic/invalid.rules" );
		Assert.assertTrue( f.exists());

		Map<String,AutonomicRule> map = RulesParser.parseRules( f );
		Assert.assertEquals( 0, map.size());
	}


	@Test
	public void testParsing_inexsting() throws Exception {

		Application app = new TestApplication().directory( new File( "inexisting" ));
		ManagedApplication ma = new ManagedApplication( app );
		Map<String,AutonomicRule> map = RulesParser.parseRules( ma );
		Assert.assertEquals( 0, map.size());
	}
}
