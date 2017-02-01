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

package net.roboconf.karaf.prepare;

import java.io.File;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import net.roboconf.core.utils.Utils;

/**
 * A stand-alone class used to generate the preferences file for the DM's distribution.
 * @author Vincent Zurczak - Linagora
 */
public class GeneratePreferencesFileTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();


	@Test
	public void testGeneratedFile_success() throws Exception {

		File targetFile = this.folder.newFile();
		Assert.assertEquals( 0, targetFile.length());
		new GeneratePreferencesFile().run( new String[] { targetFile.getAbsolutePath()});
		Assert.assertNotEquals( 0, targetFile.length());

		String content = Utils.readFileContent( targetFile );
		for( String line : content.split( "\n" )) {

			if( Utils.isEmptyOrWhitespaces( line ))
				continue;

			if( ! line.startsWith( "##" )
					&& ! line.startsWith( "# " )
					&& ! line.matches( "\\w[\\w.]+\\w = .*" ))
				throw new Exception( "Invalid line: " + line );
		}
	}


	@Test( expected = RuntimeException.class )
	public void testGeneratedFile_invalidNumberOfArguments() throws Exception {

		File targetFile = this.folder.newFile();
		Assert.assertEquals( 0, targetFile.length());
		new GeneratePreferencesFile().run( new String[] { "a", "b", targetFile.getAbsolutePath()});
	}


	@Test( expected = RuntimeException.class )
	public void testGeneratedFile_invalidArguments() throws Exception {

		File targetFile = this.folder.newFolder();
		new GeneratePreferencesFile().run( new String[] { targetFile.getAbsolutePath()});
	}
}
