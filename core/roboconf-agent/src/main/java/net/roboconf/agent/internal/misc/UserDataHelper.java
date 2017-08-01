/**
 * Copyright 2014-2017 Linagora, Université Joseph Fourier, Floralis
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
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

import net.roboconf.agent.internal.AgentProperties;
import net.roboconf.core.Constants;
import net.roboconf.core.utils.UriUtils;
import net.roboconf.core.utils.Utils;
import net.roboconf.messaging.api.MessagingConstants;

/**
 * No static method so that we can mock this class.
 * @author Noël - LIG
 * @author Pierre-Yves Gibello - Linagora
 */
public class UserDataHelper {

	/**
	 * Configures the agent from a IaaS registry.
	 * @param logger a logger
	 * @return the agent's data, or null if they could not be parsed
	 */
	public AgentProperties findParametersForAmazonOrOpenStack( Logger logger ) {
		logger.info( "User data are being retrieved for AWS / Openstack..." );

		// Copy the user data
		String userData = Utils.readUrlContentQuietly( "http://169.254.169.254/latest/user-data", logger );
		String ip = Utils.readUrlContentQuietly( "http://169.254.169.254/latest/meta-data/public-ipv4", logger );

		AgentProperties result = null;
		try {
			// Parse the user data
			result = AgentProperties.readIaasProperties( userData, logger );

			// Verify the IP
			if( ! AgentUtils.isValidIP( ip )) {
				// Failed retrieving public IP: try private one instead
				ip = Utils.readUrlContentQuietly( "http://169.254.169.254/latest/meta-data/local-ipv4", logger );
			}

			if( AgentUtils.isValidIP( ip ))
				result.setIpAddress( ip );
			else
				logger.severe( "No IP address could be retrieved (either public-ipv4 or local-ipv4)." );

		} catch( IOException e ) {
			logger.severe( "The network properties could not be read. " + e.getMessage());
			Utils.logException( logger, e );
		}

		return result;
	}


	/**
	 * Configures the agent from Azure.
	 * @param logger a logger
	 * @return the agent's data, or null if they could not be parsed
	 */
	public AgentProperties findParametersForAzure( Logger logger ) {
		logger.info( "User data are being retrieved for Microsoft Azure..." );

		String userData = "";
		try {
			// Get the user data from /var/lib/waagent/ovf-env.xml and decode it
			String userDataEncoded = getValueOfTagInXMLFile( "/var/lib/waagent/ovf-env.xml", "CustomData" );
			userData = new String( Base64.decodeBase64( userDataEncoded.getBytes( StandardCharsets.UTF_8 )), "UTF-8" );

		} catch( IOException | ParserConfigurationException | SAXException e ) {
			logger.severe( "The agent properties could not be read. " + e.getMessage());
			Utils.logException( logger, e );
		}

		// Get the public IP Address from /var/lib/waagent/SharedConfig.xml
		AgentProperties result = null;
		String publicIPAddress;
		try {
			result = AgentProperties.readIaasProperties( userData, logger );
			publicIPAddress = getSpecificAttributeOfTagInXMLFile( "/var/lib/waagent/SharedConfig.xml", "Endpoint", "loadBalancedPublicAddress" );
			result.setIpAddress( publicIPAddress );

		} catch( ParserConfigurationException | SAXException e ) {
			logger.severe( "The agent could not retrieve a public IP address. " + e.getMessage());
			Utils.logException( logger, e );

		} catch( IOException e ) {
			logger.severe( "The agent could not retrieve its configuration. " + e.getMessage());
			Utils.logException( logger, e );
		}

		return result;
	}


	/**
	 * Configures the agent from VMWare.
	 * @param logger a logger
	 * @return the agent's data, or null if they could not be parsed
	 */
	public AgentProperties findParametersForVmware( Logger logger ) {
		logger.info( "User data are being retrieved for VMWare..." );

		AgentProperties result = null;
		File propertiesFile = new File("/tmp/roboconf.properties");
		try {
			int retries = 30;
			while((! propertiesFile.exists() || ! propertiesFile.canRead()) && retries-- > 0) {
				logger.fine("Agent tries to read properties file " + propertiesFile + ": trial #" + (30-retries));
				try {
					Thread.sleep( 2000 );

				} catch( InterruptedException e ) {
					throw new IOException( "Cannot read properties file: " + e );
				}
			}

			result = AgentProperties.readIaasProperties(Utils.readPropertiesFile(propertiesFile));

			/*
			 * HACK for specific IaaS configurations (using properties file in a VMWare-like manner)
			 * Try to pick IP address... in the case we are on OpenStack or any IaaS with amazon-compatible API
			 * Some configurations (with floating IPs) do not provide network interfaces exposing public IPs !
			 */
			InputStream in = null;
			try {
				URL userDataUrl = new URL( "http://169.254.169.254/latest/meta-data/public-ipv4" );
				in = userDataUrl.openStream();
				ByteArrayOutputStream os = new ByteArrayOutputStream();

				Utils.copyStreamSafely( in, os );
				String ip = os.toString( "UTF-8" );
				if(! AgentUtils.isValidIP( ip )) {
					// Failed retrieving public IP: try private one instead
					Utils.closeQuietly( in );
					userDataUrl = new URL( "http://169.254.169.254/latest/meta-data/local-ipv4" );
					in = userDataUrl.openStream();
					os = new ByteArrayOutputStream();

					Utils.copyStreamSafely( in, os );
					ip = os.toString( "UTF-8" );
				}

				if(AgentUtils.isValidIP( ip ))
					result.setIpAddress( os.toString( "UTF-8" ));

			} catch( IOException e ) {
				Utils.logException( logger, e );

			} finally {
				Utils.closeQuietly( in );
			}
			/* HACK ends here (see comment above). Removing it is harmless on classical VMWare configurations. */

		} catch( IOException e ) {
			logger.fine( "Agent failed to read properties file " + propertiesFile );
			result = null;
		}

		return result;
	}


	/**
	 * Retrieve the agent's configuration from an URL.
	 * @param logger a logger
	 * @return the agent's data, or null if they could not be parsed
	 */
	public AgentProperties findParametersFromUrl( String url, Logger logger ) {
		logger.info( "User data are being retrieved from URL: " + url );

		AgentProperties result = null;
		try {
			URI uri = UriUtils.urlToUri( url );
			Properties props = new Properties();
			InputStream in = null;
			try {
				in = uri.toURL().openStream();
				props.load( in );

			} finally {
				Utils.closeQuietly( in );
			}

			result = AgentProperties.readIaasProperties( props );

		} catch( Exception e ) {
			logger.fine( "Agent parameters could not be read from " + url );
			result = null;
		}

		return result;
	}


	/**
	 * Reconfigures the messaging.
	 * @param etcDir the KARAF_ETC directory
	 * @param msgData the messaging configuration parameters
	 */
	public void reconfigureMessaging( String etcDir, Map<String,String> msgData )
	throws IOException {

		String messagingType = msgData.get( MessagingConstants.MESSAGING_TYPE_PROPERTY );
		Logger.getLogger( getClass().getName()).fine( "Messaging type for reconfiguration: " + messagingType );
		if( ! Utils.isEmptyOrWhitespaces( etcDir )
				&& ! Utils.isEmptyOrWhitespaces( messagingType )) {

			// Write the messaging configuration
			File f = new File( etcDir, "net.roboconf.messaging." + messagingType + ".cfg" );
			Logger logger = Logger.getLogger( getClass().getName());

			Properties props = Utils.readPropertiesFileQuietly( f, logger );
			props.putAll( msgData );
			props.remove( MessagingConstants.MESSAGING_TYPE_PROPERTY );

			Utils.writePropertiesFile( props, f );

			// Set the messaging type
			f = new File( etcDir, Constants.KARAF_CFG_FILE_AGENT );

			props = Utils.readPropertiesFileQuietly( f, Logger.getLogger( getClass().getName()));
			if( messagingType != null ) {
				props.put( Constants.MESSAGING_TYPE, messagingType );
				Utils.writePropertiesFile( props, f );
			}
		}
	}


	// FIXME: there must be a shorter way with XPath...
	private static String getValueOfTagInXMLFile( String filePath, String tagName )
	throws ParserConfigurationException, SAXException, IOException {

		File fXmlFile = new File(filePath);
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(fXmlFile);

		// Optional, but recommended
		// Read this: http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
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
			if (attrName.equals(theAttribute.getNodeName()))
				attrValue = theAttribute.getTextContent().split(":")[0];
		}

		return attrValue;
	}
}
