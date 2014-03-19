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

import net.roboconf.iaas.api.IaasInterface;
import net.roboconf.iaas.api.exceptions.CommunicationToIaasException;
import net.roboconf.iaas.api.exceptions.IaasException;
import net.roboconf.iaas.api.exceptions.InvalidIaasPropertiesException;

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

/**
 * @author Pierre-Yves Gibello - Linagora
 */
public class IaasOpenstack implements IaasInterface {

	//TODO replace all println() with logger...
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
	public void setIaasProperties(Map<String, String> iaasProperties) throws InvalidIaasPropertiesException {

		this.iaasProperties = iaasProperties;

		this.machineImageId = iaasProperties.get("openstack.image");
		this.tenantId = iaasProperties.get("openstack.tenantId");
		this.keypair = iaasProperties.get("openstack.keypair");
		this.floatingIpPool = iaasProperties.get("openstack.floatingIpPool");
		String val = iaasProperties.get("openstack.flavor");
		if(val != null) this.flavor = val;
		val = iaasProperties.get("openstack.securityGroup");
		if(val != null) this.securityGroup = val;
		
		this.identityUrl = iaasProperties.get("openstack.identityUrl");
		this.computeUrl = iaasProperties.get("openstack.computeUrl");
		
		try {
			Keystone keystone = new Keystone(this.identityUrl);
			//Keystone keystone = new Keystone("http://localhost:8888/v2.0");
			
			Authenticate auth = keystone.tokens().authenticate(
					new UsernamePassword(iaasProperties.get("openstack.user"),
							iaasProperties.get("openstack.password")));
			if(this.tenantId != null) auth = auth.withTenantId(this.tenantId);

			Access access = auth.execute();
			this.token = access.getToken().getId();
			keystone.token(this.token);

			this.novaClient = new Nova(this.computeUrl
					+ (this.tenantId == null ? ""
						: (this.computeUrl.endsWith("/") ? "" : "/") + this.tenantId));
			this.novaClient.token(this.token);

		} catch(Exception e) {
			throw new InvalidIaasPropertiesException(e);
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
	throws IaasException, CommunicationToIaasException {

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
		
		final Server server = this.novaClient.servers().boot(serverForCreate).execute();
		System.out.println(server);

		// Wait for server to be in ACTIVE state, before associating floating IP
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
		} catch (Exception ignore) { /*ignore*/ }

		// Associate floating IP
		if(this.floatingIpPool != null) {
			FloatingIps ips = this.novaClient.floatingIps().list().execute();
			
			FloatingIp ip = null;
			for(FloatingIp ip2 : ips) {
				System.out.println("ip=" + ip2);
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
	public void terminateVM(String instanceId) throws IaasException, CommunicationToIaasException {
		try {
			this.novaClient.servers().delete(instanceId).execute();
		} catch(Exception e) {
			throw new CommunicationToIaasException(e);
		}
	}

	public static void main(String args[]) throws Exception {
		
		Map<String, String> conf = new HashMap<String, String>();
		
		java.util.Properties p = new java.util.Properties();
		p.load(new java.io.FileReader(args[0]));
		
		for(Object name : p.keySet()) {
			conf.put(name.toString(), p.get(name).toString());
		}
		// conf.put("openstack.computeUrl", "http://localhost:8888/v2");

		IaasOpenstack iaas = new IaasOpenstack();
		iaas.setIaasProperties(conf);

		String machineImageId = conf.get("openstack.image");
		String channelName = "test";
		String applicationName = "roboconf";
		String ipMessagingServer = "localhost";
		String serverId = iaas.createVM(machineImageId, ipMessagingServer, channelName, applicationName);
		/*Thread.sleep(25000);
		iaas.terminateVM(serverId);*/
	}
}
