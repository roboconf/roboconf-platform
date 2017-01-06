/**
 * Copyright 2013-2017 Linagora, Université Joseph Fourier, Floralis
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
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.junit.Assert;
import net.roboconf.core.internal.tests.TestUtils;

/**
 * @author Amadou Diarra - Université Joseph Fourier
 */
public final class DockerfileParserTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();


	@Test
	public void dockerfileToCommandListTest_1() throws Exception {

		File testFile = TestUtils.findTestFile( "/dockerfiles/dockerfile1" );
		List<DockerfileParser.DockerCommand> ldc = DockerfileParser.dockerfileToCommandList( testFile );
		Assert.assertEquals( 9, ldc.size());

		Assert.assertEquals( DockerfileParser.DockerCommandType.RUN, ldc.get( 0 ).type );
		Assert.assertEquals( "apt-get update", ldc.get( 0 ).argument );

		Assert.assertEquals( DockerfileParser.DockerCommandType.RUN, ldc.get( 1 ).type );
		Assert.assertEquals( "apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 7F0CEB10", ldc.get( 1 ).argument );

		Assert.assertEquals( DockerfileParser.DockerCommandType.RUN, ldc.get( 2 ).type );
		Assert.assertEquals( "echo 'deb http://downloads-distro.mongodb.org/repo/ubuntu-upstart dist 10gen' | tee /etc/apt/sources.list.d/mongodb.list", ldc.get( 2 ).argument );

		Assert.assertEquals( DockerfileParser.DockerCommandType.ADD, ldc.get( 3 ).type );
		Assert.assertEquals( "rabbitmq-env.conf /etc/rabbitmq/rabbitmq-env.conf", ldc.get( 3 ).argument );

		Assert.assertEquals( DockerfileParser.DockerCommandType.COPY, ldc.get( 4 ).type );
		Assert.assertEquals( "/ttee/tree/toto.txt /usr/local/deploy/", ldc.get( 4 ).argument );

		Assert.assertEquals( DockerfileParser.DockerCommandType.RUN, ldc.get( 5 ).type );
		Assert.assertEquals( "apt-get update", ldc.get( 5 ).argument );

		Assert.assertEquals( DockerfileParser.DockerCommandType.RUN, ldc.get( 6 ).type );
		Assert.assertEquals( "apt-get install -y mongodb-10gen", ldc.get( 6 ).argument );

		Assert.assertEquals( DockerfileParser.DockerCommandType.RUN, ldc.get( 7 ).type );
		Assert.assertEquals( "mkdir -p /data/db", ldc.get( 7 ).argument );

		Assert.assertEquals( DockerfileParser.DockerCommandType.COPY, ldc.get( 8 ).type );
		Assert.assertEquals( "ext1.jar /usr/local/deploy/", ldc.get( 8 ).argument );
	}


	@Test
	public void dockerfileToCommandListTest_2() throws Exception {

		File testFile = TestUtils.findTestFile( "/dockerfiles/dockerfile2" );
		List<DockerfileParser.DockerCommand> ldc = DockerfileParser.dockerfileToCommandList( testFile );
		Assert.assertEquals( 4, ldc.size());

		Assert.assertEquals( DockerfileParser.DockerCommandType.RUN, ldc.get( 0 ).type );
		Assert.assertEquals( "apt-get update", ldc.get( 0 ).argument );

		Assert.assertEquals( DockerfileParser.DockerCommandType.RUN, ldc.get( 1 ).type );
		Assert.assertEquals( "apt-get install -y python python-pip wget", ldc.get( 1 ).argument );

		Assert.assertEquals( DockerfileParser.DockerCommandType.RUN, ldc.get( 2 ).type );
		Assert.assertEquals( "pip install Flask", ldc.get( 2 ).argument );

		Assert.assertEquals( DockerfileParser.DockerCommandType.ADD, ldc.get( 3 ).type );
		Assert.assertEquals( "hello.py /home/hello.py", ldc.get( 3 ).argument );
	}


	@Test
	public void dockerfileToCommandListTest_empty() throws Exception {

		File testFile = this.folder.newFile();
		List<DockerfileParser.DockerCommand> ldc = DockerfileParser.dockerfileToCommandList( testFile );
		Assert.assertEquals( 0, ldc.size());
	}
}
