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

package net.roboconf.core.dsl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import net.roboconf.core.dsl.parsing.AbstractBlock;
import net.roboconf.core.dsl.parsing.BlockInstanceOf;
import net.roboconf.core.dsl.parsing.FileDefinition;
import net.roboconf.core.errors.ErrorCode;
import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.utils.Utils;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ParsingModelIoTest {

	private static final String PATH = "/configurations/valid";


	@Test
	public void testFileTypes() throws Exception {

		Map<String,Integer> fileNameToFileType = new LinkedHashMap<> ();
		fileNameToFileType.put( "commented-import-1.graph", FileDefinition.AGGREGATOR );
		fileNameToFileType.put( "only-import-1.graph", FileDefinition.AGGREGATOR );
		fileNameToFileType.put( "commented-import-3.graph", FileDefinition.AGGREGATOR );

		fileNameToFileType.put( "only-component-3.graph", FileDefinition.GRAPH );
		fileNameToFileType.put( "only-component-4.graph", FileDefinition.GRAPH );

		fileNameToFileType.put( "real-lamp-all-in-one.graph", FileDefinition.GRAPH );
		fileNameToFileType.put( "real-lamp-all-in-one-flex.graph", FileDefinition.GRAPH );
		fileNameToFileType.put( "real-lamp-components.graph", FileDefinition.GRAPH );
		fileNameToFileType.put( "commented-component-2.graph", FileDefinition.GRAPH );

		fileNameToFileType.put( "instance-single.instances", FileDefinition.INSTANCE );
		fileNameToFileType.put( "instance-multiple.instances", FileDefinition.INSTANCE );
		fileNameToFileType.put( "instance-imbricated-3.instances", FileDefinition.INSTANCE );

		for( Map.Entry<String,Integer> entry : fileNameToFileType.entrySet()) {
			File f = TestUtils.findTestFile( PATH + "/" + entry.getKey());
			FileDefinition rel = ParsingModelIo.readConfigurationFile( f, false );
			Assert.assertEquals( "Invalid file type for " + entry.getKey(), entry.getValue().intValue(), rel.getFileType());
			Assert.assertEquals( entry.getKey(), 0, rel.getParsingErrors().size());
		}
	}


	@Test
	public void testInvalidFileType() throws Exception {

		File f = TestUtils.findTestFile( "/configurations/invalid/mix.txt" );
		FileDefinition def = ParsingModelIo.readConfigurationFile( f, true );
		Assert.assertEquals( 1, def.getParsingErrors().size());
		Assert.assertEquals( ErrorCode.P_INVALID_FILE_TYPE, def.getParsingErrors().iterator().next().getErrorCode());
	}


	@Test
	public void testComplexInstances() throws Exception {

		File f = TestUtils.findTestFile( "/configurations/valid/complex-instances.instances" );
		FileDefinition def = ParsingModelIo.readConfigurationFile( f, true );
		Assert.assertEquals( 0, def.getParsingErrors().size());

		List<AbstractBlock> toProcess = new ArrayList<> ();
		toProcess.addAll( def.getBlocks());

		List<BlockInstanceOf> instances = new ArrayList<> ();
		while( ! toProcess.isEmpty()) {
			AbstractBlock currentBlock = toProcess.remove( 0 );

			if( currentBlock.getInstructionType() == AbstractBlock.INSTANCEOF ) {
				BlockInstanceOf blockInstanceOf = (BlockInstanceOf) currentBlock;
				instances.add( blockInstanceOf );
				toProcess.addAll( blockInstanceOf.getInnerBlocks());
			}
		}

		// Keep a list instead of a count, so that we can read the
		// list content at debug time.
		Assert.assertEquals( 8, instances.size());
	}


	@Test
	public void testLoadingAndWritingOfValidFile() throws Exception {

		List<File> validFiles = TestUtils.findTestFiles( PATH );
		for( File f : validFiles ) {
			if( ! "instance-with-space-after.instances".equals( f.getName())
					&& ! "component-with-complex-variables-values.graph".equals( f.getName())
					&& ! "app-template-descriptor.properties".equals( f.getName()))
				testLoadingAndWritingOfValidFile( f );
		}
	}


	@Test( expected = IOException.class )
	public void saveRelatrionFileRequiresTargetFile() throws Exception {

		FileDefinition def = new FileDefinition((File) null );
		ParsingModelIo.saveRelationsFile( def, true, "\n " );
	}


	/**
	 * @param f
	 */
	private static void testLoadingAndWritingOfValidFile( File f ) throws Exception {

		// Preserving comments
		FileDefinition rel = ParsingModelIo.readConfigurationFile( f, false );
		Assert.assertTrue(  f.getName() + ": parsing errors were found.", rel.getParsingErrors().isEmpty());

		String fileContent = Utils.readFileContent( f );
		fileContent = fileContent.replaceAll( "\r?\n", System.getProperty( "line.separator" ));

		String s = ParsingModelIo.writeConfigurationFile( rel, true, null );
		Assert.assertEquals( f.getName() + ": serialized model is different from the source.",  fileContent, s );

		// The same, but without writing comments
		s = ParsingModelIo.writeConfigurationFile( rel, false, null );
		Assert.assertFalse( f.getName() + ": serialized model should not contain a comment delimiter.",  s.contains( ParsingConstants.COMMENT_DELIMITER ));

		// Ignore comments at parsing time
		rel = ParsingModelIo.readConfigurationFile( f, true );
		Assert.assertTrue( f.getName() + ": parsing errors were found.", rel.getParsingErrors().isEmpty());

		s = ParsingModelIo.writeConfigurationFile( rel, true, null );
		Assert.assertFalse( f.getName() + ": serialized model should not contain a comment delimiter.",  s.contains( ParsingConstants.COMMENT_DELIMITER ));

		s = ParsingModelIo.writeConfigurationFile( rel, false, null );
		Assert.assertFalse( f.getName() + ": serialized model should not contain a comment delimiter.",  s.contains( ParsingConstants.COMMENT_DELIMITER ));
	}


	/**
	 * To use for debug.
	 * @param args
	 */
	public static void main( String[] args ) {
		try {
			File f = TestUtils.findTestFile( PATH + "/commented-import-2.graph" );
			testLoadingAndWritingOfValidFile( f  );

		} catch( Exception e ) {
			e.printStackTrace();
		}
	}
}
