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

package net.roboconf.core.internal.model.parsing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;
import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.internal.utils.Utils;
import net.roboconf.core.model.ErrorCode;
import net.roboconf.core.model.ErrorCode.ErrorCategory;
import net.roboconf.core.model.ModelError;
import net.roboconf.core.model.parsing.AbstractRegion;
import net.roboconf.core.model.parsing.FileDefinition;
import net.roboconf.core.model.parsing.RegionBlank;
import net.roboconf.core.model.parsing.RegionComment;
import net.roboconf.core.model.parsing.RegionComponent;
import net.roboconf.core.model.parsing.RegionImport;
import net.roboconf.core.model.parsing.RegionProperty;

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class FileDefinitionParserTest {

	@Test
	public void testRecognizeComment() {

		Collection<AbstractRegion> instr = new ArrayList<AbstractRegion> ();
		String[] validLines = {
				"# ok",
				"#",
				"     # woo ",
				"  ###############"
		};

		// Without ignoring comments
		for( String line : validLines ) {
			instr.clear();
			FileDefinitionParser parser = new FileDefinitionParser((URI) null, false );
			int code = parser.recognizeComment( line, instr );

			Assert.assertEquals( "Expected a YES code for '" + line + "'", FileDefinitionParser.P_CODE_YES, code );
			Assert.assertEquals( "No pasing error was expected for '" + line + "'", 0, parser.getFileRelations().getParsingErrors().size());
			Assert.assertEquals( "Expected ONE instruction to be registered for '" + line + "'", 1, instr.size());
			Assert.assertEquals( "Expected ONE comment instruction for '" + line + "'",
					AbstractRegion.COMMENT,
					instr.iterator().next().getInstructionType());
		}

		// Ignoring comments
		for( String line : validLines ) {
			instr.clear();
			FileDefinitionParser parser = new FileDefinitionParser((URI) null, true );
			int code = parser.recognizeComment( line, instr );

			Assert.assertEquals( "Expected a YES code for '" + line + "'", FileDefinitionParser.P_CODE_YES, code );
			Assert.assertEquals( "No pasing error was expected for '" + line + "'", 0, parser.getFileRelations().getParsingErrors().size());
			Assert.assertEquals( "Expected ZERO instruction to be registered for '" + line + "'", 0, instr.size());
		}

		// Testing invalid comments
		String[] invalidLines = {
				"",
				"   	",
				"something",
				"something # with a comment after"
		};

		for( String line : invalidLines ) {
			instr.clear();
			FileDefinitionParser parser = new FileDefinitionParser((URI) null, true );
			int code = parser.recognizeComment( line, instr );

			Assert.assertEquals( "Expected a NO code for '" + line + "'", FileDefinitionParser.P_CODE_NO, code );
			Assert.assertEquals( "No pasing error was expected for '" + line + "'", 0, parser.getFileRelations().getParsingErrors().size());
			Assert.assertEquals( "Expected ZERO instruction to be registered for '" + line + "'", 0, instr.size());
		}
	}


	@Test
	public void testRecognizeBlank() {
		Collection<AbstractRegion> instr = new ArrayList<AbstractRegion> ();

		String[] validLines = { "  ", "" };
		for( String line : validLines ) {
			instr.clear();
			FileDefinitionParser parser = new FileDefinitionParser((URI) null, false );
			int code = parser.recognizeBlankLine( line, instr );

			Assert.assertEquals( "Expected a YES code for '" + line + "'", FileDefinitionParser.P_CODE_YES, code );
			Assert.assertEquals( "No pasing error was expected for '" + line + "'", 0, parser.getFileRelations().getParsingErrors().size());
			Assert.assertEquals( "Expected ONE instruction to be registered for '" + line + "'", 1, instr.size());
			Assert.assertEquals( "Expected ONE blank instruction for '" + line + "'",
					AbstractRegion.BLANK,
					instr.iterator().next().getInstructionType());
		}

		String[] invalidLines = { "#", "something", "  something with spaces before an after  " };
		for( String line : invalidLines ) {
			instr.clear();
			FileDefinitionParser parser = new FileDefinitionParser((URI) null, false );
			int code = parser.recognizeBlankLine( line, instr );

			Assert.assertEquals( "Expected a NO code for '" + line + "'", FileDefinitionParser.P_CODE_NO, code );
			Assert.assertEquals( "No pasing error was expected for '" + line + "'", 0, parser.getFileRelations().getParsingErrors().size());
			Assert.assertEquals( "Expected ZERO instruction to be registered for '" + line + "'", 0, instr.size());
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
			FileDefinitionParser parser = new FileDefinitionParser((URI) null, false );
			String theLine = line[ 0 ] + line[ 1 ];
			String[] parts = parser.splitFromInlineComment( theLine );
			Assert.assertEquals( "Test failed for '" + theLine + "'", line[ 0 ], parts[ 0 ]);
			Assert.assertEquals( "Test failed for '" + theLine + "'", line[ 1 ], parts[ 1 ]);
		}

		// Ignoring comments
		for( String[] line : beforeToAfter ) {
			FileDefinitionParser parser = new FileDefinitionParser((URI) null, true );
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
			FileDefinitionParser parser = new FileDefinitionParser((URI) null, false );
			int code = parser.recognizeImport( line );
			FileDefinition def = parser.getFileRelations();

			Assert.assertEquals( "Expected a YES code for '" + line + "'", FileDefinitionParser.P_CODE_YES, code );
			Assert.assertEquals( "No pasing error was expected for '" + line + "'", 0, def.getParsingErrors().size());
			Assert.assertEquals( "Expected ONE instruction to be registered for '" + line + "'", 1, def.getInstructions().size());

			RegionImport imp = (RegionImport) def.getInstructions().iterator().next();
			Assert.assertEquals( "Expected ONE import instruction for '" + line + "'", AbstractRegion.IMPORT, imp.getInstructionType());

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
			FileDefinitionParser parser = new FileDefinitionParser((URI) null, false );
			String theLine = line[ 0 ] + line[ 1 ];
			int code = parser.recognizeImport( theLine );
			FileDefinition def  = parser.getFileRelations();

			Assert.assertEquals( "Expected a YES code for '" + theLine + "'", FileDefinitionParser.P_CODE_YES, code );
			Assert.assertEquals( "No pasing error was expected for '" + theLine + "'", 0, def.getParsingErrors().size());
			Assert.assertEquals( "Expected ONE instruction to be registered for '" + theLine + "'", 1, def.getInstructions().size());

			RegionImport imp = (RegionImport) def.getInstructions().iterator().next();
			Assert.assertEquals( "Expected ONE import instruction for '" + theLine + "'", AbstractRegion.IMPORT, imp.getInstructionType());

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
			FileDefinitionParser parser = new FileDefinitionParser((URI) null, false );
			int code = parser.recognizeImport( line );
			Assert.assertEquals( "Expected a NO code for '" + line + "'", FileDefinitionParser.P_CODE_NO, code );
		}


		// Test error codes
		Map<String,ErrorCode> invalidLineToErrorCode = new LinkedHashMap<String,ErrorCode> ();
		invalidLineToErrorCode.put( "import facets.rcf", ErrorCode.P_IMPORT_ENDS_WITH_SEMI_COLON );
		invalidLineToErrorCode.put( "import facets.rcf ; facet toto {", ErrorCode.P_ONE_INSTRUCTION_PER_LINE );
		invalidLineToErrorCode.put( "import facets.rcf ; invalid comment", ErrorCode.P_ONE_INSTRUCTION_PER_LINE );
		invalidLineToErrorCode.put( "import facets.rcf ;;", ErrorCode.P_ONE_INSTRUCTION_PER_LINE );

		for( Map.Entry<String,ErrorCode> entry : invalidLineToErrorCode.entrySet()) {
			String line = entry.getKey();
			FileDefinitionParser parser = new FileDefinitionParser((URI) null, false );
			int code = parser.recognizeImport( entry.getKey());
			FileDefinition def = parser.getFileRelations();

			Assert.assertEquals( "Expected a YES code for '" + line + "'", FileDefinitionParser.P_CODE_YES, code );
			Assert.assertEquals( "A pasing error was expected for '" + line + "'", 1, def.getParsingErrors().size());
			Assert.assertEquals( "Expected ONE instruction to be registered for '" + line + "'", 1, def.getInstructions().size());
			Assert.assertEquals( "Expected ONE import instruction for '" + line + "'",
					AbstractRegion.IMPORT,
					def.getInstructions().iterator().next().getInstructionType());

			ModelError error = def.getParsingErrors().iterator().next();
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
				"property:;"
		};

		// Comments do not matter here
		for( String line : validLines ) {
			FileDefinitionParser parser = new FileDefinitionParser((URI) null, false );
			RegionComponent holder = new RegionComponent( file );
			int code = parser.recognizeProperty( line, holder );

			Assert.assertEquals( "Expected a YES code for '" + line + "'", FileDefinitionParser.P_CODE_YES, code );
			Assert.assertEquals( "No pasing error was expected for '" + line + "'", 0, parser.getFileRelations().getParsingErrors().size());
			Assert.assertEquals( "Expected ONE instruction to be registered for '" + line + "'", 1, holder.getInternalInstructions().size());

			RegionProperty prop = (RegionProperty) holder.getInternalInstructions().iterator().next();
			Assert.assertEquals( "Expected ONE property for '" + line + "'", AbstractRegion.PROPERTY, prop.getInstructionType());
			Assert.assertEquals( "Invalid property name for '" + line + "'", "property", prop.getName().toLowerCase());
		}

		// In-line comments
		String[][] commentedLines = {
				{ "property: value;", "" },
				{ "property: value;", "# stuck comment" },
				{ "property: value;", "#" },
				{ "property: value1, value2;", "\t# some in-line comment   " }
		};

		for( String[] line : commentedLines ) {
			FileDefinitionParser parser = new FileDefinitionParser((URI) null, false );
			String theLine = line[ 0 ] + line[ 1 ];
			RegionComponent holder = new RegionComponent( file );
			int code = parser.recognizeProperty( theLine, holder );

			Assert.assertEquals( "Expected a YES code for '" + theLine + "'", FileDefinitionParser.P_CODE_YES, code );
			Assert.assertEquals( "No pasing error was expected for '" + theLine + "'", 0, parser.getFileRelations().getParsingErrors().size());
			Assert.assertEquals( "Expected ONE instruction to be registered for '" + theLine + "'", 1, holder.getInternalInstructions().size());

			RegionProperty prop = (RegionProperty) holder.getInternalInstructions().iterator().next();
			Assert.assertEquals( "Expected ONE property for '" + theLine + "'", AbstractRegion.PROPERTY, prop.getInstructionType());

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
			FileDefinitionParser parser = new FileDefinitionParser((URI) null, false );
			RegionComponent holder = new RegionComponent( file );
			int code = parser.recognizeProperty( line, holder );
			Assert.assertEquals( "Expected a NO code for '" + line + "'", FileDefinitionParser.P_CODE_NO, code );
		}


		// Test error codes
		Map<String,ErrorCode> invalidLineToErrorCode = new LinkedHashMap<String,ErrorCode> ();
		invalidLineToErrorCode.put( "property: value", ErrorCode.P_PROPERTY_ENDS_WITH_SEMI_COLON );
		invalidLineToErrorCode.put( "property:", ErrorCode.P_PROPERTY_ENDS_WITH_SEMI_COLON );
		invalidLineToErrorCode.put( "property: value ; facet toto {", ErrorCode.P_ONE_INSTRUCTION_PER_LINE );
		invalidLineToErrorCode.put( "property: value ; invalid comment", ErrorCode.P_ONE_INSTRUCTION_PER_LINE );
		invalidLineToErrorCode.put( "property: value;;", ErrorCode.P_ONE_INSTRUCTION_PER_LINE );

		for( Map.Entry<String,ErrorCode> entry : invalidLineToErrorCode.entrySet()) {
			String line = entry.getKey();
			FileDefinitionParser parser = new FileDefinitionParser((URI) null, false );
			RegionComponent holder = new RegionComponent( file );
			FileDefinition def = parser.getFileRelations();
			int code = parser.recognizeProperty( line, holder );

			Assert.assertEquals( "Expected a YES code for '" + line + "'", FileDefinitionParser.P_CODE_YES, code );
			Assert.assertEquals( "A pasing error was expected for '" + line + "'", 1, def.getParsingErrors().size());
			Assert.assertEquals( "Expected ONE instruction to be registered for '" + line + "'", 1, holder.getInternalInstructions().size());
			Assert.assertEquals( "Expected ONE property for '" + line + "'",
					AbstractRegion.PROPERTY,
					holder.getInternalInstructions().iterator().next().getInstructionType());

			ModelError error = def.getParsingErrors().iterator().next();
			Assert.assertEquals( "Expecting parsing error for '" + line + "'", entry.getValue().getCategory(), ErrorCategory.PARSING );
			Assert.assertEquals( "Invalid error code for '" + line + "'", entry.getValue(), error.getErrorCode());
		}
	}


	@Test
	public void testMergeContiguousRegions() {
		FileDefinition file = new FileDefinition( new File( "some-file" ));

		FileDefinitionParser parser = new FileDefinitionParser((URI) null, false );
		List<AbstractRegion> instructions = new ArrayList<AbstractRegion> ();
		instructions.add( new RegionImport( file ));
		instructions.add( new RegionImport( file ));
		parser.mergeContiguousRegions( instructions );
		Assert.assertEquals( 2, instructions.size());

		instructions.clear();
		instructions.add( new RegionProperty( file ));
		instructions.add( new RegionProperty( file ));
		parser.mergeContiguousRegions( instructions );
		Assert.assertEquals( 2, instructions.size());

		instructions.clear();
		instructions.add( new RegionComment( file, "# comment 1" ));
		instructions.add( new RegionComment( file, "# comment 2" ));
		parser.mergeContiguousRegions( instructions );
		Assert.assertEquals( 1, instructions.size());
		Assert.assertEquals( AbstractRegion.COMMENT, instructions.get( 0 ).getInstructionType());

		instructions.clear();
		instructions.add( new RegionComment( file, "# comment 1" ));
		instructions.add( new RegionBlank( file, "" ));
		instructions.add( new RegionComment( file, "# comment 2" ));
		parser.mergeContiguousRegions( instructions );
		Assert.assertEquals( 3, instructions.size());

		instructions.clear();
		instructions.add( new RegionComment( file, "# comment 1" ));
		instructions.add( new RegionBlank( file, "" ));
		instructions.add( new RegionComment( file, "# comment 2" ));
		instructions.add( new RegionComment( file, "# comment 3" ));
		parser.mergeContiguousRegions( instructions );
		Assert.assertEquals( 3, instructions.size());
		Assert.assertEquals( AbstractRegion.COMMENT, instructions.get( 0 ).getInstructionType());
		Assert.assertEquals( AbstractRegion.BLANK, instructions.get( 1 ).getInstructionType());
		Assert.assertEquals( AbstractRegion.COMMENT, instructions.get( 0 ).getInstructionType());

		instructions.clear();
		instructions.add( new RegionBlank( file, "" ));
		instructions.add( new RegionBlank( file, "" ));
		parser.mergeContiguousRegions( instructions );
		Assert.assertEquals( 1, instructions.size());
		Assert.assertEquals( AbstractRegion.BLANK, instructions.get( 0 ).getInstructionType());

		instructions.clear();
		instructions.add( new RegionBlank( file, "" ));
		instructions.add( new RegionComment( file, "# comment 1" ));
		instructions.add( new RegionBlank( file, "" ));
		parser.mergeContiguousRegions( instructions );
		Assert.assertEquals( 3, instructions.size());

		instructions.clear();
		instructions.add( new RegionBlank( file, "" ));
		instructions.add( new RegionBlank( file, "" ));
		instructions.add( new RegionComment( file, "# comment 1" ));
		instructions.add( new RegionBlank( file, "" ));
		parser.mergeContiguousRegions( instructions );
		Assert.assertEquals( 3, instructions.size());
		Assert.assertEquals( AbstractRegion.BLANK, instructions.get( 0 ).getInstructionType());
		Assert.assertEquals( AbstractRegion.COMMENT, instructions.get( 1 ).getInstructionType());
		Assert.assertEquals( AbstractRegion.BLANK, instructions.get( 0 ).getInstructionType());
	}


	@Test
	public void testRecognizeFacet() {

		Map<String,ModelError> resourceNameToErrorCode = new LinkedHashMap<String,ModelError> ();
		resourceNameToErrorCode.put( "facet-invalid-property.graph", new ModelError( ErrorCode.P_INVALID_PROPERTY, 2 ));
		resourceNameToErrorCode.put( "facet-invalid-end.graph", new ModelError( ErrorCode.P_C_C_BRACKET_EXTRA_CHARACTERS, 3 ));
		resourceNameToErrorCode.put( "facet-missing-closing-cb.graph", new ModelError( ErrorCode.P_C_C_BRACKET_MISSING, 3 ));
		resourceNameToErrorCode.put( "facet-missing-opening-cb.graph", new ModelError( ErrorCode.P_O_C_BRACKET_MISSING, 1 ));
		resourceNameToErrorCode.put( "facet-extra-char.graph", new ModelError( ErrorCode.P_O_C_BRACKET_EXTRA_CHARACTERS, 1 ));
		resourceNameToErrorCode.put( "facet-line-number.graph", new ModelError( ErrorCode.P_C_C_BRACKET_MISSING, 6 ));

		testRecognizePropertiesHolder( resourceNameToErrorCode, AbstractRegion.FACET );
	}


	@Test
	public void testRecognizeComponent() {

		Map<String,ModelError> resourceNameToErrorCode = new LinkedHashMap<String,ModelError> ();
		resourceNameToErrorCode.put( "component-invalid-property.graph", new ModelError( ErrorCode.P_INVALID_PROPERTY, 2 ));
		resourceNameToErrorCode.put( "component-invalid-end.graph", new ModelError( ErrorCode.P_C_C_BRACKET_EXTRA_CHARACTERS, 3 ));
		resourceNameToErrorCode.put( "component-missing-closing-cb.graph", new ModelError( ErrorCode.P_C_C_BRACKET_MISSING, 3 ));
		resourceNameToErrorCode.put( "component-missing-opening-cb.graph", new ModelError( ErrorCode.P_O_C_BRACKET_MISSING, 1 ));
		resourceNameToErrorCode.put( "component-extra-char.graph", new ModelError( ErrorCode.P_O_C_BRACKET_EXTRA_CHARACTERS, 1 ));
		resourceNameToErrorCode.put( "component-line-number.graph", new ModelError( ErrorCode.P_C_C_BRACKET_MISSING, 6 ));

		testRecognizePropertiesHolder( resourceNameToErrorCode, AbstractRegion.COMPONENT );
	}


	@Test
	public void testRecognizeInstanceOf() {

		Map<String,ModelError> resourceNameToErrorCode = new LinkedHashMap<String,ModelError> ();
		resourceNameToErrorCode.put( "instanceof-invalid-property.instances", new ModelError( ErrorCode.P_INVALID_PROPERTY_OR_INSTANCE, 2 ));
		resourceNameToErrorCode.put( "instanceof-invalid-end.instances", new ModelError( ErrorCode.P_C_C_BRACKET_EXTRA_CHARACTERS, 3 ));
		resourceNameToErrorCode.put( "instanceof-missing-closing-cb.instances", new ModelError( ErrorCode.P_C_C_BRACKET_MISSING, 3 ));
		resourceNameToErrorCode.put( "instanceof-missing-opening-cb.instances", new ModelError( ErrorCode.P_O_C_BRACKET_MISSING, 1 ));
		resourceNameToErrorCode.put( "instanceof-extra-char.instances", new ModelError( ErrorCode.P_O_C_BRACKET_EXTRA_CHARACTERS, 1 ));
		resourceNameToErrorCode.put( "instanceof-line-number.instances", new ModelError( ErrorCode.P_C_C_BRACKET_MISSING, 6 ));

		resourceNameToErrorCode.put( "instanceof-imbricated-invalid-property.instances", new ModelError( ErrorCode.P_INVALID_PROPERTY_OR_INSTANCE, 4 ));
		resourceNameToErrorCode.put( "instanceof-imbricated-invalid-end.instances", new ModelError( ErrorCode.P_C_C_BRACKET_EXTRA_CHARACTERS, 5 ));
		resourceNameToErrorCode.put( "instanceof-imbricated-missing-closing-cb.instances", new ModelError( ErrorCode.P_C_C_BRACKET_MISSING, 7 ));
		resourceNameToErrorCode.put( "instanceof-imbricated-missing-opening-cb.instances", new ModelError( ErrorCode.P_O_C_BRACKET_MISSING, 3 ));
		resourceNameToErrorCode.put( "instanceof-imbricated-extra-char.instances", new ModelError( ErrorCode.P_O_C_BRACKET_EXTRA_CHARACTERS, 3 ));

		resourceNameToErrorCode.put( "instanceof-very-imbricated-invalid-property.instances", new ModelError( ErrorCode.P_INVALID_PROPERTY_OR_INSTANCE, 5 ));
		resourceNameToErrorCode.put( "instanceof-very-imbricated-invalid-end.instances", new ModelError( ErrorCode.P_C_C_BRACKET_EXTRA_CHARACTERS, 6 ));
		resourceNameToErrorCode.put( "instanceof-very-imbricated-missing-closing-cb.instances", new ModelError( ErrorCode.P_C_C_BRACKET_MISSING, 8 ));
		resourceNameToErrorCode.put( "instanceof-very-imbricated-missing-opening-cb.instances", new ModelError( ErrorCode.P_O_C_BRACKET_MISSING, 5 ));
		resourceNameToErrorCode.put( "instanceof-very-imbricated-extra-char.instances", new ModelError( ErrorCode.P_O_C_BRACKET_EXTRA_CHARACTERS, 4 ));

		testRecognizePropertiesHolder( resourceNameToErrorCode, AbstractRegion.INSTANCEOF );
	}


	private void testRecognizePropertiesHolder( Map<String,ModelError> resourceNameToErrorCode, int blockType ) {

		final String root = "/configurations/invalid/";
		for( Map.Entry<String,ModelError> entry : resourceNameToErrorCode.entrySet()) {
			BufferedReader br = null;
			try {
				File f = TestUtils.findTestFile( root + entry.getKey());
				Assert.assertNotNull( f );
				Assert.assertTrue( f.getName(), f.exists());

				br = new BufferedReader( new FileReader( f ));
				String line = br.readLine();
				Assert.assertNotNull( line );

				FileDefinitionParser parser = new FileDefinitionParser((URI) null, false );
				if( blockType == AbstractRegion.FACET )
					parser.recognizeFacet( line, br );
				else if( blockType == AbstractRegion.COMPONENT )
					parser.recognizeComponent( line, br );
				else if( blockType == AbstractRegion.INSTANCEOF )
					parser.recognizeInstanceOf( line, br, null );
				else
					Assert.fail( "Invalid block type." );

				Assert.assertEquals( "ONE pasing error was expected for '" + entry.getKey() + "'", 1, parser.getFileRelations().getParsingErrors().size());
				ModelError error = parser.getFileRelations().getParsingErrors().iterator().next();
				Assert.assertEquals( "Expected error code " + entry.getValue().getErrorCode() + " for '" + entry.getKey() + "'", entry.getValue().getErrorCode(), error.getErrorCode());
				Assert.assertEquals( "Expected error line to be " + entry.getValue().getLine() + " for '" + entry.getKey() + "'", entry.getValue().getLine(), error.getLine());
				Assert.assertEquals( "Expected a parsing error for '" + entry.getKey() + "'", ErrorCategory.PARSING, error.getErrorCode().getCategory());

			} catch( Exception e ) {
				if( Utils.isEmptyOrWhitespaces( e.getMessage()))
					Assert.fail( "Got an exception (" + e.getClass().getSimpleName() + ") for " + entry.getKey());
				else
					Assert.fail( "Got an exception for " + entry.getKey() + ": " + e.getMessage());

			} finally {
				if( br != null ) {
					try {
						br.close();
					} catch( IOException e ) {
						Assert.fail( e.getMessage());
					}
				}
			}
		}
	}
}
