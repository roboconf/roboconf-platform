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
import net.roboconf.core.model.parsing.AbstractInstruction;
import net.roboconf.core.model.parsing.FileRelations;
import net.roboconf.core.model.parsing.RelationBlank;
import net.roboconf.core.model.parsing.RelationComment;
import net.roboconf.core.model.parsing.RelationComponent;
import net.roboconf.core.model.parsing.RelationImport;
import net.roboconf.core.model.parsing.RelationProperty;

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class RelationsParserTest {

	@Test
	public void testRecognizeComment() {

		Collection<AbstractInstruction> instr = new ArrayList<AbstractInstruction> ();
		String[] validLines = {
				"# ok",
				"#",
				"     # woo ",
				"  ###############"
		};

		// Without ignoring comments
		for( String line : validLines ) {
			instr.clear();
			RelationsParser parser = new RelationsParser((URI) null, false );
			int code = parser.recognizeComment( line, instr );

			Assert.assertEquals( "Expected a YES code for '" + line + "'", RelationsParser.P_CODE_YES, code );
			Assert.assertEquals( "No pasing error was expected for '" + line + "'", 0, parser.getFileRelations().getParsingErrors().size());
			Assert.assertEquals( "Expected ONE instruction to be registered for '" + line + "'", 1, instr.size());
			Assert.assertEquals( "Expected ONE comment instruction for '" + line + "'",
					AbstractInstruction.COMMENT,
					instr.iterator().next().getInstructionType());
		}

		// Ignoring comments
		for( String line : validLines ) {
			instr.clear();
			RelationsParser parser = new RelationsParser((URI) null, true );
			int code = parser.recognizeComment( line, instr );

			Assert.assertEquals( "Expected a YES code for '" + line + "'", RelationsParser.P_CODE_YES, code );
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
			RelationsParser parser = new RelationsParser((URI) null, true );
			int code = parser.recognizeComment( line, instr );

			Assert.assertEquals( "Expected a NO code for '" + line + "'", RelationsParser.P_CODE_NO, code );
			Assert.assertEquals( "No pasing error was expected for '" + line + "'", 0, parser.getFileRelations().getParsingErrors().size());
			Assert.assertEquals( "Expected ZERO instruction to be registered for '" + line + "'", 0, instr.size());
		}
	}


	@Test
	public void testRecognizeBlank() {
		Collection<AbstractInstruction> instr = new ArrayList<AbstractInstruction> ();

		String[] validLines = { "  ", "" };
		for( String line : validLines ) {
			instr.clear();
			RelationsParser parser = new RelationsParser((URI) null, false );
			int code = parser.recognizeBlankLine( line, instr );

			Assert.assertEquals( "Expected a YES code for '" + line + "'", RelationsParser.P_CODE_YES, code );
			Assert.assertEquals( "No pasing error was expected for '" + line + "'", 0, parser.getFileRelations().getParsingErrors().size());
			Assert.assertEquals( "Expected ONE instruction to be registered for '" + line + "'", 1, instr.size());
			Assert.assertEquals( "Expected ONE blank instruction for '" + line + "'",
					AbstractInstruction.BLANK,
					instr.iterator().next().getInstructionType());
		}

		String[] invalidLines = { "#", "something", "  something with spaces before an after  " };
		for( String line : invalidLines ) {
			instr.clear();
			RelationsParser parser = new RelationsParser((URI) null, false );
			int code = parser.recognizeBlankLine( line, instr );

			Assert.assertEquals( "Expected a NO code for '" + line + "'", RelationsParser.P_CODE_NO, code );
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
			RelationsParser parser = new RelationsParser((URI) null, false );
			String theLine = line[ 0 ] + line[ 1 ];
			String[] parts = parser.splitFromInlineComment( theLine );
			Assert.assertEquals( "Test failed for '" + theLine + "'", line[ 0 ], parts[ 0 ]);
			Assert.assertEquals( "Test failed for '" + theLine + "'", line[ 1 ], parts[ 1 ]);
		}

		// Ignoring comments
		for( String[] line : beforeToAfter ) {
			RelationsParser parser = new RelationsParser((URI) null, true );
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
			RelationsParser parser = new RelationsParser((URI) null, false );
			int code = parser.recognizeImport( line );
			FileRelations fr = parser.getFileRelations();

			Assert.assertEquals( "Expected a YES code for '" + line + "'", RelationsParser.P_CODE_YES, code );
			Assert.assertEquals( "No pasing error was expected for '" + line + "'", 0, fr.getParsingErrors().size());
			Assert.assertEquals( "Expected ONE instruction to be registered for '" + line + "'", 1, fr.getInstructions().size());

			RelationImport imp = (RelationImport) fr.getInstructions().iterator().next();
			Assert.assertEquals( "Expected ONE import instruction for '" + line + "'", AbstractInstruction.IMPORT, imp.getInstructionType());

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
			RelationsParser parser = new RelationsParser((URI) null, false );
			String theLine = line[ 0 ] + line[ 1 ];
			int code = parser.recognizeImport( theLine );
			FileRelations fr = parser.getFileRelations();

			Assert.assertEquals( "Expected a YES code for '" + theLine + "'", RelationsParser.P_CODE_YES, code );
			Assert.assertEquals( "No pasing error was expected for '" + theLine + "'", 0, fr.getParsingErrors().size());
			Assert.assertEquals( "Expected ONE instruction to be registered for '" + theLine + "'", 1, fr.getInstructions().size());

			RelationImport imp = (RelationImport) fr.getInstructions().iterator().next();
			Assert.assertEquals( "Expected ONE import instruction for '" + theLine + "'", AbstractInstruction.IMPORT, imp.getInstructionType());

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
			RelationsParser parser = new RelationsParser((URI) null, false );
			int code = parser.recognizeImport( line );
			Assert.assertEquals( "Expected a NO code for '" + line + "'", RelationsParser.P_CODE_NO, code );
		}


		// Test error codes
		Map<String,ErrorCode> invalidLineToErrorCode = new LinkedHashMap<String,ErrorCode> ();
		invalidLineToErrorCode.put( "import facets.rcf", ErrorCode.IMPORT_ENDS_WITH_SEMI_COLON );
		invalidLineToErrorCode.put( "import facets.rcf ; facet toto {", ErrorCode.ONE_INSTRUCTION_PER_LINE );
		invalidLineToErrorCode.put( "import facets.rcf ; invalid comment", ErrorCode.ONE_INSTRUCTION_PER_LINE );
		invalidLineToErrorCode.put( "import facets.rcf ;;", ErrorCode.ONE_INSTRUCTION_PER_LINE );

		for( Map.Entry<String,ErrorCode> entry : invalidLineToErrorCode.entrySet()) {
			String line = entry.getKey();
			RelationsParser parser = new RelationsParser((URI) null, false );
			int code = parser.recognizeImport( entry.getKey());
			FileRelations fr = parser.getFileRelations();

			Assert.assertEquals( "Expected a YES code for '" + line + "'", RelationsParser.P_CODE_YES, code );
			Assert.assertEquals( "A pasing error was expected for '" + line + "'", 1, fr.getParsingErrors().size());
			Assert.assertEquals( "Expected ONE instruction to be registered for '" + line + "'", 1, fr.getInstructions().size());
			Assert.assertEquals( "Expected ONE import instruction for '" + line + "'",
					AbstractInstruction.IMPORT,
					fr.getInstructions().iterator().next().getInstructionType());

			ModelError error = fr.getParsingErrors().iterator().next();
			Assert.assertEquals( "Expecting parsing error for '" + line + "'", entry.getValue().getCategory(), ErrorCategory.PARSING );
			Assert.assertEquals( "Invalid error code for '" + line + "'", entry.getValue(), error.getErrorCode());
		}
	}


	@Test
	public void testRecognizeProperty() {

		FileRelations file = new FileRelations( new File( "some-file" ));
		String[] validLines = {
				"property: value;",
				"   property : value  ;",
				"PROPERTY: value; # with an inline comment",
				"property: value1, value2  ,  value3;",
				"property:;"
		};

		// Comments do not matter here
		for( String line : validLines ) {
			RelationsParser parser = new RelationsParser((URI) null, false );
			RelationComponent holder = new RelationComponent( file );
			int code = parser.recognizeProperty( line, holder );

			Assert.assertEquals( "Expected a YES code for '" + line + "'", RelationsParser.P_CODE_YES, code );
			Assert.assertEquals( "No pasing error was expected for '" + line + "'", 0, parser.getFileRelations().getParsingErrors().size());
			Assert.assertEquals( "Expected ONE instruction to be registered for '" + line + "'", 1, holder.getInternalInstructions().size());

			RelationProperty prop = (RelationProperty) holder.getInternalInstructions().iterator().next();
			Assert.assertEquals( "Expected ONE property for '" + line + "'", AbstractInstruction.PROPERTY, prop.getInstructionType());
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
			RelationsParser parser = new RelationsParser((URI) null, false );
			String theLine = line[ 0 ] + line[ 1 ];
			RelationComponent holder = new RelationComponent( file );
			int code = parser.recognizeProperty( theLine, holder );

			Assert.assertEquals( "Expected a YES code for '" + theLine + "'", RelationsParser.P_CODE_YES, code );
			Assert.assertEquals( "No pasing error was expected for '" + theLine + "'", 0, parser.getFileRelations().getParsingErrors().size());
			Assert.assertEquals( "Expected ONE instruction to be registered for '" + theLine + "'", 1, holder.getInternalInstructions().size());

			RelationProperty prop = (RelationProperty) holder.getInternalInstructions().iterator().next();
			Assert.assertEquals( "Expected ONE property for '" + theLine + "'", AbstractInstruction.PROPERTY, prop.getInstructionType());

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
			RelationsParser parser = new RelationsParser((URI) null, false );
			RelationComponent holder = new RelationComponent( file );
			int code = parser.recognizeProperty( line, holder );
			Assert.assertEquals( "Expected a NO code for '" + line + "'", RelationsParser.P_CODE_NO, code );
		}


		// Test error codes
		Map<String,ErrorCode> invalidLineToErrorCode = new LinkedHashMap<String,ErrorCode> ();
		invalidLineToErrorCode.put( "property: value", ErrorCode.PROPERTY_ENDS_WITH_SEMI_COLON );
		invalidLineToErrorCode.put( "property:", ErrorCode.PROPERTY_ENDS_WITH_SEMI_COLON );
		invalidLineToErrorCode.put( "property: value ; facet toto {", ErrorCode.ONE_INSTRUCTION_PER_LINE );
		invalidLineToErrorCode.put( "property: value ; invalid comment", ErrorCode.ONE_INSTRUCTION_PER_LINE );
		invalidLineToErrorCode.put( "property: value;;", ErrorCode.ONE_INSTRUCTION_PER_LINE );

		for( Map.Entry<String,ErrorCode> entry : invalidLineToErrorCode.entrySet()) {
			String line = entry.getKey();
			RelationsParser parser = new RelationsParser((URI) null, false );
			RelationComponent holder = new RelationComponent( file );
			FileRelations fr = parser.getFileRelations();
			int code = parser.recognizeProperty( line, holder );

			Assert.assertEquals( "Expected a YES code for '" + line + "'", RelationsParser.P_CODE_YES, code );
			Assert.assertEquals( "A pasing error was expected for '" + line + "'", 1, fr.getParsingErrors().size());
			Assert.assertEquals( "Expected ONE instruction to be registered for '" + line + "'", 1, holder.getInternalInstructions().size());
			Assert.assertEquals( "Expected ONE property for '" + line + "'",
					AbstractInstruction.PROPERTY,
					holder.getInternalInstructions().iterator().next().getInstructionType());

			ModelError error = fr.getParsingErrors().iterator().next();
			Assert.assertEquals( "Expecting parsing error for '" + line + "'", entry.getValue().getCategory(), ErrorCategory.PARSING );
			Assert.assertEquals( "Invalid error code for '" + line + "'", entry.getValue(), error.getErrorCode());
		}
	}


	@Test
	public void testMergeContiguousRegions() {
		FileRelations file = new FileRelations( new File( "some-file" ));

		RelationsParser parser = new RelationsParser((URI) null, false );
		List<AbstractInstruction> instructions = new ArrayList<AbstractInstruction> ();
		instructions.add( new RelationImport( file ));
		instructions.add( new RelationImport( file ));
		parser.mergeContiguousRegions( instructions );
		Assert.assertEquals( 2, instructions.size());

		instructions.clear();
		instructions.add( new RelationProperty( file ));
		instructions.add( new RelationProperty( file ));
		parser.mergeContiguousRegions( instructions );
		Assert.assertEquals( 2, instructions.size());

		instructions.clear();
		instructions.add( new RelationComment( file, "# comment 1" ));
		instructions.add( new RelationComment( file, "# comment 2" ));
		parser.mergeContiguousRegions( instructions );
		Assert.assertEquals( 1, instructions.size());
		Assert.assertEquals( AbstractInstruction.COMMENT, instructions.get( 0 ).getInstructionType());

		instructions.clear();
		instructions.add( new RelationComment( file, "# comment 1" ));
		instructions.add( new RelationBlank( file, "" ));
		instructions.add( new RelationComment( file, "# comment 2" ));
		parser.mergeContiguousRegions( instructions );
		Assert.assertEquals( 3, instructions.size());

		instructions.clear();
		instructions.add( new RelationComment( file, "# comment 1" ));
		instructions.add( new RelationBlank( file, "" ));
		instructions.add( new RelationComment( file, "# comment 2" ));
		instructions.add( new RelationComment( file, "# comment 3" ));
		parser.mergeContiguousRegions( instructions );
		Assert.assertEquals( 3, instructions.size());
		Assert.assertEquals( AbstractInstruction.COMMENT, instructions.get( 0 ).getInstructionType());
		Assert.assertEquals( AbstractInstruction.BLANK, instructions.get( 1 ).getInstructionType());
		Assert.assertEquals( AbstractInstruction.COMMENT, instructions.get( 0 ).getInstructionType());

		instructions.clear();
		instructions.add( new RelationBlank( file, "" ));
		instructions.add( new RelationBlank( file, "" ));
		parser.mergeContiguousRegions( instructions );
		Assert.assertEquals( 1, instructions.size());
		Assert.assertEquals( AbstractInstruction.BLANK, instructions.get( 0 ).getInstructionType());

		instructions.clear();
		instructions.add( new RelationBlank( file, "" ));
		instructions.add( new RelationComment( file, "# comment 1" ));
		instructions.add( new RelationBlank( file, "" ));
		parser.mergeContiguousRegions( instructions );
		Assert.assertEquals( 3, instructions.size());

		instructions.clear();
		instructions.add( new RelationBlank( file, "" ));
		instructions.add( new RelationBlank( file, "" ));
		instructions.add( new RelationComment( file, "# comment 1" ));
		instructions.add( new RelationBlank( file, "" ));
		parser.mergeContiguousRegions( instructions );
		Assert.assertEquals( 3, instructions.size());
		Assert.assertEquals( AbstractInstruction.BLANK, instructions.get( 0 ).getInstructionType());
		Assert.assertEquals( AbstractInstruction.COMMENT, instructions.get( 1 ).getInstructionType());
		Assert.assertEquals( AbstractInstruction.BLANK, instructions.get( 0 ).getInstructionType());
	}


	@Test
	public void testRecognizeFacet() {

		Map<String,ErrorCode> resourceNameToErrorCode = new LinkedHashMap<String,ErrorCode> ();
		resourceNameToErrorCode.put( "facet-invalid-property.rcf", ErrorCode.INVALID_PROPERTY );
		resourceNameToErrorCode.put( "facet-invalid-end.rcf", ErrorCode.C_C_BRACKET_EXTRA_CHARACTERS );
		resourceNameToErrorCode.put( "facet-missing-closing-cb.rcf", ErrorCode.C_C_BRACKET_MISSING );
		resourceNameToErrorCode.put( "facet-missing-opening-cb.rcf", ErrorCode.O_C_BRACKET_MISSING );
		resourceNameToErrorCode.put( "facet-extra-char.rcf", ErrorCode.O_C_BRACKET_EXTRA_CHARACTERS );

		testRecognizePropertiesHolder( resourceNameToErrorCode, true );
	}


	@Test
	public void testRecognizeComponent() {

		Map<String,ErrorCode> resourceNameToErrorCode = new LinkedHashMap<String,ErrorCode> ();
		resourceNameToErrorCode.put( "component-invalid-property.rcf", ErrorCode.INVALID_PROPERTY );
		resourceNameToErrorCode.put( "component-invalid-end.rcf", ErrorCode.C_C_BRACKET_EXTRA_CHARACTERS );
		resourceNameToErrorCode.put( "component-missing-closing-cb.rcf", ErrorCode.C_C_BRACKET_MISSING );
		resourceNameToErrorCode.put( "component-missing-opening-cb.rcf", ErrorCode.O_C_BRACKET_MISSING );
		resourceNameToErrorCode.put( "component-extra-char.rcf", ErrorCode.O_C_BRACKET_EXTRA_CHARACTERS );

		testRecognizePropertiesHolder( resourceNameToErrorCode, false );
	}


	private void testRecognizePropertiesHolder( Map<String,ErrorCode> resourceNameToErrorCode, boolean facet ) {

		final String root = "/configurations/graph/invalid/";
		for( Map.Entry<String,ErrorCode> entry : resourceNameToErrorCode.entrySet()) {
			BufferedReader br = null;
			try {
				File f = TestUtils.findTestFile( root + entry.getKey());
				br = new BufferedReader( new FileReader( f ));
				String line = br.readLine();
				Assert.assertNotNull( line );

				RelationsParser parser = new RelationsParser((URI) null, false );
				if( facet )
					parser.recognizeFacet( line, br );
				else
					parser.recognizeComponent( line, br );

				Assert.assertEquals( "ONE pasing error was expected for '" + entry.getKey() + "'", 1, parser.getFileRelations().getParsingErrors().size());
				ModelError error = parser.getFileRelations().getParsingErrors().iterator().next();
				Assert.assertEquals( "Expected error code " + entry.getValue() + " for '" + entry.getKey() + "'", entry.getValue(), error.getErrorCode());
				Assert.assertEquals( "Expected a parsing error for '" + entry.getKey() + "'", ErrorCategory.PARSING, error.getErrorCode().getCategory());

			} catch( Exception e ) {
				Assert.fail( e.getMessage());

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
