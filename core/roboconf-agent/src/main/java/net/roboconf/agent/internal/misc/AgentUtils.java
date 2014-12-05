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

package net.roboconf.agent.internal.misc;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Map;

import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.utils.Utils;

/**
 * @author Noël - LIG
 * @author Pierre-Yves Gibello - Linagora
 */
public final class AgentUtils {

	/**
	 * Private empty constructor.
	 */
	private AgentUtils() {
		// nothing
	}


	/**
	 * Checks the syntax of an IP address (basic check, not exhaustive).
	 * @param ip The IP to check
	 * @return true if IP looks like an IP address, false otherwise
	 */
	public static boolean isValidIP( String ip ) {

		boolean result = false;
		try {
			String[] parts;
			if( ! Utils.isEmptyOrWhitespaces( ip )
					&& (parts = ip.split("\\.")).length == 4 ) {

				result = true;
				for( String s : parts ) {
					int part = Integer.parseInt( s );
					if( part < 0 || part > 255 ) {
						result = false;
						break;
					}
				}
			}

		} catch( NumberFormatException e ) {
			result = false;
		}

		return result;
	}


	/**
	 * Copies the resources of an instance on the disk.
	 * @param instance an instance
	 * @param pluginName the plug-in's name
	 * @param fileNameToFileContent the files to write down (key = relative file location, value = file's content)
	 * @throws IOException if the copy encountered a problem
	 */
	public static void copyInstanceResources( Instance instance, String pluginName, Map<String,byte[]> fileNameToFileContent )
	throws IOException {

		File dir = InstanceHelpers.findInstanceDirectoryOnAgent( instance, pluginName );
		if( ! dir.isDirectory()
				&& ! dir.mkdirs())
			throw new IOException( "The directory " + dir.getAbsolutePath() + " could not be created." );

		if( fileNameToFileContent != null ) {
			for( Map.Entry<String,byte[]> entry : fileNameToFileContent.entrySet()) {

				File f = new File( dir, entry.getKey());
				if( ! f.getParentFile().isDirectory()
						&& ! f.getParentFile().mkdirs())
					throw new IOException( "The directory " + f.getParentFile() + " could not be created." );

				ByteArrayInputStream in = new ByteArrayInputStream( entry.getValue());
				Utils.copyStream( in, f );
			}
		}
	}


	/**
	 * Deletes the resources for a given instance.
	 * @param instance an instance
	 * @param pluginName the plug-in's name
	 * @throws IOException if resources could not be deleted
	 */
	public static void deleteInstanceResources( Instance instance, String pluginName )
	throws IOException {
		File dir = InstanceHelpers.findInstanceDirectoryOnAgent( instance, pluginName );
		Utils.deleteFilesRecursively( dir );
	}
}
