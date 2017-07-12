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

import static net.roboconf.core.errors.ErrorDetails.value;
import static net.roboconf.core.errors.ErrorDetails.variable;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.roboconf.core.errors.ErrorCode;
import net.roboconf.core.model.ParsingError;
import net.roboconf.core.utils.Utils;

/**
 * @author Vincent Zurczak - Linagora
 */
public class DefineVariableCommandInstruction extends AbstractCommandInstruction {

	static final String PREFIX = "define";

	public static final String MILLI_TIME = "$(MILLI_TIME)";
	public static final String NANO_TIME = "$(NANO_TIME)";
	public static final String FORMATTED_TIME_PREFIX = "$(FORMATTED_TIME ";
	public static final String EXISTING_INDEX_PREFIX = "$(EXISTING_INDEX ";
	public static final String RANDOM_UUID = "$(UUID)";
	public static final String SMART_INDEX = "$(SMART_INDEX)";

	static final String FAKE_COMPONENT_NAME = "@fake-component@";
	private static final String EXISTING_INDEX_PATTERN =
			"(.*)" + Pattern.quote( EXISTING_INDEX_PREFIX ) + "\\s*((MIN)|(MAX))\\s*([<>]\\s*\\d+)?\\s*\\)(.*)";

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

		// Basic checks
		if( Utils.isEmptyOrWhitespaces( this.key ))
			result.add( error( ErrorCode.CMD_EMPTY_VARIABLE_NAME ));

		// Formatted time must be processed apart.
		// Such a variable must be single on its line.
		else if( this.value.startsWith( FORMATTED_TIME_PREFIX )
				&& this.value.endsWith( ")" )) {

			String datePattern = extractDatePattern( this.value );
			if( datePattern.contains( "$(" )) {
				result.add( error( ErrorCode.CMD_NO_MIX_FOR_PATTERNS, variable( this.key )));

			} else try {
				new SimpleDateFormat( datePattern );

			} catch( Exception e ) {
				result.add( error( ErrorCode.CMD_INVALID_DATE_PATTERN, value( datePattern )));
			}
		}

		// A formatted time in the middle of something else?
		else if( this.value.contains( FORMATTED_TIME_PREFIX )) {
			result.add( error( ErrorCode.CMD_NO_MIX_FOR_PATTERNS, variable( this.value )));
		}

		// The index variable is very strict
		Pattern indexPattern = Pattern.compile( EXISTING_INDEX_PATTERN );
		if( this.value.contains( EXISTING_INDEX_PREFIX )
				&& ! indexPattern.matcher( this.value ).matches()) {
			result.add( error( ErrorCode.CMD_INVALID_INDEX_PATTERN, variable( this.value )));
		}

		// Validate instance existence at the end, and only if no error was found before
		if( result.isEmpty()
				&& this.instancePath != null
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
		if( newValue != null ) {
			this.context.variables.put( this.key, newValue );
		}

		// When not resolved, we disable it.
		// We also replace it by a mock value so that we can perform
		// validation. It implies populating the context.
		else {
			setDisabled( true );
			this.context.disabledVariables.add( this.key );
			this.context.variables.put( this.key, "fake" );
			String path = "/fake";
			if( this.instancePath != null )
				path = this.instancePath + path;

			this.context.instancePathToComponentName.put( path, FAKE_COMPONENT_NAME );
		}
	}


	/**
	 * Gets the (native) variables to ignore.
	 * @return a non-null list
	 */
	@Override
	protected List<String> getVariablesToIgnore() {

		List<String> variablesToIgnore = new ArrayList<> ();
		variablesToIgnore.add( "^" + Pattern.quote( MILLI_TIME ) + "$" );
		variablesToIgnore.add( "^" + Pattern.quote( NANO_TIME ) + "$" );
		variablesToIgnore.add( "^" + Pattern.quote( SMART_INDEX ) + "$" );
		variablesToIgnore.add( "^" + Pattern.quote( RANDOM_UUID ) + "$" );

		for( String var : this.context.variables.keySet())
			variablesToIgnore.add( "^\\$\\(" + Pattern.quote( var ) + "\\)$" );

		variablesToIgnore.add( "^" + Pattern.quote( FORMATTED_TIME_PREFIX ) + "[^)]*\\)$" );
		variablesToIgnore.add( "^" + Pattern.quote( EXISTING_INDEX_PREFIX ) + "[^)]*\\)$" );
		return variablesToIgnore;
	}


	/**
	 * Extracts the date pattern for a "formatted time" variable.
	 * @param group a formatted time
	 * @return a non-null string
	 */
	static String extractDatePattern( String group ) {
		return group.substring( FORMATTED_TIME_PREFIX.length(), group.length() - 2 ).trim();
	}


	/**
	 * Replaces variables in instance names.
	 * @param value an expression that can reference other variables (not null)
	 * @param context a context (not null)
	 * @param parentInstancePath the parent instance path (may be null)
	 * @param context a non-null map with context information
	 * @return the updated value, or null if an <code>$(existing_index ...)</code> variable failed to be resolved
	 */
	static String replaceVariables( String value, Context context, String parentInstancePath ) {

		long nanoTime = System.nanoTime();
		long milliTime = TimeUnit.MILLISECONDS.convert( nanoTime, TimeUnit.NANOSECONDS );

		// Simple replacements
		value = value.replace( NANO_TIME, String.valueOf( nanoTime ));
		value = value.replace( MILLI_TIME, String.valueOf( milliTime ));
		value = value.replace( RANDOM_UUID, UUID.randomUUID().toString());

		// A little more complex
		if( value.startsWith( FORMATTED_TIME_PREFIX )
				&& value.endsWith( ")" )) {

			String datePattern = extractDatePattern( value );
			SimpleDateFormat sdf = new SimpleDateFormat( datePattern );

			// We replace the whole value as such variables were validated and span over a entire line
			value = sdf.format( new Date( milliTime ));
		}

		// Even more complex
		int smartCpt = 0;
		if( value.contains( SMART_INDEX )) {
			for( int cpt = 1; smartCpt == 0; cpt++ ) {
				String testName = value.replace( SMART_INDEX, String.valueOf( cpt ));
				String testPath = "/" + testName;
				if( parentInstancePath != null )
					testPath = parentInstancePath + testPath;

				if( ! context.instanceExists( testPath )) {
					smartCpt = cpt;
				}
			}
		}

		value = value.replace( SMART_INDEX, String.valueOf( smartCpt ));

		// To perform in last: deal with existing indexes
		boolean disabled = false;
		Pattern fullPattern = Pattern.compile( EXISTING_INDEX_PATTERN );
		Matcher m = fullPattern.matcher( value );
		if( value.contains( EXISTING_INDEX_PREFIX )
				&& m.matches()) {

			String minOrMax = m.group( 2 );
			String prefix = m.group( 1 );
			String suffix = m.group( 6 );
			String bound = m.group( 5 );
			String instanceNamePattern = "/" + prefix + "(\\d+)" + suffix;
			if( parentInstancePath != null )
				instanceNamePattern = parentInstancePath + instanceNamePattern;

			// Find the index
			int cpt = findIndex( instanceNamePattern, context, bound, "MIN".equals( minOrMax ));

			// If cpt is negative, do not replace anything, nothing matches the requirements.
			// We just return null for the variable's value.
			if( cpt < 0 )
				disabled = true;
			else
				value = m.replaceAll( "$1" + String.valueOf( cpt ) + "$6" );
		}

		// Disabled variable => return null
		if( disabled )
			value = null;

		// Inject other variables, if necessary.
		// Disabled variables are contagious, this is managed during the parsing.
		else
			value = CommandsParser.injectContextVariables( value, context.variables );

		return value;
	}


	/**
	 * @param instanceNamePattern
	 * @param context
	 * @param bound
	 * @param findMinimum true to get the minimum value, false for the maximum
	 * @return -1 if no matching instance was found, a positive integer otherwise
	 */
	private static int findIndex( String instanceNamePattern, Context context, String bound, boolean findMinimum ) {

		// Find all the matching indexes?
		Pattern pattern = Pattern.compile( instanceNamePattern );
		SortedSet<Integer> foundIndexes = new TreeSet<> ();
		for( String instancePath : context.instancePathToComponentName.keySet()) {
			Matcher m = pattern.matcher( instancePath );
			if( ! m .find())
				continue;

			int index = Integer.parseInt( m.group( 1 ));
			foundIndexes.add( index );
		}

		// Exclude indexes that are out of bound
		if( bound != null ) {
			bound = bound.trim();
			int min = 0, max = Integer.MAX_VALUE;
			int value = Integer.parseInt( bound.substring( 1 ).trim());
			if( '<' == bound.charAt( 0 ))
				max = value;
			else
				min = value;

			Set<Integer> toRemove = new HashSet<> ();
			for( Integer index : foundIndexes ) {
				if( index <= min || index >= max )
					toRemove.add( index );
			}

			foundIndexes.removeAll( toRemove );
		}

		// Eventually, decide whether we want the tail or the head
		int result;
		if( foundIndexes.isEmpty())
			result = -1;
		else if( findMinimum )
			result = foundIndexes.first();
		else
			result = foundIndexes.last();

		return result;
	}
}
