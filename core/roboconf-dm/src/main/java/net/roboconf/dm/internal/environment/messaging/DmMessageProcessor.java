/**
 * Copyright 2013-2017 Linagora, Université Joseph Fourier, Floralis
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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.core.model.helpers.ImportHelpers;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.utils.DockerAndScriptUtils;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.internal.utils.DmUtils;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.messaging.api.AbstractMessageProcessor;
import net.roboconf.messaging.api.business.IDmClient;
import net.roboconf.messaging.api.messages.Message;
import net.roboconf.messaging.api.messages.from_agent_to_dm.MsgNotifAutonomic;
import net.roboconf.messaging.api.messages.from_agent_to_dm.MsgNotifHeartbeat;
import net.roboconf.messaging.api.messages.from_agent_to_dm.MsgNotifInstanceChanged;
import net.roboconf.messaging.api.messages.from_agent_to_dm.MsgNotifInstanceRemoved;
import net.roboconf.messaging.api.messages.from_agent_to_dm.MsgNotifLogs;
import net.roboconf.messaging.api.messages.from_agent_to_dm.MsgNotifMachineDown;
import net.roboconf.messaging.api.messages.from_dm_to_agent.MsgCmdSetScopedInstance;
import net.roboconf.messaging.api.messages.from_dm_to_dm.MsgEcho;

/**
 * This class is in charge of updating the model from messages / notifications.
 * <p>
 * These messages have been sent by an agent.
 * </p>
 *
 * @author Noël - LIG
 * @author Pierre Bourret - Université Joseph Fourier
 * @author Amadou Diarra - UGA
 */
public class DmMessageProcessor extends AbstractMessageProcessor<IDmClient> {

	private final Logger logger = Logger.getLogger( DmMessageProcessor.class.getName());
	private final Manager manager;

	// Set as a class attribute so that it can be replaced for unit tests.
	String tmpDir = System.getProperty( "java.io.tmpdir" );


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
	 * @see net.roboconf.messaging.api.business.AbstractMessageProcessor
	 * #processMessage(net.roboconf.messaging.api.messages.Message)
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

		else if(message instanceof MsgNotifAutonomic)
			processMsgMonitoringEvent((MsgNotifAutonomic) message );

		else if( message instanceof MsgEcho )
			this.manager.debugMngr().notifyMsgEchoReceived((MsgEcho) message );

		else if( message instanceof MsgNotifLogs )
			processMsgNotifLogs((MsgNotifLogs) message );

		else
			this.logger.warning( "The DM got an undetermined message to process: " + message.getClass().getName());
	}


	private void processMsgNotifLogs( MsgNotifLogs message ) {

		StringBuilder path = new StringBuilder();
		path.append( "roboconf-logs/" );
		path.append( message.getApplicationName());
		path.append( "/" );
		path.append( DockerAndScriptUtils.cleanInstancePath( message.getScopedInstancePath()));

		// Dump these messages in the temporary directory...
		File dumpDir = new File( this.tmpDir, path.toString());
		try {
			Utils.createDirectory( dumpDir );
			for( Map.Entry<String,byte[]> entry : message.getLogFiles().entrySet()) {
				ByteArrayInputStream in = new ByteArrayInputStream( entry.getValue());
				Utils.copyStream( in, new File( dumpDir, entry.getKey()));
			}

		} catch( IOException e ) {
			StringBuilder sb = new StringBuilder();
			sb.append( "An error occurred while dumping logs from agent " );
			sb.append( message.getScopedInstancePath());
			sb.append( " @ " );
			sb.append( message.getApplicationName());
			sb.append( ". " );
			if( ! Utils.isEmptyOrWhitespaces( e.getMessage()))
				sb.append( e.getMessage());

			this.logger.severe( sb.toString());
			Utils.logException( this.logger, e );
		}
	}


	private void processMsgNotifMachineDown( MsgNotifMachineDown message ) {

		String scopedInstancePath = message.getScopedInstancePath();
		ManagedApplication ma = this.manager.applicationMngr().findManagedApplicationByName( message.getApplicationName());
		Application app = ma == null ? null : ma.getApplication();
		Instance scopedInstance = InstanceHelpers.findInstanceByPath( app, scopedInstancePath );

		// If 'app' is null, then 'instance' is also null.
		if( scopedInstance == null ) {
			StringBuilder sb = new StringBuilder();
			sb.append( "A 'DOWN' notification was received from an unknown agent: " );
			sb.append( scopedInstancePath );
			sb.append( " (app = " );
			sb.append( app );
			sb.append( ")." );
			this.logger.warning( sb.toString());

		} else {
			DmUtils.markScopedInstanceAsNotDeployed( scopedInstance, ma, this.manager.notificationMngr(), this.manager.instancesMngr());
			this.logger.info( scopedInstance + " is now terminated. Back to NOT_DEPLOYED state." );
		}
	}



	private void processMsgNotifHeartbeat( MsgNotifHeartbeat message ) {

		String scopedInstancePath = message.getScopedInstancePath();
		ManagedApplication ma = this.manager.applicationMngr().findManagedApplicationByName( message.getApplicationName());
		Application app = ma == null ? null : ma.getApplication();
		Instance scopedInstance = InstanceHelpers.findInstanceByPath( app, scopedInstancePath );

		if( scopedInstance == null ) {
			// If 'app' is null, then 'instance' is also null.
			StringBuilder sb = new StringBuilder();
			sb.append( "A 'HEART BEAT' was received from an unknown agent: " );
			sb.append( scopedInstancePath );
			sb.append( " (app = " );
			sb.append( ma );
			sb.append( ", inst = " );
			sb.append( scopedInstancePath );
			sb.append( "). The heart beat is dropped." );
			this.logger.warning( sb.toString());

		} else if( ! InstanceHelpers.isTarget( scopedInstance )) {
			StringBuilder sb = new StringBuilder();
			sb.append( "A 'HEART BEAT' was received for a non-scoped instance: " );
			sb.append( scopedInstancePath );
			sb.append( " (app = " );
			sb.append( ma );
			sb.append( ", inst = " );
			sb.append( scopedInstancePath );
			sb.append( "). The heart beat is dropped." );
			this.logger.warning( sb.toString());

		} else {
			// Update the data
			String ipAddress = message.getIpAddress();
			boolean ipWasSet = false;
			if( scopedInstance.data.get( Instance.IP_ADDRESS ) == null ) {
				this.logger.fine( scopedInstancePath + " @ " + ipAddress + " is up and running." );
				scopedInstance.data.put( Instance.IP_ADDRESS, ipAddress );
				ipWasSet = true;
			}

			// Log and notify
			this.logger.finest( "A heart beat was acknowledged for " + scopedInstancePath + " in the application " + ma + "." );
			InstanceStatus oldStatus = scopedInstance.getStatus();
			ma.acknowledgeHeartBeat( scopedInstance );
			if( ipWasSet || oldStatus != scopedInstance.getStatus())
				this.manager.instancesMngr().instanceWasUpdated( scopedInstance, ma );

			try {
				// Need to send the model to the agent?
				// A heart beat may also say whether the agent received its model.
				if( message.isModelRequired()) {
					this.logger.fine( "The DM is sending its model to agent " + scopedInstancePath + "." );
					Map<String,byte[]> scriptResources = this.manager.targetsMngr().findScriptResourcesForAgent( ma.getApplication(), scopedInstance );
					Message msg = new MsgCmdSetScopedInstance(
							scopedInstance,
							app.getExternalExports(),
							app.getApplicationBindings(),
							scriptResources);

					this.messagingClient.sendMessageToAgent( ma.getApplication(), scopedInstance, msg );
				}

				// Send stored messages after an acknowledgement
				this.manager.messagingMngr().sendStoredMessages( ma, scopedInstance );

			} catch( IOException e ) {
				this.logger.warning( "Agent " + scopedInstancePath + " requested its model but an error occurred. " + e.getMessage());
				Utils.logException( this.logger, e );
			}
		}
	}


	private void processMsgNotifInstanceChanged( MsgNotifInstanceChanged message ) {

		String instancePath = message.getInstancePath();
		ManagedApplication ma = this.manager.applicationMngr().findManagedApplicationByName( message.getApplicationName());
		Application app = ma == null ? null : ma.getApplication();
		Instance instance = InstanceHelpers.findInstanceByPath( app, instancePath );

		// If 'app' is null, then 'instance' is also null.
		if( instance == null ) {
			StringBuilder sb = new StringBuilder();
			sb.append( "A 'CHANGED' notification was received from an unknown instance: " );
			sb.append( instancePath );
			sb.append( " (app = " );
			sb.append( app );
			sb.append( ")." );
			this.logger.warning( sb.toString());

		} else if( InstanceHelpers.findRootInstance( instance ).getStatus() == InstanceStatus.NOT_DEPLOYED ) {
			// See roboconf-platform #107
			StringBuilder sb = new StringBuilder();
			sb.append( "A 'CHANGED' notification was received from a instance: " );
			sb.append( instancePath );
			sb.append( " (app = " );
			sb.append( app );
			sb.append( ") but the root instance is not deployed. Status update is dismissed." );
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

			// Notify the changes!
			this.manager.instancesMngr().instanceWasUpdated( instance, ma );
		}
	}


	private void processMsgNotifInstanceRemoved( MsgNotifInstanceRemoved message ) {

		String instancePath = message.getInstancePath();
		Application app = this.manager.applicationMngr().findApplicationByName( message.getApplicationName());
		Instance instance = InstanceHelpers.findInstanceByPath( app, instancePath );

		// If 'app' is null, then 'instance' is also null.
		if( instance == null ) {
			StringBuilder sb = new StringBuilder();
			sb.append( "A 'REMOVE' notification was received for an unknown instance: " );
			sb.append( instancePath );
			sb.append( " (app = " );
			sb.append( app );
			sb.append( ")." );
			this.logger.warning( sb.toString());

		} else {
			if( InstanceHelpers.isTarget( instance ))
				this.logger.warning( "Anormal behavior. A 'REMOVE' notification was received for a scoped instance: " + instancePath + "." );
			else
				instance.getParent().getChildren().remove( instance );

			this.logger.info( "Instance " + instancePath + " was removed from the model." );
		}
	}


	private void processMsgMonitoringEvent( MsgNotifAutonomic message ) {

		Application app = this.manager.applicationMngr().findApplicationByName( message.getApplicationName());
		Instance scopedInstance = InstanceHelpers.findInstanceByPath( app, message.getScopedInstancePath());

		// If 'app' is null, then 'instance' is also null.
		if( scopedInstance == null ) {
			StringBuilder sb = new StringBuilder();
			sb.append( "A notification associated with autonomic management was received for an unknown instance: " );
			sb.append( message.getScopedInstancePath());
			sb.append( " (app = " );
			sb.append( app );
			sb.append( ")." );
			this.logger.warning( sb.toString());

		} else {
			ManagedApplication ma = this.manager.applicationMngr().findManagedApplicationByName( app.getName());
			this.manager.autonomicMngr().handleEvent( ma, message );
		}
	}
}
