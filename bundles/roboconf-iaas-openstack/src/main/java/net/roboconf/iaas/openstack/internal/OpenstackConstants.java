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

package net.roboconf.iaas.openstack.internal;

/**
 * @author Pierre-Yves Gibello - Linagora
 */
public interface OpenstackConstants {
	String IMAGE = "openstack.image";
	String TENANT_ID = "openstack.tenantId";
	String KEYPAIR = "openstack.keypair";
	String FLOATING_IP_POOL = "openstack.floatingIpPool";
	String NETWORK_ID = "openstack.networkId";
	String FIXED_IP = "openstack.fixedIp";
	String FLAVOR = "openstack.flavor";
	String SECURITY_GROUP = "openstack.securityGroup";
	String IDENTITY_URL = "openstack.identityUrl";
	String COMPUTE_URL = "openstack.computeUrl";
	String USER = "openstack.user";
	String PASSWORD = "openstack.password";
	String VOLUME_ID = "openstack.volumeId";
	String VOLUME_MOUNT_POINT = "openstack.volumeMountPoint";
	String VOLUME_SIZE_GB = "openstack.volumeSizeGb";
}
