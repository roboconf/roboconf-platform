/**
 * Copyright 2014-2015 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.target.docker.internal;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import net.roboconf.core.utils.Utils;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author Pierre-Yves Gibello - Linagora
 */
public class DockerfileGeneratorTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();


	@Test
	public void testNewFileGenerator() throws Exception {

		File agentPackZip = this.folder.newFile( "dockertest.zip" );
		DockerfileGenerator gen = new DockerfileGenerator("file://" + agentPackZip.getAbsolutePath(), null, null, null);
		Assert.assertEquals( "openjdk-7-jre-headless", gen.getPackages());
		Assert.assertEquals( "ubuntu", gen.getBaseImageName());
		Assert.assertFalse( gen.isTar());

		File agentPackTgz = this.folder.newFile( "dockertest.tar.gz" );
		gen = new DockerfileGenerator( agentPackTgz.getAbsolutePath(), "pack1 pack2", null, "ubuntu:15.1" );
		Assert.assertEquals( "pack1 pack2", gen.getPackages());
		Assert.assertEquals( "ubuntu:15.1", gen.getBaseImageName());
		Assert.assertTrue( gen.isTar());
	}


	@Test
	public void testGenerateDockerFile() throws IOException {

		File dockerfile = null;
		File[] files = new File[] {
				this.folder.newFile( "dockertest.tar.gz" ),
				this.folder.newFile( "dockertest.zip" )
		};

		for( File file : files ) {
			try {
				DockerfileGenerator gen = new DockerfileGenerator( file.getAbsolutePath(), null, null, null );
				dockerfile = gen.generateDockerfile();
				Assert.assertTrue( file.getName(), dockerfile.isDirectory());

				File f = new File(dockerfile, "Dockerfile");
				Assert.assertTrue( file.getName(), f.exists());
				Assert.assertTrue( file.getName(), f.length() > 0 );

				String dfContent = Utils.readFileContent( f );
				Assert.assertTrue( dfContent.startsWith( "FROM ubuntu\n" ));

				f = new File(dockerfile, "start.sh");
				Assert.assertTrue( file.getName(), f.exists());
				Assert.assertTrue( file.getName(), f.length() > 0 );
				Assert.assertTrue( file.getName(), f.canExecute());

				f = new File(dockerfile, "rc.local");
				Assert.assertTrue( file.getName(), f.exists());
				Assert.assertTrue( file.getName(), f.length() > 0 );
				Assert.assertTrue( file.getName(), f.canExecute());

				f = new File(dockerfile, "rename.sh");
				Assert.assertTrue( file.getName(), f.exists());
				Assert.assertTrue( file.getName(), f.length() > 0 );
				Assert.assertTrue( file.getName(), f.canExecute());

				f = new File(dockerfile, file.getName());
				Assert.assertTrue( file.getName(), f.exists());

			} finally {
				Utils.deleteFilesRecursively(dockerfile);
			}
		}
	}


	@Test
	public void testGenerateDockerFile_withOtherBaseImage() throws IOException {

		File dockerfile = null;
		File file = this.folder.newFile( "dockertest.tar.gz" );

		try {
			DockerfileGenerator gen = new DockerfileGenerator( file.getAbsolutePath(), null, null, "titi:21" );
			dockerfile = gen.generateDockerfile();
			Assert.assertTrue( file.getName(), dockerfile.isDirectory());

			File f = new File(dockerfile, "Dockerfile");
			Assert.assertTrue( file.getName(), f.exists());
			Assert.assertTrue( file.getName(), f.length() > 0 );

			String dfContent = Utils.readFileContent( f );
			Assert.assertTrue( dfContent.startsWith( "FROM titi:21\n" ));

		} finally {
			Utils.deleteFilesRecursively(dockerfile);
		}
	}

	@Test
	public void testDockerfileWithAdditionalDeploys() throws Exception {

		List<String> urls = new ArrayList<String>( 2 );
		urls.add("toto");
		urls.add("tutu");

		File agentPackZip = this.folder.newFile( "dockertest.zip" );
		DockerfileGenerator gen = new DockerfileGenerator( "file://" + agentPackZip.getAbsolutePath(), null, urls, null );
		Assert.assertEquals( 2, gen.deployList.size());
		Assert.assertEquals( "toto", gen.deployList.get(0));
		Assert.assertEquals("tutu", gen.deployList.get(1));
	}


	@Test
	public void testFindAgentFileName() {

		Assert.assertEquals(
				"agent-0.4.tar.gz",
				DockerfileGenerator.findAgentFileName( "file:/home/toto/.m2/net/roboconf/.../agent-0.4.tar.gz", true ));

		Assert.assertEquals(
				"agent-0.4.zip",
				DockerfileGenerator.findAgentFileName( "file:/home/toto/.m2/net/roboconf/.../agent-0.4.zip", false ));

		Assert.assertEquals(
				"roboconf-agent-0.5.zip",
				DockerfileGenerator.findAgentFileName( "http://maven/.../roboconf-agent-0.5.zip", false ));

		Assert.assertEquals(
				"roboconf-agent-0.5.tar.gz",
				DockerfileGenerator.findAgentFileName( "http://maven/.../roboconf-agent-0.5.tar.gz", true ));

		Assert.assertEquals(
				"roboconf-agent.tar.gz",
				DockerfileGenerator.findAgentFileName( "https://oss.sonatype.org/.../redirect?a=roboconf-karaf-dist-agent&v=0.4&p=zip", true ));

		Assert.assertEquals(
				"roboconf-agent.zip",
				DockerfileGenerator.findAgentFileName( "https://oss.sonatype.org/.../redirect?a=roboconf-karaf-dist-agent&v=0.4&p=zip", false ));
	}


	@Test
	public void testGetFileNameFromFileUrl() throws Exception {

		String url = "http://host.com/test.html";
		Assert.assertEquals( "test.html", DockerfileGenerator.getFileNameFromFileUrl( url ));

		url = "http://host.com/path/test.html";
		Assert.assertEquals( "test.html", DockerfileGenerator.getFileNameFromFileUrl( url ));

		url = "file://dir1/dir2/test.html";
		Assert.assertEquals( "test.html", DockerfileGenerator.getFileNameFromFileUrl( url ));

		url = "file://host.com/dir/dir?name=test.html";
		Assert.assertEquals( "dir", DockerfileGenerator.getFileNameFromFileUrl( url ));

		url = "file://host.com/dir/?test.html";
		Assert.assertEquals( "test.html", DockerfileGenerator.getFileNameFromFileUrl( url ));
	}


	@Test
	public void testPrepareKarafFeature() throws Exception {

		List<String> urls = new ArrayList<String> ();
		String res = DockerfileGenerator.prepareKarafFeature( urls );
		Assert.assertNull( res );

		urls.add( "toto.txt" );
		res = DockerfileGenerator.prepareKarafFeature( urls );
		Assert.assertNotNull( res );
		Assert.assertTrue( res.contains( "<bundle>toto.txt</bundle>" ));
		Assert.assertFalse( res.contains( "%CONTENT%" ));

		urls.add( "titi.txt" );
		res = DockerfileGenerator.prepareKarafFeature( urls );
		Assert.assertNotNull( res );
		Assert.assertTrue( res.contains( "<bundle>titi.txt</bundle>" ));
		Assert.assertFalse( res.contains( "%CONTENT%" ));
	}


	@Test
	public void testHandleAdditionalDeployments() throws Exception {

		// Empty
		List<String> urls = new ArrayList<String> ();
		DockerfileGenerator gen = new DockerfileGenerator( "file://whatever.zip", null, urls, null );

		String content = gen.handleAdditionalDeployments();
		Assert.assertEquals( 0, content.length());
		Assert.assertEquals( 0, gen.bundleUrls.size());
		Assert.assertEquals( 0, gen.fileUrlsToCopyInDockerFile.size());

		// Mix local and remote bundles, local and remote XML
		urls = new ArrayList<String> ();
		urls.add( "file:///oops/my_local_bundle.jar" );
		urls.add( "http://oops/my_remote_bundle.jar" );
		urls.add( "file:///oops/my_local_feature.xml" );
		urls.add( "http://oops/my_remote_feature.xml" );

		gen = new DockerfileGenerator( "file://whatever.zip", null, urls, null );

		content = gen.handleAdditionalDeployments();
		Assert.assertTrue( content.length() > 0 );

		Assert.assertEquals( 2, gen.bundleUrls.size());
		Assert.assertEquals( 2, gen.fileUrlsToCopyInDockerFile.size());

		Assert.assertTrue( gen.fileUrlsToCopyInDockerFile.contains( "file:///oops/my_local_bundle.jar" ));
		Assert.assertTrue( gen.fileUrlsToCopyInDockerFile.contains( "file:///oops/my_local_feature.xml" ));

		Assert.assertTrue( gen.bundleUrls.contains( "http://oops/my_remote_bundle.jar" ));
		Assert.assertTrue( gen.bundleUrls.contains( "file://" + DockerfileGenerator.RBCF_DIR + DockerfileGenerator.BACKUP + "my_local_bundle.jar" ));
	}
}
