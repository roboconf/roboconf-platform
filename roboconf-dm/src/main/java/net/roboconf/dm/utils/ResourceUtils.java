/**
 * Copyright 2014 Linagora, Université Joseph Fourier
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

package net.roboconf.dm.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.roboconf.core.Constants;
import net.roboconf.core.internal.utils.Utils;
import net.roboconf.core.model.runtime.Instance;

/**
 * @author Vincent Zurczak - Linagora
 */
public final class ResourceUtils {

	/**
	 * Private empty constructor.
	 */
	private ResourceUtils() {
		// nothing
	}


	/**
	 * Stores the instance resources into a map.
	 * @param applicationFilesDirectory the application's directory
	 * @param instance an instance (not null)
	 * @return a non-null map (key = the file location, relative to the instance's directory, value = file content)
	 * @throws IOException if something went wrong while reading a file
	 */
	public static Map<String,byte[]> storeInstanceResources( File applicationFilesDirectory, Instance instance ) throws IOException {

		File instanceResourcesDirectory = findInstanceResourcesDirectory( applicationFilesDirectory, instance );
		if( ! instanceResourcesDirectory.exists())
			throw new IOException( "The resource directory was not found for instance " + instance.getName() + ". " + instanceResourcesDirectory.getAbsolutePath());

		if( ! instanceResourcesDirectory.isDirectory())
			throw new IOException( "The resource directory for instance " + instance.getName() + " is not a valid directory. " + instanceResourcesDirectory.getAbsolutePath());

		Map<String,byte[]> result = new HashMap<String,byte[]> ();
		List<File> resourceFiles = listAllFiles( instanceResourcesDirectory );
		for( File file : resourceFiles ) {

			String key = computeFileRelativeLocation( instanceResourcesDirectory, file );
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			Utils.copyStream( file, os );
			result.put( key, os.toByteArray());
		}

		return result;
	}


	/**
	 * Finds the resource directory for an instance.
	 * @param applicationFilesDirectory the application's directory
	 * @param instance an instance
	 * @return a non-null file (that may not exist)
	 */
	public static File findInstanceResourcesDirectory( File applicationFilesDirectory, Instance instance ) {
		return findInstanceResourcesDirectory( applicationFilesDirectory, instance.getComponent().getName());
	}


	/**
	 * Finds the resource directory for an instance.
	 * @param applicationFilesDirectory the application's directory
	 * @param componentName the component name
	 * @return a non-null file (that may not exist)
	 */
	public static File findInstanceResourcesDirectory( File applicationFilesDirectory, String componentName ) {

		File result = new File( applicationFilesDirectory, Constants.PROJECT_DIR_GRAPH );
		result = new File( result, componentName);

		return result;
	}


	/**
	 * Finds all the files (direct and indirect) from a directory.
	 * <p>
	 * This method skips hidden files and files whose name starts
	 * with a dot.
	 * </p>
	 *
	 * @param directory an existing directory
	 * @return a non-null list of files
	 */
	public static List<File> listAllFiles( File directory ) {

		if( ! directory.exists()
				|| ! directory.isDirectory())
			throw new IllegalArgumentException( directory.getAbsolutePath() + " does not exist or is not a directory." );

		List<File> result = new ArrayList<File> ();
		List<File> directoriesToInspect = new ArrayList<File> ();
		directoriesToInspect.add( directory );

		while( ! directoriesToInspect.isEmpty()) {
			File[] subFiles = directoriesToInspect.remove( 0 ).listFiles();
			if( subFiles == null )
				continue;

			for( File subFile : subFiles ) {
				if(  subFile.isHidden()
						|| subFile.getName().startsWith( "." ))
					continue;

				if( subFile.isFile())
					result.add( subFile );
				else
					directoriesToInspect.add( subFile );
			}
		}

		return result;
	}


	/**
	 * Computes the relative location of a file with respect to a root directory.
	 * @param rootDirectory a directory
	 * @param subFile a file contained (directly or indirectly) in the directory
	 * @return a non-null string
	 */
	public static String computeFileRelativeLocation( File rootDirectory, File subFile ) {

		String rootPath = rootDirectory.getAbsolutePath();
		String subPath = subFile.getAbsolutePath();
		if(  ! subPath.startsWith( rootPath ))
			throw new IllegalArgumentException( "The sub-file must be contained in the directory." );

		if(  rootDirectory.equals( subFile ))
			throw new IllegalArgumentException( "The sub-file must be different than the directory." );

		return subPath.substring( rootPath.length() + 1 ).replace( '\\', '/' );
	}
}
