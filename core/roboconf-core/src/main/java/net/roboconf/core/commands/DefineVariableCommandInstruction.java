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
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.roboconf.core.ErrorCode;
import net.roboconf.core.model.ParsingError;
import net.roboconf.core.utils.Utils;

/**
 * @author Vincent Zurczak - Linagora
 */
public class DefineVariableCommandInstruction extends AbstractCommandInstruction {

	static final String PREFIX = "define";

	static final String MILLI_TIME = "$(MILLI_TIME)";
	static final String NANO_TIME = "$(NANO_TIME)";
	static final String RANDOM_UUID = "$(UUID)";
	static final String SMART_INDEX = "$(SMART_INDEX)";

	private String key, value, instancePath;


	/**
	 * Constructor.
	 * @param context
	 * @param instruction
	 * @param line
	 */
	DefineVariableCommandInstruction( Context context, String instruction, int line ) {
		super( context, instruction, line );

		// We could use a look-around in the regexp, but that would be complicated to maintain.
		// Instead, we will process it as two patterns.
		Pattern p = Pattern.compile( PREFIX + "\\s+([^=]*)\\s*=\\s*(.*)", Pattern.CASE_INSENSITIVE );
		Matcher m = p.matcher( instruction );
		if( m.matches()) {
			this.syntaxicallyCorrect = true;
			this.key = m.group( 1 ).trim();
			this.value = m.group( 2 ).trim();

			// \\s+under\\s+(/.*)
			Pattern subP = Pattern.compile( "(.*)\\s+under\\s+(.*)", Pattern.CASE_INSENSITIVE );
			if(( m = subP.matcher( this.value )).matches()) {
				this.value = m.group( 1 ).trim();
				this.instancePath = m.group( 2 ).trim();
			}
		}
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.core.commands.AbstractCommandInstruction#doValidate()
	 */
	@Override
	public List<ParsingError> doValidate() {

		List<ParsingError> result = new ArrayList<> ();
		if( Utils.isEmptyOrWhitespaces( this.key ))
			result.add( error( ErrorCode.CMD_EMPTY_VARIABLE_NAME ));
		else if( this.instancePath != null
				&& ! this.context.instanceExists( this.instancePath ))
			result.add( error( ErrorCode.CMD_NO_MATCHING_INSTANCE ));

		return result;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.core.commands.AbstractCommandInstruction#updateContext()
	 */
	@Override
	public void updateContext() {
		String newValue = replaceVariables( this.value, this.context, this.instancePath );
		this.context.variables.put( this.key, newValue );
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.core.commands.AbstractCommandInstruction#getVariablesToIgnore()
	 */
	@Override
	protected List<String> getVariablesToIgnore() {

		List<String> variablesToIgnore = new ArrayList<> ();
		variablesToIgnore.add( MILLI_TIME );
		variablesToIgnore.add( NANO_TIME );
		variablesToIgnore.add( SMART_INDEX );
		variablesToIgnore.add( RANDOM_UUID );

		for( String var : this.context.variables.keySet() )
			variablesToIgnore.add( "$(" + var + ")" );

		return variablesToIgnore;
	}


	/**
	 * Replaces variables in instance names.
	 * @param name an instance name (not null)
	 * @param context a context (not null)
	 * @param parentInstancePath the parent instance path (may be null)
	 * @param context a non-null map with context information
	 * @return the updated name
	 */
	static String replaceVariables( String name, Context context, String parentInstancePath ) {

		long nanoTime = System.nanoTime();
		long milliTime = TimeUnit.MILLISECONDS.convert( nanoTime, TimeUnit.NANOSECONDS );

		// Simple replacements
		name = name.replace( NANO_TIME, String.valueOf( nanoTime ));
		name = name.replace( MILLI_TIME, String.valueOf( milliTime ));
		name = name.replace( RANDOM_UUID, UUID.randomUUID().toString());

		// A little more complex
		int smartCpt = 0;
		if( name.contains( SMART_INDEX )) {
			for( int cpt = 1; smartCpt == 0; cpt++ ) {
				String testName = name.replace( SMART_INDEX, String.valueOf( cpt ));
				String testPath = "/" + testName;
				if( parentInstancePath != null )
					testPath = parentInstancePath + testPath;

				if( ! context.instanceExists( testPath )) {
					smartCpt = cpt;
				}
			}
		}

		name = name.replace( SMART_INDEX, String.valueOf( smartCpt ));

		// Inject other variables, if necessary
		name = CommandsParser.injectContextVariables( name, context.variables );

		return name;
	}
}
