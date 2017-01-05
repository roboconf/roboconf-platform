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

/**
 * The constants defining properties for the EC2 IaaS.
 * @author Vincent Zurczak - Linagora
 */
public interface Ec2Constants {

	/**
	 * Where to access EC2 API to manipulate its resources.
	 */
	String EC2_ENDPOINT = "ec2.endpoint";

	/**
	 * The access key for EC2 API.
	 */
	String EC2_ACCESS_KEY = "ec2.access.key";

	/**
	 * The secret key for EC2 API.
	 */
	String EC2_SECRET_KEY = "ec2.secret.key";

	/**
	 * The ID of the image to instantiate for new VMs.
	 */
	String AMI_VM_NODE = "ec2.ami";

	/**
	 * The kind of instance to create (<i>flavour</i>).
	 */
	String VM_INSTANCE_TYPE = "ec2.instance.type";

	/**
	 * The SSH key name.
	 */
	String SSH_KEY_NAME = "ec2.ssh.key";

	/**
	 * The name of the security group for new VMs.
	 */
	String SECURITY_GROUP_NAME = "ec2.security.group";

	/**
	 * An elastic IP to associate with a VM.
	 */
	String ELASTIC_IP = "ec2.elastic.ip";

	/**
	 * The availability zone for instances (necessary to reuse volumes: only volumes in the same zone can be attached).
	 */
	String AVAILABILITY_ZONE = "ec2.availability.zone";

	/**
	 * Use block storage, or not.
	 */
	String USE_BLOCK_STORAGE = "ec2.use-block-storage";

	/**
	 * EBS (Elastic Block Storage) snapshot ID.
	 */
	String VOLUME_SNAPSHOT_ID = "ec2.ebs-snapshot-id";

	/**
	 * EBS volume size (GB).
	 */
	String VOLUME_SIZE = "ec2.ebs-size";

	/**
	 * EBS mount point (eg. /dev/sda2).
	 */
	String VOLUME_MOUNTPOINT = "ec2.ebs-mount-point";

	/**
	 * EBS volume type (general purpose or provisioned iops SSD, magnetic).
	 */
	String VOLUME_TYPE = "ec2.ebs-type";

	/**
	 * EBS delete volume on termination (default false).
	 */
	String VOLUME_VOLATILE = "ec2.ebs-delete-on-termination";
}
