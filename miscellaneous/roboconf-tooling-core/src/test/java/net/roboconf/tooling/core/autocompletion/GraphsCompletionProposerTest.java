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

import static net.roboconf.core.dsl.ParsingConstants.KEYWORD_FACET;
import static net.roboconf.core.dsl.ParsingConstants.KEYWORD_IMPORT;
import static net.roboconf.core.dsl.ParsingConstants.PROPERTY_GRAPH_EXPORTS;
import static net.roboconf.tooling.core.autocompletion.GraphsCompletionProposer.COMPONENT_BLOCK;
import static net.roboconf.tooling.core.autocompletion.GraphsCompletionProposer.COMPONENT_PROPERTY_NAMES;
import static net.roboconf.tooling.core.autocompletion.GraphsCompletionProposer.FACET_BLOCK;
import static net.roboconf.tooling.core.autocompletion.GraphsCompletionProposer.FACET_PREFIX;
import static net.roboconf.tooling.core.autocompletion.GraphsCompletionProposer.FACET_PROPERTY_NAMES;
import static net.roboconf.tooling.core.autocompletion.GraphsCompletionProposer.IMPORT_PREFIX;
import static net.roboconf.tooling.core.autocompletion.GraphsCompletionProposer.KNOWN_INSTALLERS;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.Assert;
import org.junit.Test;

import net.roboconf.core.Constants;
import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.utils.Utils;
import net.roboconf.tooling.core.autocompletion.ICompletionProposer.RoboconfCompletionProposal;

/**
 * @author Vincent Zurczak - Linagora
 */
public class GraphsCompletionProposerTest extends AbstractCompletionProposerTest {

	@Test
	public void testOffsetAtZero() throws Exception {

		// Expected: import, facet, facet block, component block
		Couple couple = prepare( "app1", "edited1.graph", 0 );
		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		verifyNeutralOffset( proposals );
	}


	@Test
	public void testOffsetInComment() throws Exception {

		// Expected: nothing
		Couple couple = prepare( "app1", "edited1.graph", 9 );
		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( 0, proposals.size());
	}


	@Test
	public void testOffsetRightBeforeComment_1() throws Exception {

		// Expected: import, facet, facet block, component block
		Couple couple = prepare( "app1", "edited1.graph", 1 );
		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		verifyNeutralOffset( proposals );
	}


	@Test
	public void testOffsetRightBeforeComment_2() throws Exception {

		// Expected: import, facet, facet block, component block
		Couple couple = prepare( "app1", "edited1.graph", 21 );
		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		verifyNeutralOffset( proposals );
	}


	@Test
	public void testOffsetRightAfterCommentSymbol() throws Exception {

		// Expected: nothing
		Couple couple = prepare( "app1", "edited1.graph", 2 );
		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( 0, proposals.size());
	}


	@Test
	public void testOffsetBetweenTwoTypes() throws Exception {

		// Expected: import, facet, facet block, component block
		Couple couple = prepare( "app1", "edited1.graph", 44 );
		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		verifyNeutralOffset( proposals );

		couple = prepare( "app1", "edited1.graph", 60 );
		proposals = couple.proposer.findProposals( couple.text );
		verifyNeutralOffset( proposals );

		couple = prepare( "app1", "edited1.graph", 70 );
		proposals = couple.proposer.findProposals( couple.text );
		verifyNeutralOffset( proposals );

		// Right after a type
		couple.text = "comp {\n}\n";
		proposals = couple.proposer.findProposals( couple.text );
		verifyNeutralOffset( proposals );

		// Right after a closing curly bracket
		couple.text = "comp {\n}";
		proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( 0, proposals.size());
	}


	@Test
	public void testOffsetInsideFacetBeginning() throws Exception {

		// Expected: facet, facet block
		Couple couple = prepare( "app1", "edited1.graph", 70 );
		couple.text += "\nfac";

		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( 2, proposals.size());

		Assert.assertEquals( KEYWORD_FACET, proposals.get( 0 ).getProposalName());
		Assert.assertEquals( FACET_PREFIX, proposals.get( 0 ).getProposalString());
		Assert.assertNull( proposals.get( 0 ).getProposalDescription());
		Assert.assertEquals( 3, proposals.get( 0 ).getReplacementOffset());

		Assert.assertEquals( FACET_BLOCK, proposals.get( 1 ).getProposalName());
		Assert.assertTrue( proposals.get( 1 ).getProposalString().startsWith( FACET_PREFIX ));
		Assert.assertTrue( proposals.get( 1 ).getProposalDescription().startsWith( FACET_BLOCK ));
		Assert.assertEquals( 3, proposals.get( 1 ).getReplacementOffset());
	}


	@Test
	public void testOffsetInsideFakeFacetBeginning() throws Exception {

		// Expected: nothing
		Couple couple = prepare( "app1", "edited1.graph", 70 );
		couple.text += "\nfac anotherWord";

		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( 0, proposals.size());
	}


	@Test
	public void testOffsetRightAfterClosingBracket() throws Exception {

		// Expected: nothing
		Couple couple = prepare( "app1", "edited1.graph", 59 );
		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( 0, proposals.size());
	}


	@Test
	public void testOffsetInsideImportBeginning() throws Exception {

		// Expected: import
		Couple couple = prepare( "app1", "edited1.graph", 70 );
		couple.text += "\nimpo";

		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( 1, proposals.size());

		Assert.assertEquals( KEYWORD_IMPORT, proposals.get( 0 ).getProposalName());
		Assert.assertEquals( IMPORT_PREFIX, proposals.get( 0 ).getProposalString());
		Assert.assertNull( proposals.get( 0 ).getProposalDescription());
		Assert.assertEquals( 4, proposals.get( 0 ).getReplacementOffset());
	}


	@Test
	public void testOffsetInsideImportBeginningAndUpperCase() throws Exception {

		// Expected: import
		Couple couple = prepare( "app1", "edited1.graph", 70 );
		couple.text += "\niMPo";

		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( 1, proposals.size());

		Assert.assertEquals( KEYWORD_IMPORT, proposals.get( 0 ).getProposalName());
		Assert.assertEquals( IMPORT_PREFIX, proposals.get( 0 ).getProposalString());
		Assert.assertNull( proposals.get( 0 ).getProposalDescription());
		Assert.assertEquals( 4, proposals.get( 0 ).getReplacementOffset());
	}


	@Test
	public void testOffsetInsideFacet() throws Exception {

		// Expected: all the facet properties
		Couple couple = prepare( "app1", "edited1.graph", 70 );
		couple.text += "\nfacet f {\n\t";

		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( FACET_PROPERTY_NAMES.length, proposals.size());

		for( int i=0; i<FACET_PROPERTY_NAMES.length; i++ ) {
			Assert.assertEquals( FACET_PROPERTY_NAMES[ i ], FACET_PROPERTY_NAMES[ i ] + ": ", proposals.get( i ).getProposalName());
			Assert.assertEquals( FACET_PROPERTY_NAMES[ i ], FACET_PROPERTY_NAMES[ i ] + ": ", proposals.get( i ).getProposalString());
			Assert.assertNull( FACET_PROPERTY_NAMES[ i ], proposals.get( i ).getProposalDescription());
			Assert.assertEquals( FACET_PROPERTY_NAMES[ i ], 0, proposals.get( i ).getReplacementOffset());
		}
	}


	@Test
	public void testOffsetInsideFacetWithValidPrefix() throws Exception {

		// Expected: "exports:"
		Couple couple = prepare( "app1", "edited1.graph", 70 );
		couple.text += "\nfacet f {\n\texp";

		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( 1, proposals.size());

		Assert.assertEquals( PROPERTY_GRAPH_EXPORTS + ": ", proposals.get( 0 ).getProposalName());
		Assert.assertEquals( PROPERTY_GRAPH_EXPORTS + ": ", proposals.get( 0 ).getProposalString());
		Assert.assertNull( proposals.get( 0 ).getProposalDescription());
		Assert.assertEquals( 3, proposals.get( 0 ).getReplacementOffset());
	}


	@Test
	public void testOffsetInsideFacetWithInvalidPrefix() throws Exception {

		// Expected: nothing
		Couple couple = prepare( "app1", "edited1.graph", 70 );
		couple.text += "\nfacet f {\n\tnaw";

		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( 0, proposals.size());
	}


	@Test
	public void testOffsetInsideComponent() throws Exception {

		// Expected: all the component properties
		Couple couple = prepare( "app1", "edited1.graph", 70 );
		couple.text += "\ncomp {\n\t";

		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( COMPONENT_PROPERTY_NAMES.length, proposals.size());

		for( int i=0; i<COMPONENT_PROPERTY_NAMES.length; i++ ) {
			Assert.assertEquals( COMPONENT_PROPERTY_NAMES[ i ], COMPONENT_PROPERTY_NAMES[ i ] + ": ", proposals.get( i ).getProposalName());
			Assert.assertEquals( COMPONENT_PROPERTY_NAMES[ i ], COMPONENT_PROPERTY_NAMES[ i ] + ": ", proposals.get( i ).getProposalString());
			Assert.assertNull( COMPONENT_PROPERTY_NAMES[ i ], proposals.get( i ).getProposalDescription());
			Assert.assertEquals( COMPONENT_PROPERTY_NAMES[ i ], 0, proposals.get( i ).getReplacementOffset());
		}
	}


	@Test
	public void testOffsetInsideComponentWithValidPrefix() throws Exception {

		// Expected: "exports:"
		Couple couple = prepare( "app1", "edited1.graph", 70 );
		couple.text += "\ncomp {\n\texpo";

		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( 1, proposals.size());

		Assert.assertEquals( PROPERTY_GRAPH_EXPORTS + ": ", proposals.get( 0 ).getProposalName());
		Assert.assertEquals( PROPERTY_GRAPH_EXPORTS + ": ", proposals.get( 0 ).getProposalString());
		Assert.assertNull( proposals.get( 0 ).getProposalDescription());
		Assert.assertEquals( 4, proposals.get( 0 ).getReplacementOffset());
	}


	@Test
	public void testOffsetInsideComponentWithInvalidPrefix() throws Exception {

		// Expected: nothing
		Couple couple = prepare( "app1", "edited1.graph", 70 );
		couple.text += "\ncomp {\n\tnaw";

		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( 0, proposals.size());
	}


	@Test
	public void testOffsetInsideComponentRightBeforeColon() throws Exception {

		// Expected: "exports"
		Couple couple = prepare( "app1", "edited1.graph", 70 );
		couple.text += "\ncomp {\n\texport";

		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( 1, proposals.size());

		Assert.assertEquals( PROPERTY_GRAPH_EXPORTS + ": ", proposals.get( 0 ).getProposalName());
		Assert.assertEquals( PROPERTY_GRAPH_EXPORTS + ": ", proposals.get( 0 ).getProposalString());
		Assert.assertNull( proposals.get( 0 ).getProposalDescription());
		Assert.assertEquals( 6, proposals.get( 0 ).getReplacementOffset());
	}


	@Test
	public void testOffsetInsideComponentRightAfterColon() throws Exception {

		// Expected: "exports:"
		Couple couple = prepare( "app1", "edited1.graph", 70 );
		couple.text += "\ncomp {\n\texports:";

		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( 1, proposals.size());

		Assert.assertEquals( PROPERTY_GRAPH_EXPORTS + ": ", proposals.get( 0 ).getProposalName());
		Assert.assertEquals( PROPERTY_GRAPH_EXPORTS + ": ", proposals.get( 0 ).getProposalString());
		Assert.assertNull( proposals.get( 0 ).getProposalDescription());
		Assert.assertEquals( 8, proposals.get( 0 ).getReplacementOffset());
	}


	@Test
	public void testOffsetInsideComponentAfterColon() throws Exception {

		// Expected: nothing
		Couple couple = prepare( "app1", "edited1.graph", 70 );
		couple.text += "\ncomp {\n\texports: ";

		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( 0, proposals.size());
	}


	@Test
	public void testOffsetForInstallerNamesWithoutPrefix() throws Exception {

		// Expected: all the installer names
		Couple couple = prepare( "app1", "edited1.graph", 70 );
		couple.text += "\ncomp {\n\tinstaller:  ";

		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		List<String> installerNames = new ArrayList<>( Arrays.asList( KNOWN_INSTALLERS ));
		Collections.sort( installerNames );
		Assert.assertEquals( installerNames.size(), proposals.size());

		for( int i=0; i<installerNames.size(); i++ ) {
			Assert.assertEquals( installerNames.get( i ), installerNames.get( i ), proposals.get( i ).getProposalName());
			Assert.assertEquals( installerNames.get( i ), installerNames.get( i ), proposals.get( i ).getProposalString());
			Assert.assertNull( installerNames.get( i ), proposals.get( i ).getProposalDescription());
			Assert.assertEquals( installerNames.get( i ), 0, proposals.get( i ).getReplacementOffset());
		}
	}


	@Test
	public void testOffsetForInstallerNamesWithValidPrefix() throws Exception {

		// Expected: logger
		Couple couple = prepare( "app1", "edited1.graph", 70 );
		couple.text += "\ncomp {\n\tinstaller: log";

		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( 1, proposals.size());

		Assert.assertEquals( "logger", proposals.get( 0 ).getProposalName());
		Assert.assertEquals( "logger", proposals.get( 0 ).getProposalString());
		Assert.assertNull( proposals.get( 0 ).getProposalDescription());
		Assert.assertEquals( 3, proposals.get( 0 ).getReplacementOffset());
	}


	@Test
	public void testOffsetForInstallerNamesWithInvalidPrefix_1() throws Exception {

		// Expected: nothing
		Couple couple = prepare( "app1", "edited1.graph", 70 );
		couple.text += "\ncomp {\n\tinstaller: what";

		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( 0, proposals.size());
	}


	@Test
	public void testOffsetForInstallerNamesWithInvalidPrefix_2() throws Exception {

		// Expected: nothing
		Couple couple = prepare( "app1", "edited1.graph", 70 );
		couple.text += "\ncomp {\n\tinstaller: log ";

		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( 0, proposals.size());
	}


	@Test
	public void testOffsetForInstallerNamesWithValidPrefixAndUpperCase() throws Exception {

		// Expected: logger
		Couple couple = prepare( "app1", "edited1.graph", 70 );
		couple.text += "\ncomp {\n\tinstaller: LOg";

		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( 1, proposals.size());

		Assert.assertEquals( "logger", proposals.get( 0 ).getProposalName());
		Assert.assertEquals( "logger", proposals.get( 0 ).getProposalString());
		Assert.assertNull( proposals.get( 0 ).getProposalDescription());
		Assert.assertEquals( 3, proposals.get( 0 ).getReplacementOffset());
	}


	@Test
	public void testOffsetForFacetChildrenWithoutPrefix() throws Exception {

		// Expected: c1, c2, f1
		Couple couple = prepare( "app2", "edited2.graph", 140 );
		couple.text += "\nfacet myfacet {\n\tchildren: ";

		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( 3, proposals.size());

		Assert.assertEquals( "c1", proposals.get( 0 ).getProposalName());
		Assert.assertEquals( "c1", proposals.get( 0 ).getProposalString());
		Assert.assertNull( proposals.get( 0 ).getProposalDescription());
		Assert.assertEquals( 0, proposals.get( 0 ).getReplacementOffset());

		Assert.assertEquals( "c2", proposals.get( 1 ).getProposalName());
		Assert.assertEquals( "c2", proposals.get( 1 ).getProposalString());
		Assert.assertNull( proposals.get( 1 ).getProposalDescription());
		Assert.assertEquals( 0, proposals.get( 1 ).getReplacementOffset());

		Assert.assertEquals( "f1", proposals.get( 2 ).getProposalName());
		Assert.assertEquals( "f1", proposals.get( 2 ).getProposalString());
		Assert.assertNull( proposals.get( 2 ).getProposalDescription());
		Assert.assertEquals( 0, proposals.get( 2 ).getReplacementOffset());
	}


	@Test
	public void testOffsetForFacetChildrenWithPrefix() throws Exception {

		// Expected: c1, c2
		Couple couple = prepare( "app2", "edited2.graph", 140 );
		couple.text += "\nfacet myfacet {\n\tchildren: C";

		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( 2, proposals.size());

		Assert.assertEquals( "c1", proposals.get( 0 ).getProposalName());
		Assert.assertEquals( "c1", proposals.get( 0 ).getProposalString());
		Assert.assertNull( proposals.get( 0 ).getProposalDescription());
		Assert.assertEquals( 1, proposals.get( 0 ).getReplacementOffset());

		Assert.assertEquals( "c2", proposals.get( 1 ).getProposalName());
		Assert.assertEquals( "c2", proposals.get( 1 ).getProposalString());
		Assert.assertNull( proposals.get( 1 ).getProposalDescription());
		Assert.assertEquals( 1, proposals.get( 1 ).getReplacementOffset());
	}


	@Test
	public void testOffsetForComponentChildrenWithoutPrefix() throws Exception {

		// Expected: c1, c2, f1
		Couple couple = prepare( "app2", "edited2.graph", 140 );
		couple.text += "\ncomp {\n\tchildren: ";

		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( 3, proposals.size());

		Assert.assertEquals( "c1", proposals.get( 0 ).getProposalName());
		Assert.assertEquals( "c1", proposals.get( 0 ).getProposalString());
		Assert.assertNull( proposals.get( 0 ).getProposalDescription());
		Assert.assertEquals( 0, proposals.get( 0 ).getReplacementOffset());

		Assert.assertEquals( "c2", proposals.get( 1 ).getProposalName());
		Assert.assertEquals( "c2", proposals.get( 1 ).getProposalString());
		Assert.assertNull( proposals.get( 1 ).getProposalDescription());
		Assert.assertEquals( 0, proposals.get( 1 ).getReplacementOffset());

		Assert.assertEquals( "f1", proposals.get( 2 ).getProposalName());
		Assert.assertEquals( "f1", proposals.get( 2 ).getProposalString());
		Assert.assertNull( proposals.get( 2 ).getProposalDescription());
		Assert.assertEquals( 0, proposals.get( 2 ).getReplacementOffset());
	}


	@Test
	public void testOffsetForComponentChildrenWithPrefix() throws Exception {

		// Expected: c1, c2
		Couple couple = prepare( "app2", "edited2.graph", 140 );
		couple.text += "\ncomp {\n\tchildren: C";

		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( 2, proposals.size());

		Assert.assertEquals( "c1", proposals.get( 0 ).getProposalName());
		Assert.assertEquals( "c1", proposals.get( 0 ).getProposalString());
		Assert.assertNull( proposals.get( 0 ).getProposalDescription());
		Assert.assertEquals( 1, proposals.get( 0 ).getReplacementOffset());

		Assert.assertEquals( "c2", proposals.get( 1 ).getProposalName());
		Assert.assertEquals( "c2", proposals.get( 1 ).getProposalString());
		Assert.assertNull( proposals.get( 1 ).getProposalDescription());
		Assert.assertEquals( 1, proposals.get( 1 ).getReplacementOffset());
	}


	@Test
	public void testOffsetForFacetExtendsWithoutPrefix() throws Exception {

		// Expected: f1
		Couple couple = prepare( "app2", "edited2.graph", 140 );
		couple.text += "\nfacet myfacet {\n\textends: ";

		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( 1, proposals.size());

		Assert.assertEquals( "f1", proposals.get( 0 ).getProposalName());
		Assert.assertEquals( "f1", proposals.get( 0 ).getProposalString());
		Assert.assertNull( proposals.get( 0 ).getProposalDescription());
		Assert.assertEquals( 0, proposals.get( 0 ).getReplacementOffset());
	}


	@Test
	public void testOffsetForFacetExtendsWithValidPrefix() throws Exception {

		// Expected: f1
		Couple couple = prepare( "app2", "edited2.graph", 140 );
		couple.text += "\nfacet myfacet {\n\textends: f";

		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( 1, proposals.size());

		Assert.assertEquals( "f1", proposals.get( 0 ).getProposalName());
		Assert.assertEquals( "f1", proposals.get( 0 ).getProposalString());
		Assert.assertNull( proposals.get( 0 ).getProposalDescription());
		Assert.assertEquals( 1, proposals.get( 0 ).getReplacementOffset());
	}


	@Test
	public void testOffsetForFacetExtendsWithInvalidPrefix() throws Exception {

		// Expected: nothing
		Couple couple = prepare( "app2", "edited2.graph", 140 );
		couple.text += "\nfacet myfacet {\n\textends: no";

		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( 0, proposals.size());
	}


	@Test
	public void testOffsetForComponentExtendsWithoutPrefix() throws Exception {

		// Expected: f1
		Couple couple = prepare( "app2", "edited2.graph", 140 );
		couple.text += "\ncomp {\n\textends: ";

		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( 2, proposals.size());

		Assert.assertEquals( "c1", proposals.get( 0 ).getProposalName());
		Assert.assertEquals( "c1", proposals.get( 0 ).getProposalString());
		Assert.assertNull( proposals.get( 0 ).getProposalDescription());
		Assert.assertEquals( 0, proposals.get( 0 ).getReplacementOffset());

		Assert.assertEquals( "c2", proposals.get( 1 ).getProposalName());
		Assert.assertEquals( "c2", proposals.get( 1 ).getProposalString());
		Assert.assertNull( proposals.get( 1 ).getProposalDescription());
		Assert.assertEquals( 0, proposals.get( 1 ).getReplacementOffset());
	}


	@Test
	public void testOffsetForComponentExtendsWithValidPrefix() throws Exception {

		// Expected: c1, c2
		Couple couple = prepare( "app2", "edited2.graph", 140 );
		couple.text += "\ncomp {\n\textends: C";

		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( 2, proposals.size());

		Assert.assertEquals( "c1", proposals.get( 0 ).getProposalName());
		Assert.assertEquals( "c1", proposals.get( 0 ).getProposalString());
		Assert.assertNull( proposals.get( 0 ).getProposalDescription());
		Assert.assertEquals( 1, proposals.get( 0 ).getReplacementOffset());

		Assert.assertEquals( "c2", proposals.get( 1 ).getProposalName());
		Assert.assertEquals( "c2", proposals.get( 1 ).getProposalString());
		Assert.assertNull( proposals.get( 1 ).getProposalDescription());
		Assert.assertEquals( 1, proposals.get( 1 ).getReplacementOffset());
	}


	@Test
	public void testOffsetForComponentExtendsWithInvalidPrefix() throws Exception {

		// Expected: nothing
		Couple couple = prepare( "app2", "edited2.graph", 140 );
		couple.text += "\ncomp {\n\textends: no";

		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( 0, proposals.size());
	}


	@Test
	public void testOffsetForComponentFacetsWithoutPrefix() throws Exception {

		// Expected: f1
		Couple couple = prepare( "app2", "edited2.graph", 140 );
		couple.text += "\ncomp {\n\tfacets: ";

		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( 1, proposals.size());

		Assert.assertEquals( "f1", proposals.get( 0 ).getProposalName());
		Assert.assertEquals( "f1", proposals.get( 0 ).getProposalString());
		Assert.assertNull( proposals.get( 0 ).getProposalDescription());
		Assert.assertEquals( 0, proposals.get( 0 ).getReplacementOffset());
	}


	@Test
	public void testOffsetForComponentFacetsWithValidPrefix() throws Exception {

		// Expected: c1, c2
		Couple couple = prepare( "app2", "edited2.graph", 140 );
		couple.text += "\ncomp {\n\tfacets: F";

		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( 1, proposals.size());

		Assert.assertEquals( "f1", proposals.get( 0 ).getProposalName());
		Assert.assertEquals( "f1", proposals.get( 0 ).getProposalString());
		Assert.assertNull( proposals.get( 0 ).getProposalDescription());
		Assert.assertEquals( 1, proposals.get( 0 ).getReplacementOffset());
	}


	@Test
	public void testOffsetForComponentFacetsWithInvalidPrefix() throws Exception {

		// Expected: nothing
		Couple couple = prepare( "app2", "edited2.graph", 140 );
		couple.text += "\ncomp {\n\tfacets: no";

		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( 0, proposals.size());
	}


	@Test
	public void testOffsetForFacetFacetsWithoutPrefix() throws Exception {

		// Expected: f1
		Couple couple = prepare( "app2", "edited2.graph", 140 );
		couple.text += "\nfacet myfacet {\n\tfacets: ";

		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( 0, proposals.size());
	}


	@Test
	public void testOffsetForFacetInstallerWithoutPrefix() throws Exception {

		// Expected: f1
		Couple couple = prepare( "app2", "edited2.graph", 140 );
		couple.text += "\nfacet myfacet {\n\tinstaller: ";

		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( 0, proposals.size());
	}


	@Test
	public void testOffsetForFacetVariablesImportsWithoutPrefix() throws Exception {

		// Expected: f1
		Couple couple = prepare( "app2", "edited2.graph", 140 );
		couple.text += "\nfacet myfacet {\n\timports: ";

		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( 0, proposals.size());
	}


	@Test
	public void testOffsetForComponentCommentsWithoutPrefix() throws Exception {

		// Expected: c1, c2
		Couple couple = prepare( "app3", "edited3.graph", 200 );
		couple.text += "\ncomp {\n\tchildren: ";

		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( 4, proposals.size());

		Assert.assertEquals( "c1", proposals.get( 0 ).getProposalName());
		Assert.assertEquals( "c1", proposals.get( 0 ).getProposalString());
		Assert.assertEquals( "A comment about c1", proposals.get( 0 ).getProposalDescription());
		Assert.assertEquals( 0, proposals.get( 0 ).getReplacementOffset());

		Assert.assertEquals( "c2", proposals.get( 1 ).getProposalName());
		Assert.assertEquals( "c2", proposals.get( 1 ).getProposalString());
		Assert.assertNull( proposals.get( 1 ).getProposalDescription());
		Assert.assertEquals( 0, proposals.get( 1 ).getReplacementOffset());

		Assert.assertEquals( "f1", proposals.get( 2 ).getProposalName());
		Assert.assertEquals( "f1", proposals.get( 2 ).getProposalString());
		Assert.assertEquals( "This is facet f1.\nAnd the desc spans over two lines.", proposals.get( 2 ).getProposalDescription());
		Assert.assertEquals( 0, proposals.get( 2 ).getReplacementOffset());

		Assert.assertEquals( "f2", proposals.get( 3 ).getProposalName());
		Assert.assertEquals( "f2", proposals.get( 3 ).getProposalString());
		Assert.assertEquals( "Simple comment.", proposals.get( 3 ).getProposalDescription());
		Assert.assertEquals( 0, proposals.get( 3 ).getReplacementOffset());
	}


	@Test
	public void testOffsetForGraphImportAllFiles() throws Exception {

		// Expected: 3 files to import
		Couple couple = prepare( "app3", "edited3.graph", 700 );
		couple.text += "\nimport ";

		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( 3, proposals.size());

		String[] expected = {
				"imports/imp1.graph",
				"imports/subimports/imp2.graph",
				"imports/subimports/imp3.graph"
		};

		for( int i=0; i<expected.length; i++ ) {
			Assert.assertEquals( expected[ i ], proposals.get( i ).getProposalName());
			Assert.assertEquals( expected[ i ], proposals.get( i ).getProposalString());
			Assert.assertNull( proposals.get( i ).getProposalDescription());
			Assert.assertEquals( 0, proposals.get( i ).getReplacementOffset());
		}
	}


	@Test
	public void testOffsetForGraphImportNoSpaceAfterKeyword() throws Exception {

		// Expected: "import"
		Couple couple = prepare( "app3", "edited3.graph", 700 );
		couple.text += "\nimport";

		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( 1, proposals.size());

		Assert.assertEquals( KEYWORD_IMPORT, proposals.get( 0 ).getProposalName());
		Assert.assertEquals( IMPORT_PREFIX, proposals.get( 0 ).getProposalString());
		Assert.assertNull( proposals.get( 0 ).getProposalDescription());
		Assert.assertEquals( KEYWORD_IMPORT.length(), proposals.get( 0 ).getReplacementOffset());
	}


	@Test
	public void testOffsetForGraphImportWithPrefix() throws Exception {

		// Expected: 2 files to import
		Couple couple = prepare( "app3", "edited3.graph", 700 );
		couple.text += "\nimport imports/s";

		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( 2, proposals.size());

		String[] expected = {
				"imports/subimports/imp2.graph",
				"imports/subimports/imp3.graph"
		};

		for( int i=0; i<expected.length; i++ ) {
			Assert.assertEquals( expected[ i ], proposals.get( i ).getProposalName());
			Assert.assertEquals( expected[ i ], proposals.get( i ).getProposalString());
			Assert.assertNull( proposals.get( i ).getProposalDescription());
			Assert.assertEquals( 0, proposals.get( i ).getReplacementOffset());
		}
	}


	@Test
	public void testOffsetForGraphImportAvoidDuplicates() throws Exception {

		// Expected: 2 files to import
		Couple couple = prepare( "app3", "edited3.graph", 700 );
		couple.text += "\nimport imports/subimports/imp2.graph\n\nimport ";

		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( 2, proposals.size());

		String[] expected = {
				"imports/imp1.graph",
				"imports/subimports/imp3.graph"
		};

		for( int i=0; i<expected.length; i++ ) {
			Assert.assertEquals( expected[ i ], proposals.get( i ).getProposalName());
			Assert.assertEquals( expected[ i ], proposals.get( i ).getProposalString());
			Assert.assertNull( proposals.get( i ).getProposalDescription());
			Assert.assertEquals( 0, proposals.get( i ).getReplacementOffset());
		}
	}


	@Test
	public void testOffsetForGraphImportAvoidNastyDuplicates() throws Exception {

		// Expected: 2 files to import
		Couple couple = prepare( "app3", "edited3.graph", 700 );
		couple.text += "\nimport imports/subimports/imp2.graph  ;   \n\nimport imp";

		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( 2, proposals.size());

		String[] expected = {
				"imports/imp1.graph",
				"imports/subimports/imp3.graph"
		};

		for( int i=0; i<expected.length; i++ ) {
			Assert.assertEquals( expected[ i ], proposals.get( i ).getProposalName());
			Assert.assertEquals( expected[ i ], proposals.get( i ).getProposalString());
			Assert.assertNull( proposals.get( i ).getProposalDescription());
			Assert.assertEquals( 0, proposals.get( i ).getReplacementOffset());
		}
	}


	@Test
	public void testOffsetForVariablesImportsWithoutPrefix() throws Exception {

		// Expected: all the exported variables
		Couple couple = prepare( "app2", "edited2.graph", 140 );
		couple.text += "\ncomp {\n\timports: ";

		Map<String,String> expected = new TreeMap<> ();
		expected.put( "c1.*", CompletionUtils.IMPORT_ALL_THE_VARIABLES );
		expected.put( "c1.v1", CompletionUtils.DEFAULT_VALUE + "version1" );
		expected.put( "c1.v2", CompletionUtils.DEFAULT_VALUE + "version2" );
		expected.put( "c1.ip", CompletionUtils.SET_BY_ROBOCONF );
		expected.put( "c1.port", CompletionUtils.DEFAULT_VALUE + "8100" );
		expected.put( "c2.*", CompletionUtils.IMPORT_ALL_THE_VARIABLES );
		expected.put( "c2.ip", CompletionUtils.SET_BY_ROBOCONF );
		expected.put( "f1.*", CompletionUtils.IMPORT_ALL_THE_VARIABLES );
		expected.put( "f1.v1", CompletionUtils.DEFAULT_VALUE + "version1" );
		expected.put( "f1.v2", CompletionUtils.DEFAULT_VALUE + "version2" );

		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( expected.size(), proposals.size());

		int index = 0;
		for( Map.Entry<String,String> entry : expected.entrySet()) {
			Assert.assertEquals( entry.getKey(), proposals.get( index ).getProposalName());
			Assert.assertEquals( entry.getKey(), proposals.get( index ).getProposalString());
			Assert.assertEquals( entry.getValue(), proposals.get( index ).getProposalDescription());
			Assert.assertEquals( 0, proposals.get( index ).getReplacementOffset());
			index ++;
		}
	}


	@Test
	public void testOffsetForVariablesImportsSkipNonExporting_BrokenGraph_1() throws Exception {

		// Expected: all the exported variables.
		// We should not set "comp1.*" (no export).
		Couple couple = prepare( "app5", "edited5.graph", 394 );

		Map<String,String> expected = new TreeMap<> ();
		expected.put( "c1.*", CompletionUtils.IMPORT_ALL_THE_VARIABLES );
		expected.put( "c1.v1", CompletionUtils.DEFAULT_VALUE + "version1" );
		expected.put( "c1.v2", CompletionUtils.DEFAULT_VALUE + "version2" );
		expected.put( "c1.ip", CompletionUtils.SET_BY_ROBOCONF );
		expected.put( "c1.port", CompletionUtils.DEFAULT_VALUE + "8100" );
		expected.put( "c2.*", CompletionUtils.IMPORT_ALL_THE_VARIABLES );
		expected.put( "c2.ip", CompletionUtils.SET_BY_ROBOCONF );
		expected.put( "f1.*", CompletionUtils.IMPORT_ALL_THE_VARIABLES );
		expected.put( "f1.v1", CompletionUtils.DEFAULT_VALUE + "version1" );
		expected.put( "f1.v2", CompletionUtils.DEFAULT_VALUE + "version2" );

		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( expected.size(), proposals.size());

		int index = 0;
		for( Map.Entry<String,String> entry : expected.entrySet()) {
			Assert.assertEquals( entry.getKey(), proposals.get( index ).getProposalName());
			Assert.assertEquals( entry.getKey(), proposals.get( index ).getProposalString());
			Assert.assertEquals( entry.getValue(), proposals.get( index ).getProposalDescription());
			Assert.assertEquals( 0, proposals.get( index ).getReplacementOffset());
			index ++;
		}
	}


	@Test
	public void testOffsetForVariablesImportsSkipNonExporting_BrokenGraph_2() throws Exception {

		// Expected: all the exported variables.
		// We should not set "comp1.*" (no export).
		Couple couple = prepare( "app6", "edited6.graph", 410 );

		Map<String,String> expected = new TreeMap<> ();
		expected.put( "c1.*", CompletionUtils.IMPORT_ALL_THE_VARIABLES );
		expected.put( "c1.v1", CompletionUtils.DEFAULT_VALUE + "version1" );
		expected.put( "c1.v2", CompletionUtils.DEFAULT_VALUE + "version2" );
		expected.put( "c1.ip", CompletionUtils.SET_BY_ROBOCONF );
		expected.put( "c1.port", CompletionUtils.DEFAULT_VALUE + "8100" );
		expected.put( "c2.*", CompletionUtils.IMPORT_ALL_THE_VARIABLES );
		expected.put( "c2.ip", CompletionUtils.SET_BY_ROBOCONF );
		expected.put( "comp2.*", CompletionUtils.IMPORT_ALL_THE_VARIABLES );
		expected.put( "comp2.toto", null );
		expected.put( "f1.*", CompletionUtils.IMPORT_ALL_THE_VARIABLES );
		expected.put( "f1.v1", CompletionUtils.DEFAULT_VALUE + "version1" );
		expected.put( "f1.v2", CompletionUtils.DEFAULT_VALUE + "version2" );

		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( expected.size(), proposals.size());

		int index = 0;
		for( Map.Entry<String,String> entry : expected.entrySet()) {
			Assert.assertEquals( entry.getKey(), proposals.get( index ).getProposalName());
			Assert.assertEquals( entry.getKey(), proposals.get( index ).getProposalString());
			Assert.assertEquals( entry.getValue(), proposals.get( index ).getProposalDescription());
			Assert.assertEquals( 0, proposals.get( index ).getReplacementOffset());
			index ++;
		}
	}


	@Test
	public void testOffsetForVariablesImportsWithPrefix_1() throws Exception {

		// Expected: all the exported variables
		Couple couple = prepare( "app2", "edited2.graph", 140 );
		couple.text += "\ncomp {\n\timports: c1";

		Map<String,String> expected = new TreeMap<> ();
		expected.put( "c1.*", CompletionUtils.IMPORT_ALL_THE_VARIABLES );
		expected.put( "c1.v1", CompletionUtils.DEFAULT_VALUE + "version1" );
		expected.put( "c1.v2", CompletionUtils.DEFAULT_VALUE + "version2" );
		expected.put( "c1.ip", CompletionUtils.SET_BY_ROBOCONF );
		expected.put( "c1.port", CompletionUtils.DEFAULT_VALUE + "8100" );

		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( expected.size(), proposals.size());

		int index = 0;
		for( Map.Entry<String,String> entry : expected.entrySet()) {
			Assert.assertEquals( entry.getKey(), proposals.get( index ).getProposalName());
			Assert.assertEquals( entry.getKey(), proposals.get( index ).getProposalString());
			Assert.assertEquals( entry.getValue(), proposals.get( index ).getProposalDescription());
			Assert.assertEquals( 2, proposals.get( index ).getReplacementOffset());
			index ++;
		}
	}


	@Test
	public void testOffsetForVariablesImportsWithPrefix_2() throws Exception {

		// Expected: all the exported variables
		Couple couple = prepare( "app2", "edited2.graph", 140 );
		couple.text += "\ncomp {\n\timports: c2.";

		Map<String,String> expected = new TreeMap<> ();
		expected.put( "c2.*", CompletionUtils.IMPORT_ALL_THE_VARIABLES );
		expected.put( "c2.ip", CompletionUtils.SET_BY_ROBOCONF );

		List<RoboconfCompletionProposal> proposals = couple.proposer.findProposals( couple.text );
		Assert.assertEquals( expected.size(), proposals.size());

		int index = 0;
		for( Map.Entry<String,String> entry : expected.entrySet()) {
			Assert.assertEquals( entry.getKey(), proposals.get( index ).getProposalName());
			Assert.assertEquals( entry.getKey(), proposals.get( index ).getProposalString());
			Assert.assertEquals( entry.getValue(), proposals.get( index ).getProposalDescription());
			Assert.assertEquals( 3, proposals.get( index ).getReplacementOffset());
			index ++;
		}
	}


	private static final String FORBIDDEN_PATTERN_FOR_INDENTATION = "(?s)(^|(.*\n))\t{2,}.*\\S*";


	@Test
	public void testForbiddenPatternForIndentation() {

		Assert.assertFalse( "\tsomething\t".matches( FORBIDDEN_PATTERN_FOR_INDENTATION ));
		Assert.assertFalse( "whatever \t\t\t".matches( FORBIDDEN_PATTERN_FOR_INDENTATION ));
		Assert.assertFalse( "whaT \t\t\t Ever".matches( FORBIDDEN_PATTERN_FOR_INDENTATION ));

		Assert.assertTrue( "\t\tsomething\t".matches( FORBIDDEN_PATTERN_FOR_INDENTATION ));
		Assert.assertTrue( "co {\n\t\tca {\n".matches( FORBIDDEN_PATTERN_FOR_INDENTATION ));
	}


	/**
	 * Verifies neutral proposals.
	 * <p>
	 * When the offset is at the beginning of the document or between two types,
	 * the same proposals should be made.
	 * </p>
	 *
	 * @param proposals a non-null list of proposals
	 */
	private void verifyNeutralOffset( List<RoboconfCompletionProposal> proposals ) {

		Assert.assertEquals( 4, proposals.size());

		Assert.assertEquals( KEYWORD_IMPORT, proposals.get( 0 ).getProposalName());
		Assert.assertEquals( IMPORT_PREFIX, proposals.get( 0 ).getProposalString());
		Assert.assertNull( proposals.get( 0 ).getProposalDescription());
		Assert.assertEquals( 0, proposals.get( 0 ).getReplacementOffset());

		Assert.assertEquals( KEYWORD_FACET, proposals.get( 1 ).getProposalName());
		Assert.assertEquals( FACET_PREFIX, proposals.get( 1 ).getProposalString());
		Assert.assertNull( proposals.get( 1 ).getProposalDescription());
		Assert.assertEquals( 0, proposals.get( 1 ).getReplacementOffset());

		Assert.assertEquals( FACET_BLOCK, proposals.get( 2 ).getProposalName());
		Assert.assertTrue( proposals.get( 2 ).getProposalString().startsWith( FACET_PREFIX ));
		Assert.assertTrue( proposals.get( 2 ).getProposalDescription().startsWith( FACET_BLOCK ));
		Assert.assertEquals( 0, proposals.get( 2 ).getReplacementOffset());

		Assert.assertEquals( COMPONENT_BLOCK, proposals.get( 3 ).getProposalName());
		Assert.assertTrue( proposals.get( 3 ).getProposalString().startsWith( "name {" ));
		Assert.assertTrue( proposals.get( 3 ).getProposalDescription().startsWith( COMPONENT_BLOCK ));
		Assert.assertEquals( 0, proposals.get( 3 ).getReplacementOffset());

		// Verify indentation: only one tab at the beginning of a line
		Assert.assertFalse( proposals.get( 3 ).getProposalString().matches( FORBIDDEN_PATTERN_FOR_INDENTATION ));
	}


	protected Couple prepare( String appName, String fileName, int offset )
	throws IOException, URISyntaxException {

		File appDir = TestUtils.findTestFile( "/completion/" + appName );
		File graphFile = new File( appDir, Constants.PROJECT_DIR_GRAPH + "/" + fileName );

		Couple result = new Couple();
		result.proposer = new GraphsCompletionProposer( appDir, graphFile );

		String cacheKey = appName + "/" + fileName;
		String fileContent = CACHE.get( cacheKey );
		if( fileContent == null ) {
			fileContent = Utils.readFileContent( graphFile );
			CACHE.put( cacheKey, fileContent );
		}

		result.text = fileContent;
		if( offset < fileContent.length())
			result.text = fileContent.substring( 0, offset );

		return result;
	}
}
