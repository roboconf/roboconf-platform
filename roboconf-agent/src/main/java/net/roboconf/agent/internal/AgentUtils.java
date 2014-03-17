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

package net.roboconf.agent.internal;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import net.roboconf.agent.AgentData;
import net.roboconf.core.internal.utils.Utils;

/**
 * @author Noël - LIG
 * @author Pierre-Yves Gibello - Linagora
 */
public final class AgentUtils {

	private static final String PROPERTY_APPLICATION_NAME = "applicationName";
	private static final String PROPERTY_MESSAGE_SERVER_IP = "ipMessagingServer";
	private static final String PROPERTY_ROOT_INSTANCE_NAME = "channelName";

	public static void configureLogger( Logger logger, String rootInstanceName ) {

        logger.setLevel(Level.ALL);
        Handler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Level.ALL);
        consoleHandler.setFormatter(new SimpleFormatter());
        logger.addHandler(consoleHandler);
        Handler fileHandler;
        try {
            File file = new File( System.getProperty( "java.io.tmpdir" ), "roboconf_agent_" + rootInstanceName + ".log" );
            file.createNewFile();
            fileHandler = new FileHandler( file.getAbsolutePath());
            fileHandler.setLevel(Level.ALL);
            fileHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(fileHandler);

        } catch (SecurityException e) {
            logger.severe( "Security exception: " + e.getMessage());

        } catch (IOException e) {
            logger.severe( "IO exception: " + e.getMessage());
        }
    }

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
    	result.setApplicationName( args[ 0 ]);
    	result.setRootInstanceName( args[ 1 ]);
    	result.setMessageServerIp( args[ 2 ]);
		result.setIpAddress( args[ 3 ]);

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
		String content = "";
		InputStream in = null;
		try {
			URL userDataUrl = new URL( "http://169.254.169.254/latest/user-data" );
			in = userDataUrl.openStream();
			ByteArrayOutputStream os = new ByteArrayOutputStream();

			Utils.copyStream( in, os );
			content = os.toString( "UTF-8" );

		} catch( IOException e ) {
			logger.severe( "The agent properties could not be read. " + e.getMessage());
			logger.finest( Utils.writeException( e ));

		} finally {
			Utils.closeQuietly( in );
		}

		// Parse them
		AgentData result = new AgentData();
		for( String line : content.split( "\n" )) {
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
		in = null;
		try {
			URL userDataUrl = new URL( "http://169.254.169.254/latest/meta-data/public-ipv4" );
			in = userDataUrl.openStream();
			ByteArrayOutputStream os = new ByteArrayOutputStream();

			Utils.copyStream( in, os );
			result.setIpAddress( os.toString( "UTF-8" ));

		} catch( IOException e ) {
			logger.severe( "The network properties could not be read. " + e.getMessage());
			logger.finest( Utils.writeException( e ));

		} finally {
			Utils.closeQuietly( in );
		}

		return result;
	}
}
