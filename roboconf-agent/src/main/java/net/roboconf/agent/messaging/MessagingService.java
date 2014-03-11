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

package net.roboconf.agent.messaging;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import net.roboconf.agent.AgentData;
import net.roboconf.core.internal.utils.Utils;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.helpers.VariableHelpers;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.messaging.MessagingConstants;
import net.roboconf.messaging.client.IMessageServerClient;
import net.roboconf.messaging.client.InteractionType;
import net.roboconf.messaging.messages.Message;
import net.roboconf.messaging.messages.from_agent_to_agent.MsgCmdImportAdd;
import net.roboconf.messaging.messages.from_agent_to_agent.MsgCmdImportNotification;
import net.roboconf.messaging.messages.from_agent_to_agent.MsgCmdImportRemove;
import net.roboconf.messaging.messages.from_agent_to_dm.MsgNotifHeartbeat;
import net.roboconf.messaging.messages.from_agent_to_dm.MsgNotifMachineDown;
import net.roboconf.messaging.messages.from_agent_to_dm.MsgNotifMachineUp;

/**
 * @author Noël - LIG
 * @author Pierre-Yves Gibello - Linagora
 * @author Vincent Zurczak - Linagora
 */
public final class MessagingService {

	private static final String EXPORTS = "exports.";
	private static final String IMPORTS = "imports.";

	private final Logger logger = Logger.getLogger( getClass().getName());

	private IMessageServerClient client;
	private AgentData agentData;



	/**
	 * Initializes the connection with the message server.
	 * @param agentData the agent's data
	 * @param client the client for the message server
	 * @throws IOException if something went wrong
	 */
	public void initializeAgentConnection( final AgentData agentData, final IMessageServerClient client ) throws IOException {

		this.agentData = agentData;
		this.client = client;
		client.openConnection();

		// Indicate this machine is up
		MsgNotifMachineUp machineIsUp = new MsgNotifMachineUp( agentData.getRootInstanceName(), agentData.getIpAddress());
		client.publish( InteractionType.AGENT_TO_DM, null, machineIsUp );


		// Add a hook for when the VM shutdowns
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {

				MsgNotifMachineDown machineIsDown = new MsgNotifMachineDown( agentData.getRootInstanceName());
				try {
					client.publish( InteractionType.AGENT_TO_DM, null, machineIsDown );

				} catch( IOException e ) {
					MessagingService.this.logger.severe( e.getMessage());
					MessagingService.this.logger.finest( Utils.writeException( e ));
				}
			}
		}));


		// Regularly send a heart beat message
		Timer timer = new Timer( "Roboconf's Heartbeat Timer @ Agent", true );
		TimerTask timerTask = new TimerTask() {
			@Override
			public void run() {

				MsgNotifHeartbeat heartBeat = new MsgNotifHeartbeat( agentData.getRootInstanceName());
				try {
					client.publish( InteractionType.AGENT_TO_DM, null, heartBeat );

				} catch( IOException e ) {
					MessagingService.this.logger.severe( e.getMessage());
					MessagingService.this.logger.finest( Utils.writeException( e ));
				}
			}
		};

		timer.scheduleAtFixedRate( timerTask, 0, MessagingConstants.HEARTBEAT_PERIOD );


		// Listen to messages that come from the DM
		// FIXME: no routing key?
		client.subscribeTo( InteractionType.DM_TO_AGENT, null );
	}


	public void publishMessage( InteractionType interactionType, Message message ) throws IOException {

		// client.publish( interactionType, filterName, message );
//		String applicationName = this.agent.getApplicationName();
//		this.channel.basicPublish(
//				this.agentExchangeName,
//				MessagingUtils.buildAgentQueueName( applicationName ),
//				null,
//				SerializationUtils.serializeObject( message ));
	}



	public void configureInstanceMessaging( Instance instance, boolean init ) throws IOException {
		String applicationName = this.agentData.getApplicationName();

		// Process the exports
		// Step 1: get components and facets names to find the queue names.
		// Step 2: subscribe to notifications for when an instance needs variables this instance exports.
		// Step 3: publish the variables this instance exports.
		Map<String,String> instanceExports = InstanceHelpers.getExportedVariables( instance );

		Set<String> alreadyProcessedPrefixes = new HashSet<String> ();
		for( String exportedVariableName : instanceExports.keySet()) {

			// This is step 1.
			String facetOrComponentName = VariableHelpers.parseVariableName( exportedVariableName ).getKey();

			// This is step 2.
			if( alreadyProcessedPrefixes.contains( facetOrComponentName ))
				continue;

			alreadyProcessedPrefixes.add( facetOrComponentName );
			if( init )
				configureExports( applicationName, facetOrComponentName, instance );
			else
				unconfigureExports( applicationName, facetOrComponentName, instance );
		}


		// Process the imports
		// Step 1: get components and facets names to find the queue names.
		// Step 2: subscribe to notifications for when an instance exports variables this instance needs.
		// Step 3: publish a notification indicating which variables this instance needs.
		alreadyProcessedPrefixes.clear();
		for( String importedVariableName : instance.getComponent().getImportedVariableNames()) {

			// This is step 1.
			String facetOrComponentName = VariableHelpers.parseVariableName( importedVariableName ).getKey();

			// This is step 2.
			if( alreadyProcessedPrefixes.contains( facetOrComponentName ))
				continue;

			alreadyProcessedPrefixes.add( facetOrComponentName );
			if( init )
				configureImports( applicationName, facetOrComponentName, importedVariableName, instance );
			else
				unconfigureImports( applicationName, facetOrComponentName, instance );
		}
	}



	private void unconfigureImports( String applicationName, String facetOrComponentName, Instance instance ) throws IOException {

		this.logger.fine( "Instance " + instance.getName() + " is unsubscribing to components that export variables facetOrComponentNameed by " + facetOrComponentName + "." );
		this.client.unsubscribeTo( InteractionType.AGENT_TO_AGENT, EXPORTS + facetOrComponentName );
	}


	private void configureImports( String applicationName, String facetOrComponentName, String importedVariableName, Instance instance ) throws IOException {

		this.logger.fine( "Instance " + instance.getName() + " is subscribing to components that export variables facetOrComponentNameed by " + facetOrComponentName + "." );
		this.client.subscribeTo( InteractionType.AGENT_TO_AGENT, EXPORTS + facetOrComponentName );

		// This is step 3.
		this.logger.fine( "Instance " + instance.getName() + " is notifying other components about the variables it needs." );
		MsgCmdImportNotification message = new MsgCmdImportNotification( importedVariableName, null );
		this.client.publish( InteractionType.AGENT_TO_AGENT, IMPORTS + facetOrComponentName, message );
	}


	private void unconfigureExports( String applicationName, String facetOrComponentName, Instance instance ) throws IOException {

		this.logger.fine( "Instance " + instance.getName() + " is unsubscribing from components that need variables facetOrComponentNameed by " + facetOrComponentName + "." );
		this.client.unsubscribeTo( InteractionType.AGENT_TO_AGENT, IMPORTS + facetOrComponentName );

		// This is step 3.
		// FIXME: maybe we should filter the map to only keep the required variables. For security?
		this.logger.fine( "Instance " + instance.getName() + " is exporting its variables on the messaging server." );
		MsgCmdImportRemove message = new MsgCmdImportRemove( facetOrComponentName, instance.getName());	// FIXME: review the parameters
		this.client.publish( InteractionType.AGENT_TO_AGENT, EXPORTS + facetOrComponentName, message );
	}


	private void configureExports( String applicationName, String facetOrComponentName, Instance instance ) throws IOException {

		this.logger.fine( "Instance " + instance.getName() + " is subscribing to components that need variables facetOrComponentNameed by " + facetOrComponentName + "." );
		this.client.subscribeTo( InteractionType.AGENT_TO_AGENT, IMPORTS + facetOrComponentName );

		// This is step 3.
		// FIXME: maybe we should filter the map to only keep the required variables. For security?
		this.logger.fine( "Instance " + instance.getName() + " is exporting its variables on the messaging server." );

		Map<String,String> instanceExports = InstanceHelpers.getExportedVariables( instance );
		MsgCmdImportAdd message = new MsgCmdImportAdd( facetOrComponentName, instance.getName(), instanceExports );
		this.client.publish( InteractionType.AGENT_TO_AGENT, EXPORTS + facetOrComponentName, message );
	}
}
