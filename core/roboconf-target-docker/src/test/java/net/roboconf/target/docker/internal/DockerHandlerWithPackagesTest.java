/**
 * Copyright 2014-2016 Linagora, Université Joseph Fourier, Floralis
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.InspectExecResponse;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.ExecStartResultCallback;

import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.utils.Utils;
import net.roboconf.target.api.TargetException;
import net.roboconf.target.api.TargetHandlerParameters;

/**
 * Test correct Docker image generation and container configuration.
 * <p>
 * WARNING: these tests may last very long....
 * </p>
 *
 * @author Pierre Bourret - Université Joseph Fourier
 */
@Ignore
public class DockerHandlerWithPackagesTest {

	private static final Logger LOGGER = Logger.getLogger(DockerHandlerWithPackagesTest.class.getName());
	private static final String APPLICATION_NAME = "roboconf_test";
	private static final String FAKE_AGENT_LOCATION = "/usr/local/roboconf-agent/roboconf-fake-agent.txt";
	private static final String FAKE_AGENT_CONTENT = "INSTALLED!";

	private static final Map<String,String> MESSAGING_CONFIGURATION;
	static {
		Map<String,String> basis = new HashMap<> ();
		basis.put("net.roboconf.messaging.type", "telepathy");
		basis.put("mindControl", "false");
		basis.put("psychosisProtection", "active");

		MESSAGING_CONFIGURATION = Collections.unmodifiableMap( basis );
	}

	@Rule
	public final TemporaryFolder tmpFolder = new TemporaryFolder();

	private final Map<String,String> targetProperties = new LinkedHashMap<> ();
	private final DockerHandler dockerHandler = new DockerHandler();
	private final Instance instance = new Instance("test-" + UUID.randomUUID().toString());
	private final String instancePath = InstanceHelpers.computeInstancePath(this.instance);

	private DockerClient dockerClient;
	private String dockerImageId, dockerContainerId;
	private File agentTarGz, agentZip;


	/**
	 * Initializes the test environment.
	 * @throws Exception if something bad happened.
	 */
	@Before
	public void initDockerClient() throws Exception {

		LOGGER.warning( "This test may take quite A LOT of TIME!!!!" );

		// Checks Docker is installed.
		try {
			DockerTestUtils.checkDockerIsInstalled();

		} catch (Exception e) {
			LOGGER.warning("Tests are skipped because Docker is not installed.");
			Utils.logException(LOGGER, e);
			Assume.assumeNoException(e);
		}

		// Load test files
		this.agentTarGz = TestUtils.findTestFile( "/archives/roboconf-fake-agent.tar.gz" );
		this.agentZip = TestUtils.findTestFile( "/archives/roboconf-fake-agent.zip" );

		// Load the Docker target properties.
		final Properties targetProperties = Utils.readPropertiesFile( TestUtils.findTestFile( "/conf/docker.properties" ));
		for (final Map.Entry<Object, Object> e : targetProperties.entrySet()) {
			this.targetProperties.put(e.getKey().toString(), e.getValue().toString());
		}

		// Add a infinite loop command, so the container stays while we test it.
		this.targetProperties.put(DockerHandler.RUN_EXEC, "[ \"tail\", \"-f\", \"/dev/null\" ]");

		// Generated a unique Docker image id.
		this.dockerImageId = "roboconf.test.generated." + UUID.randomUUID().toString().replace('-', '.');
		this.targetProperties.put(DockerHandler.IMAGE_ID, this.dockerImageId);

		// Create a client.
		try {
			this.dockerClient = DockerClientBuilder.getInstance(
					DefaultDockerClientConfig.createDefaultConfigBuilder()
							.withDockerHost( this.targetProperties.get( DockerHandler.ENDPOINT ))
							.build())
					.build();

		} catch( Exception e ) {
			LOGGER.warning("Tests are skipped because Docker is misconfigured.");
			Utils.logException(LOGGER, e);
			Assume.assumeNoException(e);
		}

		// Start the Docker target handler.
		this.dockerHandler.start();
	}


	/**
	 * Cleanup the test environment.
	 */
	@After
	public void cleanupDocker() throws Exception {
		final List<Exception> exceptions = new ArrayList<>();

		// Stop the docker target handler.
		try {
			this.dockerHandler.stop();

		} catch (final Exception e) {
			// We must keep going on, save the exception and continue.
			exceptions.add(e);
		}

		if (this.dockerClient != null) {

			// Kill the container, if any.
			if (this.dockerContainerId != null) {
				final InspectContainerResponse.ContainerState state =
						DockerUtils.getContainerState( this.dockerContainerId, this.dockerClient);

				if( state != null
						&& ( extractBoolean( state.getRunning()) || extractBoolean( state.getPaused()))) {

					try {
						this.dockerClient.killContainerCmd(this.dockerContainerId) .exec();

					} catch (final Exception e) {
						// We must keep going on, save the exception and continue.
						exceptions.add(e);
					}
				}
			}

			// Delete the built image, if any.
			try {
				DockerUtils.deleteImageIfItExists(this.dockerImageId, this.dockerClient);
			} catch (final Exception e) {
				// We must keep going on, save the exception and continue.
				exceptions.add(e);
			}

			// Finally close the client.
			try {
				this.dockerClient.close();
			} catch (final Exception e) {
				// We must keep going on, save the exception and continue.
				exceptions.add(e);
			}
		}

		// Log encountered exceptions, if any.
		if (!exceptions.isEmpty()) {
			for (final Exception e : exceptions) {
				LOGGER.severe("Exception while cleaning test up");
				Utils.logException(LOGGER, Level.SEVERE, e);
			}

			// Throw the first exception, so the whole thing fails!
			throw exceptions.get(0);
		}
	}


	@Test
	public void testAgentTarGz_withAdditionalPackagesOnly() throws Exception {
		// Configure the container:
		// - we use the TarGz agent archive,
		// - we clear the JRE packages property, so the default is used.
		// - we add additional packages: vim & net-tools.
		this.targetProperties.put(DockerHandler.AGENT_PACKAGE_URL, this.agentTarGz.getAbsolutePath());
		this.targetProperties.remove(DockerHandler.AGENT_JRE_AND_PACKAGES);
		this.targetProperties.put(DockerHandler.ADDITIONAL_PACKAGES, "vim net-tools");
		runAndTestDockerContainer(Collections.<String>emptyList(), DockerHandler.AGENT_JRE_AND_PACKAGES_DEFAULT, "vim", "net-tools");
	}


	@Test
	public void testAgentZip_withAdditionalPackagesOnly() throws Exception {
		// Configure the container:
		// - we use the Zip agent archive,
		// - we clear the JRE packages property, so the default is used.
		// - we add additional packages: vim & net-tools.
		this.targetProperties.put(DockerHandler.AGENT_PACKAGE_URL, this.agentZip.getAbsolutePath());
		this.targetProperties.remove(DockerHandler.AGENT_JRE_AND_PACKAGES);
		this.targetProperties.put(DockerHandler.ADDITIONAL_PACKAGES, "vim net-tools");
		runAndTestDockerContainer(Collections.<String>emptyList(), DockerHandler.AGENT_JRE_AND_PACKAGES_DEFAULT, "unzip", "vim", "net-tools");
	}


	@Test
	public void testAgentTarGz_withAlternateJreAndAdditionalPackages() throws Exception {
		// Configure the container:
		// - we use the TarGz agent archive,
		// - we set the JRE packages property to use JamVM.
		// - we add additional packages: vim & net-tools.
		this.targetProperties.put(DockerHandler.AGENT_PACKAGE_URL, this.agentTarGz.getAbsolutePath());
		this.targetProperties.put(DockerHandler.AGENT_JRE_AND_PACKAGES, "icedtea-7-jre-jamvm");
		this.targetProperties.put(DockerHandler.ADDITIONAL_PACKAGES, "vim net-tools");
		runAndTestDockerContainer(Collections.<String>emptyList(), "icedtea-7-jre-jamvm", "vim", "net-tools");
	}


	@Test
	public void testAgentZip_withAlternateJreOnly() throws Exception {
		// Configure the container:
		// - we use the Zip agent archive,
		// - we set the JRE packages property to use JamVM.
		// - we use no additional packages.
		this.targetProperties.put(DockerHandler.AGENT_PACKAGE_URL, this.agentZip.getAbsolutePath());
		this.targetProperties.put(DockerHandler.AGENT_JRE_AND_PACKAGES, "icedtea-7-jre-jamvm");
		this.targetProperties.remove(DockerHandler.ADDITIONAL_PACKAGES);
		runAndTestDockerContainer(Collections.<String>emptyList(), "icedtea-7-jre-jamvm");
	}


	@Test
	public void testAgentZip_withAdditionalDeploys() throws Exception {
		// Create a dummy file in the tmp folder.
		final File dummy = this.tmpFolder.newFile("DUMMY.TXT");

		// Configure the container:
		// - we use the Zip agent archive,
		// - we clear the JRE packages property, so the default is used.
		// - we use no additional packages.
		// - we two additional deploy URLs, that will be copied in the (container's) Karaf deploy directory:
		//    - a remote URL (Apache license v2: LICENSE-2.0.txt)
		//    - a local file (DUMMY.TXT)
		this.targetProperties.put(DockerHandler.AGENT_PACKAGE_URL, this.agentZip.getAbsolutePath());
		this.targetProperties.remove(DockerHandler.AGENT_JRE_AND_PACKAGES);
		this.targetProperties.remove(DockerHandler.ADDITIONAL_PACKAGES);
		this.targetProperties.put(DockerHandler.ADDITIONAL_DEPLOY,
				"http://www.apache.org/licenses/LICENSE-2.0.txt " + dummy.toURI());

		List<String> values = new ArrayList<>( 2 );
		values.add("/usr/local/roboconf-agent/deploy/LICENSE-2.0.txt");
		values.add("/usr/local/roboconf-agent/deploy/DUMMY.TXT");
		runAndTestDockerContainer( values );
	}


	/**
	 * Creates, configures, runs and tests a Docker container.
	 *
	 * @param filesToCheck a list of files that must be present on the Docker container.
	 * @param packages the packages that must be installed on the Docker container.
	 * @throws Exception if anything bad happens during the run & tests.
	 */
	private void runAndTestDockerContainer( final List<String> filesToCheck, final String... packages ) throws Exception {

		// Create the machine.
		TargetHandlerParameters parameters =
				new TargetHandlerParameters().targetProperties( this.targetProperties )
					.messagingProperties( MESSAGING_CONFIGURATION )
					.scopedInstancePath( this.instancePath )
					.applicationName( APPLICATION_NAME )
					.domain( "domain" );

		final String machineId = this.dockerHandler.createMachine( parameters );

		Assert.assertNotNull(machineId);
		Assert.assertNull(this.instance.data.get(Instance.MACHINE_ID));

		// Configure the machine.
		this.dockerHandler.configureMachine( parameters, machineId, this.instance);

		// Now wait until the Docker target updates the machine id, or the timeout expires...
		this.dockerContainerId = DockerTestUtils.waitForMachineId(
				machineId,
				this.instance.data,
				DockerTestUtils.DOCKER_CONFIGURE_TIMEOUT);
		Assert.assertNotNull(this.dockerContainerId);

		// Ensure the machine is running
		Assert.assertTrue(this.dockerHandler.isMachineRunning(this.targetProperties, this.dockerContainerId));

		// Now perform the tests...
		checkAgentIsUnpacked();
		for (final String file : filesToCheck)
			checkFileIsPresent(file);
		for (final String p : packages)
			checkPackageIsInstalled(p);

		// Terminate the container.
		this.dockerHandler.terminateMachine(this.targetProperties, this.dockerContainerId);
		Assert.assertFalse(this.dockerHandler.isMachineRunning(this.targetProperties, this.dockerContainerId));
	}


	private void checkAgentIsUnpacked() throws Exception {
		final CommandResult result = execDockerCommand("cat", FAKE_AGENT_LOCATION);
		Assert.assertEquals("Fake agent is not installed", 0, result.exitCode);
		Assert.assertTrue("Fake agent is not installed", result.output.contains(FAKE_AGENT_CONTENT));
	}


	/**
	 * Check that a given Debian package is installed on the given Docker container.
	 *
	 * @param packageName the name of the package to check.
	 * @throws TargetException if the container cannot be reached.
	 */
	private void checkPackageIsInstalled( String packageName ) throws Exception {

		// Execute the package checker command.
		// As we use pipes, we need to bash -c the whole quoted command.
		Assert.assertEquals("package '" + packageName + "' is not installed on container " + this.dockerContainerId,
				0,
				execDockerCommand(
						"bash", "-c", "dpkg --get-selections | grep ^" + packageName + " "
				).exitCode);
	}


	/**
	 * Check that a given file is present on the given Docker container.
	 *
	 * @param path the path of the file to check.
	 * @throws TargetException if the container cannot be reached.
	 */
	private void checkFileIsPresent( String path ) throws Exception {

		// Execute the package checker command.
		// As we use pipes, we need to bash -c the whole quoted command.
		Assert.assertEquals( "file '" + path + "' is not present on container " + this.dockerContainerId, 0,
				execDockerCommand( "test", "-f", path ).exitCode );
	}


	/**
	 * The result of a Docker exec.
	 */
	private static class CommandResult {
		final int exitCode;
		final String output;

		CommandResult( final int exitCode, final String output ) {
			this.exitCode = exitCode;
			this.output = output;
		}
	}


	/**
	 * Executes a command on the tested docker container.
	 * @param commandLine the command line to run.
	 * @return the result of the command execution.
	 * @throws Exception if something bas happened during the command execution
	 *                   (excluding the command itself, see {@code result}).
	 */
	private CommandResult execDockerCommand( String... commandLine ) throws Exception {

		// Create the command and get its execId (execCreateCmd)
		final String execId = this.dockerClient.createContainerCmd( this.dockerContainerId )
				.withCmd( commandLine )
				.withAttachStdout( true )
				.exec()
				.getId();

		// Start the command (execStartCmd), and get the output.
		ByteArrayOutputStream stdOutAndErr = new ByteArrayOutputStream();
		this.dockerClient.execStartCmd( this.dockerContainerId )
				.withExecId(execId)
				.exec( new ExecStartResultCallback( stdOutAndErr, stdOutAndErr )).awaitCompletion();

		// Wait until the command has finished...
		InspectExecResponse cmd;
		do {
			cmd = this.dockerClient.inspectExecCmd(execId).exec();

		} while (cmd.isRunning());

		// Now return...
		return new CommandResult(
				cmd.getExitCode(),
				stdOutAndErr.toString("UTF-8"));
	}
}
