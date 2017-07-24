/**
 * Copyright 2015-2017 Linagora, Université Joseph Fourier, Floralis
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

import static net.roboconf.core.errors.ErrorDetails.instruction;
import static net.roboconf.core.errors.ErrorDetails.variable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.roboconf.core.errors.ErrorCode;
import net.roboconf.core.errors.ErrorDetails;
import net.roboconf.core.model.ParsingError;
import net.roboconf.core.model.beans.AbstractApplication;

/**
 * @author Vincent Zurczak - Linagora
 */
public abstract class AbstractCommandInstruction {

	private static final Pattern VAR_PATTERN = Pattern.compile( "(\\$\\([^)]*\\))" );

	/**
	 * The instruction's text.
	 */
	protected final String instruction;

	/**
	 * The context to evaluate the correctness of the instruction.
	 */
	protected final Context context;

	/**
	 * The corresponding line in the source file.
	 */
	protected final int line;

	/**
	 * Whether the instruction is syntaxically correct.
	 */
	protected boolean syntaxicallyCorrect = false;

	/**
	 * Whether the instruction can be executed or not.
	 * <p>
	 * An instruction is disabled if and only if it uses a variable
	 * that was not resolved. Only query variables can fail to be
	 * resolved.
	 * </p>
	 */
	protected boolean disabled = false;



	/**
	 * Constructor.
	 * @param context the context
	 * @param instruction the instruction
	 * @param line the line number
	 */
	public AbstractCommandInstruction( Context context, String instruction, int line ) {
		this.instruction = instruction;
		this.context = context;
		this.line = line;
	}


	/**
	 * @return a non-null list of errors, possibly empty
	 */
	public final List<ParsingError> validate() {

		List<ParsingError> errors = new ArrayList<> ();
		if( ! this.syntaxicallyCorrect )
			errors.add( error( ErrorCode.CMD_INVALID_SYNTAX, instruction( this.instruction )));

		Matcher m = VAR_PATTERN.matcher( this.instruction );
		while( m.find()) {
			boolean found = false;
			String varName = m.group( 1 );
			for( String variablePattern : getVariablesToIgnore()) {
				if( varName.matches( variablePattern )) {
					found = true;
					break;
				}
			}

			if( ! found )
				errors.add( error( ErrorCode.CMD_UNRESOLVED_VARIABLE, variable( varName )));
		}

		if( errors.isEmpty())
			errors.addAll( doValidate());

		return errors;
	}


	/**
	 * @return the (abstract) application this instruction is associated with
	 */
	public AbstractApplication getApplication() {
		return this.context.getApp();
	}


	/**
	 * Validates specific items related to sub-classes.
	 * @return a non-null list of errors
	 */
	protected abstract List<ParsingError> doValidate();


	/**
	 * @return a non-null list of patterns describing variables to ignore
	 */
	protected List<String> getVariablesToIgnore() {
		return Collections.emptyList();
	}


	/**
	 * Updates the context (to be invoked after {@link #validate()}).
	 */
	public void updateContext() {
		// nothing
	}


	/**
	 * A shortcut method to create a parsing error with relevant information.
	 * @param errorCode an error code
	 * @param details (can be null)
	 * @return a new parsing error
	 */
	protected ParsingError error( ErrorCode errorCode, ErrorDetails... details ) {
		return new ParsingError(
				errorCode,
				this.context.getCommandFile(),
				this.line,
				details );
	}


	/**
	 * A shortcut method to create a parsing error with relevant information.
	 * @param errorCode an error code
	 * @return a new parsing error
	 */
	protected ParsingError error( ErrorCode errorCode ) {
		return error( errorCode, (ErrorDetails[]) null );
	}


	/**
	 * @return the disabled
	 */
	public boolean isDisabled() {
		return this.disabled;
	}


	/**
	 * @param disabled the disabled to set
	 */
	public void setDisabled( boolean disabled ) {
		this.disabled = disabled;
	}
}
