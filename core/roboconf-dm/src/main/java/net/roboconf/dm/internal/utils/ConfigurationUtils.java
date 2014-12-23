/**
 * Copyright 2014 Linagora, Université Joseph Fourier, Floralis
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
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import net.roboconf.core.model.RuntimeModelIo;
import net.roboconf.core.model.RuntimeModelIo.InstancesLoadResult;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.management.ManagedApplication;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ConfigurationUtils {

	public static final String APPLICATIONS = "applications";
	public static final String INSTANCES = "instances";


	/**
	 * Private constructor.
	 */
	private ConfigurationUtils() {
		// nothing
	}


	/**
	 * Given a configuration directory, this method finds the application directories.
	 * @param configurationDirectory the configuration directory
	 * @return a non-null list
	 */
	public static List<File> findApplicationDirectories( File configurationDirectory ) {

		List<File> result = new ArrayList<File> ();
		File[] files = new File( configurationDirectory, APPLICATIONS ).listFiles( new FileFilter() {
			@Override
			public boolean accept( File f ) {
				return f.isDirectory();
			}
		});

		if( files != null )
			result.addAll( Arrays.asList( files ));

		return result;
	}


	/**
	 * @param applicationName an application name
	 * @param configurationDirectory the configuration directory
	 * @return a non-null file that should point to an application directory
	 */
	public static File findApplicationdirectory( String applicationName, File configurationDirectory ) {
		return new File( configurationDirectory, APPLICATIONS + "/" + applicationName );
	}


	/**
	 * Saves the instances into a file.
	 * @param ma the application (not null)
	 * @param configurationDirectory the configuration directory
	 * @throws IOException if something went wrong
	 */
	public static void saveInstances( ManagedApplication ma, File configurationDirectory ) {

		Logger logger = Logger.getLogger( ConfigurationUtils.class.getName());
		File targetFile = new File( configurationDirectory, INSTANCES + "/" + ma.getName() + ".instances" );
		try {
			if( ! targetFile.getParentFile().exists()
					&& ! targetFile.getParentFile().mkdirs())
				logger.severe( targetFile + " could not be saved." );
			else
				RuntimeModelIo.writeInstances( targetFile, ma.getApplication().getRootInstances());

		} catch( IOException e ) {
			logger.severe( "Failed to save instances. " + e.getMessage());
			Utils.logException( logger, e );
		}
	}


	/**
	 * Deletes the instances definition for a given application.
	 * @param applicationName the application name
	 * @param configurationDirectory the configuration directory
	 */
	public static void deleteInstancesFile( String applicationName, File configurationDirectory ) {

		File targetFile = new File( configurationDirectory, INSTANCES + "/" + applicationName + ".instances" );
		if( targetFile.exists()
				&& ! targetFile.delete()) {
			Logger logger = Logger.getLogger( ConfigurationUtils.class.getName());
			logger.warning( "Instance file " + targetFile + " could not be deleted." );
		}
	}


	/**
	 * Restores instances and set them in the application.
	 * @param ma the application
	 * @param configurationDirectory the configuration directory
	 */
	public static InstancesLoadResult restoreInstances( ManagedApplication ma, File configurationDirectory ) {

		File instDirectory = new File( configurationDirectory, INSTANCES );
		File sourceFile = new File( instDirectory, ma.getName() + ".instances" );
		InstancesLoadResult result;
		if( sourceFile.exists())
			result = RuntimeModelIo.loadInstances( sourceFile, instDirectory, ma.getApplication().getGraphs(), ma.getApplication().getName());
		else
			result = new InstancesLoadResult();

		return result;
	}
}
