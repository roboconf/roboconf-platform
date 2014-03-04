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
import java.io.OutputStream;
import java.util.Properties;

import junit.framework.Assert;
import net.roboconf.core.Constants;
import net.roboconf.core.internal.utils.Utils;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.iaas.api.IaasInterface;
import net.roboconf.iaas.ec2.IaasEc2;
import net.roboconf.iaas.local.IaasLocalhost;

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class IaasHelpersTest {

	@Test
	public void testFindIaasHandler() {

		Properties props = new Properties();
		Assert.assertNull( IaasHelpers.findIaasHandler( props ));

		props.setProperty( IaasHelpers.IAAS_TYPE, "local" );
		Assert.assertTrue( IaasHelpers.findIaasHandler( props ) instanceof IaasLocalhost );

		props.setProperty( IaasHelpers.IAAS_TYPE, "ec2" );
		Assert.assertTrue( IaasHelpers.findIaasHandler( props ) instanceof IaasEc2 );
	}


	@Test( expected = IOException.class )
	public void testLoadIaasProperties_inexistingFile() throws Exception {

		File applicationDirectory = new File( System.getProperty( "java.io.tmpdir" ), "roboconf_test" );
		if( applicationDirectory.exists())
			Utils.deleteFilesRecursively( applicationDirectory );

		Assert.assertFalse( applicationDirectory.exists());
		IaasHelpers.loadIaasProperties( applicationDirectory, new Instance( "my-vm" ));
	}


	@Test
	public void testLoadIaasProperties_success() throws Exception {

		final String instanceName = "my-vm";

		File applicationDirectory = new File( System.getProperty( "java.io.tmpdir" ), "roboconf_test" );
		if( applicationDirectory.exists())
			Utils.deleteFilesRecursively( applicationDirectory );

		File f = new File( applicationDirectory, Constants.PROJECT_DIR_GRAPH );
		f = new File( f, instanceName );
		f = new File( f, IaasInterface.DEFAULT_IAAS_PROPERTIES_FILE_NAME );

		OutputStream fos = null;
		try {
			if( ! f.getParentFile().mkdirs())
				throw new IOException( "Failed to create the temporary properties." );

			fos = new FileOutputStream( f );
			Properties props = new Properties();
			props.setProperty( "my-key", "my value" );
			props.store( fos, null );

		} catch( Exception e ) {
			Utils.deleteFilesRecursively( applicationDirectory );

		} finally {
			Utils.closeQuietly( fos );
		}

		try {
			Properties loadedProperties = IaasHelpers.loadIaasProperties( applicationDirectory, new Instance( instanceName ));
			Assert.assertNotNull( loadedProperties );
			Assert.assertEquals( "my value", loadedProperties.getProperty( "my-key" ));

		} finally {
			Utils.deleteFilesRecursively( applicationDirectory );
		}
	}
}
