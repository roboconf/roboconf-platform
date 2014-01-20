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
import net.roboconf.core.model.validators.ParsingModelValidator;

/**
 * A class to serialize a relations model.
 * @author Vincent Zurczak - Linagora
 */
public class RelationsSerializer {

	private String lineSeparator = System.getProperty( "line.separator" );


	/**
	 * Constructor.
	 */
	public RelationsSerializer() {
		// nothing
	}

	/**
	 * Constructor.
	 * @param lineSeparator the line separator (ignored if null)
	 */
	public RelationsSerializer( String lineSeparator ) {
		if( lineSeparator != null )
			this.lineSeparator = lineSeparator;
	}


	/**
	 * @param fileRelations the relations file
	 * @param writeComments true to write comments
	 * @return a string (never null)
	 */
	public String write( FileRelations fileRelations, boolean writeComments ) {

		StringBuilder sb = new StringBuilder();
		for( Iterator<AbstractInstruction> it = fileRelations.getInstructions().iterator(); it.hasNext(); ) {
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
	public String write( AbstractInstruction instr, boolean writeComments ) {

		String result;
		switch( instr.getInstructionType()) {
		case AbstractInstruction.COMPONENT:
			result = write((RelationComponent) instr, writeComments );
			break;

		case AbstractInstruction.FACET:
			result = write((RelationFacet) instr, writeComments );
			break;

		case AbstractInstruction.IMPORT:
			result = write((RelationImport) instr, writeComments );
			break;

		case AbstractInstruction.PROPERTY:
			result = write((RelationProperty) instr, writeComments );
			break;

		case AbstractInstruction.COMMENT:
			result = write((RelationComment) instr, writeComments );
			break;

		case AbstractInstruction.BLANK:
			result = write((RelationBlank) instr, writeComments );
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
	public String write( RelationImport instr, boolean writeComments ) {

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
	public String write( RelationComponent instr, boolean writeComments ) {
		return writePropertiesHolder( instr, writeComments );
	}


	/**
	 * @param instr the instruction
	 * @param writeComments true to write comments
	 * @return a string (never null)
	 */
	public String write( RelationFacet instr, boolean writeComments ) {
		return Constants.KEYWORD_FACET + " " + writePropertiesHolder( instr, writeComments );
	}


	/**
	 * @param instr the instruction
	 * @param writeComments true to write comments
	 * @return a string (never null)
	 */
	public String write( RelationBlank instr, boolean writeComments ) {

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
	public String write( RelationProperty instr, boolean writeComments ) {

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
	public String write( RelationComment instr, boolean writeComments ) {

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
	 * @return a string (never null)
	 */
	private String writePropertiesHolder( AbstractPropertiesHolder holder, boolean writeComments ) {

		StringBuilder sb = new StringBuilder();
		sb.append( holder.getName());
		sb.append( " {" );

		if( writeComments
				&& holder.getInlineComment() != null )
			sb.append( holder.getInlineComment());

		for( AbstractInstruction instr : holder.getInternalInstructions()) {
			sb.append( this.lineSeparator );
			if( instr.getInstructionType() == AbstractInstruction.PROPERTY )
				sb.append( "\t" );

			sb.append( write( instr, writeComments ));
		}

		sb.append( this.lineSeparator );
		sb.append( "}" );
		if( writeComments
				&& holder.getClosingInlineComment() != null )
			sb.append( holder.getClosingInlineComment());

		return sb.toString();
	}
}
