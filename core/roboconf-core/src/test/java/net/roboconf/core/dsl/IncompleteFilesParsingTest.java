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

package net.roboconf.core.dsl;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import net.roboconf.core.dsl.converters.FromGraphDefinition;
import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.beans.Graphs;

/**
 * @author Vincent Zurczak - Linagora
 */
public class IncompleteFilesParsingTest {

	@Test
	public void testIncompleteFilesResultInEmptyGraphByDefault() throws Exception {

		List<File> files = TestUtils.findTestFiles( "/configurations/flexibles" );
		Assert.assertNotSame( 0, files.size());

		List<String> almostValids = Arrays.asList( new String[] {
				"one-component-no-property.graph",
				"two-components-no-property.graph"
		});

		File directory = files.iterator().next().getParentFile();
		for( File f : files ) {

			// There are some exceptions
			if( almostValids.contains( f.getName()))
				continue;

			// Others contain syntactic errors and no component was found
			FromGraphDefinition fromDef = new FromGraphDefinition( directory );
			Graphs graph = fromDef.buildGraphs( f );

			Assert.assertNotSame( f.getName(), 0, fromDef.getErrors().size());
			Assert.assertEquals( f.getName(), 0, graph.getRootComponents().size());
			Assert.assertEquals( f.getName(), 0, graph.getFacetNameToFacet().size());
		}
	}
}
