/**
 * Copyright 2013-2014 Linagora, Université Joseph Fourier
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

package net.roboconf.iaas.local;

import java.util.Properties;

import net.roboconf.iaas.api.IaasInterface;
import net.roboconf.iaas.api.exceptions.CommunicationToIaasException;
import net.roboconf.iaas.api.exceptions.IaasException;

/**
 * A IaaS emulation on the local host.
 * @author Pierre-Yves Gibello - Linagora
 */
public class IaasLocalhost implements IaasInterface {

	/*
	 * (non-Javadoc)
	 * @see net.roboconf.iaas.api.IaasInterface
	 * #setIaasProperties(java.util.Properties)
	 */
	@Override
	public void setIaasProperties( Properties iaasProperties ) {
		// nothing
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.iaas.api.IaasInterface
	 * #createVM(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public String createVM(
			String ipMessagingServer,
			String channelName,
			String applicationName)
	throws IaasException, CommunicationToIaasException {
		// TBD startup script ?
		System.out.println("IaaS localhost createVMNode");
		return "localhost";
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.iaas.api.IaasInterface
	 * #terminateVM(java.lang.String)
	 */
	@Override
	public void terminateVM( String instanceId ) throws IaasException, CommunicationToIaasException {
		// TBD shutdown script ?
		System.out.println("IaaS localhost terminateVM");
	}
}