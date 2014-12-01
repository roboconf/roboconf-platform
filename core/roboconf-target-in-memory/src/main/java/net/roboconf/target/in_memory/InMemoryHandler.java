/**
 * Copyright 2013-2014 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.target.in_memory;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import net.roboconf.target.api.TargetException;
import net.roboconf.target.api.TargetHandler;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.MissingHandlerException;
import org.apache.felix.ipojo.UnacceptableConfiguration;

/**
 * A target that runs agents in memory.
 * @author Pierre-Yves Gibello - Linagora
 * @author Vincent Zurczak - Linagora
 */
public class InMemoryHandler implements TargetHandler {

	public static final String TARGET_ID = "in-memory";
	private Factory agentFactory;


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.target.api.TargetHandler#getTargetId()
	 */
	@Override
	public String getTargetId() {
		return TARGET_ID;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.target.api.TargetHandler
	 * #createOrConfigureMachine(java.util.Map, java.lang.String, java.lang.String,
	 * java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public String createOrConfigureMachine(
			Map<String, String> targetProperties,
			String messagingIp,
			String messagingUsername,
			String messagingPassword,
			String rootInstanceName,
			String applicationName )
	throws TargetException {

		Dictionary<String,Object> configuration = new Hashtable<String,Object> ();
	    configuration.put( "message-server-ip", messagingIp );
	    configuration.put( "message-server-username", messagingUsername );
	    configuration.put( "message-server-password", messagingPassword );
	    configuration.put( "application-name", applicationName );
	    configuration.put( "root-instance-name", rootInstanceName );

	    String machineId = rootInstanceName + " @ " + applicationName;
	    configuration.put( Factory.INSTANCE_NAME_PROPERTY, machineId );

	    if( this.agentFactory == null )
	    	throw new TargetException( "The iPojo  factory was not available." );

	    try {
	    	ComponentInstance instance = this.agentFactory.createComponentInstance( configuration );
	    	instance.start();

		} catch( UnacceptableConfiguration e ) {
			throw new TargetException( "An in-memory agent could not be launched. Root instance name: " + rootInstanceName, e );

		} catch( MissingHandlerException e ) {
			throw new TargetException( "An in-memory agent could not be launched. Root instance name: " + rootInstanceName, e );

		} catch( ConfigurationException e ) {
			throw new TargetException( "An in-memory agent could not be launched. Root instance name: " + rootInstanceName, e );
		}

		return machineId;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.target.api.TargetHandler
	 * #terminateMachine(java.util.Map, java.lang.String)
	 */
	@Override
	public void terminateMachine( Map<String, String> targetProperties, String machineId ) throws TargetException {

		if( this.agentFactory != null ) {
			for( ComponentInstance instance : this.agentFactory.getInstances()) {
				if( machineId.equals( instance.getInstanceName())) {
					instance.dispose();
					break;
				}
			}
		}
	}


	/**
	 * @param agentFactory the agentFactory to set
	 */
	public void setAgentFactory( Factory agentFactory ) {
		this.agentFactory = agentFactory;
	}
}
