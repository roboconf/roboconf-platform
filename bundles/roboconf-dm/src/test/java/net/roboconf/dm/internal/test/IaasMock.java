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

import java.util.Map;

import net.roboconf.iaas.api.IaasException;
import net.roboconf.iaas.api.IaasInterface;

/**
 * @author Vincent Zurczak - Linagora
 */
public class IaasMock implements IaasInterface {

	private final String installerName;


	/**
	 * Constructor.
	 * @param installerName
	 */
	public IaasMock( String installerName ) {
		this.installerName = installerName;
	}

	@Override
	public void terminateVM( String machineId ) throws IaasException {
		// nothing
	}

	@Override
	public void setIaasProperties( Map<String,String> iaasProperties ) throws IaasException {
		// nothing
	}

	@Override
	public String getIaasType() {
		return this.installerName;
	}

	@Override
	public String createVM( String messagingIp, String messagingUsername, String messagingPassword, String rootInstanceName, String applicationName )
	throws IaasException {
		return "whatever";
	}
}
