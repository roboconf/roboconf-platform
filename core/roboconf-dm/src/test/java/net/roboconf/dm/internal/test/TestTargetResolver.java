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

package net.roboconf.dm.internal.test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.roboconf.core.model.beans.Instance;
import net.roboconf.dm.internal.environment.target.TargetResolver;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.target.api.TargetException;
import net.roboconf.target.api.TargetHandler;

/**
 * @author Vincent Zurczak - Linagora
 */
public class TestTargetResolver extends TargetResolver {

	public final Map<Instance,Boolean> instanceToRunningStatus = new HashMap<Instance,Boolean> ();


	@Override
	public Target findTargetHandler( List<TargetHandler> target, ManagedApplication ma, final Instance instance )
	throws TargetException {

		TargetHandler handler = new TargetHandler() {

			@Override
			public String getTargetId() {
				return "test";
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

				TestTargetResolver.this.instanceToRunningStatus.put( instance, Boolean.TRUE );
				return "generated machine id for " + rootInstanceName;
			}


			@Override
			public void configureMachine(
					Map<String,String> targetProperties,
					String machineId,
					String messagingIp,
					String messagingUsername,
					String messagingPassword,
					String rootInstanceName,
					String applicationName)
			throws TargetException {
				// nothing
			}


			@Override
			public boolean isMachineRunning( Map<String,String> targetProperties, String machineId )
			throws TargetException {

				Boolean running = TestTargetResolver.this.instanceToRunningStatus.get( instance );
				return running != null && running;
			}

			@Override
			public void terminateMachine( Map<String,String> targetProperties, String machineId )
			throws TargetException {

				TestTargetResolver.this.instanceToRunningStatus.put( instance, Boolean.FALSE );
			}
		};

		return new Target( handler, new HashMap<String,String>( 0 ));
	}
}
