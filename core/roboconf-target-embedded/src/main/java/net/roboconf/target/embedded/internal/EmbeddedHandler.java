/**
 * Copyright 2014-2015 Linagora, Université Joseph Fourier, Floralis
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

import net.roboconf.target.api.TargetException;
import net.roboconf.target.api.TargetHandler;

/**
 * A target for embedded systems (e.g. the local host).
 * @author Pierre-Yves Gibello - Linagora
 */
public class EmbeddedHandler implements TargetHandler {

	public static final String TARGET_ID = "embedded";
	private final Map<String,Boolean> machineIdToRunning = new HashMap<> ();


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.target.api.TargetHandler#getTargetId()
	 */
	@Override
	public String getTargetId() {
		return TARGET_ID;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.target.api.TargetHandler
	 * #createMachine(java.util.Map, java.lang.String, java.lang.String, java.lang.String,
	 * java.lang.String, java.lang.String)
	 */
	@Override
	public String createMachine(
			Map<String, String> targetProperties,
			Map<String,String> messagingConfiguration,
			String scopedInstancePath,
			String applicationName )
	throws TargetException {

		String machineId = scopedInstancePath + " (" + TARGET_ID + ")";
		this.machineIdToRunning.put( machineId, Boolean.TRUE );
		return machineId;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.target.api.TargetHandler
	 * #terminateMachine(java.util.Map, java.lang.String)
	 */
	@Override
	public void terminateMachine( Map<String, String> targetProperties, String machineId )
	throws TargetException {
		this.machineIdToRunning.remove( machineId );
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.target.api.TargetHandler#configureMachine(java.util.Map,
	 * java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public void configureMachine(
		Map<String,String> targetProperties,
		Map<String,String> messagingConfiguration,
		String machineId,
		String scopedInstancePath,
		String applicationName )
	throws TargetException {
		// nothing
	}


	@Override
	public boolean isMachineRunning( Map<String,String> targetProperties, String machineId )
	throws TargetException {
		return this.machineIdToRunning.containsKey( machineId );
	}
}
