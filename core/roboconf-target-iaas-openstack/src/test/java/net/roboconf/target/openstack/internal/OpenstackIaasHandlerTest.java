/**
 * Copyright 2014-2015 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.target.openstack.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import junit.framework.Assert;
import net.roboconf.target.api.TargetException;

import org.junit.Test;

/**
 * @author Pierre-Yves Gibello - Linagora
 */
public class OpenstackIaasHandlerTest {

	@Test
	public void testGetTargetId() {
		Assert.assertEquals( OpenstackIaasHandler.TARGET_ID, new OpenstackIaasHandler().getTargetId());
	}


	@Test
	public void testValidate() throws Exception {

		Map<String,String> targetProperties = new HashMap<String,String> ();
		targetProperties.put( OpenstackIaasHandler.FLAVOR_NAME, UUID.randomUUID().toString());
		targetProperties.put( OpenstackIaasHandler.TENANT_NAME, UUID.randomUUID().toString());
		targetProperties.put( OpenstackIaasHandler.IMAGE_NAME, UUID.randomUUID().toString());
		targetProperties.put( OpenstackIaasHandler.SECURITY_GROUP, UUID.randomUUID().toString());
		targetProperties.put( OpenstackIaasHandler.USER, UUID.randomUUID().toString());
		targetProperties.put( OpenstackIaasHandler.PASSWORD, UUID.randomUUID().toString());
		targetProperties.put( OpenstackIaasHandler.KEY_PAIR, UUID.randomUUID().toString());
		targetProperties.put( OpenstackIaasHandler.API_URL, UUID.randomUUID().toString());
		OpenstackIaasHandler.validate( targetProperties );

		targetProperties.put( OpenstackIaasHandler.FLOATING_IP_POOL, UUID.randomUUID().toString());
		targetProperties.put( OpenstackIaasHandler.NETWORK_ID, UUID.randomUUID().toString());
		OpenstackIaasHandler.validate( targetProperties );
	}


	@Test( expected = TargetException.class )
	public void testValidate_error1() throws Exception {

		// Tenant name is missing
		Map<String,String> targetProperties = new HashMap<String,String> ();
		targetProperties.put( OpenstackIaasHandler.FLAVOR_NAME, UUID.randomUUID().toString());
		targetProperties.put( OpenstackIaasHandler.IMAGE_NAME, UUID.randomUUID().toString());
		targetProperties.put( OpenstackIaasHandler.SECURITY_GROUP, UUID.randomUUID().toString());
		targetProperties.put( OpenstackIaasHandler.USER, UUID.randomUUID().toString());
		targetProperties.put( OpenstackIaasHandler.PASSWORD, UUID.randomUUID().toString());
		targetProperties.put( OpenstackIaasHandler.KEY_PAIR, UUID.randomUUID().toString());
		targetProperties.put( OpenstackIaasHandler.API_URL, UUID.randomUUID().toString());
		OpenstackIaasHandler.validate( targetProperties );
	}


	@Test( expected = TargetException.class )
	public void testValidate_error2() throws Exception {

		OpenstackIaasHandler.validate( new HashMap<String,String>( 0 ));
	}


	@Test
	public void testIdentity() {

		Map<String,String> targetProperties = new HashMap<String,String> ();
		targetProperties.put( OpenstackIaasHandler.TENANT_NAME, "tenant" );
		targetProperties.put( OpenstackIaasHandler.USER, "me" );
		Assert.assertEquals( "tenant:me", OpenstackIaasHandler.identity( targetProperties ));

		targetProperties.remove( OpenstackIaasHandler.USER );
		Assert.assertEquals( "tenant:null", OpenstackIaasHandler.identity( targetProperties ));

		targetProperties.remove( OpenstackIaasHandler.TENANT_NAME );
		Assert.assertEquals( "null:null", OpenstackIaasHandler.identity( targetProperties ));

		targetProperties.put( OpenstackIaasHandler.USER, "me" );
		Assert.assertEquals( "null:me", OpenstackIaasHandler.identity( targetProperties ));
	}
}
