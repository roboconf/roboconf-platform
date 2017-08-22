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
import static net.roboconf.target.embedded.internal.EmbeddedHandler.DEFAULT_SCP_AGENT_CONFIG_DIR;
import static net.roboconf.target.embedded.internal.EmbeddedHandler.SCP_AGENT_CONFIG_DIR;
import static net.roboconf.target.embedded.internal.EmbeddedHandler.SCP_KEYFILE;
import static net.roboconf.target.embedded.internal.EmbeddedHandler.SCP_USER;
import static net.roboconf.target.embedded.internal.EmbeddedHandler.USER_DATA_FILE;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import net.roboconf.core.Constants;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.userdata.UserDataHelpers;
import net.roboconf.core.utils.Utils;
import net.roboconf.target.api.AbstractThreadedTargetHandler.MachineConfigurator;
import net.roboconf.target.api.TargetException;
import net.roboconf.target.api.TargetHandlerParameters;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.xfer.FileSystemFile;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ConfiguratorOnCreation implements MachineConfigurator {

	private final Logger logger = Logger.getLogger( getClass().getName());

	protected EmbeddedHandler embedded;
	protected final TargetHandlerParameters parameters;
	protected final String ip, machineId;



	/**
	 * Constructor.
	 * @param parameters
	 * @param ip
	 * @param machineId
	 * @param embedded
	 */
	public ConfiguratorOnCreation(
			TargetHandlerParameters parameters,
			String ip,
			String machineId,
			EmbeddedHandler embedded ) {

		this.parameters = parameters;
		this.embedded = embedded;
		this.machineId = machineId;
		this.ip = ip;
	}


	@Override
	public void close() throws IOException {
		// nothing
	}


	@Override
	public boolean configure() throws TargetException {

		try {
			configureRemoteAgent( this.ip, this.parameters );

		} catch( IOException e ) {

			// In case of error, release the IP address
			this.embedded.machineIdToIp.remove( this.machineId );
			this.embedded.releaseIpAddress( this.ip );

			// Propagate the error
			throw new TargetException( e );
		}

		return true;
	}


	@Override
	public Instance getScopedInstance() {
		return this.parameters.getScopedInstance();
	}


	/**
	 * Configures a remote agent by using SCP.
	 * <p>
	 * A user-data file will be generated,
	 * sent to the remote host (in agent configuration directory), then the remote agent configuration file
	 * will be updated to point on the new user-data file ("parameters" property).
	 * </p>
	 *
	 * @param ip IP address of remote host
	 * @param parameters Target handler parameters (with user-data inside)
	 * @throws IOException
	 */
	protected void configureRemoteAgent( String ip, TargetHandlerParameters parameters )
	throws IOException {

		this.logger.fine( "Configuring remote agent @ " + ip );
		SSHClient ssh = new SSHClient();
		File tmpDir = new File( System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString());
		Utils.createDirectory( tmpDir );
		try {
			// Connect
			ssh.loadKnownHosts();
			ssh.connect(ip);

			// "ubuntu" is the default user name on several (ubuntu) systems, including IaaS VMs
			String user = Utils.getValue( parameters.getTargetProperties(), SCP_USER, "ubuntu" );
			String keyfile = parameters.getTargetProperties().get( SCP_KEYFILE );

			if( keyfile == null )
				ssh.authPublickey(user); // use ~/.ssh/id_rsa and ~/.ssh/id_dsa
			else
				ssh.authPublickey(user, keyfile); // load key from specified file (e.g .pem).

			// Do what we need to do
			Map<String,String> keyToNewValue = prepareConfiguration( parameters, ssh, tmpDir );
			updateAgentConfigurationFile( parameters, ssh, tmpDir, keyToNewValue );

		} finally {
			try {
				ssh.disconnect();
				ssh.close();

			} catch( Exception e ) {
				Utils.logException( this.logger, e );
			}

			Utils.deleteFilesRecursivelyAndQuietly( tmpDir );
		}
	}


	Map<String,String> prepareConfiguration( TargetHandlerParameters parameters, SSHClient ssh, File tmpDir )
	throws IOException {

		// Generate local user-data file
		String userData = UserDataHelpers.writeUserDataAsString(
				parameters.getMessagingProperties(),
				parameters.getDomain(),
				parameters.getApplicationName(),
				parameters.getScopedInstancePath());

		File userdataFile = new File(tmpDir, USER_DATA_FILE);
		Utils.writeStringInto(userData, userdataFile);

		// Then upload it
		String agentConfigDir = Utils.getValue( parameters.getTargetProperties(), SCP_AGENT_CONFIG_DIR, DEFAULT_SCP_AGENT_CONFIG_DIR );
		ssh.newSCPFileTransfer().upload( new FileSystemFile(userdataFile), agentConfigDir);

		// Update the agent's configuration file
		Map<String,String> keyToNewValue = new HashMap<> ();
		File remoteAgentConfig = new File(agentConfigDir, userdataFile.getName());

		// Reset all the fields
		keyToNewValue.put( AGENT_APPLICATION_NAME, "" );
		keyToNewValue.put( AGENT_SCOPED_INSTANCE_PATH, "" );
		keyToNewValue.put( AGENT_DOMAIN, "" );

		// The location of the parameters must be an URL
		keyToNewValue.put( AGENT_PARAMETERS, remoteAgentConfig.toURI().toURL().toString());

		return keyToNewValue;
	}


	/**
	 * Updates the configuration file of an agent.
	 * @param parameters
	 * @param ssh
	 * @param tmpDir
	 * @param keyToNewValue
	 * @throws IOException
	 */
	void updateAgentConfigurationFile(
			TargetHandlerParameters parameters,
			SSHClient ssh,
			File tmpDir,
			Map<String,String> keyToNewValue )
	throws IOException {

		// Update the agent's configuration file
		String agentConfigDir = Utils.getValue( parameters.getTargetProperties(), SCP_AGENT_CONFIG_DIR, DEFAULT_SCP_AGENT_CONFIG_DIR );
		File localAgentConfig = new File( tmpDir, Constants.KARAF_CFG_FILE_AGENT );
		File remoteAgentConfig = new File( agentConfigDir, Constants.KARAF_CFG_FILE_AGENT );

		// Download remote agent config file...
		ssh.newSCPFileTransfer().download(remoteAgentConfig.getCanonicalPath(), new FileSystemFile(tmpDir));

		// Replace "parameters" property to point on the user data file...
		String config = Utils.readFileContent(localAgentConfig);
		config = Utils.updateProperties( config, keyToNewValue );
		Utils.writeStringInto(config, localAgentConfig);

		// Then upload agent config file back
		ssh.newSCPFileTransfer().upload(new FileSystemFile(localAgentConfig), agentConfigDir);
	}
}
