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

package net.roboconf.dm.internal.delegates;

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
import net.roboconf.core.model.helpers.RoboconfErrorHelpers;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.internal.utils.ConfigurationUtils;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.exceptions.AlreadyExistingException;
import net.roboconf.dm.management.exceptions.InvalidApplicationException;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ApplicationMngrDelegate {

	final Map<String,ManagedApplication> nameToManagedApplication = new ConcurrentHashMap<String,ManagedApplication> ();
	private final Logger logger = Logger.getLogger( getClass().getName());


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
	public ManagedApplication createApplication(
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


	/**
	 * Deletes an application.
	 * @param app an application
	 * @param configurationDirectory the DM's configuration directory
	 * @throws IOException if the application files could not be deleted
	 */
	public void deleteApplication( Application app, File configurationDirectory )
	throws IOException {

		this.logger.info( "Deleting the application called " + app.getName() + "..." );
		this.nameToManagedApplication.remove( app.getName());
		app.removeAssociationWithTemplate();

		File targetDirectory = ConfigurationUtils.findApplicationDirectory( app.getName(), configurationDirectory );
		Utils.deleteFilesRecursively( targetDirectory );

		this.logger.info( "Application " + app.getName() + " was successfully deleted." );
	}


	/**
	 * Finds an application by name.
	 * @param applicationName the application name (not null)
	 * @return an application, or null if it was not found
	 */
	public Application findApplicationByName( String applicationName ) {
		ManagedApplication ma = this.nameToManagedApplication.get( applicationName );
		return ma != null ? ma.getApplication() : null;
	}


	/**
	 * Finds a managed application by name.
	 * @param applicationName the application name (not null)
	 * @return a managed application, or null if it was not found
	 */
	public ManagedApplication findManagedApplicationByName( String applicationName ) {
		return this.nameToManagedApplication.get( applicationName );
	}


	/**
	 * Restores applications from the configuration directory.
	 * @param configurationDirectory the DM's configuration directory
	 */
	public void restoreApplications( File configurationDirectory, ApplicationTemplateMngrDelegate mngr ) {

		this.logger.info( "Restoring applications from " + configurationDirectory + "..." );
		this.nameToManagedApplication.clear();

		File templatesDirectory = new File( configurationDirectory, ConfigurationUtils.APPLICATIONS );
		for( File dir : Utils.listDirectories( templatesDirectory )) {

			try {
				// Read the descriptor
				File descriptorFile = new File( dir, Constants.PROJECT_DIR_DESC + "/" + Constants.PROJECT_FILE_DESCRIPTOR );
				ApplicationDescriptor desc = ApplicationDescriptor.load( descriptorFile );
				ApplicationTemplate tpl = mngr.findTemplate( desc.getTemplateName(), desc.getTemplateQualifier());
				if( tpl == null )
					throw new InvalidApplicationException( new RoboconfError( ErrorCode.PROJ_APPLICATION_TEMPLATE_NOT_FOUND ));

				// Recreate the application
				ManagedApplication ma = createApplication( desc.getName(), desc.getDescription(), tpl, configurationDirectory );

				// Restore the instances
				InstancesLoadResult ilr = ConfigurationUtils.restoreInstances( ma, configurationDirectory );
				if( RoboconfErrorHelpers.containsCriticalErrors( ilr.getLoadErrors()))
					throw new InvalidApplicationException( ilr.getLoadErrors());

				for( String warning : RoboconfErrorHelpers.extractAndFormatWarnings( ilr.getLoadErrors()))
					this.logger.warning( warning );

				ma.getApplication().getRootInstances().clear();
				ma.getApplication().getRootInstances().addAll( ilr.getRootInstances());

			} catch( AlreadyExistingException e ) {
				this.logger.warning( "Cannot restore application in " + dir + " (already existing)." );
				Utils.logException( this.logger, e );

			} catch( InvalidApplicationException e ) {
				this.logger.warning( "Cannot restore application in " + dir + " (invalid application or instances)." );
				Utils.logException( this.logger, e );

			} catch( IOException e ) {
				this.logger.warning( "Application's restoration was incomplete from " + dir + " (I/O exception)." );
				Utils.logException( this.logger, e );
			}
		}

		this.logger.info( "Applications restoration from " + configurationDirectory + " has just completed." );
	}


	/**
	 * @return a non-null collection of managed applications
	 */
	public Collection<ManagedApplication> getManagedApplications() {
		return this.nameToManagedApplication.values();
	}


	/**
	 * @return the nameToManagedApplication
	 */
	public Map<String,ManagedApplication> getNameToManagedApplication() {
		return this.nameToManagedApplication;
	}


	/**
	 * Determines whether a template is used by an application.
	 * @param tpl an application template (not null)
	 * @return true if at least one application uses it, false otherwise
	 */
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
}
