/**
 * Copyright 2014 Linagora, Université Joseph Fourier
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

import net.roboconf.core.model.runtime.Instance;
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
	public TargetHandler findTargetHandler( List<TargetHandler> target, ManagedApplication ma, final Instance instance )
	throws TargetException {

		return new TargetHandler() {

			@Override
			public String getTargetId() {
				return "test";
			}

			@Override
			public void setTargetProperties( Map<String, String> targetProperties )
			throws TargetException {
				// nothing
			}

			@Override
			public String createOrConfigureMachine( String messagingIp, String messagingUsername, String messagingPassword, String rootInstanceName, String applicationName )
			throws TargetException {

				TestTargetResolver.this.instanceToRunningStatus.put( instance, Boolean.TRUE );
				return "generated machine id for " + rootInstanceName;
			}

			@Override
			public void terminateMachine( String machineId )
			throws TargetException {

				TestTargetResolver.this.instanceToRunningStatus.put( instance, Boolean.FALSE );
			}
		};
	}
}
