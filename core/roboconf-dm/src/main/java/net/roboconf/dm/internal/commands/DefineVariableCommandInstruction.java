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

package net.roboconf.dm.internal.commands;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.roboconf.core.ErrorCode;
import net.roboconf.core.RoboconfError;
import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.exceptions.CommandException;

/**
 * @author Vincent Zurczak - Linagora
 */
class DefineVariableCommandInstruction implements ICommandInstruction {

	static final String PREFIX = "define";

	static final String MILLI_TIME = "$(MILLI_TIME)";
	static final String NANO_TIME = "$(NANO_TIME)";
	static final String RANDOM_UUID = "$(UUID)";
	static final String SMART_INDEX = "$(INDEX)";

	private final Map<String,String> context;
	private final ManagedApplication ma;
	private String key, value, instancePath;
	private Instance instance;


	/**
	 * Constructor.
	 * @param instruction
	 * @param context
	 */
	DefineVariableCommandInstruction( ManagedApplication ma, String instruction, Map<String,String> context ) {
		this.context = context;
		this.ma = ma;

		// We could use a look-around in the regexp, but that would be complicated to maintain.
		// Instead, we will process it as two patterns.
		Pattern p = Pattern.compile( PREFIX + "\\s+([^=]+)\\s*=\\s*(.*)", Pattern.CASE_INSENSITIVE );
		Matcher m = p.matcher( instruction );
		if( m.matches()) {
			this.key = m.group( 1 ).trim();
			this.value = m.group( 2 ).trim();

			// \\s+under\\s+(/.*)
			Pattern subP = Pattern.compile( "(.*)\\s+under\\s+(.*)", Pattern.CASE_INSENSITIVE );
			if(( m = subP.matcher( this.value )).matches()) {
				this.value = m.group( 1 ).trim();
				this.instancePath = m.group( 2 ).trim();
				this.instance = InstanceHelpers.findInstanceByPath( ma.getApplication(), this.instancePath );
			}
		}
	}


	@Override
	public RoboconfError validate() {

		RoboconfError result = null;
		if( Utils.isEmptyOrWhitespaces( this.key ))
			result = new RoboconfError( ErrorCode.EXEC_CMD_EMPTY_VARIABLE_NAME );
		else if( this.instancePath != null && this.instance == null )
			result = new RoboconfError( ErrorCode.EXEC_CMD_NO_MATCHING_INSTANCE );

		return result;
	}


	@Override
	public void execute() throws CommandException {
		String newValue = replaceVariables( this.value, this.ma.getApplication(), this.instance, this.context );
		this.context.put( this.key, newValue );
	}


	/**
	 * Replaces variables in instance names.
	 * @param name an instance name (not null)
	 * @param app an application (not null)
	 * @param parentInstance the parent instance
	 * @param context a non-null map with context information
	 * @return the updated name
	 */
	static String replaceVariables( String name, Application app, Instance parentInstance, Map<String,String> context ) {

		long nanoTime = System.nanoTime();
		long milliTime = TimeUnit.MILLISECONDS.convert( nanoTime, TimeUnit.NANOSECONDS );

		// Simple replacements
		name = name.replace( NANO_TIME, String.valueOf( nanoTime ));
		name = name.replace( MILLI_TIME, String.valueOf( milliTime ));
		name = name.replace( RANDOM_UUID, UUID.randomUUID().toString());

		// A little more complex
		int smartCpt = 0;
		if( name.contains( SMART_INDEX ) && parentInstance != null ) {
			for( int cpt = 1; smartCpt == 0; cpt++ ) {
				String testName = name.replace( SMART_INDEX, String.valueOf( cpt ));
				if( ! InstanceHelpers.hasChildWithThisName( app, parentInstance, testName )) {
					smartCpt = cpt;
				}
			}
		}

		name = name.replace( SMART_INDEX, String.valueOf( smartCpt ));

		// Inject other variables
		for( Map.Entry<String,String> entry : context.entrySet())
			name = name.replace( "$(" + entry.getKey() + ")", entry.getValue());

		return name;
	}
}
