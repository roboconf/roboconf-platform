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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import net.roboconf.core.Constants;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.userdata.UserDataHelpers;
import net.roboconf.core.utils.Utils;
import net.roboconf.target.api.TargetException;
import net.roboconf.target.api.TargetHandler;
import net.roboconf.target.api.TargetHandlerParameters;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.xfer.FileSystemFile;

/**
 * A target for systems where the servers are not started by Roboconf (e.g. on-premise hosts).
 * @author Pierre-Yves Gibello - Linagora
 */
public class EmbeddedHandler implements TargetHandler {

	public static final String TARGET_ID = "embedded";
	public static final String IP_ADDRESSES = "embedded.ip";
	public static final String SCP_USER = "scp.user";
	public static final String SCP_KEYFILE = "scp.keyfile";
	public static final String SCP_AGENT_CONFIG_DIR = "scp.agent.configdir";

	static final String DEFAULT_SCP_AGENT_CONFIG_DIR = "/etc/roboconf-agent";
	static final String USER_DATA_FILE = "roboconf-agent-parameters.properties";

	static final String AGENT_SCOPED_INSTANCE_PATH = "scoped-instance-path";
	static final String AGENT_APPLICATION_NAME = "application-name";
	static final String AGENT_PARAMETERS = "parameters";
	static final String AGENT_DOMAIN = "domain";



	/**
	 * All the running machines are stored in this map.
	 * <p>
	 * The IP may be empty if it was not assigned by this class
	 * (e.g. the remote agent is pre-configured with a given identity).
	 * </p>
	 */
	final Map<String,String> machineIdToIp = new ConcurrentHashMap<> ();

	/**
	 * The used IP addresses.
	 * <p>
	 * The value is not used.
	 * </p>
	 */
	final ConcurrentHashMap<String,Integer> usedIps = new ConcurrentHashMap<> ();
	private final Logger logger = Logger.getLogger( getClass().getName());



	@Override
	public String getTargetId() {
		return TARGET_ID;
	}


	@Override
	public String createMachine( TargetHandlerParameters parameters ) throws TargetException {

		// A unique machine ID must include both the application name and the scoped instance path
		String machineId = parameters.getApplicationName() + "_" + parameters.getScopedInstancePath();
		this.logger.fine( "Creating machine with ID " + machineId );

		// Try to acquire an IP address
		if( parameters.getTargetProperties().containsKey( IP_ADDRESSES )) {
			String ip = acquireIpAddress( parameters, machineId );
			if( ip == null ) {
				this.logger.fine( "No IP address is available from the pool for " + machineId );
				throw new TargetException( "No IP address is available from the pool for " + machineId );
			}

			this.logger.fine( "IP " + ip + " was assigned to " + machineId );
		}

		// Otherwise, consider the machine as launched
		else {
			this.machineIdToIp.put( machineId, "" );
		}

		return machineId;
	}


	@Override
	public void terminateMachine( TargetHandlerParameters parameters, String machineId )
	throws TargetException {

		this.logger.fine( "Terminating machine " + machineId );
		if( machineId != null ) {
			String ip = this.machineIdToIp.remove( machineId );
			if( ! Utils.isEmptyOrWhitespaces( ip )) {

				// Release the IP address
				releaseIpAddress(ip);

				// Erase the remote agent's identity
				try {
					configureRemoteAgent( ip, parameters, true );

				} catch( IOException e ) {
					// We do not propagate the exception
					Utils.logException( this.logger, e );
				}
			}
		}
	}


	@Override
	public void configureMachine( TargetHandlerParameters parameters, String machineId, Instance scopedInstance )
	throws TargetException {

		// It may require to be post-configured from the DM => add the right marker
		scopedInstance.data.put( Instance.READY_FOR_CFG_MARKER, "true" );

		// Retrieve the IP address
		String ip = this.machineIdToIp.get( machineId );
		if( ! Utils.isEmptyOrWhitespaces(ip)) {
			try {
				configureRemoteAgent( ip, parameters, false );

			} catch( IOException e ) {
				// In case of error, release the IP address
				this.machineIdToIp.remove( machineId );
				releaseIpAddress( ip );
				throw new TargetException( e );
			}
		}
	}


	@Override
	public boolean isMachineRunning( TargetHandlerParameters parameters, String machineId )
	throws TargetException {
		return this.machineIdToIp.containsKey( machineId );
	}


	@Override
	public String retrievePublicIpAddress( TargetHandlerParameters parameters, String machineId )
	throws TargetException {
		String ip = this.machineIdToIp.get(machineId);
		return Utils.isEmptyOrWhitespaces(ip) ? null : ip;
	}


	/**
	 * Acquires an IP address among available ones.
	 * @param parameters the target parameters
	 * @param machineId the machine ID
	 * @return The acquired IP address
	 */
	protected String acquireIpAddress( TargetHandlerParameters parameters, String machineId ) {

		// Load IP addresses on demand (that's the case if we are in this method).
		// This implementation is compliant with a same IP address being used in
		// several "target.properties" files.
		String result = null;
		String ips = parameters.getTargetProperties().get( IP_ADDRESSES );
		ips = ips == null ? "" : ips;
		for( String ip : Utils.splitNicely( ips, "," )) {
			if( Utils.isEmptyOrWhitespaces( ip ))
				continue;

			if( this.usedIps.putIfAbsent( ip, 1 ) == null ) {
				this.machineIdToIp.put( machineId, ip );
				result = ip;
				break;
			}
		}

		return result;
	}


	/**
	 * Releases a (previously acquired) IP address.
	 * @param ip The IP address to release
	 */
	protected void releaseIpAddress( String ip ) {
		this.logger.fine( "Releasing IP address: " + ip );
		this.usedIps.remove( ip );
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
	 * @param erase true to erase the configuration, false to set it
	 * @throws IOException
	 */
	protected void configureRemoteAgent( String ip, TargetHandlerParameters parameters, boolean erase )
	throws IOException {

		this.logger.fine( "Configuring remote agent @ " + ip + (erase ? "(erase mode)" : "(set mode)"));
		SSHClient ssh = new SSHClient();
		File tmpDir = new File(System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString());
		Utils.createDirectory( tmpDir );
		try {
			// Connect
			ssh.loadKnownHosts();
			ssh.connect(ip);

			// "ubuntu" is the default user name on several (ubuntu) systems, including IaaS VMs
			String user = Utils.getValue( parameters.getTargetProperties(), SCP_USER, "ubuntu" );
			String keyfile = parameters.getTargetProperties().get(SCP_KEYFILE);

			if(keyfile == null)
				ssh.authPublickey(user); // use ~/.ssh/id_rsa and ~/.ssh/id_dsa
			else
				ssh.authPublickey(user, keyfile); // load key from specified file (e.g .pem).

			// Do what we need to do
			if( erase )
				eraseConfiguration( parameters, ssh, tmpDir );
			else
				setConfiguration( parameters, ssh, tmpDir );

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


	void setConfiguration( TargetHandlerParameters parameters, SSHClient ssh, File tmpDir )
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

		updateAgentConfigurationFile( parameters, ssh, tmpDir, keyToNewValue );
	}


	void eraseConfiguration( TargetHandlerParameters parameters, SSHClient ssh, File tmpDir )
	throws IOException {

		// Reset all the fields
		Map<String,String> keyToNewValue = new HashMap<> ();
		keyToNewValue.put( AGENT_APPLICATION_NAME, "" );
		keyToNewValue.put( AGENT_SCOPED_INSTANCE_PATH, "" );
		keyToNewValue.put( AGENT_DOMAIN, "" );
		keyToNewValue.put( AGENT_PARAMETERS, Constants.AGENT_RESET );

		updateAgentConfigurationFile( parameters, ssh, tmpDir, keyToNewValue );
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
