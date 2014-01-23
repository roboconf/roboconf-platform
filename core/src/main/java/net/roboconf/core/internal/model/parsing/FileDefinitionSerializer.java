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

import java.util.Iterator;

import net.roboconf.core.model.parsing.AbstractPropertiesHolder;
import net.roboconf.core.model.parsing.AbstractRegion;
import net.roboconf.core.model.parsing.Constants;
import net.roboconf.core.model.parsing.FileDefinition;
import net.roboconf.core.model.parsing.RegionBlank;
import net.roboconf.core.model.parsing.RegionComment;
import net.roboconf.core.model.parsing.RegionComponent;
import net.roboconf.core.model.parsing.RegionFacet;
import net.roboconf.core.model.parsing.RegionImport;
import net.roboconf.core.model.parsing.RegionInstanceOf;
import net.roboconf.core.model.parsing.RegionProperty;
import net.roboconf.core.model.validators.ParsingModelValidator;

/**
 * A class to serialize a relations model.
 * @author Vincent Zurczak - Linagora
 */
public class FileDefinitionSerializer {

	private String lineSeparator = System.getProperty( "line.separator" );


	/**
	 * Constructor.
	 */
	public FileDefinitionSerializer() {
		// nothing
	}

	/**
	 * Constructor.
	 * @param lineSeparator the line separator (ignored if null)
	 */
	public FileDefinitionSerializer( String lineSeparator ) {
		if( lineSeparator != null )
			this.lineSeparator = lineSeparator;
	}


	/**
	 * @param definitionFile the relations file
	 * @param writeComments true to write comments
	 * @return a string (never null)
	 */
	public String write( FileDefinition definitionFile, boolean writeComments ) {

		StringBuilder sb = new StringBuilder();
		for( Iterator<AbstractRegion> it = definitionFile.getInstructions().iterator(); it.hasNext(); ) {
			sb.append( write( it.next(), writeComments ));
			if( it.hasNext())
				sb.append( this.lineSeparator );
		}

		return sb.toString();
	}


	/**
	 * @param instr an instruction
	 * @param writeComments true to write comments
	 * @return a string (never null)
	 */
	public String write( AbstractRegion instr, boolean writeComments ) {

		String result;
		switch( instr.getInstructionType()) {
		case AbstractRegion.COMPONENT:
			result = write((RegionComponent) instr, writeComments );
			break;

		case AbstractRegion.FACET:
			result = write((RegionFacet) instr, writeComments );
			break;

		case AbstractRegion.INSTANCEOF:
			result = write((RegionInstanceOf) instr, writeComments, 0 );
			break;

		case AbstractRegion.IMPORT:
			result = write((RegionImport) instr, writeComments );
			break;

		case AbstractRegion.PROPERTY:
			result = write((RegionProperty) instr, writeComments );
			break;

		case AbstractRegion.COMMENT:
			result = write((RegionComment) instr, writeComments );
			break;

		case AbstractRegion.BLANK:
			result = write((RegionBlank) instr, writeComments );
			break;

		default:
			result = null;
			break;
		}

		return result;
	}


	/**
	 * @param instr the instruction
	 * @param writeComments true to write comments
	 * @return a string (never null)
	 */
	public String write( RegionImport instr, boolean writeComments ) {

		StringBuilder sb = new StringBuilder();
		sb.append( "import " );
		sb.append( instr.getUri());
		sb.append( ";" );

		if( writeComments
				&& instr.getInlineComment() != null )
			sb.append( instr.getInlineComment());

		return sb.toString();
	}


	/**
	 * @param instr the instruction
	 * @param writeComments true to write comments
	 * @return a string (never null)
	 */
	public String write( RegionComponent instr, boolean writeComments ) {
		return writePropertiesHolder( instr, writeComments, 0 );
	}


	/**
	 * @param instr the instruction
	 * @param writeComments true to write comments
	 * @return a string (never null)
	 */
	public String write( RegionFacet instr, boolean writeComments ) {
		return Constants.KEYWORD_FACET + " " + writePropertiesHolder( instr, writeComments, 0 );
	}


	/**
	 * @param instr the instruction
	 * @param writeComments true to write comments
	 * @param indentationLevel the indentation level
	 * @return a string (never null)
	 */
	public String write( RegionInstanceOf instr, boolean writeComments, int indentationLevel ) {

		StringBuilder sb = new StringBuilder();
		indent( sb, indentationLevel );
		sb.append( Constants.KEYWORD_INSTANCE_OF );
		sb.append( " " );
		sb.append( writePropertiesHolder( instr, writeComments, indentationLevel ));

		return sb.toString();
	}


	/**
	 * @param instr the instruction
	 * @param writeComments true to write comments
	 * @return a string (never null)
	 */
	public String write( RegionBlank instr, boolean writeComments ) {

		StringBuilder sb = new StringBuilder();
		if( writeComments ) {
			// Invalid blank sections can be "repaired" even if they are invalid.
			// Here, we replace it by a line break.
			// The error is signaled as a warning in the validation.
			// Useful if a blank section was built programmatically.
			if( ParsingModelValidator.validate( instr ).isEmpty())
				sb.append( instr.getContent());
			else
				sb.append( this.lineSeparator );
		}

		return sb.toString();
	}


	/**
	 * @param instr the instruction
	 * @param writeComments true to write comments
	 * @return a string (never null)
	 */
	public String write( RegionProperty instr, boolean writeComments ) {

		StringBuilder sb = new StringBuilder();
		sb.append( instr.getName());
		sb.append( ": " );
		sb.append( instr.getValue());
		sb.append( ";" );

		if( writeComments
				&& instr.getInlineComment() != null )
			sb.append( instr.getInlineComment());

		return sb.toString();
	}


	/**
	 * @param instr the instruction
	 * @param writeComments true to write comments
	 * @return a string (never null)
	 */
	public String write( RegionComment instr, boolean writeComments ) {

		StringBuilder sb = new StringBuilder();
		if( writeComments ) {
			// Invalid comment sections can be "repaired" even if they are invalid.
			// Here, we insert a comment delimiter where needed.
			// The error is signaled as a warning in the validation.
			// Useful if a comment was built programmatically.
			if( ParsingModelValidator.validate( instr ).isEmpty()) {
				sb.append( instr.getContent());

			} else {
				for( String s : instr.getContent().split( "\n" )) {
					if( s.trim().startsWith( Constants.COMMENT_DELIMITER ))
						sb.append( "# " );

					sb.append( s );
					sb.append( "\n" );
				}
			}
		}

		return sb.toString();
	}


	/**
	 * @param holder the instruction
	 * @param writeComments true to write comments
	 * @param indentationLevel the indentation level
	 * @return a string (never null)
	 */
	private String writePropertiesHolder( AbstractPropertiesHolder holder, boolean writeComments, int indentationLevel ) {

		StringBuilder sb = new StringBuilder();
		sb.append( holder.getName());
		sb.append( " {" );

		if( writeComments
				&& holder.getInlineComment() != null )
			sb.append( holder.getInlineComment());

		for( AbstractRegion instr : holder.getInternalInstructions()) {
			sb.append( this.lineSeparator );
			if( instr.getInstructionType() == AbstractRegion.INSTANCEOF ) {
				sb.append( write((RegionInstanceOf) instr, writeComments, indentationLevel + 1 ));

			} else {
				if( instr.getInstructionType() == AbstractRegion.PROPERTY )
					indent( sb, indentationLevel + 1 );

				sb.append( write( instr, writeComments ));
			}
		}

		sb.append( this.lineSeparator );
		indent( sb, indentationLevel );
		sb.append( "}" );
		if( writeComments
				&& holder.getClosingInlineComment() != null )
			sb.append( holder.getClosingInlineComment());

		return sb.toString();
	}


	/**
	 * @param sb
	 * @param indentationLevel
	 */
	private void indent( StringBuilder sb, int indentationLevel ) {
		for( int i=0; i<indentationLevel; i++ )
			sb.append( "\t" );
	}
}
