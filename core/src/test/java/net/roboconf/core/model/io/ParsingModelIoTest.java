/**
 * Copyright 2014 Linagora, Université Joseph Fourier
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

package net.roboconf.core.model.io;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;
import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.parsing.Constants;
import net.roboconf.core.model.parsing.FileDefinition;

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ParsingModelIoTest {

	private static final String PATH = "/configurations/valid";


	@Test
	public void testFileTypes() {

		Map<String,Integer> fileNameToFileType = new LinkedHashMap<String,Integer> ();
		fileNameToFileType.put( "commented-import-1.graph", FileDefinition.AGGREGATOR );
		fileNameToFileType.put( "only-import-1.graph", FileDefinition.AGGREGATOR );
		fileNameToFileType.put( "commented-import-3.graph", FileDefinition.AGGREGATOR );

		fileNameToFileType.put( "only-component-3.graph", FileDefinition.GRAPH );
		fileNameToFileType.put( "real-lamp-all-in-one.graph", FileDefinition.GRAPH );
		fileNameToFileType.put( "real-lamp-components.graph", FileDefinition.GRAPH );
		fileNameToFileType.put( "commented-component-2.graph", FileDefinition.GRAPH );

		fileNameToFileType.put( "instance-single.instances", FileDefinition.INSTANCE );
		fileNameToFileType.put( "instance-multiple.instances", FileDefinition.INSTANCE );
		fileNameToFileType.put( "instance-imbricated-3.instances", FileDefinition.INSTANCE );

		for( Map.Entry<String,Integer> entry : fileNameToFileType.entrySet()) {
			try {
				File f = TestUtils.findTestFile( PATH + "/" + entry.getKey());
				FileDefinition rel = ParsingModelIo.readConfigurationFile( f, false );
				Assert.assertEquals( "Invalid file type for " + entry.getKey(), entry.getValue().intValue(), rel.getFileType());

			} catch( Exception e ) {
				e.printStackTrace();
				Assert.fail( "Failed to find " + entry.getKey());
			}
		}
	}


	@Test
	public void testLoadingAndWritingOfValidFile() {

		// Find the files
		List<File> validFiles;
		try {
			validFiles = TestUtils.findTestFiles( PATH );

		} catch( Exception e ) {
			e.printStackTrace();
			Assert.fail( "Failed to initialize the test." );
			return;
		}

		// For every of them, make some checks
		for( File f : validFiles )
			testLoadingAndWritingOfValidFile( f );
	}


	/**
	 * @param f
	 */
	private static void testLoadingAndWritingOfValidFile( File f ) {

		// Preserving comments
		FileDefinition rel = ParsingModelIo.readConfigurationFile( f, false );
		Assert.assertTrue(  f.getName() + ": parsing errors were found.", rel.getParsingErrors().isEmpty());

		String fileContent;
		try {
			fileContent = TestUtils.readFileContent( f );
			fileContent = fileContent.replaceAll( "\r?\n", System.getProperty( "line.separator" ));

		} catch( IOException e ) {
			Assert.fail( f.getName() + ": failed to read the file content." );
			return;
		}

		String s = ParsingModelIo.writeConfigurationFile( rel, true, null );
		Assert.assertEquals( f.getName() + ": serialized model is different from the source.",  fileContent, s );

		// The same, but without writing comments
		s = ParsingModelIo.writeConfigurationFile( rel, false, null );
		Assert.assertFalse( f.getName() + ": serialized model should not contain a comment delimiter.",  s.contains( Constants.COMMENT_DELIMITER ));

		// Ignore comments at parsing time
		rel = ParsingModelIo.readConfigurationFile( f, true );
		Assert.assertTrue( f.getName() + ": parsing errors were found.", rel.getParsingErrors().isEmpty());

		s = ParsingModelIo.writeConfigurationFile( rel, true, null );
		Assert.assertFalse( f.getName() + ": serialized model should not contain a comment delimiter.",  s.contains( Constants.COMMENT_DELIMITER ));

		s = ParsingModelIo.writeConfigurationFile( rel, false, null );
		Assert.assertFalse( f.getName() + ": serialized model should not contain a comment delimiter.",  s.contains( Constants.COMMENT_DELIMITER ));
	}


	/**
	 * To use for debug.
	 * @param args
	 */
	public static void main( String[] args ) {
		try {
			File f = TestUtils.findTestFile( PATH + "/commented-import-2.graph" );
			testLoadingAndWritingOfValidFile( f  );

		} catch( IOException e ) {
			e.printStackTrace();

		} catch( URISyntaxException e ) {
			e.printStackTrace();
		}
	}
}
