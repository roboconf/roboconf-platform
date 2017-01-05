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
 * The constants defining properties for the Azure IaaS.
 * @author Linh-Manh Pham - LIG
 */
public interface AzureConstants {

	/**
	 * The subscription ID for Azure API.
	 */
	String AZURE_SUBSCRIPTION_ID = "azure.subscription.id";

	/**
	 * The path to key store file authentication with Azure API.
	 */
	String AZURE_KEY_STORE_FILE = "azure.key.store.file";

	/**
	 * The password for key store file.
	 */
	String AZURE_KEY_STORE_PASSWORD = "azure.key.store.password";

	/**
	 * The path to Create Cloud Service xml template file.
	 */
	String AZURE_CREATE_CLOUD_SERVICE_TEMPLATE = "azure.create.cloud.service.template";

	/**
	 * The path to Create Deployment xml template file.
	 */
	String AZURE_CREATE_DEPLOYMENT_TEMPLATE = "azure.create.deployment.template";

	/**
	 * The location for all Azure services.
	 */
	String AZURE_LOCATION = "azure.location";

	/**
	 * The size of a VM Azure according to http://msdn.microsoft.com/library/azure/dn197896.aspx .
	 */
	String AZURE_VM_SIZE = "azure.vm.size";

	/**
	 * The name of a VM template on your account.
	 */
	String AZURE_VM_TEMPLATE = "azure.vm.template";


}
