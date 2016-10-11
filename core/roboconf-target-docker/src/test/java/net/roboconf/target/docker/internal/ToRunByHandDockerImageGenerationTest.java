/**
 * Copyright 2016 Linagora, Université Joseph Fourier, Floralis
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
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DefaultDockerClientConfig.Builder;
import com.github.dockerjava.core.DockerClientBuilder;

import net.roboconf.core.internal.tests.TestUtils;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ToRunByHandDockerImageGenerationTest {

	@Test
	@Ignore
	public void createRoboconfImage() throws Exception {

		// Verify no image exists
		final String tag = "roboconf-test-by-hand";

		Builder config = DefaultDockerClientConfig.createDefaultConfigBuilder();
		config.withDockerHost( "http://localhost:" + DockerTestUtils.DOCKER_TCP_PORT );

		DockerClient docker = DockerClientBuilder.getInstance( config.build()).build();
		Image img = DockerUtils.findImageByIdOrByTag( tag, docker );
		if( img != null ) {
			docker.removeImageCmd( tag ).exec();
			img = DockerUtils.findImageByIdOrByTag( tag, docker );
		}

		Assert.assertNull( img );

		// Prepare the parameters
		File agentTarGz = TestUtils.findTestFile( "/archives/roboconf-fake-agent.tar.gz" );

		Map<String,String> targetProperties = new HashMap<> ();
		targetProperties.put( DockerHandler.AGENT_PACKAGE_URL, agentTarGz.getAbsolutePath());

		DockerMachineConfigurator configurator = new DockerMachineConfigurator(
				targetProperties,
				new HashMap<String,String>( 0 ),
				"machineId", "scopedInstancePath", "applicationName", null );

		// Test the creation
		try {
			configurator.dockerClient = docker;
			configurator.createImage( tag );

			img = DockerUtils.findImageByIdOrByTag( tag, docker );
			Assert.assertNotNull( img );

		} finally {
			if( img != null )
				docker.removeImageCmd( tag ).exec();

			img = DockerUtils.findImageByIdOrByTag( tag, docker );
			configurator.close();
			Assert.assertNull( img );
		}
	}
}
