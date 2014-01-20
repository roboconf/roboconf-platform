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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.roboconf.core.internal.utils.UriHelper;
import net.roboconf.core.internal.utils.Utils;
import net.roboconf.core.model.ErrorCode;
import net.roboconf.core.model.ModelError;
import net.roboconf.core.model.parsing.AbstractIgnorableInstruction;
import net.roboconf.core.model.parsing.AbstractInstruction;
import net.roboconf.core.model.parsing.AbstractPropertiesHolder;
import net.roboconf.core.model.parsing.Constants;
import net.roboconf.core.model.parsing.FileRelations;
import net.roboconf.core.model.parsing.RelationBlank;
import net.roboconf.core.model.parsing.RelationComment;
import net.roboconf.core.model.parsing.RelationComponent;
import net.roboconf.core.model.parsing.RelationFacet;
import net.roboconf.core.model.parsing.RelationImport;
import net.roboconf.core.model.parsing.RelationProperty;

/**
 * A parser for relation files.
 * @author Vincent Zurczak - Linagora
 */
public class RelationsParser {

	static final int P_CODE_YES = 1;
	static final int P_CODE_NO = 2;
	static final int P_CODE_CANCEL = 3;

	private static final char O_CURLY_BRACKET = '{';
	private static final char C_CURLY_BRACKET = '}';
	private static final char SEMI_COLON = ';';

	private final FileRelations fileRelations;
	private boolean ignoreComments = true;
	private boolean lastLineEndedWithLineBreak = false;
	private int currentLineNumber;


	/**
	 * Constructor.
	 * @param relationsFileUri the file URI
	 * @param ignoreComments true to ignore comments during parsing
	 */
	public RelationsParser( URI relationsFileUri, boolean ignoreComments ) {
		this.ignoreComments = ignoreComments;
		this.currentLineNumber = 0;
		this.fileRelations = new FileRelations( relationsFileUri );
	}


	/**
	 * Constructor.
	 * @param relationsFile the relation file
	 * @param ignoreComments true to ignore comments during parsing
	 */
	public RelationsParser( File relationsFile, boolean ignoreComments ) {
		this( relationsFile.toURI(), ignoreComments );
		this.fileRelations.setEditedFile( relationsFile );
	}


	/**
	 * Constructor.
	 * @param relationsFileUri the file URI
	 * @param ignoreComments true to ignore comments during parsing
	 */
	public RelationsParser( String relationsFileUri, boolean ignoreComments ) throws URISyntaxException {
		this( UriHelper.urlToUri( relationsFileUri ), ignoreComments );
	}


	/**
	 * Reads a relation file.
	 * @return an instance of {@link FileRelations} (never null)
	 * <p>
	 * Parsing errors are stored in the result.<br />
	 * See {@link FileRelations#getParingErrors()}.
	 * </p>
	 */
	public FileRelations read() {
		try {
			fillIn();
			mergeContiguousRegions( this.fileRelations.getInstructions());

		} catch( IOException e ) {
			ModelError error = new ModelError( ErrorCode.IO_ERROR, this.currentLineNumber );
			if( e.getMessage() != null )
				error.setDetails( e.getMessage());

			this.fileRelations.getParsingErrors().add( error );
		}

		return this.fileRelations;
	}


	/**
	 * @param line the raw line
	 * @return one of the P_CODE constants from {@link RelationsParser}
	 */
	int recognizeComponent( String line, BufferedReader br ) throws IOException {

		int result = P_CODE_NO;
		String alteredLine = line.trim().toLowerCase();
		if( ! alteredLine.isEmpty()
				&& ! alteredLine.startsWith( String.valueOf( Constants.COMMENT_DELIMITER ))
				&& ! alteredLine.toLowerCase().startsWith( Constants.KEYWORD_FACET )
				&& ! alteredLine.toLowerCase().startsWith( Constants.KEYWORD_IMPORT ))
			result = recognizePropertiesHolder( line, br, new RelationComponent( this.fileRelations ));

		return result;
	}


	/**
	 * @param line the raw line
	 * @return one of the P_CODE constants from {@link RelationsParser}
	 */
	int recognizeFacet( String line, BufferedReader br ) throws IOException {

		int result = P_CODE_NO;
		if( line.trim().toLowerCase().startsWith( Constants.KEYWORD_FACET )) {
			String newLine = line.substring( Constants.KEYWORD_FACET.length());
			result = recognizePropertiesHolder( newLine, br, new RelationFacet( this.fileRelations ));
		}

		return result;
	}


	/**
	 * @param line the raw line
	 * @param instructions the instructions to update
	 * @return {@link #P_CODE_YES} or {@link #P_CODE_NO}
	 */
	int recognizeComment( String line, Collection<AbstractInstruction> instructions ) {

		int result = P_CODE_NO;
		if( line.trim().startsWith( Constants.COMMENT_DELIMITER )) {
			result = P_CODE_YES;
			if( ! this.ignoreComments )
				instructions.add( new RelationComment( this.fileRelations, line ));
		}

		return result;
	}


	/**
	 * @param line the raw line
	 * @param instructions the instructions to update
	 * @return {@link #P_CODE_YES} or {@link #P_CODE_NO}
	 */
	int recognizeBlankLine( String line, Collection<AbstractInstruction> instructions ) {

		int result = P_CODE_NO;
		if( Utils.isEmptyOrWhitespaces( line )) {
			result = P_CODE_YES;
			instructions.add( new RelationBlank( this.fileRelations, line ));
		}

		return result;
	}


	/**
	 * @param line the raw line
	 * @param holder
	 * @return one of the P_CODE constants from {@link RelationsParser}
	 */
	int recognizeProperty( String line, AbstractPropertiesHolder holder ) {

		int result = P_CODE_NO;
		String[] parts = splitFromInlineComment( line );
		String realLine = parts[ 0 ].trim();

		String regex = "([^:\\s]+)\\s*:\\s*([^;]*)";
		Matcher m = Pattern.compile( regex ).matcher( realLine );
		if( m.find()) {
			result = P_CODE_YES;
			RelationProperty instr = new RelationProperty( this.fileRelations );
			instr.setLine( this.currentLineNumber );
			instr.setName( m.group( 1 ));
			instr.setValue( m.group( 2 ));
			instr.setInlineComment( parts[ 1 ]);
			holder.getPropertyNameToProperty().put( instr.getName(), instr );
			holder.getInternalInstructions().add( instr );

			realLine = realLine.substring( m.end());
			if( ! realLine.startsWith( String.valueOf( SEMI_COLON )))
				this.fileRelations.getParsingErrors().add( new ModelError( ErrorCode.PROPERTY_ENDS_WITH_SEMI_COLON, this.currentLineNumber ));
			else if( realLine.indexOf( SEMI_COLON ) < realLine.length() - 1 )
				this.fileRelations.getParsingErrors().add( new ModelError( ErrorCode.ONE_INSTRUCTION_PER_LINE, this.currentLineNumber ));
		}

		return result;
	}


	/**
	 * @param line the raw line
	 * @return one of the P_CODE constants from {@link RelationsParser}
	 */
	int recognizeImport( String line ) {

		int result = P_CODE_NO;
		String[] parts = splitFromInlineComment( line );
		String realLine = parts[ 0 ].trim();

		String regex = "import\\s+([^;]*)";
		Matcher m = Pattern.compile( regex, Pattern.CASE_INSENSITIVE ).matcher( realLine );
		if( m.find()) {
			result = P_CODE_YES;
			RelationImport instr = new RelationImport( this.fileRelations );
			instr.setLine( this.currentLineNumber );
			instr.setUri( m.group( 1 ).trim());
			instr.setInlineComment( parts[ 1 ]);
			this.fileRelations.getInstructions().add( instr );

			realLine = realLine.substring( m.end());
			if( ! realLine.startsWith( String.valueOf( SEMI_COLON )))
				this.fileRelations.getParsingErrors().add( new ModelError( ErrorCode.IMPORT_ENDS_WITH_SEMI_COLON, this.currentLineNumber ));
			else if( realLine.indexOf( SEMI_COLON ) < realLine.length() - 1 )
				this.fileRelations.getParsingErrors().add( new ModelError( ErrorCode.ONE_INSTRUCTION_PER_LINE, this.currentLineNumber ));
		}

		return result;
	}


	/**
	 * Splits the line from the comment delimiter.
	 * @param line a string (not null)
	 * @return an array of 2 strings
	 * <p>
	 * Index 0: the line without the in-line comment. Never null.<br />
	 * Index 1: the in-line comment (if not null, it starts with a '#' symbol).
	 * </p>
	 */
	String[] splitFromInlineComment( String line ) {

		String[] result = new String[] { line, "" };
		int index = line.indexOf( Constants.COMMENT_DELIMITER );
		if( index >= 0 ) {
			result[ 0 ] = line.substring( 0, index );
			if( ! this.ignoreComments ) {
				// Find extra spaces before the in-line comment and put them in the comment
				Matcher m = Pattern.compile( "(\\s+)$" ).matcher( result[ 0 ]);
				String prefix = "";
				if( m.find())
					prefix = m.group( 1 );

				result[ 1 ] = prefix + line.substring( index );
			}

			result[ 0 ] = result[ 0 ].trim();
		}

		return result;
	}


	/**
	 * Merges the contiguous regions for which it makes sense.
	 * <p>
	 * Contiguous comments are merged together, as well as contiguous blank regions.
	 * This reduces the number of regions.
	 * </p>
	 *
	 * @param instructions
	 */
	void mergeContiguousRegions( Collection<AbstractInstruction> instructions ) {

		AbstractIgnorableInstruction initialInstr = null;
		List<AbstractInstruction> toRemove = new ArrayList<AbstractInstruction> ();
		StringBuilder sb = new StringBuilder();

		// We only merge comments and blank regions to reduce their number
		for( AbstractInstruction instr : instructions ) {
			if( initialInstr == null ) {

				if( instr.getInstructionType() == AbstractInstruction.COMMENT
						|| instr.getInstructionType() == AbstractInstruction.BLANK ) {

					AbstractIgnorableInstruction currentInstr = (AbstractIgnorableInstruction) instr;
					initialInstr = currentInstr;
					sb = new StringBuilder( currentInstr.getContent());
				}

			} else if( initialInstr.getInstructionType() == instr.getInstructionType()) {
				toRemove.add( instr );
				sb.append( System.getProperty( "line.separator" ));
				sb.append(((AbstractIgnorableInstruction) instr).getContent());

			} else {
				initialInstr.setContent( sb.toString());
				initialInstr = null;
			}
		}

		// Remove the instructions that have been merged
		instructions.removeAll( toRemove );

		// In a second time, we can reduce facets and components too
		for( AbstractInstruction instr : instructions ) {
			if( instr.getInstructionType() == AbstractInstruction.COMPONENT
					|| instr.getInstructionType() == AbstractInstruction.FACET )
				mergeContiguousRegions(((AbstractPropertiesHolder) instr).getInternalInstructions());
		}
	}


	/**
	 * @return the fileRelations (for tests)
	 */
	FileRelations getFileRelations() {
		return this.fileRelations;
	}


	/**
	 * @param line
	 * @param br
	 * @param holderInstance
	 * @return
	 * @throws IOException
	 */
	private int recognizePropertiesHolder(
			String line,
			BufferedReader br,
			AbstractPropertiesHolder holderInstance )
	throws IOException {

		int result = P_CODE_NO;
		String[] parts = splitFromInlineComment( line );
		String realLine = parts[ 0 ].trim();

		// Recognize the declaration
		AbstractPropertiesHolder holder = null;
		StringBuilder sb = new StringBuilder();
		boolean endInstructionReached = false, foundExtraChars = false;
		for( char c : realLine.toCharArray()) {
			if( c == O_CURLY_BRACKET )
				endInstructionReached = true;
			else if( ! endInstructionReached )
				sb.append( c );
			else {
				foundExtraChars = true;
				break;
			}
		}

		if( foundExtraChars ) {
			this.fileRelations.getParsingErrors().add( new ModelError( ErrorCode.O_C_BRACKET_EXTRA_CHARACTERS, this.currentLineNumber ));
			result = P_CODE_CANCEL;

		} else if( ! endInstructionReached ) {
			this.fileRelations.getParsingErrors().add( new ModelError( ErrorCode.O_C_BRACKET_MISSING, this.currentLineNumber ));
			result = P_CODE_CANCEL;

		} else {
			result = P_CODE_YES;
			holder = holderInstance;
			holder.setName( sb.toString().trim());
			holder.setLine( this.currentLineNumber );
			holder.setInlineComment( parts[ 1 ]);
			this.fileRelations.getInstructions().add( holder );
		}

		// Recognize the properties
		boolean errorInProperties = false;
		if( holder != null ) {
			while(( line = nextLine( br )) != null ) {

				int code = recognizeBlankLine( line, holder.getInternalInstructions());
				if( code == P_CODE_YES )
					continue;

				code = recognizeComment( line, holder.getInternalInstructions());
				if( code == P_CODE_YES )
					continue;

				code = recognizeProperty( line, holder );
				if( code == P_CODE_CANCEL )
					result = P_CODE_CANCEL;

				if( code != P_CODE_YES ) {
					errorInProperties = true;
					break;
				}
			}
		}

		// Recognize the declaration end
		if( result == P_CODE_YES ) {
			if( line == null
					|| ! line.trim().startsWith( String.valueOf( C_CURLY_BRACKET ))) {

				if( errorInProperties )
					this.fileRelations.getParsingErrors().add( new ModelError( ErrorCode.INVALID_PROPERTY, this.currentLineNumber ));
				else
					this.fileRelations.getParsingErrors().add( new ModelError( ErrorCode.C_C_BRACKET_MISSING, this.currentLineNumber ));

			} else {
				parts = splitFromInlineComment( line );
				if( parts[ 0 ].length() > 1 )
					this.fileRelations.getParsingErrors().add( new ModelError( ErrorCode.C_C_BRACKET_EXTRA_CHARACTERS, this.currentLineNumber ));

				holder.setClosingInlineComment( parts[ 1 ]);
			}
		}

		return result;
	}


	/**
	 * @param br
	 * @return
	 * @throws IOException
	 */
	private String nextLine( BufferedReader br ) throws IOException {

		// {@link BufferedReader#readLine()} does not allow to detect when the last line is empty.
		// We need this precision. So, we read character by character.
		int c = 0;
		StringBuilder sb = new StringBuilder();
		while(( c = br.read()) != -1
				&& ((char) c) != '\n' ) {

			if((char) c != '\r' )
				sb.append((char) c);
		}

		if( sb.length() > 0 )
			this.lastLineEndedWithLineBreak = c != -1;

		String line = c == -1 && sb.length() == 0 ? null : sb.toString();
		if( line != null )
			this.currentLineNumber ++;

		return line;
	}


	/**
	 * Parses the file and fills-in the resulting structure.
	 * @throws IOException
	 */
	private void fillIn() throws IOException {

		BufferedReader br = null;
		try {
			InputStream in = this.fileRelations.getFileLocation().toURL().openStream();
			br = new BufferedReader( new InputStreamReader( in, "UTF-8" ));

			String line;
			while(( line = nextLine( br )) != null ) {

				int code = recognizeBlankLine( line, this.fileRelations.getInstructions());
				if( code == P_CODE_YES )
					continue;

				code = recognizeComment( line, this.fileRelations.getInstructions());
				if( code == P_CODE_YES )
					continue;

				code = recognizeImport( line );
				if( code == P_CODE_CANCEL )
					break;
				else if( code == P_CODE_YES )
					continue;

				code = recognizeFacet( line, br );
				if( code == P_CODE_CANCEL )
					break;
				else if( code == P_CODE_YES )
					continue;

				code = recognizeComponent( line, br );
				if( code == P_CODE_CANCEL )
					break;
				else if( code == P_CODE_NO )
					this.fileRelations.getParsingErrors().add( new ModelError( ErrorCode.UNRECOGNIZED_INSTRUCTION, this.currentLineNumber ));
			}

			if( line == null
					&& this.lastLineEndedWithLineBreak )
				this.fileRelations.getInstructions().add( new RelationBlank( this.fileRelations, "" ));

		} finally {
			if( br != null )
				br.close();
		}
	}
}
