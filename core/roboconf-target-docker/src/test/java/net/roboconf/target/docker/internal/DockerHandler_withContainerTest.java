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
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import junit.framework.Assert;
import net.roboconf.core.utils.Utils;
import net.roboconf.target.api.TargetException;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.BuildImageCmd;
import com.github.dockerjava.api.command.CreateImageResponse;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig.DockerClientConfigBuilder;

/**
 * @author Pierre-Yves Gibello - Linagora
 */
public class DockerHandler_withContainerTest {

	private final Logger logger = Logger.getLogger( getClass().getName());

	private boolean dockerIsInstalled = true;
	private DockerClient docker;
	private String dockerImageId;



	@Before
	public void checkDockerIsInstalled() throws Exception {

		Assume.assumeTrue( this.dockerIsInstalled );
		try {
			DockerTestUtils.checkDockerIsInstalled();
			prepareDockerTest();

		} catch( Exception e ) {
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
		DockerHandler target = new DockerHandler();
		Map<String,String> targetProperties = loadTargetProperties();

		String containerId = target.createMachine( targetProperties, "127.0.0.1", "roboconf", "roboconf", "test", "roboconf" );
		Assert.assertNotNull( containerId );
		Assert.assertTrue( target.isMachineRunning( targetProperties, containerId ));

		target.configureMachine( targetProperties, containerId, null, null, null, null, null );
		target.terminateMachine( targetProperties, containerId );
		Assert.assertFalse( target.isMachineRunning( targetProperties, containerId ));
	}


	@Test( expected = TargetException.class )
	public void testCreateVM_missingParameters() throws Exception {

		Assume.assumeTrue( this.dockerIsInstalled );
		DockerHandler target = new DockerHandler();
		Map<String,String> targetProperties = loadTargetProperties();
		targetProperties.remove( DockerHandler.IMAGE_ID );

		target.createMachine( targetProperties, "127.0.0.1", "roboconf", "roboconf", "test", "roboconf" );
	}


	/**
	 * Loads the target properties for the configuration of Docker.
	 */
	private Map<String,String> loadTargetProperties() throws Exception {

		File propertiesFile = new File( Thread.currentThread().getContextClassLoader().getResource("conf/docker.properties").getFile());
		Properties p = Utils.readPropertiesFile( propertiesFile );

		HashMap<String,String> targetProperties = new HashMap<String,String> ();
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

		DockerClientConfigBuilder config = DockerClientConfig.createDefaultConfigBuilder();
		config.withUri( "http://localhost:" + DockerTestUtils.DOCKER_TCP_PORT );

		this.docker = DockerClientBuilder.getInstance( config.build()).build();
		File baseDir = new File( Thread.currentThread().getContextClassLoader().getResource("image").getFile());
		BuildImageCmd img = this.docker.buildImageCmd(baseDir).withNoCache().withTag("roboconf-test");
		CreateImageResponse rsp = this.docker.createImageCmd( "roboconf-test", img.getTarInputStream()).exec();

		this.dockerImageId = rsp.getId();
	}
}
