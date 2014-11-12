/**
 * Copyright 2014 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.target.openstack.internal;


/**
 * @author Vincent Zurczak - Linagora
 */
public class OpenstackBean {

	private String machineImageId, volumeId, volumeSize, volumeMountPoint;
	private String tenantId, keypair;
	private String flavor = "m1.tiny";
	private String securityGroup = "default";
	private String floatingIpPool;
	private String identityUrl;
	private String computeUrl;
	private String user, password;
	private String networkId, fixedIp;


	/**
	 * @return the tenantId
	 */
	public String getTenantId() {
		return this.tenantId;
	}

	/**
	 * @param tenantId the tenantId to set
	 */
	public void setTenantId( String tenantId ) {
		this.tenantId = tenantId;
	}

	/**
	 * @return the keypair
	 */
	public String getKeypair() {
		return this.keypair;
	}

	/**
	 * @param keypair the keypair to set
	 */
	public void setKeypair( String keypair ) {
		this.keypair = keypair;
	}

	/**
	 * @return the flavor
	 */
	public String getFlavor() {
		return this.flavor;
	}

	/**
	 * @param flavor the flavor to set
	 */
	public void setFlavor( String flavor ) {
		this.flavor = flavor;
	}

	/**
	 * @return the securityGroup
	 */
	public String getSecurityGroup() {
		return this.securityGroup;
	}

	/**
	 * @param securityGroup the securityGroup to set
	 */
	public void setSecurityGroup( String securityGroup ) {
		this.securityGroup = securityGroup;
	}

	/**
	 * @return the floatingIpPool
	 */
	public String getFloatingIpPool() {
		return this.floatingIpPool;
	}

	/**
	 * @param floatingIpPool the floatingIpPool to set
	 */
	public void setFloatingIpPool( String floatingIpPool ) {
		this.floatingIpPool = floatingIpPool;
	}

	/**
	 * @return the identityUrl
	 */
	public String getIdentityUrl() {
		return this.identityUrl;
	}

	/**
	 * @param identityUrl the identityUrl to set
	 */
	public void setIdentityUrl( String identityUrl ) {
		this.identityUrl = identityUrl;
	}

	/**
	 * @return the computeUrl
	 */
	public String getComputeUrl() {
		return this.computeUrl;
	}

	/**
	 * @param computeUrl the computeUrl to set
	 */
	public void setComputeUrl( String computeUrl ) {
		this.computeUrl = computeUrl;
	}

	/**
	 * @return the user
	 */
	public String getUser() {
		return this.user;
	}

	/**
	 * @param user the user to set
	 */
	public void setUser( String user ) {
		this.user = user;
	}

	/**
	 * @return the password
	 */
	public String getPassword() {
		return this.password;
	}

	/**
	 * @param password the password to set
	 */
	public void setPassword( String password ) {
		this.password = password;
	}

	/**
	 * @return the machineImageId
	 */
	public String getMachineImageId() {
		return this.machineImageId;
	}

	/**
	 * @param machineImageId the machineImageId to set
	 */
	public void setMachineImageId( String machineImageId ) {
		this.machineImageId = machineImageId;
	}

	/**
	 * @return the volumeId
	 */
	public String getVolumeId() {
		return this.volumeId;
	}

	/**
	 * @param volumeId the volumeId to set
	 */
	public void setVolumeId( String volumeId ) {
		this.volumeId = volumeId;
	}

	/**
	 * @return the volumeSize
	 */
	public String getVolumeSize() {
		return this.volumeSize;
	}

	/**
	 * @param volumeSize the volumeSize to set
	 */
	public void setVolumeSize( String volumeSize ) {
		this.volumeSize = volumeSize;
	}

	/**
	 * @return the networkId
	 */
	public String getNetworkId() {
		return this.networkId;
	}

	/**
	 * @param networkId the networkId to set
	 */
	public void setNetworkId( String networkId ) {
		this.networkId = networkId;
	}

	/**
	 * @return the fixedIp
	 */
	public String getFixedIp() {
		return this.fixedIp;
	}

	/**
	 * @param fixedIp the fixedIp to set
	 */
	public void setFixedIp( String fixedIp ) {
		this.fixedIp = fixedIp;
	}

	/**
	 * @return the volumeMountPoint
	 */
	public String getVolumeMountPoint() {
		return this.volumeMountPoint;
	}

	/**
	 * @param volumeMountPoint the volumeMountPoint to set
	 */
	public void setVolumeMountPoint( String volumeMountPoint ) {
		this.volumeMountPoint = volumeMountPoint;
	}
}
