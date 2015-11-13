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
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.management.exceptions.CommandException;

/**
 * @author Vincent Zurczak - Linagora
 */
class AssociateTargetCommandInstruction implements ICommandInstruction {

	static final String PREFIX = "associate";

	private final ManagedApplication ma;
	private final Manager manager;

	private Instance scopedInstance;
	private String targetId;


	/**
	 * Constructor.
	 * @param ma
	 * @param manager
	 * @param instruction
	 */
	AssociateTargetCommandInstruction( ManagedApplication ma, Manager manager, String instruction ) {
		this.ma = ma;
		this.manager = manager;

		Pattern p = Pattern.compile( PREFIX + "\\s+(/.*)\\s+with\\s+(.*)", Pattern.CASE_INSENSITIVE );
		Matcher m = p.matcher( instruction );
		if( m.matches()) {
			this.targetId = m.group( 2 ).trim();
			String instanceName = m.group( 1 ).trim();
			this.scopedInstance = InstanceHelpers.findInstanceByPath( this.ma.getApplication(), instanceName );
		}
	}


	@Override
	public RoboconfError validate() {

		RoboconfError result = null;
		if( Utils.isEmptyOrWhitespaces( this.targetId ))
			result = new RoboconfError( ErrorCode.EXEC_CMD_INVALID_TARGET_ID, "Target ID: " + this.targetId );
		else if( this.scopedInstance == null )
			result = new RoboconfError( ErrorCode.EXEC_CMD_NO_MATCHING_INSTANCE );
		else if( ! InstanceHelpers.isTarget( this.scopedInstance ))
			result = new RoboconfError( ErrorCode.EXEC_CMD_NOT_A_SCOPED_INSTANCE );
		else if( this.manager.targetsMngr().findTargetById( this.targetId ) == null )
			result = new RoboconfError( ErrorCode.EXEC_CMD_TARGET_WAS_NOT_FOUND, "Target ID: " + this.targetId );

		return result;
	}


	@Override
	public void execute() throws CommandException {

		try {
			this.manager.targetsMngr().associateTargetWithScopedInstance(
					this.targetId, this.ma.getApplication(),
					InstanceHelpers.computeInstancePath( this.scopedInstance ));

		} catch( Exception e ) {
			throw new CommandException( e );
		}
	}
}
