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

import static net.roboconf.core.errors.ErrorDetails.component;
import static net.roboconf.core.errors.ErrorDetails.instance;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.roboconf.core.dsl.ParsingConstants;
import net.roboconf.core.errors.ErrorCode;
import net.roboconf.core.model.ParsingError;
import net.roboconf.core.model.beans.Component;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.helpers.ComponentHelpers;
import net.roboconf.core.utils.Utils;

/**
 * @author Vincent Zurczak - Linagora
 */
public class CreateInstanceCommandInstruction extends AbstractCommandInstruction {

	static final String PREFIX = "create";

	private String instanceName, componentName, parentInstancePath;
	private Component component;


	/**
	 * Constructor.
	 * @param context
	 * @param instruction
	 * @param line
	 */
	CreateInstanceCommandInstruction( Context context, String instruction, int line ) {
		super( context, instruction, line );

		// We could use a look-around in the regexp, but that would be complicated to maintain.
		// Instead, we will process it as two patterns.
		Pattern p = Pattern.compile( PREFIX + "\\s+(.*)\\s*as\\b(.*)", Pattern.CASE_INSENSITIVE );
		Matcher m = p.matcher( instruction );
		if( m.matches()) {
			this.syntaxicallyCorrect = true;
			this.componentName = m.group( 1 ).trim();
			this.component = ComponentHelpers.findComponent( context.getApp(), this.componentName );
			this.instanceName = m.group( 2 ).trim();

			Pattern subP = Pattern.compile( "(.*)\\s+under\\s+(.*)", Pattern.CASE_INSENSITIVE );
			if(( m = subP.matcher( this.instanceName )).matches()) {
				this.instanceName = m.group( 1 ).trim();
				this.parentInstancePath = m.group( 2 ).trim();
			}
		}
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.core.commands.AbstractCommandInstruction#doValidate()
	 */
	@Override
	public List<ParsingError> doValidate() {

		// Component checks
		List<ParsingError> result = new ArrayList<> ();
		if( Utils.isEmptyOrWhitespaces( this.componentName ))
			result.add( error( ErrorCode.CMD_MISSING_COMPONENT_NAME ));

		else if( this.component == null )
			result.add( error( ErrorCode.CMD_INEXISTING_COMPONENT, component( this.componentName )));

		else if( ! this.component.getAncestors().isEmpty()) {

			Instance resolvedParentInstance;
			if( this.parentInstancePath == null )
				result.add( error( ErrorCode.CMD_MISSING_PARENT_INSTANCE ));
			else if(( resolvedParentInstance = this.context.resolveInstance( this.parentInstancePath )) == null )
				result.add( error( ErrorCode.CMD_NO_MATCHING_INSTANCE, instance( this.parentInstancePath )));
			else if( ! this.component.getAncestors().contains( resolvedParentInstance.getComponent()))
				result.add( error( ErrorCode.CMD_NOT_AN_ACCEPTABLE_PARENT, component( this.componentName )));

		} else if( this.parentInstancePath != null ) {
			result.add( error( ErrorCode.CMD_CANNOT_HAVE_ANY_PARENT, component( this.componentName )));
		}

		// Instance stuff
		if( Utils.isEmptyOrWhitespaces( this.instanceName ))
			result.add( error( ErrorCode.CMD_MISSING_INSTANCE_NAME ));
		else if( ! this.instanceName.matches( ParsingConstants.PATTERN_FLEX_ID ))
			result.add( error( ErrorCode.CMD_INVALID_INSTANCE_NAME, instance( this.instanceName )));

		String newInstancePath = (this.parentInstancePath == null ? "" : this.parentInstancePath) + "/" + this.instanceName;
		if( this.context.instancePathToComponentName.containsKey( newInstancePath ))
			result.add( error( ErrorCode.CMD_CONFLICTING_INSTANCE_NAME ));

		return result;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.core.commands.AbstractCommandInstruction#updateContext()
	 */
	@Override
	public void updateContext() {

		String newInstancePath = (this.parentInstancePath == null ? "" : this.parentInstancePath) + "/" + this.instanceName;
		this.context.instancePathToComponentName.put( newInstancePath, this.componentName );
	}


	/**
	 * @return the instanceName
	 */
	public String getInstanceName() {
		return this.instanceName;
	}


	/**
	 * @return the parentInstancePath
	 */
	public String getParentInstancePath() {
		return this.parentInstancePath;
	}


	/**
	 * @return the component
	 */
	public Component getComponent() {
		return this.component;
	}
}
