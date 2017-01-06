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

package net.roboconf.target.azure.internal;

/**
 * A Java bean to manipulate IaaS properties for Azure.
 * @author Linh-Manh Pham - LIG
 */
public class AzureProperties {

	private String subscriptionId;
	private String keyStoreFile, keyStorePassword;
	private String createCloudServiceTemplate, createDeploymentTemplate;
	private String location, vmSize, vmTemplate;


	/**
	 * @return the subscriptionId
	 */
	public String getSubscriptionId() {
		return this.subscriptionId;
	}

	/**
	 * @param subscriptionId the subscriptionId to set
	 */
	public void setSubscriptionId( String subscriptionId ) {
		this.subscriptionId = subscriptionId;
	}

	/**
	 * @return the keyStoreFile
	 */
	public String getKeyStoreFile() {
		return this.keyStoreFile;
	}

	/**
	 * @param keyStoreFile the keyStoreFile to set
	 */
	public void setKeyStoreFile( String keyStoreFile ) {
		this.keyStoreFile = keyStoreFile;
	}

	/**
	 * @return the keyStorePassword
	 */
	public String getKeyStorePassword() {
		return this.keyStorePassword;
	}

	/**
	 * @param keyStorePassword the keyStorePassword to set
	 */
	public void setKeyStorePassword( String keyStorePassword ) {
		this.keyStorePassword = keyStorePassword;
	}

	/**
	 * @return the path to createCloudService.xml template file
	 */
	public String getCreateCloudServiceTemplate() {
		return this.createCloudServiceTemplate;
	}

	/**
	 * @param createCloudServiceTemplate the createCloudServiceFile.xml template file to set
	 */
	public void setCreateCloudServiceTemplate( String createCloudServiceTemplate ) {
		this.createCloudServiceTemplate = createCloudServiceTemplate;
	}

	/**
	 * @return the path to createDeployment.xml template file
	 */
	public String getCreateDeploymentTemplate() {
		return this.createDeploymentTemplate;
	}

	/**
	 * @param createDeploymentTemplate the createDeployment.xml template file to set
	 */
	public void setCreateDeploymentTemplate( String createDeploymentTemplate ) {
		this.createDeploymentTemplate = createDeploymentTemplate;
	}

	/**
	 * @return the location of Azure services
	 */
	public String getLocation() {
		return this.location;
	}

	/**
	 * @param location the location to set
	 */
	public void setLocation( String location ) {
		this.location = location;
	}

	/**
	 * @return the VM size
	 */
	public String getVMSize() {
		return this.vmSize;
	}

	/**
	 * @param vmSize the VM Size to set
	 */
	public void setVMSize( String vmSize ) {
		this.vmSize = vmSize;
	}

	/**
	 * @return the VM Template name
	 */
	public String getVMTemplate() {
		return this.vmTemplate;
	}

	/**
	 * @param vmTemplate the name of VM Template to set
	 */
	public void setVMTemplate( String vmTemplate ) {
		this.vmTemplate = vmTemplate;
	}
}
