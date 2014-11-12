/**
 * Copyright 2014 Linagora, Université Joseph Fourier, Floralis
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
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import net.roboconf.core.agents.DataHelpers;

import org.apache.commons.codec.binary.Base64;

import com.woorea.openstack.connector.JerseyConnector;
import com.woorea.openstack.keystone.Keystone;
import com.woorea.openstack.keystone.api.TokensResource.Authenticate;
import com.woorea.openstack.keystone.model.Access;
import com.woorea.openstack.keystone.model.authentication.UsernamePassword;
import com.woorea.openstack.nova.Nova;
import com.woorea.openstack.nova.model.Flavor;
import com.woorea.openstack.nova.model.Flavors;
import com.woorea.openstack.nova.model.FloatingIp;
import com.woorea.openstack.nova.model.FloatingIps;
import com.woorea.openstack.nova.model.Server;
import com.woorea.openstack.nova.model.ServerForCreate;
import com.woorea.openstack.nova.model.Volume;
import com.woorea.openstack.nova.model.VolumeForCreate;
import com.woorea.openstack.nova.model.Volumes;

/**
 * FIXME: should we create a new client instead of storing it?
 * @author Vincent Zurczak - Linagora
 */
public class MachineHandler {

	/**
	 * @author Vincent Zurczak - Linagora
	 */
	public static enum State {
		RAW, COMPUTE, WAITING, STORAGE, NETWORK, COMPLETED;
	}

	private OpenstackBean config;
	private String token;
	private String messagingIp, messagingUsername, messagingPassword, rootInstanceName, applicationName;

	private Nova novaClient;
	private State state = State.RAW;
	private String serverId;
	private final Logger logger = Logger.getLogger( getClass().getName());


	/**
	 * Resumes the creation and the configuration of an Openstack machine.
	 * @throws IOException
	 */
	public void resume() throws IOException {

		switch( this.state ) {
		case RAW:
			raw();
			break;

		case COMPUTE:
			prepare();
			break;

		case WAITING:
			waitForMachine();
			break;

		case STORAGE:
			storage();
			break;

		case NETWORK:
			network();
			break;

		default: break;
		}
	}


	public void terminate( String instanceId ) {

		this.novaClient.servers().delete( instanceId ).execute();
		synchronized( ipAssociations ) {
			for(Map.Entry<String, String> entry : ipAssociations.entrySet()) {
				if( instanceId.equals(entry.getValue())) {
					ipAssociations.remove( entry.getKey());
					break;
				}
			}
		}
	}



	private void raw() {

		this.logger.fine( "Initiating a connection with Openstack..." );
		Keystone keystone = new Keystone( this.config.getIdentityUrl(), new JerseyConnector());
		Authenticate auth = keystone.tokens().authenticate( new UsernamePassword( this.config.getUser(), this.config.getPassword()));
		if( this.config.getTenantId() != null )
			auth = auth.withTenantId( this.config.getTenantId());

		Access access = auth.execute();
		this.token = access.getToken().getId();
		keystone.token(this.token);

		String url = this.config.getComputeUrl();
		if( ! url.endsWith( "/" ))
			url += "/";

		if( this.config.getTenantId() != null )
			url += this.config.getTenantId();

		this.novaClient = new Nova( url, new JerseyConnector());
		this.novaClient.token( this.token );
		this.state = State.COMPUTE;
	}



	private void prepare() throws IOException {
		this.logger.fine( "Instantiating a new server..." );

		// Normally we use flavor names in the configuration, not IDs
		// But lets's assume it can be an ID...
		String flavorId = this.config.getFlavor();
		Flavors flavors = this.novaClient.flavors().list(true).execute();
		for(Flavor f : flavors) {
			if( f.getName().equals( this.config.getFlavor()))
				flavorId = f.getId();
		}

		ServerForCreate serverForCreate = new ServerForCreate();
		serverForCreate.setName( this.applicationName + "." + this.rootInstanceName );
		serverForCreate.setFlavorRef(flavorId);
		serverForCreate.setImageRef( this.config.getMachineImageId());
		if( this.config.getKeypair() != null)
			serverForCreate.setKeyName( this.config.getKeypair());

		serverForCreate.getSecurityGroups().add( new ServerForCreate.SecurityGroup( this.config.getSecurityGroup()));

		// User data will be retrieved (like on Amazon WS) on guest OS as
		// http://169.254.169.254/latest/user-data
		String userData = DataHelpers.writeUserDataAsString( this.messagingIp, this.messagingUsername, this.messagingPassword, this.applicationName, this.rootInstanceName );
		String encodedUserData = new String( Base64.encodeBase64( userData.getBytes( "UTF-8" )), "UTF-8" );
		serverForCreate.setUserData( encodedUserData );

		// Is there any volume (ID or name) to attach ?
		String volumeIdToAttach = this.config.getVolumeId();
		if(volumeIdToAttach != null) {

			boolean volumeFound = false;
			try {
				// Volume not found may return a 404... that in turn throws a RuntimeException !
				volumeFound = (this.novaClient.volumes().show(volumeIdToAttach).execute() != null);
			} catch(Throwable t) {
				volumeFound = false;
			}

			if(! volumeFound) {
				// Volume not found: assume volume ID is in fact a volume name.
				String volumeName = volumeIdToAttach;
				volumeIdToAttach = null;
				Volumes volumes = this.novaClient.volumes().list(false).execute();
				for(Volume volume : volumes) {
					if(volume.getName().equals(volumeName)) {
						volumeIdToAttach = volume.getId();
						break;
					}
				}

				// Volume not found by name ? Create one if requested (= size specified)
				String size = this.config.getVolumeSize();
				if(volumeIdToAttach == null && size != null) {
					VolumeForCreate volumeForCreate = new VolumeForCreate();
					volumeForCreate.setName(volumeName);
					volumeForCreate.setDescription("Created by Roboconf");
					volumeForCreate.setSize( Integer.valueOf( size ));

					volumeIdToAttach = this.novaClient.volumes().create(volumeForCreate).execute().getId();
				}
			}
		}

		// If a network ID is specified (neutron network), use it
		String networkId = this.config.getNetworkId();
		if(networkId != null) {
			String fixedIp = this.config.getFixedIp();
			// fixedIp may be null (DHCP).

			// TODO: uncomment the next line if you want Neutron support
			// (think about updating the openstack-java-sdk version in the POM).
			serverForCreate.addNetworks(networkId, fixedIp);
		}

		Server server = this.novaClient.servers().boot(serverForCreate).execute();
		this.serverId = server.getId();
		this.state = State.WAITING;
	}



	private void waitForMachine() {

		this.logger.fine( "Waiting for the new server to be online..." );
		Server checked = this.novaClient.servers().show( this.serverId ).execute();
		if("ACTIVE".equals(checked.getStatus()))
			this.state = State.STORAGE;
	}


	private void storage() {

		this.logger.fine( "Configuring the storage..." );
		String volumeIdToAttach = this.config.getVolumeId();
		if( volumeIdToAttach != null) {
			String mountPoint = this.config.getVolumeMountPoint();
			if(mountPoint == null)
				mountPoint = "/dev/vdb";

			this.novaClient.servers().attachVolume( this.serverId, volumeIdToAttach, mountPoint ).execute();
		}

		this.state = State.NETWORK;
	}


	private void network() {

		this.logger.fine( "Configuring the network..." );
		if( this.config.getFloatingIpPool() != null) {
			FloatingIp ip = requestFloatingIp( this.novaClient, this.serverId );

			/*FloatingIps ips = this.novaClient.floatingIps().list().execute();

					FloatingIp ip = null;
					for(FloatingIp ip2 : ips) {
						// Look for an IP that is not yet associated to a VM
						if(ip2.getInstanceId() == null) ip = ip2;
					}*/

			if( ip != null ) {
				this.novaClient.servers().associateFloatingIp( this.serverId, ip.getIp()).execute();
			}
		}

		this.state = State.COMPLETED;
	}


	/**
	 * @return the current state
	 */
	public State getState() {
		return this.state;
	}


	/**
	 * @return the server ID
	 */
	public String getServerId() {
		return this.serverId;
	}



	// Baaaaaaaaaad!!!!!

	private static HashMap<String, String> ipAssociations = new HashMap<String, String>();
	private static synchronized FloatingIp requestFloatingIp(Nova novaClient, String serverId) {
		FloatingIps ips = novaClient.floatingIps().list().execute();

		FloatingIp ip = null;
		for(FloatingIp ip2 : ips) {
			// Look for an IP that is not yet associated to a VM
			if(ip2.getInstanceId() == null && ipAssociations.get(ip2.getId()) == null) {
				ip = ip2;
				ipAssociations.put(ip2.getId(), serverId);
				break;
			}
		}

		return ip;
	}


	// Setters and getters


	/**
	 * @return the config
	 */
	public OpenstackBean getConfig() {
		return this.config;
	}


	/**
	 * @param config the config to set
	 */
	public void setConfig( OpenstackBean config ) {
		this.config = config;
	}


	/**
	 * @return the messagingIp
	 */
	public String getMessagingIp() {
		return this.messagingIp;
	}


	/**
	 * @param messagingIp the messagingIp to set
	 */
	public void setMessagingIp( String messagingIp ) {
		this.messagingIp = messagingIp;
	}


	/**
	 * @return the messagingUsername
	 */
	public String getMessagingUsername() {
		return this.messagingUsername;
	}


	/**
	 * @param messagingUsername the messagingUsername to set
	 */
	public void setMessagingUsername( String messagingUsername ) {
		this.messagingUsername = messagingUsername;
	}


	/**
	 * @return the messagingPassword
	 */
	public String getMessagingPassword() {
		return this.messagingPassword;
	}


	/**
	 * @param messagingPassword the messagingPassword to set
	 */
	public void setMessagingPassword( String messagingPassword ) {
		this.messagingPassword = messagingPassword;
	}


	/**
	 * @return the rootInstanceName
	 */
	public String getRootInstanceName() {
		return this.rootInstanceName;
	}


	/**
	 * @param rootInstanceName the rootInstanceName to set
	 */
	public void setRootInstanceName( String rootInstanceName ) {
		this.rootInstanceName = rootInstanceName;
	}


	/**
	 * @return the applicationName
	 */
	public String getApplicationName() {
		return this.applicationName;
	}


	/**
	 * @param applicationName the applicationName to set
	 */
	public void setApplicationName( String applicationName ) {
		this.applicationName = applicationName;
	}
}
