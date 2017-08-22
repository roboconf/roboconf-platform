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
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import net.roboconf.core.Constants;
import net.roboconf.target.api.TargetHandlerParameters;
import net.schmizz.sshj.SSHClient;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ConfiguratorOnTermination extends ConfiguratorOnCreation {

	/**
	 * Constructor.
	 * @param parameters
	 * @param ip
	 * @param machineId
	 * @param embedded
	 */
	public ConfiguratorOnTermination(
			TargetHandlerParameters parameters,
			String ip,
			String machineId,
			EmbeddedHandler embedded ) {

		super( parameters, ip, machineId, embedded );
	}


	/* (non-Javadoc)
	 * @see net.roboconf.target.embedded.internal.ConfiguratorOnCreation
	 * #prepareConfiguration(net.roboconf.target.api.TargetHandlerParameters, net.schmizz.sshj.SSHClient, java.io.File)
	 */
	@Override
	Map<String,String> prepareConfiguration( TargetHandlerParameters parameters, SSHClient ssh, File tmpDir )
	throws IOException {

		try {
			// Reset all the fields
			Map<String,String> keyToNewValue = new HashMap<> ();
			keyToNewValue.put( AGENT_APPLICATION_NAME, "" );
			keyToNewValue.put( AGENT_SCOPED_INSTANCE_PATH, "" );
			keyToNewValue.put( AGENT_DOMAIN, "" );
			keyToNewValue.put( AGENT_PARAMETERS, Constants.AGENT_RESET );

			return keyToNewValue;

		} finally {
			// Consider the IP as not used anymore, no matter what
			this.embedded.releaseIpAddress( this.ip );
		}
	}
}
