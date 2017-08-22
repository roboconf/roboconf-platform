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

import static net.roboconf.target.docker.internal.DockerHandler.GENERATE_IMAGE_FROM;
import static net.roboconf.target.docker.internal.DockerHandler.OPTION_PREFIX_ENV;
import static net.roboconf.target.docker.internal.DockerMachineConfigurator.USER_DATA_DIR;
import static net.roboconf.target.docker.internal.DockerMachineConfigurator.USER_DATA_FILE;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.BuildImageCmd;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.StartContainerCmd;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Volume;

import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.utils.Utils;
import net.roboconf.messaging.api.MessagingConstants;
import net.roboconf.target.api.TargetException;
import net.roboconf.target.api.TargetHandlerParameters;
import net.roboconf.target.docker.internal.DockerMachineConfigurator.RoboconfBuildImageResultCallback;

/**
 * @author Vincent Zurczak - Linagora
 */
public class DockerMachineConfiguratorTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private DockerClient dockerClient;
	private DockerMachineConfigurator configurator;
	private Map<String,File> containerIdToVolume;


	@Before
	public void prepareConfigurator() throws Exception {

		TargetHandlerParameters parameters= new TargetHandlerParameters();
		parameters.setTargetProperties( new LinkedHashMap<String,String>( 0 ));
		parameters.setMessagingProperties( new HashMap<String,String>( 0 ));
		parameters.setApplicationName( "applicationName" );
		parameters.setScopedInstancePath( "/vm" );
		parameters.setScopedInstance( new Instance());

		this.containerIdToVolume = new HashMap<> ();
		this.configurator = new DockerMachineConfigurator(
				parameters,
				"machineId",
				this.folder.newFolder(),
				this.containerIdToVolume );

		this.dockerClient = Mockito.mock( DockerClient.class );
		this.configurator.dockerClient = this.dockerClient;
		this.configurator = Mockito.spy( this.configurator );
	}


	@Test
	public void testClose() throws Exception {

		// Docker client is not null
		Mockito.verifyZeroInteractions( this.dockerClient );
		this.configurator.close();
		Mockito.verify( this.dockerClient, Mockito.times( 1 )).close();

		// When it is null
		Mockito.reset( this.dockerClient );
		this.configurator.dockerClient = null;
		this.configurator.close();
		Mockito.verifyNoMoreInteractions( this.dockerClient );
	}


	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void testCreateContainer_success() throws Exception {

		CreateContainerCmd ccc = Mockito.mock( CreateContainerCmd.class );
		Mockito.when( ccc.withEnv( Mockito.anyListOf( String.class ))).thenReturn( ccc );
		Mockito.when( ccc.withBinds( Mockito.anyListOf( Bind.class ))).thenReturn( ccc );
		Mockito.when( ccc.withVolumes( Mockito.anyListOf( Volume.class ))).thenReturn( ccc );
		Mockito.when( ccc.withName( Mockito.anyString())).thenReturn( ccc );

		Mockito.when( this.dockerClient.createContainerCmd( Mockito.anyString())).thenReturn( ccc );
		CreateContainerResponse containerResponse = Mockito.mock( CreateContainerResponse.class );
		Mockito.when( ccc.exec()).thenReturn( containerResponse );
		Mockito.when( containerResponse.getId()).thenReturn( "cid" );

		StartContainerCmd scc = Mockito.mock( StartContainerCmd.class );
		Mockito.when( this.dockerClient.startContainerCmd( Mockito.anyString())).thenReturn( scc );

		// Create the container (mock)
		final String imageId = "toto";
		this.configurator.createContainer( imageId );

		// Check the client
		Mockito.verify( this.dockerClient ).createContainerCmd( imageId );
		Mockito.verify( this.dockerClient ).startContainerCmd( "cid" );
		Mockito.verifyNoMoreInteractions( this.dockerClient );

		// Check the user data
		Assert.assertEquals( 1, this.containerIdToVolume.size());
		File dir = this.containerIdToVolume.values().iterator().next();
		Assert.assertTrue( dir.isDirectory());
		Assert.assertTrue( new File( dir, USER_DATA_FILE ).isFile());

		// Check the creation request
		Mockito.verify( ccc ).withName( Mockito.anyString());

		ArgumentCaptor<List> env = ArgumentCaptor.forClass( List.class );
		Mockito.verify( ccc ).withEnv( env.capture());

		List<String> effectiveEnv = env.getValue();
		Assert.assertEquals( 2, effectiveEnv.size());
		Assert.assertEquals( "RBCF_VERSION=latest", effectiveEnv.get( 0 ));
		Assert.assertEquals( "AGENT_PARAMETERS=file:" + USER_DATA_DIR + USER_DATA_FILE, effectiveEnv.get( 1 ));

		// Volumes
		ArgumentCaptor<List> volumes = ArgumentCaptor.forClass( List.class );
		Mockito.verify( ccc ).withVolumes( volumes.capture());

		List<Volume> effectiveVolumes = volumes.getValue();
		Assert.assertNotNull( effectiveVolumes );
		Assert.assertEquals( 1, effectiveVolumes.size());

		Volume effectiveVolume = effectiveVolumes.get( 0 );
		Assert.assertNotNull( effectiveVolume );
		Assert.assertEquals( USER_DATA_DIR, effectiveVolume.getPath());

		// Bounds
		ArgumentCaptor<List> bounds = ArgumentCaptor.forClass( List.class );
		Mockito.verify( ccc ).withBinds( bounds.capture());

		List<Bind> effectiveBounds = bounds.getValue();
		Assert.assertNotNull( effectiveBounds );
		Assert.assertEquals( 1, effectiveBounds.size());

		Bind effectiveBind = effectiveBounds.get( 0 );
		Assert.assertNotNull( effectiveBind );
		Assert.assertEquals( dir.getAbsolutePath(), effectiveBind.getPath());
		Assert.assertEquals( effectiveVolume, effectiveBind.getVolume());
	}


	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void testCreateContainer_withEnv() throws Exception {

		CreateContainerCmd ccc = Mockito.mock( CreateContainerCmd.class );
		Mockito.when( ccc.withEnv( Mockito.anyListOf( String.class ))).thenReturn( ccc );
		Mockito.when( ccc.withBinds( Mockito.anyListOf( Bind.class ))).thenReturn( ccc );
		Mockito.when( ccc.withVolumes( Mockito.anyListOf( Volume.class ))).thenReturn( ccc );
		Mockito.when( ccc.withName( Mockito.anyString())).thenReturn( ccc );

		Mockito.when( this.dockerClient.createContainerCmd( Mockito.anyString())).thenReturn( ccc );
		CreateContainerResponse containerResponse = Mockito.mock( CreateContainerResponse.class );
		Mockito.when( ccc.exec()).thenReturn( containerResponse );
		Mockito.when( containerResponse.getId()).thenReturn( "cid" );

		StartContainerCmd scc = Mockito.mock( StartContainerCmd.class );
		Mockito.when( this.dockerClient.startContainerCmd( Mockito.anyString())).thenReturn( scc );

		// Prepare the parameters
		final String imageId = "toto";
		this.configurator.getParameters().getMessagingProperties().put( MessagingConstants.MESSAGING_TYPE_PROPERTY, "bird" );

		this.configurator.getParameters().getTargetProperties().put( OPTION_PREFIX_ENV + "t1", "v1" );
		this.configurator.getParameters().getTargetProperties().put( OPTION_PREFIX_ENV + "t2", "<application-name>" );
		this.configurator.getParameters().getTargetProperties().put( OPTION_PREFIX_ENV + "t3", "<application-name>_2" );
		this.configurator.getParameters().getTargetProperties().put( OPTION_PREFIX_ENV + "t4", "<scoped-instance-path>" );
		this.configurator.getParameters().getTargetProperties().put( OPTION_PREFIX_ENV + "t5", "<scoped-messaging_type>" );

		// Create the container (mock)
		this.configurator.createContainer( imageId );

		// Check the client
		Mockito.verify( this.dockerClient ).createContainerCmd( imageId );
		Mockito.verify( this.dockerClient ).startContainerCmd( "cid" );
		Mockito.verifyNoMoreInteractions( this.dockerClient );

		// Check the user data
		Assert.assertEquals( 1, this.containerIdToVolume.size());
		File dir = this.containerIdToVolume.values().iterator().next();
		Assert.assertTrue( dir.isDirectory());
		Assert.assertTrue( new File( dir, USER_DATA_FILE ).isFile());

		// Check the creation request
		Mockito.verify( ccc ).withName( Mockito.anyString());

		ArgumentCaptor<List> env = ArgumentCaptor.forClass( List.class );
		Mockito.verify( ccc ).withEnv( env.capture());

		List<String> effectiveEnv = env.getValue();
		Assert.assertEquals( 7, effectiveEnv.size());
		Assert.assertEquals( "t1=v1", effectiveEnv.get( 0 ));
		Assert.assertEquals( "t2=applicationName", effectiveEnv.get( 1 ));
		Assert.assertEquals( "t3=applicationName_2", effectiveEnv.get( 2 ));
		Assert.assertEquals( "t4=/vm", effectiveEnv.get( 3 ));
		Assert.assertEquals( "t5=bird", effectiveEnv.get( 4 ));
		Assert.assertEquals( "RBCF_VERSION=latest", effectiveEnv.get( 5 ));
		Assert.assertEquals( "AGENT_PARAMETERS=file:" + USER_DATA_DIR + USER_DATA_FILE, effectiveEnv.get( 6 ));

		// Volumes
		ArgumentCaptor<List> volumes = ArgumentCaptor.forClass( List.class );
		Mockito.verify( ccc ).withVolumes( volumes.capture());

		List<Volume> effectiveVolumes = volumes.getValue();
		Assert.assertNotNull( effectiveVolumes );
		Assert.assertEquals( 1, effectiveVolumes.size());

		Volume effectiveVolume = effectiveVolumes.get( 0 );
		Assert.assertNotNull( effectiveVolume );
		Assert.assertEquals( USER_DATA_DIR, effectiveVolume.getPath());

		// Bounds
		ArgumentCaptor<List> bounds = ArgumentCaptor.forClass( List.class );
		Mockito.verify( ccc ).withBinds( bounds.capture());

		List<Bind> effectiveBounds = bounds.getValue();
		Assert.assertNotNull( effectiveBounds );
		Assert.assertEquals( 1, effectiveBounds.size());

		Bind effectiveBind = effectiveBounds.get( 0 );
		Assert.assertNotNull( effectiveBind );
		Assert.assertEquals( dir.getAbsolutePath(), effectiveBind.getPath());
		Assert.assertEquals( effectiveVolume, effectiveBind.getVolume());
	}


	@Test
	public void testCreateContainer_loggedWarnings() throws Exception {

		CreateContainerCmd ccc = Mockito.mock( CreateContainerCmd.class );
		Mockito.when( ccc.withEnv( Mockito.anyListOf( String.class ))).thenReturn( ccc );
		Mockito.when( ccc.withBinds( Mockito.anyListOf( Bind.class ))).thenReturn( ccc );
		Mockito.when( ccc.withVolumes( Mockito.anyListOf( Volume.class ))).thenReturn( ccc );
		Mockito.when( ccc.withName( Mockito.anyString())).thenReturn( ccc );

		CreateContainerResponse containerResponse = Mockito.mock( CreateContainerResponse.class );
		Mockito.when( this.dockerClient.createContainerCmd( Mockito.anyString())).thenReturn( ccc );
		Mockito.when( ccc.exec()).thenReturn( containerResponse );
		Mockito.when( containerResponse.getId()).thenReturn( "cid" );

		StartContainerCmd scc = Mockito.mock( StartContainerCmd.class );
		Mockito.when( this.dockerClient.startContainerCmd( Mockito.anyString())).thenReturn( scc );

		this.configurator.logger = Mockito.mock( Logger.class );
		Mockito.when( this.configurator.logger.isLoggable( Level.FINE )).thenReturn( true );
		Mockito.when( containerResponse.getWarnings()).thenReturn( new String[]{ "Not good.", "Stay well." });

		// Create the container (mock)
		final String imageId = "toto";
		this.configurator.createContainer( imageId );

		// Check the client
		Mockito.verify( this.dockerClient ).createContainerCmd( imageId );
		Mockito.verify( this.dockerClient ).startContainerCmd( "cid" );
		Mockito.verifyNoMoreInteractions( this.dockerClient );

		// Check the logs were correctly handled
		Mockito.verify( containerResponse, Mockito.times( 3 )).getWarnings();
		Mockito.verify( this.configurator.logger ).fine( "The following warnings have been found.\nNot good.\nStay well." );
	}


	@Test( expected = TargetException.class )
	public void testCreateContainer_exception() throws Exception {

		CreateContainerCmd ccc = Mockito.mock( CreateContainerCmd.class );
		Mockito.when( ccc.withEnv( Mockito.anyListOf( String.class ))).thenReturn( ccc );
		Mockito.when( ccc.withName( Mockito.anyString())).thenReturn( ccc );

		Mockito.when( this.dockerClient.createContainerCmd( Mockito.anyString())).thenThrow( new RuntimeException( "for test" ));

		final String imageId = "toto";
		this.configurator.createContainer( imageId );
	}


	@Test
	public void testCreateImage_noGenerateFrom() throws Exception {

		final String imageId = "toto";
		this.configurator.createImage( imageId );
		Mockito.verifyZeroInteractions( this.dockerClient );
	}


	@Test
	public void testCreateImage_noTargetDirectory() throws Exception {

		final String imageId = "toto";
		this.configurator.getParameters().getTargetProperties().put( GENERATE_IMAGE_FROM, "img" );
		this.configurator.createImage( imageId );
		Mockito.verifyZeroInteractions( this.dockerClient );
	}


	@Test( expected = TargetException.class )
	public void testCreateImage_invalidDockerfilePath() throws Exception {

		final String imageId = "toto";
		this.configurator.getParameters().setTargetPropertiesDirectory( this.folder.newFolder());
		this.configurator.getParameters().getTargetProperties().put( GENERATE_IMAGE_FROM, "img" );

		this.configurator.createImage( imageId );
	}


	@Test
	public void testCreateImage_success() throws Exception {

		// Prepare the mocks
		BuildImageCmd buildImageCmd = Mockito.mock( BuildImageCmd.class );
		Mockito.when( this.dockerClient.buildImageCmd( Mockito.any( File.class ))).thenReturn( buildImageCmd );

		Mockito.when( buildImageCmd.withPull( true )).thenReturn( buildImageCmd );
		Mockito.when( buildImageCmd.withTags( Mockito.anySetOf( String.class ))).thenReturn( buildImageCmd );

		RoboconfBuildImageResultCallback cb = Mockito.mock( RoboconfBuildImageResultCallback.class );
		Mockito.when( buildImageCmd.exec( Mockito.any( RoboconfBuildImageResultCallback.class ))).thenReturn( cb );
		Mockito.when( cb.awaitImageId()).thenReturn( "my-img-id" );

		// Create the container (mock)
		final String imageId = "toto";
		this.configurator.getParameters().setTargetPropertiesDirectory( this.folder.newFolder());
		this.configurator.getParameters().getTargetProperties().put( GENERATE_IMAGE_FROM, "img" );

		File dockerfileDir = new File( this.configurator.getParameters().getTargetPropertiesDirectory(), "img" );
		Utils.createDirectory( dockerfileDir );

		this.configurator.createImage( imageId );

		// Check the client
		Mockito.verify( this.dockerClient, Mockito.only()).buildImageCmd( dockerfileDir );
		Mockito.verify( buildImageCmd ).withPull( true );
		Mockito.verify( buildImageCmd ).withTags( new HashSet<>( Arrays.asList( imageId )));
		Mockito.verify( buildImageCmd ).exec( Mockito.any( RoboconfBuildImageResultCallback.class ));
		Mockito.verifyNoMoreInteractions( buildImageCmd );
		Mockito.verify( cb, Mockito.only()).awaitImageId();
	}


	@Test( expected = TargetException.class )
	public void testCreateImage_error() throws Exception {

		// Prepare the mocks
		Mockito.when( this.dockerClient.buildImageCmd( Mockito.any( File.class ))).thenThrow( new RuntimeException( "for test" ));

		// Create the container (mock)
		final String imageId = "toto";
		this.configurator.getParameters().setTargetPropertiesDirectory( this.folder.newFolder());
		this.configurator.getParameters().getTargetProperties().put( GENERATE_IMAGE_FROM, "img" );

		File dockerfileDir = new File( this.configurator.getParameters().getTargetPropertiesDirectory(), "img" );
		Utils.createDirectory( dockerfileDir );

		this.configurator.createImage( imageId );
	}
}
