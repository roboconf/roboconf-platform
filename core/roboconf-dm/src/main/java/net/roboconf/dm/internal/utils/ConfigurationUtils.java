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

package net.roboconf.dm.internal.utils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

import net.roboconf.core.Constants;
import net.roboconf.core.model.RuntimeModelIo;
import net.roboconf.core.model.RuntimeModelIo.InstancesLoadResult;
import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.core.model.beans.Graphs;
import net.roboconf.core.utils.IconUtils;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.management.ManagedApplication;

/**
 * @author Vincent Zurczak - Linagora
 */
public final class ConfigurationUtils {

	public static final String TEMPLATES = "application-templates";
	public static final String APPLICATIONS = "applications";
	public static final String TARGETS = "targets";

	public static final String INSTANCES_FILE = "current.instances";
	public static final String APP_BINDINGS_FILE = "application-bindings.properties";

	public static final String TARGETS_ASSOC_FILE = "targets-associations.properties";
	public static final String TARGETS_HINTS_SUFFIX = ".hints.properties";
	public static final String TARGETS_USAGE_SUFFIX = ".usage.properties";


	/**
	 * Private constructor.
	 */
	private ConfigurationUtils() {
		// nothing
	}


	/**
	 * @param applicationName an application name
	 * @param configurationDirectory the configuration directory
	 * @return a non-null file that should point to an application directory
	 */
	public static File findApplicationDirectory( String applicationName, File configurationDirectory ) {
		return new File( configurationDirectory, APPLICATIONS + "/" + applicationName );
	}


	/**
	 * Finds the directory that contains the files for an application template.
	 * @param tpl an application template
	 * @param configurationDirectory the DM's configuration directory
	 * @return a non-null file that should point to a directory
	 */
	public static File findTemplateDirectory( ApplicationTemplate tpl, File configurationDirectory ) {

		StringBuilder sb = new StringBuilder( TEMPLATES );
		sb.append( "/" );
		sb.append( tpl.getName());
		if( ! Utils.isEmptyOrWhitespaces( tpl.getVersion())) {
			sb.append( " - " );
			sb.append( tpl.getVersion());
		}

		return new File( configurationDirectory, sb.toString());
	}


	/**
	 * Saves the instances into a file.
	 * @param ma the application (not null)
	 */
	public static void saveInstances( ManagedApplication ma ) {
		saveInstances( ma.getApplication());
	}


	/**
	 * Saves the instances into a file.
	 * @param app the application (not null)
	 * @param configurationDirectory the configuration directory
	 */
	public static void saveInstances( Application app ) {

		File targetFile = new File( app.getDirectory(), Constants.PROJECT_DIR_INSTANCES + "/" + INSTANCES_FILE );
		try {
			Utils.createDirectory( targetFile.getParentFile());
			RuntimeModelIo.writeInstances( targetFile, app.getRootInstances());

		} catch( IOException e ) {
			Logger logger = Logger.getLogger( ConfigurationUtils.class.getName());
			logger.severe( "Failed to save instances. " + e.getMessage());
			Utils.logException( logger, e );
		}
	}


	/**
	 * Restores instances and set them in the application.
	 * @param ma the application
	 * @param configurationDirectory the configuration directory
	 */
	public static InstancesLoadResult restoreInstances( ManagedApplication ma ) {

		File sourceFile = new File( ma.getDirectory(), Constants.PROJECT_DIR_INSTANCES + "/" + INSTANCES_FILE );
		Graphs graphs = ma.getApplication().getTemplate().getGraphs();
		InstancesLoadResult result;
		if( sourceFile.exists())
			result = RuntimeModelIo.loadInstances( sourceFile, sourceFile.getParentFile(), graphs, ma.getApplication().getName());
		else
			result = new InstancesLoadResult();

		return result;
	}


	/**
	 * Finds the icon associated with an application template.
	 * @param name the application or template name
	 * @param qualifier the template qualifier or <code>null</code> for an application
	 * @param configurationDirectory the DM's configuration directory
	 * @return an existing file, or null if no icon was found
	 */
	public static File findIcon( String name, String qualifier, File configurationDirectory ) {

		// Deal with an invalid directory
		if( configurationDirectory == null )
			return null;

		// Find the root directory
		File root;
		if( ! Utils.isEmptyOrWhitespaces( qualifier )) {
			ApplicationTemplate tpl = new ApplicationTemplate( name ).version( qualifier );
			root = ConfigurationUtils.findTemplateDirectory( tpl, configurationDirectory );
		} else {
			root = ConfigurationUtils.findApplicationDirectory( name, configurationDirectory );
		}

		// Find an icon in the directory
		return IconUtils.findIcon( root );
	}


	/**
	 * Loads the application bindings into an application.
	 * @param app a non-null application
	 * @param configurationDirectory the DM's configuration directory
	 */
	public static void loadApplicationBindings( Application app ) {

		File descDir = new File( app.getDirectory(), Constants.PROJECT_DIR_DESC );
		File appBindingsFile = new File( descDir, APP_BINDINGS_FILE );

		Logger logger = Logger.getLogger( ConfigurationUtils.class.getName());
		Properties props = Utils.readPropertiesFileQuietly( appBindingsFile, logger );
		for( Map.Entry<?,?> entry : props.entrySet()) {
			for( String part : Utils.splitNicely((String) entry.getValue(), "," )) {
				if( ! Utils.isEmptyOrWhitespaces( part ))
					app.bindWithApplication((String) entry.getKey(), part );
			}
		}
	}


	/**
	 * Saves the application bindings into the DM's directory.
	 * @param app a non-null application
	 * @param configurationDirectory the DM's configuration directory
	 */
	public static void saveApplicationBindings( Application app ) {

		File descDir = new File( app.getDirectory(), Constants.PROJECT_DIR_DESC );
		File appBindingsFile = new File( descDir, APP_BINDINGS_FILE );

		// Convert the bindings map
		Map<String,String> format = new HashMap<> ();
		for( Map.Entry<String,Set<String>> entry : app.getApplicationBindings().entrySet()) {
			String s = Utils.format( entry.getValue(), ", " );
			format.put( entry.getKey(), s );
		}

		// Save it
		Properties props = new Properties();
		props.putAll( format );

		try {
			Utils.createDirectory( descDir );
			Utils.writePropertiesFile( props, appBindingsFile );

		} catch( IOException e ) {
			Logger logger = Logger.getLogger( ConfigurationUtils.class.getName());
			logger.severe( "Failed to save application bindings for " + app + ". " + e.getMessage());
			Utils.logException( logger, e );
		}
	}
}
