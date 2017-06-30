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

import static net.roboconf.core.errors.ErrorDetails.conflicting;
import static net.roboconf.core.errors.ErrorDetails.instance;
import static net.roboconf.core.errors.ErrorDetails.name;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.roboconf.core.dsl.ParsingConstants;
import net.roboconf.core.errors.ErrorCode;
import net.roboconf.core.model.ParsingError;
import net.roboconf.core.utils.Utils;

/**
 * @author Vincent Zurczak - Linagora
 */
public class RenameCommandInstruction extends AbstractCommandInstruction {

	static final String PREFIX = "rename";
	private String newInstanceName, instancePath;


	/**
	 * Constructor.
	 * @param context
	 * @param instruction
	 * @param line
	 */
	RenameCommandInstruction( Context context, String instruction, int line ) {
		super( context, instruction, line );

		Pattern p = Pattern.compile( PREFIX + "\\s+(/.*)\\s*as\\b(.*)", Pattern.CASE_INSENSITIVE );
		Matcher m = p.matcher( instruction );
		if( m.matches()) {
			this.syntaxicallyCorrect = true;
			this.instancePath = m.group( 1 ).trim();
			this.newInstanceName = m.group( 2 ).trim();
		}
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.core.commands.AbstractCommandInstruction#doValidate()
	 */
	@Override
	public List<ParsingError> doValidate() {

		List<ParsingError> result = new ArrayList<> ();
		if( Utils.isEmptyOrWhitespaces( this.newInstanceName ))
			result.add( error( ErrorCode.CMD_MISSING_INSTANCE_NAME ));
		else if( ! this.newInstanceName.matches( ParsingConstants.PATTERN_FLEX_ID ))
			result.add( error( ErrorCode.CMD_INVALID_INSTANCE_NAME, name( this.newInstanceName )));

		if( ! this.context.instanceExists( this.instancePath )) {
			result.add( error( ErrorCode.CMD_NO_MATCHING_INSTANCE, instance( this.instancePath )));

		} else {
			String parentInstancePath = this.instancePath.replaceFirst( "/[^/]+$", "" );
			String siblingPath = parentInstancePath + "/" + this.newInstanceName;
			if( this.context.instanceExists( siblingPath ))
				result.add( error( ErrorCode.CMD_CONFLICTING_INSTANCE_NAME, conflicting( siblingPath )));
		}

		return result;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.core.commands.AbstractCommandInstruction#updateContext()
	 */
	@Override
	public void updateContext() {

		// Find the path to update
		String newInstancePath = "/" + this.instancePath.replaceFirst( "/[^/]+$", this.newInstanceName );

		Map<String,String> copy = new HashMap<>( this.context.instancePathToComponentName );
		for( Map.Entry<String,String> entry : copy.entrySet()) {
			if( ! entry.getKey().startsWith( this.instancePath + "/" )
					&& ! entry.getKey().equals( this.instancePath ))
				continue;

			String updatedPath = entry.getKey().replace( this.instancePath, newInstancePath );
			this.context.instancePathToComponentName.remove( entry.getKey());
			this.context.instancePathToComponentName.put( updatedPath, entry.getValue());
		}
	}


	/**
	 * @return the newInstanceName
	 */
	public String getNewInstanceName() {
		return this.newInstanceName;
	}


	/**
	 * @return the instancePath
	 */
	public String getInstancePath() {
		return this.instancePath;
	}
}
