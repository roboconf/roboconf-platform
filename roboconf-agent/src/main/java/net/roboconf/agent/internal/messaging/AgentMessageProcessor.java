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

package net.roboconf.agent.internal.messaging;

import java.util.logging.Logger;

import net.roboconf.agent.internal.Agent;
import net.roboconf.core.actions.ApplicationAction;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.messaging.client.IMessageProcessor;
import net.roboconf.messaging.messages.Message;
import net.roboconf.messaging.messages.from_agent_to_agent.MsgCmdImportAdd;
import net.roboconf.messaging.messages.from_agent_to_agent.MsgCmdImportNotification;
import net.roboconf.messaging.messages.from_agent_to_agent.MsgCmdImportRemove;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdInstanceAdd;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdInstanceDeploy;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdInstanceRemove;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdInstanceStart;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdInstanceStop;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdInstanceUndeploy;

/**
 * The message processor for the agent.
 * <p>
 * Unlike the DM, the main agent's thread is in charge of listening to new messages.
 * Real agent actions are performed sequentially. When the agent receives a message, it processes
 * it before reading the next one.
 * </p>
 *
 * @author Noël - LIG
 * @author Pierre-Yves Gibello - Linagora
 * @author Vincent Zurczak - Linagora
 */
public final class AgentMessageProcessor implements IMessageProcessor {

	private final Agent agent;
	private final Logger logger = Logger.getLogger( getClass().getName());


	/**
	 * Constructor.
	 * @param agent
	 */
	public AgentMessageProcessor( Agent agent ) {
		this.agent = agent;
	}


	/**
	 * Processes a message (dispatch method).
	 * @param message a message (not null)
	 */
	@Override
	public void processMessage( Message message ) {

		if( message instanceof MsgCmdInstanceAdd )
			processMsgInstanceAdd((MsgCmdInstanceAdd) message );

		else if( message instanceof MsgCmdInstanceRemove )
			processMsgInstanceRemove((MsgCmdInstanceRemove) message );

		else if( message instanceof MsgCmdInstanceDeploy )
			processMsgInstanceDeploy((MsgCmdInstanceDeploy) message );

		else if( message instanceof MsgCmdInstanceUndeploy )
			processMsgInstanceUndeploy((MsgCmdInstanceUndeploy) message );

		else if( message instanceof MsgCmdInstanceStart )
			processMsgInstanceStart((MsgCmdInstanceStart) message );

		else if( message instanceof MsgCmdInstanceStop )
			processMsgInstanceStop((MsgCmdInstanceStop) message );

		else if( message instanceof MsgCmdImportAdd )
			processMsgImportAdd((MsgCmdImportAdd) message );

		else if( message instanceof MsgCmdImportRemove )
			processMsgImportRemove((MsgCmdImportRemove) message );

		else if( message instanceof MsgCmdImportNotification )
			processMsgImportNotification((MsgCmdImportNotification) message );

		else
			this.logger.warning( "Got an undetermined message to process: " + message.getClass().getName());
	}


	private void processMsgImportNotification( MsgCmdImportNotification msg ) {

//		String instancePath = msg.getInstancePath();
//		this.logger.fine( "Removing instance " + instancePath + "." );
//		this.agent.performAction( ApplicationAction.remove, instancePath );
	}

	private void processMsgImportRemove( MsgCmdImportRemove msg ) {

//		String instancePath = msg.getInstancePath();
//		this.logger.fine( "Removing instance " + instancePath + "." );
//		this.agent.performAction( ApplicationAction.remove, instancePath );
	}

	private void processMsgImportAdd( MsgCmdImportAdd msg ) {

//		String instancePath = msg.getInstancePath();
//		this.logger.fine( "Removing instance " + instancePath + "." );
//		this.agent.performAction( ApplicationAction.remove, instancePath );
	}

	private void processMsgInstanceAdd( MsgCmdInstanceAdd msg ) {

		Instance newInstance = msg.getInstanceToAdd();
		String parentInstancePath = msg.getParentInstancePath();

		this.logger.fine( "Adding instance " + newInstance.getName() + " under " + parentInstancePath + "." );
		this.agent.addInstance( parentInstancePath, newInstance );
	}


	private void processMsgInstanceRemove( MsgCmdInstanceRemove msg ) {

		String instancePath = msg.getInstancePath();
		this.logger.fine( "Removing instance " + instancePath + "." );
		this.agent.performAction( ApplicationAction.remove, instancePath );
	}


	private void processMsgInstanceDeploy( MsgCmdInstanceDeploy msg ) {

		String instancePath = msg.getInstancePath();
		this.logger.fine( "Deploying instance " + instancePath + "." );
		this.agent.performAction( ApplicationAction.deploy, instancePath );
	}


	private void processMsgInstanceUndeploy( MsgCmdInstanceUndeploy msg ) {

		String instancePath = msg.getInstancePath();
		this.logger.fine( "Undeploying instance " + instancePath + "." );
		this.agent.performAction( ApplicationAction.undeploy, instancePath );
	}


	private void processMsgInstanceStart( MsgCmdInstanceStart msg ) {

		String instancePath = msg.getInstancePath();
		this.logger.fine( "Starting instance " + instancePath + "." );
		this.agent.performAction( ApplicationAction.start, instancePath );
	}


	private void processMsgInstanceStop( MsgCmdInstanceStop msg ) {

		String instancePath = msg.getInstancePath();
		this.logger.fine( "Stopping instance " + instancePath + "." );
		this.agent.performAction( ApplicationAction.stop, instancePath );
	}
}
