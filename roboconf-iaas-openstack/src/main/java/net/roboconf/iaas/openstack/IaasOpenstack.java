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

package net.roboconf.iaas.openstack;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import net.roboconf.iaas.api.IaasException;
import net.roboconf.iaas.api.IaasInterface;

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
public class IaasOpenstack implements IaasInterface {

	private Logger logger;

	private String machineImageId;
	private  Map<String, String> iaasProperties;

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
	public IaasOpenstack() {
		this.logger = Logger.getLogger( getClass().getName());
	}


	/**
	 * @param logger the logger to set
	 */
	public void setLogger( Logger logger ) {
		this.logger = logger;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.iaas.api.IaasInterface
	 * #setIaasProperties(net.roboconf.iaas.api.IaasProperties)
	 */
	@Override
	public void setIaasProperties(Map<String, String> iaasProperties) throws IaasException {

		this.iaasProperties = iaasProperties;

		this.machineImageId = iaasProperties.get(OpenstackConstants.IMAGE);
		this.tenantId = iaasProperties.get(OpenstackConstants.TENANT_ID);
		this.keypair = iaasProperties.get(OpenstackConstants.KEYPAIR);
		this.floatingIpPool = iaasProperties.get(OpenstackConstants.FLOATING_IP_POOL);
		String val = iaasProperties.get(OpenstackConstants.FLAVOR);
		if(val != null) this.flavor = val;
		val = iaasProperties.get(OpenstackConstants.SECURITY_GROUP);
		if(val != null) this.securityGroup = val;

		this.identityUrl = iaasProperties.get(OpenstackConstants.IDENTITY_URL);
		this.computeUrl = iaasProperties.get(OpenstackConstants.COMPUTE_URL);

		try {
			Keystone keystone = new Keystone(this.identityUrl);
			//Keystone keystone = new Keystone("http://localhost:8888/v2.0");

			Authenticate auth = keystone.tokens().authenticate(
					new UsernamePassword(iaasProperties.get(OpenstackConstants.USER),
							iaasProperties.get(OpenstackConstants.PASSWORD)));
			if(this.tenantId != null) auth = auth.withTenantId(this.tenantId);

			Access access = auth.execute();
			this.token = access.getToken().getId();
			keystone.token(this.token);

			this.novaClient = new Nova(this.computeUrl
					+ (this.tenantId == null ? ""
						: (this.computeUrl.endsWith("/") ? "" : "/") + this.tenantId));
			this.novaClient.token(this.token);

		} catch(Exception e) {
			throw new IaasException(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see net.roboconf.iaas.api.IaasInterface
	 * #createVM(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public String createVM(
			String machineImageId,
			String ipMessagingServer,
			String channelName,
			String applicationName)
	throws IaasException {

		if(machineImageId == null || "".equals(machineImageId))
			machineImageId = this.machineImageId;

		// Normally we use flavor names in the configuration, not IDs
		// But lets's assume it can be an ID...
		String flavorId = this.flavor;
		Flavors flavors = this.novaClient.flavors().list(true).execute();
		for(Flavor f : flavors) {
			if(f.getName().equals(this.flavor)) flavorId = f.getId();
		}

		ServerForCreate serverForCreate = new ServerForCreate();
		serverForCreate.setName(applicationName + "." + channelName);
		serverForCreate.setFlavorRef(flavorId);
		serverForCreate.setImageRef(machineImageId);
		if(this.keypair != null) serverForCreate.setKeyName(this.keypair);
		serverForCreate.getSecurityGroups().add(
			new ServerForCreate.SecurityGroup(this.securityGroup));

		// User data will be retrieved (like on Amazon WS) on guest OS as
		// http://169.254.169.254/latest/user-data
		String userData = "applicationName=" + applicationName
				+ "\nmachineName=" + channelName //TBD machineName=channelName
				+ "\nchannelName=" + channelName
				+ "\nipMessagingServer=" + ipMessagingServer;
		serverForCreate.setUserData(new String(Base64.encodeBase64(userData.getBytes())));

		// Is there any volume (ID or name) to attach ?
		String volumeIdToAttach = this.iaasProperties.get(OpenstackConstants.VOLUME_ID);
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
				String size = this.iaasProperties.get(OpenstackConstants.VOLUME_SIZE_GB);
				if(volumeIdToAttach == null && size != null) {
					VolumeForCreate volumeForCreate = new VolumeForCreate();
					volumeForCreate.setName(volumeName);
					volumeForCreate.setDescription("Created by Roboconf");
					volumeForCreate.setSize(new Integer(size));

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

		final Server server = this.novaClient.servers().boot(serverForCreate).execute();
		//System.out.println(server);

		// Wait for server to be in ACTIVE state, before associating floating IP and/or attaching volumes
		try {
			final ScheduledThreadPoolExecutor timer = new ScheduledThreadPoolExecutor(1);
			timer.scheduleAtFixedRate(new Runnable() {

				@Override
				public void run() {
					Server checked = IaasOpenstack.this.novaClient.servers().show(server.getId()).execute();
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
			String mountPoint = this.iaasProperties.get(OpenstackConstants.VOLUME_MOUNT_POINT);
			if(mountPoint == null) mountPoint = "/dev/vdb";
			this.novaClient.servers().attachVolume(server.getId(), volumeIdToAttach, mountPoint).execute();
		}

		// Associate floating IP
		if(this.floatingIpPool != null) {
			FloatingIps ips = this.novaClient.floatingIps().list().execute();

			FloatingIp ip = null;
			for(FloatingIp ip2 : ips) {
				//System.out.println("ip=" + ip2);
				ip = ip2;
			}
			//FloatingIp ip = ips.allocate(this.floatingIpPool).execute();
			if(ip != null) {
				this.novaClient.servers().associateFloatingIp(
						server.getId(), ip.getIp()).execute();
			}
		}

		return server.getId();
	}

	/*
	 * (non-Javadoc)
	 * @see net.roboconf.iaas.api.IaasInterface
	 * #terminateVM(java.lang.String)
	 */
	@Override
	public void terminateVM(String instanceId) throws IaasException {
		try {
			this.novaClient.servers().delete(instanceId).execute();
		} catch(Exception e) {
			throw new IaasException(e);
		}
	}

	public static void main(String args[]) throws Exception {

		Map<String, String> conf = new HashMap<String, String>();

		java.util.Properties p = new java.util.Properties();
		p.load(new java.io.FileReader(args[0]));

		for( Map.Entry<Object,Object> entry : p.entrySet()) {
			conf.put( entry.getKey().toString(), entry.getValue().toString());
		}
		// conf.put(OpenstackConstants.COMPUTE_URL, "http://localhost:8888/v2");

		IaasOpenstack iaas = new IaasOpenstack();
		iaas.setIaasProperties(conf);

		String machineImageId = conf.get(OpenstackConstants.IMAGE);
		String channelName = "test";
		String applicationName = "roboconf";
		String ipMessagingServer = "localhost";
		String serverId = iaas.createVM(machineImageId, ipMessagingServer, channelName, applicationName);
		/*Thread.sleep(25000);
		iaas.terminateVM(serverId);*/
	}
}
