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

package net.roboconf.target.embedded.internal;

import static net.roboconf.target.embedded.internal.EmbeddedHandler.AGENT_APPLICATION_NAME;
import static net.roboconf.target.embedded.internal.EmbeddedHandler.AGENT_DOMAIN;
import static net.roboconf.target.embedded.internal.EmbeddedHandler.AGENT_PARAMETERS;
import static net.roboconf.target.embedded.internal.EmbeddedHandler.AGENT_SCOPED_INSTANCE_PATH;
import static net.roboconf.target.embedded.internal.EmbeddedHandler.DEFAULT_SCP_AGENT_CONFIG_DIR;
import static net.roboconf.target.embedded.internal.EmbeddedHandler.IP_ADDRESSES;
import static net.roboconf.target.embedded.internal.EmbeddedHandler.TARGET_ID;
import static net.roboconf.target.embedded.internal.EmbeddedHandler.USER_DATA_FILE;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import net.roboconf.core.Constants;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.utils.Utils;
import net.roboconf.target.api.TargetException;
import net.roboconf.target.api.TargetHandlerParameters;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.xfer.FileSystemFile;
import net.schmizz.sshj.xfer.LocalSourceFile;
import net.schmizz.sshj.xfer.scp.SCPFileTransfer;

/**
 * @author Vincent Zurczak - Linagora
 */
public class EmbeddedHandlerTest {

	private static final String NOTHING = "app_nothing";

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();


	@Test
	public void testTargetEmbedded_noIpPool() throws Exception {

		// Basics
		EmbeddedHandler target = new EmbeddedHandler();
		Assert.assertEquals( TARGET_ID, target.getTargetId());

		// Terminate machine should not throw any error at this stage
		TargetHandlerParameters parameters = new TargetHandlerParameters();
		parameters.setTargetProperties( new HashMap<String,String>( 0 ));
		parameters.setMessagingProperties( new HashMap<String,String>( 0 ));
		parameters = parameters
				.applicationName( "app" )
				.domain( "domain" )
				.scopedInstancePath( "nothing" );

		target.terminateMachine( parameters, null );
		target.terminateMachine( parameters, "anything" );

		// Same thing for isMachineRunning
		Assert.assertFalse( target.isMachineRunning( null, NOTHING ));

		// Let's try to create a machine
		String machineId = target.createMachine( parameters );
		Assert.assertEquals( NOTHING, machineId );
		Assert.assertTrue( target.isMachineRunning( null, machineId ));
		Assert.assertEquals( 0, target.usedIps.size());
		Assert.assertEquals( 1, target.machineIdToIp.size());
		Assert.assertEquals( "", target.machineIdToIp.get( machineId ));

		// And let's configure it
		Instance scopedInstance = new Instance();
		Assert.assertEquals( 0, scopedInstance.data.size());
		target.configureMachine(
				parameters,
				machineId,
				scopedInstance );

		Assert.assertTrue( scopedInstance.data.containsKey( Instance.READY_FOR_CFG_MARKER ));

		// Terminate it
		target.terminateMachine( parameters, machineId );
		Assert.assertFalse( target.isMachineRunning( null, machineId ));
		Assert.assertEquals( 0, target.usedIps.size());
		Assert.assertEquals( 0, target.machineIdToIp.size());
	}


	@Test
	public void testTargetEmbedded_withIpPool() throws Exception {
		EmbeddedHandler target = new EmbeddedHandler();

		// Terminate machine should not throw any error at this stage
		TargetHandlerParameters parameters = new TargetHandlerParameters();
		parameters.setTargetProperties( new HashMap<String,String>( 0 ));
		parameters.getTargetProperties().put( IP_ADDRESSES, "192.168.1.1, 192.168.1.2" );
		parameters.setMessagingProperties( new HashMap<String,String>( 0 ));
		parameters = parameters
				.applicationName( "app" )
				.domain( "domain" )
				.scopedInstancePath( "nothing" );

		target.terminateMachine( parameters, null );
		target.terminateMachine( parameters, "anything" );

		// Same thing for isMachineRunning
		Assert.assertFalse( target.isMachineRunning( null, NOTHING ));

		// Let's try to create a machine
		String machineId = target.createMachine( parameters );
		Assert.assertEquals( NOTHING, machineId );
		Assert.assertTrue( target.isMachineRunning( null, machineId ));
		Assert.assertEquals( 1, target.usedIps.size());
		Assert.assertTrue( target.usedIps.containsKey( "192.168.1.1" ));
		Assert.assertEquals( 1, target.machineIdToIp.size());
		Assert.assertEquals( "192.168.1.1", target.machineIdToIp.get( machineId ));

		// No configuration here...

		// Terminate it
		target.terminateMachine( parameters, machineId );
		Assert.assertFalse( target.isMachineRunning( null, machineId ));
		Assert.assertEquals( 0, target.usedIps.size());
		Assert.assertEquals( 0, target.machineIdToIp.size());
	}


	@Test( expected = TargetException.class )
	public void testIpList_noMoreIpAvailable() throws Exception {

		Map<String,String> targetProperties = new HashMap<> ();
		targetProperties.put( IP_ADDRESSES, "192.168.1.1, 192.168.1.2" );

		EmbeddedHandler target = new EmbeddedHandler();
		try {
			Assert.assertNotNull( target.createMachine( new TargetHandlerParameters()
				.applicationName( "app" )
				.domain( "domain" )
				.scopedInstancePath( "nothing1" )
				.targetProperties( targetProperties )));

			Assert.assertNotNull( target.createMachine( new TargetHandlerParameters()
					.applicationName( "app" )
					.domain( "domain" )
					.scopedInstancePath( "nothing2" )
					.targetProperties( targetProperties )));

		} catch( Exception e ) {
			Assert.fail( "No exception should have been thrown here." );
		}

		Assert.assertNotNull( target.createMachine( new TargetHandlerParameters()
				.applicationName( "app" )
				.domain( "domain" )
				.scopedInstancePath( "nothing3" )
				.targetProperties( targetProperties )));
	}


	@Test
	public void testUpdateAgentConfigurationFile() throws Exception {

		// Prepare the mocks
		File tmpDir = this.folder.newFolder();
		File agentConfigurationFile = new File( tmpDir, Constants.KARAF_CFG_FILE_AGENT );

		Properties props = new Properties();
		props.setProperty( "a0", "c0" );
		props.setProperty( "a1", "c213" );
		props.setProperty( "a2", "c2" );
		props.setProperty( "a3", "c3" );
		props.setProperty( "a4", "c4" );
		props.setProperty( "a5", "c5" );
		Utils.writePropertiesFile( props, agentConfigurationFile );

		TargetHandlerParameters parameters = new TargetHandlerParameters()
				.targetProperties( new HashMap<String,String>( 0 ));

		Map<String,String> keyToNewValue = new HashMap<> ();
		keyToNewValue.put( "a1", "b1" );
		keyToNewValue.put( "a2", "b2" );
		keyToNewValue.put( "a3", "b3" );

		SSHClient ssh = Mockito.mock( SSHClient.class );
		SCPFileTransfer scp = Mockito.mock( SCPFileTransfer.class );
		Mockito.when( ssh.newSCPFileTransfer()).thenReturn( scp );

		// Invoke the method
		EmbeddedHandler target = new EmbeddedHandler();
		target.updateAgentConfigurationFile( parameters, ssh, tmpDir, keyToNewValue );

		// Verify
		ArgumentCaptor<String> remotePathCaptor = ArgumentCaptor.forClass( String.class );
		ArgumentCaptor<FileSystemFile> fileCaptor = ArgumentCaptor.forClass( FileSystemFile.class );
		Mockito.verify( scp ).download( remotePathCaptor.capture(), fileCaptor.capture());

		Assert.assertEquals( tmpDir, fileCaptor.getValue().getFile());
		Assert.assertEquals(
				new File( DEFAULT_SCP_AGENT_CONFIG_DIR, Constants.KARAF_CFG_FILE_AGENT ).getAbsolutePath(),
				remotePathCaptor.getValue());

		// We reupload the same file than the one we downloaded
		ArgumentCaptor<String> remotePathCaptor2 = ArgumentCaptor.forClass( String.class );
		ArgumentCaptor<FileSystemFile> fileCaptor2 = ArgumentCaptor.forClass( FileSystemFile.class );
		Mockito.verify( scp ).upload( fileCaptor2.capture(), remotePathCaptor2.capture());

		Assert.assertEquals( agentConfigurationFile.getAbsolutePath(), fileCaptor2.getValue().getFile().getAbsolutePath());
		Assert.assertEquals( DEFAULT_SCP_AGENT_CONFIG_DIR, remotePathCaptor2.getValue());

		// And no additional call
		Mockito.verifyNoMoreInteractions( scp );
		Mockito.verify( ssh, Mockito.times( 2 )).newSCPFileTransfer();
		Mockito.verifyNoMoreInteractions( ssh );

		// Verify the properties were correctly updated in the file
		Properties readProps = Utils.readPropertiesFile( agentConfigurationFile );
		Assert.assertEquals( "c0", readProps.get( "a0" ));
		Assert.assertEquals( "b1", readProps.get( "a1" ));
		Assert.assertEquals( "b2", readProps.get( "a2" ));
		Assert.assertEquals( "b3", readProps.get( "a3" ));
		Assert.assertEquals( "c4", readProps.get( "a4" ));
		Assert.assertEquals( "c5", readProps.get( "a5" ));
		Assert.assertEquals( props.size(), readProps.size());
	}


	@Test
	public void testEraseConfiguration() throws Exception {

		// Prepare the mocks
		File tmpDir = this.folder.newFolder();
		File agentConfigurationFile = new File( tmpDir, Constants.KARAF_CFG_FILE_AGENT );

		Properties props = new Properties();
		props.setProperty( AGENT_PARAMETERS, "/tmp/somewhere" );
		props.setProperty( AGENT_APPLICATION_NAME, "my app" );
		props.setProperty( "a2", "c2" );
		props.setProperty( AGENT_SCOPED_INSTANCE_PATH, "/vm 1" );
		props.setProperty( "a4", "c4" );
		props.setProperty( AGENT_DOMAIN, "d" );
		Utils.writePropertiesFile( props, agentConfigurationFile );

		TargetHandlerParameters parameters = new TargetHandlerParameters()
				.targetProperties( new HashMap<String,String>( 0 ));

		SSHClient ssh = Mockito.mock( SSHClient.class );
		SCPFileTransfer scp = Mockito.mock( SCPFileTransfer.class );
		Mockito.when( ssh.newSCPFileTransfer()).thenReturn( scp );

		// Invoke the method
		EmbeddedHandler target = new EmbeddedHandler();
		target.eraseConfiguration( parameters, ssh, tmpDir );

		// Verify the properties were correctly updated in the file
		Properties readProps = Utils.readPropertiesFile( agentConfigurationFile );
		Assert.assertEquals( "", readProps.get( AGENT_PARAMETERS ));
		Assert.assertEquals( "", readProps.get( AGENT_APPLICATION_NAME ));
		Assert.assertEquals( "c2", readProps.get( "a2" ));
		Assert.assertEquals( "", readProps.get( AGENT_SCOPED_INSTANCE_PATH ));
		Assert.assertEquals( "c4", readProps.get( "a4" ));
		Assert.assertEquals( "", readProps.get( AGENT_DOMAIN ));
		Assert.assertEquals( props.size(), readProps.size());

		// We upload only one file
		Mockito.verify( scp, Mockito.times( 1 )).upload(
				Mockito.any( LocalSourceFile.class ),
				Mockito.anyString());
	}


	@Test
	public void testSetConfiguration() throws Exception {

		// Prepare the mocks
		File tmpDir = this.folder.newFolder();
		File agentConfigurationFile = new File( tmpDir, Constants.KARAF_CFG_FILE_AGENT );

		Properties props = new Properties();
		props.setProperty( AGENT_PARAMETERS, "/tmp/somewhere" );
		props.setProperty( AGENT_APPLICATION_NAME, "my app" );
		props.setProperty( "a2", "c2" );
		props.setProperty( AGENT_SCOPED_INSTANCE_PATH, "/vm 1" );
		props.setProperty( "a4", "c4" );
		props.setProperty( AGENT_DOMAIN, "d" );
		Utils.writePropertiesFile( props, agentConfigurationFile );

		TargetHandlerParameters parameters = new TargetHandlerParameters()
				.targetProperties( new HashMap<String,String>( 0 ));

		SSHClient ssh = Mockito.mock( SSHClient.class );
		SCPFileTransfer scp = Mockito.mock( SCPFileTransfer.class );
		Mockito.when( ssh.newSCPFileTransfer()).thenReturn( scp );

		// Invoke the method
		EmbeddedHandler target = new EmbeddedHandler();
		target.setConfiguration( parameters, ssh, tmpDir );

		// Verify the properties were correctly updated in the file
		Properties readProps = Utils.readPropertiesFile( agentConfigurationFile );
		Assert.assertEquals( "file:" + DEFAULT_SCP_AGENT_CONFIG_DIR + "/" + USER_DATA_FILE, readProps.get( AGENT_PARAMETERS ));
		Assert.assertEquals( "", readProps.get( AGENT_APPLICATION_NAME ));
		Assert.assertEquals( "c2", readProps.get( "a2" ));
		Assert.assertEquals( "", readProps.get( AGENT_SCOPED_INSTANCE_PATH ));
		Assert.assertEquals( "c4", readProps.get( "a4" ));
		Assert.assertEquals( "", readProps.get( AGENT_DOMAIN ));
		Assert.assertEquals( props.size(), readProps.size());

		// We made two uploads for this method!
		Mockito.verify( scp, Mockito.times( 2 )).upload(
				Mockito.any( LocalSourceFile.class ),
				Mockito.anyString());
	}
}
