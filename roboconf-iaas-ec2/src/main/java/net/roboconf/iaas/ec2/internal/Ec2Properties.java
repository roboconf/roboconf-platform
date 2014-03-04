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
 * A Java bean to manipulate IaaS properties for EC2.
 * @author Vincent Zurczak - Linagora
 */
public class Ec2Properties {

	private String endpoint, accessKey, secretKey;
	private String amiVmNode, vmInstanceType;
	private String sshKeyName, securityGroupName;


	/**
	 * @return the endpoint
	 */
	public String getEndpoint() {
		return this.endpoint;
	}

	/**
	 * @param endpoint the endpoint to set
	 */
	public void setEndpoint( String endpoint ) {
		this.endpoint = endpoint;
	}

	/**
	 * @return the accessKey
	 */
	public String getAccessKey() {
		return this.accessKey;
	}

	/**
	 * @param accessKey the accessKey to set
	 */
	public void setAccessKey( String accessKey ) {
		this.accessKey = accessKey;
	}

	/**
	 * @return the secretKey
	 */
	public String getSecretKey() {
		return this.secretKey;
	}

	/**
	 * @param secretKey the secretKey to set
	 */
	public void setSecretKey( String secretKey ) {
		this.secretKey = secretKey;
	}

	/**
	 * @return the amiVmNode
	 */
	public String getAmiVmNode() {
		return this.amiVmNode;
	}

	/**
	 * @param amiVmNode the amiVmNode to set
	 */
	public void setAmiVmNode( String amiVmNode ) {
		this.amiVmNode = amiVmNode;
	}

	/**
	 * @return the vmInstanceType
	 */
	public String getVmInstanceType() {
		return this.vmInstanceType;
	}

	/**
	 * @param vmInstanceType the vmInstanceType to set
	 */
	public void setVmInstanceType( String vmInstanceType ) {
		this.vmInstanceType = vmInstanceType;
	}

	/**
	 * @return the sshKeyName
	 */
	public String getSshKeyName() {
		return this.sshKeyName;
	}

	/**
	 * @param sshKeyName the sshKeyName to set
	 */
	public void setSshKeyName( String sshKeyName ) {
		this.sshKeyName = sshKeyName;
	}

	/**
	 * @return the securityGroupName
	 */
	public String getSecurityGroupName() {
		return this.securityGroupName;
	}

	/**
	 * @param securityGroupName the securityGroupName to set
	 */
	public void setSecurityGroupName( String securityGroupName ) {
		this.securityGroupName = securityGroupName;
	}
}
