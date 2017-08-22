/**
 * Copyright 2016-2017 Linagora, Université Joseph Fourier, Floralis
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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DefaultDockerClientConfig.Builder;
import com.github.dockerjava.core.DockerClientBuilder;

import net.roboconf.core.model.beans.Instance;
import net.roboconf.target.api.TargetHandlerParameters;
import net.roboconf.target.docker.internal.test.DockerTestUtils;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ToRunByHandDockerImageGenerationTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();


	@Test
	@Ignore
	public void createRoboconfImage() throws Exception {

		// Verify no image exists
		final String tag = "roboconf-test-by-hand";
		Builder config = DefaultDockerClientConfig.createDefaultConfigBuilder();
		config.withDockerHost( "tcp://localhost:" + DockerTestUtils.DOCKER_TCP_PORT );

		DockerClient docker = DockerClientBuilder.getInstance( config.build()).build();
		Image img = DockerUtils.findImageByIdOrByTag( tag, docker );
		if( img != null ) {
			docker.removeImageCmd( tag ).exec();
			img = DockerUtils.findImageByIdOrByTag( tag, docker );
		}

		Assert.assertNull( img );

		// Prepare the parameters
		File baseDir = new File( Thread.currentThread().getContextClassLoader().getResource( "./image/roboconf" ).getFile());
		Assert.assertTrue( baseDir.exists());

		Map<String,String> targetProperties = new HashMap<> ();
		targetProperties.put( DockerHandler.IMAGE_ID, tag );
		targetProperties.put( DockerHandler.GENERATE_IMAGE_FROM, "." );

		TargetHandlerParameters parameters= new TargetHandlerParameters();
		parameters.setTargetProperties( targetProperties );
		parameters.setMessagingProperties( new HashMap<String,String>( 0 ));
		parameters.setApplicationName( "applicationName" );
		parameters.setScopedInstancePath( "/vm" );
		parameters.setScopedInstance( new Instance());
		parameters.setTargetPropertiesDirectory( baseDir );

		File tmpFolder = this.folder.newFolder();
		Map<String,File> containerIdToVolume = new HashMap<> ();
		DockerMachineConfigurator configurator = new DockerMachineConfigurator(
				parameters,
				"machineId",
				tmpFolder,
				containerIdToVolume );

		// Test the creation
		Container container = null;
		try {
			configurator.configure();
			img = DockerUtils.findImageByIdOrByTag( tag, docker );
			Assert.assertNotNull( img );

			String containerName = DockerUtils.buildContainerNameFrom(
					parameters.getScopedInstancePath(),
					parameters.getApplicationName());

			container = DockerUtils.findContainerByIdOrByName( containerName, docker );
			Assert.assertNotNull( container );

		} finally {
			if( img != null )
				docker.removeImageCmd( tag ).exec();

			img = DockerUtils.findImageByIdOrByTag( tag, docker );
			Assert.assertNull( img );

			if( container != null ) {
				docker.removeContainerCmd( container.getId()).withForce( true ).exec();
				container = DockerUtils.findContainerByIdOrByName( container.getId(), docker );
				Assert.assertNull( container );
			}

			configurator.close();
		}
	}
}
