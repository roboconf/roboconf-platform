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
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.codec.binary.Base64;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

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
			String ip = os.toString( "UTF-8" );
			if(! isValidIP(ip)) {
				// Failed retrieving public IP: try private one instead
				Utils.closeQuietly( in );
				userDataUrl = new URL( "http://169.254.169.254/latest/meta-data/local-ipv4" );
				in = userDataUrl.openStream();
				os = new ByteArrayOutputStream();

				Utils.copyStream( in, os );
				ip = os.toString( "UTF-8" );
			}
			if(! isValidIP(ip)) throw new IOException("Can\'t retrieve IP address (either public-ipv4 or local-ipv4)");
			
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
	
	private static String getSpecificAttributeOfTagInXMLFile(String filePath, String tagName, String attrName) throws ParserConfigurationException, SAXException, IOException {
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
	 * Check IP address syntax (basic check, not exhaustive)
	 * @param ip The IP to check
	 * @return true if ip looks like an IP address, false otherwise
	 */
	private static boolean isValidIP(String ip) {
		try {
	        if (ip == null || ip.trim().isEmpty()) return false;

	        String[] parts = ip.split("\\.");
	        if (parts.length != 4) return false;

	        for (String s : parts) {
	            int part = Integer.parseInt(s);
	            if (part < 0 || part > 255) return false;
	        }

	        return true;
	    } catch (NumberFormatException e) {
	        return false;
	    }
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return result;
	}
}
