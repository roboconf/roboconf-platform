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

package net.roboconf.target.vmware.internal;

import java.util.HashMap;

import junit.framework.Assert;
import net.roboconf.target.api.TargetException;

import org.junit.Test;

/**
 * @author Pierre-Yves Gibello - Linagora
 */
public class VmwareIaasHandlerTest {

	@Test( expected = TargetException.class )
	public void testInvalidConfiguration_1() throws Exception {

		VmwareIaasHandler target = new VmwareIaasHandler();
		target.setTargetProperties( new HashMap<String,String> ());
	}


	@Test( expected = TargetException.class )
	public void testInvalidConfiguration_2() throws Exception {

		VmwareIaasHandler target = new VmwareIaasHandler();
		HashMap<String, String> targetProperties = new HashMap<String,String> ();
		targetProperties.put("ipMessagingServer", "127.0.0.1" );

		target.setTargetProperties( targetProperties );
	}


//	@Test
//	public void testValidConfiguration() throws Exception {
//
//		Map<String, String> targetProperties = new HashMap<String,String> ();
//		VmwareIaasHandler target = new VmwareIaasHandler();
//
//		targetProperties.put( "vmware.url", "https://localhost:8890/sdk" );
//		targetProperties.put( "vmware.user", "roboconf" );
//		targetProperties.put( "vmware.password", "password" );
//		targetProperties.put( "vmware.ignorecert", "true" );
//		targetProperties.put( "vmware.cluster", "MYCLUSTER" );
//
//		target.setIaasProperties( targetProperties );
//	}


	@Test
	public void testGetTargetId() {
		Assert.assertEquals( VmwareIaasHandler.TARGET_ID, new VmwareIaasHandler().getTargetId());
	}
}
