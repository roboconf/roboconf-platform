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

package net.roboconf.agent;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.logging.Logger;

import net.roboconf.core.internal.utils.Utils;

/**
 * @author Noël - LIG
 * @author Pierre-Yves Gibello - Linagora
 */
public final class AgentUtils {

	private static final String PROPERTY_APPLICATION_NAME = "application-name";
	private static final String PROPERTY_MESSAGE_SERVER_IP = "message-server-ip";
	private static final String PROPERTY_ROOT_INSTANCE_NAME = "root-instance-name";


	/**
	 * Private empty constructor.
	 */
	private AgentUtils() {
		// nothing
	}


	/**
	 * Configures the agent from the program arguments.
	 * @param args the program arguments
	 * @return the agent's data
	 */
	public static AgentData findParametersInProgramArguments( String[] args ) {

		AgentData result = new AgentData();
    	result.setApplicationName( args[ 1 ]);
    	result.setRootInstanceName( args[ 2 ]);
    	result.setMessageServerIp( args[ 3 ]);
		result.setIpAddress( args[ 4 ]);

		return result;
	}


	/**
	 * Configures the agent from a local properties file.
	 * @param logger a logger
	 * @param filePath the file path
	 * @return the agent's data
	 */
	public static AgentData findParametersInPropertiesFile( Logger logger, String filePath ) {

    	File propertiesFile = new File( filePath );
    	while( ! propertiesFile.exists()
    			|| ! propertiesFile.canRead()) {

    		try {
    			Thread.sleep( 2000 );

    		} catch( InterruptedException e ) {
    			logger.finest( Utils.writeException( e  ));
    		}
    	}

    	// Properties file found... proceed !
    	Properties props = new Properties();
    	FileInputStream in = null;
    	try {
			in = new FileInputStream( propertiesFile );
			props.load( in );

		} catch( IOException e ) {
			logger.severe( "The agent properties could not be read. " + e.getMessage());
			logger.finest( Utils.writeException( e ));

		} finally {
			Utils.closeQuietly( in );
		}

    	AgentData result = new AgentData();
    	result.setApplicationName( props.getProperty( PROPERTY_APPLICATION_NAME ));
    	result.setRootInstanceName( props.getProperty( PROPERTY_ROOT_INSTANCE_NAME ));
    	result.setMessageServerIp( props.getProperty( PROPERTY_MESSAGE_SERVER_IP ));
    	try {
			result.setIpAddress( InetAddress.getLocalHost().getHostAddress());

		} catch( UnknownHostException e ) {
			logger.severe( "The IP address could not be found. " + e.getMessage());
			logger.finest( Utils.writeException( e ));
		}

    	return result;
	}


	/**
	 * Configures the agent from a IaaS registry.
	 * @param logger a logger
	 * @return the agent's data
	 * FIXME: this is too specific for EC2.
	 */
	public static AgentData findParametersInWsInfo( Logger logger ) {

		// Copy the user data
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		InputStream in = null;
		try {
			URL userDataUrl = new URL( "http://instance-data/latest/user-data" );
			in = userDataUrl.openStream();
			Utils.copyStream( in, os );

		} catch( IOException e ) {
			logger.severe( "The agent properties could not be read. " + e.getMessage());
			logger.finest( Utils.writeException( e ));

		} finally {
			Utils.closeQuietly( in );
		}

		// Parse them
		AgentData result = new AgentData();
		for( String line : os.toString().split( "\n" )) {
			line = line.trim();

			if( line.startsWith( PROPERTY_APPLICATION_NAME )) {
				String[] data = line.split( "=" );
				result.setApplicationName(data[ data.length - 1 ]);

			} else if( line.startsWith( PROPERTY_MESSAGE_SERVER_IP )) {
				String[] data = line.split( "=" );
				result.setMessageServerIp( data[ data.length - 1 ]);

			} else if( line.startsWith( PROPERTY_ROOT_INSTANCE_NAME )) {
				String[] data = line.split( "=" );
				result.setRootInstanceName( data[ data.length - 1 ]);
			}
		}

		// FIXME VZ: seriously, why do we need to ask our IP address?
		os = new ByteArrayOutputStream();
		in = null;
		try {
			URL userDataUrl = new URL( "http://instance-data/latest/meta-data/public-ipv4" );
			in = userDataUrl.openStream();
			Utils.copyStream( in, os );
			result.setIpAddress( os.toString());

		} catch( IOException e ) {
			logger.severe( "The network properties could not be read. " + e.getMessage());
			logger.finest( Utils.writeException( e ));

		} finally {
			Utils.closeQuietly( in );
		}

		return result;
	}
}
