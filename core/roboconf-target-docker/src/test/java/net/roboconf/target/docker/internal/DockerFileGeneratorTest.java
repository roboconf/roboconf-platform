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
import java.nio.file.Files;

import junit.framework.Assert;
import net.roboconf.core.utils.Utils;

import org.junit.Before;
import org.junit.Test;

/**
 * @author Pierre-Yves Gibello - Linagora
 */
public class DockerFileGeneratorTest {

	File agentPackTgz;
	File agentPackZip;

	@Before
	public void prepareTest() throws IOException {
		this.agentPackTgz = Files.createTempFile("dockertest", ".tar.gz").toFile();
		this.agentPackTgz.deleteOnExit();

		this.agentPackZip = Files.createTempFile("dockertest", ".zip").toFile();
		this.agentPackZip.deleteOnExit();
	}


	@Test
	public void testNewFileGenerator() {
		DockerFileGenerator gen = new DockerFileGenerator("file://" + this.agentPackZip.getAbsolutePath(), null);
		Assert.assertEquals(gen.getPackages(), "openjdk-7-jre-headless");
		Assert.assertFalse(gen.isTar());

		gen = new DockerFileGenerator(this.agentPackTgz.getAbsolutePath(), "pack1 pack2");
		Assert.assertEquals(gen.getPackages(), "pack1 pack2");
		Assert.assertTrue(gen.isTar());
	}

	@Test
	public void testGenerateDockerFile() throws IOException {

		File dockerfile = null;
		try {
			DockerFileGenerator gen = new DockerFileGenerator(this.agentPackTgz.getAbsolutePath(), null);
			dockerfile = gen.generateDockerfile();
			Assert.assertTrue(dockerfile.exists());
			Assert.assertTrue(dockerfile.isDirectory());

			File f = new File(dockerfile, "Dockerfile");
			Assert.assertTrue(f.exists());
			Assert.assertTrue(f.length() > 0);

			f = new File(dockerfile, "start.sh");
			Assert.assertTrue(f.exists());
			Assert.assertTrue(f.length() > 0);
			Assert.assertTrue(f.canExecute());

			f = new File(dockerfile, "rc.local");
			Assert.assertTrue(f.exists());
			Assert.assertTrue(f.length() > 0);
			Assert.assertTrue(f.canExecute());

			f = new File(dockerfile, this.agentPackTgz.getName());
			Assert.assertTrue(f.exists());

		} finally {
			if(dockerfile != null)
				Utils.deleteFilesRecursively(dockerfile);
		}
	}
}
