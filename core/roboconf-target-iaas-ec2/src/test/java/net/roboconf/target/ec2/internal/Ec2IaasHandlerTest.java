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

package net.roboconf.target.ec2.internal;

import static net.roboconf.target.ec2.internal.Ec2IaasHandler.DEFAULTS;
import static net.roboconf.target.ec2.internal.Ec2IaasHandler.TPL_VOLUME_APP;
import static net.roboconf.target.ec2.internal.Ec2IaasHandler.TPL_VOLUME_NAME;
import static net.roboconf.target.ec2.internal.Ec2IaasHandler.USE_BLOCK_STORAGE;
import static net.roboconf.target.ec2.internal.Ec2IaasHandler.VOLUME_NAME_PREFIX;
import static net.roboconf.target.ec2.internal.Ec2IaasHandler.expandVolumeName;
import static net.roboconf.target.ec2.internal.Ec2IaasHandler.findStorageIds;
import static net.roboconf.target.ec2.internal.Ec2IaasHandler.findStorageProperty;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import net.roboconf.target.api.TargetException;

/**
 * @author Vincent Zurczak - Linagora
 */
public class Ec2IaasHandlerTest {

	@Test( expected = TargetException.class )
	public void testInvalidConfiguration() throws Exception {
		Ec2IaasHandler.parseProperties( new HashMap<String,String> ());
	}


	@Test
	public void testValidConfiguration() throws Exception {

		Map<String, String> targetProperties = new HashMap<String,String> ();
		targetProperties.put( Ec2Constants.EC2_ENDPOINT, "127.0.0.1" );
		targetProperties.put( Ec2Constants.EC2_ACCESS_KEY, "my access key" );
		targetProperties.put( Ec2Constants.EC2_SECRET_KEY, "my secret key" );
		targetProperties.put( Ec2Constants.AMI_VM_NODE, "the node" );
		targetProperties.put( Ec2Constants.VM_INSTANCE_TYPE, "tiny" );
		targetProperties.put( Ec2Constants.SSH_KEY_NAME, "secret_key" );
		targetProperties.put( Ec2Constants.SECURITY_GROUP_NAME, "WorldWideVisible" );

		Ec2IaasHandler.parseProperties( targetProperties );
	}


	@Test
	public void testGetTargetId() {
		Assert.assertEquals( Ec2IaasHandler.TARGET_ID, new Ec2IaasHandler().getTargetId());
	}

	@Test
	public void testFindStorageIds() {

		Map<String,String> targetProperties = new HashMap<>();
		targetProperties.put( USE_BLOCK_STORAGE, "" );
		Assert.assertEquals( Arrays.asList(), findStorageIds( targetProperties ));

		targetProperties.put( USE_BLOCK_STORAGE, "a" );
		Assert.assertEquals( Arrays.asList( "a" ), findStorageIds( targetProperties ));

		targetProperties.put( USE_BLOCK_STORAGE, "a,b" );
		Assert.assertEquals( Arrays.asList( "a", "b" ), findStorageIds( targetProperties ));

		targetProperties.put( USE_BLOCK_STORAGE, "a,, b , c " );
		Assert.assertEquals( Arrays.asList( "a", "b", "c" ), findStorageIds( targetProperties ));
	}


	@Test
	public void testFindStorageProperty() {

		Map<String,String> targetProperties = new HashMap<>();
		String s = findStorageProperty( targetProperties, "a", VOLUME_NAME_PREFIX );
		Assert.assertEquals( DEFAULTS.get( VOLUME_NAME_PREFIX ), s );
		Assert.assertNotNull( DEFAULTS.get( VOLUME_NAME_PREFIX ));

		targetProperties.put( VOLUME_NAME_PREFIX + "a", "" );
		s = findStorageProperty( targetProperties, "a", VOLUME_NAME_PREFIX );
		Assert.assertEquals( DEFAULTS.get( VOLUME_NAME_PREFIX ), s );

		targetProperties.put( VOLUME_NAME_PREFIX + "a", "name" );
		s = findStorageProperty( targetProperties, "a", VOLUME_NAME_PREFIX );
		Assert.assertEquals( "name", s );

		targetProperties.put( VOLUME_NAME_PREFIX + "a", "name with space after   " );
		s = findStorageProperty( targetProperties, "a", VOLUME_NAME_PREFIX );
		Assert.assertEquals( "name with space after", s );
	}

	@Test
	public void testExpandVolumeName() {

		String name = "roboconf-" + TPL_VOLUME_APP + "-" + TPL_VOLUME_NAME;
		Assert.assertEquals("roboconf-app-inst", expandVolumeName(name, "app", "inst"));

		name = "cache-" + TPL_VOLUME_NAME;
		Assert.assertEquals("cache-inst4", expandVolumeName(name, "app2", "inst4"));

		name = "pre-" + TPL_VOLUME_APP + "-post";
		Assert.assertEquals("pre-app2-post", expandVolumeName(name, "app2", "inst"));

		name = "pre-" + TPL_VOLUME_APP + "-post 2";
		Assert.assertEquals("pre-app-51--post-2", expandVolumeName(name, "app 51 ", "vm 1"));
	}
}
