/**
 * Copyright 2013-2015 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.core;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.junit.Test;

import junit.framework.Assert;

/**
 * Program that tests a Dockerfile parsing.
 *
 * @author Amadou Diarra - Université Joseph Fourier
 */
public final class DockerfileParserTest {


	@Test
	public void dockerfileToCommandListTest() throws IOException{

		URL url = Thread.currentThread().getContextClassLoader().getResource("dockerfile");
		URL url1 = Thread.currentThread().getContextClassLoader().getResource("dockerfile1");
		URL url2 = Thread.currentThread().getContextClassLoader().getResource("dockerfile2");

		File dockerfile = new File(url.getFile());
		File dockerfile1 = new File(url1.getFile());
		File dockerfile2 = new File(url2.getFile());

		List<DockerfileParser.DockerCommand> ldc = DockerfileParser.dockerfileToCommandList(dockerfile);
		List<DockerfileParser.DockerCommand> ldc1 = DockerfileParser.dockerfileToCommandList(dockerfile1);
		List<DockerfileParser.DockerCommand> ldc2 = DockerfileParser.dockerfileToCommandList(dockerfile2);

		Assert.assertEquals(9, ldc.size());
		Assert.assertEquals(4, ldc1.size());
		Assert.assertEquals(0, ldc2.size());

		DockerfileParser.DockerCommand c1 = ldc.get(0);
		DockerfileParser.DockerCommand c2 = ldc.get(1);
		DockerfileParser.DockerCommand c3 = ldc.get(2);
		DockerfileParser.DockerCommand c4 = ldc.get(3);
		DockerfileParser.DockerCommand c5 = ldc.get(4);
		DockerfileParser.DockerCommand c6 = ldc1.get(0);
		DockerfileParser.DockerCommand c7 = ldc1.get(1);
		DockerfileParser.DockerCommand c8 = ldc1.get(3);

		Assert.assertEquals(DockerfileParser.DockerCommandType.RUN, c1.type);
		Assert.assertEquals("apt-get update",c1.argument);

		Assert.assertEquals(DockerfileParser.DockerCommandType.RUN, c2.type);
		Assert.assertEquals("apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 7F0CEB10", c2.argument);

		Assert.assertEquals(DockerfileParser.DockerCommandType.RUN, c3.type);
		Assert.assertEquals("echo 'deb http://downloads-distro.mongodb.org/repo/ubuntu-upstart dist 10gen' | tee /etc/apt/sources.list.d/mongodb.list", c3.argument);

		Assert.assertEquals(DockerfileParser.DockerCommandType.ADD, c4.type);
		Assert.assertEquals("rabbitmq-env.conf /etc/rabbitmq/rabbitmq-env.conf", c4.argument);

		Assert.assertEquals(DockerfileParser.DockerCommandType.COPY, c5.type);
		Assert.assertEquals("/ttee/tree/toto.txt /usr/local/deploy/", c5.argument);

		Assert.assertEquals(DockerfileParser.DockerCommandType.RUN, c6.type);
		Assert.assertEquals("apt-get update", c6.argument);

		Assert.assertEquals(DockerfileParser.DockerCommandType.RUN, c7.type);
		Assert.assertEquals("apt-get install -y python python-pip wget", c7.argument);

		Assert.assertEquals(DockerfileParser.DockerCommandType.ADD, c8.type);
		Assert.assertEquals("hello.py /home/hello.py", c8.argument);
	}
}