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

package net.roboconf.dm.internal.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import net.roboconf.core.Constants;
import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.model.beans.AbstractApplication;
import net.roboconf.core.model.beans.Component;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.dm.internal.api.impl.TargetHandlerResolverImpl;
import net.roboconf.dm.internal.api.impl.beans.TargetPropertiesImpl;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.api.ITargetsMngr;

/**
 * @author Vincent Zurczak - Linagora
 */
public class TargetHelpersTest {

	@Test
	public void testExpandProperties_scopedInstanceIsNotRoot() throws Exception {

		Map<String,String> map = new HashMap<> ();
		map.put( "test.ip", "{{ip}}:4243" );

		final Component component = new Component( "my-vm" ).installerName( Constants.TARGET_INSTALLER );
		Instance instance = new Instance( "my-scope-instance" ).component( component );
		Instance rootInstance = new Instance( "root" );
		InstanceHelpers.insertChild( rootInstance, instance );
		rootInstance.data.put( Instance.IP_ADDRESS, "192.168.1.87" );

		Map<String,String> loadedProperties = TargetHelpers.expandProperties( instance, map );
		Assert.assertNotNull( loadedProperties );
		Assert.assertEquals( "192.168.1.87:4243", loadedProperties.get("test.ip"));
	}


	@Test
	public void testExpandProperties_scopedInstanceIsRoot() throws Exception {

		Map<String,String> map = new HashMap<> ();
		map.put( "test.ip", "{{ip}}:4243" );

		final Component component = new Component( "my-vm" ).installerName( Constants.TARGET_INSTALLER );
		Instance instance = new Instance( "my-scope-instance" ).component( component );
		instance.data.put( Instance.IP_ADDRESS, "192.168.1.84" );

		Map<String,String> loadedProperties = TargetHelpers.expandProperties( instance, map );
		Assert.assertNotNull( loadedProperties );
		Assert.assertEquals( "192.168.1.84:4243", loadedProperties.get("test.ip"));
	}


	@Test
	public void testExpandProperties_noIp() throws Exception {

		Map<String,String> map = new HashMap<> ();
		map.put( "test.ip", "{{ip}}:4243" );

		final Component component = new Component( "my-vm" ).installerName( Constants.TARGET_INSTALLER );
		Instance instance = new Instance( "my-scope-instance" ).component( component );

		Map<String,String> loadedProperties = TargetHelpers.expandProperties( instance, map );
		Assert.assertNotNull( loadedProperties );
		Assert.assertEquals( ":4243", loadedProperties.get("test.ip"));
	}


	@Test
	public void testVerifyTargets_targetException() throws Exception {

		ManagedApplication ma = new ManagedApplication( new TestApplication());
		ITargetsMngr targetsMngr = Mockito.mock( ITargetsMngr.class );
		Mockito.when(
				targetsMngr.findTargetProperties(
						Mockito.any( AbstractApplication.class ),
						Mockito.any( String.class )))
				.thenReturn( new TargetPropertiesImpl());

		TargetHelpers.verifyTargets( new TargetHandlerResolverImpl(), ma, targetsMngr );
	}


	@Test
	public void testFindTargetHandlerName_properties() {

		Properties props = new Properties();
		Assert.assertNull( TargetHelpers.findTargetHandlerName( props ));

		props.put( Constants.TARGET_PROPERTY_HANDLER, "whatever" );
		Assert.assertEquals( "whatever", TargetHelpers.findTargetHandlerName( props ));

		props.remove( Constants.TARGET_PROPERTY_HANDLER );
		Assert.assertNull( TargetHelpers.findTargetHandlerName( props ));
	}


	@Test
	public void testFindTargetHandlerName_map() {

		Map<String,String> props = new HashMap<>( 2 );
		Assert.assertNull( TargetHelpers.findTargetHandlerName( props ));

		props.put( Constants.TARGET_PROPERTY_HANDLER, "whatever" );
		Assert.assertEquals( "whatever", TargetHelpers.findTargetHandlerName( props ));

		props.remove( Constants.TARGET_PROPERTY_HANDLER );
		Assert.assertNull( TargetHelpers.findTargetHandlerName( props ));
	}
}
