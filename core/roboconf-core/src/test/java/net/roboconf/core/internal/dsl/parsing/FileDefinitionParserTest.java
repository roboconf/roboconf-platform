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

package net.roboconf.core.internal.dsl.parsing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import net.roboconf.core.dsl.ParsingModelIo;
import net.roboconf.core.dsl.converters.FromGraphDefinition;
import net.roboconf.core.dsl.parsing.AbstractBlock;
import net.roboconf.core.dsl.parsing.BlockBlank;
import net.roboconf.core.dsl.parsing.BlockComment;
import net.roboconf.core.dsl.parsing.BlockComponent;
import net.roboconf.core.dsl.parsing.BlockImport;
import net.roboconf.core.dsl.parsing.BlockInstanceOf;
import net.roboconf.core.dsl.parsing.BlockProperty;
import net.roboconf.core.dsl.parsing.FileDefinition;
import net.roboconf.core.errors.ErrorCode;
import net.roboconf.core.errors.ErrorCode.ErrorCategory;
import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.ParsingError;
import net.roboconf.core.model.beans.Component;
import net.roboconf.core.model.beans.Graphs;
import net.roboconf.core.model.beans.ImportedVariable;
import net.roboconf.core.model.helpers.ComponentHelpers;
import net.roboconf.core.utils.Utils;

/**
 * @author Vincent Zurczak - Linagora
 */
public class FileDefinitionParserTest {

	@Test
	public void testRecognizeComment() {

		Collection<AbstractBlock> blocks = new ArrayList<> ();
		String[] validLines = {
				"# ok",
				"#",
				"     # woo ",
				"  ###############"
		};

		// Without ignoring comments
		for( String line : validLines ) {
			blocks.clear();
			FileDefinitionParser parser = new FileDefinitionParser( null, false );
			int code = parser.recognizeComment( line, blocks );

			Assert.assertEquals( "Expected a YES code for '" + line + "'", FileDefinitionParser.P_CODE_YES, code );
			Assert.assertEquals( "No parsing error was expected for '" + line + "'", 0, parser.getFileRelations().getParsingErrors().size());
			Assert.assertEquals( "Expected ONE block to be registered for '" + line + "'", 1, blocks.size());
			Assert.assertEquals( "Expected ONE comment block for '" + line + "'",
					AbstractBlock.COMMENT,
					blocks.iterator().next().getInstructionType());
		}

		// Ignoring comments
		for( String line : validLines ) {
			blocks.clear();
			FileDefinitionParser parser = new FileDefinitionParser( null, true );
			int code = parser.recognizeComment( line, blocks );

			Assert.assertEquals( "Expected a YES code for '" + line + "'", FileDefinitionParser.P_CODE_YES, code );
			Assert.assertEquals( "No parsing error was expected for '" + line + "'", 0, parser.getFileRelations().getParsingErrors().size());
			Assert.assertEquals( "Expected ZERO block to be registered for '" + line + "'", 0, blocks.size());
		}

		// Testing invalid comments
		String[] invalidLines = {
				"",
				"   	",
				"something",
				"something # with a comment after"
		};

		for( String line : invalidLines ) {
			blocks.clear();
			FileDefinitionParser parser = new FileDefinitionParser( null, true );
			int code = parser.recognizeComment( line, blocks );

			Assert.assertEquals( "Expected a NO code for '" + line + "'", FileDefinitionParser.P_CODE_NO, code );
			Assert.assertEquals( "No parsing error was expected for '" + line + "'", 0, parser.getFileRelations().getParsingErrors().size());
			Assert.assertEquals( "Expected ZERO block to be registered for '" + line + "'", 0, blocks.size());
		}
	}


	@Test
	public void testRecognizeBlank() {
		Collection<AbstractBlock> blocks = new ArrayList<> ();

		String[] validLines = { "  ", "" };
		for( String line : validLines ) {
			blocks.clear();
			FileDefinitionParser parser = new FileDefinitionParser( null, false );
			int code = parser.recognizeBlankLine( line, blocks );

			Assert.assertEquals( "Expected a YES code for '" + line + "'", FileDefinitionParser.P_CODE_YES, code );
			Assert.assertEquals( "No parsing error was expected for '" + line + "'", 0, parser.getFileRelations().getParsingErrors().size());
			Assert.assertEquals( "Expected ONE block to be registered for '" + line + "'", 1, blocks.size());
			Assert.assertEquals( "Expected ONE blank block for '" + line + "'",
					AbstractBlock.BLANK,
					blocks.iterator().next().getInstructionType());
		}

		String[] invalidLines = { "#", "something", "  something with spaces before an after  " };
		for( String line : invalidLines ) {
			blocks.clear();
			FileDefinitionParser parser = new FileDefinitionParser( null, false );
			int code = parser.recognizeBlankLine( line, blocks );

			Assert.assertEquals( "Expected a NO code for '" + line + "'", FileDefinitionParser.P_CODE_NO, code );
			Assert.assertEquals( "No parsing error was expected for '" + line + "'", 0, parser.getFileRelations().getParsingErrors().size());
			Assert.assertEquals( "Expected ZERO block to be registered for '" + line + "'", 0, blocks.size());
		}
	}


	@Test
	public void testSplitFromInlineComment() {

		String[][] beforeToAfter = {
			{ "import facets.rcf;", "# import the facets" },
			{ "import facets.rcf;", "\t# import the facets" },
			{ "import facets.rcf;", "#" },
			{ "import facets.rcf;", "" },
			{ "import facets.rcf;     ", "" },
			{ "import facets.rcf;", " #### import the facets" },
			{ "", " #### import the facets" },
			{ "", " # " },
			{ "", "#" }
		};

		// Parsing comments
		for( String[] line : beforeToAfter ) {
			FileDefinitionParser parser = new FileDefinitionParser( null, false );
			String theLine = line[ 0 ] + line[ 1 ];
			String[] parts = parser.splitFromInlineComment( theLine );
			Assert.assertEquals( "Test failed for '" + theLine + "'", line[ 0 ], parts[ 0 ]);
			Assert.assertEquals( "Test failed for '" + theLine + "'", line[ 1 ], parts[ 1 ]);
		}

		// Ignoring comments
		for( String[] line : beforeToAfter ) {
			FileDefinitionParser parser = new FileDefinitionParser( null, true );
			String theLine = line[ 0 ] + line[ 1 ];
			String[] parts = parser.splitFromInlineComment( theLine );
			Assert.assertEquals( "Test failed for '" + theLine + "'", line[ 0 ], parts[ 0 ]);
			Assert.assertEquals( "Comment should be empty for '" + theLine + "'", "", parts[ 1 ]);
		}
	}


	@Test
	public void testRecognizeImport() {

		String[] validLines = {
				"import facets.rcf;",
				"IMPORT facets.rcf;",
				"import http://some-host.com/some/path/facets.rcf;",
				"import http://some-host.com/some/path/?file=facets.rcf;",
				"   import   facets.rcf ;  ",
				"import facets.rcf; # with an inline comment"
		};

		// Comments do not matter here
		for( String line : validLines ) {
			FileDefinitionParser parser = new FileDefinitionParser( null, false );
			int code = parser.recognizeImport( line );
			FileDefinition def = parser.getFileRelations();

			Assert.assertEquals( "Expected a YES code for '" + line + "'", FileDefinitionParser.P_CODE_YES, code );
			Assert.assertEquals( "No parsing error was expected for '" + line + "'", 0, def.getParsingErrors().size());
			Assert.assertEquals( "Expected ONE block to be registered for '" + line + "'", 1, def.getBlocks().size());

			BlockImport imp = (BlockImport) def.getBlocks().iterator().next();
			Assert.assertEquals( "Expected ONE import block for '" + line + "'", AbstractBlock.IMPORT, imp.getInstructionType());

			Assert.assertFalse( "URI cannot be empty for '" + line + "'", Utils.isEmptyOrWhitespaces( imp.getUri()));
			Assert.assertEquals( "URI cannot have surrounding spaces '" + line + "'", imp.getUri(), imp.getUri().trim());
			Assert.assertNotSame( "URI cannot be the entire line for '" + line + "'", imp.getUri(), line );
			Assert.assertTrue( "The URI was not found in '" + line + "'", line.contains( imp.getUri()));
		}

		// In-line comments
		String[][] commentedLines = {
				{ "import facets.rcf;", "" },
				{ "import facets.rcf;", "# stuck comment" },
				{ "import facets.rcf;", "#" },
				{ "import facets.rcf;", "\t# some in-line comment   " }
		};

		for( String[] line : commentedLines ) {
			FileDefinitionParser parser = new FileDefinitionParser( null, false );
			String theLine = line[ 0 ] + line[ 1 ];
			int code = parser.recognizeImport( theLine );
			FileDefinition def  = parser.getFileRelations();

			Assert.assertEquals( "Expected a YES code for '" + theLine + "'", FileDefinitionParser.P_CODE_YES, code );
			Assert.assertEquals( "No parsing error was expected for '" + theLine + "'", 0, def.getParsingErrors().size());
			Assert.assertEquals( "Expected ONE block to be registered for '" + theLine + "'", 1, def.getBlocks().size());

			BlockImport imp = (BlockImport) def.getBlocks().iterator().next();
			Assert.assertEquals( "Expected ONE import block for '" + theLine + "'", AbstractBlock.IMPORT, imp.getInstructionType());

			String expected = Utils.isEmptyOrWhitespaces( line[ 1 ]) ? null : line[ 1 ];
			Assert.assertEquals( "Invalid in-line comment for '" + theLine + "'", expected, imp.getInlineComment());
		}

		// Invalid lines
		String[] invalidLines = {
				"imports facets.rcf;",
				"IPORT facets.rcf;",
				"facet",
				"",
				" #"
		};

		for( String line : invalidLines ) {
			FileDefinitionParser parser = new FileDefinitionParser( null, false );
			int code = parser.recognizeImport( line );
			Assert.assertEquals( "Expected a NO code for '" + line + "'", FileDefinitionParser.P_CODE_NO, code );
		}


		// Test error codes
		Map<String,ErrorCode> invalidLineToErrorCode = new LinkedHashMap<> ();
		invalidLineToErrorCode.put( "import facets.rcf", ErrorCode.P_IMPORT_ENDS_WITH_SEMI_COLON );
		invalidLineToErrorCode.put( "import facets.rcf ; facet toto {", ErrorCode.P_ONE_BLOCK_PER_LINE );
		invalidLineToErrorCode.put( "import facets.rcf ; invalid comment", ErrorCode.P_ONE_BLOCK_PER_LINE );
		invalidLineToErrorCode.put( "import facets.rcf ;;", ErrorCode.P_ONE_BLOCK_PER_LINE );

		for( Map.Entry<String,ErrorCode> entry : invalidLineToErrorCode.entrySet()) {
			String line = entry.getKey();
			FileDefinitionParser parser = new FileDefinitionParser( null, false );
			int code = parser.recognizeImport( entry.getKey());
			FileDefinition def = parser.getFileRelations();

			Assert.assertEquals( "Expected a YES code for '" + line + "'", FileDefinitionParser.P_CODE_YES, code );
			Assert.assertEquals( "A parsing error was expected for '" + line + "'", 1, def.getParsingErrors().size());
			Assert.assertEquals( "Expected ONE block to be registered for '" + line + "'", 1, def.getBlocks().size());
			Assert.assertEquals( "Expected ONE import block for '" + line + "'",
					AbstractBlock.IMPORT,
					def.getBlocks().iterator().next().getInstructionType());

			ParsingError error = def.getParsingErrors().iterator().next();
			Assert.assertEquals( "Expecting parsing error for '" + line + "'", entry.getValue().getCategory(), ErrorCategory.PARSING );
			Assert.assertEquals( "Invalid error code for '" + line + "'", entry.getValue(), error.getErrorCode());
		}
	}


	@Test
	public void testRecognizeProperty() {

		FileDefinition file = new FileDefinition( new File( "some-file" ));
		String[] validLines = {
			"property: value;",
			"   property : value  ;",
			"PROPERTY: value; # with an inline comment",
			"property: value1, value2  ,  value3;",
			"property:;",
			"property : key6 = \" key; 6 \";"
		};

		// Comments do not matter here
		for( String line : validLines ) {
			FileDefinitionParser parser = new FileDefinitionParser( null, false );
			BlockComponent holder = new BlockComponent( file );
			int code = parser.recognizeProperty( line, holder );

			Assert.assertEquals( "Expected a YES code for '" + line + "'", FileDefinitionParser.P_CODE_YES, code );
			Assert.assertEquals( "No parsing error was expected for '" + line + "'", 0, parser.getFileRelations().getParsingErrors().size());
			Assert.assertEquals( "Expected ONE block to be registered for '" + line + "'", 1, holder.getInnerBlocks().size());

			BlockProperty prop = (BlockProperty) holder.getInnerBlocks().iterator().next();
			Assert.assertEquals( "Expected ONE property for '" + line + "'", AbstractBlock.PROPERTY, prop.getInstructionType());
			Assert.assertEquals( "Invalid property name for '" + line + "'", "property", prop.getName().toLowerCase());
		}


		// Exports with quotes and semicolons in values
		validLines = new String[] {
			"exports : random[port] key6 = \" key; 6 \";",
			"exports : random[port] key6 = \" key; 6 \", key7=\"value7\";"
		};

		// Comments do not matter here
		for( String line : validLines ) {
			FileDefinitionParser parser = new FileDefinitionParser( null, false );
			BlockComponent holder = new BlockComponent( file );
			int code = parser.recognizeProperty( line, holder );

			Assert.assertEquals( "Expected a YES code for '" + line + "'", FileDefinitionParser.P_CODE_YES, code );
			Assert.assertEquals( "No parsing error was expected for '" + line + "'", 0, parser.getFileRelations().getParsingErrors().size());
			Assert.assertEquals( "Expected ONE block to be registered for '" + line + "'", 1, holder.getInnerBlocks().size());

			BlockProperty prop = (BlockProperty) holder.getInnerBlocks().iterator().next();
			Assert.assertEquals( "Expected ONE property for '" + line + "'", AbstractBlock.PROPERTY, prop.getInstructionType());
			Assert.assertEquals( "Invalid property name for '" + line + "'", "exports", prop.getName().toLowerCase());
		}


		// In-line comments
		String[][] commentedLines = {
			{ "property: value;", "" },
			{ "property: value;", "# stuck comment" },
			{ "property: value;", "#" },
			{ "property: value1, value2;", "\t# some in-line comment   " }
		};

		for( String[] line : commentedLines ) {
			FileDefinitionParser parser = new FileDefinitionParser( null, false );
			String theLine = line[ 0 ] + line[ 1 ];
			BlockComponent holder = new BlockComponent( file );
			int code = parser.recognizeProperty( theLine, holder );

			Assert.assertEquals( "Expected a YES code for '" + theLine + "'", FileDefinitionParser.P_CODE_YES, code );
			Assert.assertEquals( "No parsing error was expected for '" + theLine + "'", 0, parser.getFileRelations().getParsingErrors().size());
			Assert.assertEquals( "Expected ONE block to be registered for '" + theLine + "'", 1, holder.getInnerBlocks().size());

			BlockProperty prop = (BlockProperty) holder.getInnerBlocks().iterator().next();
			Assert.assertEquals( "Expected ONE property for '" + theLine + "'", AbstractBlock.PROPERTY, prop.getInstructionType());

			String expected = Utils.isEmptyOrWhitespaces( line[ 1 ]) ? null : line[ 1 ];
			Assert.assertEquals( "Invalid in-line comment for '" + theLine + "'", expected, prop.getInlineComment());
		}

		// Invalid lines
		String[] invalidLines = {
			"property = value;",
			"property",
			"facet",
			"",
			" #"
		};

		for( String line : invalidLines ) {
			FileDefinitionParser parser = new FileDefinitionParser( null, false );
			BlockComponent holder = new BlockComponent( file );
			int code = parser.recognizeProperty( line, holder );
			Assert.assertEquals( "Expected a NO code for '" + line + "'", FileDefinitionParser.P_CODE_NO, code );
		}


		// Test error codes
		Map<String,ErrorCode> invalidLineToErrorCode = new LinkedHashMap<> ();
		invalidLineToErrorCode.put( "property: value", ErrorCode.P_PROPERTY_ENDS_WITH_SEMI_COLON );
		invalidLineToErrorCode.put( "property:", ErrorCode.P_PROPERTY_ENDS_WITH_SEMI_COLON );
		invalidLineToErrorCode.put( "property: value;;", ErrorCode.P_ONE_BLOCK_PER_LINE );
		invalidLineToErrorCode.put( "exports : key6 = key; 6 \";", ErrorCode.P_ONE_BLOCK_PER_LINE );

		for( Map.Entry<String,ErrorCode> entry : invalidLineToErrorCode.entrySet()) {
			String line = entry.getKey();
			FileDefinitionParser parser = new FileDefinitionParser( null, false );
			BlockComponent holder = new BlockComponent( file );
			FileDefinition def = parser.getFileRelations();
			int code = parser.recognizeProperty( line, holder );

			Assert.assertEquals( "Expected a YES code for '" + line + "'", FileDefinitionParser.P_CODE_YES, code );
			Assert.assertEquals( "A parsing error was expected for '" + line + "'", 1, def.getParsingErrors().size());
			Assert.assertEquals( "Expected ONE block to be registered for '" + line + "'", 1, holder.getInnerBlocks().size());
			Assert.assertEquals( "Expected ONE property for '" + line + "'",
					AbstractBlock.PROPERTY,
					holder.getInnerBlocks().iterator().next().getInstructionType());

			ParsingError error = def.getParsingErrors().iterator().next();
			Assert.assertEquals( "Expecting parsing error for '" + line + "'", entry.getValue().getCategory(), ErrorCategory.PARSING );
			Assert.assertEquals( "Invalid error code for '" + line + "'", entry.getValue(), error.getErrorCode());
		}

		// Complex errors
		invalidLines = new String[] {
			"property: value ; facet toto {",
			"property: value ; invalid comment"
		};

		for( String line : invalidLines ) {
			FileDefinitionParser parser = new FileDefinitionParser( null, false );
			BlockComponent holder = new BlockComponent( file );
			FileDefinition def = parser.getFileRelations();
			int code = parser.recognizeProperty( line, holder );

			Assert.assertEquals( "Expected a YES code for '" + line + "'", FileDefinitionParser.P_CODE_YES, code );
			Assert.assertEquals( "Two parsing errors were expected for '" + line + "'", 2, def.getParsingErrors().size());
			Assert.assertEquals( "Expected ONE block to be registered for '" + line + "'", 1, holder.getInnerBlocks().size());
			Assert.assertEquals( "Expected ONE property for '" + line + "'",
					AbstractBlock.PROPERTY,
					holder.getInnerBlocks().iterator().next().getInstructionType());

			Iterator<ParsingError> iterator = def.getParsingErrors().iterator();
			ParsingError error = iterator.next();
			Assert.assertEquals( "Expecting parsing error for '" + line + "'", error.getErrorCode().getCategory(), ErrorCategory.PARSING );
			Assert.assertEquals( "Invalid error code for '" + line + "'", ErrorCode.P_PROPERTY_ENDS_WITH_SEMI_COLON, error.getErrorCode());

			error = iterator.next();
			Assert.assertEquals( "Expecting parsing error for '" + line + "'", error.getErrorCode().getCategory(), ErrorCategory.PARSING );
			Assert.assertEquals( "Invalid error code for '" + line + "'", ErrorCode.P_ONE_BLOCK_PER_LINE, error.getErrorCode());
		}
	}


	@Test
	public void testMergeContiguousRegions() {
		FileDefinition file = new FileDefinition( new File( "some-file" ));

		FileDefinitionParser parser = new FileDefinitionParser( null, false );
		List<AbstractBlock> blocks = new ArrayList<> ();
		blocks.add( new BlockImport( file ));
		blocks.add( new BlockImport( file ));
		parser.mergeContiguousRegions( blocks );
		Assert.assertEquals( 2, blocks.size());

		blocks.clear();
		blocks.add( new BlockProperty( file ));
		blocks.add( new BlockProperty( file ));
		parser.mergeContiguousRegions( blocks );
		Assert.assertEquals( 2, blocks.size());

		blocks.clear();
		blocks.add( new BlockComment( file, "# comment 1" ));
		blocks.add( new BlockComment( file, "# comment 2" ));
		parser.mergeContiguousRegions( blocks );
		Assert.assertEquals( 1, blocks.size());
		Assert.assertEquals( AbstractBlock.COMMENT, blocks.get( 0 ).getInstructionType());

		blocks.clear();
		blocks.add( new BlockComment( file, "# comment 1" ));
		blocks.add( new BlockBlank( file, "" ));
		blocks.add( new BlockComment( file, "# comment 2" ));
		parser.mergeContiguousRegions( blocks );
		Assert.assertEquals( 3, blocks.size());

		blocks.clear();
		blocks.add( new BlockComment( file, "# comment 1" ));
		blocks.add( new BlockBlank( file, "" ));
		blocks.add( new BlockComment( file, "# comment 2" ));
		blocks.add( new BlockComment( file, "# comment 3" ));
		parser.mergeContiguousRegions( blocks );
		Assert.assertEquals( 3, blocks.size());
		Assert.assertEquals( AbstractBlock.COMMENT, blocks.get( 0 ).getInstructionType());
		Assert.assertEquals( AbstractBlock.BLANK, blocks.get( 1 ).getInstructionType());
		Assert.assertEquals( AbstractBlock.COMMENT, blocks.get( 0 ).getInstructionType());

		blocks.clear();
		blocks.add( new BlockBlank( file, "" ));
		blocks.add( new BlockBlank( file, "" ));
		parser.mergeContiguousRegions( blocks );
		Assert.assertEquals( 1, blocks.size());
		Assert.assertEquals( AbstractBlock.BLANK, blocks.get( 0 ).getInstructionType());

		blocks.clear();
		blocks.add( new BlockBlank( file, "" ));
		blocks.add( new BlockComment( file, "# comment 1" ));
		blocks.add( new BlockBlank( file, "" ));
		parser.mergeContiguousRegions( blocks );
		Assert.assertEquals( 3, blocks.size());

		blocks.clear();
		blocks.add( new BlockBlank( file, "" ));
		blocks.add( new BlockBlank( file, "" ));
		blocks.add( new BlockComment( file, "# comment 1" ));
		blocks.add( new BlockBlank( file, "" ));
		parser.mergeContiguousRegions( blocks );
		Assert.assertEquals( 3, blocks.size());
		Assert.assertEquals( AbstractBlock.BLANK, blocks.get( 0 ).getInstructionType());
		Assert.assertEquals( AbstractBlock.COMMENT, blocks.get( 1 ).getInstructionType());
		Assert.assertEquals( AbstractBlock.BLANK, blocks.get( 0 ).getInstructionType());
	}


	@Test
	public void testRecognizeFacet() throws Exception {

		Map<String,ParsingError> resourceNameToErrorCode = new LinkedHashMap<> ();
		resourceNameToErrorCode.put( "facet-invalid-property.graph", new ParsingError( ErrorCode.P_INVALID_PROPERTY, null, 2 ));
		resourceNameToErrorCode.put( "facet-invalid-end.graph", new ParsingError( ErrorCode.P_C_C_BRACKET_EXTRA_CHARACTERS, null, 3 ));
		resourceNameToErrorCode.put( "facet-missing-closing-cb.graph", new ParsingError( ErrorCode.P_C_C_BRACKET_MISSING, null, 3 ));
		resourceNameToErrorCode.put( "facet-missing-opening-cb.graph", new ParsingError( ErrorCode.P_O_C_BRACKET_MISSING, null, 1 ));
		resourceNameToErrorCode.put( "facet-extra-char.graph", new ParsingError( ErrorCode.P_O_C_BRACKET_EXTRA_CHARACTERS, null, 1 ));
		resourceNameToErrorCode.put( "facet-line-number.graph", new ParsingError( ErrorCode.P_C_C_BRACKET_MISSING, null, 6 ));

		testRecognizePropertiesHolder( resourceNameToErrorCode, AbstractBlock.FACET );
	}


	@Test
	public void testRecognizeComponent() throws Exception {

		Map<String,ParsingError> resourceNameToErrorCode = new LinkedHashMap<> ();
		resourceNameToErrorCode.put( "component-invalid-property.graph", new ParsingError( ErrorCode.P_INVALID_PROPERTY, null, 2 ));
		resourceNameToErrorCode.put( "component-invalid-end.graph", new ParsingError( ErrorCode.P_C_C_BRACKET_EXTRA_CHARACTERS, null, 3 ));
		resourceNameToErrorCode.put( "component-missing-closing-cb.graph", new ParsingError( ErrorCode.P_C_C_BRACKET_MISSING, null, 3 ));
		resourceNameToErrorCode.put( "component-missing-opening-cb.graph", new ParsingError( ErrorCode.P_O_C_BRACKET_MISSING, null, 1 ));
		resourceNameToErrorCode.put( "component-extra-char.graph", new ParsingError( ErrorCode.P_O_C_BRACKET_EXTRA_CHARACTERS, null, 1 ));
		resourceNameToErrorCode.put( "component-line-number.graph", new ParsingError( ErrorCode.P_C_C_BRACKET_MISSING, null, 6 ));

		testRecognizePropertiesHolder( resourceNameToErrorCode, AbstractBlock.COMPONENT );
	}


	@Test
	public void testRecognizeInstanceOf() throws Exception {

		Map<String,ParsingError> resourceNameToErrorCode = new LinkedHashMap<> ();
		resourceNameToErrorCode.put( "instanceof-invalid-property.instances", new ParsingError( ErrorCode.P_INVALID_PROPERTY_OR_INSTANCE, null, 2 ));
		resourceNameToErrorCode.put( "instanceof-invalid-property-2.instances", new ParsingError( ErrorCode.P_INVALID_PROPERTY_OR_INSTANCE, null, 3 ));
		resourceNameToErrorCode.put( "instanceof-invalid-end.instances", new ParsingError( ErrorCode.P_C_C_BRACKET_EXTRA_CHARACTERS, null, 3 ));
		resourceNameToErrorCode.put( "instanceof-missing-closing-cb.instances", new ParsingError( ErrorCode.P_C_C_BRACKET_MISSING, null, 3 ));
		resourceNameToErrorCode.put( "instanceof-missing-opening-cb.instances", new ParsingError( ErrorCode.P_O_C_BRACKET_MISSING, null, 1 ));
		resourceNameToErrorCode.put( "instanceof-extra-char.instances", new ParsingError( ErrorCode.P_O_C_BRACKET_EXTRA_CHARACTERS, null, 1 ));
		resourceNameToErrorCode.put( "instanceof-line-number.instances", new ParsingError( ErrorCode.P_C_C_BRACKET_MISSING, null, 6 ));

		resourceNameToErrorCode.put( "instanceof-imbricated-invalid-property.instances", new ParsingError( ErrorCode.P_INVALID_PROPERTY_OR_INSTANCE, null, 4 ));
		resourceNameToErrorCode.put( "instanceof-imbricated-invalid-end.instances", new ParsingError( ErrorCode.P_C_C_BRACKET_EXTRA_CHARACTERS, null, 5 ));
		resourceNameToErrorCode.put( "instanceof-imbricated-missing-closing-cb.instances", new ParsingError( ErrorCode.P_C_C_BRACKET_MISSING, null, 7 ));
		resourceNameToErrorCode.put( "instanceof-imbricated-missing-opening-cb.instances", new ParsingError( ErrorCode.P_O_C_BRACKET_MISSING, null, 3 ));
		resourceNameToErrorCode.put( "instanceof-imbricated-extra-char.instances", new ParsingError( ErrorCode.P_O_C_BRACKET_EXTRA_CHARACTERS, null, 3 ));

		resourceNameToErrorCode.put( "instanceof-very-imbricated-invalid-property.instances", new ParsingError( ErrorCode.P_INVALID_PROPERTY_OR_INSTANCE, null, 5 ));
		resourceNameToErrorCode.put( "instanceof-very-imbricated-invalid-end.instances", new ParsingError( ErrorCode.P_C_C_BRACKET_EXTRA_CHARACTERS, null, 6 ));
		resourceNameToErrorCode.put( "instanceof-very-imbricated-missing-closing-cb.instances", new ParsingError( ErrorCode.P_C_C_BRACKET_MISSING, null, 8 ));
		resourceNameToErrorCode.put( "instanceof-very-imbricated-missing-opening-cb.instances", new ParsingError( ErrorCode.P_O_C_BRACKET_MISSING, null, 5 ));
		resourceNameToErrorCode.put( "instanceof-very-imbricated-extra-char.instances", new ParsingError( ErrorCode.P_O_C_BRACKET_EXTRA_CHARACTERS, null, 4 ));

		testRecognizePropertiesHolder( resourceNameToErrorCode, AbstractBlock.INSTANCEOF );
	}


	private void testRecognizePropertiesHolder( Map<String,ParsingError> resourceNameToErrorCode, int blockType )
	throws Exception {

		final String root = "/configurations/invalid/";
		for( Map.Entry<String,ParsingError> entry : resourceNameToErrorCode.entrySet()) {
			BufferedReader br = null;
			try {
				File f = TestUtils.findTestFile( root + entry.getKey());
				Assert.assertNotNull( f );
				Assert.assertTrue( f.getName(), f.exists());

				br = new BufferedReader( new InputStreamReader( new FileInputStream( f ), StandardCharsets.UTF_8 ));
				String line = br.readLine();
				Assert.assertNotNull( line );

				// This test skips the #read() method.
				// Therefore, the first line is considered as not being read.
				// We have to counter-balance this by updating the current line number.
				FileDefinitionParser parser = new FileDefinitionParser( f, false );
				parser.currentLineNumber = 1;

				if( blockType == AbstractBlock.FACET )
					parser.recognizeFacet( line, br );
				else if( blockType == AbstractBlock.COMPONENT )
					parser.recognizeComponent( line, br );
				else if( blockType == AbstractBlock.INSTANCEOF )
					parser.recognizeInstanceOf( line, br, null );
				else
					Assert.fail( "Invalid block type." );

				Assert.assertEquals( "ONE parsing error was expected for '" + entry.getKey() + "'", 1, parser.getFileRelations().getParsingErrors().size());
				ParsingError error = parser.getFileRelations().getParsingErrors().iterator().next();
				Assert.assertEquals( "Expected error code " + entry.getValue().getErrorCode() + " for '" + entry.getKey() + "'", entry.getValue().getErrorCode(), error.getErrorCode());
				Assert.assertEquals( "Expected error line to be " + entry.getValue().getLine() + " for '" + entry.getKey() + "'", entry.getValue().getLine(), error.getLine());
				Assert.assertEquals( "Expected a parsing error for '" + entry.getKey() + "'", ErrorCategory.PARSING, error.getErrorCode().getCategory());
				Assert.assertEquals( f, error.getFile());

			} finally {
				Utils.closeQuietly( br );
			}
		}
	}


	@Test
	public void testComplexInstancesParsing() {

		// Parse the file
		FileDefinition def;
		File f;
		try {
			f = TestUtils.findTestFile( "/configurations/valid/instance-imbricated-3.instances" );
			def = ParsingModelIo.readConfigurationFile( f, false );

		} catch( Exception e ) {
			Assert.fail( "Failed to find the file." );
			return;
		}

		// Make some checks to be sure it is parsed correctly
		Assert.assertEquals( FileDefinition.INSTANCE, def.getFileType());
		Assert.assertEquals( f, def.getEditedFile());
		Assert.assertEquals( 0, def.getParsingErrors().size());

		// Check blocks
		Assert.assertEquals( AbstractBlock.COMMENT, def.getBlocks().get( 0 ).getInstructionType());
		Assert.assertEquals( AbstractBlock.BLANK, def.getBlocks().get( 1 ).getInstructionType());
		Assert.assertEquals( AbstractBlock.INSTANCEOF, def.getBlocks().get( 2 ).getInstructionType());
		Assert.assertEquals( AbstractBlock.BLANK, def.getBlocks().get( 3 ).getInstructionType());
		Assert.assertEquals( AbstractBlock.INSTANCEOF, def.getBlocks().get( 4 ).getInstructionType());
		Assert.assertEquals( AbstractBlock.BLANK, def.getBlocks().get( 5 ).getInstructionType());
		Assert.assertEquals( 6, def.getBlocks().size());

		// Check instance 1
		BlockInstanceOf block = (BlockInstanceOf) def.getBlocks().get( 2 );
		Assert.assertEquals( "vm", block.getName());
		Assert.assertEquals( AbstractBlock.PROPERTY, block.getInnerBlocks().get( 0 ).getInstructionType());
		Assert.assertEquals( AbstractBlock.BLANK, block.getInnerBlocks().get( 1 ).getInstructionType());
		Assert.assertEquals( AbstractBlock.INSTANCEOF, block.getInnerBlocks().get( 2 ).getInstructionType());
		Assert.assertEquals( 3, block.getInnerBlocks().size());

		Assert.assertEquals( "name", ((BlockProperty) block.getInnerBlocks().get( 0 )).getName());
		Assert.assertEquals( "vm1", ((BlockProperty) block.getInnerBlocks().get( 0 )).getValue());
		Assert.assertFalse( Utils.isEmptyOrWhitespaces(((BlockProperty) block.getInnerBlocks().get( 0 )).getInlineComment()));

		block = (BlockInstanceOf) block.getInnerBlocks().get( 2 );
		Assert.assertEquals( "server", block.getName());
		Assert.assertEquals( AbstractBlock.PROPERTY, block.getInnerBlocks().get( 0 ).getInstructionType());
		Assert.assertEquals( AbstractBlock.PROPERTY, block.getInnerBlocks().get( 1 ).getInstructionType());
		Assert.assertEquals( AbstractBlock.BLANK, block.getInnerBlocks().get( 2 ).getInstructionType());
		Assert.assertEquals( AbstractBlock.INSTANCEOF, block.getInnerBlocks().get( 3 ).getInstructionType());
		Assert.assertEquals( 4, block.getInnerBlocks().size());

		Assert.assertEquals( "name", ((BlockProperty) block.getInnerBlocks().get( 0 )).getName());
		Assert.assertEquals( "server", ((BlockProperty) block.getInnerBlocks().get( 0 )).getValue());
		Assert.assertTrue( Utils.isEmptyOrWhitespaces(((BlockProperty) block.getInnerBlocks().get( 0 )).getInlineComment()));
		Assert.assertEquals( "port", ((BlockProperty) block.getInnerBlocks().get( 1 )).getName());
		Assert.assertEquals( "9878", ((BlockProperty) block.getInnerBlocks().get( 1 )).getValue());

		block = (BlockInstanceOf) block.getInnerBlocks().get( 3 );
		Assert.assertEquals( "web-app1", block.getName());
		Assert.assertEquals( AbstractBlock.PROPERTY, block.getInnerBlocks().get( 0 ).getInstructionType());
		Assert.assertEquals( 1, block.getInnerBlocks().size());

		Assert.assertEquals( "name", ((BlockProperty) block.getInnerBlocks().get( 0 )).getName());
		Assert.assertEquals( "web-app1-1", ((BlockProperty) block.getInnerBlocks().get( 0 )).getValue());
		Assert.assertTrue( Utils.isEmptyOrWhitespaces(((BlockProperty) block.getInnerBlocks().get( 0 )).getInlineComment()));

		// Check instance 2
		block = (BlockInstanceOf) def.getBlocks().get( 4 );
		Assert.assertEquals( "vm", block.getName());
		Assert.assertEquals( AbstractBlock.BLANK, block.getInnerBlocks().get( 0 ).getInstructionType());
		Assert.assertEquals( AbstractBlock.PROPERTY, block.getInnerBlocks().get( 1 ).getInstructionType());
		Assert.assertEquals( AbstractBlock.PROPERTY, block.getInnerBlocks().get( 2 ).getInstructionType());
		Assert.assertEquals( AbstractBlock.BLANK, block.getInnerBlocks().get( 3 ).getInstructionType());
		Assert.assertEquals( AbstractBlock.INSTANCEOF, block.getInnerBlocks().get( 4 ).getInstructionType());
		Assert.assertEquals( 5, block.getInnerBlocks().size());

		Assert.assertEquals( "name", ((BlockProperty) block.getInnerBlocks().get( 1 )).getName());
		Assert.assertEquals( "vm-", ((BlockProperty) block.getInnerBlocks().get( 1 )).getValue());
		Assert.assertFalse( Utils.isEmptyOrWhitespaces(((BlockProperty) block.getInnerBlocks().get( 1 )).getInlineComment()));

		Assert.assertEquals( "count", ((BlockProperty) block.getInnerBlocks().get( 2 )).getName());
		Assert.assertEquals( "7", ((BlockProperty) block.getInnerBlocks().get( 2 )).getValue());
		Assert.assertTrue( Utils.isEmptyOrWhitespaces(((BlockProperty) block.getInnerBlocks().get( 2 )).getInlineComment()));

		block = (BlockInstanceOf) block.getInnerBlocks().get( 4 );
		Assert.assertEquals( "server", block.getName());
		Assert.assertEquals( AbstractBlock.PROPERTY, block.getInnerBlocks().get( 0 ).getInstructionType());
		Assert.assertEquals( AbstractBlock.PROPERTY, block.getInnerBlocks().get( 1 ).getInstructionType());
		Assert.assertEquals( AbstractBlock.BLANK, block.getInnerBlocks().get( 2 ).getInstructionType());
		Assert.assertEquals( AbstractBlock.INSTANCEOF, block.getInnerBlocks().get( 3 ).getInstructionType());
		Assert.assertEquals( 4, block.getInnerBlocks().size());

		Assert.assertEquals( "name", ((BlockProperty) block.getInnerBlocks().get( 0 )).getName());
		Assert.assertEquals( "server4osgi", ((BlockProperty) block.getInnerBlocks().get( 0 )).getValue());
		Assert.assertTrue( Utils.isEmptyOrWhitespaces(((BlockProperty) block.getInnerBlocks().get( 0 )).getInlineComment()));
		Assert.assertEquals( "port", ((BlockProperty) block.getInnerBlocks().get( 1 )).getName());
		Assert.assertEquals( "9878", ((BlockProperty) block.getInnerBlocks().get( 1 )).getValue());

		block = (BlockInstanceOf) block.getInnerBlocks().get( 3 );
		Assert.assertEquals( "osgi-container", block.getName());
		Assert.assertEquals( AbstractBlock.PROPERTY, block.getInnerBlocks().get( 0 ).getInstructionType());
		Assert.assertEquals( AbstractBlock.BLANK, block.getInnerBlocks().get( 1 ).getInstructionType());
		Assert.assertEquals( AbstractBlock.INSTANCEOF, block.getInnerBlocks().get( 2 ).getInstructionType());
		Assert.assertEquals( 3, block.getInnerBlocks().size());

		Assert.assertEquals( "name", ((BlockProperty) block.getInnerBlocks().get( 0 )).getName());
		Assert.assertEquals( "osgi-container-app", ((BlockProperty) block.getInnerBlocks().get( 0 )).getValue());
		Assert.assertTrue( Utils.isEmptyOrWhitespaces(((BlockProperty) block.getInnerBlocks().get( 0 )).getInlineComment()));

		block = (BlockInstanceOf) block.getInnerBlocks().get( 2 );
		Assert.assertEquals( "bundle", block.getName());
		Assert.assertEquals( AbstractBlock.COMMENT, block.getInnerBlocks().get( 0 ).getInstructionType());
		Assert.assertEquals( AbstractBlock.PROPERTY, block.getInnerBlocks().get( 1 ).getInstructionType());
		Assert.assertEquals( 2, block.getInnerBlocks().size());

		Assert.assertEquals( "name", ((BlockProperty) block.getInnerBlocks().get( 1 )).getName());
		Assert.assertEquals( "my-bundle", ((BlockProperty) block.getInnerBlocks().get( 1 )).getValue());
		Assert.assertTrue( Utils.isEmptyOrWhitespaces(((BlockProperty) block.getInnerBlocks().get( 1 )).getInlineComment()));
	}


	@Test
	public void testParsingWithRuntimeInformation() {

		// Parse the file
		FileDefinition def;
		File f;
		try {
			f = TestUtils.findTestFile( "/configurations/valid/single-runtime-instance.instances" );
			def = ParsingModelIo.readConfigurationFile( f, false );

		} catch( Exception e ) {
			Assert.fail( "Failed to find the file." );
			return;
		}

		// Make some checks to be sure it is parsed correctly
		Assert.assertEquals( FileDefinition.INSTANCE, def.getFileType());
		Assert.assertEquals( f, def.getEditedFile());
		Assert.assertEquals( 0, def.getParsingErrors().size());
	}


	@Test
	public void testLoadGraph_withImportedVariables_andInheritance() throws Exception {

		File file = TestUtils.findTestFile( "/configurations/valid/extending-component-with-imported-variables.graph" );
		FromGraphDefinition fromDef = new FromGraphDefinition( file.getParentFile());
		Graphs graph = fromDef.buildGraphs( file );

		Assert.assertEquals( 0, fromDef.getErrors().size());
		Assert.assertEquals( 4, ComponentHelpers.findAllComponents( graph ).size());

		Component vmComponent = ComponentHelpers.findComponent( graph, "VM" );
		Component dbComponent = ComponentHelpers.findComponent( graph, "db" );
		Component serverComponent = ComponentHelpers.findComponent( graph, "server" );
		Component myServerComponent = ComponentHelpers.findComponent( graph, "my-server" );

		Collection<Component> children = ComponentHelpers.findAllChildren( vmComponent );
		Assert.assertEquals( 3, children.size());
		Assert.assertTrue( children.contains( dbComponent ));
		Assert.assertTrue( children.contains( serverComponent ));
		Assert.assertTrue( children.contains( myServerComponent ));

		Assert.assertNotNull( serverComponent.importedVariables.get( "db.ip" ));
		Assert.assertTrue( serverComponent.importedVariables.get( "db.ip" ).isOptional());
		Assert.assertNotNull( serverComponent.importedVariables.get( "db.port" ));
		Assert.assertTrue( serverComponent.importedVariables.get( "db.port" ).isOptional());

		Map<String,ImportedVariable> imports = ComponentHelpers.findAllImportedVariables( serverComponent );
		Assert.assertEquals( 2, imports.size());
		Assert.assertNotNull( imports.get( "db.ip" ));
		Assert.assertTrue( imports.get( "db.ip" ).isOptional());
		Assert.assertNotNull( imports.get( "db.port" ));
		Assert.assertTrue( imports.get( "db.port" ).isOptional());

		Assert.assertNotNull( myServerComponent.importedVariables.get( "db.ip" ));
		Assert.assertFalse( myServerComponent.importedVariables.get( "db.ip" ).isOptional());
		Assert.assertNull( myServerComponent.importedVariables.get( "db.port" ));

		imports = ComponentHelpers.findAllImportedVariables( myServerComponent );
		Assert.assertEquals( 2, imports.size());
		Assert.assertNotNull( imports.get( "db.ip" ));
		Assert.assertFalse( imports.get( "db.ip" ).isOptional());
		Assert.assertNotNull( imports.get( "db.port" ));
		Assert.assertTrue( imports.get( "db.port" ).isOptional());
	}


	@Test
	public void testNextLine() throws Exception {

		FileDefinitionParser parser = new FileDefinitionParser( new File( "whatever" ), true );
		Assert.assertEquals( 0, parser.currentLineNumber );

		StringBuilder sb = new StringBuilder();
		sb.append( "# this is a comment\n" );
		sb.append( "\n" );
		sb.append( "VM {\n" );
		sb.append( "\tinstaller: target; # !!!\n" );
		sb.append( "\n" );
		sb.append( "}\n" );
		sb.append( "\r\n" );
		sb.append( "\n" );

		BufferedReader br = new BufferedReader( new StringReader( sb.toString()));
		Assert.assertEquals( "# this is a comment", parser.nextLine( br ));
		Assert.assertEquals( 1, parser.currentLineNumber );

		Assert.assertEquals( "", parser.nextLine( br ));
		Assert.assertEquals( 2, parser.currentLineNumber );

		Assert.assertEquals( "VM {", parser.nextLine( br ));
		Assert.assertEquals( 3, parser.currentLineNumber );

		Assert.assertEquals( "\tinstaller: target; # !!!", parser.nextLine( br ));
		Assert.assertEquals( 4, parser.currentLineNumber );

		Assert.assertEquals( "", parser.nextLine( br ));
		Assert.assertEquals( 5, parser.currentLineNumber );

		Assert.assertEquals( "}", parser.nextLine( br ));
		Assert.assertEquals( 6, parser.currentLineNumber );

		Assert.assertEquals( "", parser.nextLine( br ));
		Assert.assertEquals( 7, parser.currentLineNumber );

		Assert.assertEquals( "", parser.nextLine( br ));
		Assert.assertEquals( 8, parser.currentLineNumber );
	}
}
