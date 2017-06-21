/**
 * Copyright 2015-2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.dm.internal.api.impl;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import net.roboconf.core.Constants;
import net.roboconf.core.dsl.ParsingConstants;
import net.roboconf.core.errors.ErrorCode;
import net.roboconf.core.errors.RoboconfError;
import net.roboconf.core.errors.RoboconfErrorHelpers;
import net.roboconf.core.model.ApplicationDescriptor;
import net.roboconf.core.model.RuntimeModelIo.InstancesLoadResult;
import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.runtime.EventType;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.internal.api.IRandomMngr;
import net.roboconf.dm.internal.utils.ConfigurationUtils;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.api.IApplicationMngr;
import net.roboconf.dm.management.api.IApplicationTemplateMngr;
import net.roboconf.dm.management.api.IAutonomicMngr;
import net.roboconf.dm.management.api.IConfigurationMngr;
import net.roboconf.dm.management.api.IMessagingMngr;
import net.roboconf.dm.management.api.INotificationMngr;
import net.roboconf.dm.management.api.ITargetsMngr;
import net.roboconf.dm.management.exceptions.AlreadyExistingException;
import net.roboconf.dm.management.exceptions.InvalidApplicationException;
import net.roboconf.dm.management.exceptions.UnauthorizedActionException;
import net.roboconf.messaging.api.business.ListenerCommand;
import net.roboconf.messaging.api.messages.from_dm_to_agent.MsgCmdChangeBinding;

/**
 * @author Noël - LIG
 * @author Pierre-Yves Gibello - Linagora
 * @author Vincent Zurczak - Linagora
 * @author Pierre Bourret - Université Joseph Fourier
 */
public class ApplicationMngrImpl implements IApplicationMngr {

	private final Logger logger = Logger.getLogger( getClass().getName());
	private final Map<String,ManagedApplication> nameToManagedApplication;

	private final INotificationMngr notificationMngr;
	private final IConfigurationMngr configurationMngr;
	private final ITargetsMngr targetsMngr;
	private final IMessagingMngr messagingMngr;
	private final IRandomMngr randomMngr;
	private final IAutonomicMngr autonomicMngr;

	private IApplicationTemplateMngr applicationTemplateMngr;


	/**
	 * Constructor.
	 * @param notificationMngr
	 * @param configurationMngr
	 * @param messagingMngr
	 * @param targetsMngr
	 * @param randomMngr
	 */
	public ApplicationMngrImpl(
			INotificationMngr notificationMngr,
			IConfigurationMngr configurationMngr,
			ITargetsMngr targetsMngr,
			IMessagingMngr messagingMngr,
			IRandomMngr randomMngr,
			IAutonomicMngr autonomicMngr ) {

		this.nameToManagedApplication = new ConcurrentHashMap<> ();

		this.notificationMngr = notificationMngr;
		this.configurationMngr = configurationMngr;
		this.messagingMngr = messagingMngr;
		this.targetsMngr = targetsMngr;
		this.randomMngr = randomMngr;
		this.autonomicMngr = autonomicMngr;
	}


	/**
	 * @param applicationTemplateMngr the applicationTemplateMngr to set
	 */
	public void setApplicationTemplateMngr( IApplicationTemplateMngr applicationTemplateMngr ) {
		this.applicationTemplateMngr = applicationTemplateMngr;
	}


	@Override
	public Application findApplicationByName( String applicationName ) {
		ManagedApplication ma = this.nameToManagedApplication.get( applicationName );
		return ma != null ? ma.getApplication() : null;
	}


	@Override
	public ManagedApplication findManagedApplicationByName( String applicationName ) {
		return this.nameToManagedApplication.get( applicationName );
	}


	@Override
	public Collection<ManagedApplication> getManagedApplications() {
		return this.nameToManagedApplication.values();
	}


	@Override
	public ManagedApplication createApplication( String name, String description, String tplName, String tplQualifier )
	throws IOException, AlreadyExistingException, InvalidApplicationException {

		// Always verify the configuration first
		this.messagingMngr.checkMessagingConfiguration();

		// Create the application
		ApplicationTemplate tpl = this.applicationTemplateMngr.findTemplate( tplName, tplQualifier );
		if( tpl == null )
			throw new InvalidApplicationException( new RoboconfError( ErrorCode.PROJ_APPLICATION_TEMPLATE_NOT_FOUND ));

		return createApplication( name, description, tpl );
	}


	@Override
	public ManagedApplication createApplication( String name, String description, ApplicationTemplate tpl )
	throws IOException, AlreadyExistingException {

		// Always verify the configuration first
		this.messagingMngr.checkMessagingConfiguration();

		// Create the application
		ManagedApplication ma = createNewApplication( name, description, tpl, this.configurationMngr.getWorkingDirectory());

		// Copy the target settings, if any
		this.targetsMngr.copyOriginalMapping( ma.getApplication());

		// Set a value to random variables, if any
		this.randomMngr.generateAllRandomValues( ma.getApplication());

		// Start listening to messages
		this.messagingMngr.getMessagingClient().listenToAgentMessages( ma.getApplication(), ListenerCommand.START );

		// Notify listeners
		this.notificationMngr.application( ma.getApplication(), EventType.CREATED );

		// Load autonomic rules
		this.autonomicMngr.loadApplicationRules( ma.getApplication());

		this.logger.fine( "Application " + ma.getApplication().getName() + " was successfully loaded and added." );
		return ma;
	}


	@Override
	public void updateApplication( ManagedApplication ma, String newDesc ) throws IOException {

		// Basic checks
		this.messagingMngr.checkMessagingConfiguration();

		// Update it
		Application app = ma.getApplication();
		app.setDescription( newDesc );
		File targetDirectory = app.getDirectory();

		File descFile = new File( targetDirectory, Constants.PROJECT_DIR_DESC + "/" + Constants.PROJECT_FILE_DESCRIPTOR );
		Utils.createDirectory( descFile.getParentFile());
		ApplicationDescriptor.save( descFile, app );

		// Notify listeners
		this.notificationMngr.application( ma.getApplication(), EventType.CHANGED );
		this.logger.fine( "The description of application " + ma.getApplication().getName() + " was successfully updated." );
	}


	@Override
	public void deleteApplication( ManagedApplication ma )
	throws UnauthorizedActionException, IOException {

		// What really matters is that there is no agent running.
		// If all the root instances are not deployed, then nothing is deployed at all.
		String applicationName = ma.getApplication().getName();
		for( Instance rootInstance : ma.getApplication().getRootInstances()) {
			if( rootInstance.getStatus() != InstanceStatus.NOT_DEPLOYED )
				throw new UnauthorizedActionException( applicationName + " contains instances that are still deployed." );
		}

		// Stop listening to messages first
		try {
			this.messagingMngr.checkMessagingConfiguration();
			this.messagingMngr.getMessagingClient().listenToAgentMessages( ma.getApplication(), ListenerCommand.STOP );
			this.messagingMngr.getMessagingClient().deleteMessagingServerArtifacts( ma.getApplication());

		} catch( IOException e ) {
			Utils.logException( this.logger, e );
		}

		// Release random variables, if any
		this.randomMngr.releaseAllRandomValues( ma.getApplication());

		// Notify listeners
		this.notificationMngr.application( ma.getApplication(), EventType.DELETED );

		// Remove it from the targets
		Application app = ma.getApplication();
		this.targetsMngr.applicationWasDeleted( app );

		// Remove the autonomic context
		this.autonomicMngr.unloadApplicationRules( app );

		// Delete artifacts
		this.logger.info( "Deleting the application called " + app.getName() + "..." );
		this.nameToManagedApplication.remove( app.getName());
		app.removeAssociationWithTemplate();

		File targetDirectory = ConfigurationUtils.findApplicationDirectory( app.getName(), this.configurationMngr.getWorkingDirectory());
		Utils.deleteFilesRecursively( targetDirectory );

		this.logger.info( "Application " + app.getName() + " was successfully deleted." );
	}


	@Override
	public void restoreApplications() {

		File configurationDirectory = this.configurationMngr.getWorkingDirectory();
		this.logger.info( "Restoring applications from " + configurationDirectory + "..." );
		this.nameToManagedApplication.clear();

		File templatesDirectory = new File( configurationDirectory, ConfigurationUtils.APPLICATIONS );
		for( File dir : Utils.listDirectories( templatesDirectory )) {

			try {
				// Read the descriptor
				File descriptorFile = new File( dir, Constants.PROJECT_DIR_DESC + "/" + Constants.PROJECT_FILE_DESCRIPTOR );
				ApplicationDescriptor desc = ApplicationDescriptor.load( descriptorFile );
				ApplicationTemplate tpl = this.applicationTemplateMngr.findTemplate( desc.getTemplateName(), desc.getTemplateVersion());
				if( tpl == null )
					throw new InvalidApplicationException( new RoboconfError( ErrorCode.PROJ_APPLICATION_TEMPLATE_NOT_FOUND ));

				// Recreate the application
				if( this.nameToManagedApplication.containsKey( desc.getName()))
					throw new AlreadyExistingException( desc.getName());

				Application app = new Application( desc.getName(), tpl ).description( desc.getDescription());
				File targetDirectory = ConfigurationUtils.findApplicationDirectory( app.getName(), configurationDirectory );
				app.setDirectory( targetDirectory );

				ManagedApplication ma = new ManagedApplication( app );
				this.nameToManagedApplication.put( ma.getName(), ma );

				// Restore the cache for random generation in variables
				this.randomMngr.restoreRandomValuesCache( app );

				// Start listening to messages
				this.messagingMngr.getMessagingClient().listenToAgentMessages( ma.getApplication(), ListenerCommand.START );

				// Read application bindings.
				ConfigurationUtils.loadApplicationBindings( app );

				// Load autonomic rules
				this.autonomicMngr.loadApplicationRules( app );

				// Restore the instances
				InstancesLoadResult ilr = ConfigurationUtils.restoreInstances( ma );
				checkErrors( ilr.getLoadErrors(), this.logger );

				ma.getApplication().getRootInstances().clear();
				ma.getApplication().getRootInstances().addAll( ilr.getRootInstances());

			} catch( AlreadyExistingException | InvalidApplicationException | IOException e ) {
				this.logger.warning( "Application restoration failed for directory " + dir + " (" + e.getClass().getSimpleName() + ")." );
				Utils.logException( this.logger, e );
			}
		}

		this.logger.info( "Applications restoration from " + configurationDirectory + " has just completed." );
	}


	@Override
	public boolean isTemplateUsed( ApplicationTemplate tpl ) {

		boolean result = false;
		for( ManagedApplication ma : this.nameToManagedApplication.values()) {
			if( tpl.equals( ma.getApplication().getTemplate())) {
				result = true;
				break;
			}
		}

		return result;
	}


	/**
	 * A method to check errors.
	 * @param errors a non-null list of errors
	 * @param logger a logger
	 * @throws InvalidApplicationException if there are critical errors
	 */
	static void checkErrors( Collection<RoboconfError> errors, Logger logger )
	throws InvalidApplicationException {

		if( RoboconfErrorHelpers.containsCriticalErrors( errors ))
			throw new InvalidApplicationException( errors );

		// Null language => English (right language for logs)
		Collection<RoboconfError> warnings = RoboconfErrorHelpers.findWarnings( errors );
		for( String warningMsg : RoboconfErrorHelpers.formatErrors( warnings, null, true ).values())
			logger.warning( warningMsg );
	}


	/**
	 * Creates a new application from a template.
	 * @param name the application's name
	 * @param description the application's description
	 * @param tpl the application's template
	 * @param configurationDirectory the DM's configuration directory
	 * @return a new managed application
	 * @throws AlreadyExistingException if an application with this name already exists
	 * @throws IOException if the application's directory could not be created
	 */
	private ManagedApplication createNewApplication(
			String name,
			String description,
			ApplicationTemplate tpl,
			File configurationDirectory )
	throws AlreadyExistingException, IOException {

		this.logger.info( "Creating application " + name + " from template " + tpl + "..." );
		if( Utils.isEmptyOrWhitespaces( name ))
			throw new IOException( "An application name cannot be empty." );

		Application app = new Application( name, tpl ).description( description );
		if( ! app.getName().matches( ParsingConstants.PATTERN_APP_NAME ))
			throw new IOException( "Application names cannot contain invalid characters. Letters, digits, dots, underscores, brackets, spaces and the minus symbol are allowed." );

		if( this.nameToManagedApplication.containsKey( name ))
			throw new AlreadyExistingException( name );

		// Create the application's directory
		File targetDirectory = ConfigurationUtils.findApplicationDirectory( app.getName(), configurationDirectory );
		Utils.createDirectory( targetDirectory );
		app.setDirectory( targetDirectory );

		// Create a descriptor
		File descFile = new File( targetDirectory, Constants.PROJECT_DIR_DESC + "/" + Constants.PROJECT_FILE_DESCRIPTOR );
		Utils.createDirectory( descFile.getParentFile());
		ApplicationDescriptor.save( descFile, app );

		// Copy all the templates's directories, except the descriptor, graph and instances
		List<File> tplDirectories = Utils.listDirectories( tpl.getDirectory());
		List<String> toSkip = Arrays.asList( Constants.PROJECT_DIR_DESC, Constants.PROJECT_DIR_GRAPH, Constants.PROJECT_DIR_INSTANCES );
		for( File dir : tplDirectories ) {
			if( toSkip.contains( dir.getName().toLowerCase()))
				continue;

			File newDir = new File( targetDirectory, dir.getName());
			Utils.copyDirectory( dir, newDir );
		}

		// Update the application name in all the root instances
		for( Instance rootInstance : app.getRootInstances())
			rootInstance.data.put( Instance.APPLICATION_NAME, app.getName());

		// Read application bindings.
		// They are not supposed to exist for new applications, but let's be flexible about it.
		ConfigurationUtils.loadApplicationBindings( app );

		// Register the application
		ManagedApplication ma = new ManagedApplication( app );
		this.nameToManagedApplication.put( app.getName(), ma );

		// Save the instances!
		ConfigurationUtils.saveInstances( ma );

		this.logger.info( "Application " + name + " was successfully created from the template " + tpl + "." );
		return ma;
	}


	@Override
	public void bindOrUnbindApplication( ManagedApplication ma, String externalExportPrefix, String applicationName, boolean bind )
	throws UnauthorizedActionException, IOException {

		// Checks
		Application app = findApplicationByName( applicationName );
		if( app == null )
			throw new UnauthorizedActionException( "Application " + applicationName + " does not exist." );

		if( ! externalExportPrefix.equals( app.getTemplate().getExternalExportsPrefix()))
			throw new UnauthorizedActionException( "Application " + applicationName + "'s template does not have " + externalExportPrefix + " as external exports prefix." );

		// Update the model
		boolean notify = true;
		if( bind )
			ma.getApplication().bindWithApplication( externalExportPrefix, applicationName );
		else
			notify = ma.getApplication().unbindFromApplication( externalExportPrefix, applicationName );

		// Update and propagate the modification
		if( notify ) {

			// Save the configuration
			ConfigurationUtils.saveApplicationBindings( ma.getApplication());
			this.logger.fine( "External prefix " + externalExportPrefix + " is now bound to application " + applicationName + " in " + ma.getName() + "." );

			// Notify the agents
			for( Instance inst : InstanceHelpers.findAllScopedInstances( ma.getApplication())) {
				MsgCmdChangeBinding msg = new MsgCmdChangeBinding(
						externalExportPrefix,
						ma.getApplication().getApplicationBindings().get( externalExportPrefix ));

				this.messagingMngr.sendMessageSafely( ma, inst, msg );
			}
		}
	}


	@Override
	public void replaceApplicationBindings( ManagedApplication ma, String externalExportPrefix, Set<String> applicationNames )
	throws UnauthorizedActionException, IOException {

		// Checks
		for( String applicationName : applicationNames ) {
			Application app = findApplicationByName( applicationName );
			if( app == null )
				throw new UnauthorizedActionException( "Application " + applicationName + " does not exist." );

			if( ! externalExportPrefix.equals( app.getTemplate().getExternalExportsPrefix()))
				throw new UnauthorizedActionException( "Application " + applicationName + "'s template does not have " + externalExportPrefix + " as external exports prefix." );
		}

		// Update the model
		boolean notify = ma.getApplication().replaceApplicationBindings( externalExportPrefix, applicationNames );

		// Update and propagate the modification
		if( notify ) {

			// Save the configuration
			ConfigurationUtils.saveApplicationBindings( ma.getApplication());
			for( String applicationName : applicationNames ) {
				this.logger.fine( "External prefix " + externalExportPrefix + " is now bound to application " + applicationName + " in " + ma.getName() + "." );
			}

			// Notify the agents
			for( Instance inst : InstanceHelpers.findAllScopedInstances( ma.getApplication())) {
				MsgCmdChangeBinding msg = new MsgCmdChangeBinding(
						externalExportPrefix,
						ma.getApplication().getApplicationBindings().get( externalExportPrefix ));

				this.messagingMngr.sendMessageSafely( ma, inst, msg );
			}
		}
	}
}
