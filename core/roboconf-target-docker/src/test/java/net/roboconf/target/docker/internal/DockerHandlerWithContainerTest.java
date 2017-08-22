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

package net.roboconf.target.docker.internal;

import static net.roboconf.target.docker.internal.DockerUtils.extractBoolean;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse.ContainerState;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DefaultDockerClientConfig.Builder;
import com.github.dockerjava.core.DockerClientBuilder;

import net.roboconf.agent.internal.AgentProperties;
import net.roboconf.agent.internal.misc.UserDataHelper;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.utils.Utils;
import net.roboconf.target.api.TargetHandlerParameters;
import net.roboconf.target.docker.internal.DockerMachineConfigurator.RoboconfBuildImageResultCallback;
import net.roboconf.target.docker.internal.test.DockerTestUtils;

/**
 * We just check basic behavior like launching Docker images.
 * <p>
 * We would like to go fast. So, we do not need a real agent image.
 * We can use a minimalist image, like Alpine.
 * </p>
 *
 * @author Pierre-Yves Gibello - Linagora
 * @author Pierre Bourret - Université Joseph Fourier
 */
public class DockerHandlerWithContainerTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();
	private static final String TAG = "roboconf-test-img";

	private static boolean dockerIsInstalled = true;
	private static String dockerImageId;

	private final Logger logger = Logger.getLogger( getClass().getName());
	private DockerClient docker;



	@BeforeClass
	public static void prepareDockerEnv() throws Exception {

		final Logger logger = Logger.getLogger( DockerHandlerWithContainerTest.class.getName());
		try {
			// Is Docker installed?
			DockerTestUtils.checkDockerIsInstalled();

			// Prepare the environment
			DockerClient docker = buildDockerClient();
			File baseDir = new File( Thread.currentThread().getContextClassLoader().getResource( "./image/alpine" ).getFile());
			Assert.assertTrue( baseDir.exists());

			String builtImageId = docker.buildImageCmd(baseDir)
					.withNoCache( true ).withTags( new HashSet<>( Arrays.asList( TAG )))
					.exec( new RoboconfBuildImageResultCallback())
					.awaitImageId();

			logger.finest( "Built image ID: " + builtImageId );

			List<Image> images = docker.listImagesCmd().exec();
			images = images == null ? new ArrayList<Image>( 0 ) : images;
			Image img = DockerUtils.findImageByTag( TAG, images );

			Assert.assertNotNull( img );
			dockerImageId = img.getId();
			docker.close();

		} catch( IOException | InterruptedException e ) {
			logger.warning( "Tests are skipped because Docker is not installed or misconfigured." );
			Utils.logException( logger, e );

			dockerIsInstalled = false;
			Assume.assumeNoException( e );
		}
	}


	@AfterClass
	public static void cleanDockerEnv() throws Exception {

		DockerClient docker = buildDockerClient();
		DockerUtils.deleteImageIfItExists( dockerImageId, docker );
		docker.close();
	}


	private static DockerClient buildDockerClient() {

		Builder config = DefaultDockerClientConfig.createDefaultConfigBuilder();
		config.withDockerHost( "tcp://localhost:" + DockerTestUtils.DOCKER_TCP_PORT );
		return DockerClientBuilder.getInstance( config.build()).build();
	}



	@Before
	public void newClient() {
		this.docker = buildDockerClient();
	}


	@After
	public void closedClient() throws Exception {
		this.docker.close();
	}


	@Test
	public void testCreateAndTerminateVM() throws Exception {

		Assume.assumeTrue( dockerIsInstalled );
		Map<String,String> targetProperties = new HashMap<>( 1 );
		targetProperties.put( DockerHandler.IMAGE_ID, TAG );

		testCreateAndTerminateVM( targetProperties );
	}


	@Test
	public void testCreateAndTerminateVM_withOptions() throws Exception {

		Assume.assumeTrue( dockerIsInstalled );
		Map<String,String> targetProperties = new HashMap<>( 2 );
		targetProperties.put( DockerHandler.IMAGE_ID, TAG );
		targetProperties.put( DockerHandler.OPTION_PREFIX_RUN + "cap-add", "SYS_PTRACE" );

		testCreateAndTerminateVM( targetProperties );
	}


	@Test
	public void checkImagesAreFoundCorrectly() {

		Assert.assertNull( DockerUtils.findImageByIdOrByTag( "oops81:unknown", this.docker ));
		Assert.assertNotNull( DockerUtils.findImageByIdOrByTag( "ubuntu", this.docker ));
		Assert.assertNotNull( DockerUtils.findImageByIdOrByTag( "ubuntu:latest", this.docker ));
	}


	@Test
	public void testDockerUtils_onLimits() {

		DockerUtils.deleteImageIfItExists( null, this.docker );
		Assert.assertTrue( "No exception is thrown trying to delete a null image ID.", true );

		DockerUtils.deleteImageIfItExists( "bla 11 4 2 bla", this.docker );
		Assert.assertTrue( "No exception is thrown trying to delete something that does not exist.", true );

		Container container = DockerUtils.findContainerByIdOrByName( "bla 11 4 2 bla", this.docker );
		Assert.assertNull( container );

		Image image = DockerUtils.findImageByIdOrByTag( null, this.docker );
		Assert.assertNull( image );

		image = DockerUtils.findImageByIdOrByTag( "invalid", this.docker );
		Assert.assertNull( image );
	}


	/**
	 * Creates, checks and terminates a Docker container.
	 * @param targetProperties the target properties
	 * @throws Exception
	 */
	private void testCreateAndTerminateVM( Map<String,String> targetProperties ) throws Exception {

		DockerHandler target = new DockerHandler();
		target.userDataVolume = this.folder.newFolder();

		Instance scopedInstance = new Instance( "test-596598515" );
		String path = InstanceHelpers.computeInstancePath( scopedInstance );

		Map<String,String> msgCfg = new LinkedHashMap<>();
		msgCfg.put("net.roboconf.messaging.type", "telepathy");
		msgCfg.put("mindControl", "false");
		msgCfg.put("psychosisProtection", "active");

		TargetHandlerParameters parameters = new TargetHandlerParameters()
				.targetProperties( targetProperties )
				.messagingProperties( msgCfg )
				.applicationName( "roboconf" )
				.domain( "my-domain" )
				.scopedInstancePath( path )
				.scopedInstance( scopedInstance );

		String containerId = null;
		try {
			target.start();
			containerId = target.createMachine( parameters );
			Assert.assertNotNull( containerId );
			Assert.assertNull( scopedInstance.data.get( Instance.MACHINE_ID ));

			// DockerMachineConfigurator is implemented in such a way that it runs only
			// once when the image already exists. However, we must wait for the thread pool
			// executor to pick up the configurator.
			target.configureMachine( parameters, containerId );

			// Be careful, the Docker target changes the machine ID
			containerId = DockerTestUtils.waitForMachineId(
					containerId,
					scopedInstance.data,
					DockerTestUtils.DOCKER_CONFIGURE_TIMEOUT);

			Assert.assertNotNull( containerId );

			// Check the machine is running
			Assert.assertTrue( target.isMachineRunning( parameters, containerId ));

			// Verify user data were passed correctly (read like an agent)
			File[] children = target.userDataVolume.listFiles();
			Assert.assertNotNull( children );
			Assert.assertEquals( 1, children.length );

			File userDataDirectory = children[ 0 ];
			File userDataFile = new File( userDataDirectory, DockerMachineConfigurator.USER_DATA_FILE );
			Assert.assertTrue( userDataFile.exists());

			AgentProperties agentProps = new UserDataHelper().findParametersFromUrl( userDataFile.toURI().toString(), this.logger );
			Assert.assertNotNull( agentProps );
			Assert.assertEquals( parameters.getApplicationName(), agentProps.getApplicationName());
			Assert.assertEquals( parameters.getDomain(), agentProps.getDomain());
			Assert.assertEquals( parameters.getScopedInstancePath(), agentProps.getScopedInstancePath());
			Assert.assertEquals( parameters.getMessagingProperties(), agentProps.getMessagingConfiguration());
			Assert.assertNull( agentProps.validate());

			// Just for verification, try to terminate an invalid container
			target.terminateMachine( parameters, "invalid identifier" );
			Assert.assertTrue( target.isMachineRunning( parameters, containerId ));

			// Terminate the container
			target.terminateMachine( parameters, containerId );
			Assert.assertFalse( target.isMachineRunning( parameters, containerId ));

			// Verify user data were deleted
			Assert.assertFalse( userDataFile.exists());
			Assert.assertFalse( userDataDirectory.exists());
			Assert.assertTrue( userDataDirectory.getParentFile().exists());

		} finally {
			if( containerId != null ) {
				ContainerState state = DockerUtils.getContainerState( containerId, this.docker );
				if( state != null && extractBoolean( state.getRunning()))
					this.docker.removeContainerCmd( containerId ).withForce( true ).exec();
			}

			target.stop();
		}
	}
}
