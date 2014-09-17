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

package net.roboconf.iaas.vmware.internal;

import java.util.HashMap;

import junit.framework.Assert;
import net.roboconf.iaas.api.IaasException;

import org.junit.Test;

/**
 * @author Pierre-Yves Gibello - Linagora
 */
public class IaasVmwareTest {

	@Test( expected = IaasException.class )
	public void testInvalidConfiguration_1() throws Exception {

		IaasVmware iaas = new IaasVmware();
		iaas.setIaasProperties( new HashMap<String,String> ());
	}


	@Test( expected = IaasException.class )
	public void testInvalidConfiguration_2() throws Exception {

		IaasVmware iaas = new IaasVmware();
		HashMap<String, String> iaasProperties = new HashMap<String,String> ();
		iaasProperties.put("ipMessagingServer", "127.0.0.1" );

		iaas.setIaasProperties( iaasProperties );
	}


//	@Test
//	public void testValidConfiguration() throws Exception {
//
//		Map<String, String> iaasProperties = new HashMap<String,String> ();
//		IaasVmware iaas = new IaasVmware();
//
//		iaasProperties.put( "vmware.url", "https://localhost:8890/sdk" );
//		iaasProperties.put( "vmware.user", "roboconf" );
//		iaasProperties.put( "vmware.password", "password" );
//		iaasProperties.put( "vmware.ignorecert", "true" );
//		iaasProperties.put( "vmware.cluster", "MYCLUSTER" );
//
//		iaas.setIaasProperties( iaasProperties );
//	}


	@Test
	public void testGetIaasType() {
		Assert.assertEquals( IaasVmware.IAAS_TYPE, new IaasVmware().getIaasType());
	}
}
