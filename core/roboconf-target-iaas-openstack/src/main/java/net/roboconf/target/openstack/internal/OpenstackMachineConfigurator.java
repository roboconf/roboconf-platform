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

package net.roboconf.target.openstack.internal;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.utils.Utils;
import net.roboconf.target.api.AbstractThreadedTargetHandler.MachineConfigurator;
import net.roboconf.target.api.TargetException;

import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.domain.FloatingIP;
import org.jclouds.openstack.nova.v2_0.domain.Server;
import org.jclouds.openstack.nova.v2_0.domain.Server.Status;
import org.jclouds.openstack.nova.v2_0.extensions.FloatingIPApi;

/**
 * A machine configurator for Openstack.
 * @author Vincent Zurczak - Linagora
 */
public class OpenstackMachineConfigurator implements MachineConfigurator {

	/**
	 * A set of locks to prevent concurrent access to the pool of floating IP addresses.
	 * <p>
	 * The idea is to have one lock per Openstack URL. It could be improved by associating
	 * user names too, but this does not seem necessary at the moment.
	 * </p>
	 */
	private static final ConcurrentHashMap<String,Object> URL_TO_LOCK = new ConcurrentHashMap<String,Object> ();

	/**
	 * The steps of a workflow.
	 * <ul>
	 * <li>WAITING_VM: we wait for the VM to be active.</li>
	 * <li>ASSOCIATE_FLOATING_IP: a floating IP has to be associated, if necessary and if possible.</li>
	 * <li>COMPLETE: there is nothing to do anymore.</li>
	 * </ul>
	 *
	 * @author Vincent Zurczak - Linagora
	 */
	public static enum State {
		WAITING_VM, ASSOCIATE_FLOATING_IP, COMPLETE;
	}

	private final Instance scopedInstance;
	private final String machineId;
	private final Map<String,String> targetProperties;
	private final Logger logger = Logger.getLogger( getClass().getName());

	private NovaApi novaApi;
	private State state = State.WAITING_VM;



	/**
	 * Constructor.
	 * @param targetProperties
	 * @param machineId
	 */
	public OpenstackMachineConfigurator( Map<String,String> targetProperties, String machineId, Instance scopedInstance ) {
		this.machineId = machineId;
		this.targetProperties = targetProperties;
		this.scopedInstance = scopedInstance;
	}


	@Override
	public Instance getScopedInstance() {
		return this.scopedInstance;
	}


	@Override
	public void close() throws IOException {
		if( this.novaApi != null )
			this.novaApi.close();
	}


	@Override
	public boolean configure() throws TargetException {

		if( this.novaApi == null )
			this.novaApi = OpenstackIaasHandler.novaApi( this.targetProperties );

		if( this.state == State.WAITING_VM )
			if( checkVmIsOnline())
				this.state = State.ASSOCIATE_FLOATING_IP;

		if( this.state == State.ASSOCIATE_FLOATING_IP )
			if( associateFloatingIp())
				this.state = State.COMPLETE;

		//if( state == State.ASSOCIATE_NETWORK )
		//	associateNetwork();

		return this.state == State.COMPLETE;
	}


	/**
	 * Checks whether a VM is created.
	 * @return true if it is online, false otherwise
	 */
	private boolean checkVmIsOnline() {

		String anyZoneName = this.novaApi.getConfiguredZones().iterator().next();
		Server server = this.novaApi.getServerApiForZone( anyZoneName ).get( this.machineId );
		return Status.ACTIVE.equals( server.getStatus());
	}


	/**
	 * Associates a floating IP to the VM (if necessary and if possible).
	 * @return true if this operation successfully completed, false otherwise
	 */
	private boolean associateFloatingIp() {

		// Associating a floating IP requires a client-side synchronization since Openstack does
		// not provide it. Indeed, it can associate a floating IP to a new server, even if this
		// address was already associated with another one. It is not possible, at the moment, to
		// reserve a floating IP. So, we must treat this step with locks.
		String floatingIpPool = this.targetProperties.get( OpenstackIaasHandler.FLOATING_IP_POOL );
		if( Utils.isEmptyOrWhitespaces( floatingIpPool ))
		 return true;

		// Protected section to prevent using a same IP for several machines.
		// An action is already in progress for this URL?
		// Then return immediately, we will try in the next scheduled run.
		String url = this.targetProperties.get( OpenstackIaasHandler.API_URL );
		if( URL_TO_LOCK.putIfAbsent( url, new Object()) != null )
			return false;

		// Deal with the association.
		boolean done = false;
		try {
			// Find a floating IP
			String availableIp = null;
			String anyZoneName = this.novaApi.getConfiguredZones().iterator().next();
			FloatingIPApi floatingIPApi = this.novaApi.getFloatingIPExtensionForZone( anyZoneName ).get();
			for( FloatingIP ip : floatingIPApi.list().toList()) {
				if( ip.getInstanceId() == null ) {
					availableIp = ip.getIp();
					break;
				}
			}

			// And associate it
			if( availableIp != null )
				floatingIPApi.addToServer( availableIp, this.machineId );
			else
				this.logger.warning( "No floating IP was available in Openstack (pool '" + floatingIpPool + "')." );

			done = true;

		} finally {
			URL_TO_LOCK.remove( url );
		}

		return done;
	}


//	/**
//	 * Associates a Neutron network with the VM (if necessary and if possible).
//	 * @throws TargetException
//	 */
//	private void associateNetwork() throws TargetException {
//
//		String networkId = this.targetProperties.get( OpenstackIaasHandler.NETWORK_ID );
//		if( ! Utils.isEmptyOrWhitespaces( networkId )) {
//			NeutronApi neutronApi = OpenstackIaasHandler.neutronApi( this.targetProperties );
//			Network network = neutronApi.getNetworkApi( this.anyZoneName ).get( networkId );
//
//		}
//	}
}
