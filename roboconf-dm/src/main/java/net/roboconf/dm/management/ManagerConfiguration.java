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

package net.roboconf.dm.management;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import net.roboconf.core.utils.Utils;

/**
 * A class in charge of managing the configuration for the DM.
 * @author Vincent Zurczak - Linagora
 */
public class ManagerConfiguration {

	private static final String ROBOCONF_DM_DIR = "ROBOCONF_DM_DIR";
	private static final String PROP_MESSAGING_IP = "messaging.ip";

	private static final String APPLICATIONS = "applications";
	private static final String INSTANCES = "instances";
	private static final String CONF = "conf";

	private String messageServerIp;
	private File configurationDirectory;


	/**
	 * Loads a configuration from a given directory.
	 * @param configurationDirectory an existing directory
	 * @throws IOException if something went wrong
	 */
	public void load( File configurationDirectory ) throws IOException {

		this.configurationDirectory = configurationDirectory;

		Properties props = new Properties();
		File conf = new File( this.configurationDirectory, CONF + "/" + "configuration.properties" );
		FileInputStream in = null;
		try {
			in = new FileInputStream( conf );
			props.load( in );

		} finally {
			Utils.closeQuietly( in );
		}

		this.messageServerIp = props.getProperty( PROP_MESSAGING_IP );
	}


	/**
	 * Given a configuration directory, this method finds the application directories.
	 * @return a non-null list
	 */
	public List<File> findApplicationDirectories() {

		List<File> result = new ArrayList<File> ();
		File[] files = new File( this.configurationDirectory, APPLICATIONS ).listFiles( new FileFilter() {
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
	 * @return a non-null file that should point to an application directory
	 */
	public File findApplicationdirectory( String applicationName ) {
		return new File( this.configurationDirectory, APPLICATIONS + "/" + applicationName );
	}


	public void saveInstances( ManagedApplication ma ) {

	}


	public void restoreInstances( ManagedApplication ma ) {

	}


	/**
	 * @return the messageServerIp
	 */
	public String getMessageServerIp() {
		return this.messageServerIp;
	}


	/**
	 * Finds the configuration directory.
	 * @return a file (that may not exist)
	 * @throws IOException
	 */
	public static File findConfigurationDirectory() {

		String loc = System.getenv( ROBOCONF_DM_DIR );
		File result = loc != null ? new File( loc ) : new File( System.getProperty( "user.home" ), "roboconf_dm" );

		return result;
	}


	public static ManagerConfiguration createConfiguration( File configurationDirectory, String messagingServerIp ) {

		return null;
	}


	public static ManagerConfiguration createConfigurationDirectory( File configurationDirectory ) {

		return null;
	}
}
