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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import net.roboconf.target.api.TargetException;
import net.roboconf.target.docker.internal.DockerMachineConfigurator.RoboconfBuildImageResultCallback;
import net.roboconf.target.docker.internal.DockerMachineConfigurator.RoboconfPullImageResultCallback;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.BuildImageCmd;
import com.github.dockerjava.api.command.ListImagesCmd;
import com.github.dockerjava.api.command.PullImageCmd;
import com.github.dockerjava.api.model.Image;

/**
 * @author Vincent Zurczak - Linagora
 */
public class DockerMachineConfiguratorTest {

	private DockerfileGenerator dockerfileGenerator;
	private DockerClient dockerClient;

	private DockerMachineConfigurator configurator;
	private Map<String,String> targetProperties;


	@Before
	public void prepareConfigurator() {

		this.targetProperties = new HashMap<> ();
		this.configurator = new DockerMachineConfigurator(
				this.targetProperties,
				new HashMap<String,String>( 0 ),
				"machineId", "scopedInstancePath", "applicationName", null );

		this.configurator = Mockito.spy( this.configurator );
		this.configurator.dockerClient = Mockito.mock( DockerClient.class );
		this.dockerClient = this.configurator.dockerClient;

		this.dockerfileGenerator = Mockito.mock( DockerfileGenerator.class );
		Mockito.when( this.configurator.dockerfileGenerator(
				Mockito.anyString(),
				Mockito.anyString(),
				Mockito.anyListOf( String.class ),
				Mockito.anyString())).thenReturn( this.dockerfileGenerator );
	}


	@Test
	public void testFixImageId() {

		Assert.assertEquals(
				DockerMachineConfigurator.DEFAULT_IMG_NAME,
				DockerMachineConfigurator.fixImageId( null ));

		Assert.assertEquals(
				DockerMachineConfigurator.DEFAULT_IMG_NAME,
				DockerMachineConfigurator.fixImageId( " " ));

		Assert.assertEquals(
				"test1",
				DockerMachineConfigurator.fixImageId( "test1" ));
	}


	@Test
	public void testClose() throws Exception {

		Mockito.verifyZeroInteractions( this.dockerfileGenerator );
		Mockito.verifyZeroInteractions( this.dockerClient );
		this.configurator.close();
		Mockito.verify( this.dockerClient, Mockito.times( 1 )).close();
	}


	@Test
	public void testCreateImage_noDownload_noBaseImage_withAgentPackage() throws Exception {

		// No base image, agent URL specified, no download required.
		// => no error.
		// => no request to download an image.
		this.targetProperties.put( DockerHandler.AGENT_PACKAGE_URL, "file://somewhere" );

		BuildImageCmd buildImageCmd = Mockito.mock( BuildImageCmd.class );
		Mockito.when( this.dockerClient.buildImageCmd( Mockito.any( File.class ))).thenReturn( buildImageCmd );
		Mockito.when( buildImageCmd.withTag( Mockito.anyString())).thenReturn( buildImageCmd );

		RoboconfBuildImageResultCallback callback = Mockito.mock( RoboconfBuildImageResultCallback.class );
		Mockito.when( buildImageCmd.exec( Mockito.any( RoboconfBuildImageResultCallback.class ))).thenReturn( callback );
		Mockito.when( callback.awaitImageId()).thenReturn( "xxxx" );

		this.configurator.createImage( "invalid" );
		Mockito.verify( this.dockerClient, Mockito.never()).pullImageCmd( Mockito.anyString());
	}


	@Test( expected = TargetException.class )
	public void testCreateImage_withErrorOnCreation() throws Exception {

		// No base image, agent URL specified, no download required.
		// => no error.
		// => no request to download an image.
		this.targetProperties.put( DockerHandler.AGENT_PACKAGE_URL, "file://somewhere" );

		BuildImageCmd buildImageCmd = Mockito.mock( BuildImageCmd.class );
		Mockito.when( this.dockerClient.buildImageCmd( Mockito.any( File.class ))).thenReturn( buildImageCmd );
		Mockito.when( buildImageCmd.withTag( Mockito.anyString())).thenReturn( buildImageCmd );

		RoboconfBuildImageResultCallback callback = Mockito.mock( RoboconfBuildImageResultCallback.class );
		Mockito.when( buildImageCmd.exec( Mockito.any( RoboconfBuildImageResultCallback.class ))).thenReturn( callback );

		// We simply simulate the occurrence of an exception at the end.
		Mockito.when( callback.awaitImageId()).thenThrow( new RuntimeException( "for test" ));

		this.configurator.createImage( "invalid" );
	}


	@Test
	public void testCreateImage_noDownload_withBaseImage_notFound_withAgentPackage() throws Exception {

		// A base image and the agent URL are specified, no download required.
		// The image is not found => error.
		this.targetProperties.put( DockerHandler.AGENT_PACKAGE_URL, "file://somewhere" );
		this.targetProperties.put( DockerHandler.BASE_IMAGE, "something" );
		this.targetProperties.put( DockerHandler.DOWNLOAD_BASE_IMAGE, "false" );

		// This section deals with the (successful) creation of an image.
		BuildImageCmd buildImageCmd = Mockito.mock( BuildImageCmd.class );
		Mockito.when( this.dockerClient.buildImageCmd( Mockito.any( File.class ))).thenReturn( buildImageCmd );
		Mockito.when( buildImageCmd.withTag( Mockito.anyString())).thenReturn( buildImageCmd );

		RoboconfBuildImageResultCallback callback = Mockito.mock( RoboconfBuildImageResultCallback.class );
		Mockito.when( buildImageCmd.exec( Mockito.any( RoboconfBuildImageResultCallback.class ))).thenReturn( callback );
		Mockito.when( callback.awaitImageId()).thenReturn( "xxxx" );
		// End of section

		// Make sure the image is not found.
		ListImagesCmd listImagesCmd = Mockito.mock( ListImagesCmd.class );
		Mockito.when( this.dockerClient.listImagesCmd()).thenReturn( listImagesCmd );
		Mockito.when( listImagesCmd.exec()).thenReturn( new ArrayList<Image>( 0 ));

		try {
			this.configurator.createImage( "invalid" );
			Assert.fail( "Creation should have failed. The base image was not supposed to be found." );

		} catch( Exception e ) {
			// nothing
		}

		// We did not try to download anything
		Mockito.verify( this.dockerClient, Mockito.never()).pullImageCmd( Mockito.anyString());
	}


	@Test
	public void testCreateImage_withDownload_withBaseImage_notFound_withAgentPackage() throws Exception {

		// A base image and the agent URL are specified, download is required.
		// By the end, the image will not be found. => error.
		this.targetProperties.put( DockerHandler.AGENT_PACKAGE_URL, "file://somewhere" );
		this.targetProperties.put( DockerHandler.BASE_IMAGE, "something" );
		this.targetProperties.put( DockerHandler.DOWNLOAD_BASE_IMAGE, "true" );

		// This section deals with the (successful) creation of an image.
		BuildImageCmd buildImageCmd = Mockito.mock( BuildImageCmd.class );
		Mockito.when( this.dockerClient.buildImageCmd( Mockito.any( File.class ))).thenReturn( buildImageCmd );
		Mockito.when( buildImageCmd.withTag( Mockito.anyString())).thenReturn( buildImageCmd );

		RoboconfBuildImageResultCallback callback = Mockito.mock( RoboconfBuildImageResultCallback.class );
		Mockito.when( buildImageCmd.exec( Mockito.any( RoboconfBuildImageResultCallback.class ))).thenReturn( callback );
		Mockito.when( callback.awaitImageId()).thenReturn( "xxxx" );
		// End of section

		// Make sure the image is not found.
		ListImagesCmd listImagesCmd = Mockito.mock( ListImagesCmd.class );
		Mockito.when( this.dockerClient.listImagesCmd()).thenReturn( listImagesCmd );
		Mockito.when( listImagesCmd.exec()).thenReturn( new ArrayList<Image>( 0 ));

		// Make sure there is no error thrown by the pull request.
		ArgumentCaptor<String> tagArg = ArgumentCaptor.forClass( String.class );
		ArgumentCaptor<String> imageNameArg = ArgumentCaptor.forClass( String.class );

		PullImageCmd pullImageCmd = Mockito.mock( PullImageCmd.class );
		Mockito.when( this.dockerClient.pullImageCmd( imageNameArg.capture())).thenReturn( pullImageCmd );
		Mockito.when( pullImageCmd.withRegistry( Mockito.anyString())).thenReturn( pullImageCmd );
		Mockito.when( pullImageCmd.withTag( tagArg.capture())).thenReturn( pullImageCmd );

		RoboconfPullImageResultCallback pullCallback = Mockito.mock( RoboconfPullImageResultCallback.class );
		Mockito.when( pullImageCmd.exec( Mockito.any( RoboconfPullImageResultCallback.class ))).thenReturn( pullCallback );

		// Execution
		try {
			this.configurator.createImage( "invalid" );
			Assert.fail( "Creation should have failed. The base image was not supposed to be found." );

		} catch( Exception e ) {
			// nothing
		}

		// We tried to download something
		Mockito.verify( this.dockerClient, Mockito.times( 1 )).pullImageCmd( Mockito.anyString());
		Assert.assertEquals( "something", imageNameArg.getValue());
		Assert.assertEquals( 0, tagArg.getAllValues().size());

		// Try with an image name using a tag
		this.targetProperties.put( DockerHandler.BASE_IMAGE, "something:else" );
		try {
			this.configurator.createImage( "invalid" );
			Assert.fail( "Creation should have failed. The base image was not supposed to be found." );

		} catch( Exception e ) {
			// nothing
		}

		// We tried to download something
		Mockito.verify( this.dockerClient, Mockito.times( 2 )).pullImageCmd( Mockito.anyString());
		Assert.assertEquals( "something", imageNameArg.getValue());
		Assert.assertEquals( "else", tagArg.getValue());
	}


	@Test( expected = TargetException.class )
	public void testCreateImage_withDownload_withBaseImage_withAgentPackage_withErrorOnPull() throws Exception {

		// A base image and the agent URL are specified, download is required.
		// By the end, the image will not be found. => error.
		this.targetProperties.put( DockerHandler.AGENT_PACKAGE_URL, "file://somewhere" );
		this.targetProperties.put( DockerHandler.BASE_IMAGE, "something" );
		this.targetProperties.put( DockerHandler.DOWNLOAD_BASE_IMAGE, "true" );

		// Make sure the image is not found.
		ListImagesCmd listImagesCmd = Mockito.mock( ListImagesCmd.class );
		Mockito.when( this.dockerClient.listImagesCmd()).thenReturn( listImagesCmd );
		Mockito.when( listImagesCmd.exec()).thenReturn( new ArrayList<Image>( 0 ));

		// Normal flow for the pull request.
		ArgumentCaptor<String> tagArg = ArgumentCaptor.forClass( String.class );
		ArgumentCaptor<String> imageNameArg = ArgumentCaptor.forClass( String.class );

		PullImageCmd pullImageCmd = Mockito.mock( PullImageCmd.class );
		Mockito.when( this.dockerClient.pullImageCmd( imageNameArg.capture())).thenReturn( pullImageCmd );
		Mockito.when( pullImageCmd.withRegistry( Mockito.anyString())).thenReturn( pullImageCmd );
		Mockito.when( pullImageCmd.withTag( tagArg.capture())).thenReturn( pullImageCmd );

		// Execution => exception.
		Mockito.when( pullImageCmd.exec( Mockito.any( RoboconfPullImageResultCallback.class ))).thenThrow( new RuntimeException( "for test" ));

		this.configurator.createImage( "invalid" );
	}
}
