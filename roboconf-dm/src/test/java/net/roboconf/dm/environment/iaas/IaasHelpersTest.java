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
import java.util.Map;
import java.util.Properties;

import junit.framework.Assert;
import net.roboconf.core.internal.utils.Utils;
import net.roboconf.core.model.runtime.Component;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.dm.utils.ResourceUtils;
import net.roboconf.iaas.api.IaasInterface;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author Vincent Zurczak - Linagora
 */
public class IaasHelpersTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();


	@Test( expected = IOException.class )
	public void testLoadIaasProperties_inexistingFile() throws Exception {

		File applicationDirectory = new File( System.getProperty( "java.io.tmpdir" ), "roboconf_test" );
		if( applicationDirectory.exists())
			Utils.deleteFilesRecursively( applicationDirectory );

		Assert.assertFalse( applicationDirectory.exists());
		Instance instance = new Instance( "my-vm" ).component( new Component( "my-component" ));
		IaasHelpers.loadIaasProperties( applicationDirectory, instance );
	}


	@Test
	public void testLoadIaasProperties_success() throws Exception {

		final String componentName = "my-vm";

		File applicationDirectory = this.folder.newFolder( "roboconf_test" );
		if( applicationDirectory.exists())
			Utils.deleteFilesRecursively( applicationDirectory );

		File f = ResourceUtils.findInstanceResourcesDirectory( applicationDirectory, componentName );
		f = new File( f, IaasInterface.DEFAULT_IAAS_PROPERTIES_FILE_NAME );

		OutputStream fos = null;
		try {
			if( ! f.getParentFile().mkdirs())
				throw new IOException( "Failed to create the temporary properties." );

			fos = new FileOutputStream( f );
			Properties props = new Properties();
			props.setProperty( "my-key", "my value" );
			props.store( fos, null );

		} finally {
			Utils.closeQuietly( fos );
		}

		Instance instance = new Instance( "my-vm-instance" ).component( new Component( componentName ));
		Map<String, String> loadedProperties = IaasHelpers.loadIaasProperties( applicationDirectory, instance );
		Assert.assertNotNull( loadedProperties );
		Assert.assertEquals( "my value", loadedProperties.get("my-key"));
	}
}
