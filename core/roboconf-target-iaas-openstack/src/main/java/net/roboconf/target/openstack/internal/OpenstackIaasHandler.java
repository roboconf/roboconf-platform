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
import net.roboconf.target.api.TargetHandler;

import org.jclouds.ContextBuilder;
import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.domain.FloatingIP;
import org.jclouds.openstack.nova.v2_0.domain.ServerCreated;
import org.jclouds.openstack.nova.v2_0.extensions.FloatingIPApi;
import org.jclouds.openstack.nova.v2_0.options.CreateServerOptions;
import org.jclouds.openstack.v2_0.domain.Resource;

/**
 * @author Pierre-Yves Gibello - Linagora
 */
public class OpenstackIaasHandler implements TargetHandler {

	public static final String TARGET_ID = "iaas-openstack";
	private static String PROVIDER_ID = "openstack-nova";

	static String IMAGE_NAME = "openstack.image-name";
	static String TENANT_NAME = "openstack.tenant-name";
	static String KEY_PAIR = "openstack.key-pair";
	static String FLAVOR_NAME = "openstack.flavor-name";
	static String SECURITY_GROUP = "openstack.security-group";
	static String NOVA_URL = "openstack.nova-url";
	static String USER = "openstack.user";
	static String PASSWORD = "openstack.password";

	static String FLOATING_IP_POOL = "openstack.floating-ip-pool";
	static String NETWORK_ID = "openstack.network-id";
	// static String VOLUME_ID = "openstack.volumeId";
	// static String VOLUME_MOUNT_POINT = "openstack.volumeMountPoint";
	// static String VOLUME_SIZE_GB = "openstack.volumeSizeGb";

	private final Logger logger = Logger.getLogger( getClass().getName());


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
	 * @see net.roboconf.target.api.TargetHandler
	 * #createOrConfigureMachine(java.util.Map, java.lang.String, java.lang.String,
	 * java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public String createOrConfigureMachine(
			Map<String,String> targetProperties,
			String messagingIp,
			String messagingUsername,
			String messagingPassword,
			String rootInstanceName,
			String applicationName )
	throws TargetException {

		this.logger.fine( "Creating a new machine." );
		NovaApi novaApi = novaApi( targetProperties );
		String anyZoneName = novaApi.getConfiguredZones().iterator().next();

		String machineId = null;
		try {
			// Find flavor and image IDs
			String flavorId = null;
			String flavorName = targetProperties.get( FLAVOR_NAME );
			for( Resource res : novaApi.getFlavorApiForZone( anyZoneName ).list().concat()) {
				if( res.getName().equalsIgnoreCase( flavorName )) {
					flavorId = res.getId();
					break;
				}
			}

			if( flavorId == null )
				throw new TargetException( "No flavor named '" + flavorName + "' was found." );

			String imageId = null;
			String imageName = targetProperties.get( IMAGE_NAME );
			for( Resource res : novaApi.getImageApiForZone( anyZoneName ).list().concat()) {
				if( res.getName().equalsIgnoreCase( imageName )) {
					imageId = res.getId();
					break;
				}
			}

			if( imageId == null )
				throw new TargetException( "No image named '" + imageName + "' was found." );

			// Prepare the server creation
			Map<String,String> metadata = new HashMap<String,String>( 3 );
			metadata.put( "Application Name", applicationName );
			metadata.put( "Root Instance Name", rootInstanceName );
			metadata.put( "Created by", "Roboconf" );

			String userData = DataHelpers.writeUserDataAsString( messagingIp, messagingUsername, messagingPassword, applicationName, rootInstanceName );
			CreateServerOptions options = CreateServerOptions.Builder
			                 .keyPairName( targetProperties.get( KEY_PAIR ))
			                 .securityGroupNames( targetProperties.get( SECURITY_GROUP ))
			                 .userData( userData.getBytes( "UTF-8" ))
			                 .metadata( metadata );

			String networkId = targetProperties.get( NETWORK_ID );
			if( ! Utils.isEmptyOrWhitespaces( networkId ))
				options = options.networks( networkId );

			String vmName = applicationName + "." + rootInstanceName;
			ServerCreated server = novaApi.getServerApiForZone( anyZoneName ).create( vmName, imageId, flavorId, options);
			machineId = server.getId();

			// Floating IPs
			String floatingIpPool = targetProperties.get( FLOATING_IP_POOL );
			if( ! Utils.isEmptyOrWhitespaces( floatingIpPool )) {
				FloatingIPApi floatingIPApi = novaApi.getFloatingIPExtensionForZone( anyZoneName ).get();
				FloatingIP floatingIp = floatingIPApi.allocateFromPool( floatingIpPool );
				if( floatingIp == null )
					throw new TargetException( "No floating IP could be associated to Openstack machine '" + machineId + "'." );

				floatingIPApi.addToServer( floatingIp.getIp(), server.getId());
			}

			// Neutron support
//			String networkId = targetProperties.get( NETWORK_ID );
//			if( ! Utils.isEmptyOrWhitespaces( networkId )) {
//				NeutronApi neutron = computeService.getContext().unwrapApi( NeutronApi.class );
//				String anyRegionName = neutron.getConfiguredRegions().iterator().next();
//				Network network = neutron.getNetworkApi( anyRegionName ).get( networkId );
//				network.
//			}

		} catch( IOException e ) {
			throw new TargetException( "An error occurred while creating a new Openstack server.", e );

		} finally {
			try {
				novaApi.close();

			} catch( IOException e ) {
				throw new TargetException( "An error occurred while creating a new Openstack server.", e );
			}
		}

		return machineId;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.target.api.TargetHandler
	 * #terminateMachine(java.util.Map, java.lang.String)
	 */
	@Override
	public void terminateMachine( Map<String,String> targetProperties, String machineId ) throws TargetException {

		this.logger.fine( "Terminating machine " + machineId );
		try {
			NovaApi novaApi = novaApi( targetProperties );
			String anyZoneName = novaApi.getConfiguredZones().iterator().next();
			novaApi.getServerApiForZone( anyZoneName ).delete( machineId );
			novaApi.close();

		} catch( IOException e ) {
			throw new TargetException( e );
		}
	}


	/**
	 * Creates a JCloud context.
	 * @param targetProperties the target properties
	 * @return a non-null object
	 * @throws TargetException if the target properties are invalid
	 */
	NovaApi novaApi( Map<String,String> targetProperties ) throws TargetException {

		validate( targetProperties );
		return ContextBuilder
				.newBuilder( PROVIDER_ID )
				.endpoint( targetProperties.get( NOVA_URL ))
			    .credentials( identity( targetProperties ), targetProperties.get( PASSWORD ))
			    .buildApi( NovaApi.class );
	}


	/**
	 * Validates the target properties
	 * @param targetProperties the properties
	 * @throws TargetException if an error occurred during the validation
	 */
	static void validate( Map<String,String> targetProperties ) throws TargetException {

		checkProperty( NOVA_URL, targetProperties );
		checkProperty( IMAGE_NAME, targetProperties );
		checkProperty( TENANT_NAME, targetProperties );
		checkProperty( FLAVOR_NAME, targetProperties );
		checkProperty( SECURITY_GROUP, targetProperties );
		checkProperty( KEY_PAIR, targetProperties );
		checkProperty( USER, targetProperties );
		checkProperty( PASSWORD, targetProperties );
	}


	/**
	 * @param targetProperties the target properties (assumed to be valid)
	 * @return the identity
	 */
	static String identity( Map<String,String> targetProperties ) {
		return targetProperties.get( TENANT_NAME ) + ":" + targetProperties.get( USER );
	}


	private static void checkProperty( String propertyName, Map<String,String> targetProperties )
	throws TargetException {

		if( ! targetProperties.containsKey( propertyName ))
			throw new TargetException( "Property '" + propertyName + "' is missing." );

		if( Utils.isEmptyOrWhitespaces( targetProperties.get( propertyName )))
			throw new TargetException( "Property '" + propertyName + "' must have a value." );
	}
}
