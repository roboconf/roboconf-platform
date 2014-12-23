/**
 * Copyright 2014 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.dm.internal.autonomic;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import net.roboconf.core.model.beans.Component;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.helpers.ComponentHelpers;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.messaging.messages.from_agent_to_dm.MsgNotifAutonomic;

/**
 * An event handler to evaluate rules.
 * <p>
 * This handler can create new instances, including virtual machines.
 * It can also delete instances it has created.
 * </p>
 *
 * @author Pierre-Yves Gibello - Linagora
 */
public class RuleBasedEventHandler {

	static final String DELETE_SERVICE = "delete-service";
	static final String REPLICATE_SERVICE = "replicate-service";
	static final String MAIL = "mail";

	private final Manager manager;
	private final Logger logger = Logger.getLogger( getClass().getName());
	final Map<String,List<Instance>> componentNameToCreatedRootInstances;


	/**
	 * Create a new rule-based event handler.
	 * @param manager the application manager
	 */
	public RuleBasedEventHandler( Manager manager ) {
		this.componentNameToCreatedRootInstances = new HashMap<String,List<Instance>> ();
		this.manager = manager;
	}


	/**
	 * React upon autonomic monitoring message (aka "autonomic event").
	 * @param event The autonomic event message
	 * @throws Exception
	 */
	public void handleEvent( ManagedApplication ma, MsgNotifAutonomic event ) {

		try {
			Map<String,AutonomicRule> rules = RulesParser.parseRules( ma );
			AutonomicRule rule = rules.get( event.getEventId());

			if( rule == null )
				this.logger.fine( "No rule was found to handle events with the '" + event.getEventId() + "' ID." );

			// EVENT_ID ReplicateService ComponentTemplate
			else if( REPLICATE_SERVICE.equalsIgnoreCase( rule.getReactionId()))
				createInstances( ma, rule.getReactionInfo());

			// EVENT_ID StopService ComponentName
			else if( DELETE_SERVICE.equalsIgnoreCase(rule.getReactionId()))
				deleteInstances( ma, rule.getReactionInfo());

			// EVENT_ID Mail DestinationEmail
			// TODO: implement it
			else if( MAIL.equalsIgnoreCase(rule.getReactionId()))
				this.logger.info( "We should send an e-mail..." );

			// EVENT_ID Log LogMessage
			// And default behavior...
			else
				this.logger.info( "AUTONOMIC Monitoring event. Info = " + rule.getReactionInfo());

		} catch( IOException e ) {
			this.logger.warning( "An autonomic event could not be handled. " + e.getMessage());
			Utils.logException( this.logger, e );
		}
	}


	/**
	 * Instantiate a new VM with instances on it.
	 * @param ma the managed application
	 * @param componentTemplates the component names, separated by "/"
	 * <p>
	 * (e.g. VM_name/Software_container_name/App_artifact_name)
	 * </p>
	 */
	void createInstances( ManagedApplication ma, String componentTemplates ) {

		try {
			if( componentTemplates.startsWith( "/" ))
				componentTemplates = componentTemplates.substring( 1 );

			String templates[] = componentTemplates.split( "/" );

			// First check that all component to instantiate are valid and found...
			// Necessary, not to create a VM then try to instantiate a fake component there!
			for( String s : templates) {
				Component compToInstantiate = ComponentHelpers.findComponent( ma.getApplication().getGraphs(), s);
				if( compToInstantiate == null )
					throw new IOException( "Component " + s + " was not found in application " + ma.getApplication().getName());
			}

			// We register new instances in the model
			Instance previousInstance = null;
			for( String s : templates) {
				Component compToInstantiate = ComponentHelpers.findComponent( ma.getApplication().getGraphs(), s);
				// compToInstantiate should never be null (check done above).

				// All the root instances must have a different name. Others do not matter.
				String instanceName;
				if( previousInstance == null )
					instanceName = compToInstantiate.getName() + "_" + System.currentTimeMillis();
				else
					instanceName = compToInstantiate.getName().toLowerCase();

				Instance currentInstance = new Instance( instanceName ).component(compToInstantiate);
				this.manager.addInstance( ma, previousInstance, currentInstance );
				previousInstance = currentInstance;
			}

			// Now, deploy and start all
			Instance rootInstance = InstanceHelpers.findRootInstance( previousInstance );
			this.manager.deployAndStartAll( ma, rootInstance );

			// Remember the VM this class has created
			String componentName = previousInstance.getName();
			List<Instance> vmList = this.componentNameToCreatedRootInstances.get( componentName );
			if(vmList == null)
				vmList = new ArrayList<Instance>();

			vmList.add( rootInstance );
			this.componentNameToCreatedRootInstances.put( componentName, vmList );

		} catch( Exception e ) {
			this.logger.warning( "The creation of instances (autonomic context) failed. " + e.getMessage());
			Utils.logException( this.logger, e );
		}
	}


	/**
	 * Deletes an instance and the VM that hosts it.
	 * <p>
	 * Only instances that were created by this class can be undeployed and removed from the model.
	 * </p>
	 *
	 * @param ma the managed application
	 * @param componentName the component name of an instance to delete
	 */
	void deleteInstances( ManagedApplication ma, String componentName ) {

		try {
			List<Instance> vmList = this.componentNameToCreatedRootInstances.get( componentName );
			if( vmList != null ) {
				if( vmList.size() <= 1 )
					this.componentNameToCreatedRootInstances.remove( componentName );

				Instance vmInstance = vmList.remove( 0 );
				this.manager.undeployAll( ma, vmInstance );
				this.manager.removeInstance( ma, vmInstance );
			}

		} catch( Exception e ) {
			this.logger.warning( "The deletion of an instance (autonomic context) failed. " + e.getMessage());
			Utils.logException( this.logger, e );
		}
	}
}
