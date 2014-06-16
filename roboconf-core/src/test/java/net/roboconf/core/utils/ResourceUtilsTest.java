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

package net.roboconf.core.utils;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import junit.framework.Assert;
import net.roboconf.core.Constants;
import net.roboconf.core.model.runtime.Component;
import net.roboconf.core.model.runtime.Instance;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ResourceUtilsTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();


	@Test
	public void testFindInstanceResourcesDirectory_success() throws Exception {

		final File appDir = this.folder.newFolder();
		final String componentName = "my-component";
		final File expectedFile = new File( appDir, Constants.PROJECT_DIR_GRAPH + File.separator + componentName );

		Assert.assertEquals(
				expectedFile,
				ResourceUtils.findInstanceResourcesDirectory( appDir, componentName ));

		Instance instance = new Instance( "whatever" ).component( new Component( componentName ));
		Assert.assertEquals(
				expectedFile,
				ResourceUtils.findInstanceResourcesDirectory( appDir, instance ));
	}


	@Test
	public void testStoreInstanceResources_inexistingDirectory() throws Exception {

		Instance instance = new Instance( "whatever" ).component( new Component( "comp" ));
		Map<?,?> map = ResourceUtils.storeInstanceResources( new File( "file/does/not/exist" ),  instance );
		Assert.assertEquals( 0, map.size());
	}


	@Test
	public void testStoreInstanceResources_notADirectory() throws Exception {

		final File appDir = this.folder.newFolder();
		final String componentName = "my-component";
		final File componentDirectory = new File( appDir, Constants.PROJECT_DIR_GRAPH + File.separator + componentName );
		if( ! componentDirectory.getParentFile().mkdirs())
			throw new IOException( "Could not create the parent directory." );

		if( ! componentDirectory.createNewFile())
			throw new IOException( "Could not create " + componentDirectory );

		Instance instance = new Instance( "whatever" ).component( new Component( componentName ));
		Map<?,?> map = ResourceUtils.storeInstanceResources( appDir, instance );
		Assert.assertEquals( 0, map.size());
	}


	@Test
	public void testStoreInstanceResources() throws Exception {

		final File appDir = this.folder.newFolder();
		final String componentName = "my-component";
		final File componentDirectory = new File( appDir, Constants.PROJECT_DIR_GRAPH + File.separator + componentName );
		if( ! componentDirectory.mkdirs())
			throw new IOException( "Could not create " + componentDirectory );

		Instance instance = new Instance( "whatever" ).component( new Component( componentName ));
		Map<?,?> map = ResourceUtils.storeInstanceResources( appDir, instance );
		Assert.assertEquals( 0, map.size());
	}
}
