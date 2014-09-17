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

package net.roboconf.iaas.azure.internal;

import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;
import net.roboconf.iaas.api.IaasException;

import org.junit.Test;

/**
 * @author Linh-Manh Pham - LIG
 */
public class IaasAzureTest {

	@Test( expected = IaasException.class )
	public void testInvalidConfiguration() throws Exception {

		IaasAzure iaas = new IaasAzure();
		iaas.setIaasProperties( new HashMap<String,String> ());
	}


	@Test
	public void testValidConfiguration() throws Exception {

		Map<String, String> iaasProperties = new HashMap<String,String> ();
		IaasAzure iaas = new IaasAzure();

		iaasProperties.put( AzureConstants.AZURE_SUBSCRIPTION_ID, "my subscription id" );
		iaasProperties.put( AzureConstants.AZURE_KEY_STORE_FILE, "path to key store file" );
		iaasProperties.put( AzureConstants.AZURE_KEY_STORE_PASSWORD, "key store password" );
		iaasProperties.put( AzureConstants.AZURE_CREATE_CLOUD_SERVICE_TEMPLATE, "create cloud service template" );
		iaasProperties.put( AzureConstants.AZURE_CREATE_DEPLOYMENT_TEMPLATE, "create deployment template" );
		iaasProperties.put( AzureConstants.AZURE_LOCATION, "azure location" );
		iaasProperties.put( AzureConstants.AZURE_VM_SIZE, "azure VM size" );
		iaasProperties.put( AzureConstants.AZURE_VM_TEMPLATE, "azure VM template" );

		iaas.setIaasProperties( iaasProperties );
	}


	@Test
	public void testGetIaasType() {
		Assert.assertEquals( IaasAzure.IAAS_TYPE, new IaasAzure().getIaasType());
	}
}
