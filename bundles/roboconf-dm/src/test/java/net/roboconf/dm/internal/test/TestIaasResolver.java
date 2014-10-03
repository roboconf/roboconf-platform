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
import java.util.Map;

import net.roboconf.core.model.runtime.Instance;
import net.roboconf.dm.internal.environment.iaas.IaasResolver;
import net.roboconf.dm.internal.management.ManagedApplication;
import net.roboconf.iaas.api.IaasException;
import net.roboconf.iaas.api.IaasInterface;

/**
 * @author Vincent Zurczak - Linagora
 */
public class TestIaasResolver extends IaasResolver {

	public final Map<Instance,Boolean> instanceToRunningStatus = new HashMap<Instance,Boolean> ();


	@Override
	public IaasInterface findIaasInterface( IaasInterface[] iaas, ManagedApplication ma, final Instance instance )
	throws IaasException {

		return new IaasInterface() {

			@Override
			public String getIaasType() {
				return "test";
			}

			@Override
			public void setIaasProperties( Map<String, String> iaasProperties )
			throws IaasException {
				// nothing
			}

			@Override
			public String createVM( String messagingIp, String messagingUsername, String messagingPassword, String rootInstanceName, String applicationName )
			throws IaasException {

				TestIaasResolver.this.instanceToRunningStatus.put( instance, Boolean.TRUE );
				return "generated machine id for " + rootInstanceName;
			}

			@Override
			public void terminateVM( String machineId )
			throws IaasException {

				TestIaasResolver.this.instanceToRunningStatus.put( instance, Boolean.FALSE );
			}
		};
	}
}
