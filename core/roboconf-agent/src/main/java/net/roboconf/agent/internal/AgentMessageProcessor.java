/**
 * Copyright 2014-2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.agent.internal;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import net.roboconf.agent.internal.lifecycle.AbstractLifeCycleManager;
import net.roboconf.agent.internal.misc.AgentUtils;
import net.roboconf.core.Constants;
import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.core.model.beans.Component;
import net.roboconf.core.model.beans.Import;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.core.model.helpers.ComponentHelpers;
import net.roboconf.core.model.helpers.ImportHelpers;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.helpers.VariableHelpers;
import net.roboconf.core.utils.Utils;
import net.roboconf.messaging.api.AbstractMessageProcessor;
import net.roboconf.messaging.api.MessagingConstants;
import net.roboconf.messaging.api.business.IAgentClient;
import net.roboconf.messaging.api.business.ListenerCommand;
import net.roboconf.messaging.api.messages.Message;
import net.roboconf.messaging.api.messages.from_agent_to_agent.MsgCmdAddImport;
import net.roboconf.messaging.api.messages.from_agent_to_agent.MsgCmdRemoveImport;
import net.roboconf.messaging.api.messages.from_agent_to_agent.MsgCmdRequestImport;
import net.roboconf.messaging.api.messages.from_agent_to_dm.MsgNotifInstanceChanged;
import net.roboconf.messaging.api.messages.from_agent_to_dm.MsgNotifInstanceRemoved;
import net.roboconf.messaging.api.messages.from_agent_to_dm.MsgNotifLogs;
import net.roboconf.messaging.api.messages.from_agent_to_dm.MsgNotifMachineDown;
import net.roboconf.messaging.api.messages.from_dm_to_agent.MsgCmdAddInstance;
import net.roboconf.messaging.api.messages.from_dm_to_agent.MsgCmdChangeBinding;
import net.roboconf.messaging.api.messages.from_dm_to_agent.MsgCmdChangeInstanceState;
import net.roboconf.messaging.api.messages.from_dm_to_agent.MsgCmdChangeLogLevel;
import net.roboconf.messaging.api.messages.from_dm_to_agent.MsgCmdGatherLogs;
import net.roboconf.messaging.api.messages.from_dm_to_agent.MsgCmdRemoveInstance;
import net.roboconf.messaging.api.messages.from_dm_to_agent.MsgCmdResynchronize;
import net.roboconf.messaging.api.messages.from_dm_to_agent.MsgCmdSendInstances;
import net.roboconf.messaging.api.messages.from_dm_to_agent.MsgCmdSetScopedInstance;
import net.roboconf.messaging.api.messages.from_dm_to_agent.MsgCmdUpdateProbeConfiguration;
import net.roboconf.messaging.api.messages.from_dm_to_dm.MsgEcho;
import net.roboconf.plugin.api.PluginException;
import net.roboconf.plugin.api.PluginInterface;

/**
 * The class (thread) in charge of processing messages received by the agent.
 * <p>
 * The key idea in this processor is that the method {@link #processMessage(Message)}
 * CANNOT be interrupted when it is processing a message.
 * </p>
 * <p>
 * The agent can indicate
 * "Hey! I have a new configuration, I have to replace you."
 * But in this case, the agent will not directly replace the processor.
 * Instead, it will wait the current processing to complete. And only then, it will
 * replace the messaging client and the processor.
 * </p>
 *
 * @author Vincent Zurczak - Linagora
 * @author Amadou Diarra - UGA
 */
public class AgentMessageProcessor extends AbstractMessageProcessor<IAgentClient> {

	private final Logger logger = Logger.getLogger( getClass().getName());
	private boolean reset = false;

	protected final Agent agent;
	final AtomicBoolean messageUnderProcessing = new AtomicBoolean( false );
	Instance scopedInstance;

	/**
	 * A map used for external exports.
	 * <p>
	 * Key = template name, value = a set of application names.
	 * </p>
	 */
	final Map<String,Set<String>> applicationBindings = new HashMap<> ();

	/**
	 * A local cache to store external imports, even those that are NOT used by instances.
	 * <p>
	 * Several applications may have the same template, and thus, exports the same variable
	 * to other applications. But only ONE application can be bound with another. From the messaging's
	 * point of view, the agent receives messages from all these applications. It is then up to it
	 * to filter those it can use.
	 * </p>
	 * <p>
	 * The imports that are compliant with {@link #applicationBindings} are directly injected in
	 * the instance imports. But ALL the external imports are cached in this map.
	 * </p>
	 */
	final Map<String,Collection<Import>> applicationNameToExternalExports = new HashMap<> ();



	/**
	 * Constructor.
	 * @param agent
	 */
	public AgentMessageProcessor( Agent agent ) {
		super( "Roboconf Agent - Message Processor" );
		this.agent = agent;
	}


	/*
	 * @see net.roboconf.messaging.api.business.AbstractMessageProcessor
	 * #processMessage(net.roboconf.messaging.api.messages.Message)
	 */
	@Override
	protected void processMessage( Message message ) {

		/*
		 * As a reminder, this message processor was implemented so that only one message
		 * is processed at once. If all of this is sequential, reconfiguration may occur
		 * at any moment. So, there is no simple way to guarantee a resetRequest will be
		 * processed. This is why the checkReset method appears in several places, to handle
		 * the following cases:
		 *
		 * - Reset request occurs while we are about to process a new message.
		 * - Reset request occurs while we have just processed a message.
		 * - Reset request occurs while we not processing any message.
		 */

		// A reset request was received before we start processing a message
		if( checkReset()) {
			this.logger.fine( "A reset was performed. Message " + message.getClass() + " will not be processed." );
			return;
		}

		// Process the message
		this.logger.fine( "A message of type " + message.getClass().getSimpleName() + " was received and is about to be processed." );
		this.messageUnderProcessing.set( true );
		try {
			if( message instanceof MsgCmdSetScopedInstance )
				processMsgSetScopedInstance((MsgCmdSetScopedInstance) message );

			else if( message instanceof MsgCmdRemoveInstance )
				processMsgRemoveInstance((MsgCmdRemoveInstance) message );

			else if( message instanceof MsgCmdAddInstance )
				processMsgAddInstance((MsgCmdAddInstance) message );

			else if( message instanceof MsgCmdChangeInstanceState )
				processMsgChangeInstanceState((MsgCmdChangeInstanceState) message );

			else if( message instanceof MsgCmdAddImport )
				processMsgAddImport((MsgCmdAddImport) message );

			else if( message instanceof MsgCmdRemoveImport )
				processMsgRemoveImport((MsgCmdRemoveImport) message, true );

			else if( message instanceof MsgCmdRequestImport )
				processMsgRequestImport((MsgCmdRequestImport) message );

			else if( message instanceof MsgCmdSendInstances )
				processMsgSendInstances((MsgCmdSendInstances) message );

			else if( message instanceof MsgCmdResynchronize )
				processMsgResynchronize((MsgCmdResynchronize) message );

			else if( message instanceof MsgEcho )
				processMsgEcho((MsgEcho) message );

			else if( message instanceof MsgCmdChangeBinding )
				processMsgChangeBinding((MsgCmdChangeBinding) message );

			else if( message instanceof MsgCmdUpdateProbeConfiguration )
				processUpdateProbeConfiguration((MsgCmdUpdateProbeConfiguration) message );

			else if( message instanceof MsgCmdChangeLogLevel )
				processChangeLogLevel((MsgCmdChangeLogLevel) message );

			else if( message instanceof MsgCmdGatherLogs )
				processGatherLogs((MsgCmdGatherLogs) message );

			else
				this.logger.warning( getName() + " got an undetermined message to process. " + message.getClass().getName());

		} catch( IOException e ) {
			this.logger.severe( "A problem occurred with the messaging. " + e.getMessage());
			Utils.logException( this.logger, e );

		} catch( PluginException e ) {
			this.logger.severe( "A problem occurred with a plug-in. " + e.getMessage());
			Utils.logException( this.logger, e );

		} finally {
			this.messageUnderProcessing.set( false );
		}

		// A reset request was received while we were processing a message
		checkReset();
	}


	/**
	 * Requests a full reset of the agent.
	 */
	public void resetRequest() {
		this.reset = true;
		checkReset();
	}


	/**
	 * @return true if a reset was requested and not yet processed, false otherwise
	 */
	public boolean resetWasRquested() {
		return this.reset;
	}


	/**
	 * Starts a reset if necessary.
	 * @return true if a reset was done, false otherwise
	 */
	private boolean checkReset() {

		boolean resetDone = false;
		if( this.reset && ! this.messageUnderProcessing.get()) {
			resetDone = true;
			reset();
		}

		return resetDone;
	}


	/**
	 * Resets the agent while no message is being processed.
	 */
	private void reset() {

		// Log something
		this.logger.info( "Resetting the agent..." );
		this.agent.resetInProgress.set( true );

		// Clear all the messages that were waiting to be processed
		// (best-effort mode: do what is easy first)
		getMessageQueue().clear();

		// Uninstall all the programs this agent was managing
		if( this.scopedInstance != null ) {

			// Sort instances: bottom instances first
			List<Instance> sortedInstances = InstanceHelpers.buildHierarchicalList( this.scopedInstance );
			Collections.reverse( sortedInstances );

			// Remove the scoped instance, no action is permitted on it
			sortedInstances.remove( this.scopedInstance );

			// And undeploy them all
			for( Instance inst : sortedInstances ) {
				MsgCmdChangeInstanceState localMsg = new MsgCmdChangeInstanceState( inst, InstanceStatus.NOT_DEPLOYED );
				try {
					processMsgChangeInstanceState( localMsg );

				} catch( Exception e ) {
					// Best effort mode: keep on going silently...
					Utils.logException( this.logger, e );
				}
			}
		}

		// Prepare a message indicating the machine is down
		// (additional security in the case where a heart beat arrived)
		MsgNotifMachineDown downMsg = new MsgNotifMachineDown(
				this.agent.getApplicationName(),
				this.agent.getScopedInstancePath());

		// Reset the model
		this.agent.setScopedInstance( null );
		this.agent.setApplicationName( null );
		this.agent.setScopedInstance( null );
		this.agent.setScopedInstancePath( null );
		this.agent.setDomain( Constants.DEFAULT_DOMAIN );

		this.scopedInstance = null;
		this.applicationBindings.clear();
		this.applicationNameToExternalExports.clear();
		this.reset = false;

		// Send the message while we still have a message client
		try {
			this.messagingClient.sendMessageToTheDm( downMsg );

		} catch( Exception e ) {
			Utils.logException( this.logger, e );
		}

		// Update the agent's configuration files
		Map<String,String> keyToNewValue = new HashMap<> ();
		keyToNewValue.put( "application-name", "" );
		keyToNewValue.put( "scoped-instance-path", "" );
		keyToNewValue.put( "domain", Constants.DEFAULT_DOMAIN );
		keyToNewValue.put( "parameters", "" );
		keyToNewValue.put( Constants.MESSAGING_TYPE, MessagingConstants.FACTORY_IDLE );

		// Reset the configuration
		File agentConfigFile = new File( this.agent.karafEtc, Constants.KARAF_CFG_FILE_AGENT );
		try {
			String content = Utils.readFileContent( agentConfigFile );
			content = Utils.updateProperties( content, keyToNewValue );
			Utils.writeStringInto( content, agentConfigFile );

		} catch( Exception e ) {
			Utils.logException( this.logger, e );
		}

		// Clear all the messages that were received while this method was executed
		getMessageQueue().clear();

		/*
		 * By writing in the agent's configuration file,
		 * we update all the values and indirectly invoke the
		 * agent's reconfigure method (that will reset the messaging client).
		 *
		 * User data will not be reloaded since we set the parameters to "".
		 * That prevents infinite loops (!).
		 */

		this.agent.resetInProgress.set( false );
		this.logger.info( "Resetting the agent has just completed." );
	}


	/**
	 * Gathers the main log files and sends them to the DM.
	 * @param message the incoming message
	 * @throws IOException if something went wrong
	 */
	private void processGatherLogs( MsgCmdGatherLogs message ) throws IOException {

		Map<String,byte[]> logFiles = AgentUtils.collectLogs( this.agent.karafData );
		MsgNotifLogs msg = new MsgNotifLogs( this.agent.getApplicationName(), this.agent.getScopedInstancePath(), logFiles );
		this.messagingClient.sendMessageToTheDm( msg );
	}


	/**
	 * Changes the log level.
	 * @param message the incoming message
	 * @throws IOException if something went wrong
	 */
	private void processChangeLogLevel( MsgCmdChangeLogLevel message ) throws IOException {
		AgentUtils.changeRoboconfLogLevel( message.getLogLevel(), this.agent.karafEtc );
	}


	/**
	 * Updates the probe configuration of a given instance.
	 * @param message the incoming message
	 * @throws IOException if something went wrong
	 */
	private void processUpdateProbeConfiguration( MsgCmdUpdateProbeConfiguration message )
	throws IOException {

		Instance inst = InstanceHelpers.findInstanceByPath( this.scopedInstance, message.getInstancePath());
		if( inst == null )
			this.logger.warning( "Instance " + message.getInstancePath() + " could not be found. Probe configuration will not be updated." );
		else
			AgentUtils.copyInstanceResources( inst, message.getProbeResources());
	}


	/**
	 * Updates the application bindings.
	 * <p>
	 * These bindings can be modified when the application is running. In this
	 * case, it triggers a reconfiguration when necessary.
	 * </p>
	 *
	 * @param msg an change binding message
	 */
	void processMsgChangeBinding( MsgCmdChangeBinding msg ) throws IOException {
		this.logger.fine( "Updating bound applications for prefix " + msg.getExternalExportsPrefix() + "." );

		// We need to get the delta between the new bindings and the previous ones
		Set<String> oldBindings = this.applicationBindings.get( msg.getExternalExportsPrefix());
		if( oldBindings == null )
			oldBindings = new HashSet<>( 0 );

		Set<String> newBindings = msg.getAppNames();
		if( newBindings == null )
			newBindings = new HashSet<>( 0 );

		// Removed names are those present before but not after
		Set<String> removedAppNames = new LinkedHashSet<>( oldBindings );
		removedAppNames.removeAll( newBindings );

		// Added ones are those present after but not before
		Set<String> addedAppNames = new LinkedHashSet<>( newBindings );
		addedAppNames.removeAll( oldBindings );

		// If so, act as if the associated imports had been removed
		for( String removedAppName : removedAppNames ) {
			this.logger.fine( "Unbiding prefix " + msg.getExternalExportsPrefix() + " from application " + removedAppName + "." );

			// We need to remove all the instance paths associated with this inter-app prefix.
			// For that, we act as if we had received MsgCmdRemoveImport messages.

			// It means we will indirectly update some instances (at least once). And that we may invoke
			// update scripts more than once. This is not very efficient, but it is to preserve the plug-in API
			// and the way we pass parameters to our scripts.
			// Indeed, when we remove an import, we pass it to our plug-ins. The scripts can handle them. Handling
			// a collection (e.g. a list) would make scripts and recipes more complicated. Besides, it would not be
			// symmetrical with added imports.
			List<String> instancePaths = new ArrayList<> ();
			for( Instance instance : InstanceHelpers.buildHierarchicalList( this.scopedInstance )) {
				Collection<Import> imports = instance.getImports().get( msg.getExternalExportsPrefix());
				if( imports == null )
					continue;

				for( Import imp : imports )
					instancePaths.add( imp.getInstancePath());
			}

			// Now that we have the instance paths to remove,
			// remove the associated imports.
			for( String path : instancePaths ) {
				MsgCmdRemoveImport fakeMsg = new MsgCmdRemoveImport( removedAppName, msg.getExternalExportsPrefix(), path );
				try {
					processMsgRemoveImport( fakeMsg, false );

				} catch( PluginException e ) {
					this.logger.severe( "A problem occurred with a plug-in. " + e.getMessage());
					Utils.logException( this.logger, e );
				}
			}
		}

		// Update the bindings
		this.applicationBindings.put( msg.getExternalExportsPrefix(), msg.getAppNames());

		// Now, we need to find all the external imports associated with the new application.
		// Then, we will act as if we had received AddImport messages. This is symmetrical with what we
		// did with removed imports.
		// This strategy with a local cache prevents use from requesting exports from other agents.
		for( String addedAppName : addedAppNames ) {

			this.logger.fine( "Binding prefix " + msg.getExternalExportsPrefix() + " with application " + addedAppName + "." );
			Collection<Import> imports = this.applicationNameToExternalExports.get( addedAppName );
			if( imports == null )
				continue;

			for( Import imp : imports ) {
				MsgCmdAddImport fakeMsg = new MsgCmdAddImport( addedAppName, imp.getComponentName(), imp.getInstancePath(), imp.getExportedVars());
				try {
					processMsgAddImport( fakeMsg );

				} catch( PluginException e ) {
					this.logger.severe( "A problem occurred with a plug-in. " + e.getMessage());
					Utils.logException( this.logger, e );
				}
			}
		}
	}


	/**
	 * Responds to an Echo 'PING' message sent by the DM.
	 * @param message the Echo 'PING' message sent by the DM.
	 */
	void processMsgEcho( MsgEcho message ) throws IOException {

		final String content = message.getContent();
		MsgEcho response = new MsgEcho( content.replaceFirst( "^PING:", "PONG:" ), message.getUuid());
		this.logger.fine( "Responding to DM Echo message " + content + " with response " + response.getContent());
		this.messagingClient.sendMessageToTheDm( response );
	}


	/**
	 * Republishes all the variables managed by this agent.
	 * @param message the initial request
	 * @throws IOException if an error occurred with the messaging
	 */
	void processMsgResynchronize( MsgCmdResynchronize message ) throws IOException {

		if( this.scopedInstance != null ) {
			for( Instance i : InstanceHelpers.buildHierarchicalList( this.scopedInstance )) {
				if( i.getStatus() == InstanceStatus.DEPLOYED_STARTED )
					this.messagingClient.publishExports( i );
			}
		}
	}


	/**
	 * Sends the local states to the DM.
	 * @param message the initial request
	 * @throws IOException if an error occurred with the messaging
	 */
	void processMsgSendInstances( MsgCmdSendInstances message ) throws IOException {

		String appName = this.agent.getApplicationName();
		if( this.scopedInstance != null ) {
			for( Instance i : InstanceHelpers.buildHierarchicalList( this.scopedInstance ))
				this.messagingClient.sendMessageToTheDm( new MsgNotifInstanceChanged( appName, i ));
		}
	}



	/**
	 * Sets or updates the local model.
	 * <p>
	 * This method is used to initialize the local model
	 * or to update it when new instances were created.
	 * </p>
	 * <p>
	 * Deletion is handled separately.
	 * </p>
	 *
	 * @param msg the message to process
	 * @throws IOException if an error occurred with the messaging
	 * @throws PluginException if an error occurred while initializing the plug-in
	 */
	void processMsgSetScopedInstance( MsgCmdSetScopedInstance msg ) throws IOException, PluginException {

		Instance newScopedInstance = msg.getScopedInstance();
		List<Instance> instancesToProcess = new ArrayList<> ();

		// Update the model and determine what must be updated
		if( ! InstanceHelpers.isTarget( newScopedInstance )) {
			this.logger.severe( "The received instance is not a scoped one. Request to update the local model is dropped." );

		} else if( this.scopedInstance == null ) {
			this.logger.fine( "Setting the scoped instance." );
			this.scopedInstance = newScopedInstance;
			InstanceHelpers.removeOffScopeInstances( newScopedInstance );

			this.agent.setScopedInstance( newScopedInstance );
			instancesToProcess.addAll( InstanceHelpers.buildHierarchicalList( this.scopedInstance ));

			// Propagate the external mapping into the messaging
			this.messagingClient.setExternalMapping( msg.getExternalExports());

			// Initialize the application bindings
			this.applicationBindings.putAll( msg.getApplicationBindings());

			// Executes the script
			AgentUtils.copyInstanceResources( this.scopedInstance, msg.getscriptResources());
			AgentUtils.executeScriptResources( InstanceHelpers.findInstanceDirectoryOnAgent( this.scopedInstance ));

			// Notify the DM
			if( this.scopedInstance.getStatus() != InstanceStatus.DEPLOYED_STARTED ) {
				this.scopedInstance.setStatus( InstanceStatus.DEPLOYED_STARTED );
				this.messagingClient.sendMessageToTheDm( new MsgNotifInstanceChanged( this.agent.getApplicationName(), this.scopedInstance ));
			}

			// Listen to requests from other agents for the scoped instance ONLY.
			// See #301. It won't be done anywhere else for the scoped instance.
			this.messagingClient.listenToRequestsFromOtherAgents( ListenerCommand.START, this.scopedInstance );
		}

		// Configure the messaging.
		for( Instance instanceToProcess : instancesToProcess ) {
			this.messagingClient.listenToExportsFromOtherAgents( ListenerCommand.START, instanceToProcess );
			this.messagingClient.requestExportsFromOtherAgents( instanceToProcess );
		}
	}


	/**
	 * Removes an instance to the local model.
	 * @param msg the message to process
	 * @throws IOException if an error occurred with the messaging
	 */
	void processMsgRemoveInstance( MsgCmdRemoveInstance msg ) throws IOException {

		// Remove the instance
		boolean removed = false;
		Instance instance = InstanceHelpers.findInstanceByPath( this.scopedInstance, msg.getInstancePath());
		if( instance == null ) {
			this.logger.severe( "No instance matched " + msg.getInstancePath() + " on the agent. Request to remove it from the model is dropped." );

		} else if( instance.getStatus() != InstanceStatus.NOT_DEPLOYED ) {
			this.logger.severe( "Instance " + msg.getInstancePath() + " cannot be removed. Instance status: " + instance.getStatus() + "." );
			// We do not have to check children's status.
			// We cannot have a parent in NOT_DEPLOYED and a child in STARTED (as an example).

		} else if( instance.getParent() != null ) {
			removed = true;
			instance.getParent().getChildren().remove( instance );
			this.logger.fine( "Child instance " + msg.getInstancePath() + " was removed from the model." );

		} else {
			this.logger.fine( "The root instance " + msg.getInstancePath() + " cannot be removed. The agent must be reboot and/or reconfigured." );
		}

		// Configure the messaging
		if( removed ) {
			this.messagingClient.sendMessageToTheDm( new MsgNotifInstanceRemoved( this.agent.getApplicationName(), instance ));
			for( Instance instanceToProcess : InstanceHelpers.buildHierarchicalList( instance ))
				this.messagingClient.listenToExportsFromOtherAgents( ListenerCommand.STOP, instanceToProcess );
		}
	}


	/**
	 * Adds an instance to the local model.
	 * @param msg the message to process
	 * @throws IOException if an error occurred with the messaging
	 * @throws PluginException if an error occurred while initializing the plug-in
	 */
	void processMsgAddInstance( MsgCmdAddInstance msg ) throws IOException, PluginException {

		Component instanceComponent;
		Instance parentInstance = InstanceHelpers.findInstanceByPath( this.scopedInstance, msg.getParentInstancePath());
		if( parentInstance == null ) {
			this.logger.severe( "The parent instance for " + msg.getParentInstancePath() + " was not found. The request to add a new instance is dropped." );

		} else if(( instanceComponent = ComponentHelpers.findComponentFrom( parentInstance.getComponent(), msg.getComponentName())) == null ) {
			this.logger.severe( "The component " + msg.getComponentName() + " was not found in the local graph." );

		} else {
			Instance newInstance = new Instance( msg.getInstanceName()).component( instanceComponent );
			newInstance.channels.addAll( msg.getChannels());
			if( msg.getData() != null )
				newInstance.data.putAll( msg.getData());

			if( msg.getOverridenExports() != null )
				newInstance.overriddenExports.putAll( msg.getOverridenExports());

			ApplicationTemplate tempApp = new ApplicationTemplate( "temp app" );
			tempApp.getRootInstances().add( parentInstance );
			if( ! InstanceHelpers.tryToInsertChildInstance( tempApp, parentInstance, newInstance )) {
				this.logger.severe( "The new '" + msg.getInstanceName() + "' instance could not be inserted into the local model." );

			} else {
				this.messagingClient.listenToExportsFromOtherAgents( ListenerCommand.START, newInstance );
				this.messagingClient.requestExportsFromOtherAgents( newInstance );
			}
		}
	}


	/**
	 * Deploys an instance.
	 * @param msg the message to process
	 * @throws IOException if an error occurred with the messaging or while manipulating the file system
	 * @throws PluginException if something went wrong with the plug-in
	 */
	void processMsgChangeInstanceState( MsgCmdChangeInstanceState msg )
	throws IOException, PluginException {

		PluginInterface plugin;
		Instance instance = InstanceHelpers.findInstanceByPath( this.scopedInstance, msg.getInstancePath());
		if( instance == null )
			this.logger.severe( "No instance matched " + msg.getInstancePath() + " on the agent. Request to deploy it is dropped." );

		else if( instance.getParent() == null )
			this.logger.severe( "No action on the root instance is permitted." );

		else if(( plugin = this.agent.findPlugin( instance )) == null )
			this.logger.severe( "No plug-in was found to deploy " + msg.getInstancePath() + "." );

		else
			AbstractLifeCycleManager
			.build( instance, this.agent.getApplicationName(), this.messagingClient)
			.changeInstanceState( instance, plugin, msg.getNewState(), msg.getFileNameToFileContent());
	}


	/**
	 * Publishes its exports when required.
	 * @param msg the message process
	 * @throws IOException if an error occurred with the messaging
	 */
	void processMsgRequestImport( MsgCmdRequestImport msg ) throws IOException {

		for( Instance instance : InstanceHelpers.buildHierarchicalList( this.scopedInstance )) {
			if( instance.getStatus() == InstanceStatus.DEPLOYED_STARTED )
				this.messagingClient.publishExports( instance, msg.getComponentOrFacetName());
		}
	}


	/**
	 * Removes (if necessary) an import from the model instances.
	 * @param msg the message process
	 * @param realMessage true if is real received message, false for a fake one
	 * @throws IOException if an error occurred with the messaging
	 * @throws PluginException if an error occurred with a plug-in
	 */
	void processMsgRemoveImport( MsgCmdRemoveImport msg, boolean realMessage ) throws IOException, PluginException {

		// Track ALL external exports - only for real messages
		String appName = this.agent.getApplicationName();
		if( realMessage && ! msg.getApplicationOrContextName().equals( appName )) {
			removeCachedExternalImport( msg );
		}

		// Go through all the instances to see which ones are impacted.
		// If it is an external exports that is removed, it will not be found in this instance.
		for( Instance instance : InstanceHelpers.buildHierarchicalList( this.scopedInstance )) {

			Set<String> importPrefixes = VariableHelpers.findPrefixesForImportedVariables( instance );
			if( ! importPrefixes.contains( msg.getComponentOrFacetName()))
				continue;

			// Is there an import to remove?
			Collection<Import> imports = instance.getImports().get( msg.getComponentOrFacetName());
			Import toRemove = ImportHelpers.findImportByExportingInstance( imports, msg.getRemovedInstancePath());
			if( toRemove == null )
				continue;

			// Remove the import and publish an update to the DM
			imports.remove( toRemove );
			if( imports.isEmpty())
				instance.getImports().remove( msg.getComponentOrFacetName());

			this.logger.fine( "Removing import from " + InstanceHelpers.computeInstancePath( instance )
					+ ". Removed exporting instance: " + msg.getRemovedInstancePath());

			this.messagingClient.sendMessageToTheDm( new MsgNotifInstanceChanged( appName, instance ));

			// Update the life cycle if necessary
			PluginInterface plugin = this.agent.findPlugin( instance );
			if( plugin == null )
				throw new PluginException( "No plugin was found for " + InstanceHelpers.computeInstancePath( instance ));

			AbstractLifeCycleManager
			.build( instance, this.agent.getApplicationName(), this.messagingClient)
			.updateStateFromImports( instance, plugin, toRemove, InstanceStatus.DEPLOYED_STOPPED );
		}

		// Import changed => check all the waiting for ancestors...
		startChildrenInstancesWaitingForAncestors();
	}


	/**
	 * Receives and adds (if necessary) a new import to the model instances.
	 * @param msg the message process
	 * @throws IOException if an error occurred with the messaging
	 * @throws PluginException if an error occurred with a plug-in
	 */
	void processMsgAddImport( MsgCmdAddImport msg ) throws IOException, PluginException {

		// We must filter the new import.
		// It must either come from THIS application, or be referenced in the
		// application bindings. Otherwise, drop the update.
		if( ! msg.getApplicationOrContextName().equals( this.agent.getApplicationName())) {

			// Track ALL external exports
			Collection<Import> imports = this.applicationNameToExternalExports.get( msg.getApplicationOrContextName());
			if( imports == null ) {
				imports = new LinkedHashSet<> ();
				this.applicationNameToExternalExports.put( msg.getApplicationOrContextName(), imports );
			}

			// We use a set to prevent duplicates, so no need to distinguish fake and real messages
			imports.add( new Import( msg.getAddedInstancePath(), msg.getComponentOrFacetName(), msg.getExportedVariables()));

			// Should we go further?
			// If it is not in the application binding, this import should not be added in the instance imports.
			Set<String> appNames = this.applicationBindings.get( msg.getComponentOrFacetName());
			if( appNames == null || ! appNames.contains( msg.getApplicationOrContextName())) {
				this.logger.fine( "An external export was received (" + msg.getComponentOrFacetName() + ") but did not match any of the bound applications." );
				return;
			}
		}

		// Go through all the instances to see which ones need an update
		String appName = this.agent.getApplicationName();
		for( Instance instance : InstanceHelpers.buildHierarchicalList( this.scopedInstance )) {

			// This instance does not depends on it
			Set<String> importPrefixes = VariableHelpers.findPrefixesForImportedVariables( instance );
			if( ! importPrefixes.contains( msg.getComponentOrFacetName()))
				continue;

			// If an instance depends on its component, make sure it does not add itself to the imports.
			// Example: MongoDB may depend on other MongoDB instances.
			if( Objects.equals(
					InstanceHelpers.computeInstancePath( instance ),
					msg.getAddedInstancePath()))
				continue;

			// Create the right import
			Import imp = ImportHelpers.buildTailoredImport(
					instance,
					msg.getAddedInstancePath(),
					msg.getComponentOrFacetName(),
					msg.getExportedVariables());

			// Add the import and publish an update to the DM
			this.logger.fine( "Adding import to " + InstanceHelpers.computeInstancePath( instance ) + ". New import: " + imp );
			ImportHelpers.addImport( instance, msg.getComponentOrFacetName(), imp );
			this.messagingClient.sendMessageToTheDm( new MsgNotifInstanceChanged( appName, instance ));

			// Update the life cycle if necessary
			PluginInterface plugin = this.agent.findPlugin( instance );
			if( plugin == null )
				throw new PluginException( "No plugin was found for " + InstanceHelpers.computeInstancePath( instance ));

			AbstractLifeCycleManager
			.build( instance, this.agent.getApplicationName(), this.messagingClient)
			.updateStateFromImports( instance, plugin, imp, InstanceStatus.DEPLOYED_STARTED );
		}

		// Import changed => check all the waiting for ancestors...
		startChildrenInstancesWaitingForAncestors();
	}


	private void removeCachedExternalImport( MsgCmdRemoveImport msg ) {

		Collection<Import> imports = this.applicationNameToExternalExports.get( msg.getApplicationOrContextName());
		if( imports != null ) {
			Import toRemove = null;
			for( Iterator<Import> it = imports.iterator(); it.hasNext() && toRemove == null; ) {
				Import cur = it.next();
				if( cur.getInstancePath().equals( msg.getRemovedInstancePath()))
					toRemove = cur;
			}

			if( toRemove != null )
				imports.remove( toRemove );

			if( imports.isEmpty())
				this.applicationNameToExternalExports.remove( msg.getApplicationOrContextName());
		}
	}


	/**
	 * Starts children instances when they are waiting for their ancestors to start.
	 * <p>
	 * To invoke every time imports change.
	 * </p>
	 * @throws IOException if something went wrong
	 * @throws PluginException if something went wrong
	 */
	private void startChildrenInstancesWaitingForAncestors() throws IOException, PluginException {

		List<Instance> childrenInstances = InstanceHelpers.buildHierarchicalList( this.scopedInstance );
		childrenInstances.remove( this.scopedInstance );

		for( Instance childInstance : childrenInstances ) {
			if( childInstance.getStatus() != InstanceStatus.WAITING_FOR_ANCESTOR )
				continue;

			if( childInstance.getParent().getStatus() != InstanceStatus.DEPLOYED_STARTED )
				continue;

			PluginInterface plugin = this.agent.findPlugin( childInstance );
			if( plugin == null )
				this.logger.severe( "No plug-in was found for " + InstanceHelpers.computeInstancePath( childInstance ) + "." );
			else
				AbstractLifeCycleManager
				.build( childInstance, this.agent.getApplicationName(), this.messagingClient)
				.changeInstanceState( childInstance, plugin, InstanceStatus.DEPLOYED_STARTED, null );
		}
	}
}
