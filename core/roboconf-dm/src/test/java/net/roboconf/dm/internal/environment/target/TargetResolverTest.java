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

package net.roboconf.dm.internal.environment.target;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import junit.framework.Assert;
import net.roboconf.core.Constants;
import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.core.model.beans.Component;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.utils.ResourceUtils;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.internal.test.TargetHandlerMock;
import net.roboconf.dm.management.ITargetResolver.Target;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.target.api.TargetException;
import net.roboconf.target.api.TargetHandler;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author Vincent Zurczak - Linagora
 */
public class TargetResolverTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();


	@Test
	public void testFindIaasInterface_success() throws Exception {

		// Create a target properties file
		File appDir = this.folder.newFolder( "roboconf_test" );
		Application app = new Application( "my app", new ApplicationTemplate()).directory( appDir );
		ManagedApplication ma = new ManagedApplication( app );

		Instance rootInstance = new Instance( "root" ).component( new Component( "comp" ).installerName( "target" ));
		File propsDir = ResourceUtils.findInstanceResourcesDirectory( appDir, rootInstance );
		Assert.assertTrue( propsDir.mkdirs());

		File propsFile = new File( propsDir, Constants.TARGET_PROPERTIES_FILE_NAME );
		Properties props = new Properties();
		props.setProperty( TargetResolver.TARGET_ID, "test" );

		FileOutputStream os = null;
		try {
			os = new FileOutputStream( propsFile );
			props.store( os, null );

		} finally {
			Utils.closeQuietly( os );
		}

		// Create a virtual set of target handlers
		List<TargetHandler> targetHandlers = new ArrayList<TargetHandler> ();
		targetHandlers.add( new TargetHandlerMock( "toto" ));
		targetHandlers.add( new TargetHandlerMock( "test" ));

		TargetResolver resolver = new TargetResolver();
		Target target = resolver.findTargetHandler( targetHandlers, ma, rootInstance );
		Assert.assertNotNull( target );
	}


	@Test( expected = TargetException.class )
	public void testFindTargetHandler_invalidInstallerName() throws Exception {

		File appDir = this.folder.newFolder( "roboconf_test" );
		Application app = new Application( "my app", new ApplicationTemplate()).directory( appDir );
		ManagedApplication ma = new ManagedApplication( app );

		Instance rootInstance = new Instance( "root" ).component( new Component( "comp" ).installerName( "target" ));
		File propsDir = ResourceUtils.findInstanceResourcesDirectory( appDir, rootInstance );
		Assert.assertTrue( propsDir.mkdirs());

		File propsFile = new File( propsDir, Constants.TARGET_PROPERTIES_FILE_NAME );
		Properties props = new Properties();
		props.setProperty( TargetResolver.TARGET_ID, "ec2" );

		FileOutputStream os = null;
		try {
			os = new FileOutputStream( propsFile );
			props.store( os, null );

		} finally {
			Utils.closeQuietly( os );
		}

		TargetResolver resolver = new TargetResolver();
		resolver.findTargetHandler( new ArrayList<TargetHandler>( 0 ), ma, rootInstance );
	}


	@Test( expected = TargetException.class )
	public void testFindTargetHandler_unknownTargetId_emptyArray() throws Exception {

		File appDir = this.folder.newFolder( "roboconf_test" );
		Application app = new Application( "my app", new ApplicationTemplate()).directory( appDir );
		ManagedApplication ma = new ManagedApplication( app );

		Instance rootInstance = new Instance( "root" ).component( new Component( "comp" ).installerName( "target" ));
		File propsDir = ResourceUtils.findInstanceResourcesDirectory( appDir, rootInstance );
		Assert.assertTrue( propsDir.mkdirs());

		File propsFile = new File( propsDir, Constants.TARGET_PROPERTIES_FILE_NAME );
		Properties props = new Properties();
		props.setProperty( TargetResolver.TARGET_ID, "oops" );

		FileOutputStream os = null;
		try {
			os = new FileOutputStream( propsFile );
			props.store( os, null );

		} finally {
			Utils.closeQuietly( os );
		}

		TargetResolver resolver = new TargetResolver();
		resolver.findTargetHandler( new ArrayList<TargetHandler>( 0 ), ma, rootInstance );
	}


	@Test( expected = TargetException.class )
	public void testFindTargetHandler_unknownTargetId_nullArray() throws Exception {

		File appDir = this.folder.newFolder( "roboconf_test" );
		Application app = new Application( "my app", new ApplicationTemplate()).directory( appDir );
		ManagedApplication ma = new ManagedApplication( app );

		Instance rootInstance = new Instance( "root" ).component( new Component( "comp" ).installerName( "target" ));
		File propsDir = ResourceUtils.findInstanceResourcesDirectory( appDir, rootInstance );
		Assert.assertTrue( propsDir.mkdirs());

		File propsFile = new File( propsDir, Constants.TARGET_PROPERTIES_FILE_NAME );
		Properties props = new Properties();
		props.setProperty( TargetResolver.TARGET_ID, "oops" );

		FileOutputStream os = null;
		try {
			os = new FileOutputStream( propsFile );
			props.store( os, null );

		} finally {
			Utils.closeQuietly( os );
		}

		TargetResolver resolver = new TargetResolver();
		resolver.findTargetHandler( null, ma, rootInstance );
	}


	@Test( expected = TargetException.class )
	public void testFindTargetHandler_noTargetIdInProperties() throws Exception {

		File appDir = this.folder.newFolder( "roboconf_test" );
		Application app = new Application( "my app", new ApplicationTemplate()).directory( appDir );
		ManagedApplication ma = new ManagedApplication( app );

		Instance rootInstance = new Instance( "root" ).component( new Component( "comp" ).installerName( "target" ));
		File propsDir = ResourceUtils.findInstanceResourcesDirectory( appDir, rootInstance );
		Assert.assertTrue( propsDir.mkdirs());

		File propsFile = new File( propsDir, Constants.TARGET_PROPERTIES_FILE_NAME );
		Utils.writeStringInto( "", propsFile );

		TargetResolver resolver = new TargetResolver();
		resolver.findTargetHandler( null, ma, rootInstance );
	}


	@Test( expected = TargetException.class )
	public void testFindTargetHandler_noTargetProperties() throws Exception {

		File appDir = this.folder.newFolder( "roboconf_test" );
		Application app = new Application( "my app", new ApplicationTemplate()).directory( appDir );
		ManagedApplication ma = new ManagedApplication( app );

		Instance rootInstance = new Instance( "root" ).component( new Component( "comp" ).installerName( "target" ));
		TargetResolver resolver = new TargetResolver();
		resolver.findTargetHandler( new ArrayList<TargetHandler>( 0 ), ma, rootInstance );
	}


	@Test( expected = TargetException.class )
	public void testFindTargetHandler_invalidInstaller() throws Exception {

		File appDir = this.folder.newFolder( "roboconf_test" );
		Application app = new Application( "my app", new ApplicationTemplate()).directory( appDir );
		ManagedApplication ma = new ManagedApplication( app );

		Instance rootInstance = new Instance( "root" ).component( new Component( "comp" ).installerName( "not target" ));
		TargetResolver resolver = new TargetResolver();
		resolver.findTargetHandler( new ArrayList<TargetHandler>( 0 ), ma, rootInstance );
	}
}
