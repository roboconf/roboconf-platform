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

import net.roboconf.core.ErrorCode;
import net.roboconf.core.internal.utils.UriUtils;
import net.roboconf.core.internal.utils.Utils;
import net.roboconf.core.model.ModelError;
import net.roboconf.core.model.parsing.AbstractBlock;
import net.roboconf.core.model.parsing.AbstractBlockHolder;
import net.roboconf.core.model.parsing.AbstractIgnorableInstruction;
import net.roboconf.core.model.parsing.BlockBlank;
import net.roboconf.core.model.parsing.BlockComment;
import net.roboconf.core.model.parsing.BlockComponent;
import net.roboconf.core.model.parsing.BlockFacet;
import net.roboconf.core.model.parsing.BlockImport;
import net.roboconf.core.model.parsing.BlockInstanceOf;
import net.roboconf.core.model.parsing.BlockProperty;
import net.roboconf.core.model.parsing.FileDefinition;
import net.roboconf.core.model.parsing.ParsingConstants;

/**
 * A parser for relation files.
 * @author Vincent Zurczak - Linagora
 */
public class FileDefinitionParser {

	static final int P_CODE_YES = 1;
	static final int P_CODE_NO = 2;
	static final int P_CODE_CANCEL = 3;

	private static final char O_CURLY_BRACKET = '{';
	private static final char C_CURLY_BRACKET = '}';
	private static final char SEMI_COLON = ';';

	private final FileDefinition definitionFile;
	private boolean ignoreComments = true;
	private boolean lastLineEndedWithLineBreak = false;
	private int currentLineNumber;


	/**
	 * Constructor.
	 * @param relationsFileUri the file URI
	 * @param ignoreComments true to ignore comments during parsing
	 */
	public FileDefinitionParser( URI relationsFileUri, boolean ignoreComments ) {
		this.ignoreComments = ignoreComments;
		this.currentLineNumber = 1;
		this.definitionFile = new FileDefinition( relationsFileUri );
	}


	/**
	 * Constructor.
	 * @param relationsFile the relation file (not null)
	 * @param ignoreComments true to ignore comments during parsing
	 */
	public FileDefinitionParser( File relationsFile, boolean ignoreComments ) {
		this( relationsFile.toURI(), ignoreComments );
		this.definitionFile.setEditedFile( relationsFile );
	}


	/**
	 * Constructor.
	 * @param relationsFileUri the file URI
	 * @param ignoreComments true to ignore comments during parsing
	 */
	public FileDefinitionParser( String relationsFileUri, boolean ignoreComments ) throws URISyntaxException {
		this( UriUtils.urlToUri( relationsFileUri ), ignoreComments );
	}


	/**
	 * Reads a definition file.
	 * @return an instance of {@link FileDefinition} (never null)
	 * <p>
	 * Parsing errors are stored in the result.<br />
	 * See {@link FileDefinition#getParingErrors()}.
	 * </p>
	 */
	public FileDefinition read() {

		// Parse blocks
		try {
			fillIn();
			mergeContiguousRegions( this.definitionFile.getBlocks());

		} catch( IOException e ) {
			ModelError error = new ModelError( ErrorCode.P_IO_ERROR, this.currentLineNumber );
			if( e.getMessage() != null )
				error.setDetails( e.getMessage());

			this.definitionFile.getParsingErrors().add( error );
		}

		// Determine file type
		boolean hasFacets = false, hasComponents = false, hasInstances = false, hasImports = false;
		for( AbstractBlock block : this.definitionFile.getBlocks()) {
			if( block.getInstructionType() == AbstractBlock.COMPONENT )
				hasComponents = true;
			else if( block.getInstructionType() == AbstractBlock.FACET )
				hasFacets = true;
			else if( block.getInstructionType() == AbstractBlock.INSTANCEOF )
				hasInstances = true;
			else if( block.getInstructionType() == AbstractBlock.IMPORT )
				hasImports = true;
		}

		if( hasInstances ) {
			if( ! hasFacets && ! hasComponents )
				this.definitionFile.setFileType( FileDefinition.INSTANCE );
			else
				this.definitionFile.getParsingErrors().add( new ModelError( ErrorCode.P_INVALID_FILE_TYPE, 1 ));

		} else if( hasFacets || hasComponents ) {
			this.definitionFile.setFileType( FileDefinition.GRAPH );

		} else if( hasImports ) {
			this.definitionFile.setFileType( FileDefinition.AGGREGATOR );

		} else {
			this.definitionFile.getParsingErrors().add( new ModelError( ErrorCode.P_NO_FILE_TYPE, 1 ));
		}

		return this.definitionFile;
	}


	/**
	 * @param line the raw line
	 * @return one of the P_CODE constants from {@link FileDefinitionParser}
	 */
	int recognizeComponent( String line, BufferedReader br ) throws IOException {

		int result = P_CODE_NO;
		String alteredLine = line.trim().toLowerCase();
		if( ! alteredLine.isEmpty()
				&& ! alteredLine.startsWith( String.valueOf( ParsingConstants.COMMENT_DELIMITER ))
				&& ! alteredLine.toLowerCase().startsWith( ParsingConstants.KEYWORD_FACET )
				&& ! alteredLine.toLowerCase().startsWith( ParsingConstants.KEYWORD_INSTANCE_OF )
				&& ! alteredLine.toLowerCase().startsWith( ParsingConstants.KEYWORD_IMPORT ))
			result = recognizePropertiesHolder( line, br, new BlockComponent( this.definitionFile ));

		return result;
	}


	/**
	 * @param line the raw line
	 * @return one of the P_CODE constants from {@link FileDefinitionParser}
	 */
	int recognizeFacet( String line, BufferedReader br ) throws IOException {

		int result = P_CODE_NO;
		if( line.trim().toLowerCase().startsWith( ParsingConstants.KEYWORD_FACET )) {
			String newLine = line.replaceFirst( "\\s*" + Pattern.quote( ParsingConstants.KEYWORD_FACET ), "" );
			result = recognizePropertiesHolder( newLine, br, new BlockFacet( this.definitionFile ));
		}

		return result;
	}


	/**
	 * @param line the raw line
	 * @param holderInstance
	 * @return one of the P_CODE constants from {@link FileDefinitionParser}
	 */
	int recognizeInstanceOf( String line, BufferedReader br, AbstractBlockHolder holderInstance ) throws IOException {

		int result = P_CODE_NO;
		if( line.trim().toLowerCase().startsWith( ParsingConstants.KEYWORD_INSTANCE_OF )) {
			String newLine = line.replaceFirst( "\\s*" + Pattern.quote( ParsingConstants.KEYWORD_INSTANCE_OF ), "" );
			BlockInstanceOf newInstance = new BlockInstanceOf( this.definitionFile );
			result = recognizePropertiesHolder( newLine, br, newInstance );

			// Handle imbricated instances
			if( result == P_CODE_YES
					&& holderInstance != null ) {
				this.definitionFile.getBlocks().remove( newInstance );
				holderInstance.getInnerBlocks().add( newInstance );
			}
		}

		return result;
	}


	/**
	 * @param line the raw line
	 * @param blocks the blocks to update
	 * @return {@link #P_CODE_YES} or {@link #P_CODE_NO}
	 */
	int recognizeComment( String line, Collection<AbstractBlock> blocks ) {

		int result = P_CODE_NO;
		if( line.trim().startsWith( ParsingConstants.COMMENT_DELIMITER )) {
			result = P_CODE_YES;
			if( ! this.ignoreComments )
				blocks.add( new BlockComment( this.definitionFile, line ));
		}

		return result;
	}


	/**
	 * @param line the raw line
	 * @param blocks the blocks to update
	 * @return {@link #P_CODE_YES} or {@link #P_CODE_NO}
	 */
	int recognizeBlankLine( String line, Collection<AbstractBlock> blocks ) {

		int result = P_CODE_NO;
		if( Utils.isEmptyOrWhitespaces( line )) {
			result = P_CODE_YES;
			blocks.add( new BlockBlank( this.definitionFile, line ));
		}

		return result;
	}


	/**
	 * @param line the raw line
	 * @param holder
	 * @return one of the P_CODE constants from {@link FileDefinitionParser}
	 */
	int recognizeProperty( String line, AbstractBlockHolder holder ) {

		int result = P_CODE_NO;
		String[] parts = splitFromInlineComment( line );
		String realLine = parts[ 0 ].trim();

		String regex = "([^:\\s]+)\\s*:\\s*([^;]*)";
		Matcher m = Pattern.compile( regex ).matcher( realLine );
		if( m.find()) {
			result = P_CODE_YES;
			BlockProperty block = new BlockProperty( this.definitionFile );
			block.setLine( this.currentLineNumber );
			block.setName( m.group( 1 ));
			block.setValue( m.group( 2 ));
			block.setInlineComment( parts[ 1 ]);
			holder.getInnerBlocks().add( block );

			realLine = realLine.substring( m.end());
			if( ! realLine.startsWith( String.valueOf( SEMI_COLON )))
				this.definitionFile.getParsingErrors().add( new ModelError( ErrorCode.P_PROPERTY_ENDS_WITH_SEMI_COLON, this.currentLineNumber ));
			else if( realLine.indexOf( SEMI_COLON ) < realLine.length() - 1 )
				this.definitionFile.getParsingErrors().add( new ModelError( ErrorCode.P_ONE_BLOCK_PER_LINE, this.currentLineNumber ));
		}

		return result;
	}


	/**
	 * @param line the raw line
	 * @return one of the P_CODE constants from {@link FileDefinitionParser}
	 */
	int recognizeImport( String line ) {

		int result = P_CODE_NO;
		String[] parts = splitFromInlineComment( line );
		String realLine = parts[ 0 ].trim();

		String regex = "import\\s+([^;]*)";
		Matcher m = Pattern.compile( regex, Pattern.CASE_INSENSITIVE ).matcher( realLine );
		if( m.find()) {
			result = P_CODE_YES;
			BlockImport block = new BlockImport( this.definitionFile );
			block.setLine( this.currentLineNumber );
			block.setUri( m.group( 1 ).trim());
			block.setInlineComment( parts[ 1 ]);
			this.definitionFile.getBlocks().add( block );

			realLine = realLine.substring( m.end());
			if( ! realLine.startsWith( String.valueOf( SEMI_COLON )))
				this.definitionFile.getParsingErrors().add( new ModelError( ErrorCode.P_IMPORT_ENDS_WITH_SEMI_COLON, this.currentLineNumber ));
			else if( realLine.indexOf( SEMI_COLON ) < realLine.length() - 1 )
				this.definitionFile.getParsingErrors().add( new ModelError( ErrorCode.P_ONE_BLOCK_PER_LINE, this.currentLineNumber ));
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
		int index = line.indexOf( ParsingConstants.COMMENT_DELIMITER );
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
	 * @param blocks
	 */
	void mergeContiguousRegions( Collection<AbstractBlock> blocks ) {

		AbstractIgnorableInstruction initialInstr = null;
		List<AbstractBlock> toRemove = new ArrayList<AbstractBlock> ();
		StringBuilder sb = new StringBuilder();

		// We only merge comments and blank regions to reduce their number
		for( AbstractBlock block : blocks ) {
			if( initialInstr == null ) {

				if( block.getInstructionType() == AbstractBlock.COMMENT
						|| block.getInstructionType() == AbstractBlock.BLANK ) {

					AbstractIgnorableInstruction currentInstr = (AbstractIgnorableInstruction) block;
					initialInstr = currentInstr;
					sb = new StringBuilder( currentInstr.getContent());
				}

			} else if( initialInstr.getInstructionType() == block.getInstructionType()) {
				toRemove.add( block );
				sb.append( System.getProperty( "line.separator" ));
				sb.append(((AbstractIgnorableInstruction) block).getContent());

			} else {
				initialInstr.setContent( sb.toString());
				initialInstr = null;
			}
		}

		// Remove the blocks that have been merged
		blocks.removeAll( toRemove );

		// In a second time, we can reduce facets and components too
		for( AbstractBlock block : blocks ) {
			if( block.getInstructionType() == AbstractBlock.COMPONENT
					|| block.getInstructionType() == AbstractBlock.FACET )
				mergeContiguousRegions(((AbstractBlockHolder) block).getInnerBlocks());
		}
	}


	/**
	 * @return the definitionFile (for tests)
	 */
	FileDefinition getFileRelations() {
		return this.definitionFile;
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
			AbstractBlockHolder holderInstance )
	throws IOException {

		int result = P_CODE_NO;
		String[] parts = splitFromInlineComment( line );
		String realLine = parts[ 0 ].trim();

		// Recognize the declaration
		AbstractBlockHolder holder = null;
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
			this.definitionFile.getParsingErrors().add( new ModelError( ErrorCode.P_O_C_BRACKET_EXTRA_CHARACTERS, this.currentLineNumber ));
			result = P_CODE_CANCEL;

		} else if( ! endInstructionReached ) {
			this.definitionFile.getParsingErrors().add( new ModelError( ErrorCode.P_O_C_BRACKET_MISSING, this.currentLineNumber ));
			result = P_CODE_CANCEL;

		} else {
			result = P_CODE_YES;
			holder = holderInstance;
			holder.setName( sb.toString().trim());
			holder.setLine( this.currentLineNumber );
			holder.setInlineComment( parts[ 1 ]);
			this.definitionFile.getBlocks().add( holder );
		}

		// Recognize the properties
		boolean errorInSubProperties = false;
		if( holder != null ) {
			while(( line = nextLine( br )) != null
						&& ! line.trim().startsWith( String.valueOf( C_CURLY_BRACKET ))) {

				int code = recognizeBlankLine( line, holder.getInnerBlocks());
				if( code == P_CODE_YES )
					continue;

				code = recognizeComment( line, holder.getInnerBlocks());
				if( code == P_CODE_YES )
					continue;

				code = recognizeInstanceOf( line, br, holderInstance );
				if( code == P_CODE_NO )
					code = recognizeProperty( line, holder );

				if( code == P_CODE_CANCEL )
					result = P_CODE_CANCEL;

				if( code != P_CODE_YES ) {
					errorInSubProperties = true;
					break;
				}
			}
		}

		// Why did we exit the loop?
		// 1. We found an invalid content for the holder.
		// 2. We reached EOF or we found a closing curly bracket.
		// 3. We never entered the loop!

		// Inner errors prevail
		if( errorInSubProperties ) {
			if( result == P_CODE_YES ) {
				if( holderInstance.getInstructionType() == AbstractBlock.INSTANCEOF )
					this.definitionFile.getParsingErrors().add( new ModelError( ErrorCode.P_INVALID_PROPERTY_OR_INSTANCE, this.currentLineNumber ));
				else
					this.definitionFile.getParsingErrors().add( new ModelError( ErrorCode.P_INVALID_PROPERTY, this.currentLineNumber ));
			}

			result = P_CODE_CANCEL;
		}

		// Inner blocks are valid, we found a curly bracket, check the end
		else if( result == P_CODE_YES
				&& line != null
				&& line.trim().startsWith( String.valueOf( C_CURLY_BRACKET ))) {

			line = line.replaceFirst( "\\s*\\}", "" );
			parts = splitFromInlineComment( line );
			if( ! Utils.isEmptyOrWhitespaces( parts[ 0 ])) {
				this.definitionFile.getParsingErrors().add( new ModelError( ErrorCode.P_C_C_BRACKET_EXTRA_CHARACTERS, this.currentLineNumber ));
				result = P_CODE_CANCEL;
			}

			holder.setClosingInlineComment( parts[ 1 ]);
		}

		// The closing bracket is missing
		else if( result == P_CODE_YES ) {
			this.definitionFile.getParsingErrors().add( new ModelError( ErrorCode.P_C_C_BRACKET_MISSING, this.currentLineNumber ));
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
			InputStream in = this.definitionFile.getFileLocation().toURL().openStream();
			br = new BufferedReader( new InputStreamReader( in, "UTF-8" ));

			String line;
			while(( line = nextLine( br )) != null ) {

				int code = recognizeBlankLine( line, this.definitionFile.getBlocks());
				if( code == P_CODE_YES )
					continue;

				code = recognizeComment( line, this.definitionFile.getBlocks());
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

				code = recognizeInstanceOf( line, br, null );
				if( code == P_CODE_CANCEL )
					break;
				else if( code == P_CODE_YES )
					continue;

				code = recognizeComponent( line, br );
				if( code == P_CODE_CANCEL )
					break;
				else if( code == P_CODE_NO )
					this.definitionFile.getParsingErrors().add( new ModelError( ErrorCode.P_UNRECOGNIZED_BLOCK, this.currentLineNumber ));
			}

			if( line == null
					&& this.lastLineEndedWithLineBreak )
				this.definitionFile.getBlocks().add( new BlockBlank( this.definitionFile, "" ));

		} finally {
			if( br != null )
				br.close();
		}
	}
}
