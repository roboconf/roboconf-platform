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

package net.roboconf.dm.utils;

import java.io.File;
import java.io.IOException;
import java.util.List;

import junit.framework.Assert;
import net.roboconf.core.Constants;
import net.roboconf.core.internal.utils.Utils;
import net.roboconf.core.model.runtime.Component;
import net.roboconf.core.model.runtime.Instance;

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ResourceUtilsTest {

	@Test
	public void testFindInstanceResourcesDirectory() {

		final File appDir = new File( System.getProperty( "java.io.tmpdir" ));
		final String componentName = "my-component";
		final File expectedFile = new File( appDir, Constants.PROJECT_DIR_GRAPH + File.separator + componentName );

		Assert.assertEquals(
				expectedFile,
				ResourceUtils.findInstanceResourcesDirectory( appDir, componentName ));

		Instance instance = new Instance( "whatever" );
		instance.setComponent( new Component( componentName ));
		Assert.assertEquals(
				expectedFile,
				ResourceUtils.findInstanceResourcesDirectory( appDir, instance ));
	}


	@Test( expected = IllegalArgumentException.class )
	public void testComputeFileRelativeLocation_failure() {

		final File rootDir = new File( System.getProperty( "java.io.tmpdir" ));
		ResourceUtils.computeFileRelativeLocation( rootDir, new File( "invalid-path" ));
	}


	@Test
	public void testComputeFileRelativeLocation_success() {

		final File rootDir = new File( System.getProperty( "java.io.tmpdir" ));
		File directChildFile = new File( rootDir, "woo.txt" );
		Assert.assertEquals(
				directChildFile.getName(),
				ResourceUtils.computeFileRelativeLocation( rootDir, directChildFile ));

		String indirectChildPath = "dir1/dir2/script.sh";
		File indirectChildFile = new File( rootDir, indirectChildPath );
		Assert.assertEquals(
				indirectChildPath,
				ResourceUtils.computeFileRelativeLocation( rootDir, indirectChildFile ));
	}


	@Test
	public void testListAllFiles() throws Exception {

		final File tempDir = new File( System.getProperty( "java.io.tmpdir" ), "roboconf_test" );
		if( tempDir.exists())
			Utils.deleteFilesRecursively( tempDir );

		if( ! tempDir.mkdir())
			throw new IOException( "Failed to create a tempoary directory." );

		try {
			String[] paths = new String[] { "dir1", "dir2", "dir1/dir3" };
			for( String path : paths ) {
				if( ! new File( tempDir, path ).mkdir())
					throw new IOException( "Failed to create " + path );
			}

			paths = new String[] { "dir1/toto.txt", "dir2/script.sh", "dir1/dir3/smart.png" };
			for( String path : paths ) {
				if( ! new File( tempDir, path ).createNewFile())
					throw new IOException( "Failed to create " + path );
			}

			List<File> files = ResourceUtils.listAllFiles( tempDir );
			Assert.assertEquals( 3, files.size());
			for( String path : paths )
				Assert.assertTrue( path, files.contains( new File( tempDir, path )));

		} finally {
			Utils.deleteFilesRecursively( tempDir );
		}
	}
}
