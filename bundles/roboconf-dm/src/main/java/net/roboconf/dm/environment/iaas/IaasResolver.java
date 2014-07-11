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
import net.roboconf.iaas.azure.IaasAzure;
import net.roboconf.iaas.ec2.IaasEc2;
import net.roboconf.iaas.embedded.IaasEmbedded;
import net.roboconf.iaas.in_memory.IaasInMemory;
import net.roboconf.iaas.openstack.IaasOpenstack;
import net.roboconf.iaas.vmware.IaasVmware;


/**
 * @author Vincent Zurczak - Linagora
 */
public class IaasResolver {

	public static final String IAAS_TYPE = "iaas.type";
	public static final String IAAS_IN_MEMORY = "in-memory";
	public static final String IAAS_EMBEDDED = "embedded";
	public static final String IAAS_EC2 = "ec2";
	public static final String IAAS_OPENSTACK = "openstack";
	public static final String IAAS_VMWARE = "vmware";
	public static final String IAAS_AZURE = "azure";


	/**
	 * Finds the right IaaS interface for a given instance.
	 * @param ma the managed application
	 * @param instance the (root) instance associated with a IaaS
	 * @return a IaaS interface
	 * @throws IaasException if no IaaS interface was found
	 */
	public IaasInterface findIaasInterface( ManagedApplication ma, Instance instance ) throws IaasException {

		// FIXME: Not very "plug-in-like"
		IaasInterface iaasInterface;
		try {
			String installerName = instance.getComponent().getInstallerName();
			if( ! Constants.IAAS_INSTALLER.equalsIgnoreCase( installerName ))
				throw new IaasException( "Unsupported installer name: " + installerName );

			Map<String, String> props = IaasHelpers.loadIaasProperties( ma.getApplicationFilesDirectory(), instance );
			iaasInterface = findIaasHandler( props );
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
	 * @param iaasProperties non-null properties
	 * @return a IaaS interface, or null if none matched
	 * TODO: move this method in the IaaS implementations
	 */
	IaasInterface findIaasHandler( Map<String,String> iaasProperties ) {

		String iaasType = iaasProperties.get( IAAS_TYPE );
		return findIaasHandler( iaasType );
	}


	/**
	 * Finds the right IaaS handler.
	 * @param iaasType the IaaS type
	 * @return a IaaS interface, or null if none matched
	 * TODO: move this method in the IaaS implementations
	 */
	protected IaasInterface findIaasHandler( String iaasType ) {

		IaasInterface result = null;
		if( IAAS_IN_MEMORY.equalsIgnoreCase( iaasType ) ) {
			result = new IaasInMemory();

		} else if( IAAS_EMBEDDED.equalsIgnoreCase( iaasType ) ) {
			result = new IaasEmbedded();

		} else if( IAAS_EC2.equalsIgnoreCase( iaasType ) ) {
			result = new IaasEc2();

		} else if( IAAS_OPENSTACK.equalsIgnoreCase( iaasType ) ) {
			result = new IaasOpenstack();

		} else if( IAAS_VMWARE.equalsIgnoreCase( iaasType ) ) {
			result = new IaasVmware();

		} else if( IAAS_AZURE.equalsIgnoreCase( iaasType ) ) {
			result = new IaasAzure();
		}

		return result;
	}
}
