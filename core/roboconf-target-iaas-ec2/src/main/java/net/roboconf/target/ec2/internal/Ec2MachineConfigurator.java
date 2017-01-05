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

package net.roboconf.target.ec2.internal;

import static net.roboconf.target.ec2.internal.Ec2IaasHandler.VOLUME_DELETE_OT_PREFIX;
import static net.roboconf.target.ec2.internal.Ec2IaasHandler.VOLUME_MOUNT_POINT_PREFIX;
import static net.roboconf.target.ec2.internal.Ec2IaasHandler.VOLUME_NAME_PREFIX;
import static net.roboconf.target.ec2.internal.Ec2IaasHandler.VOLUME_SIZE_GB_PREFIX;
import static net.roboconf.target.ec2.internal.Ec2IaasHandler.VOLUME_TYPE_PREFIX;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.AssociateAddressRequest;
import com.amazonaws.services.ec2.model.AttachVolumeRequest;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.CreateVolumeRequest;
import com.amazonaws.services.ec2.model.CreateVolumeResult;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DescribeVolumesRequest;
import com.amazonaws.services.ec2.model.DescribeVolumesResult;
import com.amazonaws.services.ec2.model.EbsInstanceBlockDeviceSpecification;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.InstanceBlockDeviceMappingSpecification;
import com.amazonaws.services.ec2.model.ModifyInstanceAttributeRequest;
import com.amazonaws.services.ec2.model.Tag;

import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.utils.Utils;
import net.roboconf.target.api.AbstractThreadedTargetHandler.MachineConfigurator;
import net.roboconf.target.api.TargetException;

/**
 * A machine configurator for EC2.
 * @author Vincent Zurczak - Linagora
 */
public class Ec2MachineConfigurator implements MachineConfigurator {

	/**
	 * The steps of a workflow.
	 * <ul>
	 * <li>UNKNOWN_VM: the VM must be known by all the EC2 parts.</li>
	 * <li>TAG_VM: tag the VM.</li>
	 * <li>ASSOCIATE_ELASTIC_IP: an elastic IP has to be associated.</li>
	 * <li>RUNNING_VM: the VM must be running (started).</li>
	 * <li>ASSOCIATE_STORAGE: associate storage.</li>
	 * <li>COMPLETE: there is nothing to do anymore.</li>
	 * </ul>
	 *
	 * @author Vincent Zurczak - Linagora
	 */
	public enum State {
		UNKNOWN_VM, TAG_VM, RUNNING_VM, ASSOCIATE_ELASTIC_IP, CREATE_VOLUME, ATTACH_VOLUME, COMPLETE
	}

	private final Instance scopedInstance;
	private final String machineId, tagName;
	private final String applicationName;
	private String availabilityZone;
	private final Map<String,String> targetProperties;
	private final Logger logger = Logger.getLogger( getClass().getName());
	private final Map<String,String> storageIdToVolumeId = new HashMap<> ();

	private AmazonEC2 ec2Api;
	private State state = State.UNKNOWN_VM;

	private static final int DEFAULT_VOLUME_SIZE = 2;

	/**
	 * Constructor.
	 */
	public Ec2MachineConfigurator(
			Map<String,String> targetProperties,
			String machineId,
			String applicationName,
			String rootInstanceName,
			Instance scopedInstance  ) {

		this.machineId = machineId;
		this.targetProperties = targetProperties;
		this.applicationName = applicationName;
		this.tagName = applicationName + "." + rootInstanceName;
		this.scopedInstance = scopedInstance;
		this.availabilityZone = targetProperties.get(Ec2Constants.AVAILABILITY_ZONE);
	}

	@Override
	public Instance getScopedInstance() {
		return this.scopedInstance;
	}

	@Override
	public void close() throws IOException {
		// nothing
	}

	@Override
	public boolean configure() throws TargetException {

		if( this.ec2Api == null )
			this.ec2Api = Ec2IaasHandler.createEc2Client( this.targetProperties );

		if( this.state == State.UNKNOWN_VM )
			if( checkVmIsKnown())
				this.state = State.TAG_VM;

		// We cannot tag directly after the VM creation. See #197.
		// We need it to be known by all the EC2 components.
		if( this.state == State.TAG_VM )
			if( tagResource(this.machineId, this.tagName))
				this.state = State.RUNNING_VM;

		if( this.state == State.RUNNING_VM )
			if( checkVmIsStarted())
				this.state = State.ASSOCIATE_ELASTIC_IP;

		if( this.state == State.ASSOCIATE_ELASTIC_IP ) {
			if( associateElasticIp())
				this.state = State.CREATE_VOLUME;
		}

		if( this.state == State.CREATE_VOLUME ) {
			if(! volumesRequested()) {
				this.state = State.COMPLETE;
			} else if(createOrReuseVolumes()) {
				this.state = State.ATTACH_VOLUME;
			}
		}

		if( this.state == State.ATTACH_VOLUME ) {
			if(volumesCreated() && attachVolumes())
				this.state = State.COMPLETE;
		}

		return this.state == State.COMPLETE;
	}

	/**
	 * Checks whether a VM is known (i.e. all the EC2 parts know it).
	 * @return true if the VM is know, false otherwise
	 */
	private boolean checkVmIsKnown() {

		DescribeInstancesRequest dis = new DescribeInstancesRequest();
		dis.setInstanceIds(Collections.singletonList(this.machineId));

		DescribeInstancesResult disresult = this.ec2Api.describeInstances(dis);
		return disresult.getReservations().size() > 0
				&& disresult.getReservations().get( 0 ).getInstances().size() > 0;
	}

	/**
	 * Tags the specified resource, eg. a VM or volume (basically, it gives it a name).
	 * @param resourceId The ID of the resource to tag
	 * @param tagName The resource's name
	 * @return true if the tag was done, false otherwise
	 */
	private boolean tagResource(String resourceId, String tagName) {
		boolean result = false;
		if(! Utils.isEmptyOrWhitespaces(tagName)) {
			Tag tag = new Tag( "Name", tagName );
			CreateTagsRequest ctr = new CreateTagsRequest(Collections.singletonList(resourceId), Arrays.asList( tag ));
			try {
				this.ec2Api.createTags( ctr );
			} catch(Exception e) {
				this.logger.warning("Error tagging resource " + resourceId + " with name=" + tagName + ": " + e);
			}
			result = true;
		}
		return result;
	}

	/**
	 * Associates an elastic IP with the VM.
	 * @return true if there is nothing more to do about elastic IP configuration, false otherwise
	 */
	private boolean associateElasticIp() {

		String elasticIp = this.targetProperties.get( Ec2Constants.ELASTIC_IP );
		if( ! Utils.isEmptyOrWhitespaces( elasticIp )) {
			this.logger.fine( "Associating an elastic IP with the instance. IP = " + elasticIp );
			AssociateAddressRequest associateAddressRequest = new AssociateAddressRequest( this.machineId, elasticIp );
			this.ec2Api.associateAddress( associateAddressRequest );
		}

		return true;
	}

	/**
	 * Checks whether a VM is started or not (which is stronger than {@link #checkVmIsKnown()}).
	 * @return true if the VM is started, false otherwise
	 */
	private boolean checkVmIsStarted() {

		DescribeInstancesRequest dis = new DescribeInstancesRequest();
		dis.setInstanceIds(Collections.singletonList(this.machineId));

		DescribeInstancesResult disresult = this.ec2Api.describeInstances( dis );
		// Obtain availability zone (for later use, eg. volume attachment).
		// Necessary if no availability zone is specified in configuration
		// (because volumes must be attached to instances in the same availability zone).
		this.availabilityZone = disresult.getReservations().get(0).getInstances().get(0).getPlacement().getAvailabilityZone();
		return "running".equalsIgnoreCase( disresult.getReservations().get(0).getInstances().get(0).getState().getName());
	}

	/**
	 * Checks whether EBS volume(s) creation/attachment is requested.
	 * @return true if requested, false otherwise
	 */
	private boolean volumesRequested() {
		return ! Utils.isEmptyOrWhitespaces(this.targetProperties.get(Ec2Constants.USE_BLOCK_STORAGE));
	}

	/**
	 * Performs all steps necessary to create or reuse volume(s), as specified in configuration.
	 * @return true when volume(s) creation is done
	 */
	private boolean createOrReuseVolumes() {
		for( String storageId : Ec2IaasHandler.findStorageIds( this.targetProperties )) {
			String nameTemplate = Ec2IaasHandler.findStorageProperty(this.targetProperties, storageId, VOLUME_NAME_PREFIX);
			// Lookup volume, according to its snapshot ID or Name tag.
			String idOrName = Ec2IaasHandler.expandVolumeName(
					nameTemplate, this.applicationName, this.scopedInstance.getName());
			String volumeSnapshotOrId = lookupVolume(idOrName);
			if(volumeSnapshotOrId == null) volumeSnapshotOrId = idOrName;

			int size = DEFAULT_VOLUME_SIZE;
			try {
				size = Integer.parseInt(
						Ec2IaasHandler.findStorageProperty(this.targetProperties, storageId, VOLUME_SIZE_GB_PREFIX));
			} catch(Exception nfe) {
				size = DEFAULT_VOLUME_SIZE;
			}
			if(size <= 0) size = DEFAULT_VOLUME_SIZE;

			String volumeId;
			if(volumeSnapshotOrId != null && volumeCreated(volumeSnapshotOrId)) {
				volumeId = volumeSnapshotOrId;
			} else {
				volumeId = createVolume(storageId, volumeSnapshotOrId, size);
			}

			this.logger.info("Volume " + volumeId + " was successfully created.");
			this.storageIdToVolumeId.put(storageId, volumeId);
		}

		return true;
	}

	/**
	 * Creates volume for EBS.
	 * @return volume ID of newly created volume
	 */
	private String createVolume(String storageId, String snapshotId, int size) {
		String volumeType = Ec2IaasHandler.findStorageProperty(this.targetProperties, storageId, VOLUME_TYPE_PREFIX);
		if(volumeType == null) volumeType = "standard";

		CreateVolumeRequest createVolumeRequest = new CreateVolumeRequest()
				.withAvailabilityZone( this.availabilityZone )
				.withVolumeType( volumeType )
				.withSize( size ); // The size of the volume, in gigabytes.

		// EC2 snapshot IDs start with "snap-"...
		if(! Utils.isEmptyOrWhitespaces(snapshotId) && snapshotId.startsWith("snap-"))
			createVolumeRequest.withSnapshotId(snapshotId);

		CreateVolumeResult createVolumeResult = this.ec2Api.createVolume(createVolumeRequest);
		return createVolumeResult.getVolume().getVolumeId();
	}

	/**
	 * Checks whether volume is created.
	 * @param volumeId the EBS volume ID
	 * @return true if volume created, false otherwise
	 */
	private boolean volumeCreated(String volumeId) {
		DescribeVolumesRequest dvs = new DescribeVolumesRequest();
		ArrayList<String> volumeIds = new ArrayList<String>();
		volumeIds.add(volumeId);
		dvs.setVolumeIds(volumeIds);
		DescribeVolumesResult dvsresult = null;
		try {
			dvsresult = this.ec2Api.describeVolumes(dvs);
		} catch(Exception e) {
			dvsresult = null;
		}

		return dvsresult != null && "available".equals(dvsresult.getVolumes().get(0).getState());
	}

	/**
	 * Checks whether all specified volumes are created.
	 * @return true if all volumes created, false otherwise
	 */
	private boolean volumesCreated() {
		for( Map.Entry<String,String> entry : this.storageIdToVolumeId.entrySet()) {
			String volumeId = entry.getValue();
			if(! volumeCreated(volumeId)) return false;
		}
		return true;
	}

	/**
	 * Looks up volume, by ID or Name tag.
	 * @param volumeIdOrName the EBS volume ID or Name tag
	 * @return The volume ID of 1st matching volume found, null if no volume found
	 */
	private String lookupVolume(String volumeIdOrName) {

		String ret = null;
		if(! Utils.isEmptyOrWhitespaces(volumeIdOrName)) {
			// Lookup by volume ID
			DescribeVolumesRequest dvs = new DescribeVolumesRequest(Collections.singletonList(volumeIdOrName));
			DescribeVolumesResult dvsresult = null;

			try {
				dvsresult = this.ec2Api.describeVolumes(dvs);
			} catch(Exception e) {
				dvsresult = null;
			}

			// If not found, lookup by name
			if(dvsresult == null || dvsresult.getVolumes() == null || dvsresult.getVolumes().size() < 1) {
				dvs = new DescribeVolumesRequest().withFilters(new Filter().withName("tag:Name").withValues(volumeIdOrName));
				try {
					dvsresult = this.ec2Api.describeVolumes(dvs);
				} catch(Exception e) {
					dvsresult = null;
				}
			}

			if(dvsresult != null && dvsresult.getVolumes() != null && dvsresult.getVolumes().size() > 0)
				ret = dvsresult.getVolumes().get(0).getVolumeId();
		}

		return ret;
	}

	/**
	 * Attaches volume(s) for EBS.
	 * @return true if successful attachment, or nothing to do. false otherwise
	 */
	private boolean attachVolumes() {

		// If volume is found in map, it has been successfully created (no need to check here)
		for( Map.Entry<String,String> entry : this.storageIdToVolumeId.entrySet()) {

			String volumeId = entry.getValue();
			String storageId = entry.getKey();

			// Give a name to the volume before attaching
			String nameTemplate = Ec2IaasHandler.findStorageProperty(this.targetProperties, storageId, VOLUME_NAME_PREFIX);
			String name = Ec2IaasHandler.expandVolumeName(
					nameTemplate, this.applicationName, this.scopedInstance.getName());
			if(Utils.isEmptyOrWhitespaces(name)) name = "Created by Roboconf for " + this.tagName;
			tagResource(volumeId, name);

			// Attach volume now
			String mountPoint = Ec2IaasHandler.findStorageProperty(this.targetProperties, storageId, VOLUME_MOUNT_POINT_PREFIX);
			if(Utils.isEmptyOrWhitespaces(mountPoint)) mountPoint = "/dev/sdf";

			AttachVolumeRequest attachRequest = new AttachVolumeRequest()
				.withInstanceId(this.machineId)
				.withDevice(mountPoint)
				.withVolumeId(volumeId);

			try {
				this.ec2Api.attachVolume(attachRequest);
			} catch(Exception e) {
				this.logger.warning("EBS Volume attachment error: " + e);
			}

			// Set deleteOnTermination flag ?
			if(Boolean.parseBoolean(Ec2IaasHandler.findStorageProperty(this.targetProperties, storageId, VOLUME_DELETE_OT_PREFIX))) {
				EbsInstanceBlockDeviceSpecification ebsSpecification = new EbsInstanceBlockDeviceSpecification()
					.withVolumeId(volumeId)
					.withDeleteOnTermination(true);

				InstanceBlockDeviceMappingSpecification mappingSpecification = new InstanceBlockDeviceMappingSpecification()
					.withDeviceName(mountPoint)
					.withEbs(ebsSpecification);

				ModifyInstanceAttributeRequest request = new ModifyInstanceAttributeRequest()
					.withInstanceId(this.machineId)
					.withBlockDeviceMappings(mappingSpecification);

				this.ec2Api.modifyInstanceAttribute(request);
			}
		}

		return true;
	}

}
