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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DefaultDockerClientConfig.Builder;
import com.github.dockerjava.core.DockerClientBuilder;

import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.utils.Utils;
import net.roboconf.target.api.TargetException;
import net.roboconf.target.api.TargetHandlerParameters;
import net.roboconf.target.docker.internal.DockerMachineConfigurator.RoboconfBuildImageResultCallback;

/**
 * @author Pierre-Yves Gibello - Linagora
 * @author Pierre Bourret - Université Joseph Fourier
 */
public class DockerHandlerWithContainerTest {

	private final Logger logger = Logger.getLogger( getClass().getName());

	private boolean dockerIsInstalled = true;
	private DockerClient docker;
	private String dockerImageId;
	private Map<String, String> msgCfg = new LinkedHashMap<>();


	@Before
	public void setMessagingConfiguration() {
		this.msgCfg = new LinkedHashMap<>();
		this.msgCfg.put("net.roboconf.messaging.type", "telepathy");
		this.msgCfg.put("mindControl", "false");
		this.msgCfg.put("psychosisProtection", "active");
	}


	@Before
	public void checkDockerIsInstalled() throws Exception {

		Assume.assumeTrue( this.dockerIsInstalled );
		try {
			DockerTestUtils.checkDockerIsInstalled();
			prepareDockerTest();

		} catch( IOException | InterruptedException e ) {
			this.logger.warning( "Tests are skipped because Docker is not installed or misconfigured." );
			Utils.logException( this.logger, e );

			this.dockerIsInstalled = false;
			Assume.assumeNoException( e );
		}
	}


	@After
	public void dockerCleanup() {

		if( this.docker != null ) {
			DockerUtils.deleteImageIfItExists( this.dockerImageId, this.docker );
			try {
				this.docker.close();

			} catch( IOException e ) {
				// nothing
			}
		}
	}


	@Test
	public void testValidConfiguration() throws Exception {

		Assume.assumeTrue( this.dockerIsInstalled );
		Map<String, String> targetProperties = loadTargetProperties();
		DockerClient client = DockerUtils.createDockerClient( targetProperties );
		Assert.assertNotNull( client );
	}


	@Test
	public void testCreateAndTerminateVM() throws Exception {

		Assume.assumeTrue( this.dockerIsInstalled );
		Map<String,String> targetProperties = loadTargetProperties();
		testCreateAndTerminateVM( targetProperties );
	}


	@Test
	public void testCreateAndTerminateVM_withOptions() throws Exception {

		Assume.assumeTrue( this.dockerIsInstalled );
		Map<String,String> targetProperties = loadTargetProperties();
		targetProperties.put( DockerHandler.OPTION_PREFIX_RUN + "cap-add", "SYS_PTRACE" );

		testCreateAndTerminateVM( targetProperties );
	}


	@Test( expected = TargetException.class )
	public void testConfigureVM_invalidBaseImage() throws Exception {

		Assume.assumeTrue( this.dockerIsInstalled );
		Map<String,String> targetProperties = loadTargetProperties();
		targetProperties.put( DockerHandler.IMAGE_ID, "will-not-be-generated" );
		targetProperties.put( DockerHandler.BASE_IMAGE, "oops81:unknown" );

		DockerMachineConfigurator configurator = new DockerMachineConfigurator(
				targetProperties, this.msgCfg, "656sdf6sd", "/test", "app",	new Instance());

		try {
			configurator.dockerClient = this.docker;
			configurator.createImage( "will-not-be-generated" );

		} finally {
			configurator.dockerClient = null;
			configurator.close();
			Assert.assertNull( DockerUtils.findImageByIdOrByTag( "will-not-be-generated", this.docker ));
		}
	}


	@Test( expected = TargetException.class )
	public void testCreateVM_missingParameters() throws Exception {

		Assume.assumeTrue( this.dockerIsInstalled );
		DockerHandler target = new DockerHandler();

		Map<String,String> targetProperties = loadTargetProperties();
		targetProperties.remove( DockerHandler.IMAGE_ID );

		TargetHandlerParameters parameters = new TargetHandlerParameters()
				.targetProperties( targetProperties )
				.messagingProperties( this.msgCfg )
				.applicationName( "roboconf" )
				.domain( "my-domain" )
				.scopedInstancePath( "test" );

		target.createMachine( parameters );
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
	 * Loads the target properties for the configuration of Docker.
	 */
	private Map<String,String> loadTargetProperties() throws Exception {

		URL res = Thread.currentThread().getContextClassLoader().getResource("conf/docker.properties");
		File propertiesFile = new File( res.getFile());
		Properties p = Utils.readPropertiesFile( propertiesFile );

		HashMap<String,String> targetProperties = new HashMap<>();
		for( Map.Entry<Object,Object> entry : p.entrySet())
			targetProperties.put( entry.getKey().toString(), entry.getValue().toString());

		if( this.dockerImageId != null )
			targetProperties.put( DockerHandler.IMAGE_ID, this.dockerImageId );

		return targetProperties;
	}


	/**
	 * Prepares the docker environment (image, etc...) for testing.
	 * @throws IOException
	 */
	private void prepareDockerTest() throws Exception {
		final String tag = "roboconf-test";

		Builder config = DefaultDockerClientConfig.createDefaultConfigBuilder();
		config.withDockerHost( "tcp://localhost:" + DockerTestUtils.DOCKER_TCP_PORT );

		this.docker = DockerClientBuilder.getInstance( config.build()).build();
		File baseDir = new File( Thread.currentThread().getContextClassLoader().getResource("./image").getFile());

		String builtImageId = this.docker.buildImageCmd(baseDir)
				.withNoCache( true ).withTag( tag )
				.exec( new RoboconfBuildImageResultCallback())
				.awaitImageId();

		this.logger.finest( "Built image ID: " + builtImageId );

		List<Image> images = this.docker.listImagesCmd().exec();
		images = images == null ? new ArrayList<Image>( 0 ) : images;
		Image img = DockerUtils.findImageByTag( tag, images );

		this.dockerImageId = img.getId();
	}


	/**
	 * Creates, checks and terminates a Docker container.
	 * @param targetProperties the target properties
	 * @throws Exception
	 */
	private void testCreateAndTerminateVM( Map<String,String> targetProperties ) throws Exception {

		DockerHandler target = new DockerHandler();
		Instance scopedInstance = new Instance( "test-596598515" );
		String path = InstanceHelpers.computeInstancePath( scopedInstance );

		TargetHandlerParameters parameters = new TargetHandlerParameters()
				.targetProperties( targetProperties )
				.messagingProperties( this.msgCfg )
				.applicationName( "roboconf" )
				.domain( "my-domain" )
				.scopedInstancePath( path );

		try {
			target.start();
			String containerId = target.createMachine( parameters );
			Assert.assertNotNull( containerId );
			Assert.assertNull( scopedInstance.data.get( Instance.MACHINE_ID ));

			// DockerMachineConfigurator is implemented in such a way that it runs only
			// once when the image already exists. However, we must wait for the thread pool
			// executor to pick up the configurator.
			target.configureMachine( parameters, containerId, scopedInstance );

			// Be careful, the Docker target changes the machine ID
			containerId = DockerTestUtils.waitForMachineId(
					containerId,
					scopedInstance.data,
					DockerTestUtils.DOCKER_CONFIGURE_TIMEOUT);

			Assert.assertNotNull( containerId );

			// Check the machine is running
			Assert.assertTrue( target.isMachineRunning( parameters, containerId ));

			// Just for verification, try to terminate an invalid container
			target.terminateMachine( parameters, "invalid identifier" );
			Assert.assertTrue( target.isMachineRunning( parameters, containerId ));

			// Terminate the container
			target.terminateMachine( parameters, containerId );
			Assert.assertFalse( target.isMachineRunning( parameters, containerId ));

		} finally {
			target.stop();
		}
	}
}
