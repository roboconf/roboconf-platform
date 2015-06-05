/**
 * Copyright 2013-2015 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.target.ec2.internal;

import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.Map;

import net.roboconf.core.agents.DataHelpers;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.utils.Utils;
import net.roboconf.target.api.AbstractThreadedTargetHandler;
import net.roboconf.target.api.TargetException;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;

/**
 * @author Noël - LIG
 */
public class Ec2IaasHandler extends AbstractThreadedTargetHandler {

	public static final String TARGET_ID = "iaas-ec2";


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.target.api.TargetHandler#getTargetId()
	 */
	@Override
	public String getTargetId() {
		return TARGET_ID;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.target.api.TargetHandler#createMachine(java.util.Map,
	 * java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public String createMachine(
			Map<String,String> targetProperties,
			Map<String,String> messagingConfiguration,
			String scopedInstancePath,
			String applicationName )
	throws TargetException {

		this.logger.fine( "Creating a new machine on AWS." );

		// For IaaS, we only expect root instance names to be passed
		if( InstanceHelpers.countInstances( scopedInstancePath ) > 1 )
			throw new TargetException( "Only root instances can be passed in arguments." );

		String rootInstanceName = InstanceHelpers.findRootInstancePath( scopedInstancePath );

		// Deal with the creation
		String instanceId;
		try {
			AmazonEC2 ec2 = createEc2Client( targetProperties );
			String userData = DataHelpers.writeUserDataAsString(
					messagingConfiguration,
					applicationName, rootInstanceName );

			RunInstancesRequest runInstancesRequest = prepareEC2RequestNode( targetProperties, userData );
			RunInstancesResult runInstanceResult = ec2.runInstances( runInstancesRequest );
			instanceId = runInstanceResult.getReservation().getInstances().get( 0 ).getInstanceId();

		} catch( Exception e ) {
			this.logger.severe( "An error occurred while creating a new machine on EC2. " + e.getMessage());
			throw new TargetException( e );
		}

		return instanceId;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.target.api.AbstractThreadedTargetHandler#machineConfigurator(java.util.Map,
	 * java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public MachineConfigurator machineConfigurator(
			Map<String,String> targetProperties,
			Map<String,String> messagingConfiguration,
			String machineId,
			String scopedInstancePath,
			String applicationName ) {

		String rootInstanceName = InstanceHelpers.findRootInstancePath( scopedInstancePath );
		String tagName = applicationName + "." + rootInstanceName;
		return new Ec2MachineConfigurator( targetProperties, machineId, tagName );
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.target.api.TargetHandler
	 * #isMachineRunning(java.util.Map, java.lang.String)
	 */
	@Override
	public boolean isMachineRunning( Map<String,String> targetProperties, String machineId )
	throws TargetException {

		boolean result = false;
		try {
			AmazonEC2 ec2 = createEc2Client( targetProperties );
			DescribeInstancesRequest dis = new DescribeInstancesRequest();
			dis.setInstanceIds(Collections.singletonList(machineId));

			DescribeInstancesResult disresult = ec2.describeInstances( dis );
			result = ! disresult.getReservations().isEmpty();

		} catch( AmazonServiceException e ) {
			// nothing, the instance does not exist

		} catch( AmazonClientException e ) {
			this.logger.severe( "An error occurred while creating a machine on Amazon EC2. " + e.getMessage());
			throw new TargetException( e );
		}

		return result;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.target.api.TargetHandler
	 * #terminateMachine(java.util.Map, java.lang.String)
	 */
	@Override
	public void terminateMachine( Map<String, String> targetProperties, String instanceId ) throws TargetException {

		this.logger.fine( "Terminating machine '" + instanceId + "'." );
		try {
			AmazonEC2 ec2 = createEc2Client( targetProperties );
			TerminateInstancesRequest terminateInstancesRequest = new TerminateInstancesRequest();
			terminateInstancesRequest.withInstanceIds( instanceId );
			ec2.terminateInstances( terminateInstancesRequest );

		} catch( Exception e ) {
			this.logger.severe( "An error occurred while terminating a machine on Amazon EC2. " + e.getMessage());
			throw new TargetException( e );
		}
	}


	/**
	 * Parses the properties and saves them in a Java bean.
	 * @param targetProperties the IaaS properties
	 * @throws TargetException
	 */
	static void parseProperties( Map<String, String> targetProperties ) throws TargetException {

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
			if( StringUtils.isBlank( targetProperties.get( property )))
				throw new TargetException( "The value for " + property + " cannot be null or empty." );
		}
	}


	/**
	 * Creates a client for EC2.
	 * @param targetProperties the target properties (not null)
	 * @return a non-null client
	 * @throws TargetException if properties are invalid
	 */
	public static AmazonEC2 createEc2Client( Map<String,String> targetProperties ) throws TargetException {

		parseProperties( targetProperties );

		// Configure the IaaS client
		AWSCredentials credentials = new BasicAWSCredentials(
				targetProperties.get(Ec2Constants.EC2_ACCESS_KEY),
				targetProperties.get(Ec2Constants.EC2_SECRET_KEY));

		AmazonEC2 ec2 = new AmazonEC2Client( credentials );
		ec2.setEndpoint( targetProperties.get(Ec2Constants.EC2_ENDPOINT ));

		return ec2;
	}


	/**
	 * Prepares the request.
	 * @param targetProperties the target properties
	 * @param userData the user data to pass
	 * @return a request
	 * @throws UnsupportedEncodingException
	 */
	private RunInstancesRequest prepareEC2RequestNode( Map<String,String> targetProperties, String userData )
	throws UnsupportedEncodingException {

		RunInstancesRequest runInstancesRequest = new RunInstancesRequest();
		String flavor = targetProperties.get(Ec2Constants.VM_INSTANCE_TYPE);
		if( Utils.isEmptyOrWhitespaces( flavor ))
			flavor = "t1.micro";

		runInstancesRequest.setInstanceType( flavor );
		runInstancesRequest.setImageId( targetProperties.get( Ec2Constants.AMI_VM_NODE ));

		runInstancesRequest.setMinCount( 1 );
		runInstancesRequest.setMaxCount( 1 );
		runInstancesRequest.setKeyName( targetProperties.get(Ec2Constants.SSH_KEY_NAME));

		String secGroup = targetProperties.get(Ec2Constants.SECURITY_GROUP_NAME);
		if( Utils.isEmptyOrWhitespaces(secGroup))
			secGroup = "default";

		runInstancesRequest.setSecurityGroups(Collections.singletonList(secGroup));

		// The following part enables to transmit data to the VM.
		// When the VM is up, it will be able to read this data.
		String encodedUserData = new String( Base64.encodeBase64( userData.getBytes( "UTF-8" )), "UTF-8" );
		runInstancesRequest.setUserData( encodedUserData );

		return runInstancesRequest;
	}
}
