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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import net.roboconf.core.Constants;
import net.roboconf.core.errors.ErrorCode;
import net.roboconf.core.errors.RoboconfError;
import net.roboconf.core.errors.RoboconfErrorHelpers;
import net.roboconf.core.model.RuntimeModelIo;
import net.roboconf.core.model.RuntimeModelIo.ApplicationLoadResult;
import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.core.model.beans.Component;
import net.roboconf.core.model.runtime.EventType;
import net.roboconf.core.utils.ResourceUtils;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.internal.utils.ConfigurationUtils;
import net.roboconf.dm.management.api.IApplicationMngr;
import net.roboconf.dm.management.api.IApplicationTemplateMngr;
import net.roboconf.dm.management.api.IConfigurationMngr;
import net.roboconf.dm.management.api.INotificationMngr;
import net.roboconf.dm.management.api.ITargetsMngr;
import net.roboconf.dm.management.exceptions.AlreadyExistingException;
import net.roboconf.dm.management.exceptions.InvalidApplicationException;
import net.roboconf.dm.management.exceptions.UnauthorizedActionException;

/**
 * @author Noël - LIG
 * @author Pierre-Yves Gibello - Linagora
 * @author Vincent Zurczak - Linagora
 * @author Pierre Bourret - Université Joseph Fourier
 */
public class ApplicationTemplateMngrImpl implements IApplicationTemplateMngr {

	private final Logger logger = Logger.getLogger( getClass().getName());
	private final INotificationMngr notificationMngr;
	private final IConfigurationMngr configurationMngr;
	private final ITargetsMngr targetsMngr;
	private final IApplicationMngr applicationMngr;

	// A set would be enough, but we need to handle concurrent operations.
	// It is thus more simple to use a concurrent hash map.
	final Map<ApplicationTemplate,Boolean> templates = new ConcurrentHashMap<> ();

	// Loading a new application template involves several other APIs.
	// To prevent conflicts when several clients load templates, we use a lock.
	private static final Object INSTALL_LOCK = new Object();


	/**
	 * Constructor.
	 * @param notificationMngr
	 * @param targetsMngr
	 * @param applicationMngr
	 * @param configurationMngr
	 */
	public ApplicationTemplateMngrImpl(
			INotificationMngr notificationMngr,
			ITargetsMngr targetsMngr,
			IApplicationMngr applicationMngr,
			IConfigurationMngr configurationMngr ) {

		this.notificationMngr = notificationMngr;
		this.targetsMngr = targetsMngr;
		this.applicationMngr = applicationMngr;
		this.configurationMngr = configurationMngr;
	}


	@Override
	public Set<ApplicationTemplate> getApplicationTemplates() {
		return this.templates.keySet();
	}


	@Override
	public ApplicationTemplate findTemplate( String name, String version ) {

		ApplicationTemplate result = null;
		for( ApplicationTemplate tpl : this.templates.keySet()) {
			if( Objects.equals( tpl.getName(), name )
					&& Objects.equals( tpl.getVersion(), version )) {
				result = tpl;
				break;
			}
		}

		return result;
	}


	@Override
	public ApplicationTemplate loadApplicationTemplate( File applicationFilesDirectory )
	throws AlreadyExistingException, InvalidApplicationException, IOException, UnauthorizedActionException {

		// This is a critical section.
		synchronized( INSTALL_LOCK ) {

			// Load the template
			this.logger.info( "Loading an application template from " + applicationFilesDirectory + "..." );
			ApplicationLoadResult lr = RuntimeModelIo.loadApplication( applicationFilesDirectory );

			if( RoboconfErrorHelpers.containsCriticalErrors( lr.getLoadErrors()))
				throw new InvalidApplicationException( lr.getLoadErrors());

			// By default, we always log in English
			Collection<RoboconfError> warnings = RoboconfErrorHelpers.findWarnings( lr.getLoadErrors());
			for( String warningMsg : RoboconfErrorHelpers.formatErrors( warnings, null, true ).values())
				this.logger.warning( warningMsg );

			ApplicationTemplate tpl = lr.getApplicationTemplate();
			if( this.templates.containsKey( tpl ))
				throw new AlreadyExistingException( tpl.getName());

			// Verify external export prefixes (no conflict)
			Set<String> externExportPrefixes = new HashSet<> ();
			for( ApplicationTemplate otherTpl : this.templates.keySet()) {
				if( otherTpl.getExternalExportsPrefix() != null )
					externExportPrefixes.add( otherTpl.getExternalExportsPrefix());
			}

			if( externExportPrefixes.contains( tpl.getExternalExportsPrefix()))
				throw new IOException( "The external exports prefix is already used by another template." );

			// Deal with the targets.
			// If a conflict is found, we just skip it.
			Set<String> newTargetIds = registerTargets( tpl );

			// Copy the template's resources
			File targetDirectory = ConfigurationUtils.findTemplateDirectory( tpl, this.configurationMngr.getWorkingDirectory());
			try {
				if( ! applicationFilesDirectory.equals( targetDirectory )) {
					if( Utils.isAncestorFile( targetDirectory, applicationFilesDirectory ))
						throw new IOException( "Cannot move " + applicationFilesDirectory + " in Roboconf's work directory. Already a child directory." );
					else
						Utils.copyDirectory( applicationFilesDirectory, targetDirectory );
				}

			} catch( IOException e ) {
				// In case of error, unregister the targets that were saved
				unregisterTargets( newTargetIds );
				throw e;
			}

			// Change the template's directory
			tpl.setDirectory( targetDirectory );

			// In the copy (the new template's directory), delete the resources for scoped instances.
			// No need to keep target.properties there.
			for( File targetDir : ResourceUtils.findScopedInstancesDirectories( tpl ).values())
				Utils.deleteFilesRecursivelyAndQuietly( targetDir );

			// Complete the model
			this.templates.put( tpl, Boolean.TRUE );
			this.logger.info( "Application template " + tpl.getName() + " was successfully loaded." );

			this.notificationMngr.applicationTemplate( tpl, EventType.CREATED );
			return tpl;
		}
	}


	@Override
	public void deleteApplicationTemplate( String tplName, String tplVersion )
	throws UnauthorizedActionException, InvalidApplicationException, IOException {

		ApplicationTemplate tpl = findTemplate( tplName, tplVersion );
		if( tpl == null )
			throw new InvalidApplicationException( new RoboconfError( ErrorCode.PROJ_APPLICATION_TEMPLATE_NOT_FOUND ));

		if( this.applicationMngr.isTemplateUsed( tpl )) {
			throw new UnauthorizedActionException( tplName + " (" + tplVersion + ") is still used by applications. It cannot be deleted." );

		} else {
			this.logger.info( "Deleting the application template called " + tpl.getName() + "..." );
			this.templates.remove( tpl );
			this.notificationMngr.applicationTemplate( tpl, EventType.DELETED );
			this.targetsMngr.applicationWasDeleted( tpl );

			File targetDirectory = ConfigurationUtils.findTemplateDirectory( tpl, this.configurationMngr.getWorkingDirectory());
			Utils.deleteFilesRecursively( targetDirectory );

			this.logger.info( "Application template " + tpl.getName() + " was successfully deleted." );
		}
	}


	@Override
	public void restoreTemplates() {

		File configurationDirectory = this.configurationMngr.getWorkingDirectory();
		this.logger.info( "Restoring application templates from " + configurationDirectory + "..." );
		this.templates.clear();

		File templatesDirectory = new File( configurationDirectory, ConfigurationUtils.TEMPLATES );
		for( File dir : Utils.listDirectories( templatesDirectory )) {
			try {
				loadApplicationTemplate( dir );

			} catch( AlreadyExistingException | InvalidApplicationException | UnauthorizedActionException | IOException e ) {
				this.logger.warning( "Cannot restore application template in " + dir + " (" + e.getClass().getSimpleName() + ")." );
				Utils.logException( this.logger, e );
			}
		}

		this.logger.info( "Application templates restoration from " + configurationDirectory + " has just completed." );
	}


	/**
	 * Registers the targets available in this template.
	 * @param tpl a template
	 * @return a set of target IDs, created from this template
	 * @throws IOException
	 * @throws UnauthorizedActionException
	 */
	private Set<String> registerTargets( ApplicationTemplate tpl )
	throws IOException, UnauthorizedActionException {

		// Find all the properties files and register them
		IOException conflictException = null;
		Set<String> newTargetIds = new HashSet<> ();
		Map<Component,Set<String>> componentToTargetIds = new HashMap<> ();

		componentLoop: for( Map.Entry<Component,File> entry : ResourceUtils.findScopedInstancesDirectories( tpl ).entrySet()) {

			// Register the targets
			String defaultTargetId = null;
			Set<String> targetIds = new HashSet<> ();
			componentToTargetIds.put( entry.getKey(), targetIds );

			for( File f : Utils.listDirectFiles( entry.getValue(), Constants.FILE_EXT_PROPERTIES )) {
				this.logger.fine( "Registering target " + f.getName() + " from component " + entry.getKey() + " in application template " + tpl );
				String targetId;
				try {
					targetId = this.targetsMngr.createTarget( f, tpl );

				} catch( IOException e ) {
					conflictException = e;
					break componentLoop;
				}

				this.targetsMngr.addHint( targetId, tpl );

				targetIds.add( targetId );
				newTargetIds.add( targetId );
				if( Constants.TARGET_PROPERTIES_FILE_NAME.equalsIgnoreCase( f.getName()))
					defaultTargetId = targetId;
			}

			// If there is a "target.properties" file, forget the other properties.
			// They were registered but we will not use them by default.
			if( defaultTargetId != null ) {
				targetIds.clear();
				targetIds.add( defaultTargetId );
			}
		}

		// Handle conflicts during registration
		if( conflictException != null ) {
			this.logger.fine( "A conflict was found while registering " );
			unregisterTargets( newTargetIds );
			throw conflictException;
		}

		// Associate them with components.
		for( Map.Entry<Component,Set<String>> entry : componentToTargetIds.entrySet()) {
			String key = "@" + entry.getKey().getName();

			// More than one target for a component?
			// => Do not register anything.
			if( entry.getValue().size() == 1 )
				this.targetsMngr.associateTargetWith( entry.getValue().iterator().next(), tpl, key );
		}

		return newTargetIds;
	}


	/**
	 * Unregisters targets.
	 * @param newTargetIds a non-null set of target IDs
	 */
	private void unregisterTargets( Set<String> newTargetIds ) {

		for( String targetId : newTargetIds ) {
			try {
				this.targetsMngr.deleteTarget( targetId );

			} catch( Exception e ) {
				this.logger.severe( "A target ID that has just been registered could not be created. That's weird." );
				Utils.logException( this.logger, e );
			}
		}
	}
}
