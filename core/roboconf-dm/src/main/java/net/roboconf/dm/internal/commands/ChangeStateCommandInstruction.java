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
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.management.exceptions.CommandException;

/**
 * @author Vincent Zurczak - Linagora
 */
class ChangeStateCommandInstruction implements ICommandInstruction {

	static final String PREFIX = "change";

	private final ManagedApplication ma;
	private final Manager manager;

	private Instance instance;
	private InstanceStatus targetStatus;
	private String targetStatusAsString;


	/**
	 * Constructor.
	 * @param ma
	 * @param manager
	 * @param instruction
	 */
	ChangeStateCommandInstruction( ManagedApplication ma, Manager manager, String instruction ) {
		this.ma = ma;
		this.manager = manager;

		Pattern p = Pattern.compile( "change\\s+status\\s+of\\s+(/.*)\\s+to\\s+(.*)", Pattern.CASE_INSENSITIVE );
		Matcher m = p.matcher( instruction );
		if( m.matches()) {
			this.targetStatusAsString = m.group( 2 ).trim();
			this.targetStatus = InstanceStatus.whichStatus( this.targetStatusAsString );
			String instancePath = m.group( 1 ).trim();
			this.instance = InstanceHelpers.findInstanceByPath( this.ma.getApplication(), instancePath );
		}
	}


	@Override
	public RoboconfError validate() {

		RoboconfError result = null;
		if( this.targetStatus == null )
			result = new RoboconfError( ErrorCode.EXEC_CMD_INVALID_INSTANCE_STATUS, "Found: " + this.targetStatusAsString );
		else if( ! this.targetStatus.isStable())
			result = new RoboconfError( ErrorCode.EXEC_CMD_NO_MATCHING_INSTANCE );
		else if( this.instance == null )
			result = new RoboconfError( ErrorCode.EXEC_CMD_NO_MATCHING_INSTANCE );

		return result;
	}


	@Override
	public void execute() throws CommandException {

		try {
			this.manager.instancesMngr().changeInstanceState( this.ma, this.instance, this.targetStatus );

		} catch( Exception e ) {
			throw new CommandException( e );
		}
	}
}
