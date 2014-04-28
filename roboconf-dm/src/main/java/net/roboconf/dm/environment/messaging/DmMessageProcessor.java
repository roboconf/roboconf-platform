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

import java.io.IOException;
import java.util.logging.Logger;

import net.roboconf.core.internal.utils.Utils;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.runtime.Application;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.core.model.runtime.Instance.InstanceStatus;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.messaging.client.AbstractMessageProcessor;
import net.roboconf.messaging.messages.Message;
import net.roboconf.messaging.messages.from_agent_to_dm.MsgNotifHeartbeat;
import net.roboconf.messaging.messages.from_agent_to_dm.MsgNotifInstanceChanged;
import net.roboconf.messaging.messages.from_agent_to_dm.MsgNotifInstanceRemoved;
import net.roboconf.messaging.messages.from_agent_to_dm.MsgNotifMachineDown;
import net.roboconf.messaging.messages.from_agent_to_dm.MsgNotifMachineReadyToBeDeleted;
import net.roboconf.messaging.messages.from_agent_to_dm.MsgNotifMachineUp;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdInstanceAdd;

/**
 * This class is in charge of updating the model from messages / notifications.
 * <p>
 * These messages have been sent by an agent.
 * </p>
 *
 * @author Noël - LIG
 */
public class DmMessageProcessor extends AbstractMessageProcessor {

	private final Logger logger = Logger.getLogger( DmMessageProcessor.class.getName());



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

		else if( message instanceof MsgNotifMachineReadyToBeDeleted )
			processMsgNotifReadyToBeDeleted((MsgNotifMachineReadyToBeDeleted) message );

		else
			this.logger.warning( "The DM got an undetermined message to process: " + message.getClass().getName());
	}



	private void processMsgNotifReadyToBeDeleted( MsgNotifMachineReadyToBeDeleted message ) {

		String rootInstanceName = message.getRootInstanceName();
		Application app = Manager.INSTANCE.findApplicationByName( message.getApplicationName());
		Instance rootInstance = InstanceHelpers.findInstanceByPath( app, "/" + rootInstanceName );

		// If 'app' is null, then 'instance' is also null.
		if( rootInstance == null ) {
			StringBuilder sb = new StringBuilder();
			sb.append( "A machine signaled it is ready to be deleted, but this machine is unknown: " );
			sb.append( rootInstanceName );
			sb.append( " (app =  " );
			sb.append( app );
			sb.append( ")." );
			this.logger.warning( sb.toString());

		} else {
			Manager.INSTANCE.terminateMachine( app.getName(), rootInstance );
			this.logger.fine( "Machine " + rootInstanceName + " is ready to be deleted." );
		}
	}


	private void processMsgNotifMachineUp( MsgNotifMachineUp message ) {

		String ipAddress = message.getIpAddress();
		String rootInstanceName = message.getRootInstanceName();
		Application app = Manager.INSTANCE.findApplicationByName( message.getApplicationName());
		Instance rootInstance = InstanceHelpers.findInstanceByPath( app, "/" + rootInstanceName );

		// If 'app' is null, then 'instance' is also null.
		if( rootInstance == null ) {
			StringBuilder sb = new StringBuilder();
			sb.append( "An 'UP' notification was received from an unknown machine: " );
			sb.append( rootInstanceName );
			sb.append( " @ " );
			sb.append( ipAddress );
			sb.append( " (app =  " );
			sb.append( app );
			sb.append( ")." );
			this.logger.warning( sb.toString());

		} else {
			rootInstance.setStatus( InstanceStatus.DEPLOYED_STARTED );
			rootInstance.getData().put( Instance.IP_ADDRESS, ipAddress );
			this.logger.fine( rootInstanceName + " @ " + ipAddress + " is up and running." );

			try {
				MsgCmdInstanceAdd newMsg = new MsgCmdInstanceAdd((String) null, rootInstance );
				Manager.INSTANCE.sendModelToAgent( app, rootInstance, newMsg );

			} catch( IOException e ) {
				this.logger.severe( "The DM failed to send the agent's model for " + rootInstanceName + ". " + e.getMessage());
				this.logger.finest( Utils.writeException( e ));
			}
		}
	}


	private void processMsgNotifMachineDown( MsgNotifMachineDown message ) {

		String rootInstanceName = message.getRootInstanceName();
		Application app = Manager.INSTANCE.findApplicationByName( message.getApplicationName());
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
		ManagedApplication ma = Manager.INSTANCE.getAppNameToManagedApplication().get( message.getApplicationName());
		Application app = ma == null ? null : ma.getApplication();
		Instance rootInstance = InstanceHelpers.findInstanceByPath( app, "/" + rootInstanceName );

		// If 'app' is null, then 'instance' is also null.
		if( rootInstance == null ) {
			StringBuilder sb = new StringBuilder();
			sb.append( "A 'HEART BEAT' was received from an unknown machine: " );
			sb.append( rootInstanceName );
			sb.append( " (app =  " );
			sb.append( app );
			sb.append( ")." );
			this.logger.warning( sb.toString());

		} else {
			ma.getMonitor().acknowledgeHeartBeat( rootInstance );
			ma.getLogger().finest( "A heart beat was acknowledged for " + rootInstance.getName() + " in the application " + app.getName() + "." );
		}
	}


	private void processMsgNotifInstanceChanged( MsgNotifInstanceChanged message ) {

		String instancePath = message.getInstancePath();
		Application app = Manager.INSTANCE.findApplicationByName( message.getApplicationName());
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

		String instancePath = message.getInstancePath();
		Application app = Manager.INSTANCE.findApplicationByName( message.getApplicationName());
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
}
