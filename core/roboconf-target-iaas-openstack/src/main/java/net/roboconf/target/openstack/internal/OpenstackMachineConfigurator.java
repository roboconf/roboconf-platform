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

package net.roboconf.target.openstack.internal;

import static net.roboconf.target.openstack.internal.OpenstackIaasHandler.API_URL;
import static net.roboconf.target.openstack.internal.OpenstackIaasHandler.DELETE_ON_TERMINATION;
import static net.roboconf.target.openstack.internal.OpenstackIaasHandler.FLOATING_IP_POOL;
import static net.roboconf.target.openstack.internal.OpenstackIaasHandler.OBJ_STORAGE_DOMAINS;
import static net.roboconf.target.openstack.internal.OpenstackIaasHandler.VOLUME_DELETE_OT_PREFIX;
import static net.roboconf.target.openstack.internal.OpenstackIaasHandler.VOLUME_MOUNT_POINT_PREFIX;
import static net.roboconf.target.openstack.internal.OpenstackIaasHandler.VOLUME_NAME_PREFIX;
import static net.roboconf.target.openstack.internal.OpenstackIaasHandler.VOLUME_SIZE_GB_PREFIX;
import static net.roboconf.target.openstack.internal.OpenstackIaasHandler.VOLUME_TYPE_PREFIX;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.domain.FloatingIP;
import org.jclouds.openstack.nova.v2_0.domain.Server;
import org.jclouds.openstack.nova.v2_0.domain.Server.Status;
import org.jclouds.openstack.nova.v2_0.domain.Volume;
import org.jclouds.openstack.nova.v2_0.extensions.FloatingIPApi;
import org.jclouds.openstack.nova.v2_0.extensions.VolumeApi;
import org.jclouds.openstack.nova.v2_0.extensions.VolumeAttachmentApi;
import org.jclouds.openstack.nova.v2_0.options.CreateVolumeOptions;
import org.jclouds.openstack.swift.v1.SwiftApi;
import org.jclouds.openstack.swift.v1.domain.Container;

import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.utils.Utils;
import net.roboconf.target.api.AbstractThreadedTargetHandler.MachineConfigurator;
import net.roboconf.target.api.TargetException;

/**
 * A machine configurator for Openstack.
 * @author Vincent Zurczak - Linagora
 * @author Amadou Diarra - Université Joseph Fourier
 */
public class OpenstackMachineConfigurator implements MachineConfigurator {

	/**
	 * A set of locks to prevent concurrent access to the pool of floating IP addresses.
	 * <p>
	 * The idea is to have one lock per Openstack URL. It could be improved by associating
	 * user names too, but this does not seem necessary at the moment.
	 * </p>
	 */
	private static final ConcurrentHashMap<String,Object> URL_TO_LOCK = new ConcurrentHashMap<> ();

	/**
	 * The steps of a workflow.
	 * <ul>
	 * <li>WAITING_VM: we wait for the VM to be active.</li>
	 * <li>ASSOCIATE_FLOATING_IP: a floating IP has to be associated, if necessary and if possible.</li>
	 * <li>OBJ_STORAGE: create domains for object storage.</li>
	 * <li>CREATE_VOLUME: create volumes.</li>
	 * <li>ATTACH_VOLUME: attach volumes to the VM.</li>
	 * <li>COMPLETE: there is nothing to do anymore.</li>
	 * </ul>
	 *
	 * @author Vincent Zurczak - Linagora
	 */
	public static enum State {
		WAITING_VM, ASSOCIATE_FLOATING_IP, COMPLETE, OBJ_STORAGE, CREATE_VOLUME, ATTACH_VOLUME;
	}

	private final Instance scopedInstance;
	private final String machineId, applicationName;
	private final Map<String, String> targetProperties;

	private final Logger logger = Logger.getLogger(getClass().getName());
	private final Map<String,String> storageIdToVolumeId = new HashMap<> ();
	private final Map<String,Boolean> volumeIdToAttached = new HashMap<> ();

	private NovaApi novaApi;
	private State state = State.WAITING_VM;


	/**
	 * Constructor.
	 * @param targetProperties
	 * @param machineId
	 */
	public OpenstackMachineConfigurator(
			Map<String, String> targetProperties,
			String machineId,
			String applicationName,
			Instance scopedInstance ) {

		this.machineId = machineId;
		this.applicationName = applicationName;
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
	}


	@Override
	public boolean configure() throws TargetException {

		if( this.novaApi == null )
			this.novaApi = OpenstackIaasHandler.novaApi( this.targetProperties );

		if( this.state == State.WAITING_VM ) {
			if( checkVmIsOnline())
				this.state = State.ASSOCIATE_FLOATING_IP;
		}

		if( this.state == State.ASSOCIATE_FLOATING_IP ) {
			if( associateFloatingIp())
				this.state = State.OBJ_STORAGE;
		}

		if( this.state == State.OBJ_STORAGE ) {
			if( prepareObjectStorage())
				this.state = State.CREATE_VOLUME;
		}

		if( this.state == State.CREATE_VOLUME ) {
			if( createVolumes())
				this.state = State.ATTACH_VOLUME;
		}

		if( this.state == State.ATTACH_VOLUME ) {
			if( attachVolumes())
				this.state = State.COMPLETE;
		}

		return this.state == State.COMPLETE;
	}


	/**
	 * Checks whether a VM is created.
	 * @return true if it is online, false otherwise
	 */
	private boolean checkVmIsOnline() {

		String zoneName = OpenstackIaasHandler.findZoneName( this.novaApi, this.targetProperties );
		Server server = this.novaApi.getServerApiForZone( zoneName ).get(this.machineId);
		return Status.ACTIVE.equals(server.getStatus());
	}


	/**
	 * Associates a floating IP to the VM (if necessary and if possible).
	 * @return true if this operation successfully completed, false otherwise
	 */
	private boolean associateFloatingIp() {

		// Associating a floating IP requires a client-side synchronization
		// since Openstack does not provide it. Indeed, it can associate a floating IP to a new
		// server, even if this address was already associated with another one.
		String floatingIpPool = this.targetProperties.get( FLOATING_IP_POOL );
		if (Utils.isEmptyOrWhitespaces(floatingIpPool))
			return true;

		// Protected section to prevent using a same IP for several machines.
		// An action is already in progress for this URL?
		// Then return immediately, we will try in the next scheduled run.
		String url = this.targetProperties.get( API_URL );
		if (URL_TO_LOCK.putIfAbsent(url, new Object()) != null)
			return false;

		// Deal with the association.
		boolean done = false;
		try {
			// Find a floating IP
			String availableIp = null;
			String zoneName = OpenstackIaasHandler.findZoneName( this.novaApi, this.targetProperties );
			FloatingIPApi floatingIPApi = this.novaApi.getFloatingIPExtensionForZone( zoneName ).get();
			for(FloatingIP ip : floatingIPApi.list().toList()) {
				if (ip.getFixedIp() == null) {
					availableIp = ip.getIp();
					break;
				}
			}

			// And associate it
			if (availableIp != null) {
				floatingIPApi.addToServer(availableIp, this.machineId);
				this.scopedInstance.data.put( OpenstackIaasHandler.FLOATING_IP, availableIp );

			} else {
				this.logger.warning("No floating IP was available in Openstack (pool '" + floatingIpPool + "').");
			}

			done = true;

		} finally {
			URL_TO_LOCK.remove(url);
		}

		return done;
	}


	/**
	 * Configures the object storage.
	 * @return true if the configuration is over
	 * @throws TargetException
	 */
	public boolean prepareObjectStorage() throws TargetException {

		String domains = this.targetProperties.get( OBJ_STORAGE_DOMAINS );
		if( ! Utils.isEmptyOrWhitespaces( domains )) {

			// Get the Swift API
			String zoneName = OpenstackIaasHandler.findZoneName( this.novaApi, this.targetProperties );
			SwiftApi swiftApi = OpenstackIaasHandler.swiftApi( this.targetProperties );

			try {
				// List domains
				List<String> existingDomainNames = new ArrayList<> ();
				for( Container container : swiftApi.getContainerApi( zoneName ).list()) {
					existingDomainNames.add( container.getName());
				}

				// Create missing domains
				List<String> domainsToCreate = Utils.splitNicely( domains, "," );
				domainsToCreate.removeAll( existingDomainNames );
				for( String domainName : domainsToCreate ) {
					this.logger.info( "Creating container " + domainName + " (object storage)..." );
					swiftApi.getContainerApi( zoneName ).create( domainName );
				}

			} catch( Exception e ) {
				throw new TargetException( e );

			} finally {
				// Release the API
				try {
					swiftApi.close();

				} catch( IOException e ) {
					throw new TargetException( e );
				}
			}
		}

		return true;
	}


	/**
	 * Creates block storage volumes in Openstack infrastructure.
	 * <p>
	 * Volume creation goes in a single row. For a given VM, we should enter this method
	 * only once.
	 * </p>
	 *
	 * @throws TargetException
	 */
	public boolean createVolumes() throws TargetException {

		String zoneName = OpenstackIaasHandler.findZoneName( this.novaApi, this.targetProperties );
		for( String storageId : OpenstackIaasHandler.findStorageIds( this.targetProperties )) {

			// Prepare the parameters
			String name = OpenstackIaasHandler.findStorageProperty( this.targetProperties, storageId, VOLUME_NAME_PREFIX );
			name = OpenstackIaasHandler.expandVolumeName( name, this.applicationName, this.scopedInstance.getName());
			VolumeApi volumeApi = this.novaApi.getVolumeExtensionForZone( zoneName ).get();

			// If the volume should not volatile (i.e. not deleted on termination), we try to reuse it, if it exists.
			String deleteOnT = OpenstackIaasHandler.findStorageProperty( this.targetProperties, storageId, VOLUME_DELETE_OT_PREFIX );
			boolean deleteOnTermination = Boolean.parseBoolean( deleteOnT );
			String volumeId = null;
			if( ! deleteOnTermination ) {
				for( Volume vol : volumeApi.list()) {

					if( name.equals( vol.getName())) {
						this.logger.info( "Volume " + name + " (" + vol.getId() + ") already exists and is not volatile. It will be reused." );
						volumeId = vol.getId();
						break;
					}
				}
			}

			// Otherwise, create it.
			if( volumeId == null ) {

				String volumeType = OpenstackIaasHandler.findStorageProperty( this.targetProperties, storageId, VOLUME_TYPE_PREFIX );
				String volumeSize = OpenstackIaasHandler.findStorageProperty( this.targetProperties, storageId, VOLUME_SIZE_GB_PREFIX );
				int vsize = Integer.parseInt( volumeSize );

				CreateVolumeOptions options = CreateVolumeOptions.Builder.name( name );
				if( ! Utils.isEmptyOrWhitespaces( volumeType ))
					options = options.volumeType( volumeType );

				if( deleteOnTermination ) {
					Map<String,String> metadata = new HashMap<>( 1 );
					metadata.put( DELETE_ON_TERMINATION, "true" );
					options = options.metadata( metadata );
				}

				Volume volume = volumeApi.create( vsize, options );
				volumeId = volume.getId();
			}

			if( Utils.isEmptyOrWhitespaces( volumeId ))
				throw new TargetException( "Volume " + name + " was not found and could not be created." );

			this.logger.info( "Volume " + volumeId + " was successfully created." );
			this.storageIdToVolumeId.put( storageId, volumeId );
		}

		return true;
	}


	/**
	 * Attaches the created volume to a device.
	 * <p>
	 * Attachment can be iterative, as not all the volumes may be available
	 * at the same moment.
	 * </p>
	 * */
	public boolean attachVolumes() {

		boolean allAttached = true;
		String zoneName = OpenstackIaasHandler.findZoneName( this.novaApi, this.targetProperties );
		for( Map.Entry<String,String> entry : this.storageIdToVolumeId.entrySet()) {

			String volumeId = entry.getValue();
			String storageId = entry.getKey();

			VolumeApi volumeApi = this.novaApi.getVolumeExtensionForZone( zoneName ).get();
			Volume createdVolume = volumeApi.get( volumeId );

			// Already attached? Skip...
			Boolean attached = this.volumeIdToAttached.get( volumeId );
			if( attached != null && attached )
				continue;

			// Otherwise, try to attach it, if possible.
			if( createdVolume.getStatus() == Volume.Status.AVAILABLE ) {
				String device = OpenstackIaasHandler.findStorageProperty( this.targetProperties, storageId, VOLUME_MOUNT_POINT_PREFIX );
				VolumeAttachmentApi volumeAttachmentApi = this.novaApi.getVolumeAttachmentExtensionForZone( zoneName ).get();
				volumeAttachmentApi.attachVolumeToServerAsDevice( volumeId, this.machineId, device );

				// Notice: there is no way, unlike in AWS, to specify a volume should be deleted when the
				// server terminates. This option is only available with BlockDeviceMapping, which means
				// booting a server from a volume (which is not the same than attaching a volume to a server).
				// See https://review.openstack.org/#/c/67067/

				// BlockDeviceMapping would rather be used when creating the server with Nova.
				// And it is a real mess to understand.

				// FIXME: how about when attachment fails (e.g. if the volume is already attached to another VM)?
				this.volumeIdToAttached.put( volumeId, Boolean.TRUE );
				this.logger.info( "Volume " + volumeId + " was successfully attached to " + this.scopedInstance.getName());
			}

			// Not available, we will attach it later.
			else {
				this.logger.fine( "Volume " + volumeId + " is not yet available to be attached to " + this.scopedInstance.getName());
				allAttached = false;
			}
		}

		return allAttached;
	}
}
