/**
 * Copyright 2015 Linagora, Université Joseph Fourier, Floralis
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;
import net.roboconf.target.api.TargetException;

import org.junit.Test;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.Capability;

/**
 * @author Vincent Zurczak - Linagora
 */
public class DockerUtilsTest {

	@Test( expected = TargetException.class )
	public void testInvalidConfiguration_noImage_noGeneration() throws Exception {

		Map<String,String> map = new HashMap<> ();
		map.put( DockerHandler.ENDPOINT, "whatever" );
		DockerUtils.createDockerClient( map );
	}


	@Test
	public void testIncompleteConfiguration_noEndpoint_withImage() throws Exception {

		Map<String,String> map = new HashMap<> ();
		map.put( DockerHandler.IMAGE_ID, "whatever" );
		DockerClient client = DockerUtils.createDockerClient( map );
		Assert.assertNotNull( client );
	}


	@Test
	public void testIncompleteConfiguration_noEndpoint_withGeneration() throws Exception {

		Map<String,String> map = new HashMap<> ();
		map.put( DockerHandler.GENERATE_IMAGE, "true" );
		DockerClient client = DockerUtils.createDockerClient( map );
		Assert.assertNotNull( client );
	}


	@Test
	public void testPrepareParameter() throws Exception {

		// Known types
		String input = "whatever";
		Assert.assertEquals( input, DockerUtils.prepareParameter( input, String.class ));

		input = "50";
		Assert.assertEquals( 50, DockerUtils.prepareParameter( input, int.class ));
		Assert.assertEquals( 50L, DockerUtils.prepareParameter( input, long.class ));

		input = "true";
		Assert.assertEquals( true, DockerUtils.prepareParameter( input, boolean.class ));

		input = "test1, test2,test3,   test4  ";
		Object result = DockerUtils.prepareParameter( input, String[].class );
		Assert.assertEquals( String[].class, result.getClass());

		String[] expectedString = { "test1", "test2", "test3", "test4" };
		Assert.assertTrue( Arrays.deepEquals( expectedString, (String[]) result ));

		input = "AUDIT_CONTROL, SYS_PTRACE";
		Capability[] expectedCapabilities = new Capability[] {
				Capability.AUDIT_CONTROL,
				Capability.SYS_PTRACE
		};

		result = DockerUtils.prepareParameter( input, Capability[].class );
		Assert.assertEquals( Capability[].class, result.getClass());
		Assert.assertTrue( Arrays.deepEquals( expectedCapabilities, (Capability[]) result ));

		// Unknown types
		input = "0.4f";
		Assert.assertEquals( input, DockerUtils.prepareParameter( input, double.class ));
	}


	@Test( expected = TargetException.class )
	public void testPrepareParameter_invalidCapability() throws Exception {

		String input = "AUDIT_CONTROL, read minds";
		DockerUtils.prepareParameter( input, Capability[].class );
	}


	@Test
	public void testConfigureOptions() throws Exception {

		Map<String,String> options = new HashMap<> ();
		options.put( "cap-drop", "AUDIT_CONTROL" );
		options.put( "cap-add", "SYS_PTRACE, SYS_NICE" );
		options.put( "hostname", "hello" );
		options.put( "AttachStdin", "true" );
		options.put( "MemorySwap", "50" );
		options.put( "CpuShares", "2" );
		options.put( "Memory", "1024" );
		options.put( "env", "env1, env2, env3" );

		Map<String,String> targetProperties = new HashMap<>( 1 );
		targetProperties.put( DockerHandler.IMAGE_ID, "whatever" );

		DockerClient dockerClient = DockerUtils.createDockerClient( targetProperties );
		CreateContainerCmd cmd = dockerClient.createContainerCmd( "whatever, we won't execute it" );
		Assert.assertFalse( cmd.isAttachStdin());

		DockerUtils.configureOptions( options, cmd );

		Assert.assertTrue( cmd.isAttachStdin());
		Assert.assertEquals( "hello", cmd.getHostName());
		Assert.assertTrue( Arrays.deepEquals(
				new Capability[] { Capability.SYS_PTRACE, Capability.SYS_NICE },
				cmd.getCapAdd()));

		Assert.assertTrue( Arrays.deepEquals(
				new Capability[] { Capability.AUDIT_CONTROL },
				cmd.getCapDrop()));

		Assert.assertEquals( 50L, cmd.getMemorySwap());
		Assert.assertEquals( 1024, cmd.getMemoryLimit());
		Assert.assertEquals( 2, cmd.getCpuShares());
		Assert.assertTrue( Arrays.deepEquals(
				new String[] { "env1", "env2", "env3" },
				cmd.getEnv()));
	}


	@Test( expected = TargetException.class )
	public void testConfigureOptions_unknownOption() throws Exception {

		Map<String,String> options = new HashMap<> ();
		options.put( "what-is-this", "AUDIT_CONTROL" );

		Map<String,String> targetProperties = new HashMap<>( 1 );
		targetProperties.put( DockerHandler.IMAGE_ID, "whatever" );

		DockerClient dockerClient = DockerUtils.createDockerClient( targetProperties );
		CreateContainerCmd cmd = dockerClient.createContainerCmd( "whatever, we won't execute it" );

		DockerUtils.configureOptions( options, cmd );
	}


	@Test( expected = TargetException.class )
	public void testConfigureOptions_unsupportedOption() throws Exception {

		Map<String,String> options = new HashMap<> ();
		options.put( "ExposedPorts", "24, 25" );

		Map<String,String> targetProperties = new HashMap<>( 1 );
		targetProperties.put( DockerHandler.IMAGE_ID, "whatever" );

		DockerClient dockerClient = DockerUtils.createDockerClient( targetProperties );
		CreateContainerCmd cmd = dockerClient.createContainerCmd( "whatever, we won't execute it" );

		DockerUtils.configureOptions( options, cmd );
	}


	@Test
	public void testConfigureOptions_synonyms() throws Exception {

		Map<String,String> targetProperties = new HashMap<>( 1 );
		targetProperties.put( DockerHandler.IMAGE_ID, "whatever" );

		DockerClient dockerClient = DockerUtils.createDockerClient( targetProperties );
		CreateContainerCmd cmd = dockerClient.createContainerCmd( "whatever, we won't execute it" );

		// First try
		Map<String,String> options = new HashMap<>( 1 );
		options.put( "cap-drop", "AUDIT_CONTROL" );

		DockerUtils.configureOptions( options, cmd );
		Assert.assertTrue( Arrays.deepEquals(
				new Capability[] { Capability.AUDIT_CONTROL },
				cmd.getCapDrop()));

		// Second try
		options = new HashMap<>( 1 );
		options.put( "CapDrop", "SYS_PTRACE" );

		DockerUtils.configureOptions( options, cmd );
		Assert.assertTrue( Arrays.deepEquals(
				new Capability[] { Capability.SYS_PTRACE },
				cmd.getCapDrop()));

		// Third try
		options = new HashMap<>( 1 );
		options.put( "--cap-drop", "SYS_NICE" );

		DockerUtils.configureOptions( options, cmd );
		Assert.assertTrue( Arrays.deepEquals(
				new Capability[] { Capability.SYS_NICE },
				cmd.getCapDrop()));
	}


	@Test
	public void testParseRunExecLine() {

		Assert.assertNull( DockerUtils.parseRunExecLine( null ));
		Assert.assertNull( DockerUtils.parseRunExecLine( "" ));
		Assert.assertNull( DockerUtils.parseRunExecLine( "   " ));

		List<String> args = DockerUtils.parseRunExecLine( "[ toto, titi" );
		Assert.assertNull( args );

		args = DockerUtils.parseRunExecLine( "[ toto, titi ]" );
		Assert.assertEquals( 2, args.size());
		Assert.assertTrue( args.contains( "toto" ));
		Assert.assertTrue( args.contains( "titi" ));

		args = DockerUtils.parseRunExecLine( "[ \"toto\", \"titi\" ]" );
		Assert.assertEquals( 2, args.size());
		Assert.assertTrue( args.contains( "toto" ));
		Assert.assertTrue( args.contains( "titi" ));
	}


	@Test
	public void testBuildRunCommand_defaultValue_emptyMessaging() {

		List<String> args = DockerUtils.buildRunCommand( null, new HashMap<String,String>( 0 ), "app", "/root" );
		Assert.assertEquals( 5, args.size());
		Assert.assertTrue( args.get( 0 ).endsWith( "/start.sh" ));
		Assert.assertEquals( "etc/net.roboconf.messaging..cfg", args.get( 1 ));
		Assert.assertEquals( "agent.application-name=app", args.get( 2 ));
		Assert.assertEquals( "agent.scoped-instance-path=/root", args.get( 3 ));
		Assert.assertEquals( "agent.messaging-type=", args.get( 4 ));

		// No messaging parameter, so no sixth argument
	}


	@Test
	public void testBuildRunCommand_defaultValue_withMessaging() {

		Map<String,String> messagingConfiguration = new LinkedHashMap<> ();
		messagingConfiguration.put( DockerHandler.MESSAGING_TYPE, "bird" );
		messagingConfiguration.put( "paper", "with somethig written on it" );
		messagingConfiguration.put( "pen", "to write the answer" );

		List<String> args = DockerUtils.buildRunCommand( null, messagingConfiguration, "app", "/root" );
		Assert.assertEquals( 7, args.size());
		Assert.assertTrue( args.get( 0 ).endsWith( "/start.sh" ));
		Assert.assertEquals( "etc/net.roboconf.messaging.bird.cfg", args.get( 1 ));
		Assert.assertEquals( "agent.application-name=app", args.get( 2 ));
		Assert.assertEquals( "agent.scoped-instance-path=/root", args.get( 3 ));
		Assert.assertEquals( "agent.messaging-type=bird", args.get( 4 ));
		Assert.assertEquals( "msg.paper=with somethig written on it", args.get( 5 ));
		Assert.assertEquals( "msg.pen=to write the answer", args.get( 6 ));
	}


	@Test
	public void testBuildRunCommand_overriding_noCommand() {

		Map<String,String> messagingConfiguration = new LinkedHashMap<> ();
		messagingConfiguration.put( DockerHandler.MESSAGING_TYPE, "bird" );
		messagingConfiguration.put( "paper", "with somethig written on it" );
		messagingConfiguration.put( "pen", "to write the answer" );

		List<String> args = DockerUtils.buildRunCommand( "[]", messagingConfiguration, "app", "/root" );
		Assert.assertEquals( 0, args.size());
	}


	@Test
	public void testBuildRunCommand_overriding_extraCommand() {

		Map<String,String> messagingConfiguration = new LinkedHashMap<> ();
		messagingConfiguration.put( DockerHandler.MESSAGING_TYPE, "bird" );
		messagingConfiguration.put( "paper", "with somethig written on it" );
		messagingConfiguration.put( "pen", "to write the answer" );

		List<String> args = DockerUtils.buildRunCommand( "[ start.sh ]", messagingConfiguration, "app", "/root" );
		Assert.assertEquals( 1, args.size());
		Assert.assertEquals( "start.sh", args.get( 0 ));
	}


	@Test
	public void testBuildRunCommand_overriding_mixCommand() {

		Map<String,String> messagingConfiguration = new LinkedHashMap<> ();
		messagingConfiguration.put( DockerHandler.MESSAGING_TYPE, "bird" );
		messagingConfiguration.put( "paper", "with somethig written on it" );
		messagingConfiguration.put( "pen", "to write the answer" );

		List<String> args = DockerUtils.buildRunCommand(
				"[ \"agent.application-name=$applicationName$\", \"start.sh\", \"$messagingType$\" ]",
				messagingConfiguration, "app", "/root" );

		Assert.assertEquals( 3, args.size());
		Assert.assertEquals( "agent.application-name=app", args.get( 0 ));
		Assert.assertEquals( "start.sh", args.get( 1 ));
		Assert.assertEquals( "bird", args.get( 2 ));
	}


	@Test
	public void testBuildRunCommand_overriding_mixCommandAndMessaging() {

		Map<String,String> messagingConfiguration = new LinkedHashMap<> ();
		messagingConfiguration.put( DockerHandler.MESSAGING_TYPE, "bird" );
		messagingConfiguration.put( "paper", "with somethig written on it" );
		messagingConfiguration.put( "pen", "to write the answer" );

		List<String> args = DockerUtils.buildRunCommand(
				"[ \"$msgConfig$\", \"start.sh\", \"etc/net.roboconf.messaging.$messagingType$.cfg\" ]",
				messagingConfiguration, "app", "/root" );

		Assert.assertEquals( 4, args.size());
		Assert.assertEquals( "msg.paper=with somethig written on it", args.get( 0 ));
		Assert.assertEquals( "msg.pen=to write the answer", args.get( 1 ));
		Assert.assertEquals( "start.sh", args.get( 2 ));
		Assert.assertEquals( "etc/net.roboconf.messaging.bird.cfg", args.get( 3 ));
	}
}
