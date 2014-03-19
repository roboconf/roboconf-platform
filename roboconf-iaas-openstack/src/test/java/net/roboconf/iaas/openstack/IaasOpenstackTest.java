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

package net.roboconf.iaas.openstack;

import java.util.HashMap;

import junit.framework.Assert;
import net.roboconf.iaas.api.exceptions.InvalidIaasPropertiesException;
import net.roboconf.iaas.openstack.IaasOpenstack;

import org.junit.Test;

/**
 * @author Pierre-Yves Gibello - Linagora
 */
public class IaasOpenstackTest {

	@Test
	public void testConfigurationParsing() {

		// Empty configuration
		HashMap<String, String> iaasProperties = new HashMap<String, String>();
		IaasOpenstack iaas = new IaasOpenstack();
		try {
			iaas.setIaasProperties( iaasProperties );
			Assert.fail( "An invalid configuration should have been detected." );

		} catch( InvalidIaasPropertiesException e ) {
			Assert.assertTrue(true);
		}

		// Move on
		iaasProperties.put("ipMessagingServer", "127.0.0.1" );
		try {
			iaas.setIaasProperties( iaasProperties );
			Assert.fail( "An invalid configuration should have been detected." );

		} catch( InvalidIaasPropertiesException e ) {
			Assert.assertTrue(true);
		}

		//TODO parameterize test with external file for IaaS credentials ?
		/*
		// Fill-in everything
		iaasProperties.put("openstack.tenantId", "bf6110e105824ae2b412c7db53d4d79a");
		iaasProperties.put("openstack.user", "username");
		iaasProperties.put("openstack.password", "password");
		iaasProperties.put("openstack.keypair", "mykey");
		iaasProperties.put("openstack.flavor", "m1.small");
		iaasProperties.put("openstack.floatingIpPool", "public");
		iaasProperties.put("openstack.identityUrl", "http://mystack:5000/v2.0");
		iaasProperties.put("openstack.computeUrl", "http://mystack:8774/v2");
		iaasProperties.put("openstack.image", "92a3c0b8-eef6-4b64-b569-2cdf85101d15");

		try {
			iaas.setIaasProperties( iaasProperties );

		} catch( InvalidIaasPropertiesException e ) {
			e.printStackTrace();
			Assert.fail( "An invalid configuration was detected while it was valid." );
		}
		*/
	}
}
