/**
 * Copyright 2015-2016 Linagora, Université Joseph Fourier, Floralis
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
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.roboconf.core.ErrorCode;
import net.roboconf.core.model.ParsingError;
import net.roboconf.core.model.beans.AbstractApplication;

/**
 * @author Vincent Zurczak - Linagora
 */
public abstract class AbstractCommandInstruction {

	private static final Pattern VAR_PATTERN = Pattern.compile( "(\\$\\(.*\\))" );

	protected final String instruction;
	protected final Context context;
	protected final int line;
	protected boolean syntaxicallyCorrect = false;


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
			errors.add( error( ErrorCode.CMD_INVALID_SYNTAX, "Instruction: " + this.instruction ));

		Matcher m = VAR_PATTERN.matcher( this.instruction );
		while( m.find()) {
			if( ! getVariablesToIgnore().contains( m.group( 1 )))
				errors.add( error( ErrorCode.CMD_UNRESOLVED_VARIABLE, "Variable: " + m.group( 1 )));
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
	 * @return a non-null list of variables to ignore
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
	protected ParsingError error( ErrorCode errorCode, String details ) {
		return new ParsingError( errorCode, this.context.getCommandFile(), this.line, details );
	}


	/**
	 * A shortcut method to create a parsing error with relevant information.
	 * @param errorCode an error code
	 * @return a new parsing error
	 */
	protected ParsingError error( ErrorCode errorCode ) {
		return error( errorCode, null );
	}
}
