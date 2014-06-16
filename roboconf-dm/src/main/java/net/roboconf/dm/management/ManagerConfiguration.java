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
public class ManagerConfiguration {

	private static final String ROBOCONF_DM_DIR = "ROBOCONF_DM_DIR";
	private static final String PROP_MESSAGING_IP = "messaging.ip";

	private static final String APPLICATIONS = "applications";
	private static final String INSTANCES = "instances";
	private static final String CONF = "conf";
	private static final String CONF_PROPERTIES = "configuration.properties";

	private final Logger logger = Logger.getLogger( getClass().getName());
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
		File conf = new File( this.configurationDirectory, CONF + "/" + CONF_PROPERTIES );
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


	/**
	 * Saves the instances into a file.
	 * @param ma the application
	 * @throws IOException if something went wrong
	 */
	public void saveInstances( ManagedApplication ma ) {

		File targetFile = new File( this.configurationDirectory, ma.getName() + ".instances" );
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
	public void restoreInstances( ManagedApplication ma ) {

		File sourceFile = new File( this.configurationDirectory, ma.getName() + ".instances" );
		InstancesLoadResult ilr = RuntimeModelIo.loadInstances( sourceFile, ma.getApplication().getGraphs());

		// TODO: what happens if there are errors?
		ma.getApplication().getRootInstances().clear();;
		ma.getApplication().getRootInstances().addAll( ilr.getRootInstances());
	}


	/**
	 * @return the messageServerIp
	 */
	public String getMessageServerIp() {
		return this.messageServerIp;
	}


	/**
	 * Finds the configuration directory.
	 * @return a directory (that may not exist)
	 */
	public static File findConfigurationDirectory() {

		String loc = System.getenv( ROBOCONF_DM_DIR );
		File result = loc != null ? new File( loc ) : new File( System.getProperty( "user.home" ), "roboconf_dm" );

		return result;
	}


	/**
	 * Creates a configuration from given parameters.
	 * <p>
	 * The directory and its structure will be created if necessary.
	 * </p>
	 *
	 * @param configurationDirectory an existing directory
	 * @param messagingServerIp a non-null IP address
	 * @return a non-null configuration
	 * @throws IOException if some directories could not be created
	 */
	public static ManagerConfiguration createTemporaryConfiguration( File configurationDirectory, String messagingServerIp )
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

		// Save the configuration
		Properties props = new Properties();
		props.setProperty( "PROP_MESSAGING_IP", messagingServerIp );
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
	public static ManagerConfiguration createTemporaryConfiguration( File configurationDirectory ) throws IOException {
		return createTemporaryConfiguration( configurationDirectory, "localhost" );
	}
}
