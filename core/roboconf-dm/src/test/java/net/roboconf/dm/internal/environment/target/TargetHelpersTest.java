/**
 * Copyright 2014 Linagora, Université Joseph Fourier, Floralis
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
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.Properties;

import junit.framework.Assert;
import net.roboconf.core.Constants;
import net.roboconf.core.model.beans.Component;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.utils.ResourceUtils;
import net.roboconf.core.utils.Utils;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author Vincent Zurczak - Linagora
 */
public class TargetHelpersTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();


	@Test( expected = IOException.class )
	public void testLoadTargetProperties_inexistingFile() throws Exception {

		File applicationDirectory = new File( System.getProperty( "java.io.tmpdir" ), "roboconf_test" );
		if( applicationDirectory.exists())
			Utils.deleteFilesRecursively( applicationDirectory );

		Assert.assertFalse( applicationDirectory.exists());
		Instance instance = new Instance( "my-vm" ).component( new Component( "my-component" ));
		TargetHelpers.loadTargetProperties( applicationDirectory, instance );
	}


	@Test
	public void testLoadTargetProperties_success() throws Exception {

		final Component component = new Component( "my-vm" );
		File applicationDirectory = this.folder.newFolder( "roboconf_test" );
		if( applicationDirectory.exists())
			Utils.deleteFilesRecursively( applicationDirectory );

		File f = ResourceUtils.findInstanceResourcesDirectory( applicationDirectory, component );
		f = new File( f, Constants.TARGET_PROPERTIES_FILE_NAME );

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

		Instance instance = new Instance( "my-vm-instance" ).component( component );
		Map<String, String> loadedProperties = TargetHelpers.loadTargetProperties( applicationDirectory, instance );
		Assert.assertNotNull( loadedProperties );
		Assert.assertEquals( "my value", loadedProperties.get("my-key"));
	}
}
