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

import java.util.HashMap;

import junit.framework.Assert;
import net.roboconf.iaas.api.IaasException;

import org.junit.Test;

/**
 * @author Pierre-Yves Gibello - Linagora
 */
public class IaasOpenstackTest {

	@Test( expected = IaasException.class )
	public void testInvalidConfiguration_1() throws Exception {

		IaasOpenstack iaas = new IaasOpenstack();
		iaas.setIaasProperties( new HashMap<String, String>());
	}


	@Test( expected = IaasException.class )
	public void testInvalidConfiguration_2() throws Exception {

		IaasOpenstack iaas = new IaasOpenstack();
		HashMap<String, String> iaasProperties = new HashMap<String, String>();
		iaasProperties.put("ipMessagingServer", "127.0.0.1" );

		iaas.setIaasProperties( iaasProperties );
	}


//	@Test
//	public void testValidConfiguration() throws Exception {
//
//		IaasOpenstack iaas = new IaasOpenstack();
//		HashMap<String, String> iaasProperties = new HashMap<String, String>();
//
//		iaasProperties.put("openstack.tenantId", "bf6110e105824ae2b412c7db53d4d79a");
//		iaasProperties.put("openstack.user", "username");
//		iaasProperties.put("openstack.password", "password");
//		iaasProperties.put("openstack.keypair", "mykey");
//		iaasProperties.put("openstack.flavor", "m1.small");
//		iaasProperties.put("openstack.floatingIpPool", "public");
//		iaasProperties.put("openstack.identityUrl", "http://my-own-stack:5000/v2.0");
//		iaasProperties.put("openstack.computeUrl", "http://my-own-stack:8774/v2");
//		iaasProperties.put("openstack.image", "92a3c0b8-eef6-4b64-b569-2cdf85101d15");
//
//		iaas.setIaasProperties( iaasProperties );
//	}


	@Test
	public void testGetIaasType() {
		Assert.assertEquals( IaasOpenstack.IAAS_TYPE, new IaasOpenstack().getIaasType());
	}
}
