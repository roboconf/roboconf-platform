/**
 * Copyright 2013 Linagora, Université Joseph Fourier
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

package net.roboconf.iaas.ec2.internal;

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
}
