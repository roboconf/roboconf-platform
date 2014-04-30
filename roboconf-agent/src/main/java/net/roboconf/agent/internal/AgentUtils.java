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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import net.roboconf.agent.AgentData;
import net.roboconf.core.internal.utils.Utils;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.runtime.Instance;

import org.apache.commons.codec.binary.Base64;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * @author Noël - LIG
 * @author Pierre-Yves Gibello - Linagora
 */
public final class AgentUtils {

	private static final String PROPERTY_APPLICATION_NAME = "applicationName";
	private static final String PROPERTY_MESSAGE_SERVER_IP = "ipMessagingServer";
	private static final String PROPERTY_ROOT_INSTANCE_NAME = "channelName";



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

		// We need to ask our IP address because we may have several network interfaces.
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

	private static String getValueOfTagInXMLFile(String filePath, String tagName) throws ParserConfigurationException, SAXException, IOException {

		File fXmlFile = new File(filePath);
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(fXmlFile);

		//optional, but recommended
		//read this - http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
		doc.getDocumentElement().normalize();

		NodeList nList = doc.getElementsByTagName(tagName);
	    String valueOfTagName = "";

		for (int temp = 0; temp < nList.getLength(); temp++) {
			Node nNode = nList.item(temp);
			valueOfTagName = nNode.getTextContent();
		}

		return valueOfTagName;
	}

	private static String getSpecificAttributeOfTagInXMLFile(String filePath, String tagName, String attrName)
	throws ParserConfigurationException, SAXException, IOException {

		File fXmlFile = new File(filePath);
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(fXmlFile);

		doc.getDocumentElement().normalize();

		NodeList nList = doc.getElementsByTagName(tagName);
	    Node aNode = nList.item(2);
	    NamedNodeMap attributes = aNode.getAttributes();
	    String attrValue = "";
	    for (int a = 0; a < attributes.getLength(); a++) {
	      Node theAttribute = attributes.item(a);
	      if (attrName.equals(theAttribute.getNodeName())) attrValue = theAttribute.getTextContent().split(":")[0];
	    }

	    return attrValue;
	}

	/**
	 * Configures the agent from a IaaS registry.
	 * @param logger a logger
	 * @return the agent's data
	 * This is dedicated to Azure.
	 */
	public static AgentData findParametersForAzure( Logger logger ) {

		String content = "";
		try {
			// Get the user data from /var/lib/waagent/ovf-env.xml and decode it
			String userDataEncoded = getValueOfTagInXMLFile("/var/lib/waagent/ovf-env.xml", "CustomData");
			content = new String( Base64.decodeBase64( userDataEncoded.getBytes( "UTF-8" ) ));

		} catch( IOException e ) {
			logger.severe( "The agent properties could not be read. " + e.getMessage());
			logger.finest( Utils.writeException( e ));

		} catch (ParserConfigurationException e) {
			logger.severe( "The agent properties could not be read. " + e.getMessage());
			logger.finest( Utils.writeException( e ));

		} catch (SAXException e) {
			logger.severe( "The agent properties could not be read. " + e.getMessage());
			logger.finest( Utils.writeException( e ));
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

		// Get the public IP Address from /var/lib/waagent/SharedConfig.xml
		String publicIPAddress;
		try {
			publicIPAddress = getSpecificAttributeOfTagInXMLFile("/var/lib/waagent/SharedConfig.xml", "Endpoint", "loadBalancedPublicAddress");
			result.setIpAddress( publicIPAddress );

		} catch (ParserConfigurationException e) {
			logger.severe( "The agent could not retrieve a public IP address. " + e.getMessage());
			logger.finest( Utils.writeException( e ));

		} catch (SAXException e) {
			logger.severe( "The agent could not retrieve a public IP address. " + e.getMessage());
			logger.finest( Utils.writeException( e ));

		} catch (IOException e) {
			logger.severe( "The agent could not retrieve a public IP address. " + e.getMessage());
			logger.finest( Utils.writeException( e ));
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
		if( ! dir.exists()
				&& ! dir.mkdirs())
			throw new IOException( "The directory " + dir.getAbsolutePath() + " could not be created." );

		for( Map.Entry<String,byte[]> entry : fileNameToFileContent.entrySet()) {

			File f = new File( dir, entry.getKey());
			if( ! f.getParentFile().exists()
					&& ! f.getParentFile().mkdirs())
				throw new IOException( "The directory " + f.getParentFile() + " could not be created." );

			ByteArrayInputStream in = new ByteArrayInputStream( entry.getValue());
			Utils.copyStream( in, f );
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
