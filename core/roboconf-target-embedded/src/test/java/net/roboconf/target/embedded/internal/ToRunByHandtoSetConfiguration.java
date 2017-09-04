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

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import net.roboconf.core.model.beans.Instance;
import net.roboconf.target.api.TargetHandlerParameters;

/**
 * Create/get a server by hand, install a Roboconf agent and let's run!
 * @author Pierre-Yves Gibello - Linagora
 */
//@Ignore
public class ToRunByHandtoSetConfiguration {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();


	@Test
	public void toRunByHand() throws Exception {

		EmbeddedHandler handler = new EmbeddedHandler();
		handler.karafData = this.folder.newFolder().getAbsolutePath();

		TargetHandlerParameters parameters = parameters( getkeyFile());
		ConfiguratorOnCreation configurator = new ConfiguratorOnCreation( parameters, getIp(), "whatever", handler );
		Assert.assertTrue( configurator.configure());
		configurator.close();
	}


	/**
	 * @return the IP address
	 */
	protected String getIp() {
		return "54.171.159.33";
	}


	/**
	 * @return the location of the key file
	 */
	protected String getkeyFile() {
		return "/home/gibello/Linagora/EC2Linagora/aws-linagora.pem";
	}


	/**
	 * Prepares the parameters for the target handler.
	 * @param keyFile the key file
	 * @return a non-null parameters object
	 */
	protected TargetHandlerParameters parameters( String keyFile ) {

		TargetHandlerParameters parameters = new TargetHandlerParameters();
		Map<String, String> messagingProperties = new HashMap<>();
		messagingProperties.put("messaging.type",  "http");
		parameters.setMessagingProperties(messagingProperties);
		parameters.setDomain("test-domain");
		parameters.setApplicationName("test-application");
		parameters.setScopedInstancePath("/test/instance");
		parameters.setScopedInstance( new Instance());

		Map<String, String> targetProperties = new HashMap<>();
		targetProperties.put( EmbeddedHandler.SCP_KEY_FILE, keyFile );
		parameters.setTargetProperties( targetProperties );

		return parameters;
	}
}
