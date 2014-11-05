/**
 * Copyright 2013-2014 Linagora, Université Joseph Fourier, Floralis
 *
 * The present code is developed in the scope of their joint LINAGORA -
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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import net.roboconf.core.agents.DataHelpers;
import net.roboconf.target.api.TargetException;
import net.roboconf.target.api.TargetHandler;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.AttachVolumeRequest;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.CreateVolumeRequest;
import com.amazonaws.services.ec2.model.CreateVolumeResult;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DescribeVolumesRequest;
import com.amazonaws.services.ec2.model.DescribeVolumesResult;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;

/**
 * @author Noël - LIG
 */
public class Ec2IaasHandler implements TargetHandler {

	public static final String TARGET_ID = "iaas-ec2";

	private final Logger logger;
	private AmazonEC2 ec2;
	private Map<String, String> targetProperties;


	/**
	 * Constructor.
	 */
	public Ec2IaasHandler() {
		this.logger = Logger.getLogger( getClass().getName());
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
	 * @see net.roboconf.target.api.TargetHandler#setTargetProperties(java.util.Map)
	 */
	@Override
	public void setTargetProperties(Map<String, String> targetProperties) throws TargetException {

		// Check the properties
		parseProperties(targetProperties );
		this.targetProperties = targetProperties;

		// Configure the IaaS client
		AWSCredentials credentials = new BasicAWSCredentials(
				targetProperties.get(Ec2Constants.EC2_ACCESS_KEY),
				targetProperties.get(Ec2Constants.EC2_SECRET_KEY));

		this.ec2 = new AmazonEC2Client( credentials );
		this.ec2.setEndpoint( targetProperties.get(Ec2Constants.EC2_ENDPOINT));
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.target.api.TargetHandler
	 * #createOrConfigureMachine(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public String createOrConfigureMachine(
			String messagingIp,
			String messagingUsername,
			String messagingPassword,
			String rootInstanceName,
			String applicationName )
	throws TargetException {

		String instanceId = null;
		try {
			String userData = DataHelpers.writeUserDataAsString(
					messagingIp, messagingUsername, messagingPassword,
					applicationName, rootInstanceName );

			RunInstancesRequest runInstancesRequest = prepareEC2RequestNode(
					this.targetProperties.get(Ec2Constants.AMI_VM_NODE),
					userData );

			RunInstancesResult runInstanceResult = this.ec2.runInstances( runInstancesRequest );
			instanceId = runInstanceResult.getReservation().getInstances().get( 0 ).getInstanceId();

			// Is there any volume (ID or name) to attach ?
			String snapshotIdToAttach = this.targetProperties.get(Ec2Constants.VOLUME_SNAPSHOT_ID);
			if(snapshotIdToAttach != null) {
				boolean running = false;
				while(! running) {
					DescribeInstancesRequest dis = new DescribeInstancesRequest();
					ArrayList<String> instanceIds = new ArrayList<String>();
					instanceIds.add(instanceId);
					dis.setInstanceIds(instanceIds);
					DescribeInstancesResult disresult = this.ec2.describeInstances(dis);
					running = "running".equals(disresult.getReservations().get(0).getInstances().get(0).getState().getName());
					if(! running) {
						try {
							Thread.sleep(5000);

						} catch (InterruptedException e) {
							// nothing
						}
					}
				}
				CreateVolumeRequest createVolumeRequest = new CreateVolumeRequest()
					.withAvailabilityZone("eu-west-1c")
					.withSnapshotId(snapshotIdToAttach);
					//.withSize(2); // The size of the volume, in gigabytes.

				CreateVolumeResult createVolumeResult = this.ec2.createVolume(createVolumeRequest);

				running = false;
				while(! running) {
					DescribeVolumesRequest dvs = new DescribeVolumesRequest();
					ArrayList<String> volumeIds = new ArrayList<String>();
					volumeIds.add(createVolumeResult.getVolume().getVolumeId());
					DescribeVolumesResult dvsresult = this.ec2.describeVolumes(dvs);
					running = "available".equals(dvsresult.getVolumes().get(0).getState());
					if(! running) {
						try {
							Thread.sleep(5000);

						} catch( InterruptedException e ) {
							// nothing
						}
					}
				}

				AttachVolumeRequest attachRequest = new AttachVolumeRequest()
					.withInstanceId(instanceId)
					.withDevice("/dev/sda2")
					.withVolumeId(createVolumeResult.getVolume().getVolumeId());

				this.ec2.attachVolume( attachRequest );
			}

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
			throw new TargetException( e );

		} catch( AmazonClientException e ) {
			this.logger.severe( "An error occurred while creating a machine on Amazon EC2. " + e.getMessage());
			throw new TargetException( e );

		} catch( UnsupportedEncodingException e ) {
			this.logger.severe( "An error occurred while contacting Amazon EC2. " + e.getMessage());
			throw new TargetException( e );

		} catch( IOException e ) {
			this.logger.severe( "An error occurred while preparing the user data. " + e.getMessage());
			throw new TargetException( e );
		}

		return instanceId;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.target.api.TargetHandler
	 * #terminateMachine(java.lang.String)
	 */
	@Override
	public void terminateMachine( String instanceId ) throws TargetException {
		try {
			TerminateInstancesRequest terminateInstancesRequest = new TerminateInstancesRequest();
			terminateInstancesRequest.withInstanceIds( instanceId );
			this.ec2.terminateInstances( terminateInstancesRequest );

		} catch( AmazonServiceException e ) {
			this.logger.severe( "An error occurred on Amazon while terminating the machine. " + e.getMessage());
			throw new TargetException( e );

		} catch( AmazonClientException e ) {
			this.logger.severe( "An error occurred while terminating a machine on Amazon EC2. " + e.getMessage());
			throw new TargetException( e );
		}
	}


	/**
	 * Parses the properties and saves them in a Java bean.
	 * @param targetProperties the IaaS properties
	 * @throws TargetException
	 */
	private void parseProperties( Map<String, String> targetProperties ) throws TargetException {

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
	 * Prepares the request.
	 * @param machineImageId the ID of the image to use
	 * @param userData the user data to pass
	 * @return a request
	 * @throws UnsupportedEncodingException
	 */
	private RunInstancesRequest prepareEC2RequestNode( String machineImageId, String userData )
	throws UnsupportedEncodingException {

		RunInstancesRequest runInstancesRequest = new RunInstancesRequest();
		String flavor = this.targetProperties.get(Ec2Constants.VM_INSTANCE_TYPE);
		if(StringUtils.isBlank(flavor)) flavor = "t1.micro";
		runInstancesRequest.setInstanceType( this.targetProperties.get(Ec2Constants.VM_INSTANCE_TYPE));
		if( StringUtils.isBlank( machineImageId ))
			runInstancesRequest.setImageId( this.targetProperties.get(Ec2Constants.AMI_VM_NODE));
		else
			runInstancesRequest.setImageId( machineImageId );

		// TBD provide kernel ID (eg. "aki-62695816")?
		// runInstancesRequest.setKernelId(this.targetProperties.get(Ec2Constants.KERNEL_ID);
		runInstancesRequest.setMinCount( 1 );
		runInstancesRequest.setMaxCount( 1 );
		runInstancesRequest.setKeyName( this.targetProperties.get(Ec2Constants.SSH_KEY_NAME));
		String secGroup = this.targetProperties.get(Ec2Constants.SECURITY_GROUP_NAME);
		if(StringUtils.isBlank(secGroup)) secGroup = "default";
		runInstancesRequest.setSecurityGroups(Arrays.asList(secGroup));

/*
		// Create the block device mapping to describe the root partition.
		BlockDeviceMapping blockDeviceMapping = new BlockDeviceMapping();
		blockDeviceMapping.setDeviceName("/dev/sda1");

		// Set the delete on termination flag to false.
		EbsBlockDevice ebs = new EbsBlockDevice();
		ebs.setSnapshotId(snapshotId);
		ebs.setDeleteOnTermination(Boolean.FALSE);

		blockDeviceMapping.setEbs(ebs);

		// Add the block device mapping to the block list.
		ArrayList<BlockDeviceMapping> blockList = new ArrayList<BlockDeviceMapping>();
		blockList.add(blockDeviceMapping);

		// Set the block device mapping configuration in the launch specifications.
		runInstancesRequest.setBlockDeviceMappings(blockList);
*/


		// The following part enables to transmit data to the VM.
		// When the VM is up, it will be able to read this data.
		String encodedUserData = new String( Base64.encodeBase64( userData.getBytes( "UTF-8" )), "UTF-8" );
		runInstancesRequest.setUserData( encodedUserData );

		return runInstancesRequest;
	}
}
