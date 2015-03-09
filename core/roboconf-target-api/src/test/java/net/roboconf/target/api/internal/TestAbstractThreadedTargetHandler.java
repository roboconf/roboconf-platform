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

package net.roboconf.target.api.internal;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import net.roboconf.target.api.AbstractThreadedTargetHandler;
import net.roboconf.target.api.TargetException;

/**
 * @author Vincent Zurczak - Linagora
 */
public class TestAbstractThreadedTargetHandler extends AbstractThreadedTargetHandler {

	private final AtomicInteger cpt = new AtomicInteger();
	private final boolean failConfiguration;


	/**
	 * Constructor.
	 * @param failConfiguration
	 */
	public TestAbstractThreadedTargetHandler( boolean failConfiguration ) {
		this.failConfiguration = failConfiguration;
	}


	@Override
	public String getTargetId() {
		return "whatever";
	}

	@Override
	public String createMachine(
			Map<String,String> targetProperties,
			String messagingIp,
			String messagingUsername,
			String messagingPassword,
			String rootInstanceName,
			String applicationName )
	throws TargetException {
		return "some-id";
	}


	@Override
	public void terminateMachine( Map<String,String> targetProperties, String machineId )
	throws TargetException {
		// nothing
	}


	@Override
	public boolean isMachineRunning( Map<String,String> targetProperties, String machineId )
	throws TargetException {
		return false;
	}


	@Override
	public MachineConfigurator machineConfigurator(
			Map<String,String> targetProperties,
			String machineId,
			String messagingIp,
			String messagingUsername,
			String messagingPassword,
			String rootInstanceName,
			String applicationName ) {

		return new TestMachineConfigurator( this.cpt, this.failConfiguration );
	}


	public int getCpt() {
		return this.cpt.get();
	}
}
