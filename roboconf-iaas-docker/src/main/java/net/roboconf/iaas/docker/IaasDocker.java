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

package net.roboconf.iaas.docker;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig.DockerClientConfigBuilder;
import com.github.dockerjava.core.DockerClientImpl;

import net.roboconf.iaas.api.IaasException;
import net.roboconf.iaas.api.IaasInterface;
import net.roboconf.iaas.docker.internal.DockerConstants;

/**
 * @author Pierre-Yves Gibello - Linagora
 */
public class IaasDocker implements IaasInterface {

	private Logger logger;

	private String machineImageId;
	private Map<String, String> iaasProperties;
	private DockerClient docker;

	/**
	 * Constructor.
	 */
	public IaasDocker() {
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
		if((this.machineImageId = iaasProperties.get(DockerConstants.IMAGE_ID)) == null) {
			throw new IaasException(DockerConstants.IMAGE_ID + " is missing!");
		}

		String endpoint = iaasProperties.get(DockerConstants.ENDPOINT);
		
		DockerClientConfigBuilder config = DockerClientConfig.createDefaultConfigBuilder();
		if(endpoint != null) config.withUri(endpoint);

		String username = iaasProperties.get(DockerConstants.USER);
		if(username != null) {
			String password = iaasProperties.get(DockerConstants.PASSWORD);
			if(password == null) password = "";
			String email = iaasProperties.get(DockerConstants.EMAIL);
			if(email == null) email = "";
			
			config.withUsername(username);
			config.withPassword(password);
			config.withEmail(email);
		}

		this.docker = new DockerClientImpl(config.build());
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.iaas.api.IaasInterface
	 * #createVM(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public String createVM(
			String messagingIp,
			String messagingUsername,
			String messagingPassword,
			String rootInstanceName,
			String applicationName )
	throws IaasException {

		CreateContainerResponse container = this.docker
			.createContainerCmd(this.machineImageId)
			.withCmd("/etc/rc.local",
					applicationName,
					rootInstanceName,
					messagingIp,
					messagingUsername,
					messagingPassword)
			.exec();

		this.docker.startContainerCmd(container.getId()).exec();
		//this.docker.waitContainerCmd(container.getId()).exec();
		
		return container.getId();
	}

	/*
	 * (non-Javadoc)
	 * @see net.roboconf.iaas.api.IaasInterface
	 * #terminateVM(java.lang.String)
	 */
	@Override
	public void terminateVM(String instanceId) throws IaasException {
		try {
			this.docker.killContainerCmd(instanceId).exec();
		} catch(Exception e) {
			throw new IaasException(e);
		}
	}

	public static void main(String args[]) throws Exception {

		Map<String, String> conf = new HashMap<String, String>();

		java.util.Properties p = new java.util.Properties();
		if(args.length > 0) p.load(new java.io.FileReader(args[0]));
		else p.load(new java.io.FileReader("/tmp/docker.properties"));

		for( Map.Entry<Object,Object> entry : p.entrySet()) {
			conf.put( entry.getKey().toString(), entry.getValue().toString());
		}

		IaasDocker iaas = new IaasDocker();
		iaas.setIaasProperties(conf);
		
		String rootInstanceName = "test";
		String applicationName = "roboconf";
		
		String ipMessagingServer = java.net.InetAddress.getLocalHost().getHostAddress();
		java.net.NetworkInterface ni = java.net.NetworkInterface.getByName("eth0");    
		java.util.Enumeration<java.net.InetAddress> inetAddresses =  ni.getInetAddresses();
		while(inetAddresses.hasMoreElements()) {  
		         java.net.InetAddress ia = inetAddresses.nextElement();  
		         if(!ia.isLinkLocalAddress()) {  
		        	 ipMessagingServer = ia.getHostAddress();
		         }    
		}

		String user = "roboconf";
		String pwd = "roboconf";
		String containerId = iaas.createVM( ipMessagingServer, user, pwd, rootInstanceName, applicationName);
		System.out.println("Docker container ID=" + containerId);
		/*Thread.sleep(25000);
		iaas.terminateVM(containerId);*/
	}
}
