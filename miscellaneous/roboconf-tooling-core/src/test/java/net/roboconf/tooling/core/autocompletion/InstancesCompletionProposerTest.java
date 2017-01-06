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

package net.roboconf.tooling.core.autocompletion;

import static net.roboconf.core.dsl.ParsingConstants.KEYWORD_IMPORT;
import static net.roboconf.core.dsl.ParsingConstants.KEYWORD_INSTANCE_OF;
import static net.roboconf.core.dsl.ParsingConstants.PROPERTY_INSTANCE_CHANNELS;
import static net.roboconf.core.dsl.ParsingConstants.PROPERTY_INSTANCE_NAME;
import static net.roboconf.tooling.core.autocompletion.GraphsCompletionProposer.IMPORT_PREFIX;
import static net.roboconf.tooling.core.autocompletion.InstancesCompletionProposer.INSTANCE_OF_BLOCK;
import static net.roboconf.tooling.core.autocompletion.InstancesCompletionProposer.INSTANCE_OF_PREFIX;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import net.roboconf.core.Constants;
import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.utils.Utils;
import net.roboconf.tooling.core.autocompletion.ICompletionProposer.RoboconfCompletionProposal;

/**
 * @author Vincent Zurczak - Linagora
 */
public class InstancesCompletionProposerTest extends AbstractCompletionProposerTest {

	@Test
	public void testOffsetAtZero() throws Exception {

		// Expected: import, instance of, instance of block
		Couple couple = prepare( "app1", "initial.instances", 0 );
		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		verifyNeutralOffset( proposals );
	}


	@Test
	public void testOffsetInComment_1() throws Exception {

		// Expected: nothing
		Couple couple = prepare( "app1", "initial.instances", 9 );
		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( 0, proposals.size());
	}


	@Test
	public void testOffsetInComment_2() throws Exception {

		// Expected: nothing
		Couple couple = prepare( "app1", "initial.instances", 32 );
		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( 0, proposals.size());
	}


	@Test
	public void testOffsetInInlineCommentInInstance() throws Exception {

		// Expected: nothing
		Couple couple = prepare( "app1", "initial.instances", 83 );
		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( 0, proposals.size());
	}


	@Test
	public void testOffsetRightBeforeComment() throws Exception {

		// Expected: import, instance of, instance of block
		Couple couple = prepare( "app1", "initial.instances", 1 );
		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		verifyNeutralOffset( proposals );
	}


	@Test
	public void testOffsetRightAfterCommentSymbol() throws Exception {

		// Expected: nothing
		Couple couple = prepare( "app1", "initial.instances", 2 );
		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( 0, proposals.size());
	}


	@Test
	public void testOffsetOutisdeInstances() throws Exception {

		// Expected: import, instance of, instance of block
		Couple couple = prepare( "app1", "initial.instances", 43 );
		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		verifyNeutralOffset( proposals );

		couple = prepare( "app1", "initial.instances", 107 );
		proposals = couple.proposer.findProposals( couple.text );
		verifyNeutralOffset( proposals );
	}


	@Test
	public void testOffsetRightClosingCurlyBracket() throws Exception {

		// Expected: nothing
		Couple couple = prepare( "app1", "initial.instances", 2 );
		couple.text = "instanceof Toto {\n}";

		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( 0, proposals.size());
	}


	@Test
	public void testOffsetInsideInstances() throws Exception {

		// Expected: "channels:", "name:", instance of, instance of block
		Couple couple = prepare( "app1", "initial.instances", 61 );
		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		verifyBasicInstanceProperties( proposals );

		couple = prepare( "app1", "initial.instances", 99 );
		proposals = couple.proposer.findProposals( couple.text );
		verifyBasicInstanceProperties( proposals );

		couple = prepare( "app1", "initial.instances", 103 );
		proposals = couple.proposer.findProposals( couple.text );
		verifyBasicInstanceProperties( proposals );
	}


	@Test
	public void testOffsetInsideInstancesWithInstanceOfPrefix() throws Exception {

		// Expected: instance of, instance of block
		Couple couple = prepare( "app1", "initial.instances", 61 );
		couple.text += "inst";

		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( 2, proposals.size());

		Assert.assertEquals( KEYWORD_INSTANCE_OF, proposals.get( 0 ).getProposalName());
		Assert.assertEquals( INSTANCE_OF_PREFIX, proposals.get( 0 ).getProposalString());
		Assert.assertNull( proposals.get( 0 ).getProposalDescription());
		Assert.assertEquals( 4, proposals.get( 0 ).getReplacementOffset());

		Assert.assertEquals( INSTANCE_OF_BLOCK, proposals.get( 1 ).getProposalName());
		Assert.assertTrue( proposals.get( 1 ).getProposalString().startsWith( INSTANCE_OF_PREFIX ));
		Assert.assertTrue( proposals.get( 1 ).getProposalDescription().startsWith( INSTANCE_OF_BLOCK ));
		Assert.assertEquals( 4, proposals.get( 1 ).getReplacementOffset());
	}


	@Test
	public void testOffsetInsideInstancesWithBasicPropertyPrefix_1() throws Exception {

		// Expected: "name:"
		Couple couple = prepare( "app1", "initial.instances", 61 );
		couple.text += "nam";

		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( 1, proposals.size());

		Assert.assertEquals( PROPERTY_INSTANCE_NAME + ": ", proposals.get( 0 ).getProposalName());
		Assert.assertEquals( PROPERTY_INSTANCE_NAME + ": ", proposals.get( 0 ).getProposalString());
		Assert.assertNull( proposals.get( 0 ).getProposalDescription());
		Assert.assertEquals( 3, proposals.get( 0 ).getReplacementOffset());
	}


	@Test
	public void testOffsetInsideInstancesWithBasicPropertyPrefix_2() throws Exception {

		// Expected: "name:"
		Couple couple = prepare( "app1", "initial.instances", 61 );
		couple.text += "name";

		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( 1, proposals.size());

		Assert.assertEquals( PROPERTY_INSTANCE_NAME + ": ", proposals.get( 0 ).getProposalName());
		Assert.assertEquals( PROPERTY_INSTANCE_NAME + ": ", proposals.get( 0 ).getProposalString());
		Assert.assertNull( proposals.get( 0 ).getProposalDescription());
		Assert.assertEquals( 4, proposals.get( 0 ).getReplacementOffset());
	}


	@Test
	public void testOffsetInsideInstancesWithBasicPropertyPrefix_3() throws Exception {

		// Expected: "name:"
		Couple couple = prepare( "app1", "initial.instances", 61 );
		couple.text += "nAMe:";

		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( 1, proposals.size());

		Assert.assertEquals( PROPERTY_INSTANCE_NAME + ": ", proposals.get( 0 ).getProposalName());
		Assert.assertEquals( PROPERTY_INSTANCE_NAME + ": ", proposals.get( 0 ).getProposalString());
		Assert.assertNull( proposals.get( 0 ).getProposalDescription());
		Assert.assertEquals( 5, proposals.get( 0 ).getReplacementOffset());
	}


	@Test
	public void testOffsetInsideInstancesWithBasicPropertyPrefix_4() throws Exception {

		// Expected: "name:"
		Couple couple = prepare( "app1", "initial.instances", 61 );
		couple.text += "name: ";

		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( 0, proposals.size());
	}


	@Test
	public void testOffsetInsideInstancesWithBasicPropertyPrefix_5() throws Exception {

		// Expected: "name:"
		Couple couple = prepare( "app1", "initial.instances", 61 );
		couple.text += "name : ";

		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( 0, proposals.size());
	}


	@Test
	public void testOffsetInsideInstancesWithInvalidPrefix() throws Exception {

		// Expected: nothing
		Couple couple = prepare( "app1", "initial.instances", 61 );
		couple.text += "pop";

		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( 0, proposals.size());
	}


	@Test
	public void testOffsetOutisdeInstancesWithImportPrefix() throws Exception {

		// Expected: import
		Couple couple = prepare( "app1", "initial.instances", 43 );
		couple.text += "im";

		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( 1, proposals.size());

		Assert.assertEquals( KEYWORD_IMPORT, proposals.get( 0 ).getProposalName());
		Assert.assertEquals( IMPORT_PREFIX, proposals.get( 0 ).getProposalString());
		Assert.assertNull( proposals.get( 0 ).getProposalDescription());
		Assert.assertEquals( 2, proposals.get( 0 ).getReplacementOffset());
	}


	@Test
	public void testOffsetOutisdeInstancesWithFakeImportPrefix() throws Exception {

		// Expected: nothing
		Couple couple = prepare( "app1", "initial.instances", 43 );
		couple.text += "im po";

		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( 0, proposals.size());
	}


	@Test
	public void testOffsetOutisdeInstancesWithInstanceOfPrefix() throws Exception {

		// Expected: instance of, instance of block
		Couple couple = prepare( "app1", "initial.instances", 43 );
		couple.text += "in";

		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( 2, proposals.size());

		Assert.assertEquals( KEYWORD_INSTANCE_OF, proposals.get( 0 ).getProposalName());
		Assert.assertEquals( INSTANCE_OF_PREFIX, proposals.get( 0 ).getProposalString());
		Assert.assertNull( proposals.get( 0 ).getProposalDescription());
		Assert.assertEquals( 2, proposals.get( 0 ).getReplacementOffset());

		Assert.assertEquals( INSTANCE_OF_BLOCK, proposals.get( 1 ).getProposalName());
		Assert.assertTrue( proposals.get( 1 ).getProposalString().startsWith( INSTANCE_OF_PREFIX ));
		Assert.assertTrue( proposals.get( 1 ).getProposalDescription().startsWith( INSTANCE_OF_BLOCK ));
		Assert.assertEquals( 2, proposals.get( 1 ).getReplacementOffset());
	}


	@Test
	public void testOffsetOutisdeInstancesWithFakeInstanceOfPrefix() throws Exception {

		// Expected: nothing
		Couple couple = prepare( "app1", "initial.instances", 43 );
		couple.text += "instanceof";

		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( 0, proposals.size());
	}


	@Test
	public void testOffsetInsideInstancesWithComponentPropertiesAndWithoutInheritance() throws Exception {

		// Expected:  "c2.ip", "channels:", "name:", instance of, instance of block
		Couple couple = prepare( "app3", "initial.instances", 61 );
		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( 5, proposals.size());

		Assert.assertEquals( "c2.ip: ", proposals.get( 0 ).getProposalName());
		Assert.assertEquals( "c2.ip: ", proposals.get( 0 ).getProposalString());
		Assert.assertEquals( CompletionUtils.SET_BY_ROBOCONF, proposals.get( 0 ).getProposalDescription());
		Assert.assertEquals( 0, proposals.get( 0 ).getReplacementOffset());

		Assert.assertEquals( PROPERTY_INSTANCE_CHANNELS + ": ", proposals.get( 1 ).getProposalName());
		Assert.assertEquals( PROPERTY_INSTANCE_CHANNELS + ": ", proposals.get( 1 ).getProposalString());
		Assert.assertNull( proposals.get( 1 ).getProposalDescription());
		Assert.assertEquals( 0, proposals.get( 1 ).getReplacementOffset());

		Assert.assertEquals( PROPERTY_INSTANCE_NAME + ": ", proposals.get( 2 ).getProposalName());
		Assert.assertEquals( PROPERTY_INSTANCE_NAME + ": ", proposals.get( 2 ).getProposalString());
		Assert.assertNull( proposals.get( 2 ).getProposalDescription());
		Assert.assertEquals( 0, proposals.get( 2 ).getReplacementOffset());

		Assert.assertEquals( KEYWORD_INSTANCE_OF, proposals.get( 3 ).getProposalName());
		Assert.assertEquals( INSTANCE_OF_PREFIX, proposals.get( 3 ).getProposalString());
		Assert.assertNull( proposals.get( 3 ).getProposalDescription());
		Assert.assertEquals( 0, proposals.get( 3 ).getReplacementOffset());

		Assert.assertEquals( INSTANCE_OF_BLOCK, proposals.get( 4 ).getProposalName());
		Assert.assertTrue( proposals.get( 4 ).getProposalString().startsWith( INSTANCE_OF_PREFIX ));
		Assert.assertTrue( proposals.get( 4 ).getProposalDescription().startsWith( INSTANCE_OF_BLOCK ));
		Assert.assertEquals( 0, proposals.get( 4 ).getReplacementOffset());
	}


	@Test
	public void testOffsetInsideInstancesWithComponentPropertiesAfterInstance() throws Exception {

		// Expected:  "c2.ip", "channels:", "name:", instance of, instance of block
		Couple couple = prepare( "app3", "initial.instances", 142 );
		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( 5, proposals.size());

		Assert.assertEquals( "c2.ip: ", proposals.get( 0 ).getProposalName());
		Assert.assertEquals( "c2.ip: ", proposals.get( 0 ).getProposalString());
		Assert.assertEquals( CompletionUtils.SET_BY_ROBOCONF, proposals.get( 0 ).getProposalDescription());
		Assert.assertEquals( 0, proposals.get( 0 ).getReplacementOffset());

		Assert.assertEquals( PROPERTY_INSTANCE_CHANNELS + ": ", proposals.get( 1 ).getProposalName());
		Assert.assertEquals( PROPERTY_INSTANCE_CHANNELS + ": ", proposals.get( 1 ).getProposalString());
		Assert.assertNull( proposals.get( 1 ).getProposalDescription());
		Assert.assertEquals( 0, proposals.get( 1 ).getReplacementOffset());

		Assert.assertEquals( PROPERTY_INSTANCE_NAME + ": ", proposals.get( 2 ).getProposalName());
		Assert.assertEquals( PROPERTY_INSTANCE_NAME + ": ", proposals.get( 2 ).getProposalString());
		Assert.assertNull( proposals.get( 2 ).getProposalDescription());
		Assert.assertEquals( 0, proposals.get( 2 ).getReplacementOffset());

		Assert.assertEquals( KEYWORD_INSTANCE_OF, proposals.get( 3 ).getProposalName());
		Assert.assertEquals( INSTANCE_OF_PREFIX, proposals.get( 3 ).getProposalString());
		Assert.assertNull( proposals.get( 3 ).getProposalDescription());
		Assert.assertEquals( 0, proposals.get( 3 ).getReplacementOffset());

		Assert.assertEquals( INSTANCE_OF_BLOCK, proposals.get( 4 ).getProposalName());
		Assert.assertTrue( proposals.get( 4 ).getProposalString().startsWith( INSTANCE_OF_PREFIX ));
		Assert.assertTrue( proposals.get( 4 ).getProposalDescription().startsWith( INSTANCE_OF_BLOCK ));
		Assert.assertEquals( 0, proposals.get( 4 ).getReplacementOffset());
	}


	@Test
	public void testOffsetInsideInstancesWithComponentPropertiesAndPrefixAndWithoutInheritance() throws Exception {

		// Expected: "c2.ip", "channels:"
		Couple couple = prepare( "app3", "initial.instances", 61 );
		couple.text += "C";

		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( 2, proposals.size());

		Assert.assertEquals( "c2.ip: ", proposals.get( 0 ).getProposalName());
		Assert.assertEquals( "c2.ip: ", proposals.get( 0 ).getProposalString());
		Assert.assertEquals( CompletionUtils.SET_BY_ROBOCONF, proposals.get( 0 ).getProposalDescription());
		Assert.assertEquals( 1, proposals.get( 0 ).getReplacementOffset());

		Assert.assertEquals( PROPERTY_INSTANCE_CHANNELS + ": ", proposals.get( 1 ).getProposalName());
		Assert.assertEquals( PROPERTY_INSTANCE_CHANNELS + ": ", proposals.get( 1 ).getProposalString());
		Assert.assertNull( proposals.get( 1 ).getProposalDescription());
		Assert.assertEquals( 1, proposals.get( 1 ).getReplacementOffset());
	}


	@Test
	public void testOffsetInsideInstancesWithInvalidComponent() throws Exception {

		// Expected: nothing
		Couple couple = prepare( "app3", "initial.instances", 500 );
		couple.text += "instance of InvalidComponent {\n\t";

		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		verifyBasicInstanceProperties( proposals );
	}


	@Test
	public void testOffsetInsideInstancesWithNoGraph() throws Exception {

		// Expected: "channels:", "name:", instance of, instance of block
		Couple couple = prepare( "app4", "initial.instances", 61 );
		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		verifyBasicInstanceProperties( proposals );
	}


	@Test
	public void testOffsetInsideInstancesWithComponentPropertiesAndWithInheritance() throws Exception {

		// Expected: variables, "channels:", "name:", instance of, instance of block
		Map<String,String> expected = new LinkedHashMap<> ();
		expected.put( "c1.ip: ",  CompletionUtils.SET_BY_ROBOCONF );
		expected.put( "c1.port: ", CompletionUtils.DEFAULT_VALUE + "8100" );
		expected.put( "c1.v1: ", CompletionUtils.DEFAULT_VALUE + "version1" );
		expected.put( "c1.v2: ", CompletionUtils.DEFAULT_VALUE + "version2" );
		expected.put( PROPERTY_INSTANCE_CHANNELS + ": ", null );
		expected.put( "f1.v1: ", CompletionUtils.DEFAULT_VALUE + "version1" );
		expected.put( "f1.v2: ", CompletionUtils.DEFAULT_VALUE + "version2" );
		expected.put( PROPERTY_INSTANCE_NAME + ": ", null );

		Couple couple = prepare( "app3", "initial.instances", 135 );
		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( expected.size() + 2, proposals.size());

		int index = 0;
		for( Map.Entry<String,String> entry : expected.entrySet()) {
			Assert.assertEquals( entry.getKey(), proposals.get( index ).getProposalName());
			Assert.assertEquals( entry.getKey(), proposals.get( index ).getProposalString());
			Assert.assertEquals( entry.getValue(), proposals.get( index ).getProposalDescription());
			Assert.assertEquals( 0, proposals.get( index ).getReplacementOffset());
			index ++;
		}

		Assert.assertEquals( KEYWORD_INSTANCE_OF, proposals.get( index ).getProposalName());
		Assert.assertEquals( INSTANCE_OF_PREFIX, proposals.get( index ).getProposalString());
		Assert.assertNull( proposals.get( index ).getProposalDescription());
		Assert.assertEquals( 0, proposals.get( index ).getReplacementOffset());

		index ++;
		Assert.assertEquals( INSTANCE_OF_BLOCK, proposals.get( index ).getProposalName());
		Assert.assertTrue( proposals.get( index ).getProposalString().startsWith( INSTANCE_OF_PREFIX ));
		Assert.assertTrue( proposals.get( index ).getProposalDescription().startsWith( INSTANCE_OF_BLOCK ));
		Assert.assertEquals( 0, proposals.get( index ).getReplacementOffset());
	}


	@Test
	public void testOffsetInsideInstancesWithComponentPropertiesAndPrefixAndWithInheritance() throws Exception {

		// Expected: "f" variables
		Map<String,String> expected = new LinkedHashMap<> ();
		expected.put( "f1.v1: ", CompletionUtils.DEFAULT_VALUE + "version1" );
		expected.put( "f1.v2: ", CompletionUtils.DEFAULT_VALUE + "version2" );

		Couple couple = prepare( "app3", "initial.instances", 135 );
		couple.text += " F";

		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( expected.size(), proposals.size());

		int index = 0;
		for( Map.Entry<String,String> entry : expected.entrySet()) {
			Assert.assertEquals( entry.getKey(), proposals.get( index ).getProposalName());
			Assert.assertEquals( entry.getKey(), proposals.get( index ).getProposalString());
			Assert.assertEquals( entry.getValue(), proposals.get( index ).getProposalDescription());
			Assert.assertEquals( 1, proposals.get( index ).getReplacementOffset());
			index ++;
		}
	}


	@Test
	public void testOffsetForComponentNameWithNoGraph() throws Exception {

		// Expected: nothing
		Couple couple = prepare( "app4", "initial.instances", 91 );
		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( 0, proposals.size());
	}


	@Test
	public void testOffsetForRootComponentNameWithGraph() throws Exception {

		// Expected: "c2"
		Couple couple = prepare( "app3", "initial.instances", 91 );
		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( 1, proposals.size());

		Assert.assertEquals( "c2", proposals.get( 0 ).getProposalName());
		Assert.assertEquals( "c2", proposals.get( 0 ).getProposalString());
		Assert.assertNull( proposals.get( 0 ).getProposalDescription());
		Assert.assertEquals( 0, proposals.get( 0 ).getReplacementOffset());
	}


	@Test
	public void testOffsetForRootComponentNameWithGraphAndPrefix() throws Exception {

		// Expected: "c2"
		Couple couple = prepare( "app3", "initial.instances", 91 );
		couple.text += " C";

		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( 1, proposals.size());

		Assert.assertEquals( "c2", proposals.get( 0 ).getProposalName());
		Assert.assertEquals( "c2", proposals.get( 0 ).getProposalString());
		Assert.assertNull( proposals.get( 0 ).getProposalDescription());
		Assert.assertEquals( 1, proposals.get( 0 ).getReplacementOffset());
	}


	@Test
	public void testOffsetForRootComponentNameWithGraphAndInvalidPrefix_1() throws Exception {

		// Expected: nothing
		Couple couple = prepare( "app3", "initial.instances", 91 );
		couple.text += " C ";

		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( 0, proposals.size());
	}


	@Test
	public void testOffsetForRootComponentNameWithGraphAndInvalidPrefix_2() throws Exception {

		// Expected: nothing
		Couple couple = prepare( "app3", "initial.instances", 91 );
		couple.text += "d";

		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( 0, proposals.size());
	}


	@Test
	public void testOffsetForChildComponentNameWithGraph() throws Exception {

		// Expected: "c1"
		Couple couple = prepare( "app3", "initial.instances", 125 );
		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( 1, proposals.size());

		Assert.assertEquals( "c1", proposals.get( 0 ).getProposalName());
		Assert.assertEquals( "c1", proposals.get( 0 ).getProposalString());
		Assert.assertEquals( "A comment about c1", proposals.get( 0 ).getProposalDescription());
		Assert.assertEquals( 0, proposals.get( 0 ).getReplacementOffset());
	}


	@Test
	public void testOffsetForChildComponentNameWithGraphAndPrefix() throws Exception {

		// Expected: "c1"
		Couple couple = prepare( "app3", "initial.instances", 125 );
		couple.text += " C";

		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( 1, proposals.size());

		Assert.assertEquals( "c1", proposals.get( 0 ).getProposalName());
		Assert.assertEquals( "c1", proposals.get( 0 ).getProposalString());
		Assert.assertEquals( "A comment about c1", proposals.get( 0 ).getProposalDescription());
		Assert.assertEquals( 1, proposals.get( 0 ).getReplacementOffset());
	}


	@Test
	public void testOffsetForChildComponentNameWithGraphAndInvalidPrefix_1() throws Exception {

		// Expected: nothing
		Couple couple = prepare( "app3", "initial.instances", 125 );
		couple.text += " C ";

		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( 0, proposals.size());
	}


	@Test
	public void testOffsetForChildComponentNameWithGraphAndInvalidPrefix_2() throws Exception {

		// Expected: nothing
		Couple couple = prepare( "app3", "initial.instances", 125 );
		couple.text += "d";

		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( 0, proposals.size());
	}


	@Test
	public void testOffsetForComponentNameAndInvalidParent() throws Exception {

		// Expected: nothing
		Couple couple = prepare( "app3", "initial.instances", 700 );
		couple.text += "instance of invalid {\n\tinstance of  ";

		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( 0, proposals.size());
	}


	@Test
	public void testOffsetForComponentNameAndValidParent() throws Exception {

		// Expected: "c1"
		Couple couple = prepare( "app3", "initial.instances", 700 );
		couple.text += "instance of c2 {\n\tinstance of  ";

		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( 1, proposals.size());

		Assert.assertEquals( "c1", proposals.get( 0 ).getProposalName());
		Assert.assertEquals( "c1", proposals.get( 0 ).getProposalString());
		Assert.assertEquals( "A comment about c1", proposals.get( 0 ).getProposalDescription());
		Assert.assertEquals( 0, proposals.get( 0 ).getReplacementOffset());
	}


	@Test
	public void verifyIndentation_1() throws Exception {

		Couple couple = prepare( "app3", "initial.instances", 700 );
		couple.text = "";

		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( 3, proposals.size());

		Assert.assertEquals( INSTANCE_OF_BLOCK, proposals.get( 2 ).getProposalName());
		Assert.assertEquals( indentation( 0 ), proposals.get( 2 ).getProposalString());
		Assert.assertEquals( 0, proposals.get( 2 ).getReplacementOffset());
	}


	@Test
	public void verifyIndentation_2() throws Exception {

		Couple couple = prepare( "app3", "initial.instances", 700 );
		couple.text = "instance of n1 {\n";

		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( 4, proposals.size());

		Assert.assertEquals( INSTANCE_OF_BLOCK, proposals.get( 3 ).getProposalName());
		Assert.assertEquals( indentation( 1 ), proposals.get( 3 ).getProposalString());
		Assert.assertEquals( 0, proposals.get( 3 ).getReplacementOffset());
	}


	@Test
	public void verifyIndentation_3() throws Exception {

		Couple couple = prepare( "app3", "initial.instances", 700 );
		couple.text = "instance of n1 {\n\tinstance of n2 {\n";

		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( 4, proposals.size());

		Assert.assertEquals( INSTANCE_OF_BLOCK, proposals.get( 3 ).getProposalName());
		Assert.assertEquals( indentation( 2 ), proposals.get( 3 ).getProposalString());
		Assert.assertEquals( 0, proposals.get( 3 ).getReplacementOffset());
	}


	@Test
	public void verifyIndentation_4() throws Exception {

		Couple couple = prepare( "app3", "initial.instances", 700 );
		couple.text = "instance of n1 {\n\tinstance of n2 {\n\t\tinstance of n3 {\n";

		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( 4, proposals.size());

		Assert.assertEquals( INSTANCE_OF_BLOCK, proposals.get( 3 ).getProposalName());
		Assert.assertEquals( indentation( 3 ), proposals.get( 3 ).getProposalString());
		Assert.assertEquals( 0, proposals.get( 3 ).getReplacementOffset());
	}


	@Test
	public void verifyIndentationWithBadIndentationForParent() throws Exception {

		Couple couple = prepare( "app3", "initial.instances", 700 );
		couple.text = "instance of n1 {\ninstance of n2 {\n";

		// instance of n1 {
		// instance of n2 {
		// ...
		// => We take n2's indentation as a reference.

		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( 4, proposals.size());

		Assert.assertEquals( INSTANCE_OF_BLOCK, proposals.get( 3 ).getProposalName());
		Assert.assertEquals( indentation( 1 ), proposals.get( 3 ).getProposalString());
		Assert.assertEquals( 0, proposals.get( 3 ).getReplacementOffset());
	}


	@Test
	public void testOffsetForInstancesImportAllFiles() throws Exception {

		// Expected: 3 files to import
		Couple couple = prepare( "app4", "initial.instances", 700 );
		couple.text += "\nimport ";

		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( 3, proposals.size());

		String[] expected = {
				"imports/imp1.instances",
				"imports/subimports/imp2.instances",
				"imports/subimports/imp3.instances"
		};

		for( int i=0; i<expected.length; i++ ) {
			Assert.assertEquals( expected[ i ], proposals.get( i ).getProposalName());
			Assert.assertEquals( expected[ i ], proposals.get( i ).getProposalString());
			Assert.assertNull( proposals.get( i ).getProposalDescription());
			Assert.assertEquals( 0, proposals.get( i ).getReplacementOffset());
		}
	}


	@Test
	public void testOffsetForInstancesImportNoSpaceAfterKeyword() throws Exception {

		// Expected: "import"
		Couple couple = prepare( "app4", "initial.instances", 700 );
		couple.text += "\nimport";

		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( 1, proposals.size());

		Assert.assertEquals( KEYWORD_IMPORT, proposals.get( 0 ).getProposalName());
		Assert.assertEquals( IMPORT_PREFIX, proposals.get( 0 ).getProposalString());
		Assert.assertNull( proposals.get( 0 ).getProposalDescription());
		Assert.assertEquals( KEYWORD_IMPORT.length(), proposals.get( 0 ).getReplacementOffset());
	}


	@Test
	public void testOffsetForInstancesImportWithPrefix() throws Exception {

		// Expected: 2 files to import
		Couple couple = prepare( "app4", "initial.instances", 700 );
		couple.text += "\nimport imports/s";

		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( 2, proposals.size());

		String[] expected = {
				"imports/subimports/imp2.instances",
				"imports/subimports/imp3.instances"
		};

		for( int i=0; i<expected.length; i++ ) {
			Assert.assertEquals( expected[ i ], proposals.get( i ).getProposalName());
			Assert.assertEquals( expected[ i ], proposals.get( i ).getProposalString());
			Assert.assertNull( proposals.get( i ).getProposalDescription());
			Assert.assertEquals( 0, proposals.get( i ).getReplacementOffset());
		}
	}


	@Test
	public void testOffsetForInstancesImportAvoidDuplicates() throws Exception {

		// Expected: 2 files to import
		Couple couple = prepare( "app4", "initial.instances", 700 );
		couple.text += "\nimport imports/subimports/imp2.instances\n\nimport ";

		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( 2, proposals.size());

		String[] expected = {
				"imports/imp1.instances",
				"imports/subimports/imp3.instances"
		};

		for( int i=0; i<expected.length; i++ ) {
			Assert.assertEquals( expected[ i ], proposals.get( i ).getProposalName());
			Assert.assertEquals( expected[ i ], proposals.get( i ).getProposalString());
			Assert.assertNull( proposals.get( i ).getProposalDescription());
			Assert.assertEquals( 0, proposals.get( i ).getReplacementOffset());
		}
	}


	@Test
	public void testOffsetForInstancesImportAvoidNastyDuplicates() throws Exception {

		// Expected: 2 files to import
		Couple couple = prepare( "app4", "initial.instances", 700 );
		couple.text += "\nimport imports/subimports/imp2.instances  ;   \n\nimport imp";

		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( 2, proposals.size());

		String[] expected = {
				"imports/imp1.instances",
				"imports/subimports/imp3.instances"
		};

		for( int i=0; i<expected.length; i++ ) {
			Assert.assertEquals( expected[ i ], proposals.get( i ).getProposalName());
			Assert.assertEquals( expected[ i ], proposals.get( i ).getProposalString());
			Assert.assertNull( proposals.get( i ).getProposalDescription());
			Assert.assertEquals( 0, proposals.get( i ).getReplacementOffset());
		}
	}


	@Test
	public void testFindWhenNoDirectory() {

		InstancesCompletionProposer proposer = new InstancesCompletionProposer( null, null );
		Assert.assertEquals( 0, proposer.findComponentNames( null ).size());
		Assert.assertEquals( 2, proposer.findExportedVariableNames( null ).size());
	}


	/**
	 * Prepares the expected replacement text with the right indentation.
	 * @param level the indentation level
	 * @return a non-null string
	 */
	private String indentation( int level ) {

		StringBuilder sb = new StringBuilder();
		sb.append( "instance of component {\n" );
		for( int i=0; i<=level; i++ )
			sb.append( "\t" );

		sb.append( "name: name;\n" );
		for( int i=0; i<level; i++ )
		sb.append( "\t" );

		sb.append( "}" );
		return sb.toString();
	}


	/**
	 * Verifies neutral proposals.
	 * <p>
	 * When the offset is at the beginning of the document or between two instances definitions,
	 * the same proposals should be made (with imports).
	 * </p>
	 *
	 * @param proposals a non-null list of proposals
	 */
	private void verifyNeutralOffset( List<RoboconfCompletionProposal> proposals ) {

		Assert.assertEquals( 3, proposals.size());

		Assert.assertEquals( KEYWORD_IMPORT, proposals.get( 0 ).getProposalName());
		Assert.assertEquals( IMPORT_PREFIX, proposals.get( 0 ).getProposalString());
		Assert.assertNull( proposals.get( 0 ).getProposalDescription());
		Assert.assertEquals( 0, proposals.get( 0 ).getReplacementOffset());

		Assert.assertEquals( KEYWORD_INSTANCE_OF, proposals.get( 1 ).getProposalName());
		Assert.assertEquals( INSTANCE_OF_PREFIX, proposals.get( 1 ).getProposalString());
		Assert.assertNull( proposals.get( 1 ).getProposalDescription());
		Assert.assertEquals( 0, proposals.get( 1 ).getReplacementOffset());

		Assert.assertEquals( INSTANCE_OF_BLOCK, proposals.get( 2 ).getProposalName());
		Assert.assertTrue( proposals.get( 2 ).getProposalString().startsWith( INSTANCE_OF_PREFIX ));
		Assert.assertTrue( proposals.get( 2 ).getProposalDescription().startsWith( INSTANCE_OF_BLOCK ));
		Assert.assertEquals( 0, proposals.get( 2 ).getReplacementOffset());
	}


	/**
	 * Verifies neutral proposals.
	 * <p>
	 * When the offset is at the beginning of the document or between two instances definitions,
	 * the same proposals should be made (with imports).
	 * </p>
	 *
	 * @param proposals a non-null list of proposals
	 */
	private void verifyBasicInstanceProperties( List<RoboconfCompletionProposal> proposals ) {

		Assert.assertEquals( 4, proposals.size());

		Assert.assertEquals( PROPERTY_INSTANCE_CHANNELS + ": ", proposals.get( 0 ).getProposalName());
		Assert.assertEquals( PROPERTY_INSTANCE_CHANNELS + ": ", proposals.get( 0 ).getProposalString());
		Assert.assertNull( proposals.get( 0 ).getProposalDescription());
		Assert.assertEquals( 0, proposals.get( 0 ).getReplacementOffset());

		Assert.assertEquals( PROPERTY_INSTANCE_NAME + ": ", proposals.get( 1 ).getProposalName());
		Assert.assertEquals( PROPERTY_INSTANCE_NAME + ": ", proposals.get( 1 ).getProposalString());
		Assert.assertNull( proposals.get( 1 ).getProposalDescription());
		Assert.assertEquals( 0, proposals.get( 1 ).getReplacementOffset());

		Assert.assertEquals( KEYWORD_INSTANCE_OF, proposals.get( 2 ).getProposalName());
		Assert.assertEquals( INSTANCE_OF_PREFIX, proposals.get( 2 ).getProposalString());
		Assert.assertNull( proposals.get( 2 ).getProposalDescription());
		Assert.assertEquals( 0, proposals.get( 2 ).getReplacementOffset());

		Assert.assertEquals( INSTANCE_OF_BLOCK, proposals.get( 3 ).getProposalName());
		Assert.assertTrue( proposals.get( 3 ).getProposalString().startsWith( INSTANCE_OF_PREFIX ));
		Assert.assertTrue( proposals.get( 3 ).getProposalDescription().startsWith( INSTANCE_OF_BLOCK ));
		Assert.assertEquals( 0, proposals.get( 3 ).getReplacementOffset());
	}


	protected Couple prepare( String appName, String fileName, int offset )
	throws IOException, URISyntaxException {

		File appDir = TestUtils.findTestFile( "/completion/" + appName );
		File instancesFile = new File( appDir, Constants.PROJECT_DIR_INSTANCES + "/" + fileName );

		Couple result = new Couple();
		result.proposer = new InstancesCompletionProposer( appDir, instancesFile );

		String cacheKey = appName + "/" + fileName;
		String fileContent = CACHE.get( cacheKey );
		if( fileContent == null ) {
			fileContent = Utils.readFileContent( instancesFile );
			CACHE.put( cacheKey, fileContent );
		}

		result.text = fileContent;
		if( offset < fileContent.length())
			result.text = fileContent.substring( 0, offset );

		return result;
	}
}
