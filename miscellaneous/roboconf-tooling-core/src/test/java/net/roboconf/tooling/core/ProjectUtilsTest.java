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

package net.roboconf.tooling.core;

import java.io.File;

import junit.framework.Assert;
import net.roboconf.core.ErrorCode;
import net.roboconf.core.RoboconfError;
import net.roboconf.core.model.io.RuntimeModelIo;
import net.roboconf.core.model.io.RuntimeModelIo.ApplicationLoadResult;
import net.roboconf.tooling.core.ProjectUtils.CreationBean;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * FIXME: add a unit test with a custom POM
 * @author Vincent Zurczak - Linagora
 */
public class ProjectUtilsTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();


	@Test
	public void testSimpleProject() throws Exception {

		File dir = this.folder.newFolder();
		CreationBean bean = new CreationBean()
							.projectDescription( "some desc" ).projectName( "my-project" )
							.projectVersion( "1.0-SNAPSHOT" ).mavenProject( false );

		Assert.assertEquals( 0, dir.listFiles().length );
		ProjectUtils.createProjectSkeleton( dir, bean );
		Assert.assertEquals( 3, dir.listFiles().length );

		ApplicationLoadResult alr = RuntimeModelIo.loadApplication( dir );
		Assert.assertEquals( 2, alr.getLoadErrors().size());
		for( RoboconfError roboconfError : alr.getLoadErrors())
			Assert.assertEquals( ErrorCode.PROJ_NO_RESOURCE_DIRECTORY, roboconfError.getErrorCode());

		Assert.assertEquals( bean.getProjectDescription(), alr.getApplication().getDescription());
		Assert.assertEquals( bean.getProjectName(), alr.getApplication().getName());
		Assert.assertEquals( bean.getProjectVersion(), alr.getApplication().getQualifier());
	}


	@Test
	public void testMavenProject() throws Exception {

		File dir = this.folder.newFolder();
		CreationBean bean = new CreationBean()
							.projectDescription( "some desc" ).projectName( "my-project" )
							.projectVersion( "1.0-SNAPSHOT" ).pluginVersion( "1.0.0" ).groupId( "net.roboconf" );

		Assert.assertEquals( 0, dir.listFiles().length );
		ProjectUtils.createProjectSkeleton( dir, bean );
		Assert.assertEquals( 2, dir.listFiles().length );
		Assert.assertTrue( new File( dir, "pom.xml" ).exists());

		File modelDir = new File( dir, "src/main/model" );
		Assert.assertTrue( modelDir.exists());
		Assert.assertEquals( 3, modelDir.listFiles().length );

		ApplicationLoadResult alr = RuntimeModelIo.loadApplication( modelDir );
		Assert.assertEquals( 2, alr.getLoadErrors().size());
		for( RoboconfError roboconfError : alr.getLoadErrors())
			Assert.assertEquals( ErrorCode.PROJ_NO_RESOURCE_DIRECTORY, roboconfError.getErrorCode());

		Assert.assertEquals( "${project.description}", alr.getApplication().getDescription());
		Assert.assertEquals( "${project.artifact.artifactId}", alr.getApplication().getName());
		Assert.assertEquals( "${project.version}", alr.getApplication().getQualifier());
	}


	@Test
	public void testListPluginVersions() {
		Assert.assertNotNull( ProjectUtils.listMavenPluginVersions());
	}
}
