/**
 * Copyright 2014-2015 Linagora, Université Joseph Fourier, Floralis
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
import java.util.logging.Logger;

import net.roboconf.core.Constants;
import net.roboconf.core.model.RuntimeModelIo;
import net.roboconf.core.model.RuntimeModelIo.InstancesLoadResult;
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
	public static final String TARGETS = "targetsMngr";

	public static final String INSTANCES_FILE = "current.instances";
	public static final String TARGETS_ASSOC_FILE = "target-associations.properties";


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
		if( ! Utils.isEmptyOrWhitespaces( tpl.getQualifier())) {
			sb.append( " - " );
			sb.append( tpl.getQualifier());
		}

		return new File( configurationDirectory, sb.toString());
	}


	/**
	 * Saves the instances into a file.
	 * @param ma the application (not null)
	 * @param configurationDirectory the configuration directory
	 */
	public static void saveInstances( ManagedApplication ma, File configurationDirectory ) {

		Logger logger = Logger.getLogger( ConfigurationUtils.class.getName());
		String relativeFilePath = findInstancesRelativeLocation( ma.getName());
		File targetFile = new File( configurationDirectory, relativeFilePath );
		try {
			Utils.createDirectory( targetFile.getParentFile());
			RuntimeModelIo.writeInstances( targetFile, ma.getApplication().getRootInstances());

		} catch( IOException e ) {
			logger.severe( "Failed to save instances. " + e.getMessage());
			Utils.logException( logger, e );
		}
	}


	/**
	 * Restores instances and set them in the application.
	 * @param ma the application
	 * @param configurationDirectory the configuration directory
	 */
	public static InstancesLoadResult restoreInstances( ManagedApplication ma, File configurationDirectory ) {

		String relativeFilePath = findInstancesRelativeLocation( ma.getName());
		File sourceFile = new File( configurationDirectory, relativeFilePath );
		Graphs graphs = ma.getApplication().getTemplate().getGraphs();
		InstancesLoadResult result;
		if( sourceFile.exists())
			result = RuntimeModelIo.loadInstances( sourceFile, sourceFile.getParentFile(), graphs, ma.getApplication().getName());
		else
			result = new InstancesLoadResult();

		return result;
	}


	/**
	 * Finds the relative file path of the file that stores instances for a given application.
	 * @param appName the application's name
	 * @return a non-null string
	 */
	public static String findInstancesRelativeLocation( String appName ) {

		StringBuilder sb = new StringBuilder( APPLICATIONS );
		sb.append( "/" );
		sb.append( appName );
		sb.append( "/" );
		sb.append( Constants.PROJECT_DIR_INSTANCES );
		sb.append( "/" );
		sb.append( INSTANCES_FILE );

		return sb.toString();
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
			ApplicationTemplate tpl = new ApplicationTemplate( name ).qualifier( qualifier );
			root = ConfigurationUtils.findTemplateDirectory( tpl, configurationDirectory );
		} else {
			root = ConfigurationUtils.findApplicationDirectory( name, configurationDirectory );
		}

		// Find an icon in the directory
		return IconUtils.findIcon( root );
	}
}
