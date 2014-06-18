/**
 * Copyright 2013-2014 Linagora, Université Joseph Fourier
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

package net.roboconf.iaas.azure;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.util.Map;
import java.util.logging.Logger;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import net.roboconf.core.agents.DataHelpers;
import net.roboconf.core.utils.Utils;
import net.roboconf.iaas.api.IaasException;
import net.roboconf.iaas.api.IaasInterface;
import net.roboconf.iaas.azure.internal.AzureConstants;
import net.roboconf.iaas.azure.internal.AzureProperties;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * @author Linh-Manh Pham - LIG
 */
public class IaasAzure implements IaasInterface {

	private Logger logger;
	private AzureProperties azureProperties;


	/**
	 * Constructor.
	 */
	public IaasAzure() {
		this.logger = Logger.getLogger( getClass().getName());
	}


	/**
	 * @param logger the logger to set
	 */
	public void setLogger( Logger logger ) {
		this.logger = logger;
	}


	private KeyStore getKeyStore(String keyStoreName, String password) throws IOException {
	    KeyStore ks = null;
	    FileInputStream fis = null;
	    try {
	        ks = KeyStore.getInstance("JKS");
	        char[] passwordArray = password.toCharArray();
	        fis = new java.io.FileInputStream(keyStoreName);
	        ks.load(fis, passwordArray);

	    } catch (Exception e) {
	    	this.logger.severe( e.getMessage() );
	    }
	    finally {
	        if (fis != null) {
	        	Utils.closeQuietly( fis );
	        }
	    }
	    return ks;
	}


	private SSLSocketFactory getSSLSocketFactory( String keyStoreName, String password )
	throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException, KeyManagementException, IOException {

	    KeyStore ks = this.getKeyStore(keyStoreName, password);
	    KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
	    keyManagerFactory.init(ks, password.toCharArray());

	      SSLContext context = SSLContext.getInstance("TLS");
	      context.init(keyManagerFactory.getKeyManagers(), null, new SecureRandom());

	      return context.getSocketFactory();
	}


	private String getStringFromInputStream( InputStream in ) {

		String result = "";
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		try {
			Utils.copyStream( in, os );
			result = os.toString( "UTF-8" );

		} catch( IOException e ) {
			this.logger.severe( e.getMessage());
			this.logger.finest( Utils.writeException( e ));
		}

	     return result;
	}


	private static boolean getExistResutlFromXML(String xmlStr, String nameOfNode)
	throws ParserConfigurationException, UnsupportedEncodingException, SAXException, IOException {

		DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
		DocumentBuilder b;
		b = f.newDocumentBuilder();
		Document doc;
		doc = b.parse(new ByteArrayInputStream(xmlStr.getBytes("UTF-8")));
		NodeList nodes = doc.getElementsByTagName(nameOfNode);
		String result = "false";
		for (int i = 0; i < nodes.getLength(); i++) {
			 Element node = (Element) nodes.item(i);
			 result = node.getTextContent();
		}

		return Boolean.parseBoolean( result );
	}


	private String processGetRequest(URL url, String keyStore, String keyStorePassword)
	throws UnrecoverableKeyException, KeyManagementException, KeyStoreException, NoSuchAlgorithmException, IOException {

        SSLSocketFactory sslFactory = this.getSSLSocketFactory(keyStore, keyStorePassword);
        HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
        con.setSSLSocketFactory(sslFactory);
        con.setRequestMethod("GET");
        con.addRequestProperty("x-ms-version", "2014-04-01");
        InputStream responseStream = (InputStream) con.getContent();

        try {
        	String response = getStringFromInputStream(responseStream);
        	return response;

        } finally {
        	Utils.closeQuietly( responseStream );
        }
    }


	private int processPostRequest(URL url, byte[] data, String contentType, String keyStore, String keyStorePassword)
	throws UnrecoverableKeyException, KeyManagementException, KeyStoreException, NoSuchAlgorithmException, IOException {

	    SSLSocketFactory sslFactory = this.getSSLSocketFactory(keyStore, keyStorePassword);
	    HttpsURLConnection con = null;
	    con = (HttpsURLConnection) url.openConnection();
	    con.setSSLSocketFactory(sslFactory);
	    con.setDoOutput(true);
	    con.setRequestMethod("POST");
	    con.addRequestProperty("x-ms-version", "2014-04-01");
	    con.setRequestProperty("Content-Length", String.valueOf(data.length));
	    con.setRequestProperty("Content-Type", contentType);

	    DataOutputStream  requestStream = new DataOutputStream (con.getOutputStream());
	    requestStream.write(data);
	    requestStream.flush();
	    requestStream.close();
	    return con.getResponseCode();
	}


	private int processDeleteRequest(URL url, String keyStore, String keyStorePassword)
	throws UnrecoverableKeyException, KeyManagementException, KeyStoreException, NoSuchAlgorithmException, IOException {

        SSLSocketFactory sslFactory = this.getSSLSocketFactory(keyStore, keyStorePassword);
        HttpsURLConnection con = null;
        con = (HttpsURLConnection) url.openConnection();
        con.setSSLSocketFactory(sslFactory);
        con.setRequestMethod("DELETE");
        con.addRequestProperty("x-ms-version", "2014-04-01");
        return con.getResponseCode();
    }

	private byte[] convertFileToByte(String xmlFilePath) {

		ByteArrayOutputStream os = new ByteArrayOutputStream();
		try {
			Utils.copyStream ( new File(xmlFilePath), os );
		} catch (IOException e) {
			this.logger.severe( e.getMessage());
		}

		return os.toByteArray();
    }


	private void replaceValueOfTagInXMLFile(String filePath, String tagName, String replacingValue)
	throws ParserConfigurationException, SAXException, IOException {

		File fXmlFile = new File(filePath);
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(fXmlFile);

		//optional, but recommended
		//read this - http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
		doc.getDocumentElement().normalize();

		NodeList nList = doc.getElementsByTagName(tagName);
		Node nNode = nList.item(0);
		nNode.setTextContent(replacingValue);

		// write the modified content into xml file
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer;
		try {
			transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(new File(filePath));
			transformer.transform(source, result);

		} catch (TransformerException e) {
			this.logger.severe( e.getMessage() );
		}
	}

	/*
	 * (non-Javadoc)
	 * @see net.roboconf.iaas.api.IaasInterface
	 * #setIaasProperties(java.util.Properties)
	 */
	@Override
	public void setIaasProperties(Map<String, String> iaasProperties) throws IaasException {

		// Quick check
		String[] properties = {
				AzureConstants.AZURE_SUBSCRIPTION_ID,
				AzureConstants.AZURE_KEY_STORE_FILE,
				AzureConstants.AZURE_KEY_STORE_PASSWORD,
				AzureConstants.AZURE_CREATE_CLOUD_SERVICE_TEMPLATE,
				AzureConstants.AZURE_CREATE_DEPLOYMENT_TEMPLATE,
				AzureConstants.AZURE_LOCATION,
				AzureConstants.AZURE_VM_SIZE,
				AzureConstants.AZURE_VM_TEMPLATE
		};

		for( String property : properties ) {
			if( StringUtils.isBlank( iaasProperties.get( property )))
				throw new IaasException( "The value for " + property + " cannot be null or empty." );
		}

		// Create a bean
		this.azureProperties = new AzureProperties();

		String s = iaasProperties.get( AzureConstants.AZURE_SUBSCRIPTION_ID );
		if( s != null )
			this.azureProperties.setSubscriptionId( s.trim());

		s = iaasProperties.get( AzureConstants.AZURE_KEY_STORE_FILE );
		if( s != null )
			this.azureProperties.setKeyStoreFile( s.trim());

		s = iaasProperties.get( AzureConstants.AZURE_KEY_STORE_PASSWORD );
		if( s != null )
			this.azureProperties.setKeyStoreFile( s.trim());

		s = iaasProperties.get( AzureConstants.AZURE_CREATE_CLOUD_SERVICE_TEMPLATE );
		if( s != null )
			this.azureProperties.setKeyStoreFile( s.trim());

		s = iaasProperties.get( AzureConstants.AZURE_CREATE_DEPLOYMENT_TEMPLATE );
		if( s != null )
			this.azureProperties.setKeyStoreFile( s.trim());

		s = iaasProperties.get( AzureConstants.AZURE_LOCATION );
		if( s != null )
			this.azureProperties.setKeyStoreFile( s.trim());

		s = iaasProperties.get( AzureConstants.AZURE_VM_SIZE );
		if( s != null )
			this.azureProperties.setKeyStoreFile( s.trim());

		s = iaasProperties.get( AzureConstants.AZURE_VM_TEMPLATE );
		if( s != null )
			this.azureProperties.setKeyStoreFile( s.trim());
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.iaas.api.IaasInterface
	 * #createVM(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public String createVM(
			String messagingIp,
			String messagingUsername,
			String messagingPassword,
			String rootInstanceName,
			String applicationName )
	throws IaasException {

		String instanceId = null;
		try {
			// The following part enables to transmit data to the VM.
			// When the VM is up, it will be able to read this data.
			// TODO: Azure does not allow a VM name with spaces whereas graph configuration of Roboconf supports it. It conflicts.
			// channelName = channelName.replaceAll("\\s+","-").toLowerCase();
			String userData = DataHelpers.writeIaasDataAsString( messagingIp, messagingUsername, messagingPassword, applicationName, rootInstanceName );
			String encodedUserData = new String( Base64.encodeBase64( userData.getBytes( "UTF-8" )), "UTF-8" );

			replaceValueOfTagInXMLFile(this.azureProperties.getCreateCloudServiceTemplate(), "ServiceName", rootInstanceName );
			replaceValueOfTagInXMLFile(this.azureProperties.getCreateCloudServiceTemplate(), "Location", this.azureProperties.getLocation() );
			replaceValueOfTagInXMLFile(this.azureProperties.getCreateDeploymentTemplate(), "CustomData", encodedUserData );
			replaceValueOfTagInXMLFile(this.azureProperties.getCreateDeploymentTemplate(), "Name", rootInstanceName );
			replaceValueOfTagInXMLFile(this.azureProperties.getCreateDeploymentTemplate(), "HostName", rootInstanceName );
			replaceValueOfTagInXMLFile(this.azureProperties.getCreateDeploymentTemplate(), "RoleName", rootInstanceName );
			replaceValueOfTagInXMLFile(this.azureProperties.getCreateDeploymentTemplate(), "RoleSize", this.azureProperties.getVMSize() );
			replaceValueOfTagInXMLFile(this.azureProperties.getCreateDeploymentTemplate(), "SourceImageName", this.azureProperties.getVMTemplate() );

			// Let send the request to Azure API to create a Cloud Service and a Deployment (a PersistentVMRole)
			String baseURL = String.format("https://management.core.windows.net/%s/services", this.azureProperties.getSubscriptionId());
			String requestHeaderContentType = "application/xml";
			byte[] requestBodyCreateCloudService = convertFileToByte(this.azureProperties.getCreateCloudServiceTemplate());
			byte[] requestBodyCreateDeployment = convertFileToByte(this.azureProperties.getCreateDeploymentTemplate());
			String checkCloudServiceURL = baseURL+"/hostedservices/operations/isavailable/"+rootInstanceName;
			String createCloudServiceURL = baseURL+"/hostedservices";
			String createDeploymentURL = baseURL+"/hostedservices/"+rootInstanceName+"/deployments";

			// check if Cloud Service exist
			String responseCheckCloudService = processGetRequest(new URL(checkCloudServiceURL), this.azureProperties.getKeyStoreFile(), this.azureProperties.getKeyStorePassword());
			boolean checkResult = getExistResutlFromXML(responseCheckCloudService, "Result");	// true means the name still available
			this.logger.info( "Response Result: Cloud Service Name is still available: " + checkResult);

			// create Cloud Service, Deployment & Add a Role (Linux VM), maybe add a second Role (another Linux VM)
			int rescodeCreateCloudService = -1;
			if (checkResult) rescodeCreateCloudService = processPostRequest(
					new URL(createCloudServiceURL),
					requestBodyCreateCloudService,
					requestHeaderContentType,
					this.azureProperties.getKeyStoreFile(),
					this.azureProperties.getKeyStorePassword());	// rescode shoud be 201

			this.logger.info( "Create Cloud Service: Response Code: " + rescodeCreateCloudService);
			this.logger.info( "Creating Azure VM in progress: " + rootInstanceName);
			if (rescodeCreateCloudService == 201) {
				int rescodeCreateDeployment = processPostRequest(
						new URL(createDeploymentURL),
						requestBodyCreateDeployment,
						requestHeaderContentType,
						this.azureProperties.getKeyStoreFile(),
						this.azureProperties.getKeyStorePassword());	// rescode shoud be 202

				this.logger.info( "Create VM: Response Code: " + rescodeCreateDeployment);
			}

			instanceId = rootInstanceName;	// instanceID in this context should be rootInstanceName

		} catch( UnsupportedEncodingException e ) {
			throw new IaasException( e );

		} catch( ParserConfigurationException e ) {
			throw new IaasException( e );

		} catch( SAXException e ) {
			throw new IaasException( e );

		} catch( IOException e ) {
			throw new IaasException( e );

		} catch( UnrecoverableKeyException e ) {
			throw new IaasException( e );

		} catch( KeyManagementException e ) {
			throw new IaasException( e );

		} catch( KeyStoreException e ) {
			throw new IaasException( e );

		} catch( NoSuchAlgorithmException e ) {
			throw new IaasException( e );
		}

		return instanceId;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.iaas.api.IaasInterface
	 * #terminateVM(java.lang.String)
	 * instanceID is CloudServiceName
	 */
	@Override
	public void terminateVM( String instanceId ) throws IaasException {
		try {
			String baseURL = String.format("https://management.core.windows.net/%s/services", this.azureProperties.getSubscriptionId());
			String deleteCloudServiceURL = baseURL+"/hostedservices/"+instanceId+"?comp=media";

			// Delete Cloud Service, and also delete all the related things
			int rescodeDeleteCloudService = processDeleteRequest(
					new URL(deleteCloudServiceURL),
					this.azureProperties.getKeyStoreFile(),
					this.azureProperties.getKeyStorePassword());		// rescode shoud be 202

			this.logger.info("Response Code: Delete VM: " + rescodeDeleteCloudService);

		} catch( UnrecoverableKeyException e ) {
			throw new IaasException( e );

		} catch( KeyManagementException e ) {
			throw new IaasException( e );

		} catch( KeyStoreException e ) {
			throw new IaasException( e );

		} catch( NoSuchAlgorithmException e ) {
			throw new IaasException( e );

		} catch( MalformedURLException e ) {
			throw new IaasException( e );

		} catch( IOException e ) {
			throw new IaasException( e );
		}
	}
}
