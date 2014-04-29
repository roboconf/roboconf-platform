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
	public void testFindInstanceResourcesDirectory_success() {

		final File appDir = new File( System.getProperty( "java.io.tmpdir" ));
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


	@Test( expected = IllegalArgumentException.class )
	public void testStoreInstanceResources_inexistingDirectory() throws Exception {

		Instance instance = new Instance( "whatever" ).component( new Component( "comp" ));
		ResourceUtils.storeInstanceResources( new File( "/file/does/not/exist" ),  instance );
	}


	@Test( expected = IllegalArgumentException.class )
	public void testStoreInstanceResources_notADirectory() throws Exception {

		Instance instance = new Instance( "whatever" ).component( new Component( "comp" ));
		File f = this.folder.newFile( "roboconf_.txt" );
		ResourceUtils.storeInstanceResources( f, instance );
	}
}
