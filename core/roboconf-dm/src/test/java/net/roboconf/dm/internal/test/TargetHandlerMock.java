/**
 * Copyright 2014-2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.dm.internal.test;

import java.util.Objects;

import net.roboconf.target.api.TargetException;
import net.roboconf.target.api.TargetHandler;
import net.roboconf.target.api.TargetHandlerParameters;

/**
 * @author Vincent Zurczak - Linagora
 */
public class TargetHandlerMock implements TargetHandler {

	private final String installerName;


	/**
	 * Constructor.
	 */
	public TargetHandlerMock( String installerName ) {
		this.installerName = installerName;
	}

	@Override
	public void terminateMachine( TargetHandlerParameters parameters, String machineId ) throws TargetException {
		// nothing
	}

	@Override
	public String getTargetId() {
		return this.installerName;
	}

	@Override
	public String createMachine( TargetHandlerParameters parameters )
	throws TargetException {
		return "whatever";
	}

	@Override
	public void configureMachine( TargetHandlerParameters parameters, String machineId )
	throws TargetException {
		// nothing
	}

	@Override
	public boolean isMachineRunning( TargetHandlerParameters parameters, String machineId ) throws TargetException {
		return false;
	}

	@Override
	public String retrievePublicIpAddress( TargetHandlerParameters parameters, String machineId )
	throws TargetException {
		return null;
	}

	@Override
	public boolean equals( Object obj ) {
		return obj instanceof TargetHandlerMock
				&& Objects.equals( this.installerName, ((TargetHandlerMock) obj ).installerName );
	}

	@Override
	public int hashCode() {
		return this.installerName == null ? 11 : this.installerName.hashCode();
	}
}
