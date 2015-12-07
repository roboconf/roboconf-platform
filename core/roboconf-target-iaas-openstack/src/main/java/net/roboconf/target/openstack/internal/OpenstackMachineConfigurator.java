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

//import org.jclouds.openstack.neutron.v2.domain.FloatingIP;
//import org.jclouds.openstack.neutron.v2.extensions.FloatingIPApi;
import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.domain.FloatingIP;
import org.jclouds.openstack.nova.v2_0.domain.Server;
import org.jclouds.openstack.nova.v2_0.domain.Server.Status;
import org.jclouds.openstack.nova.v2_0.domain.Volume;
import org.jclouds.openstack.nova.v2_0.extensions.FloatingIPApi;
import org.jclouds.openstack.nova.v2_0.extensions.VolumeApi;
import org.jclouds.openstack.nova.v2_0.extensions.VolumeAttachmentApi;
import org.jclouds.openstack.nova.v2_0.options.CreateVolumeOptions;

import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.utils.Utils;
import net.roboconf.target.api.AbstractThreadedTargetHandler.MachineConfigurator;
import net.roboconf.target.api.TargetException;

/**
 * A machine configurator for Openstack.
 * @author Vincent Zurczak - Linagora
 * @author Amadou Diarra - UJF
 */
public class OpenstackMachineConfigurator implements MachineConfigurator {

	/**
	 * A set of locks to prevent concurrent access to the pool of floating IP addresses.
	 * <p>
	 * The idea is to have one lock per Openstack URL. It could be improved by associating
	 * user names too, but this does not seem necessary at the moment.
	 * </p>
	 */
	private static final ConcurrentHashMap<String, Object> URL_TO_LOCK = new ConcurrentHashMap<>();

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
		WAITING_VM, ASSOCIATE_FLOATING_IP, COMPLETE, ASSOCIATE_NETWORK, CREATE_VOLUME, ATTACH_VOLUME;
	}

	private final Instance scopedInstance;
	private final String machineId;
	private final Map<String, String> targetProperties;
	private final Logger logger = Logger.getLogger(getClass().getName());
	private String volumeId = "";
	private NovaApi novaApi;
	//private NeutronApi neutronApi;
	private State state = State.WAITING_VM;

	/**
	 * Constructor.
	 *
	 * @param targetProperties
	 * @param machineId
	 */
	public OpenstackMachineConfigurator(Map<String, String> targetProperties, String machineId, Instance scopedInstance) {
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
		if( this.novaApi != null)
			this.novaApi.close();

		//		if( this.neutronApi != null)
		//			this.neutronApi.close();
	}

	@Override
	public boolean configure() throws TargetException {

		if( this.novaApi == null )
			this.novaApi = OpenstackIaasHandler.novaApi( this.targetProperties );

		//		if( this.neutronApi == null )
		//			this.neutronApi = OpenstackIaasHandler.neutronApi( this.targetProperties );

		if( this.state == State.WAITING_VM )
			if( checkVmIsOnline())
				this.state = State.ASSOCIATE_FLOATING_IP;

		if( this.state == State.ASSOCIATE_FLOATING_IP )
			if( associateFloatingIp())
				this.state = State.CREATE_VOLUME;
		//this.state = State.ASSOCIATE_NETWORK;


		//		if( this.state == State.ASSOCIATE_NETWORK )
		//			if( associateNetwork())
		//				this.state = State.CREATE_VOLUME;


		if( this.state == State.CREATE_VOLUME ) {
			if( createBlockStorage())
				this.state = State.ATTACH_VOLUME;
		}

		if( this.state == State.ATTACH_VOLUME ) {
			if( attachVolume())
				this.state = State.COMPLETE;
		}

		return this.state == State.COMPLETE;
	}

	/**
	 * Checks whether a VM is created.
	 *
	 * @return true if it is online, false otherwise
	 */
	private boolean checkVmIsOnline() {

		String anyZoneName = this.novaApi.getConfiguredZones().iterator().next();
		Server server = this.novaApi.getServerApiForZone(anyZoneName).get(this.machineId);
		return Status.ACTIVE.equals(server.getStatus());
	}

	/**
	 * Associates a floating IP to the VM (if necessary and if possible).
	 *
	 * @return true if this operation successfully completed, false otherwise
	 */
	private boolean associateFloatingIp() {

		// Associating a floating IP requires a client-side synchronization
		// since Openstack does
		// not provide it. Indeed, it can associate a floating IP to a new
		// server, even if this
		// address was already associated with another one. It is not possible,
		// at the moment, to
		// reserve a floating IP. So, we must treat this step with locks.
		String floatingIpPool = this.targetProperties.get(OpenstackIaasHandler.FLOATING_IP_POOL);
		if (Utils.isEmptyOrWhitespaces(floatingIpPool))
			return true;

		// Protected section to prevent using a same IP for several machines.
		// An action is already in progress for this URL?
		// Then return immediately, we will try in the next scheduled run.
		String url = this.targetProperties.get(OpenstackIaasHandler.API_URL);
		if (URL_TO_LOCK.putIfAbsent(url, new Object()) != null)
			return false;

		// Deal with the association.
		boolean done = false;
		try {
			// Find a floating IP
			String availableIp = null;
			String anyZoneName = this.novaApi.getConfiguredZones().iterator().next();
			//String anyZoneName = this.neutronApi.getConfiguredRegions().iterator().next();
			FloatingIPApi floatingIPApi = this.novaApi.getFloatingIPExtensionForZone(anyZoneName).get();
			//FloatingIPApi floatingIPApi = this.neutronApi.getFloatingIPApi(anyZoneName).get();
			//List<IterableWithMarker<FloatingIP>> floatingIps = floatingIPApi.list().toList();
			for(FloatingIP ip : floatingIPApi.list().toList()) {
				if (ip.getFixedIp() == null) {
					availableIp = ip.getIp();
					break;
				}
			}

			// And associate it
			if (availableIp != null)
				floatingIPApi.addToServer(availableIp, this.machineId);
			else
				this.logger.warning("No floating IP was available in Openstack (pool '" + floatingIpPool + "').");

			done = true;

		} finally {
			URL_TO_LOCK.remove(url);
		}

		return done;
	}

	//		/**
	//		 * Associates a Neutron network with the VM (if necessary and if possible).
	//		 * @throws TargetException
	//		 */
	//		private boolean associateNetwork() throws TargetException {
	//
	//			String networkId = this.targetProperties.get( OpenstackIaasHandler.NETWORK_ID );
	//			String anyZoneName = this.novaApi.getConfiguredZones().iterator().next();
	//			boolean isAssociated = true;
	//			if( ! Utils.isEmptyOrWhitespaces( networkId )) {
	//				NeutronApi neutronApi = OpenstackIaasHandler.neutronApi( this.targetProperties );
	//				Network network = neutronApi.getNetworkApi( anyZoneName ).get( networkId );
	//				isAssociated = !Utils.isEmptyOrWhitespaces( network.getId());
	//			}
	//			return isAssociated;
	//		}

	/**
	 * Creates a block storage in Openstack infrastructure.
	 * @throws TargetException
	 */
	public boolean createBlockStorage() throws TargetException {

		String useBlockStorage = Utils.getValue(this.targetProperties, OpenstackIaasHandler.USE_BLOCK_STORAGE, "false");
		boolean isCreated = true;
		if( Boolean.parseBoolean(useBlockStorage)) {
			String vol = Utils.getValue(this.targetProperties, OpenstackIaasHandler.VOLUME_SIZE_GB, OpenstackIaasHandler.DEFAULT_VOLUME_SIZE_GB);
			String name = Utils.getValue(this.targetProperties, OpenstackIaasHandler.VOLUME_NAME, OpenstackIaasHandler.VOLUME_NAME_DEFAULT);
			int vsize = Integer.parseInt(vol);
			String anyZoneName = this.novaApi.getConfiguredZones().iterator().next();
			VolumeApi volumeApi = this.novaApi.getVolumeExtensionForZone(anyZoneName).get();
			this.volumeId = volumeApi.create(vsize, CreateVolumeOptions.Builder.name(name)).getId();
			isCreated = !Utils.isEmptyOrWhitespaces( this.volumeId );
			if( !isCreated )
				throw new TargetException( "No volume created" );
		}
		return isCreated;
	}

	/**
	 * Attach the created volume to a device.
	 * */
	public boolean attachVolume() {
		boolean attach = true;
		if( !Utils.isEmptyOrWhitespaces(this.volumeId)) {
			String anyZoneName = this.novaApi.getConfiguredZones().iterator().next();
			VolumeApi volumeApi = this.novaApi.getVolumeExtensionForZone(anyZoneName).get();
			Volume createdVolume = volumeApi.get(this.volumeId);
			attach = false;
			if( createdVolume.getStatus() == Volume.Status.AVAILABLE ) {
				String device = Utils.getValue(this.targetProperties, OpenstackIaasHandler.VOLUME_MOUNT_POINT, OpenstackIaasHandler.DEFAULT_VOLUME_MOUNT_POINT);
				VolumeAttachmentApi volumeAttachmentApi = this.novaApi.getVolumeAttachmentExtensionForZone(anyZoneName).get();
				volumeAttachmentApi.attachVolumeToServerAsDevice(this.volumeId, this.machineId, device);
				attach = true;
			}
		}
		return attach;
	}
}
