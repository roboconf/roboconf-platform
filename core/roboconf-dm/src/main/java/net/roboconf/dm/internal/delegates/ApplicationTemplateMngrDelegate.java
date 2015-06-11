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
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import net.roboconf.core.model.RuntimeModelIo;
import net.roboconf.core.model.RuntimeModelIo.ApplicationLoadResult;
import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.core.model.helpers.RoboconfErrorHelpers;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.internal.utils.ConfigurationUtils;
import net.roboconf.dm.management.exceptions.AlreadyExistingException;
import net.roboconf.dm.management.exceptions.InvalidApplicationException;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ApplicationTemplateMngrDelegate {

	// A set would be enough, but we need to handle concurrent operations.
	// It is thus more simple to use a concurrent hash map.
	final Map<ApplicationTemplate,Boolean> templates = new ConcurrentHashMap<ApplicationTemplate, Boolean> ();
	private final Logger logger = Logger.getLogger( getClass().getName());


	/**
	 * Loads an application template.
	 * @param applicationFilesDirectory the directory that contains the template's files
	 * @param configurationDirectory the DM's configuration directory
	 * @throws InvalidApplicationException if the template contains errors
	 * @throws AlreadyExistingException if this template already exists
	 * @throws IOException if the template's files could not be saved
	 * @return the loaded application template
	 */
	public ApplicationTemplate loadApplicationTemplate( File applicationFilesDirectory, File configurationDirectory )
	throws InvalidApplicationException, AlreadyExistingException, IOException {

		this.logger.info( "Loading an application template from " + applicationFilesDirectory + "..." );
		ApplicationLoadResult lr = RuntimeModelIo.loadApplication( applicationFilesDirectory );

		if( RoboconfErrorHelpers.containsCriticalErrors( lr.getLoadErrors()))
			throw new InvalidApplicationException( lr.getLoadErrors());

		for( String warning : RoboconfErrorHelpers.extractAndFormatWarnings( lr.getLoadErrors()))
			this.logger.warning( warning );

		ApplicationTemplate tpl = lr.getApplicationTemplate();
		if( this.templates.containsKey( tpl ))
			throw new AlreadyExistingException( tpl.getName());

		File targetDirectory = ConfigurationUtils.findTemplateDirectory( tpl, configurationDirectory );
		if( ! applicationFilesDirectory.equals( targetDirectory )) {
			if( Utils.isAncestorFile( targetDirectory, applicationFilesDirectory ))
				throw new IOException( "Cannot move " + applicationFilesDirectory + " in Roboconf's work directory. Already a child directory." );
			else
				Utils.copyDirectory( applicationFilesDirectory, targetDirectory );
		}

		tpl.setDirectory( targetDirectory );
		this.templates.put( tpl, Boolean.TRUE );
		this.logger.info( "Application template " + tpl.getName() + " was successfully loaded." );

		return tpl;
	}


	/**
	 * Deletes an application template.
	 * @param tpl an application template
	 * @param configurationDirectory the DM's configuration directory
	 * @throws IOException if the template files could not be deleted
	 */
	public void deleteApplicationTemplate( ApplicationTemplate tpl, File configurationDirectory )
	throws IOException {

		this.logger.info( "Deleting the application template called " + tpl.getName() + "..." );
		this.templates.remove( tpl );

		File targetDirectory = ConfigurationUtils.findTemplateDirectory( tpl, configurationDirectory );
		Utils.deleteFilesRecursively( targetDirectory );

		this.logger.info( "Application template " + tpl.getName() + " was successfully deleted." );
	}


	/**
	 * Restores templates from the configuration directory.
	 * @param configurationDirectory the DM's configuration directory
	 */
	public void restoreTemplates( File configurationDirectory ) {

		this.logger.info( "Restoring application templates from " + configurationDirectory + "..." );
		this.templates.clear();

		File templatesDirectory = new File( configurationDirectory, ConfigurationUtils.TEMPLATES );
		for( File dir : Utils.listDirectories( templatesDirectory )) {
			try {
				loadApplicationTemplate( dir, configurationDirectory );

			} catch( AlreadyExistingException e ) {
				this.logger.warning( "Cannot restore application template in " + dir + " (already existing)." );
				Utils.logException( this.logger, e );

			} catch( InvalidApplicationException e ) {
				this.logger.warning( "Cannot restore application template in " + dir + " (invalid application)." );
				Utils.logException( this.logger, e );

			} catch( IOException e ) {
				this.logger.warning( "Application template's restoration was incomplete from " + dir + " (I/O exception)." );
				Utils.logException( this.logger, e );
			}
		}

		this.logger.info( "Application templates restoration from " + configurationDirectory + " has just completed." );
	}


	/**
	 * Finds a template.
	 * <p>
	 * A template is identified by its name and its qualifier.
	 * </p>
	 *
	 * @param name the template's name
	 * @param qualifier the template's qualifier
	 * @return an application template, or null if none was found
	 */
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


	/**
	 * @return a non-null set of all the application templates
	 */
	public Set<ApplicationTemplate> getAllTemplates() {
		return this.templates.keySet();
	}


	/**
	 * @return the raw templates (never null)
	 */
	public Map<ApplicationTemplate,Boolean> getRawTemplates() {
		return this.templates;
	}
}
