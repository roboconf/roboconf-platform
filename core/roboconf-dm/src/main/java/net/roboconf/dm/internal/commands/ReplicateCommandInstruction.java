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

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.roboconf.core.ErrorCode;
import net.roboconf.core.RoboconfError;
import net.roboconf.core.dsl.ParsingConstants;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.management.exceptions.CommandException;

/**
 * @author Vincent Zurczak - Linagora
 */
class ReplicateCommandInstruction implements ICommandInstruction {

	static final String PREFIX = "replicate";

	private final ManagedApplication ma;
	private final Manager manager;

	private Instance rootInstance;
	private String newInstanceName;


	/**
	 * Constructor.
	 * @param ma
	 * @param manager
	 * @param instruction
	 */
	ReplicateCommandInstruction( ManagedApplication ma, Manager manager, String instruction ) {
		this.ma = ma;
		this.manager = manager;

		Pattern p = Pattern.compile( PREFIX + "\\s+(.*)\\s+as\\s+(.*)", Pattern.CASE_INSENSITIVE );
		Matcher m = p.matcher( instruction );
		if( m.matches()) {
			this.newInstanceName = m.group( 2 ).trim();
			String instanceName = m.group( 1 ).trim();
			this.rootInstance = InstanceHelpers.findInstanceByPath( this.ma.getApplication(), instanceName );
		}
	}


	@Override
	public RoboconfError validate() {

		RoboconfError result = null;
		if( Utils.isEmptyOrWhitespaces( this.newInstanceName ))
			result = new RoboconfError( ErrorCode.EXEC_CMD_MISSING_INSTANCE_NAME );
		else if( ! this.newInstanceName.matches( ParsingConstants.PATTERN_FLEX_ID ))
			result = new RoboconfError( ErrorCode.EXEC_CMD_INVALID_INSTANCE_NAME );
		else if( this.rootInstance == null )
			result = new RoboconfError( ErrorCode.EXEC_CMD_NO_MATCHING_INSTANCE );
		else if( this.rootInstance.getParent() != null )
			result = new RoboconfError( ErrorCode.EXEC_CMD_NOT_A_ROOT_INSTANCE );

		return result;
	}


	@Override
	public void execute() throws CommandException {

		try {
			// Copy the instance
			Instance copy = InstanceHelpers.replicateInstance( this.rootInstance );
			copy.setName( this.newInstanceName );
			this.manager.instancesMngr().addInstance( this.ma, null, copy );

			// Associate this new instance with the same target, if it has one
			String targetId = this.manager.targetsMngr().findTargetId( this.ma.getApplication(), "/" + this.rootInstance.getName());
			String defaultTargetId = this.manager.targetsMngr().findTargetId( this.ma.getApplication(), null );
			if( targetId != null
					&& ! Objects.equals( targetId, defaultTargetId ))
				this.manager.targetsMngr().associateTargetWithScopedInstance( targetId, this.ma.getApplication(), "/" + copy.getName());

		} catch( Exception e ) {
			throw new CommandException( e );
		}
	}
}
