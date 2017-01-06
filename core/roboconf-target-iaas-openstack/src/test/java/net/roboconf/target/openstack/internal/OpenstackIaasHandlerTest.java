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

package net.roboconf.target.openstack.internal;

import static net.roboconf.target.openstack.internal.OpenstackIaasHandler.API_URL;
import static net.roboconf.target.openstack.internal.OpenstackIaasHandler.DEFAULTS;
import static net.roboconf.target.openstack.internal.OpenstackIaasHandler.FLAVOR_NAME;
import static net.roboconf.target.openstack.internal.OpenstackIaasHandler.FLOATING_IP_POOL;
import static net.roboconf.target.openstack.internal.OpenstackIaasHandler.IMAGE_NAME;
import static net.roboconf.target.openstack.internal.OpenstackIaasHandler.KEY_PAIR;
import static net.roboconf.target.openstack.internal.OpenstackIaasHandler.NETWORK_ID;
import static net.roboconf.target.openstack.internal.OpenstackIaasHandler.PASSWORD;
import static net.roboconf.target.openstack.internal.OpenstackIaasHandler.SECURITY_GROUP;
import static net.roboconf.target.openstack.internal.OpenstackIaasHandler.TARGET_ID;
import static net.roboconf.target.openstack.internal.OpenstackIaasHandler.TENANT_NAME;
import static net.roboconf.target.openstack.internal.OpenstackIaasHandler.TPL_VOLUME_APP;
import static net.roboconf.target.openstack.internal.OpenstackIaasHandler.TPL_VOLUME_NAME;
import static net.roboconf.target.openstack.internal.OpenstackIaasHandler.USER;
import static net.roboconf.target.openstack.internal.OpenstackIaasHandler.USE_BLOCK_STORAGE;
import static net.roboconf.target.openstack.internal.OpenstackIaasHandler.VOLUME_DELETE_OT_PREFIX;
import static net.roboconf.target.openstack.internal.OpenstackIaasHandler.VOLUME_MOUNT_POINT_PREFIX;
import static net.roboconf.target.openstack.internal.OpenstackIaasHandler.VOLUME_NAME_PREFIX;
import static net.roboconf.target.openstack.internal.OpenstackIaasHandler.VOLUME_SIZE_GB_PREFIX;
import static net.roboconf.target.openstack.internal.OpenstackIaasHandler.expandVolumeName;
import static net.roboconf.target.openstack.internal.OpenstackIaasHandler.findStorageIds;
import static net.roboconf.target.openstack.internal.OpenstackIaasHandler.findStorageProperty;
import static net.roboconf.target.openstack.internal.OpenstackIaasHandler.identity;
import static net.roboconf.target.openstack.internal.OpenstackIaasHandler.validate;
import static net.roboconf.target.openstack.internal.OpenstackIaasHandler.validateAll;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.jclouds.openstack.nova.v2_0.domain.FloatingIP;
import org.junit.Assert;
import org.junit.Test;

import net.roboconf.target.api.TargetException;
import net.roboconf.target.openstack.internal.OpenstackIaasHandler.InstancePredicate;

/**
 * @author Pierre-Yves Gibello - Linagora
 */
public class OpenstackIaasHandlerTest {

	@Test
	public void testGetTargetId() {
		Assert.assertEquals( TARGET_ID, new OpenstackIaasHandler().getTargetId());
	}


	@Test
	public void testValidate() throws Exception {

		Map<String,String> targetProperties = new HashMap<>();
		targetProperties.put( FLAVOR_NAME, UUID.randomUUID().toString());
		targetProperties.put( TENANT_NAME, UUID.randomUUID().toString());
		targetProperties.put( IMAGE_NAME, UUID.randomUUID().toString());
		targetProperties.put( SECURITY_GROUP, UUID.randomUUID().toString());
		targetProperties.put( USER, UUID.randomUUID().toString());
		targetProperties.put( PASSWORD, UUID.randomUUID().toString());
		targetProperties.put( KEY_PAIR, UUID.randomUUID().toString());
		targetProperties.put( API_URL, UUID.randomUUID().toString());
		validate( targetProperties );

		targetProperties.put( FLOATING_IP_POOL, UUID.randomUUID().toString());
		targetProperties.put( NETWORK_ID, UUID.randomUUID().toString());
		validate( targetProperties );
	}


	@Test( expected = TargetException.class )
	public void testValidate_error1() throws Exception {

		// Tenant name is missing
		Map<String,String> targetProperties = new HashMap<>();
		targetProperties.put( FLAVOR_NAME, UUID.randomUUID().toString());
		targetProperties.put( IMAGE_NAME, UUID.randomUUID().toString());
		targetProperties.put( SECURITY_GROUP, UUID.randomUUID().toString());
		targetProperties.put( USER, UUID.randomUUID().toString());
		targetProperties.put( PASSWORD, UUID.randomUUID().toString());
		targetProperties.put( KEY_PAIR, UUID.randomUUID().toString());
		targetProperties.put( API_URL, UUID.randomUUID().toString());
		validate( targetProperties );
	}


	@Test( expected = TargetException.class )
	public void testValidate_error2() throws Exception {

		validate( new HashMap<String,String>( 0 ));
	}


	@Test( expected = TargetException.class )
	public void testValidate_error3() throws Exception {

		// The flavor name is empty
		Map<String,String> targetProperties = new HashMap<>();
		targetProperties.put( FLAVOR_NAME, "" );
		targetProperties.put( TENANT_NAME, UUID.randomUUID().toString());
		targetProperties.put( IMAGE_NAME, UUID.randomUUID().toString());
		targetProperties.put( SECURITY_GROUP, UUID.randomUUID().toString());
		targetProperties.put( USER, UUID.randomUUID().toString());
		targetProperties.put( PASSWORD, UUID.randomUUID().toString());
		targetProperties.put( KEY_PAIR, UUID.randomUUID().toString());
		targetProperties.put( API_URL, UUID.randomUUID().toString());
		validate( targetProperties );
	}


	@Test
	public void testIdentity() {

		Map<String,String> targetProperties = new HashMap<>();
		targetProperties.put( TENANT_NAME, "tenant" );
		targetProperties.put( USER, "me" );
		Assert.assertEquals( "tenant:me", identity( targetProperties ));

		targetProperties.remove( USER );
		Assert.assertEquals( "tenant:null", identity( targetProperties ));

		targetProperties.remove( TENANT_NAME );
		Assert.assertEquals( "null:null", identity( targetProperties ));

		targetProperties.put( USER, "me" );
		Assert.assertEquals( "null:me", identity( targetProperties ));
	}


	@Test
	public void testValidateAll_success() throws Exception {

		Map<String,String> targetProperties = new HashMap<>();
		targetProperties.put( FLAVOR_NAME, UUID.randomUUID().toString());
		targetProperties.put( TENANT_NAME, UUID.randomUUID().toString());
		targetProperties.put( IMAGE_NAME, UUID.randomUUID().toString());
		targetProperties.put( SECURITY_GROUP, UUID.randomUUID().toString());
		targetProperties.put( USER, UUID.randomUUID().toString());
		targetProperties.put( PASSWORD, UUID.randomUUID().toString());
		targetProperties.put( KEY_PAIR, UUID.randomUUID().toString());
		targetProperties.put( API_URL, UUID.randomUUID().toString());
		validateAll( targetProperties, null, null );

		targetProperties.put( USE_BLOCK_STORAGE, "a, b, ,," );
		targetProperties.put( VOLUME_NAME_PREFIX + "a", "name" );
		targetProperties.put( VOLUME_SIZE_GB_PREFIX + "a", "20" );
		targetProperties.put( VOLUME_MOUNT_POINT_PREFIX + "a", "/dev/sdf" );
		validateAll( targetProperties, "app", "inst" );
	}


	@Test( expected = TargetException.class )
	public void testValidateAll_duplicateVolumeName_allUseDefaults() throws Exception {

		Map<String,String> targetProperties = new HashMap<>();
		targetProperties.put( FLAVOR_NAME, UUID.randomUUID().toString());
		targetProperties.put( TENANT_NAME, UUID.randomUUID().toString());
		targetProperties.put( IMAGE_NAME, UUID.randomUUID().toString());
		targetProperties.put( SECURITY_GROUP, UUID.randomUUID().toString());
		targetProperties.put( USER, UUID.randomUUID().toString());
		targetProperties.put( PASSWORD, UUID.randomUUID().toString());
		targetProperties.put( KEY_PAIR, UUID.randomUUID().toString());
		targetProperties.put( API_URL, UUID.randomUUID().toString());

		targetProperties.put( USE_BLOCK_STORAGE, "a, b, ,," );
		targetProperties.put( VOLUME_MOUNT_POINT_PREFIX + "a", "/dev/sdc" );
		targetProperties.put( VOLUME_MOUNT_POINT_PREFIX + "b", "/dev/sdd" );
		validateAll( targetProperties, "app", "inst" );
	}


	@Test( expected = TargetException.class )
	public void testValidateAll_duplicateVolumeName_emptyNameShouldResultInDefault() throws Exception {

		Map<String,String> targetProperties = new HashMap<>();
		targetProperties.put( FLAVOR_NAME, UUID.randomUUID().toString());
		targetProperties.put( TENANT_NAME, UUID.randomUUID().toString());
		targetProperties.put( IMAGE_NAME, UUID.randomUUID().toString());
		targetProperties.put( SECURITY_GROUP, UUID.randomUUID().toString());
		targetProperties.put( USER, UUID.randomUUID().toString());
		targetProperties.put( PASSWORD, UUID.randomUUID().toString());
		targetProperties.put( KEY_PAIR, UUID.randomUUID().toString());
		targetProperties.put( API_URL, UUID.randomUUID().toString());

		targetProperties.put( USE_BLOCK_STORAGE, "a, b, ,," );
		targetProperties.put( VOLUME_NAME_PREFIX + "a", "" );
		targetProperties.put( VOLUME_MOUNT_POINT_PREFIX + "a", "/dev/sdc" );
		targetProperties.put( VOLUME_MOUNT_POINT_PREFIX + "b", "/dev/sdd" );
		validateAll( targetProperties, "app", "inst" );
	}


	@Test( expected = TargetException.class )
	public void testValidateAll_duplicateMountPoint() throws Exception {

		Map<String,String> targetProperties = new HashMap<>();
		targetProperties.put( FLAVOR_NAME, UUID.randomUUID().toString());
		targetProperties.put( TENANT_NAME, UUID.randomUUID().toString());
		targetProperties.put( IMAGE_NAME, UUID.randomUUID().toString());
		targetProperties.put( SECURITY_GROUP, UUID.randomUUID().toString());
		targetProperties.put( USER, UUID.randomUUID().toString());
		targetProperties.put( PASSWORD, UUID.randomUUID().toString());
		targetProperties.put( KEY_PAIR, UUID.randomUUID().toString());
		targetProperties.put( API_URL, UUID.randomUUID().toString());

		targetProperties.put( USE_BLOCK_STORAGE, "a, b" );
		targetProperties.put( VOLUME_NAME_PREFIX + "a", "vol-a" );
		targetProperties.put( VOLUME_NAME_PREFIX + "b", "vol-b" );
		validateAll( targetProperties, "app", "inst" );
	}


	@Test( expected = TargetException.class )
	public void testValidateAll_invalidVolumeSize() throws Exception {

		Map<String,String> targetProperties = new HashMap<>();
		targetProperties.put( FLAVOR_NAME, UUID.randomUUID().toString());
		targetProperties.put( TENANT_NAME, UUID.randomUUID().toString());
		targetProperties.put( IMAGE_NAME, UUID.randomUUID().toString());
		targetProperties.put( SECURITY_GROUP, UUID.randomUUID().toString());
		targetProperties.put( USER, UUID.randomUUID().toString());
		targetProperties.put( PASSWORD, UUID.randomUUID().toString());
		targetProperties.put( KEY_PAIR, UUID.randomUUID().toString());
		targetProperties.put( API_URL, UUID.randomUUID().toString());

		targetProperties.put( USE_BLOCK_STORAGE, "a, b" );
		targetProperties.put( VOLUME_NAME_PREFIX + "a", "vol-a" );
		targetProperties.put( VOLUME_NAME_PREFIX + "b", "vol-b" );
		targetProperties.put( VOLUME_MOUNT_POINT_PREFIX + "a", "/dev/sdc" );
		targetProperties.put( VOLUME_MOUNT_POINT_PREFIX + "b", "/dev/sdd" );
		targetProperties.put( VOLUME_SIZE_GB_PREFIX + "b", "five" );
		validateAll( targetProperties, "app", "inst" );
	}


	@Test
	public void testValidateAll_success2() throws Exception {

		Map<String,String> targetProperties = new HashMap<>();
		targetProperties.put( FLAVOR_NAME, UUID.randomUUID().toString());
		targetProperties.put( TENANT_NAME, UUID.randomUUID().toString());
		targetProperties.put( IMAGE_NAME, UUID.randomUUID().toString());
		targetProperties.put( SECURITY_GROUP, UUID.randomUUID().toString());
		targetProperties.put( USER, UUID.randomUUID().toString());
		targetProperties.put( PASSWORD, UUID.randomUUID().toString());
		targetProperties.put( KEY_PAIR, UUID.randomUUID().toString());
		targetProperties.put( API_URL, UUID.randomUUID().toString());

		targetProperties.put( USE_BLOCK_STORAGE, "a, b" );
		targetProperties.put( VOLUME_NAME_PREFIX + "a", "vol-a" );
		targetProperties.put( VOLUME_NAME_PREFIX + "b", "vol-b" );
		targetProperties.put( VOLUME_MOUNT_POINT_PREFIX + "a", "/dev/sdc" );
		targetProperties.put( VOLUME_MOUNT_POINT_PREFIX + "b", "/dev/sdd" );
		targetProperties.put( VOLUME_SIZE_GB_PREFIX + "a", "40" );
		targetProperties.put( VOLUME_SIZE_GB_PREFIX + "b", "4" );
		validateAll( targetProperties, "app", "inst" );
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
	public void testDefaults() {

		Assert.assertNotNull( DEFAULTS.get( VOLUME_NAME_PREFIX ));
		Assert.assertNotNull( DEFAULTS.get( VOLUME_MOUNT_POINT_PREFIX ));
		Assert.assertNotNull( DEFAULTS.get( VOLUME_SIZE_GB_PREFIX ));
		Assert.assertNotNull( DEFAULTS.get( VOLUME_DELETE_OT_PREFIX ));
	}


	@Test
	public void testExpandVolumeName() {

		String name = DEFAULTS.get( VOLUME_NAME_PREFIX );
		Assert.assertEquals( "roboconf-app-inst", expandVolumeName( name, "app", "inst" ));

		name = "cache-" + TPL_VOLUME_NAME;
		Assert.assertEquals( "cache-inst4", expandVolumeName( name, "app2", "inst4" ));

		name = "pre-" + TPL_VOLUME_APP + "-post";
		Assert.assertEquals( "pre-app2-post", expandVolumeName( name, "app2", "inst" ));

		name = "pre-" + TPL_VOLUME_APP + "-post 2";
		Assert.assertEquals( "pre-app-51--post-2", expandVolumeName( name, "app 51 ", "vm 1" ));
	}


	@Test
	public void testInstancePredicate() {

		FloatingIP ip = new FloatingIP( "id", "ip", "fixedIp", "mid", "pool" ) {};
		InstancePredicate predicate = new InstancePredicate( "mid" );
		Assert.assertTrue( predicate.apply( ip ));
	}
}
