/**
 * Copyright 2017 Linagora, Université Joseph Fourier, Floralis
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

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import net.roboconf.core.Constants;
import net.roboconf.target.api.TargetHandlerParameters;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.xfer.scp.SCPFileTransfer;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ConfiguratorOnTerminationTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();


	@Test
	public void testPrepareConfiguration() throws Exception {

		// Prepare the mocks
		File tmpDir = this.folder.newFolder();
		TargetHandlerParameters parameters = new TargetHandlerParameters()
				.targetProperties( new HashMap<String,String>( 0 ));

		SSHClient ssh = Mockito.mock( SSHClient.class );
		SCPFileTransfer scp = Mockito.mock( SCPFileTransfer.class );
		Mockito.when( ssh.newSCPFileTransfer()).thenReturn( scp );

		// Invoke the method
		EmbeddedHandler embedded = new EmbeddedHandler();
		embedded.karafData = this.folder.newFolder().getAbsolutePath();
		ConfiguratorOnTermination configurator = new ConfiguratorOnTermination( parameters, "ip", "machineId", embedded );
		Map<String,String> keyToNewValue = configurator.prepareConfiguration( parameters, ssh, tmpDir );

		// There is no upload by this method
		Mockito.verifyZeroInteractions( scp );

		// Prevent a compilation warning about leaks
		configurator.close();

		// Verify the map's content
		Assert.assertEquals( 4, keyToNewValue.size());
		Assert.assertEquals( "", keyToNewValue.get( AGENT_APPLICATION_NAME ));
		Assert.assertEquals( "", keyToNewValue.get( AGENT_SCOPED_INSTANCE_PATH ));
		Assert.assertEquals( "", keyToNewValue.get( AGENT_DOMAIN ));
		Assert.assertEquals( Constants.AGENT_RESET, keyToNewValue.get( AGENT_PARAMETERS ));
	}
}
