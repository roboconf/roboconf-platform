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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import net.roboconf.core.model.io.RuntimeModelIo;
import net.roboconf.core.model.io.RuntimeModelIo.InstancesLoadResult;
import net.roboconf.core.utils.Utils;
import net.roboconf.messaging.client.IDmClient;
import net.roboconf.messaging.client.MessageServerClientFactory;

/**
 * A class in charge of managing the configuration for the DM.
 * @author Vincent Zurczak - Linagora
 */
public class ManagerConfiguration {

	// Constants
	static final String APPLICATIONS = "applications";
	static final String INSTANCES = "instances";

	// Fields injected by OSGi Config Admin or by iPojo
	private String messageServerIp, messageServerUsername, messageServerPassword, configurationDirectoryLocation;
	private Manager manager;

	// Inferred fields
	IDmClient messagingClient;
	private MessageServerClientFactory messgingFactory = new MessageServerClientFactory();
	private final Logger logger = Logger.getLogger( getClass().getName());
	private File configurationDirectory;
	private boolean validConfiguration = false;



	/**
	 * Constructor.
	 */
	public ManagerConfiguration() {
		this( new File( System.getProperty( "java.io.tmpdir" ), "roboconf-dm" ));
	}


	/**
	 * Constructor.
	 * @param directory a directory
	 */
	public ManagerConfiguration( File directory ) {

		this.messageServerIp = "localhost";
		this.messageServerUsername = "guest";
		this.messageServerPassword = "guest";
		this.configurationDirectoryLocation = directory.getAbsolutePath();
	}


	/**
	 * Constructor.
	 * @param messageServerIp
	 * @param messageServerUsername
	 * @param messageServerPassword
	 * @param configurationDirectory
	 */
	public ManagerConfiguration(
			String messageServerIp,
			String messageServerUsername,
			String messageServerPassword,
			File configurationDirectory ) {
		this( messageServerIp, messageServerUsername, messageServerPassword, configurationDirectory.getAbsolutePath());
	}


	/**
	 * Constructor.
	 * @param messageServerIp
	 * @param messageServerUsername
	 * @param messageServerPassword
	 * @param configurationDirectoryLocation
	 */
	public ManagerConfiguration(
			String messageServerIp,
			String messageServerUsername,
			String messageServerPassword,
			String configurationDirectoryLocation ) {

		this.messageServerIp = messageServerIp;
		this.messageServerUsername = messageServerUsername;
		this.messageServerPassword = messageServerPassword;
		this.configurationDirectoryLocation = configurationDirectoryLocation;
	}


	/**
	 * @return true if and only if the configuration is valid
	 */
	public boolean isValidConfiguration() {
		return this.validConfiguration;
	}


	/**
	 * This method handles the manager's reconfiguration when a parameter changes.
	 */
	public void update() {

		try {
			// Deal with the directory
			this.configurationDirectory = new File( this.configurationDirectoryLocation );
			if( ! this.configurationDirectory.isDirectory()
					&& ! this.configurationDirectory.mkdirs())
				throw new IOException( "Could not create " + this.configurationDirectory );

			File f = new File( this.configurationDirectory, APPLICATIONS );
			if( ! f.isDirectory() && ! f.mkdirs())
				throw new IOException( "Could not create " + f );

			f = new File( this.configurationDirectory, INSTANCES );
			if( ! f.isDirectory() && ! f.mkdirs())
				throw new IOException( "Could not create " + f );

			// Create a new messaging client
			closeConnection();

			this.messagingClient = this.messgingFactory.createDmClient();
			this.messagingClient.setParameters( this.messageServerIp, this.messageServerUsername, this.messageServerPassword );

			// The manager should not be null at runtime (iPojo guarantees it).
			// But this test is useful for unit testing.
			if( this.manager != null )
				this.manager.configurationChanged();

			this.validConfiguration = true;

		} catch( IOException e ) {
			this.logger.warning( "An error occurred while reconfiguring the manager. " + e.getMessage());
			this.logger.finest( Utils.writeException( e ));
			this.validConfiguration = false;
		}

		this.logger.info( "The DM configuration was updated..." );
	}


	/**
	 * Closes the manager's connection.
	 */
	public void closeConnection() {

		try {
			if( this.messagingClient != null
					&& this.messagingClient.isConnected())
				this.messagingClient.closeConnection();

		} catch( IOException e ) {
			this.logger.warning( "An error occurred while closing the manager's connection. " + e.getMessage());
			this.logger.finest( Utils.writeException( e ));
		}
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
	 * Deletes the instances definition for a given application.
	 * @param applicationName the application name
	 */
	public void deleteInstancesFile( String applicationName ) {

		File targetFile = new File( this.configurationDirectory, INSTANCES + "/" + applicationName + ".instances" );
		if( targetFile.exists()
				&& ! targetFile.delete())
			this.logger.warning( "Instance file " + targetFile + " could not be deleted." );
	}


	/**
	 * Restores instances and set them in the application.
	 * @param ma the application
	 */
	public InstancesLoadResult restoreInstances( ManagedApplication ma ) {

		File sourceFile = new File( this.configurationDirectory, INSTANCES + "/" + ma.getName() + ".instances" );
		InstancesLoadResult result;
		if( sourceFile.exists())
			result = RuntimeModelIo.loadInstances( sourceFile, ma.getApplication().getGraphs(), ma.getApplication().getName());
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
	 * @param messgingFactory the messgingFactory to set
	 */
	public void setMessgingFactory( MessageServerClientFactory messgingFactory ) {
		this.messgingFactory = messgingFactory;
	}


	/**
	 * @param manager the manager to set
	 */
	public void setManager( Manager manager ) {
		this.manager = manager;
	}


	/**
	 * @return the manager
	 */
	public Manager getManager() {
		return this.manager;
	}


	/**
	 * @return the configurationDirectoryLocation
	 */
	public String getConfigurationDirectoryLocation() {
		return this.configurationDirectoryLocation;
	}


	/**
	 * @param configurationDirectoryLocation the configurationDirectoryLocation to set
	 */
	public void setConfigurationDirectoryLocation( String configurationDirectoryLocation ) {
		this.configurationDirectoryLocation = configurationDirectoryLocation;
	}


	/**
	 * @param messageServerIp the messageServerIp to set
	 */
	public void setMessageServerIp( String messageServerIp ) {
		this.messageServerIp = messageServerIp;
	}


	/**
	 * @param messageServerUsername the messageServerUsername to set
	 */
	public void setMessageServerUsername( String messageServerUsername ) {
		this.messageServerUsername = messageServerUsername;
	}


	/**
	 * @param messageServerPassword the messageServerPassword to set
	 */
	public void setMessageServerPassword( String messageServerPassword ) {
		this.messageServerPassword = messageServerPassword;
	}
}
