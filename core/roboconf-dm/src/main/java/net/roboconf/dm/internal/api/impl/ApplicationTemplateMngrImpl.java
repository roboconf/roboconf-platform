/**
 * Copyright 2015-2016 Linagora, Université Joseph Fourier, Floralis
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import net.roboconf.core.Constants;
import net.roboconf.core.ErrorCode;
import net.roboconf.core.RoboconfError;
import net.roboconf.core.model.RuntimeModelIo;
import net.roboconf.core.model.RuntimeModelIo.ApplicationLoadResult;
import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.core.model.beans.Component;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.helpers.ComponentHelpers;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.helpers.RoboconfErrorHelpers;
import net.roboconf.core.utils.ResourceUtils;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.internal.utils.ConfigurationUtils;
import net.roboconf.dm.management.api.IApplicationMngr;
import net.roboconf.dm.management.api.IApplicationTemplateMngr;
import net.roboconf.dm.management.api.IConfigurationMngr;
import net.roboconf.dm.management.api.INotificationMngr;
import net.roboconf.dm.management.api.ITargetsMngr;
import net.roboconf.dm.management.events.EventType;
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
	final Map<ApplicationTemplate,Boolean> templates = new ConcurrentHashMap<ApplicationTemplate,Boolean> ();


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
	public ApplicationTemplate findTemplate( String name, String qualifier ) {

		ApplicationTemplate result = null;
		for( ApplicationTemplate tpl : this.templates.keySet()) {
			if( Objects.equals( tpl.getName(), name )
					&& Objects.equals( tpl.getQualifier(), qualifier )) {
				result = tpl;
				break;
			}
		}

		return result;
	}


	@Override
	public ApplicationTemplate loadApplicationTemplate( File applicationFilesDirectory )
	throws AlreadyExistingException, InvalidApplicationException, IOException {

		this.logger.info( "Loading an application template from " + applicationFilesDirectory + "..." );
		ApplicationLoadResult lr = RuntimeModelIo.loadApplication( applicationFilesDirectory );

		if( RoboconfErrorHelpers.containsCriticalErrors( lr.getLoadErrors()))
			throw new InvalidApplicationException( lr.getLoadErrors());

		for( String warning : RoboconfErrorHelpers.extractAndFormatWarnings( lr.getLoadErrors()))
			this.logger.warning( warning );

		ApplicationTemplate tpl = lr.getApplicationTemplate();
		if( this.templates.containsKey( tpl ))
			throw new AlreadyExistingException( tpl.getName());

		Set<String> externExportPrefixes = new HashSet<> ();
		for( ApplicationTemplate otherTpl : this.templates.keySet()) {
			if( otherTpl.getExternalExportsPrefix() != null )
				externExportPrefixes.add( otherTpl.getExternalExportsPrefix());
		}

		if( externExportPrefixes.contains( tpl.getExternalExportsPrefix()))
			throw new IOException( "The external exports prefix is already used by another template." );

		File targetDirectory = ConfigurationUtils.findTemplateDirectory( tpl, this.configurationMngr.getWorkingDirectory());
		if( ! applicationFilesDirectory.equals( targetDirectory )) {
			if( Utils.isAncestorFile( targetDirectory, applicationFilesDirectory ))
				throw new IOException( "Cannot move " + applicationFilesDirectory + " in Roboconf's work directory. Already a child directory." );
			else
				Utils.copyDirectory( applicationFilesDirectory, targetDirectory );
		}

		tpl.setDirectory( targetDirectory );
		this.templates.put( tpl, Boolean.TRUE );
		this.logger.info( "Application template " + tpl.getName() + " was successfully loaded." );

		registerTargets( tpl );

		this.notificationMngr.applicationTemplate( tpl, EventType.CREATED );
		return tpl;
	}


	@Override
	public void deleteApplicationTemplate( String tplName, String tplQualifier )
	throws UnauthorizedActionException, InvalidApplicationException, IOException {

		ApplicationTemplate tpl = findTemplate( tplName, tplQualifier );
		if( tpl == null )
			throw new InvalidApplicationException( new RoboconfError( ErrorCode.PROJ_APPLICATION_TEMPLATE_NOT_FOUND ));

		if( this.applicationMngr.isTemplateUsed( tpl )) {
			throw new UnauthorizedActionException( tplName + " (" + tplQualifier + ") is still used by applications. It cannot be deleted." );

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

			} catch( AlreadyExistingException | InvalidApplicationException | IOException e ) {
				this.logger.warning( "Cannot restore application template in " + dir + " (" + e.getClass().getSimpleName() + ")." );
				Utils.logException( this.logger, e );
			}
		}

		this.logger.info( "Application templates restoration from " + configurationDirectory + " has just completed." );
	}


	/**
	 * Finds and registers the targets available in this template.
	 * @param tpl a template
	 * @throws IOException
	 * @throws InvalidApplicationException
	 */
	private void registerTargets( ApplicationTemplate tpl )
	throws IOException, InvalidApplicationException {

		// Find all the properties files and register them
		Map<Component,Set<String>> componentToTargetIds = new HashMap<> ();
		for( Component c : ComponentHelpers.findAllComponents( tpl )) {

			// Target?
			if( ! Constants.TARGET_INSTALLER.equalsIgnoreCase( c.getInstallerName()))
				continue;

			// Is there a resources directory?
			File dir = ResourceUtils.findInstanceResourcesDirectory( tpl.getDirectory(), c );
			if( ! dir.exists())
				continue;

			// Register the targets
			String defaultTargetId = null;
			Set<String> targetIds = new HashSet<> ();
			componentToTargetIds.put( c, targetIds );

			for( File f : Utils.listAllFiles( dir )) {
				if( ! f.getName().toLowerCase().endsWith( Constants.FILE_EXT_PROPERTIES ))
					continue;

				// FIXME: with this mechanism, we may create the same target over and over again
				// when we deploy a test application more than once. #520 would avoid it.
				this.logger.fine( "Registering target " + f.getName() + " from component " + c + " in application template " + tpl );
				String targetId = this.targetsMngr.createTarget( f );
				this.targetsMngr.addHint( targetId, tpl );

				targetIds.add( targetId );
				if( Constants.TARGET_PROPERTIES_FILE_NAME.equalsIgnoreCase( f.getName()))
					defaultTargetId = targetId;
			}

			// If there is a "target.properties" file, forget the other properties.
			// They were registered but we will not use them by default.
			if( defaultTargetId != null ) {
				targetIds.clear();
				targetIds.add( defaultTargetId );
			}

			// Delete them
			Utils.deleteFilesRecursivelyAndQuietly( dir );
		}

		// Associate them with instances.
		Application app = new Application( tpl );
		try {
			// If there is only one root component,
			// and only one target for it, register it by default for the application template.
			Set<String> targetIds;
			if( componentToTargetIds.size() == 1
					&& (targetIds = componentToTargetIds.values().iterator().next()).size() == 1 ) {
				this.targetsMngr.associateTargetWithScopedInstance( targetIds.iterator().next(), tpl, null );
			}

			// Otherwise, make the associations per instances.
			else for( Instance scopedInstance : InstanceHelpers.findAllScopedInstances( app )) {
				targetIds = componentToTargetIds.get( scopedInstance.getComponent());

				// No target for the component? Skip.
				if( targetIds == null )
					continue;

				// Only one target? Register it for the instance.
				if( targetIds.size() == 1 ) {
					String instancePath = InstanceHelpers.computeInstancePath( scopedInstance );
					this.targetsMngr.associateTargetWithScopedInstance( targetIds.iterator().next(), tpl, instancePath );
				}

				// Otherwise, do not register anything.
			}

		} catch( UnauthorizedActionException e ) {
			throw new InvalidApplicationException( e );
		}
	}
}
