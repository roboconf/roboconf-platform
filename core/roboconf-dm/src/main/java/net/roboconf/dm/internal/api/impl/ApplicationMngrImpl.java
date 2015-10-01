/**
 * Copyright 2015 Linagora, Université Joseph Fourier, Floralis
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import net.roboconf.core.Constants;
import net.roboconf.core.ErrorCode;
import net.roboconf.core.RoboconfError;
import net.roboconf.core.model.ApplicationDescriptor;
import net.roboconf.core.model.RuntimeModelIo.InstancesLoadResult;
import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.helpers.RoboconfErrorHelpers;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.internal.utils.ConfigurationUtils;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.api.IApplicationMngr;
import net.roboconf.dm.management.api.IApplicationTemplateMngr;
import net.roboconf.dm.management.api.IConfigurationMngr;
import net.roboconf.dm.management.api.IMessagingMngr;
import net.roboconf.dm.management.api.INotificationMngr;
import net.roboconf.dm.management.api.ITargetsMngr;
import net.roboconf.dm.management.events.EventType;
import net.roboconf.dm.management.exceptions.AlreadyExistingException;
import net.roboconf.dm.management.exceptions.InvalidApplicationException;
import net.roboconf.dm.management.exceptions.UnauthorizedActionException;
import net.roboconf.messaging.api.client.ListenerCommand;
import net.roboconf.messaging.api.messages.from_dm_to_agent.MsgCmdChangeBinding;

/**
 * @author Noël - LIG
 * @author Pierre-Yves Gibello - Linagora
 * @author Vincent Zurczak - Linagora
 * @author Pierre Bourret - Université Joseph Fourier
 */
public class ApplicationMngrImpl implements IApplicationMngr {

	private final Logger logger = Logger.getLogger( getClass().getName());
	private final INotificationMngr notificationMngr;
	private final IConfigurationMngr configurationMngr;
	private final ITargetsMngr targetMngr;
	private final IMessagingMngr messagingMngr;
	private final Map<String,ManagedApplication> nameToManagedApplication;

	private IApplicationTemplateMngr applicationTemplateMngr;


	/**
	 * Constructor.
	 * @param notificationMngr
	 * @param configurationMngr
	 * @param messagingMngr
	 * @param targetMngr
	 */
	public ApplicationMngrImpl(
			INotificationMngr notificationMngr,
			IConfigurationMngr configurationMngr,
			ITargetsMngr targetMngr,
			IMessagingMngr messagingMngr ) {
		this.nameToManagedApplication = new ConcurrentHashMap<String,ManagedApplication> ();

		this.notificationMngr = notificationMngr;
		this.configurationMngr = configurationMngr;
		this.messagingMngr = messagingMngr;
		this.targetMngr = targetMngr;
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
		ManagedApplication ma = createApplication( name, description, tpl, this.configurationMngr.getWorkingDirectory());

		// Copy the target settings, if any
		this.targetMngr.copyOriginalMapping( ma.getApplication());

		// Start listening to messages
		this.messagingMngr.getMessagingClient().listenToAgentMessages( ma.getApplication(), ListenerCommand.START );

		// Notify listeners
		this.notificationMngr.application( ma.getApplication(), EventType.CREATED );

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

		// Notify listeners
		this.notificationMngr.application( ma.getApplication(), EventType.DELETED );

		// Delete artifacts
		Application app = ma.getApplication();
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
				ApplicationTemplate tpl = this.applicationTemplateMngr.findTemplate( desc.getTemplateName(), desc.getTemplateQualifier());
				if( tpl == null )
					throw new InvalidApplicationException( new RoboconfError( ErrorCode.PROJ_APPLICATION_TEMPLATE_NOT_FOUND ));

				// Recreate the application
				ManagedApplication ma = createApplication( desc.getName(), desc.getDescription(), tpl, configurationDirectory );

				// Restore the instances
				InstancesLoadResult ilr = ConfigurationUtils.restoreInstances( ma, configurationDirectory );
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

		for( String warning : RoboconfErrorHelpers.extractAndFormatWarnings( errors ))
			logger.warning( warning );
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
	private ManagedApplication createApplication(
			String name,
			String description,
			ApplicationTemplate tpl,
			File configurationDirectory )
	throws AlreadyExistingException, IOException {

		this.logger.info( "Creating application " + name + " from template " + tpl + "..." );
		if( Utils.isEmptyOrWhitespaces( name ))
			throw new IOException( "An application name cannot be empty." );

		if( this.nameToManagedApplication.containsKey( name ))
			throw new AlreadyExistingException( name );

		// Create the application's directory
		Application app = new Application( name, tpl ).description( description );
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

		// Register the application
		ManagedApplication ma = new ManagedApplication( app );
		this.nameToManagedApplication.put( name, ma );

		this.logger.info( "Application " + name + " was successfully created from the template " + tpl + "." );
		return ma;
	}


	@Override
	public void bindApplication( ManagedApplication ma, String applicationTemplateName, String applicationName )
	throws UnauthorizedActionException, IOException {

		Application app = findApplicationByName( applicationName );
		if( app == null )
			throw new UnauthorizedActionException( "Application " + applicationName + " does not exist." );

		if( ! app.getTemplate().getName().equals( applicationTemplateName ))
			throw new UnauthorizedActionException( "Application " + applicationName + " is not associated with the " + applicationTemplateName + " template." );

		app.getApplicationBindings().put( applicationTemplateName, applicationName );
		// TODO: persist bindings

		for( Instance inst : InstanceHelpers.findAllScopedInstances( app )) {
			MsgCmdChangeBinding msg = new MsgCmdChangeBinding( app.getTemplate().getExternalExportsPrefix(), applicationName );
			this.messagingMngr.sendMessageSafely( ma, inst, msg );
		}
	}
}
