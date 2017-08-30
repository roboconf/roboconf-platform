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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import net.roboconf.core.utils.Utils;
import net.roboconf.target.api.AbstractThreadedTargetHandler;
import net.roboconf.target.api.TargetException;
import net.roboconf.target.api.TargetHandlerParameters;

/**
 * A target for systems where the servers are not started by Roboconf (e.g. on-premise hosts).
 * @author Pierre-Yves Gibello - Linagora
 */
public class EmbeddedHandler extends AbstractThreadedTargetHandler {

	public static final String TARGET_ID = "embedded";
	public static final String IP_ADDRESSES = "embedded.ip";
	public static final String SCP_USER = "scp.user";
	public static final String SCP_KEY_FILE = "scp.keyfile";
	public static final String SCP_KNOWN_HOSTS_FILE = "scp.known.hosts.file";
	public static final String SCP_AGENT_CONFIG_DIR = "scp.agent.configdir";
	public static final String SCP_HOST_KEY_PREFIX = "hostkey.";
	public static final String SCP_DISABLE_HOST_VALIDATION = "scp.disable.host.validation";

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


	/* (non-Javadoc)
	 * @see net.roboconf.target.api.AbstractThreadedTargetHandler
	 * #machineConfigurator(net.roboconf.target.api.TargetHandlerParameters, java.lang.String)
	 */
	@Override
	public MachineConfigurator machineConfigurator( TargetHandlerParameters parameters, String machineId )
	throws TargetException {

		// Configure the machine only if there is an IP address
		MachineConfigurator configurator = null;

		// Retrieve the IP address
		String ip = this.machineIdToIp.get( machineId );
		if( ! Utils.isEmptyOrWhitespaces(ip)) {
			configurator = new ConfiguratorOnCreation( parameters, ip, machineId, this );
		}

		return configurator;
	}


	@Override
	public void terminateMachine( TargetHandlerParameters parameters, String machineId )
	throws TargetException {

		// Cancel configuration, if any
		cancelMachineConfigurator( machineId );

		// If there is a remote IP, prepare the agent for recycle
		if( machineId != null ) {
			String ip = this.machineIdToIp.remove( machineId );
			if( ! Utils.isEmptyOrWhitespaces( ip )) {
				this.logger.fine( "Terminating machine " + machineId + " with a machine configurator." );
				MachineConfigurator configurator = new ConfiguratorOnTermination( parameters, ip, machineId, this );
				final String uniqueMachineId = "#STOP# " + machineId;
				submitMachineConfiguratorUseWithCaution( uniqueMachineId, configurator );
			}

			else {
				this.logger.fine( "Terminating machine " + machineId );
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
}
