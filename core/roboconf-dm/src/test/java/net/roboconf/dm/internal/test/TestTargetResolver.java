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

import java.util.HashMap;
import java.util.Map;

import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.dm.internal.api.impl.TargetHandlerResolverImpl;
import net.roboconf.target.api.TargetException;
import net.roboconf.target.api.TargetHandler;
import net.roboconf.target.api.TargetHandlerParameters;

/**
 * @author Vincent Zurczak - Linagora
 */
public class TestTargetResolver extends TargetHandlerResolverImpl {

	public final Map<String,Boolean> instancePathToRunningStatus = new HashMap<> ();
	public final Map<String,Integer> instancePathToRequestsCount = new HashMap<> ();


	@Override
	public TargetHandler findTargetHandler( Map<String,String> targetProperties )
	throws TargetException {

		TargetHandler handler = new TargetHandler() {
			@Override
			public String getTargetId() {
				return "test";
			}

			@Override
			public String createMachine( TargetHandlerParameters parameters )
			throws TargetException {

				String scopedInstancePath = parameters.getScopedInstancePath();
				TestTargetResolver.this.instancePathToRunningStatus.put( scopedInstancePath, Boolean.TRUE );
				Integer cpt = TestTargetResolver.this.instancePathToRequestsCount.get( scopedInstancePath );
				if( cpt == null )
					cpt = 0;

				TestTargetResolver.this.instancePathToRequestsCount.put( scopedInstancePath, ++ cpt );
				return scopedInstancePath;
			}


			@Override
			public void configureMachine( TargetHandlerParameters parameters, String machineId )
			throws TargetException {
				// nothing
			}


			@Override
			public boolean isMachineRunning( TargetHandlerParameters parameters, String machineId )
			throws TargetException {

				// In this handler, the machine ID is the scoped instance's path
				Boolean running = TestTargetResolver.this.instancePathToRunningStatus.get( machineId );
				return running != null && running.booleanValue();
			}

			@Override
			public void terminateMachine( TargetHandlerParameters parameters, String machineId )
			throws TargetException {

				// In this handler, the machine ID is the scoped instance's path
				TestTargetResolver.this.instancePathToRunningStatus.put( machineId, Boolean.FALSE );
			}

			@Override
			public String retrievePublicIpAddress( TargetHandlerParameters parameters, String machineId )
			throws TargetException {
				return null;
			}
		};

		return handler;
	}


	public Boolean isRunning( Instance inst ) {
		String path = InstanceHelpers.computeInstancePath( inst );
		return this.instancePathToRunningStatus.get( path );
	}


	public Integer count( Instance inst ) {
		String path = InstanceHelpers.computeInstancePath( inst );
		return this.instancePathToRequestsCount.get( path );
	}
}
