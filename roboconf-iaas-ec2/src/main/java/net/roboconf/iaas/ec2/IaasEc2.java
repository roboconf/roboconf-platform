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

package net.roboconf.iaas.ec2;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import net.roboconf.iaas.api.IaasInterface;
import net.roboconf.iaas.api.exceptions.CommunicationToIaasException;
import net.roboconf.iaas.api.exceptions.IaasException;
import net.roboconf.iaas.api.exceptions.InvalidIaasPropertiesException;
import net.roboconf.iaas.ec2.internal.Ec2Constants;
import net.roboconf.iaas.ec2.internal.Ec2Properties;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;

/**
 * @author Noël - LIG
 */
public class IaasEc2 implements IaasInterface {

	private Logger logger;
	private AmazonEC2 ec2;
	private Ec2Properties ec2Properties;


	/**
	 * Constructor.
	 */
	public IaasEc2() {
		this.logger = Logger.getLogger( getClass().getName());
	}


	/**
	 * @param logger the logger to set
	 */
	public void setLogger( Logger logger ) {
		this.logger = logger;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.iaas.api.IaasInterface
	 * #setIaasProperties(java.util.Properties)
	 */
	@Override
	public void setIaasProperties(Map<String, String> iaasProperties) throws InvalidIaasPropertiesException {

		// Check the properties
		parseProperties( iaasProperties );

		// Configure the IaaS client
		AWSCredentials credentials = new BasicAWSCredentials( this.ec2Properties.getAccessKey(), this.ec2Properties.getSecretKey());
		this.ec2 = new AmazonEC2Client( credentials );
		this.ec2.setEndpoint( this.ec2Properties.getEndpoint());
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.iaas.api.IaasInterface
	 * #createVM(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public String createVM(
			String ipMessagingServer,
			String channelName,
			String applicationName,
			String rootInstanceName )
	throws IaasException, CommunicationToIaasException {

		String instanceId = null;
		try {
			RunInstancesRequest runInstancesRequest = prepareEC2RequestNode(
					this.ec2Properties.getAmiVmNode(),
					ipMessagingServer,
					rootInstanceName,
					applicationName );

			RunInstancesResult runInstanceResult = this.ec2.runInstances( runInstancesRequest );
			instanceId = runInstanceResult.getReservation().getInstances().get( 0 ).getInstanceId();

			// Set name tag for instance (human-readable in AWS webapp)
			List<Tag> tags = new ArrayList<Tag>();
			Tag t = new Tag();
			t.setKey("Name");
			t.setValue(applicationName + "." + rootInstanceName);
			tags.add(t);
			CreateTagsRequest ctr = new CreateTagsRequest();
			ctr.setTags(tags);
			ctr.withResources(instanceId);
			this.ec2.createTags(ctr);

		} catch( AmazonServiceException e ) {
			this.logger.severe( "An error occurred on Amazon while instantiating a machine. " + e.getMessage());
			throw new IaasException( e );

		} catch( AmazonClientException e ) {
			this.logger.severe( "An error occurred while creating a machine on Amazon EC2. " + e.getMessage());
			throw new CommunicationToIaasException( e );

		} catch( UnsupportedEncodingException e ) {
			this.logger.severe( "An error occurred while contacting Amazon EC2. " + e.getMessage());
			throw new CommunicationToIaasException( e );
		}

		return instanceId;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.iaas.api.IaasInterface
	 * #terminateVM(java.lang.String)
	 */
	@Override
	public void terminateVM( String instanceId ) throws IaasException, CommunicationToIaasException {
		try {
			TerminateInstancesRequest terminateInstancesRequest = new TerminateInstancesRequest();
			terminateInstancesRequest.withInstanceIds( instanceId );
			this.ec2.terminateInstances( terminateInstancesRequest );

		} catch( AmazonServiceException e ) {
			this.logger.severe( "An error occurred on Amazon while terminating the machine. " + e.getMessage());
			throw new IaasException( e );

		} catch( AmazonClientException e ) {
			this.logger.severe( "An error occurred while terminating a machine on Amazon EC2. " + e.getMessage());
			throw new CommunicationToIaasException( e );
		}
	}


	/**
	 * Parses the properties and saves them in a Java bean.
	 * @param iaasProperties the IaaS properties
	 * @throws InvalidIaasPropertiesException
	 */
	private void parseProperties( Map<String, String> iaasProperties ) throws InvalidIaasPropertiesException {

		// Quick check
		String[] properties = {
			Ec2Constants.EC2_ENDPOINT,
			Ec2Constants.EC2_ACCESS_KEY,
			Ec2Constants.EC2_SECRET_KEY,
			Ec2Constants.AMI_VM_NODE,
			Ec2Constants.VM_INSTANCE_TYPE,
			Ec2Constants.SSH_KEY_NAME,
			Ec2Constants.SECURITY_GROUP_NAME
		};

		for( String property : properties ) {
			if( StringUtils.isBlank( iaasProperties.get( property )))
				throw new InvalidIaasPropertiesException( "The value for " + property + " cannot be null or empty." );
		}

		// Create a bean
		this.ec2Properties = new Ec2Properties();
		this.ec2Properties.setEndpoint( iaasProperties.get( Ec2Constants.EC2_ENDPOINT ).trim());
		this.ec2Properties.setAccessKey( iaasProperties.get( Ec2Constants.EC2_ACCESS_KEY ).trim());
		this.ec2Properties.setSecretKey( iaasProperties.get( Ec2Constants.EC2_SECRET_KEY ).trim());
		this.ec2Properties.setAmiVmNode( iaasProperties.get( Ec2Constants.AMI_VM_NODE ).trim());
		this.ec2Properties.setVmInstanceType( iaasProperties.get( Ec2Constants.VM_INSTANCE_TYPE ).trim());
		this.ec2Properties.setSshKeyName( iaasProperties.get( Ec2Constants.SSH_KEY_NAME ).trim());
		this.ec2Properties.setSecurityGroupName( iaasProperties.get( Ec2Constants.SECURITY_GROUP_NAME ).trim());
	}


	/**
	 * Prepares the request.
	 * @param machineImageId
	 * @param ipMessagingServer
	 * @param channelName
	 * @param applicationName
	 * @return a request
	 * @throws UnsupportedEncodingException
	 */
	private RunInstancesRequest prepareEC2RequestNode( String machineImageId, String ipMessagingServer, String channelName, String applicationName ) throws UnsupportedEncodingException {

		RunInstancesRequest runInstancesRequest = new RunInstancesRequest();
		runInstancesRequest.setInstanceType( this.ec2Properties.getVmInstanceType());
		if( StringUtils.isBlank( machineImageId ))
			runInstancesRequest.setImageId( this.ec2Properties.getAmiVmNode());
		else
			runInstancesRequest.setImageId( machineImageId );

		// FIXME (VZ): why this kernel ID?
		runInstancesRequest.setKernelId( "aki-62695816" );
		runInstancesRequest.setMinCount( 1 );
		runInstancesRequest.setMaxCount( 1 );
		runInstancesRequest.setKeyName( this.ec2Properties.getSshKeyName());
		runInstancesRequest.setSecurityGroups( Arrays.asList( this.ec2Properties.getSecurityGroupName()));

		// The following part enables to transmit data to the VM.
		// When the VM is up, it will be able to read this data.
		StringBuilder data = new StringBuilder();
		data.append( "ipMessagingServer=" + ipMessagingServer + "\n" );
		data.append( "applicationName=" + applicationName + "\n" );
		data.append( "channelName=" + channelName + "\n" );

		String dataToPass = data.toString();
		String userData = new String( Base64.encodeBase64( dataToPass.getBytes( "UTF-8" )));
		runInstancesRequest.setUserData( userData );

		return runInstancesRequest;
	}
}
