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
import net.roboconf.core.utils.Utils;
import net.roboconf.target.api.TargetException;

import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.domain.FloatingIP;
import org.jclouds.openstack.nova.v2_0.domain.Server;
import org.jclouds.openstack.nova.v2_0.domain.Server.Status;
import org.jclouds.openstack.nova.v2_0.domain.ServerCreated;
import org.jclouds.openstack.nova.v2_0.extensions.FloatingIPApi;
import org.jclouds.openstack.nova.v2_0.options.CreateServerOptions;
import org.jclouds.openstack.v2_0.domain.Resource;

/**
 * Some configuration actions need the server to be created.
 * <p>
 * This is the case, as an example, of the floating IP.
 * Therefore, we need to wait the server to be online. This class
 * provides a small and efficient workflow to regularly poll the state
 * of a server under creation. When a server is created, it performs the right
 * actions to complete the configuration.
 * </p>
 *
 * @author Vincent Zurczak - Linagora
 */
public class MachineLauncher {

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

	private Map<String,String> targetProperties;
	private String messagingIp, messagingUsername, messagingPassword, rootInstanceName, applicationName;

	private NovaApi novaApi;
	private State state = State.WAITING_VM;
	private String serverId, vmName, anyZoneName;
	private final Logger logger = Logger.getLogger( getClass().getName());


	/**
	 * Resumes the creation and the configuration of an Openstack machine.
	 * <p>
	 * Must be invoked after {@link #createVm()}.<br />
	 * Must be invoked until {@link #getState()} is {@value State#COMPLETE}.
	 * </p>
	 * @throws TargetException
	 */
	public void resume() throws TargetException {

		switch( this.state ) {
		case WAITING_VM:
			checkVmIsOnline();
			break;

		case ASSOCIATE_FLOATING_IP:
			associateFloatingIp();
			break;

//		case ASSOCIATE_NETWORK:
//			associateNetwork();
//			break;

		default:
			break;
		}
	}


	/**
	 * Creates a VM.
	 * @throws TargetException
	 * @throws IOException
	 */
	public void createVm() throws TargetException, IOException {

		this.novaApi = OpenstackIaasHandler.novaApi( this.targetProperties );
		this.anyZoneName = this.novaApi.getConfiguredZones().iterator().next();
		this.vmName = this.applicationName + "." + this.rootInstanceName;

		// Find flavor and image IDs
		String flavorId = null;
		String flavorName = this.targetProperties.get( OpenstackIaasHandler.FLAVOR_NAME );
		for( Resource res : this.novaApi.getFlavorApiForZone( this.anyZoneName ).list().concat()) {
			if( res.getName().equalsIgnoreCase( flavorName )) {
				flavorId = res.getId();
				break;
			}
		}

		if( flavorId == null )
			throw new TargetException( "No flavor named '" + flavorName + "' was found." );

		String imageId = null;
		String imageName = this.targetProperties.get( OpenstackIaasHandler.IMAGE_NAME );
		for( Resource res : this.novaApi.getImageApiForZone( this.anyZoneName ).list().concat()) {
			if( res.getName().equalsIgnoreCase( imageName )) {
				imageId = res.getId();
				break;
			}
		}

		if( imageId == null )
			throw new TargetException( "No image named '" + imageName + "' was found." );

		// Prepare the server creation
		Map<String,String> metadata = new HashMap<String,String>( 3 );
		metadata.put( "Application Name", this.applicationName );
		metadata.put( "Root Instance Name", this.rootInstanceName );
		metadata.put( "Created by", "Roboconf" );

		String userData = DataHelpers.writeUserDataAsString( this.messagingIp, this.messagingUsername, this.messagingPassword, this.applicationName, this.rootInstanceName );
		CreateServerOptions options = CreateServerOptions.Builder
				.keyPairName( this.targetProperties.get( OpenstackIaasHandler.KEY_PAIR ))
				.securityGroupNames( this.targetProperties.get( OpenstackIaasHandler.SECURITY_GROUP ))
				.userData( userData.getBytes( "UTF-8" ))
				.metadata( metadata );

		String networkId = this.targetProperties.get( OpenstackIaasHandler.NETWORK_ID );
		if( ! Utils.isEmptyOrWhitespaces( networkId ))
			options = options.networks( networkId );

		ServerCreated server = this.novaApi.getServerApiForZone( this.anyZoneName ).create( this.vmName, imageId, flavorId, options);
		this.serverId = server.getId();
		this.state = State.WAITING_VM;
	}


	/**
	 * Checks whether a VM is created.
	 */
	private void checkVmIsOnline() {

		Server server = this.novaApi.getServerApiForZone( this.anyZoneName ).get( this.serverId );
		if( Status.ACTIVE.equals( server.getStatus()))
			this.state = State.ASSOCIATE_FLOATING_IP;
	}


	/**
	 * Associates a floating IP to the VM (if necessary and if possible).
	 */
	private void associateFloatingIp() {

		String floatingIpPool = this.targetProperties.get( OpenstackIaasHandler.FLOATING_IP_POOL );
		if( ! Utils.isEmptyOrWhitespaces( floatingIpPool )) {

			// Protected section to prevent using a same IP for several machines.
			// FIXME: this could be optimized per novaUrl (have a lock per Openstack API).
			synchronized( this ) {

				// Find a floating IP
				FloatingIPApi floatingIPApi = this.novaApi.getFloatingIPExtensionForZone( this.anyZoneName ).get();
				String availableIp = null;
				for( FloatingIP ip : floatingIPApi.list().toList()) {
					if( ip.getInstanceId() == null ) {
						availableIp = ip.getIp();
						break;
					}
				}

				if( availableIp != null )
					floatingIPApi.addToServer( availableIp, this.serverId );
				else
					this.logger.warning( "No floating IP was available in Openstack (pool '" + floatingIpPool + "'). VM '" + this.vmName + "' will only have a private IP address." );
			}
		}

		complete();
	}


//	/**
//	 * Associates a Neutron network to the VM (if necessary and if possible).
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
//
//		complete();
//	}


	/**
	 * Closes the Nova client.
	 */
	private void complete() {

		this.state = State.COMPLETE;
		try {
			this.novaApi.close();

		} catch( IOException e ) {
			this.logger.warning( "A Nova client could not be closed. " + e.getMessage());
			Utils.logException( this.logger, e );
		}
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


	/**
	 * @param messagingIp the messagingIp to set
	 */
	public void setMessagingIp( String messagingIp ) {
		this.messagingIp = messagingIp;
	}


	/**
	 * @param messagingUsername the messagingUsername to set
	 */
	public void setMessagingUsername( String messagingUsername ) {
		this.messagingUsername = messagingUsername;
	}


	/**
	 * @param messagingPassword the messagingPassword to set
	 */
	public void setMessagingPassword( String messagingPassword ) {
		this.messagingPassword = messagingPassword;
	}


	/**
	 * @param rootInstanceName the rootInstanceName to set
	 */
	public void setRootInstanceName( String rootInstanceName ) {
		this.rootInstanceName = rootInstanceName;
	}


	/**
	 * @param applicationName the applicationName to set
	 */
	public void setApplicationName( String applicationName ) {
		this.applicationName = applicationName;
	}


	/**
	 * @param targetProperties the targetProperties to set
	 */
	public void setTargetProperties( Map<String,String> targetProperties ) {
		this.targetProperties = targetProperties;
	}


	/**
	 * @return the vmName
	 */
	public String getVmName() {
		return this.vmName;
	}
}
