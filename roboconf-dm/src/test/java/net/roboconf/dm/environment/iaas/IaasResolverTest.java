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

package net.roboconf.dm.environment.iaas;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import junit.framework.Assert;
import net.roboconf.core.model.runtime.Application;
import net.roboconf.core.model.runtime.Component;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.core.utils.ResourceUtils;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.iaas.api.IaasException;
import net.roboconf.iaas.api.IaasInterface;
import net.roboconf.iaas.azure.IaasAzure;
import net.roboconf.iaas.ec2.IaasEc2;
import net.roboconf.iaas.embedded.IaasEmbedded;
import net.roboconf.iaas.local.IaasInMemory;
import net.roboconf.iaas.openstack.IaasOpenstack;
import net.roboconf.iaas.vmware.IaasVmware;

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
	public void testFindIaasHandler() {
		IaasResolver resolver = new IaasResolver();

		Map<String, String> props = new HashMap<String, String>();
		Assert.assertNull( resolver.findIaasHandler( props ));

		props.put( IaasResolver.IAAS_TYPE, IaasResolver.IAAS_IN_MEMORY );
		Assert.assertTrue( resolver.findIaasHandler( props ) instanceof IaasInMemory );

		props.put( IaasResolver.IAAS_TYPE, IaasResolver.IAAS_EC2 );
		Assert.assertTrue( resolver.findIaasHandler( props ) instanceof IaasEc2 );

		props.put( IaasResolver.IAAS_TYPE, IaasResolver.IAAS_AZURE );
		Assert.assertTrue( resolver.findIaasHandler( props ) instanceof IaasAzure );

		props.put( IaasResolver.IAAS_TYPE, IaasResolver.IAAS_EMBEDDED );
		Assert.assertTrue( resolver.findIaasHandler( props ) instanceof IaasEmbedded );

		props.put( IaasResolver.IAAS_TYPE, IaasResolver.IAAS_VMWARE );
		Assert.assertTrue( resolver.findIaasHandler( props ) instanceof IaasVmware );

		props.put( IaasResolver.IAAS_TYPE, IaasResolver.IAAS_OPENSTACK );
		Assert.assertTrue( resolver.findIaasHandler( props ) instanceof IaasOpenstack );

		props.put( IaasResolver.IAAS_TYPE, "whatever" );
		Assert.assertNull( resolver.findIaasHandler( props ));
	}


	@Test
	public void testFindIaasInterface_success() throws Exception {

		File appDir = this.folder.newFolder( "roboconf_test" );
		Application app = new Application( "my app" );
		ManagedApplication ma = new ManagedApplication( app, appDir );

		Instance rootInstance = new Instance( "root" ).component( new Component( "comp" ).installerName( "iaas" ));
		File propsDir = ResourceUtils.findInstanceResourcesDirectory( appDir, rootInstance );
		if( ! propsDir.mkdirs())
			throw new IOException( "Failed to create sub-directories." );

		File propsFile = new File( propsDir, IaasInterface.DEFAULT_IAAS_PROPERTIES_FILE_NAME );
		Properties props = new Properties();
		props.setProperty( IaasResolver.IAAS_TYPE, IaasResolver.IAAS_EMBEDDED );
		FileOutputStream os = new FileOutputStream( propsFile );
		props.store( os, null );

		IaasResolver resolver = new IaasResolver();
		IaasInterface itf = resolver.findIaasInterface( ma, rootInstance );
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

		File propsFile = new File( propsDir, IaasInterface.DEFAULT_IAAS_PROPERTIES_FILE_NAME );
		Properties props = new Properties();
		props.setProperty( IaasResolver.IAAS_TYPE, IaasResolver.IAAS_EMBEDDED );
		FileOutputStream os = new FileOutputStream( propsFile );
		props.store( os, null );

		IaasResolver resolver = new IaasResolver();
		resolver.findIaasInterface( ma, rootInstance );
	}


	@Test( expected = IaasException.class )
	public void testFindIaasInterface_unknownIaasType() throws Exception {

		File appDir = this.folder.newFolder( "roboconf_test" );
		Application app = new Application( "my app" );
		ManagedApplication ma = new ManagedApplication( app, appDir );

		Instance rootInstance = new Instance( "root" ).component( new Component( "comp" ).installerName( "iaas" ));
		File propsDir = ResourceUtils.findInstanceResourcesDirectory( appDir, rootInstance );
		if( ! propsDir.mkdirs())
			throw new IOException( "Failed to create sub-directories." );

		File propsFile = new File( propsDir, IaasInterface.DEFAULT_IAAS_PROPERTIES_FILE_NAME );
		Properties props = new Properties();
		props.setProperty( IaasResolver.IAAS_TYPE, "oops" );
		FileOutputStream os = new FileOutputStream( propsFile );
		props.store( os, null );

		IaasResolver resolver = new IaasResolver();
		resolver.findIaasInterface( ma, rootInstance );
	}
}
