/**
 * Copyright 2014-2016 Linagora, Université Joseph Fourier, Floralis
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
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import net.roboconf.core.Constants;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.utils.ProgramUtils;
import net.roboconf.core.utils.Utils;

/**
 * @author Noël - LIG
 * @author Pierre-Yves Gibello - Linagora
 * @author Amadou Diarra - UGA
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
	 * @param fileNameToFileContent the files to write down (key = relative file location, value = file's content)
	 * @throws IOException if the copy encountered a problem
	 */
	public static void copyInstanceResources( Instance instance, Map<String,byte[]> fileNameToFileContent )
	throws IOException {

		File dir = InstanceHelpers.findInstanceDirectoryOnAgent( instance );
		Utils.createDirectory( dir );

		if( fileNameToFileContent != null ) {
			for( Map.Entry<String,byte[]> entry : fileNameToFileContent.entrySet()) {

				File f = new File( dir, entry.getKey());
				Utils.createDirectory( f.getParentFile());

				ByteArrayInputStream in = new ByteArrayInputStream( entry.getValue());
				Utils.copyStream( in, f );
			}
		}
	}


	/**
	 * Executes a script resource on a given instance.
	 * @param instance an instance
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public static void excuteScriptResource( Instance instance, String applicationName, String scopedInstanceName ) throws IOException, InterruptedException {
		File dir = InstanceHelpers.findInstanceDirectoryOnAgent( instance );
		File script = new File(dir,Constants.TARGET_SCRIPT_FILE_NAME);
		Logger logger = Logger.getLogger( AgentUtils.class.getName());
		if( script.exists()) {
			if(! script.canExecute())
				script.setExecutable(true);

			String[] command = { script.getAbsolutePath() };
			int exitCode = ProgramUtils.executeCommand(logger, command, script.getParentFile(), null, applicationName, scopedInstanceName);
			if( exitCode != 0 )
				throw new IOException( "Script execution failed. Exit code: " + exitCode );
		} else {
			logger.severe("There is no script to execute");
		}
	}

	/**
	 * Deletes the resources for a given instance.
	 * @param instance an instance
	 * @throws IOException if resources could not be deleted
	 */
	public static void deleteInstanceResources( Instance instance )	throws IOException {
		File dir = InstanceHelpers.findInstanceDirectoryOnAgent( instance );
		Utils.deleteFilesRecursively( dir );
	}


	/**
	 * Changes the level of the Roboconf logger.
	 * <p>
	 * This method assumes the default log configuration for a Roboconf
	 * distribution has a certain shape. If a user manually changed the log
	 * configuration (not the level, but the logger name, as an example), then
	 * this will not work.
	 * </p>
	 */
	public static void changeRoboconfLogLevel( String logLevel, String etcDir )
	throws IOException {

		if( ! Utils.isEmptyOrWhitespaces( etcDir )) {

			File f = new File( etcDir, AgentConstants.KARAF_LOG_CONF_FILE );
			if( f.exists()) {
				Properties props = Utils.readPropertiesFile( f );
				props.put( "log4j.logger.net.roboconf", logLevel + ", roboconf" );
				Utils.writePropertiesFile( props, f );
			}
		}
	}


	/**
	 * Collect the main log files into a map.
	 * @param karafData the Karaf's data directory
	 * @return a non-null map
	 */
	public static Map<String,byte[]> collectLogs( String karafData ) throws IOException {

		Map<String,byte[]> logFiles = new HashMap<>( 2 );
		if( ! Utils.isEmptyOrWhitespaces( karafData )) {

			String[] names = { "karaf.log", "roboconf.log" };
			for( String name : names ) {
				File log = new File( karafData, AgentConstants.KARAF_LOGS_DIRECTORY + "/" + name );
				if( ! log.exists())
					continue;

				String content = Utils.readFileContent( log );
				logFiles.put( name, content.getBytes( "UTF-8" ));
			}
		}

		return logFiles;
	}


	/**
	 * Finds the IP address of the current machine.
	 * @param networkInterface the network interface to use
	 * <p>
	 * If this network interface does not exist, the default one is picked up,
	 * as returned by <code>InetAddress.getLocalHost().getHostAddress()</code>.
	 * </p>
	 *
	 * @return a non-null IP address (127.0.0.1 in case of error)
	 */
	public static String findIpAddress( String networkInterface ) {

		String ipAddress = null;
		Logger logger = Logger.getLogger( AgentUtils.class.getName());
		try {
			// Try the network interface
			NetworkInterface nif;
			if( networkInterface != null
					&& ! AgentConstants.DEFAULT_NETWORK_INTERFACE.equalsIgnoreCase( networkInterface )
					&& (nif = NetworkInterface.getByName( networkInterface )) != null ) {

				Enumeration<InetAddress> addrs = nif.getInetAddresses();
				while( addrs.hasMoreElements()
						&& (ipAddress == null || ipAddress.startsWith( "127.0" ))) {

					Object obj = addrs.nextElement();
					if( !( obj instanceof Inet4Address ))
						continue;

					ipAddress =  obj.toString();
					if( ipAddress.startsWith( "/" ))
						ipAddress = ipAddress.substring( 1 );
				}

			} else if( ! AgentConstants.DEFAULT_NETWORK_INTERFACE.equalsIgnoreCase( networkInterface )) {
				logger.severe( "Network interface " + networkInterface + " does not exists. The host's default IP will be picked up." );
			}

			// Otherwise, use the default address
			if( ipAddress == null ) {
				logger.fine( "Picking up the host's default IP address." );
				ipAddress = InetAddress.getLocalHost().getHostAddress();
			}

		} catch( Exception e ) {
			ipAddress = "127.0.0.1";
			logger.warning( "The IP address could not be found. " + e.getMessage());
			Utils.logException( logger, e );
		}

		logger.info( "The agent's address was resolved to " + ipAddress );
		return ipAddress;
	}
}
