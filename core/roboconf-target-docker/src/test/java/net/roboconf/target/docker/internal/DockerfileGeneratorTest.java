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

import junit.framework.Assert;
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
}
