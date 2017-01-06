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

package net.roboconf.target.azure.internal;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import net.roboconf.target.api.TargetException;

import org.junit.Test;

/**
 * @author Linh-Manh Pham - LIG
 */
public class AzureIaasHandlerTest {

	@Test( expected = TargetException.class )
	public void testInvalidConfiguration() throws Exception {
		AzureIaasHandler.buildProperties( new HashMap<String,String> ());
	}


	@Test
	public void testValidConfiguration() throws Exception {

		Map<String,String> targetProperties = new HashMap<String,String> ();

		targetProperties.put( AzureConstants.AZURE_SUBSCRIPTION_ID, "my subscription id" );
		targetProperties.put( AzureConstants.AZURE_KEY_STORE_FILE, "path to key store file" );
		targetProperties.put( AzureConstants.AZURE_KEY_STORE_PASSWORD, "key store password" );
		targetProperties.put( AzureConstants.AZURE_CREATE_CLOUD_SERVICE_TEMPLATE, "create cloud service template" );
		targetProperties.put( AzureConstants.AZURE_CREATE_DEPLOYMENT_TEMPLATE, "create deployment template" );
		targetProperties.put( AzureConstants.AZURE_LOCATION, "azure location" );
		targetProperties.put( AzureConstants.AZURE_VM_SIZE, "azure VM size" );
		targetProperties.put( AzureConstants.AZURE_VM_TEMPLATE, "azure VM template" );

		AzureIaasHandler.buildProperties( targetProperties );
	}


	@Test
	public void testGetTargetId() {
		Assert.assertEquals( AzureIaasHandler.TARGET_ID, new AzureIaasHandler().getTargetId());
	}
}
