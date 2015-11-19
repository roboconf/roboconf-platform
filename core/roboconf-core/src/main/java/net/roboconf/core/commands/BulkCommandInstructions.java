/**
 * Copyright 2015 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.core.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.roboconf.core.ErrorCode;
import net.roboconf.core.model.ParsingError;

/**
 * @author Vincent Zurczak - Linagora
 */
class BulkCommandInstructions extends AbstractCommandInstruction {

	private String instancePath;
	private ChangeStateInstruction changeStateInstruction;


	/**
	 * Constructor.
	 * @param context
	 * @param changeStateInstruction
	 * @param line
	 */
	BulkCommandInstructions( Context context, String instruction, int line ) {
		super( context, instruction, line );

		Matcher m = getPattern().matcher( instruction );
		if( m.matches()) {
			this.syntaxicallyCorrect = true;
			this.instancePath = m.group( 2 ).trim();
			this.changeStateInstruction = ChangeStateInstruction.which( m.group( 1 ).trim());
		}
	}


	/**
	 * @param line a non-null string
	 * @return true if it matches a supported changeStateInstruction, false otherwise
	 */
	public static boolean isBulkInstruction( String line ) {

		Matcher m = getPattern().matcher( line );
		return m.matches()
				&& ChangeStateInstruction.which( m.group( 1 ).trim()) != null;
	}


	/**
	 * @return a pattern to recognize instructions supported by this class
	 */
	private static Pattern getPattern() {
		return Pattern.compile( "([^/]+)(/.*)", Pattern.CASE_INSENSITIVE );
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.core.commands.AbstractCommandInstruction#doValidate()
	 */
	@Override
	public List<ParsingError> doValidate() {

		List<ParsingError> result = new ArrayList<> ();
		if( this.changeStateInstruction == null )
			result.add( error( ErrorCode.CMD_UNRECOGNIZED_INSTRUCTION, "Instruction: " + this.changeStateInstruction ));

		if( ! this.context.instanceExists( this.instancePath ))
			result.add( error( ErrorCode.CMD_NO_MATCHING_INSTANCE, "Instance path: " + this.instancePath ));

		return result;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.core.commands.AbstractCommandInstruction#updateContext()
	 */
	@Override
	public void updateContext() {

		if( this.changeStateInstruction == ChangeStateInstruction.DELETE )
			this.context.instancePathToComponentName.remove( this.instancePath );
	}


	/**
	 * @return the instancePath
	 */
	protected String getInstancePath() {
		return this.instancePath;
	}


	/**
	 * @return the changeStateInstruction
	 */
	protected ChangeStateInstruction getChangeStateInstruction() {
		return this.changeStateInstruction;
	}


	/**
	 * @author Vincent Zurczak - Linagora
	 */
	public static enum ChangeStateInstruction {

		DELETE,
		DEPLOY_AND_START_ALL,
		STOP_ALL,
		UNDEPLOY_ALL;


		/*
		 * (non-Javadoc)
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return super.toString().replace( '_', ' ' ).toLowerCase();
		}


		/**
		 * @param s a string (can be null)
		 * @return null if no changeStateInstruction was recognized, or an changeStateInstruction otherwise
		 */
		public static ChangeStateInstruction which( String s ) {

			ChangeStateInstruction result = null;
			for( ChangeStateInstruction elt : ChangeStateInstruction.values()) {
				if( elt.toString().equalsIgnoreCase( s )) {
					result = elt;
					break;
				}
			}

			return result;
		}
	}
}
