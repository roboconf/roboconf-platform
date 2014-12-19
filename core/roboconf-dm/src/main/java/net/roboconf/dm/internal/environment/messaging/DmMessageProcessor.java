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

package net.roboconf.dm.internal.environment.messaging;

import java.io.IOException;
import java.util.logging.Logger;

import net.roboconf.core.model.helpers.ImportHelpers;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.runtime.Application;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.core.model.runtime.Instance.InstanceStatus;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.internal.autonomic.RuleBasedEventHandler;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.messaging.client.IDmClient;
import net.roboconf.messaging.messages.Message;
import net.roboconf.messaging.messages.from_agent_to_dm.MsgNotifAutonomic;
import net.roboconf.messaging.messages.from_agent_to_dm.MsgNotifHeartbeat;
import net.roboconf.messaging.messages.from_agent_to_dm.MsgNotifInstanceChanged;
import net.roboconf.messaging.messages.from_agent_to_dm.MsgNotifInstanceRemoved;
import net.roboconf.messaging.messages.from_agent_to_dm.MsgNotifMachineDown;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdSetRootInstance;
import net.roboconf.messaging.processors.AbstractMessageProcessor;

/**
 * This class is in charge of updating the model from messages / notifications.
 * <p>
 * These messages have been sent by an agent.
 * </p>
 *
 * @author Noël - LIG
 */
public class DmMessageProcessor extends AbstractMessageProcessor<IDmClient> {

	private final Logger logger = Logger.getLogger( DmMessageProcessor.class.getName());
	private final Manager manager;


	/**
	 * Constructor.
	 * @param manager
	 */
	public DmMessageProcessor( Manager manager ) {
		super( "Roboconf DM - Message Processor" );
		this.manager = manager;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.messaging.client.AbstractMessageProcessor
	 * #processMessage(net.roboconf.messaging.messages.Message)
	 */
	@Override
	public void processMessage( Message message ) {

		if( message instanceof MsgNotifMachineDown )
			processMsgNotifMachineDown((MsgNotifMachineDown) message );

		else if( message instanceof MsgNotifInstanceChanged )
			processMsgNotifInstanceChanged((MsgNotifInstanceChanged) message );

		else if( message instanceof MsgNotifInstanceRemoved )
			processMsgNotifInstanceRemoved((MsgNotifInstanceRemoved) message );

		else if( message instanceof MsgNotifHeartbeat )
			processMsgNotifHeartbeat((MsgNotifHeartbeat) message );
		
		else if(message instanceof MsgNotifAutonomic) {
			processMsgMonitoringEvent((MsgNotifAutonomic)message);
		}

		else
			this.logger.warning( "The DM got an undetermined message to process: " + message.getClass().getName());
	}


	private void processMsgNotifMachineDown( MsgNotifMachineDown message ) {

		String rootInstanceName = message.getRootInstanceName();
		Application app = this.manager.findApplicationByName( message.getApplicationName());
		Instance rootInstance = InstanceHelpers.findInstanceByPath( app, "/" + rootInstanceName );

		// If 'app' is null, then 'instance' is also null.
		if( rootInstance == null ) {
			StringBuilder sb = new StringBuilder();
			sb.append( "A 'DOWN' notification was received from an unknown machine: " );
			sb.append( rootInstanceName );
			sb.append( " (app =  " );
			sb.append( app );
			sb.append( ")." );
			this.logger.warning( sb.toString());

		} else {
			rootInstance.setStatus( InstanceStatus.NOT_DEPLOYED );
			this.logger.info( rootInstanceName + " is now terminated. Back to NOT_DEPLOYED state." );
		}
	}



	private void processMsgNotifHeartbeat( MsgNotifHeartbeat message ) {

		String rootInstanceName = message.getRootInstanceName();
		ManagedApplication ma = this.manager.getAppNameToManagedApplication().get( message.getApplicationName());
		Application app = ma == null ? null : ma.getApplication();
		Instance rootInstance = InstanceHelpers.findInstanceByPath( app, "/" + rootInstanceName );

		if( rootInstance == null ) {
			// If 'app' is null, then 'instance' is also null.
			StringBuilder sb = new StringBuilder();
			sb.append( "A 'HEART BEAT' was received from an unknown machine: " );
			sb.append( rootInstanceName );
			sb.append( " (app =  " );
			sb.append( app );
			sb.append( ")." );
			this.logger.warning( sb.toString());

		} else {
			// Update the data
			String ipAddress = message.getIpAddress();
			if( rootInstance.getData().get( Instance.IP_ADDRESS ) == null ) {
				this.logger.fine( rootInstanceName + " @ " + ipAddress + " is up and running." );
				rootInstance.getData().put( Instance.IP_ADDRESS, ipAddress );
				this.manager.saveConfiguration( ma );
			}

			ma.acknowledgeHeartBeat( rootInstance );
			this.logger.finest( "A heart beat was acknowledged for " + rootInstance.getName() + " in the application " + app.getName() + "." );

			// A heart beat may also say whether the agent receive its model
			try {
				if( message.isModelRequired()) {
					this.logger.info( "The DM is sending its model to agent " + rootInstanceName + "." );
					this.messagingClient.sendMessageToAgent( app, rootInstance, new MsgCmdSetRootInstance( rootInstance ));
				}

			} catch( IOException e ) {
				this.logger.warning( "Agent " + rootInstanceName + " requested its model but an error occurred. " + e.getMessage());
				Utils.logException( this.logger, e );
			}
		}
	}


	private void processMsgNotifInstanceChanged( MsgNotifInstanceChanged message ) {

		String instancePath = message.getInstancePath();
		Application app = this.manager.findApplicationByName( message.getApplicationName());
		Instance instance = InstanceHelpers.findInstanceByPath( app, instancePath );

		// If 'app' is null, then 'instance' is also null.
		if( instance == null ) {
			StringBuilder sb = new StringBuilder();
			sb.append( "A 'CHANGED' notification was received from an unknown instance: " );
			sb.append( instancePath );
			sb.append( " (app =  " );
			sb.append( app );
			sb.append( ")." );
			this.logger.warning( sb.toString());

		} else {
			InstanceStatus oldStatus = instance.getStatus();
			instance.setStatus( message.getNewStatus());
			ImportHelpers.updateImports( instance, message.getNewImports());

			StringBuilder sb = new StringBuilder();
			sb.append( "Status changed from " );
			sb.append( oldStatus );
			sb.append( " to " );
			sb.append( message.getNewStatus() );
			sb.append( " for instance " );
			sb.append( instancePath );
			sb.append( ". Imports were updated too." );
			this.logger.fine( sb.toString());
		}
	}


	private void processMsgNotifInstanceRemoved( MsgNotifInstanceRemoved message ) {

		String instancePath = message.getInstancePath();
		Application app = this.manager.findApplicationByName( message.getApplicationName());
		Instance instance = InstanceHelpers.findInstanceByPath( app, instancePath );

		// If 'app' is null, then 'instance' is also null.
		if( instance == null ) {
			StringBuilder sb = new StringBuilder();
			sb.append( "A 'REMOVE' notification was received for an unknown instance: " );
			sb.append( instancePath );
			sb.append( " (app =  " );
			sb.append( app );
			sb.append( ")." );
			this.logger.warning( sb.toString());

		} else {
			if( instance.getParent() == null )
				this.logger.warning( "Anormal behavior. A 'REMOVE' notification was received for a root instance: " + instancePath + "." );
			else
				instance.getParent().getChildren().remove( instance );

			this.logger.info( "Instance " + instancePath + " was removed from the model." );
		}
	}
	
	private void processMsgMonitoringEvent(MsgNotifAutonomic message) {
		this.logger.info("Autonomic monitoring listener: EVENT " + message.getEventName());
		try {
			RuleBasedEventHandler handler = new RuleBasedEventHandler(manager, manager.getManagedApplication(message.getApplicationName()));
			handler.handleEvent(message);
		} catch (Exception e) {
			this.logger.warning("Can\'t process rule-based event: " + e.getMessage());
		}
	}
}
