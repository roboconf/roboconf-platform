/**
 * Copyright 2014-2016 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.target.occi.internal;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

import net.roboconf.core.model.beans.Instance;
import net.roboconf.target.api.AbstractThreadedTargetHandler.MachineConfigurator;
import net.roboconf.target.api.TargetException;

/**
 * A machine configurator for OCCI.
 * @author Pierre-Yves Gibello - Linagora
 */
public class OcciMachineConfigurator implements MachineConfigurator {

	private final Logger logger = Logger.getLogger( getClass().getName());
	private final Map<String,String> targetProperties;
	private final String userData, rootInstanceName;
	private final Instance scopedInstance;

	/**
	 * Constructor.
	 */
	public OcciMachineConfigurator(
			Map<String,String> targetProperties,
			String userData,
			String rootInstanceName,
			Instance scopedInstance ) {

		this.targetProperties = targetProperties;
		this.userData = userData;
		this.rootInstanceName = rootInstanceName;
		this.scopedInstance = scopedInstance;
	}


	@Override
	public Instance getScopedInstance() {
		return this.scopedInstance;
	}


	@Override
	public void close() throws IOException {
		// nothing
	}


	@Override
	public boolean configure() throws TargetException {

/*
		try {
			// Is the VM up?

			// If yes, write user data.

			return true;

		} catch( Exception e ) {
			throw new TargetException( e );
		}
*/
		return true;
	}
}
