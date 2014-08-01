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

package net.roboconf.dm.environment.iaas;

import java.io.IOException;
import java.util.Map;

import net.roboconf.core.Constants;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.iaas.api.IaasException;
import net.roboconf.iaas.api.IaasInterface;

/**
 * @author Vincent Zurczak - Linagora
 */
public class IaasResolver {

	public static final String IAAS_TYPE = "iaas.type";


	/**
	 * Finds the right IaaS interface for a given instance.
	 * @param ma the managed application
	 * @param iaas the list of available IaaS (can be null)
	 * @param instance the (root) instance associated with a IaaS
	 * @return a IaaS interface
	 * @throws IaasException if no IaaS interface was found
	 */
	public IaasInterface findIaasInterface( IaasInterface[] iaas, ManagedApplication ma, Instance instance ) throws IaasException {

		IaasInterface iaasInterface;
		try {
			String installerName = instance.getComponent().getInstallerName();
			if( ! Constants.IAAS_INSTALLER.equalsIgnoreCase( installerName ))
				throw new IaasException( "Unsupported installer name: " + installerName );

			Map<String, String> props = IaasHelpers.loadIaasProperties( ma.getApplicationFilesDirectory(), instance );
			iaasInterface = findIaasHandler( iaas, props );
			if( iaasInterface == null )
				throw new IaasException( "No IaaS handler was found for " + instance.getName() + "." );

			iaasInterface.setIaasProperties( props );

		} catch( IOException e ) {
			throw new IaasException( e );
		}

		return iaasInterface;
	}


	/**
	 * Finds the right IaaS handler.
	 * @param iaas the list of available IaaS (can be null)
	 * @param iaasProperties non-null properties
	 * @return a IaaS interface, or null if none matched
	 */
	IaasInterface findIaasHandler( IaasInterface[] iaas, Map<String,String> iaasProperties ) {

		String iaasType = iaasProperties.get( IAAS_TYPE );
		return findIaasHandler( iaas, iaasType );
	}


	/**
	 * Finds the right IaaS handler.
	 * @param iaas the list of available IaaS (can be null)
	 * @param iaasType the IaaS type
	 * @return a IaaS interface, or null if none matched
	 */
	protected IaasInterface findIaasHandler( IaasInterface[] iaas, String iaasType ) {

		IaasInterface result = null;
		if( iaas != null ) {
			for( IaasInterface itf : iaas ) {
				if( iaasType.equalsIgnoreCase( itf.getIaasType())) {
					result = itf;
					break;
				}
			}
		}

		return result;
	}
}
