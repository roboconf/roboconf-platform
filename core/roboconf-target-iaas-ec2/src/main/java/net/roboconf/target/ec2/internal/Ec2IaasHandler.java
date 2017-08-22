/**
 * Copyright 2013-2017 Linagora, Université Joseph Fourier, Floralis
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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import com.amazonaws.services.ec2.model.Placement;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;

import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.userdata.UserDataHelpers;
import net.roboconf.core.utils.Utils;
import net.roboconf.target.api.AbstractThreadedTargetHandler;
import net.roboconf.target.api.TargetException;
import net.roboconf.target.api.TargetHandlerParameters;

/**
 * @author Noël - LIG
 */
public class Ec2IaasHandler extends AbstractThreadedTargetHandler {

	public static final String TARGET_ID = "iaas-ec2";

	static final String TPL_VOLUME_NAME = "%NAME%";
	static final String TPL_VOLUME_APP = "%APP%";

	static final String USE_BLOCK_STORAGE = "ec2.use-block-storage";
	static final String VOLUME_MOUNT_POINT_PREFIX = "ec2.ebs-mount-point.";
	static final String VOLUME_NAME_PREFIX = "ec2.ebs-snapshot-id.";
	static final String VOLUME_SIZE_GB_PREFIX = "ec2.ebs-size.";
	static final String VOLUME_DELETE_OT_PREFIX = "ec2.ebs-delete-on-termination.";
	static final String VOLUME_TYPE_PREFIX = "ec2.ebs-type.";

	static final Map<String,String> DEFAULTS = new HashMap<> ();
	static {
		DEFAULTS.put( VOLUME_MOUNT_POINT_PREFIX, "/dev/sdf" );
		DEFAULTS.put( VOLUME_NAME_PREFIX, "roboconf-" + TPL_VOLUME_APP + "-" + TPL_VOLUME_NAME );
		DEFAULTS.put( VOLUME_SIZE_GB_PREFIX, "2" );
		DEFAULTS.put( VOLUME_DELETE_OT_PREFIX, "false" );
	}

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
	 * @see net.roboconf.target.api.TargetHandler
	 * #createMachine(net.roboconf.target.api.TargetHandlerParameters)
	 */
	@Override
	public String createMachine( TargetHandlerParameters parameters ) throws TargetException {

		this.logger.fine( "Creating a new machine on AWS." );

		// For IaaS, we only expect root instance names to be passed
		if( InstanceHelpers.countInstances( parameters.getScopedInstancePath()) > 1 )
			throw new TargetException( "Only root instances can be passed in arguments." );

		String rootInstanceName = InstanceHelpers.findRootInstancePath( parameters.getScopedInstancePath());

		// Deal with the creation
		String instanceId;
		try {
			AmazonEC2 ec2 = createEc2Client( parameters.getTargetProperties());
			String userData = UserDataHelpers.writeUserDataAsString(
					parameters.getMessagingProperties(),
					parameters.getDomain(),
					parameters.getApplicationName(),
					rootInstanceName );

			RunInstancesRequest runInstancesRequest = prepareEC2RequestNode( parameters.getTargetProperties(), userData );
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
	 * @see net.roboconf.target.api.AbstractThreadedTargetHandler#machineConfigurator(
	 * net.roboconf.target.api.TargetHandlerParameters, java.lang.String)
	 */
	@Override
	public MachineConfigurator machineConfigurator( TargetHandlerParameters parameters, String machineId ) {

		String rootInstanceName = InstanceHelpers.findRootInstancePath( parameters.getScopedInstancePath());
		return new Ec2MachineConfigurator(
				parameters.getTargetProperties(),
				machineId,
				parameters.getApplicationName(),
				rootInstanceName,
				parameters.getScopedInstance());
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.target.api.TargetHandler
	 * #isMachineRunning(net.roboconf.target.api.TargetHandlerParameters, java.lang.String)
	 */
	@Override
	public boolean isMachineRunning( TargetHandlerParameters parameters, String machineId )
	throws TargetException {

		boolean result = false;
		try {
			AmazonEC2 ec2 = createEc2Client( parameters.getTargetProperties());
			DescribeInstancesRequest dis = new DescribeInstancesRequest();
			dis.setInstanceIds(Collections.singletonList(machineId));

			DescribeInstancesResult disresult = ec2.describeInstances( dis );
			result = ! disresult.getReservations().isEmpty();

		} catch( AmazonServiceException e ) {
			// nothing, the instance does not exist

		} catch( AmazonClientException e ) {
			this.logger.severe( "An error occurred while checking whether a machine is running on Amazon EC2. " + e.getMessage());
			throw new TargetException( e );
		}

		return result;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.target.api.TargetHandler
	 * #terminateMachine(net.roboconf.target.api.TargetHandlerParameters, java.lang.String)
	 */
	@Override
	public void terminateMachine( TargetHandlerParameters parameters, String machineId ) throws TargetException {

		this.logger.fine( "Terminating machine '" + machineId + "'." );
		cancelMachineConfigurator( machineId );
		try {
			AmazonEC2 ec2 = createEc2Client( parameters.getTargetProperties());
			TerminateInstancesRequest terminateInstancesRequest = new TerminateInstancesRequest();
			terminateInstancesRequest.withInstanceIds( machineId );
			ec2.terminateInstances( terminateInstancesRequest );

		} catch( Exception e ) {
			this.logger.severe( "An error occurred while terminating a machine on Amazon EC2. " + e.getMessage());
			throw new TargetException( e );
		}
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.target.api.TargetHandler
	 * #retrievePublicIpAddress(net.roboconf.target.api.TargetHandlerParameters, java.lang.String)
	 */
	@Override
	public String retrievePublicIpAddress( TargetHandlerParameters parameters, String machineId )
	throws TargetException {

		String result = null;
		try {
			AmazonEC2 ec2 = createEc2Client( parameters.getTargetProperties());
			DescribeInstancesRequest dis = new DescribeInstancesRequest();
			dis.setInstanceIds(Collections.singletonList(machineId));

			DescribeInstancesResult disresult = ec2.describeInstances( dis );
			if( ! disresult.getReservations().isEmpty()) {
				// Only one instance should match this machine ID
				result = disresult.getReservations().get( 0 ).getInstances().get( 0 ).getPublicIpAddress();
			}

		} catch( AmazonServiceException e ) {
			// nothing, the instance does not exist

		} catch( Exception e ) {
			this.logger.severe( "An error occurred while retrieving a public IP address from Amazon EC2. " + e.getMessage());
			throw new TargetException( e );
		}

		return result;
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

		String availabilityZone = targetProperties.get(Ec2Constants.AVAILABILITY_ZONE);
		if(! Utils.isEmptyOrWhitespaces(availabilityZone))
			runInstancesRequest.setPlacement(new Placement(availabilityZone));

		// The following part enables to transmit data to the VM.
		// When the VM is up, it will be able to read this data.
		String encodedUserData = new String( Base64.encodeBase64( userData.getBytes( StandardCharsets.UTF_8 )), "UTF-8" );
		runInstancesRequest.setUserData( encodedUserData );

		return runInstancesRequest;
	}

	/**
	 * Finds the storage IDs (used as property suffixes).
	 * @param targetProperties
	 * @return a non-null list
	 */
	static List<String> findStorageIds( Map<String,String> targetProperties ) {

		List<String> result = new ArrayList<> ();
		String prop = targetProperties.get( USE_BLOCK_STORAGE );
		if( ! Utils.isEmptyOrWhitespaces( prop )) {
			for( String s : Utils.splitNicely( prop, "," )) {
				if( ! Utils.isEmptyOrWhitespaces( s ))
					result.add( s );
			}
		}

		return result;
	}

	/**
	 * Finds a storage property for a given storage ID.
	 * @param targetProperties
	 * @param storageId
	 * @param propertyPrefix one of the constants defined in this class
	 * @return the property's value, or the default value otherwise, if one exists
	 */
	static String findStorageProperty( Map<String,String> targetProperties, String storageId, String propertyPrefix ) {

		String property = propertyPrefix + storageId;
		String value = targetProperties.get( property );
		return Utils.isEmptyOrWhitespaces( value ) ? DEFAULTS.get( propertyPrefix ) : value.trim();
	}

	/**
	 * Updates a volume name by replacing template variables.
	 * @param nameTemplate (not null)
	 * @param appName (not null)
	 * @param instanceName (not null)
	 * @return a string representing the expanded template
	 */
	static String expandVolumeName(String nameTemplate, String appName, String instanceName) {

		if(! Utils.isEmptyOrWhitespaces(nameTemplate)) {
			String name = nameTemplate.replace(TPL_VOLUME_NAME, instanceName);
			name = name.replace(TPL_VOLUME_APP, appName);
			name = name.replaceAll("[\\W_-]", "-");
			return name;
		}

		return nameTemplate;
	}
}
