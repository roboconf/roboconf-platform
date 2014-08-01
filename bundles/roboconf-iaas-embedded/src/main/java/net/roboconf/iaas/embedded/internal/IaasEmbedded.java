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

package net.roboconf.iaas.embedded.internal;

import java.util.Map;

import net.roboconf.iaas.api.IaasException;
import net.roboconf.iaas.api.IaasInterface;

/**
 * A IaaS emulation on embedded systems (eg. the local host).
 * @author Pierre-Yves Gibello - Linagora
 */
public class IaasEmbedded implements IaasInterface {

	/*
	 * (non-Javadoc)
	 * @see net.roboconf.iaas.api.IaasInterface#getIaasType()
	 */
	@Override
	public String getIaasType() {
		return "embedded";
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.iaas.api.IaasInterface
	 * #setIaasProperties(net.roboconf.iaas.api.IaasProperties)
	 */
	@Override
	public void setIaasProperties( Map<String,String> iaasProperties ) {
		// nothing
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.iaas.api.IaasInterface
	 * #createVM(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public String createVM(
			String messagingIp,
			String messagingUsername,
			String messagingPassword,
			String rootInstanceName,
			String applicationName )
	throws IaasException {

		return rootInstanceName + " (embedded)";
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.iaas.api.IaasInterface
	 * #terminateVM(java.lang.String)
	 */
	@Override
	public void terminateVM( String instanceId ) throws IaasException {
		// TBD shutdown script ?
	}
}
