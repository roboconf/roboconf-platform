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

package net.roboconf.dm.internal.environment.iaas;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import junit.framework.Assert;
import net.roboconf.core.Constants;
import net.roboconf.core.model.runtime.Application;
import net.roboconf.core.model.runtime.Component;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.core.utils.ResourceUtils;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.internal.environment.iaas.IaasResolver;
import net.roboconf.dm.internal.management.ManagedApplication;
import net.roboconf.iaas.api.IaasException;
import net.roboconf.iaas.api.IaasInterface;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author Vincent Zurczak - Linagora
 */
public class IaasResolverTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();


	@Test
	public void testFindIaasInterface_success() throws Exception {

		// Create a IaaS properties file
		File appDir = this.folder.newFolder( "roboconf_test" );
		Application app = new Application( "my app" );
		ManagedApplication ma = new ManagedApplication( app, appDir );

		Instance rootInstance = new Instance( "root" ).component( new Component( "comp" ).installerName( "iaas" ));
		File propsDir = ResourceUtils.findInstanceResourcesDirectory( appDir, rootInstance );
		if( ! propsDir.mkdirs())
			throw new IOException( "Failed to create sub-directories." );

		File propsFile = new File( propsDir, Constants.IAAS_PROPERTIES_FILE_NAME );
		Properties props = new Properties();
		props.setProperty( IaasResolver.IAAS_TYPE, "test" );

		FileOutputStream os = null;
		try {
			os = new FileOutputStream( propsFile );
			props.store( os, null );

		} finally {
			Utils.closeQuietly( os );
		}

		// Create a virtual set of IaaS interfaces
		IaasInterface[] iaas = new IaasInterface[ 2 ];
		iaas[ 0 ] = new MyIaasInterface( "toto" );
		iaas[ 1 ] = new MyIaasInterface( "test" );

		IaasResolver resolver = new IaasResolver();
		IaasInterface itf = resolver.findIaasInterface( iaas, ma, rootInstance );
		Assert.assertNotNull( itf );
	}


	@Test( expected = IaasException.class )
	public void testFindIaasInterface_invalidInstallerName() throws Exception {

		File appDir = this.folder.newFolder( "roboconf_test" );
		Application app = new Application( "my app" );
		ManagedApplication ma = new ManagedApplication( app, appDir );

		Instance rootInstance = new Instance( "root" ).component( new Component( "comp" ).installerName( "not iaas" ));
		File propsDir = ResourceUtils.findInstanceResourcesDirectory( appDir, rootInstance );
		if( ! propsDir.mkdirs())
			throw new IOException( "Failed to create sub-directories." );

		File propsFile = new File( propsDir, Constants.IAAS_PROPERTIES_FILE_NAME );
		Properties props = new Properties();
		props.setProperty( IaasResolver.IAAS_TYPE, "ec2" );

		FileOutputStream os = null;
		try {
			os = new FileOutputStream( propsFile );
			props.store( os, null );

		} finally {
			Utils.closeQuietly( os );
		}

		IaasResolver resolver = new IaasResolver();
		resolver.findIaasInterface( new IaasInterface[ 0 ], ma, rootInstance );
	}


	@Test( expected = IaasException.class )
	public void testFindIaasInterface_unknownIaasType_emptyArray() throws Exception {

		File appDir = this.folder.newFolder( "roboconf_test" );
		Application app = new Application( "my app" );
		ManagedApplication ma = new ManagedApplication( app, appDir );

		Instance rootInstance = new Instance( "root" ).component( new Component( "comp" ).installerName( "iaas" ));
		File propsDir = ResourceUtils.findInstanceResourcesDirectory( appDir, rootInstance );
		if( ! propsDir.mkdirs())
			throw new IOException( "Failed to create sub-directories." );

		File propsFile = new File( propsDir, Constants.IAAS_PROPERTIES_FILE_NAME );
		Properties props = new Properties();
		props.setProperty( IaasResolver.IAAS_TYPE, "oops" );

		FileOutputStream os = null;
		try {
			os = new FileOutputStream( propsFile );
			props.store( os, null );

		} finally {
			Utils.closeQuietly( os );
		}

		IaasResolver resolver = new IaasResolver();
		resolver.findIaasInterface( new IaasInterface[ 0 ], ma, rootInstance );
	}


	@Test( expected = IaasException.class )
	public void testFindIaasInterface_unknownIaasType_nullArray() throws Exception {

		File appDir = this.folder.newFolder( "roboconf_test" );
		Application app = new Application( "my app" );
		ManagedApplication ma = new ManagedApplication( app, appDir );

		Instance rootInstance = new Instance( "root" ).component( new Component( "comp" ).installerName( "iaas" ));
		File propsDir = ResourceUtils.findInstanceResourcesDirectory( appDir, rootInstance );
		if( ! propsDir.mkdirs())
			throw new IOException( "Failed to create sub-directories." );

		File propsFile = new File( propsDir, Constants.IAAS_PROPERTIES_FILE_NAME );
		Properties props = new Properties();
		props.setProperty( IaasResolver.IAAS_TYPE, "oops" );

		FileOutputStream os = null;
		try {
			os = new FileOutputStream( propsFile );
			props.store( os, null );

		} finally {
			Utils.closeQuietly( os );
		}

		IaasResolver resolver = new IaasResolver();
		resolver.findIaasInterface( null, ma, rootInstance );
	}


	@Test( expected = IaasException.class )
	public void testFindIaasInterface_noIaasProperties() throws Exception {

		File appDir = this.folder.newFolder( "roboconf_test" );
		Application app = new Application( "my app" );
		ManagedApplication ma = new ManagedApplication( app, appDir );

		Instance rootInstance = new Instance( "root" ).component( new Component( "comp" ).installerName( "iaas" ));
		IaasResolver resolver = new IaasResolver();
		resolver.findIaasInterface( null, ma, rootInstance );
	}


	/**
	 * @author Vincent Zurczak - Linagora
	 */
	private static final class MyIaasInterface implements IaasInterface {
		private final String installerName;

		MyIaasInterface( String installerName ) {
			this.installerName = installerName;
		}

		@Override
		public void terminateVM( String machineId ) throws IaasException {
			// nothing
		}

		@Override
		public void setIaasProperties( Map<String,String> iaasProperties ) throws IaasException {
			// nothing
		}

		@Override
		public String getIaasType() {
			return this.installerName;
		}

		@Override
		public String createVM( String messagingIp, String messagingUsername, String messagingPassword, String rootInstanceName, String applicationName )
		throws IaasException {
			return "whatever";
		}
	}
}
