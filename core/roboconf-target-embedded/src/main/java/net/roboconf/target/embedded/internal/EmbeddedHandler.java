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

import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.userdata.UserDataHelpers;
import net.roboconf.core.utils.Utils;
import net.roboconf.target.api.TargetException;
import net.roboconf.target.api.TargetHandler;
import net.roboconf.target.api.TargetHandlerParameters;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.xfer.FileSystemFile;

/**
 * A target for embedded systems (e.g. the local host).
 * @author Pierre-Yves Gibello - Linagora
 */
public class EmbeddedHandler implements TargetHandler {

	public static final String TARGET_ID = "embedded";
	public static final String IP_ADDRESSES = "embedded.ip";
	public static final String SCP_USER = "scp.user";
	public static final String SCP_KEYFILE = "scp.keyfile";
	public static final String SCP_AGENT_CONFIG_DIR = "scp.agent.configdir";
	static final String AGENT_CONFIG_DEFAULT_DIR = "/etc/roboconf-agent";
	static final String USER_DATA_FILE = "parameters.properties";
	private final Map<String,String> machineIdToIp = new HashMap<> ();
	protected Map<String,Boolean> ipTable = new HashMap<> ();

	@Override
	public String getTargetId() {
		return TARGET_ID;
	}


	@Override
	public String createMachine( TargetHandlerParameters parameters ) throws TargetException {
		if(parameters != null && parameters.getTargetProperties() != null) {
			String ips = parameters.getTargetProperties().get(IP_ADDRESSES);
			if(ips != null) {
				String iplist[] = ips.trim().split("\\s*,\\s*");
				for(String ip : iplist) ipTable.put(ip, false);
			}
		}
		String machineId = parameters.getScopedInstancePath() + " (" + TARGET_ID + ")";
		this.machineIdToIp.put( machineId, "" );
		return machineId;
	}


	@Override
	public void terminateMachine( TargetHandlerParameters parameters, String machineId )
	throws TargetException {
		String ip = this.machineIdToIp.remove( machineId );
		if(! Utils.isEmptyOrWhitespaces(ip)) releaseIpAddress(ip);
	}


	@Override
	public void configureMachine( TargetHandlerParameters parameters, String machineId, Instance scopedInstance )
	throws TargetException {
		// It may require to be post-configured from the DM => add the right marker
		scopedInstance.data.put( Instance.READY_FOR_CFG_MARKER, "true" );

		String ip = acquireIpAddress();
		if(! Utils.isEmptyOrWhitespaces(ip)) {
			try {
				sendConfiguration(ip, parameters);
			} catch (IOException e) {
				throw new TargetException(e);
			}
			this.machineIdToIp.put(machineId, ip);
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
	 * @return The acquired IP address
	 */
	protected String acquireIpAddress() {
		for(Map.Entry<String, Boolean> entry : ipTable.entrySet()) {
			if(! entry.getValue()) {
				entry.setValue(true);
				return entry.getKey();
			}
		}
		return null;
	}

	/**
	 * Release a (previously acquired) IP address.
	 * @param ip The IP address to release
	 */
	protected void releaseIpAddress(String ip) {
		if(ipTable.get(ip) != null) ipTable.put(ip, false);
	}

	/**
	 * Send configuration to remote host using scp. A user-data file will be generated,
	 * sent to the remote host (in agent configuration directory), then the remote agent configuration file
	 * will be updated to point on the new user-data file ("parameters" property).
	 * @param ip IP address of remote host
	 * @param parameters Target handler parameters (with user-data inside)
	 * @throws IOException
	 */
	protected void sendConfiguration(String ip, TargetHandlerParameters parameters) throws IOException {
		SSHClient ssh = new SSHClient();
		File tmpDir = new File(System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString());
		tmpDir.mkdir();
		tmpDir.deleteOnExit(); // In case deletion fails
		try {
			ssh.loadKnownHosts();
			ssh.connect(ip);

			String user = parameters.getTargetProperties().get(SCP_USER);
			if(user == null) user = "ubuntu"; // default on several (ubuntu) systems, including IaaS VMs
			String keyfile = parameters.getTargetProperties().get(SCP_KEYFILE);

			if(keyfile == null) ssh.authPublickey(user); // use ~/.ssh/id_rsa and ~/.ssh/id_dsa
			else ssh.authPublickey(user, keyfile); // load key from specified file (eg .pem).

			String userData = UserDataHelpers.writeUserDataAsString(
					parameters.getMessagingProperties(),
					parameters.getDomain(),
					parameters.getApplicationName(),
					parameters.getScopedInstancePath());

			// Generate local user-data file, then upload it
			String agentConfigDir = parameters.getTargetProperties().get(SCP_AGENT_CONFIG_DIR);
			if(agentConfigDir == null) agentConfigDir = AGENT_CONFIG_DEFAULT_DIR;
			File userdataFile = new File(tmpDir, USER_DATA_FILE);
			Utils.writeStringInto(userData, userdataFile);
			ssh.newSCPFileTransfer().upload(new FileSystemFile(userdataFile), agentConfigDir);

			File localAgentConfig = new File(tmpDir, "net.roboconf.agent.configuration.cfg");
			File remoteAgentConfig = new File(agentConfigDir, "net.roboconf.agent.configuration.cfg");
			// Download remote agent config file...
			ssh.newSCPFileTransfer().download(remoteAgentConfig.getCanonicalPath(), new FileSystemFile(tmpDir));

			// ... Replace "parameters" property to point on user data file...
			String config = Utils.readFileContent(localAgentConfig);
			config = config.replaceFirst("(?m)^\\s*parameters\\s*[:=]\\s*(.*)$",
					"\nparameters = " + remoteAgentConfig.getCanonicalPath());
			Utils.writeStringInto(config, localAgentConfig);

			// ... Then upload agent config file back
			ssh.newSCPFileTransfer().upload(new FileSystemFile(localAgentConfig), agentConfigDir);

		} finally {
			try {
				ssh.disconnect();
				ssh.close();
				tmpDir.delete();
			} catch(IOException ignore) { /* ignore */ }
		}
	}
}
