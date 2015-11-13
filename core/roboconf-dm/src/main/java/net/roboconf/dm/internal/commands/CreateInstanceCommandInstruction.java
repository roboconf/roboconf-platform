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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.roboconf.core.ErrorCode;
import net.roboconf.core.RoboconfError;
import net.roboconf.core.dsl.ParsingConstants;
import net.roboconf.core.model.beans.Component;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.helpers.ComponentHelpers;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.management.exceptions.CommandException;

/**
 * @author Vincent Zurczak - Linagora
 */
class CreateInstanceCommandInstruction implements ICommandInstruction {

	static final String PREFIX = "create";

	private final Manager manager;
	private final ManagedApplication ma;

	private String instanceName, componentName;
	private Instance parentInstance;
	private Component component;


	/**
	 * Constructor.
	 * @param instruction
	 * @param ma
	 * @param manager
	 */
	CreateInstanceCommandInstruction( ManagedApplication ma, Manager manager, String instruction ) {
		this.manager = manager;
		this.ma = ma;

		// We could use a look-around in the regexp, but that would be complicated to maintain.
		// Instead, we will process it as two patterns.
		Pattern p = Pattern.compile( PREFIX + "\\s+(.*)\\s+as\\b(.*)", Pattern.CASE_INSENSITIVE );
		Matcher m = p.matcher( instruction );
		if( m.matches()) {
			this.componentName = m.group( 1 ).trim();
			this.component = ComponentHelpers.findComponent( this.ma.getApplication(), this.componentName );
			this.instanceName = m.group( 2 ).trim();

			Pattern subP = Pattern.compile( "(.*)\\s+under\\s+(.*)", Pattern.CASE_INSENSITIVE );
			if(( m = subP.matcher( this.instanceName )).matches()) {
				this.instanceName = m.group( 1 ).trim();
				String parentInstancePath = m.group( 2 ).trim();
				this.parentInstance = InstanceHelpers.findInstanceByPath( this.ma.getApplication(), parentInstancePath );
			}
		}
	}


	@Override
	public RoboconfError validate() {

		RoboconfError result = null;
		if( Utils.isEmptyOrWhitespaces( this.componentName ))
			result = new RoboconfError( ErrorCode.EXEC_CMD_MISSING_COMPONENT_NAME );
		else if( Utils.isEmptyOrWhitespaces( this.instanceName ))
			result = new RoboconfError( ErrorCode.EXEC_CMD_MISSING_INSTANCE_NAME );
		else if( ! this.instanceName.matches( ParsingConstants.PATTERN_FLEX_ID ))
			result = new RoboconfError( ErrorCode.EXEC_CMD_INVALID_INSTANCE_NAME );
		else if( this.component == null )
			result = new RoboconfError( ErrorCode.EXEC_CMD_INEXISTING_COMPONENT );
		else if( ! this.component.getAncestors().isEmpty()
				&& this.parentInstance == null )
			result = new RoboconfError( ErrorCode.EXEC_CMD_MISSING_PARENT_INSTANCE );

		return result;
	}


	@Override
	public void execute() throws CommandException {

		try {
			Instance instance = new Instance( this.instanceName ).component( this.component );
			this.manager.instancesMngr().addInstance( this.ma, this.parentInstance, instance );

		} catch( Exception e ) {
			throw new CommandException( e );
		}
	}
}
