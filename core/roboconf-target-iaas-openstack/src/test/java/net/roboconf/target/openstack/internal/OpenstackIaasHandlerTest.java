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

package net.roboconf.target.openstack.internal;

import java.util.HashMap;

import junit.framework.Assert;
import net.roboconf.target.api.TargetException;

import org.junit.Test;

/**
 * @author Pierre-Yves Gibello - Linagora
 */
public class OpenstackIaasHandlerTest {

	@Test( expected = TargetException.class )
	public void testInvalidConfiguration_1() throws Exception {

		OpenstackIaasHandler target = new OpenstackIaasHandler();
		target.setTargetProperties( new HashMap<String, String>());
	}


	@Test( expected = TargetException.class )
	public void testInvalidConfiguration_2() throws Exception {

		OpenstackIaasHandler target = new OpenstackIaasHandler();
		HashMap<String, String> targetProperties = new HashMap<String, String>();
		targetProperties.put("ipMessagingServer", "127.0.0.1" );

		target.setTargetProperties( targetProperties );
	}


	@Test
	public void testGetTargetId() {
		Assert.assertEquals( OpenstackIaasHandler.TARGET_ID, new OpenstackIaasHandler().getTargetId());
	}
}
