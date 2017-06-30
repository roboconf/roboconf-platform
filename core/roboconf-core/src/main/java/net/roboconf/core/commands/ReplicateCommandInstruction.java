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

import static net.roboconf.core.errors.ErrorDetails.instance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.roboconf.core.dsl.ParsingConstants;
import net.roboconf.core.errors.ErrorCode;
import net.roboconf.core.model.ParsingError;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.utils.Utils;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ReplicateCommandInstruction extends AbstractCommandInstruction {

	static final String PREFIX = "replicate";
	private String newInstanceName, replicatedInstancePath;


	/**
	 * Constructor.
	 * @param context
	 * @param instruction
	 * @param line
	 */
	ReplicateCommandInstruction( Context context, String instruction, int line ) {
		super( context, instruction, line );

		Pattern p = Pattern.compile( PREFIX + "\\s+(/.*)\\s*as\\b(.*)", Pattern.CASE_INSENSITIVE );
		Matcher m = p.matcher( instruction );
		if( m.matches()) {
			this.syntaxicallyCorrect = true;
			this.newInstanceName = m.group( 2 ).trim();
			this.replicatedInstancePath = m.group( 1 ).trim();
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
			result.add( error( ErrorCode.CMD_INVALID_INSTANCE_NAME ));

		if( ! this.context.instanceExists( this.replicatedInstancePath ))
			result.add( error( ErrorCode.CMD_NO_MATCHING_INSTANCE, instance( this.replicatedInstancePath )));
		else if( InstanceHelpers.countInstances( this.replicatedInstancePath ) > 1 )
			result.add( error( ErrorCode.CMD_NOT_A_ROOT_INSTANCE, instance( this.replicatedInstancePath )));

		return result;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.core.commands.AbstractCommandInstruction#updateContext()
	 */
	@Override
	public void updateContext() {

		// Find the path to add
		String copyInstancePath = "/" + this.replicatedInstancePath.replaceFirst( "/[^/]+$", this.newInstanceName );
		Map<String,String> pathToAdd = new HashMap<> ();

		for( Map.Entry<String,String> entry : this.context.instancePathToComponentName.entrySet()) {
			if( ! entry.getKey().startsWith( this.replicatedInstancePath + "/" )
					&& ! entry.getKey().equals( this.replicatedInstancePath ))
				continue;

			String updatedPath = entry.getKey().replace( this.replicatedInstancePath, copyInstancePath );
			pathToAdd.put( updatedPath, entry.getValue());
		}

		// Update them
		this.context.instancePathToComponentName.putAll( pathToAdd );
	}


	/**
	 * @return the newInstanceName
	 */
	public String getNewInstanceName() {
		return this.newInstanceName;
	}


	/**
	 * @return the replicatedInstancePath
	 */
	public String getReplicatedInstancePath() {
		return this.replicatedInstancePath;
	}
}
