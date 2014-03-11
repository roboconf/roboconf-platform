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

package net.roboconf.dm.environment.messaging;

import java.util.logging.Logger;

import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.runtime.Application;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.core.model.runtime.Instance.InstanceStatus;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.management.exceptions.InexistingException;
import net.roboconf.messaging.client.IMessageProcessor;
import net.roboconf.messaging.messages.Message;
import net.roboconf.messaging.messages.from_agent_to_dm.MsgNotifHeartbeat;
import net.roboconf.messaging.messages.from_agent_to_dm.MsgNotifInstanceChanged;
import net.roboconf.messaging.messages.from_agent_to_dm.MsgNotifInstanceRemoved;
import net.roboconf.messaging.messages.from_agent_to_dm.MsgNotifMachineDown;
import net.roboconf.messaging.messages.from_agent_to_dm.MsgNotifMachineUp;

/**
 * This class is in charge of updating the model from messages / notifications.
 * <p>
 * These messages have been sent by an agent.
 * </p>
 *
 * @author Noël - LIG
 */
public class DmMessageProcessor implements IMessageProcessor {

	private final Application application;
	private final Logger logger = Logger.getLogger( DmMessageProcessor.class.getName());


	/**
	 * Constructor.
	 * @param application
	 */
	public DmMessageProcessor( Application application ) {
		this.application = application;
	}


	/**
	 * Processes a message (dispatch method).
	 * @param message (not null)
	 */
	@Override
	public void processMessage( Message message ) {

		if( message instanceof MsgNotifMachineUp )
			processMsgNotifMachineUp((MsgNotifMachineUp) message );

		else if( message instanceof MsgNotifMachineDown )
			processMsgNotifMachineDown((MsgNotifMachineDown) message );

		else if( message instanceof MsgNotifInstanceChanged )
			processMsgNotifInstanceChanged((MsgNotifInstanceChanged) message );

		else if( message instanceof MsgNotifInstanceRemoved )
			processMsgNotifInstanceRemoved((MsgNotifInstanceRemoved) message );

		else if( message instanceof MsgNotifHeartbeat )
			processMsgNotifHeartbeat((MsgNotifHeartbeat) message );

		else
			this.logger.warning( "Got an undetermined message to process: " + message.getClass().getName());
	}


	private void processMsgNotifMachineUp( MsgNotifMachineUp message ) {

		String ipAddress = message.getIpAddress();
		String rootInstanceName = message.getRootInstanceName();
		Instance rootInstance = InstanceHelpers.findInstanceByPath( this.application, "/" + rootInstanceName );

		if( rootInstance == null ) {
			StringBuilder sb = new StringBuilder();
			sb.append( "An 'UP' notification was received from an unknown machine: " );
			sb.append( rootInstanceName );
			sb.append( " @ " );
			sb.append( ipAddress );
			this.logger.warning( sb.toString());

		} else {
			rootInstance.setStatus( InstanceStatus.DEPLOYED_STARTED );
			rootInstance.getData().put( Instance.IP_ADDRESS, ipAddress );
			this.logger.fine( rootInstanceName + " @ " + ipAddress + " is up and running." );
		}
	}


	private void processMsgNotifMachineDown( MsgNotifMachineDown message ) {

		String rootInstanceName = message.getRootInstanceName();
		Instance rootInstance = InstanceHelpers.findInstanceByPath( this.application, "/" + rootInstanceName );

		if( rootInstance == null ) {
			this.logger.warning( "A 'DOWN' notification was received from an unknown machine: " + rootInstanceName + "." );

		} else {
			rootInstance.setStatus( InstanceStatus.NOT_DEPLOYED );
			this.logger.info( rootInstanceName + " is now terminated. Back to INITIALIZED state." );
		}
	}



	private void processMsgNotifHeartbeat( MsgNotifHeartbeat message ) {

		String rootInstanceName = message.getRootInstanceName();
		Instance rootInstance = InstanceHelpers.findInstanceByPath( this.application, "/" + rootInstanceName );

		if( rootInstance == null ) {
			this.logger.warning( "A 'HEART BEAT' was received from an unknown machine: " + rootInstanceName + "." );

		} else {
			try {
				Manager.INSTANCE.acknowledgeHeartBeat( this.application.getName(), rootInstance );
				this.logger.fine( rootInstanceName + " is alive." );

			} catch( InexistingException e ) {
				// This SHOULD NEVER happen.
				// It would mean the Manager is not consistent and that life cycle management screwed up.
				this.logger.warning( "A 'HEART BEAT' was received for an application which was not found: " + this.application.getName() + "." );
			}
		}
	}


	private void processMsgNotifInstanceChanged( MsgNotifInstanceChanged message ) {

		String instancePath = message.getInstancePath();
		Instance instance = InstanceHelpers.findInstanceByPath( this.application, instancePath );

		if( instance == null ) {
			this.logger.warning( "A 'CHANGED' notification was received from an unknown instance: " + instancePath );

		} else {
			InstanceStatus oldStatus = instance.getStatus();
			instance.setStatus( message.getNewStatus());
			instance.updateImports( message.getNewImports());

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
		// TODO Auto-generated method stub

	}
}
