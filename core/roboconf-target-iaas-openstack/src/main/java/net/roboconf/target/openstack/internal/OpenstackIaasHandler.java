/**
 * Copyright 2014 Linagora, Université Joseph Fourier
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
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import net.roboconf.core.agents.DataHelpers;
import net.roboconf.core.utils.Utils;
import net.roboconf.target.api.TargetException;
import net.roboconf.target.api.TargetHandler;

import org.apache.commons.codec.binary.Base64;

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
 * @author Pierre-Yves Gibello - Linagora
 */
public class OpenstackIaasHandler implements TargetHandler {

	public static final String TARGET_ID = "iaas-openstack";

	private final Logger logger;
	private String machineImageId;
	private  Map<String, String> targetProperties;

	private String tenantId;
	private String keypair;
	private String flavor = "m1.tiny";
	private String securityGroup = "default";
	private String floatingIpPool;
	private String token;
	private Nova novaClient;
	private String identityUrl;
	private String computeUrl;


	/**
	 * Constructor.
	 */
	public OpenstackIaasHandler() {
		this.logger = Logger.getLogger( getClass().getName());
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.target.api.TargetHandler#getTargetId()
	 */
	@Override
	public String getTargetId() {
		return TARGET_ID;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.target.api.TargetHandler#setTargetProperties(java.util.Map)
	 */
	@Override
	public void setTargetProperties(Map<String, String> targetProperties) throws TargetException {

		this.targetProperties = targetProperties;

		this.machineImageId = targetProperties.get(OpenstackConstants.IMAGE);
		this.tenantId = targetProperties.get(OpenstackConstants.TENANT_ID);
		this.keypair = targetProperties.get(OpenstackConstants.KEYPAIR);
		this.floatingIpPool = targetProperties.get(OpenstackConstants.FLOATING_IP_POOL);
		this.flavor = targetProperties.get(OpenstackConstants.FLAVOR);
		this.securityGroup = targetProperties.get(OpenstackConstants.SECURITY_GROUP);
		this.identityUrl = targetProperties.get(OpenstackConstants.IDENTITY_URL);
		this.computeUrl = targetProperties.get(OpenstackConstants.COMPUTE_URL);

		try {
			Keystone keystone = new Keystone(this.identityUrl);
			//Keystone keystone = new Keystone("http://localhost:8888/v2.0");

			Authenticate auth = keystone.tokens().authenticate( new UsernamePassword(
							targetProperties.get(OpenstackConstants.USER),
							targetProperties.get(OpenstackConstants.PASSWORD)));

			if(this.tenantId != null)
				auth = auth.withTenantId(this.tenantId);

			Access access = auth.execute();
			this.token = access.getToken().getId();
			keystone.token(this.token);

			String url = this.computeUrl;
			if( ! url.endsWith( "/" ))
				url += "/";

			if( this.tenantId != null )
				url += this.tenantId;

			this.novaClient = new Nova( url );
			this.novaClient.token( this.token );

		} catch(Exception e) {
			throw new TargetException(e);
		}
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.target.api.TargetHandler
	 * #createOrConfigureMachine(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public String createOrConfigureMachine(
			String messagingIp,
			String messagingUsername,
			String messagingPassword,
			String rootInstanceName,
			String applicationName )
	throws TargetException {

		// Normally we use flavor names in the configuration, not IDs
		// But lets's assume it can be an ID...
		String flavorId = this.flavor;
		Flavors flavors = this.novaClient.flavors().list(true).execute();
		for(Flavor f : flavors) {
			if(f.getName().equals(this.flavor))
				flavorId = f.getId();
		}

		ServerForCreate serverForCreate = new ServerForCreate();
		serverForCreate.setName( applicationName + "." + rootInstanceName );
		serverForCreate.setFlavorRef(flavorId);
		serverForCreate.setImageRef(this.machineImageId);
		if(this.keypair != null)
			serverForCreate.setKeyName(this.keypair);

		serverForCreate.getSecurityGroups().add( new ServerForCreate.SecurityGroup(this.securityGroup));

		// User data will be retrieved (like on Amazon WS) on guest OS as
		// http://169.254.169.254/latest/user-data
		try {
			String userData = DataHelpers.writeUserDataAsString( messagingIp, messagingUsername, messagingPassword, applicationName, rootInstanceName );
			String encodedUserData = new String( Base64.encodeBase64( userData.getBytes( "UTF-8" )), "UTF-8" );
			serverForCreate.setUserData( encodedUserData );

		} catch( IOException e ) {
			this.logger.severe( "Failed to create user data. " + e.getMessage());
			this.logger.finest( Utils.writeException( e ));
			throw new TargetException( e );
		}

		// Is there any volume (ID or name) to attach ?
		String volumeIdToAttach = this.targetProperties.get(OpenstackConstants.VOLUME_ID);
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
				String size = this.targetProperties.get(OpenstackConstants.VOLUME_SIZE_GB);
				if(volumeIdToAttach == null && size != null) {
					VolumeForCreate volumeForCreate = new VolumeForCreate();
					volumeForCreate.setName(volumeName);
					volumeForCreate.setDescription("Created by Roboconf");
					volumeForCreate.setSize( Integer.valueOf( size ));

					volumeIdToAttach = this.novaClient.volumes().create(volumeForCreate).execute().getId();
					/*
				SnapshotForCreate snapshotForCreate = new SnapshotForCreate();
				snapshotForCreate.setName(volumeName);
				snapshotForCreate.setVolumeId(volume.getId());
				snapshotForCreate.setDescription("Created by Roboconf");

				this.novaClient.snapshots().create(snapshotForCreate).execute();

				for(Snapshot snapshot : snapshots) {
					if(snapshot.getName().equals(volumeName)) {
						toAttach = snapshot;
						break;
					}
				}
					 */
				}
			}
		}

		// If a network ID is specified (neutron network), use it
		String networkId = this.targetProperties.get(OpenstackConstants.NETWORK_ID);
		if(networkId != null) {
			String fixedIp = this.targetProperties.get(OpenstackConstants.FIXED_IP);
			// fixedIp may be null (DHCP).

			// TODO: uncomment the next line if you want Neutron support
			// (think about updating the openstack-java-sdk version in the POM).
			// serverForCreate.addNetworks(networkId, fixedIp);
		}

		final Server server = this.novaClient.servers().boot(serverForCreate).execute();
		//System.out.println(server);

		// Wait for server to be in ACTIVE state, before associating floating IP and/or attaching volumes
		try {
			final ScheduledThreadPoolExecutor timer = new ScheduledThreadPoolExecutor(1);
			timer.scheduleAtFixedRate(new Runnable() {

				@Override
				public void run() {
					Server checked = OpenstackIaasHandler.this.novaClient.servers().show(server.getId()).execute();
					if("ACTIVE".equals(checked.getStatus())) {
						timer.shutdown();
					}
				}
			}, 10, 5, TimeUnit.SECONDS);
			timer.awaitTermination(120, TimeUnit.SECONDS);

		} catch (Exception ignore) {
			// nothing
		}

		// Attach volume if required
		if(volumeIdToAttach != null) {
			String mountPoint = this.targetProperties.get(OpenstackConstants.VOLUME_MOUNT_POINT);
			if(mountPoint == null)
				mountPoint = "/dev/vdb";

			this.novaClient.servers().attachVolume(server.getId(), volumeIdToAttach, mountPoint).execute();
		}

		// Associate floating IP (nova network) if specified
		if(this.floatingIpPool != null) {
			FloatingIp ip = requestFloatingIp(this.novaClient, server.getId());

			/*FloatingIps ips = this.novaClient.floatingIps().list().execute();

			FloatingIp ip = null;
			for(FloatingIp ip2 : ips) {
				// Look for an IP that is not yet associated to a VM
				if(ip2.getInstanceId() == null) ip = ip2;
			}*/

			if( ip != null ) {
				this.novaClient.servers().associateFloatingIp( server.getId(), ip.getIp()).execute();
			}
		}

		return server.getId();
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.target.api.TargetHandler
	 * #terminateMachine(java.lang.String)
	 */
	@Override
	public void terminateMachine(String instanceId) throws TargetException {
		try {
			this.novaClient.servers().delete(instanceId).execute();
			synchronized(ipAssociations) {
				for(Map.Entry<String, String> entry : ipAssociations.entrySet()) {
					if(instanceId.equals(entry.getValue())) {
						ipAssociations.remove(entry.getKey());
						break;
					}
				}
			}

		} catch(Exception e) {
			throw new TargetException(e);
		}
	}


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


//	public static void main(String args[]) throws Exception {
//
//		Map<String, String> conf = new HashMap<String, String>();
//
//		java.util.Properties p = new java.util.Properties();
//		p.load(new java.io.FileReader(args[0]));
//
//		for( Map.Entry<Object,Object> entry : p.entrySet()) {
//			conf.put( entry.getKey().toString(), entry.getValue().toString());
//		}
//		// conf.put(OpenstackConstants.COMPUTE_URL, "http://localhost:8888/v2");
//
//		OpenstackIaasHandler target = new OpenstackIaasHandler();
//		target.setIaasProperties(conf);
//
//		String channelName = "test";
//		String applicationName = "roboconf";
//		String ipMessagingServer = "localhost";
//		String user = "guest";
//		String pwd = "guest";
//		String serverId = target.createVM( ipMessagingServer, user, pwd, channelName, applicationName);
//		/*Thread.sleep(25000);
//		target.terminateVM(serverId);*/
//	}
}
