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
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import org.junit.Assume;
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
		File dockerfile = new File(url.getFile());
		List<DockerfileParser.DockerCommand> ldc = DockerfileParser.dockerfileToCommandList(dockerfile);
		Assert.assertEquals(9, ldc.size());

		DockerfileParser.DockerCommand c1 = ldc.get(0);
		DockerfileParser.DockerCommand c2 = ldc.get(1);
		DockerfileParser.DockerCommand c3 = ldc.get(2);
		DockerfileParser.DockerCommand c4 = ldc.get(3);
		DockerfileParser.DockerCommand c5 = ldc.get(4);

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

	}

	@Test
	public void executeDockerCommandTestWindows() throws IOException, InterruptedException{
		DockerfileParser.DockerCommand command = new DockerfileParser.DockerCommand();
		command.type = DockerfileParser.DockerCommandType.RUN;
		command.argument = "md toto";
		boolean isWin = System.getProperty( "os.name" ).toLowerCase().contains( "win" );
		Assume.assumeTrue( isWin );
		int exitCode = DockerfileParser.executeDockerCommand(
				Logger.getLogger( getClass().getName()),
				null,
				null,
				command);

		Assert.assertSame( 0, exitCode );

	}

	@Test
	public void executeDockerCommandTestUnix() throws IOException, InterruptedException{
		DockerfileParser.DockerCommand c1 = new DockerfileParser.DockerCommand();
		DockerfileParser.DockerCommand c2 = new DockerfileParser.DockerCommand();
		DockerfileParser.DockerCommand c3 = new DockerfileParser.DockerCommand();

		c1.type = DockerfileParser.DockerCommandType.RUN;
		c1.argument = "mkdir toto";

		c2.type = DockerfileParser.DockerCommandType.RUN;
		c2.argument = "rm -rf toto";

		/*c3.type = DockerfileParser.DockerCommandType.COPY;
		c3.argument = "toto.jar etc/deploy";**/

		String osName = System.getProperty( "os.name" ).toLowerCase();
		boolean isUnix = osName.contains( "linux" )
				|| osName.contains( "unix" )
				|| osName.contains( "freebsd" );
		Assume.assumeTrue( isUnix );
		int exitCode1 = DockerfileParser.executeDockerCommand(
				Logger.getLogger( getClass().getName()),
				null,
				new HashMap<String,String>( 0 ),
				c1);
		int exitCode2 = DockerfileParser.executeDockerCommand(
				Logger.getLogger( getClass().getName()),
				null,
				new HashMap<String,String>( 0 ),
				c2);
		/*int exitCode3 = DockerfileParser.executeDockerCommand(
				Logger.getLogger( getClass().getName()),
				null,
				new HashMap<String,String>( 0 ),
				c3);*/
		Assert.assertSame( 0, exitCode1 );
		Assert.assertSame( 0, exitCode2 );
		//Assert.assertSame( 0, exitCode3 );

	}

}