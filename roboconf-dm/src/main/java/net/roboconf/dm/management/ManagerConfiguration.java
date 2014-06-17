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
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import net.roboconf.core.model.io.RuntimeModelIo;
import net.roboconf.core.model.io.RuntimeModelIo.InstancesLoadResult;
import net.roboconf.core.utils.Utils;

/**
 * A class in charge of managing the configuration for the DM.
 * @author Vincent Zurczak - Linagora
 */
public final class ManagerConfiguration {

	private static final String ROBOCONF_DM_DIR = "ROBOCONF_DM_DIR";
	private static final String PROP_MESSAGING_IP = "messaging.ip";
	private static final String PROP_MESSAGING_USERNAME = "messaging.username";
	private static final String PROP_MESSAGING_PASSWORD = "messaging.password";

	static final String APPLICATIONS = "applications";
	static final String INSTANCES = "instances";
	static final String CONF = "conf";
	static final String CONF_PROPERTIES = "configuration.properties";

	private final Logger logger = Logger.getLogger( getClass().getName());
	private String messageServerIp, messageServerUsername, messageServerPassword;
	private File configurationDirectory;



	/**
	 * Constructor.
	 */
	private ManagerConfiguration() {
		// nothing
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


	/**
	 * Saves the instances into a file.
	 * @param ma the application
	 * @throws IOException if something went wrong
	 */
	public void saveInstances( ManagedApplication ma ) {

		File targetFile = new File( this.configurationDirectory, INSTANCES + "/" + ma.getName() + ".instances" );
		try {
			RuntimeModelIo.writeInstances( targetFile, ma.getApplication().getRootInstances());

		} catch( IOException e ) {
			this.logger.severe( "Failed to save instances. " + e.getMessage());
			this.logger.finest( Utils.writeException( e ));
		}
	}


	/**
	 * Restores instances and set them in the application.
	 * @param ma the application
	 */
	public InstancesLoadResult restoreInstances( ManagedApplication ma ) {

		File sourceFile = new File( this.configurationDirectory, INSTANCES + "/" + ma.getName() + ".instances" );
		InstancesLoadResult result;
		if( sourceFile.exists())
			result = RuntimeModelIo.loadInstances( sourceFile, ma.getApplication().getGraphs());
		else
			result = new InstancesLoadResult();

		return result;
	}


	/**
	 * @return the messageServerIp
	 */
	public String getMessageServerIp() {
		return this.messageServerIp;
	}


	/**
	 * @return the messageServerUsername
	 */
	public String getMessageServerUsername() {
		return this.messageServerUsername;
	}


	/**
	 * @return the messageServerPassword
	 */
	public String getMessageServerPassword() {
		return this.messageServerPassword;
	}


	/**
	 * @return the configurationDirectory
	 */
	public File getConfigurationDirectory() {
		return this.configurationDirectory;
	}


	/**
	 * Finds the configuration directory.
	 * @return a directory (that may not exist)
	 */
	public static File findConfigurationDirectory() {
		return findConfigurationDirectory( new EnvResolver());
	}


	/**
	 * Creates a configuration from given parameters.
	 * <p>
	 * The directory and its structure will be created if necessary.
	 * </p>
	 *
	 * @param configurationDirectory an existing directory
	 * @param messagingServerIp a non-null IP address
	 * @param messageServerUsername the user name to connect to the messaging server
	 * @param messageServerPassword the password to connect to the messaging server
	 * @return a non-null configuration
	 * @throws IOException if some directories could not be created
	 */
	public static ManagerConfiguration createConfiguration(
			File configurationDirectory,
			String messagingServerIp,
			String messageServerUsername,
			String messageServerPassword )
	throws IOException {

		// Create the structure
		if( ! configurationDirectory.exists()
				&& ! configurationDirectory.mkdirs())
			throw new IOException( "Could not create " + configurationDirectory );

		File f = new File( configurationDirectory, APPLICATIONS );
		if( ! f.exists() && ! f.mkdirs())
			throw new IOException( "Could not create " + f );

		f = new File( configurationDirectory, INSTANCES );
		if( ! f.exists() && ! f.mkdirs())
			throw new IOException( "Could not create " + f );

		f = new File( configurationDirectory, CONF );
		if( ! f.exists() && ! f.mkdirs())
			throw new IOException( "Could not create " + f );

		// Save the configuration
		Properties props = new Properties();
		props.setProperty( PROP_MESSAGING_IP, messagingServerIp );
		props.setProperty( PROP_MESSAGING_USERNAME, messageServerUsername );
		props.setProperty( PROP_MESSAGING_PASSWORD, messageServerPassword );
		f = new File( f, CONF_PROPERTIES  );

		FileOutputStream os = null;
		try {
			os = new FileOutputStream( f );
			props.store( os, "Temporary configuration" );

		} finally {
			Utils.closeQuietly( os );
		}

		// Create the configuration
		ManagerConfiguration conf = new ManagerConfiguration();
		conf.messageServerIp = messagingServerIp;
		conf.messageServerPassword = messageServerPassword;
		conf.messageServerUsername = messageServerUsername;
		conf.configurationDirectory = configurationDirectory;


		return conf;
	}


	/**
	 * Creates a temporary configuration from given parameters.
	 * <p>
	 * Equivalent to <code>createTemporaryConfiguration( configurationDirectory, "localhost" );</code>
	 * </p>
	 * <p>
	 * The directory and its structure will be created if necessary.
	 * </p>
	 *
	 * @param configurationDirectory an existing directory
	 * @return a non-null configuration
	 * @throws IOException if some directories could not be created
	 */
	public static ManagerConfiguration createConfiguration( File configurationDirectory ) throws IOException {
		return createConfiguration( configurationDirectory, "localhost", "guest", "guest" );
	}


	/**
	 * Loads a configuration from the given directory.
	 * <p>
	 * The directory structure should already exist.
	 * </p>
	 *
	 * @param configurationDirectory an existing directory
	 * @return a non-null configuration
	 * @throws IOException if the properties could not be loaded
	 */
	public static ManagerConfiguration loadConfiguration( File configurationDirectory ) throws IOException {

		Properties props = new Properties();
		File propertiesFile = new File( configurationDirectory, CONF + "/" + CONF_PROPERTIES );
		FileInputStream in = null;
		try {
			in = new FileInputStream( propertiesFile );
			props.load( in );

		} finally {
			Utils.closeQuietly( in );
		}

		ManagerConfiguration conf = new ManagerConfiguration();
		conf.configurationDirectory = configurationDirectory;
		conf.messageServerIp = props.getProperty( PROP_MESSAGING_IP );
		conf.messageServerUsername = props.getProperty( PROP_MESSAGING_USERNAME );
		conf.messageServerPassword = props.getProperty( PROP_MESSAGING_PASSWORD );

		return conf;
	}


	/**
	 * Finds the configuration directory (for test and mocking purpose).
	 * @return a directory (that may not exist)
	 */
	static File findConfigurationDirectory( EnvResolver envResolver ) {

		String loc = envResolver.findEnvironmentVariable( ROBOCONF_DM_DIR );
		File result = loc != null ? new File( loc ) : new File( System.getProperty( "user.home" ), "roboconf_dm" );

		return result;
	}


	/**
	 * A class to mock access to environment variables.
	 * @author Vincent Zurczak - Linagora
	 */
	static class EnvResolver {

		/**
		 * Finds the value of an environment variable.
		 * @param name the variable name
		 * @return its value
		 */
		String findEnvironmentVariable( String name ) {
			return System.getenv( name );
		}
	}
}
