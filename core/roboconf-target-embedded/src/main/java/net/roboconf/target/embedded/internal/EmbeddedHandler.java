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

package net.roboconf.target.embedded.internal;

import java.util.HashMap;
import java.util.Map;

import net.roboconf.core.model.beans.Instance;
import net.roboconf.target.api.TargetException;
import net.roboconf.target.api.TargetHandler;
import net.roboconf.target.api.TargetHandlerParameters;

/**
 * A target for embedded systems (e.g. the local host).
 * @author Pierre-Yves Gibello - Linagora
 */
public class EmbeddedHandler implements TargetHandler {

	public static final String TARGET_ID = "embedded";
	private final Map<String,Boolean> machineIdToRunning = new HashMap<> ();


	@Override
	public String getTargetId() {
		return TARGET_ID;
	}


	@Override
	public String createMachine( TargetHandlerParameters parameters ) throws TargetException {

		String machineId = parameters.getScopedInstancePath() + " (" + TARGET_ID + ")";
		this.machineIdToRunning.put( machineId, Boolean.TRUE );
		return machineId;
	}


	@Override
	public void terminateMachine( TargetHandlerParameters parameters, String machineId )
	throws TargetException {
		this.machineIdToRunning.remove( machineId );
	}


	@Override
	public void configureMachine( TargetHandlerParameters parameters, String machineId, Instance scopedInstance )
	throws TargetException {

		// It may require to be configured from the DM => add the right marker
		scopedInstance.data.put( Instance.READY_FOR_CFG_MARKER, "true" );
	}


	@Override
	public boolean isMachineRunning( TargetHandlerParameters parameters, String machineId )
	throws TargetException {
		return this.machineIdToRunning.containsKey( machineId );
	}


	@Override
	public String retrievePublicIpAddress( TargetHandlerParameters parameters, String machineId )
	throws TargetException {
		// This handler cannot determine the IP address
		return null;
	}
}
