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

package net.roboconf.target.jclouds.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.Assert;
import net.roboconf.target.api.TargetException;

import org.junit.Test;

/**
 * @author Pierre-Yves Gibello - Linagora
 */
public class JCloudsHandlerTest {

	@Test
	public void testGetTargetId() {
		Assert.assertEquals( JCloudsHandler.TARGET_ID, new JCloudsHandler().getTargetId());
	}


	@Test
	public void testValidate() throws Exception {

		Map<String,String> targetProperties = new HashMap<String,String> ();
		targetProperties.put( JCloudsHandler.PROVIDER_ID, UUID.randomUUID().toString());
		targetProperties.put( JCloudsHandler.ENDPOINT, UUID.randomUUID().toString());
		targetProperties.put( JCloudsHandler.IMAGE_NAME, UUID.randomUUID().toString());
		targetProperties.put( JCloudsHandler.SECURITY_GROUP, UUID.randomUUID().toString());
		targetProperties.put( JCloudsHandler.IDENTITY, UUID.randomUUID().toString());
		targetProperties.put( JCloudsHandler.CREDENTIAL, UUID.randomUUID().toString());
		targetProperties.put( JCloudsHandler.HARDWARE_NAME, UUID.randomUUID().toString());
		JCloudsHandler.validate( targetProperties );

		targetProperties.put( JCloudsHandler.KEY_PAIR, UUID.randomUUID().toString());
		JCloudsHandler.validate( targetProperties );
	}


	@Test( expected = TargetException.class )
	public void testValidate_error1() throws Exception {

		// Provider_ID is missing
		Map<String,String> targetProperties = new HashMap<String,String> ();
		targetProperties.put( JCloudsHandler.ENDPOINT, UUID.randomUUID().toString());
		targetProperties.put( JCloudsHandler.IMAGE_NAME, UUID.randomUUID().toString());
		targetProperties.put( JCloudsHandler.SECURITY_GROUP, UUID.randomUUID().toString());
		targetProperties.put( JCloudsHandler.IDENTITY, UUID.randomUUID().toString());
		targetProperties.put( JCloudsHandler.CREDENTIAL, UUID.randomUUID().toString());
		targetProperties.put( JCloudsHandler.HARDWARE_NAME, UUID.randomUUID().toString());
		JCloudsHandler.validate( targetProperties );
	}


	@Test( expected = TargetException.class )
	public void testValidate_error2() throws Exception {

		JCloudsHandler.validate( new HashMap<String,String>( 0 ));
	}
}
