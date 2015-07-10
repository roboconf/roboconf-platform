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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
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

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.InspectExecResponse;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import junit.framework.Assert;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.utils.Utils;
import net.roboconf.target.api.TargetException;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Test correct Docker image generation and container configuration.
 * <p>
 * WARNING: these tests may last very long....
 *
 * @author Pierre Bourret - Université Joseph Fourier
 */
public class DockerHandler_withPackagesTest {

	/**
	 * The logger.
	 */
	private static final Logger LOGGER = Logger.getLogger(DockerHandler_withPackagesTest.class.getName());

	/**
	 * The messaging configuration map.
	 */
	private static class MessagingConfigurationMap extends HashMap<String, String> {
		// Instance initializer block.
		{
			put("net.roboconf.messaging.type", "telepathy");
			put("mindControl", "false");
			put("psychosisProtection", "active");
		}
	}

	/**
	 * The name of the test application.
	 */
	private static final String APPLICATION_NAME = "roboconf_test";

	/**
	 * The constant messaging configuration map.
	 */
	private static final Map<String, String> MESSAGING_CONFIGURATION = Collections.unmodifiableMap(
			new MessagingConfigurationMap());

	/**
	 * The maximum time we allow the docker handler to take to configure a machine.
	 * <p>
	 * Installing packages may take a while... So this value is quite large!
	 */
	private static long DOCKER_CONFIGURE_TIMEOUT = 300000L;

	/**
	 * The location of the Roboconf fake agent's file.
	 */
	private static final String FAKE_AGENT_LOCATION = "/usr/local/roboconf-agent/roboconf-fake-agent.txt";

	/**
	 * The content of the Roboconf fake agent's file.
	 */
	private static final String FAKE_AGENT_CONTENT = "INSTALLED!";

	/**
	 * The folder where Docker image is built.
	 */
	@Rule
	public final TemporaryFolder tmpFolder = new TemporaryFolder();

	/**
	 * The (fake) Roboconf agent TAR GZ archive.
	 */
	private final File agentTarGz = loadTestResourceFile("archives/roboconf-fake-agent.tar.gz");

	/**
	 * The (fake) Roboconf agent ZIP archive.
	 */
	private final File agentZip = loadTestResourceFile("archives/roboconf-fake-agent.zip");

	/**
	 * The Docker target properties, pre-provisioned from the "conf/docker.properties" test resource.
	 */
	private final Map<String, String> targetProperties = new LinkedHashMap<>();

	/**
	 * The Docker client.
	 */
	private DockerClient dockerClient;

	/**
	 * The Docker handler.
	 */
	private final DockerHandler dockerHandler = new DockerHandler();

	/**
	 * The tested instance.
	 */
	private final Instance instance = new Instance("test-" + UUID.randomUUID().toString());

	/**
	 * The path of the tested instance.
	 */
	private final String instancePath = InstanceHelpers.computeInstancePath(this.instance);

	/**
	 * The built Docker image id, if any.
	 */
	private String dockerImageId;

	/**
	 * The created Docker container id, if any.
	 */
	private String dockerContainerId;

	/**
	 * Load a test resources file.
	 *
	 * @param name the name of the resource to load.
	 * @return the loaded resource file, or {@code null} if the resource cannot be loaded.
	 */
	private static File loadTestResourceFile( final String name ) {
		final URL url = Thread.currentThread()
				.getContextClassLoader()
				.getResource(name);
		final File result;
		if (url != null) {
			result = new File(url.getFile());
		} else {
			result = null;
		}
		return result;
	}

	/**
	 * Initialize the test environment.
	 *
	 * @throws Exception if something bad happened.
	 */
	@Before
	public void initDockerClient() throws Exception {
		// Checks Docker is installed.
		try {
			DockerTestUtils.checkDockerIsInstalled();
		} catch (Exception e) {
			LOGGER.warning("Tests are skipped because Docker is not installed.");
			Utils.logException(LOGGER, e);
			Assume.assumeNoException(e);
		}

		// Load the Docker target properties.
		final Properties targetProperties = Utils.readPropertiesFile(loadTestResourceFile("conf/docker.properties"));
		for (final Map.Entry<Object, Object> e : targetProperties.entrySet()) {
			this.targetProperties.put(e.getKey().toString(), e.getValue().toString());
		}

		// Add a infinite loop command, so the container stays while we test it.
		this.targetProperties.put(DockerHandler.COMMAND, "tail -f /dev/null");
		this.targetProperties.put(DockerHandler.USE_COMMAND, "true");

		// Generated a unique Docker image id.
		this.dockerImageId = "roboconf.test.generated." + UUID.randomUUID().toString().replace('-', '.');
		this.targetProperties.put(DockerHandler.IMAGE_ID, this.dockerImageId);

		// Create a client.
		try {
			this.dockerClient = DockerClientBuilder.getInstance(
					DockerClientConfig.createDefaultConfigBuilder()
							.withUri(this.targetProperties.get(DockerHandler.ENDPOINT))
							.build())
					.build();
		} catch (Exception e) {
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
				final InspectContainerResponse.ContainerState state = DockerUtils.getContainerState(
						this.dockerContainerId,
						this.dockerClient);
				if (state != null && (state.isRunning() || state.isPaused())) {
					try {
						this.dockerClient
								.killContainerCmd(this.dockerContainerId)
								.exec();
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
		this.targetProperties.put(DockerHandler.AGENT_PACKAGE, agentTarGz.getAbsolutePath());
		this.targetProperties.remove(DockerHandler.AGENT_JRE_AND_PACKAGES);
		this.targetProperties.put(DockerHandler.ADDITIONAL_PACKAGES, "vim net-tools");
		runAndTestDockerContainer(DockerHandler.AGENT_JRE_AND_PACKAGES_DEFAULT, "vim", "net-tools");
	}

	@Test
	public void testAgentZip_withAdditionalPackagesOnly() throws Exception {
		// Configure the container:
		// - we use the Zip agent archive,
		// - we clear the JRE packages property, so the default is used.
		// - we add additional packages: vim & net-tools.
		this.targetProperties.put(DockerHandler.AGENT_PACKAGE, agentZip.getAbsolutePath());
		this.targetProperties.remove(DockerHandler.AGENT_JRE_AND_PACKAGES);
		this.targetProperties.put(DockerHandler.ADDITIONAL_PACKAGES, "vim net-tools");
		runAndTestDockerContainer(DockerHandler.AGENT_JRE_AND_PACKAGES_DEFAULT, "unzip", "vim", "net-tools");
	}

	@Test
	public void testAgentTarGz_withAlternateJreAndAdditionalPackages() throws Exception {
		// Configure the container:
		// - we use the TarGz agent archive,
		// - we set the JRE packages property to use JamVM.
		// - we add additional packages: vim & net-tools.
		this.targetProperties.put(DockerHandler.AGENT_PACKAGE, agentTarGz.getAbsolutePath());
		this.targetProperties.put(DockerHandler.AGENT_JRE_AND_PACKAGES, "icedtea-7-jre-jamvm");
		this.targetProperties.put(DockerHandler.ADDITIONAL_PACKAGES, "vim net-tools");
		runAndTestDockerContainer("icedtea-7-jre-jamvm", "vim", "net-tools");
	}

	@Test
	public void testAgentZip_withAlternateJreOnly() throws Exception {
		// Configure the container:
		// - we use the Zip agent archive,
		// - we set the JRE packages property to use JamVM.
		// - we use no additional packages.
		this.targetProperties.put(DockerHandler.AGENT_PACKAGE, agentZip.getAbsolutePath());
		this.targetProperties.put(DockerHandler.AGENT_JRE_AND_PACKAGES, "icedtea-7-jre-jamvm");
		this.targetProperties.remove(DockerHandler.ADDITIONAL_PACKAGES);
		runAndTestDockerContainer("icedtea-7-jre-jamvm");
	}

	/**
	 * Creates, configures, runs and tests a Docker container.
	 *
	 * @param packages the packages that must be installed on the Docker container.
	 * @throws Exception if anything bad happens during the run & tests.
	 */
	private void runAndTestDockerContainer( final String... packages ) throws Exception {


		// Create the machine.
		final String machineId = this.dockerHandler.createMachine(
				this.targetProperties,
				MESSAGING_CONFIGURATION,
				this.instancePath,
				APPLICATION_NAME);
		Assert.assertNotNull(machineId);
		Assert.assertNull(this.instance.data.get(Instance.MACHINE_ID));

		// Configure the machine.
		dockerHandler.configureMachine(
				this.targetProperties,
				MESSAGING_CONFIGURATION,
				machineId,
				this.instancePath,
				APPLICATION_NAME,
				this.instance);

		// Now wait until the Docker target updates the machine id, or the timeout expires...
		long deadLine = System.currentTimeMillis() + DOCKER_CONFIGURE_TIMEOUT;
		String containerId;
		do {
			containerId = this.instance.data.get(Instance.MACHINE_ID);
			if (containerId != null && !containerId.equals(machineId)) {
				break;
			}
			Thread.sleep(1000);
		} while (System.currentTimeMillis() < deadLine);
		Assert.assertNotNull(containerId);
		this.dockerContainerId = containerId;

		// Check the machine is running
		Assert.assertTrue(this.dockerHandler.isMachineRunning(this.targetProperties, containerId));

		// Now perform the tests...
		checkAgentIsUnpacked();
		for (final String p : packages) {
			checkPackageIsInstalled(p);
		}

		// Terminate the container.
		this.dockerHandler.terminateMachine(this.targetProperties, containerId);
		Assert.assertFalse(this.dockerHandler.isMachineRunning(this.targetProperties, containerId));
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
	 * Execute a command on the tested docker container.
	 * TODO refactor & put that method in DockerUtils.
	 *
	 * @param commandLine the command line to run.
	 * @return the result of the command execution.
	 * @throws Exception if something bas happened during the command execution
	 *                   (excluding the command itself, see {@code result}).
	 */
	private CommandResult execDockerCommand( String... commandLine ) throws Exception {
		// Create the command and get its execId (execCreateCmd)
		final String execId = this.dockerClient.execCreateCmd(this.dockerContainerId)
				.withCmd(commandLine)
				.withAttachStdout()
				.exec()
				.getId();

		// Start the command (execStartCmd), and get the output.
		final InputStream in = this.dockerClient.execStartCmd(this.dockerContainerId)
				.withExecId(execId)
				.exec();

		// Wait until the command has finished...
		InspectExecResponse cmd;
		do {
			cmd = this.dockerClient.inspectExecCmd(execId)
					.exec();
		} while (cmd.isRunning());

		// Put the command output into a string.
		// TODO put that code in Utils!
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		final byte[] buffer = new byte[1024];
		int length;
		while ((length = in.read(buffer)) != -1) {
			out.write(buffer, 0, length);
		}

		// Now return...
		return new CommandResult(
				cmd.getExitCode(),
				out.toString("UTF-8"));
	}

}
