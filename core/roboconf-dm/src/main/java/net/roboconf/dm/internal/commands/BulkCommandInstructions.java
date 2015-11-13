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
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.management.exceptions.CommandException;

/**
 * @author Vincent Zurczak - Linagora
 */
class BulkCommandInstructions implements ICommandInstruction {

	private final ManagedApplication ma;
	private final Manager manager;

	private Instance instance;
	private ChangeStateInstruction instruction;


	/**
	 * Constructor.
	 * @param ma
	 * @param manager
	 * @param instruction
	 */
	BulkCommandInstructions( ManagedApplication ma, Manager manager, String instruction ) {
		this.ma = ma;
		this.manager = manager;

		Matcher m = getPattern().matcher( instruction );
		if( m.matches()) {
			String instancePath = m.group( 2 ).trim();
			this.instance = InstanceHelpers.findInstanceByPath( this.ma.getApplication(), instancePath );
			this.instruction = ChangeStateInstruction.which( m.group( 1 ).trim());
		}
	}


	/**
	 * @param line a non-null string
	 * @return true if it matches a supported instruction, false otherwise
	 */
	public static boolean isBulkInstruction( String line ) {

		Matcher m = getPattern().matcher( line );
		return m.matches()
				&& ChangeStateInstruction.which( m.group( 1 ).trim()) != null;
	}


	/**
	 * @return a pattern to recognize instructions supported by this class
	 */
	private static Pattern getPattern() {
		return Pattern.compile( "([^/]+)(/.*)", Pattern.CASE_INSENSITIVE );
	}


	@Override
	public RoboconfError validate() {

		RoboconfError result = null;
		if( this.instruction == null )
			result = new RoboconfError( ErrorCode.EXEC_CMD_UNRECOGNIZED_INSTRUCTION );
		else if( this.instance == null )
			result = new RoboconfError( ErrorCode.EXEC_CMD_NO_MATCHING_INSTANCE );

		return result;
	}


	@Override
	public void execute() throws CommandException {

		try {
			switch( this.instruction ) {
			case DEPLOY_AND_START_ALL:
				this.manager.instancesMngr().deployAndStartAll( this.ma, this.instance );
				break;

			case STOP_ALL:
				this.manager.instancesMngr().stopAll( this.ma, this.instance );
				break;

			case UNDEPLOY_ALL:
				this.manager.instancesMngr().undeployAll( this.ma, this.instance );
				break;

			case DELETE:
				this.manager.instancesMngr().removeInstance( this.ma, this.instance );
				break;
			}

		} catch( Exception e ) {
			throw new CommandException( e );
		}
	}


	/**
	 * @author Vincent Zurczak - Linagora
	 */
	public static enum ChangeStateInstruction {

		DELETE,
		DEPLOY_AND_START_ALL,
		STOP_ALL,
		UNDEPLOY_ALL;


		/*
		 * (non-Javadoc)
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return super.toString().replace( '_', ' ' ).toLowerCase();
		}


		/**
		 * @param s a string (can be null)
		 * @return null if no instruction was recognized, or an instruction otherwise
		 */
		public static ChangeStateInstruction which( String s ) {

			ChangeStateInstruction result = null;
			for( ChangeStateInstruction elt : ChangeStateInstruction.values()) {
				if( elt.toString().equalsIgnoreCase( s )) {
					result = elt;
					break;
				}
			}

			return result;
		}
	}
}
