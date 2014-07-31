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

package net.roboconf.iaas.in_memory.internal;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import net.roboconf.iaas.api.IaasException;
import net.roboconf.iaas.api.IaasInterface;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.MissingHandlerException;
import org.apache.felix.ipojo.UnacceptableConfiguration;

/**
 * A IaaS emulation that runs agents in memory.
 * @author Pierre-Yves Gibello - Linagora
 * @author Vincent Zurczak - Linagora
 */
public class IaasInMemory implements IaasInterface {

	private Factory agentFactory;


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.iaas.api.IaasInterface
	 * #setIaasProperties(java.util.Properties)
	 */
	@Override
	public void setIaasProperties(Map<String, String> iaasProperties) {
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

		Dictionary<String,Object> configuration = new Hashtable<String,Object> ();
	    configuration.put( "message-server-ip", messagingIp );
	    configuration.put( "message-server-username", messagingUsername );
	    configuration.put( "message-server-password", messagingPassword );
	    configuration.put( "application-name", applicationName );
	    configuration.put( "root-instance-name", rootInstanceName );

	    String machineId = rootInstanceName + " @ " + applicationName;
	    configuration.put( Factory.INSTANCE_NAME_PROPERTY, machineId );

	    try {
	    	ComponentInstance instance = this.agentFactory.createComponentInstance( configuration );
	    	instance.start();

		} catch( UnacceptableConfiguration e ) {
			throw new IaasException( "An in-memory agent could not be launched. Root instance name: " + rootInstanceName, e );

		} catch( MissingHandlerException e ) {
			throw new IaasException( "An in-memory agent could not be launched. Root instance name: " + rootInstanceName, e );

		} catch( ConfigurationException e ) {
			throw new IaasException( "An in-memory agent could not be launched. Root instance name: " + rootInstanceName, e );
		}

		return machineId;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.iaas.api.IaasInterface
	 * #terminateVM(java.lang.String)
	 */
	@Override
	public void terminateVM( String machineId ) throws IaasException {

		for( ComponentInstance instance : this.agentFactory.getInstances()) {
			if( machineId.equals( instance.getInstanceName())) {
				instance.dispose();
				break;
			}
		}
	}
}
