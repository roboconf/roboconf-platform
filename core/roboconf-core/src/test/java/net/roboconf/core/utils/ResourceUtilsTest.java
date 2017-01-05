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

package net.roboconf.core.utils;

import java.io.File;
import java.util.Map;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import net.roboconf.core.Constants;
import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.model.beans.Component;
import net.roboconf.core.model.beans.Instance;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ResourceUtilsTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();


	@Test
	public void testFindInstanceResourcesDirectory_success() throws Exception {

		final File appDir = this.folder.newFolder();
		final Component c1 = new Component( "c1" );
		final Component c2 = new Component( "c2" );
		final File expectedFile = new File( appDir, Constants.PROJECT_DIR_GRAPH + File.separator + c1.getName());

		Assert.assertEquals(
				expectedFile,
				ResourceUtils.findInstanceResourcesDirectory( appDir, c1 ));

		Assert.assertNotSame(
				expectedFile,
				ResourceUtils.findInstanceResourcesDirectory( appDir, c2 ));

		c2.extendComponent( c1 );
		Assert.assertEquals(
				expectedFile,
				ResourceUtils.findInstanceResourcesDirectory( appDir, c2 ));

		Instance instance = new Instance( "whatever" ).component( c2 );
		Assert.assertEquals(
				expectedFile,
				ResourceUtils.findInstanceResourcesDirectory( appDir, instance ));
	}


	@Test
	public void testFindInstanceResourcesDirectory_withCycle() throws Exception {

		final File appDir = this.folder.newFolder();
		final Component c1 = new Component( "c1" );
		final Component c2 = new Component( "c2" );
		final Component c3 = new Component( "c3" );

		c1.extendComponent( c2 );
		c2.extendComponent( c3 );
		c3.extendComponent( c1 );

		Assert.assertFalse( ResourceUtils.findInstanceResourcesDirectory( appDir, c1 ).exists());
		Assert.assertFalse( ResourceUtils.findInstanceResourcesDirectory( appDir, c2 ).exists());
		Assert.assertFalse( ResourceUtils.findInstanceResourcesDirectory( appDir, c3 ).exists());

		Map<?,?> map = ResourceUtils.storeInstanceResources( appDir, new Instance( "i" ).component( c3 ));
		Assert.assertEquals( 0, map.size());
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

		Assert.assertTrue( componentDirectory.getParentFile().mkdirs());
		Assert.assertTrue( componentDirectory.createNewFile());

		Instance instance = new Instance( "whatever" ).component( new Component( componentName ));
		Map<?,?> map = ResourceUtils.storeInstanceResources( appDir, instance );
		Assert.assertEquals( 0, map.size());
	}


	@Test
	public void testStoreInstanceResources() throws Exception {

		final File appDir = this.folder.newFolder();
		final String componentName = "my-component";
		final File componentDirectory = new File( appDir, Constants.PROJECT_DIR_GRAPH + File.separator + componentName );

		Assert.assertTrue( componentDirectory.mkdirs());

		// No resource
		Instance instance = new Instance( "whatever" ).component( new Component( componentName ));
		Map<?,?> map = ResourceUtils.storeInstanceResources( appDir, instance );
		Assert.assertEquals( 0, map.size());

		// With a single recipe
		File recipeFile = new File( componentDirectory, "recipe.txt" );
		Assert.assertTrue( recipeFile.createNewFile());

		map = ResourceUtils.storeInstanceResources( appDir, instance );
		Assert.assertEquals( 1, map.size());

		// With a properties file for measures
		File autonomicDir = new File( appDir, Constants.PROJECT_DIR_PROBES );
		Assert.assertTrue( autonomicDir.mkdir());

		File propertiesFile = new File( autonomicDir, componentName + Constants.FILE_EXT_MEASURES + ".properties" );
		Assert.assertTrue( propertiesFile.createNewFile());

		// No measures file => the properties are not sent!
		map = ResourceUtils.storeInstanceResources( appDir, instance );
		Assert.assertEquals( 1, map.size());

		// With a measures file
		File measuresFile = new File( autonomicDir, componentName + Constants.FILE_EXT_MEASURES );
		Assert.assertTrue( measuresFile.createNewFile());

		map = ResourceUtils.storeInstanceResources( appDir, instance );
		Assert.assertEquals( 3, map.size());

		// And check without the properties file
		Utils.deleteFilesRecursively( propertiesFile );

		map = ResourceUtils.storeInstanceResources( appDir, instance );
		Assert.assertEquals( 2, map.size());
	}


	@Test
	public void testFindScopedInstancesDirectories() throws Exception {

		final File appDir = this.folder.newFolder();
		TestApplication app = new TestApplication();
		app.setDirectory( appDir );

		Assert.assertEquals( 0, ResourceUtils.findScopedInstancesDirectories( app ).size());

		File vmDir = new File( appDir, Constants.PROJECT_DIR_GRAPH + "/" + app.getMySqlVm().getComponent().getName());
		Assert.assertTrue( vmDir.mkdirs());

		Map<Component,File> map = ResourceUtils.findScopedInstancesDirectories( app );
		Assert.assertEquals( 1, map.size());
		Assert.assertEquals( vmDir, map.get( app.getMySqlVm().getComponent()));
	}
}
