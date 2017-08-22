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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.jclouds.ContextBuilder;
import org.jclouds.openstack.neutron.v2.NeutronApi;
import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.domain.FloatingIP;
import org.jclouds.openstack.nova.v2_0.domain.Server;
import org.jclouds.openstack.nova.v2_0.domain.Server.Status;
import org.jclouds.openstack.nova.v2_0.domain.ServerCreated;
import org.jclouds.openstack.nova.v2_0.domain.Volume;
import org.jclouds.openstack.nova.v2_0.domain.VolumeAttachment;
import org.jclouds.openstack.nova.v2_0.extensions.FloatingIPApi;
import org.jclouds.openstack.nova.v2_0.extensions.VolumeApi;
import org.jclouds.openstack.nova.v2_0.extensions.VolumeAttachmentApi;
import org.jclouds.openstack.nova.v2_0.options.CreateServerOptions;
import org.jclouds.openstack.swift.v1.SwiftApi;
import org.jclouds.openstack.v2_0.domain.Resource;

import com.google.common.base.Predicate;

import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.userdata.UserDataHelpers;
import net.roboconf.core.utils.Utils;
import net.roboconf.target.api.AbstractThreadedTargetHandler;
import net.roboconf.target.api.TargetException;
import net.roboconf.target.api.TargetHandlerParameters;

/**
 * @author Pierre-Yves Gibello - Linagora
 * @author Amadou Diarra - UJF
 */
public class OpenstackIaasHandler extends AbstractThreadedTargetHandler {

	public static final String TARGET_ID = "iaas-openstack";
	static final String FLOATING_IP = TARGET_ID + ".floating-ip";

	static final String TPL_VOLUME_NAME = "%NAME%";
	static final String TPL_VOLUME_APP = "%APP%";
	static final String DELETE_ON_TERMINATION = "delete.on.termination";

	private static final String PROVIDER_NOVA = "openstack-nova";
	private static final String PROVIDER_NEUTRON = "openstack-neutron";
	private static final String PROVIDER_SWIFT = "openstack-swift";

	// "Basic" options
	static final String IMAGE_NAME = "openstack.image-name";
	static final String TENANT_NAME = "openstack.tenant-name";
	static final String KEY_PAIR = "openstack.key-pair";
	static final String FLAVOR_NAME = "openstack.flavor-name";
	static final String SECURITY_GROUP = "openstack.security-group";
	static final String API_URL = "openstack.keystone-url";
	static final String USER = "openstack.user";
	static final String PASSWORD = "openstack.password";

	static final String FLOATING_IP_POOL = "openstack.floating-ip-pool";
	static final String NETWORK_ID = "openstack.network-id";
	static final String REGION_NAME = "openstack.region-name";

	// Storage has several options
	static final String USE_BLOCK_STORAGE = "openstack.use-block-storage";

	static final String VOLUME_MOUNT_POINT_PREFIX = "openstack.volume-mount-point.";
	static final String VOLUME_NAME_PREFIX = "openstack.volume-name.";
	static final String VOLUME_SIZE_GB_PREFIX = "openstack.volume-size.";
	static final String VOLUME_DELETE_OT_PREFIX = "openstack.delete-volume-on-termination.";
	static final String VOLUME_TYPE_PREFIX = "openstack.volume-type.";

	// Object storage
	static final String OBJ_STORAGE_DOMAINS = "openstack.obj-storage";

	static final Map<String,String> DEFAULTS = new HashMap<> ();
	static {
		DEFAULTS.put( VOLUME_MOUNT_POINT_PREFIX, "/dev/vdb" );
		DEFAULTS.put( VOLUME_NAME_PREFIX, "roboconf-" + TPL_VOLUME_APP + "-" + TPL_VOLUME_NAME );
		DEFAULTS.put( VOLUME_SIZE_GB_PREFIX, "5" );
		DEFAULTS.put( VOLUME_DELETE_OT_PREFIX, "false" );
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
	 * @see net.roboconf.target.api.TargetHandler
	 * #createMachine(net.roboconf.target.api.TargetHandlerParameters)
	 */
	@Override
	public String createMachine( TargetHandlerParameters parameters ) throws TargetException {

		this.logger.fine( "Creating a new machine." );

		// For IaaS, we only expect root instance names to be passed
		if( InstanceHelpers.countInstances( parameters.getScopedInstancePath()) > 1 )
			throw new TargetException( "Only root instances can be passed in arguments." );

		// Validate all the properties here
		String rootInstanceName = InstanceHelpers.findRootInstancePath( parameters.getScopedInstancePath());
		Map<String,String> targetProperties = parameters.getTargetProperties();
		validateAll( targetProperties, parameters.getApplicationName(), rootInstanceName );

		// Prepare the work
		NovaApi novaApi = OpenstackIaasHandler.novaApi( targetProperties );
		String zoneName = findZoneName( novaApi, targetProperties );
		String vmName = parameters.getApplicationName() + "." + rootInstanceName;

		// Find flavor and image IDs
		String flavorId = null;
		String flavorName = targetProperties.get( OpenstackIaasHandler.FLAVOR_NAME );
		for( Resource res : novaApi.getFlavorApiForZone( zoneName ).list().concat()) {
			if( res.getName().equalsIgnoreCase( flavorName )) {
				flavorId = res.getId();
				break;
			}
		}

		if( flavorId == null )
			throw new TargetException( "No flavor named '" + flavorName + "' was found." );

		String imageId = null;
		String imageName = targetProperties.get( OpenstackIaasHandler.IMAGE_NAME );
		for( Resource res : novaApi.getImageApiForZone( zoneName ).list().concat()) {
			if( res.getName().equalsIgnoreCase( imageName )) {
				imageId = res.getId();
				break;
			}
		}

		if( imageId == null )
			throw new TargetException( "No image named '" + imageName + "' was found." );

		// Prepare the server creation
		Map<String,String> metadata = new HashMap<>(3);
		metadata.put( "Application Name", parameters.getApplicationName());
		metadata.put( "Root Instance Name", rootInstanceName );
		metadata.put( "Created by", "Roboconf" );

		try {
			String userData = UserDataHelpers.writeUserDataAsString(
					parameters.getMessagingProperties(),
					parameters.getDomain(),
					parameters.getApplicationName(),
					rootInstanceName );

			CreateServerOptions options = CreateServerOptions.Builder
					.keyPairName( targetProperties.get( OpenstackIaasHandler.KEY_PAIR ))
					.securityGroupNames( targetProperties.get( OpenstackIaasHandler.SECURITY_GROUP ))
					.userData( userData.getBytes( StandardCharsets.UTF_8 ))
					.metadata( metadata );

			String networkId = targetProperties.get( OpenstackIaasHandler.NETWORK_ID );
			if( ! Utils.isEmptyOrWhitespaces( networkId ))
				options = options.networks( networkId );

			ServerCreated server = novaApi.getServerApiForZone( zoneName ).create( vmName, imageId, flavorId, options);
			String machineId = server.getId();
			novaApi.close();

			return machineId;

		} catch( Exception e ) {
			throw new TargetException( e );
		}
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.target.api.TargetHandler
	 * #isMachineRunning(net.roboconf.target.api.TargetHandlerParameters, java.lang.String)
	 */
	@Override
	public boolean isMachineRunning( TargetHandlerParameters parameters, String machineId )
	throws TargetException {

		NovaApi novaApi = novaApi( parameters.getTargetProperties());
		String zoneName = findZoneName( novaApi, parameters.getTargetProperties());
		Server server = novaApi.getServerApiForZone( zoneName ).get( machineId );

		boolean running = false;
		if( server != null )
			running = server.getStatus() == Status.ACTIVE || server.getStatus() == Status.REBOOT;

		return running;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.target.api.AbstractThreadedTargetHandler#machineConfigurator(
	 * net.roboconf.target.api.TargetHandlerParameters, java.lang.String)
	 */
	@Override
	public MachineConfigurator machineConfigurator( TargetHandlerParameters parameters, String machineId ) {
		return new OpenstackMachineConfigurator(
				parameters.getTargetProperties(),
				machineId,
				parameters.getApplicationName(),
				parameters.getScopedInstance());
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.target.api.TargetHandler
	 * #terminateMachine(net.roboconf.target.api.TargetHandlerParameters, java.lang.String)
	 */
	@Override
	public void terminateMachine( TargetHandlerParameters parameters, String machineId ) throws TargetException {

		try {
			this.logger.info( "Terminating Openstack machine. Machine ID: " + machineId );
			cancelMachineConfigurator( machineId );

			NovaApi novaApi = novaApi( parameters.getTargetProperties());
			String zoneName = findZoneName( novaApi, parameters.getTargetProperties());

			// List the attached volumes, if any.
			Set<String> volumeIds = new HashSet<> ();
			VolumeAttachmentApi volumeAttachmentApi = novaApi.getVolumeAttachmentExtensionForZone( zoneName ).get();
			for( VolumeAttachment vol : volumeAttachmentApi.listAttachmentsOnServer( machineId )) {
				volumeIds.add( vol.getVolumeId());
			}

			// Delete the VM
			novaApi.getServerApiForZone( zoneName ).delete( machineId );

			// Delete the volumes?
			VolumeApi volumeApi = novaApi.getVolumeExtensionForZone( zoneName ).get();
			for( String volumeId : volumeIds ) {

				Volume volume = volumeApi.get( volumeId );
				if( volume == null ) {
					this.logger.warning( "Volume " + volumeId + " was not found. Deletion check is aborted for this volume." );
					continue;
				}

				String del = volume.getMetadata().get( DELETE_ON_TERMINATION );
				if( Boolean.parseBoolean( del )) {
					this.logger.info( "Deleting volume " + volumeId );
					volumeApi.delete( volumeId );

				} else {
					this.logger.info( "Orphan volume " + volumeId + " is kept and will not be deleted." );
				}
			}

			novaApi.close();

		} catch( IOException e ) {
			throw new TargetException( e );
		}
	}


	/* (non-Javadoc)
	 * @see net.roboconf.target.api.TargetHandler
	 * #retrievePublicIpAddress(net.roboconf.target.api.TargetHandlerParameters, java.lang.String)
	 */
	@Override
	public String retrievePublicIpAddress( TargetHandlerParameters parameters, String machineId )
	throws TargetException {

		NovaApi novaApi = novaApi( parameters.getTargetProperties());
		String zoneName = findZoneName( novaApi, parameters.getTargetProperties());

		String result = null;
		Server server = novaApi.getServerApiForZone( zoneName ).get( machineId );
		if( server != null ) {
			result = server.getAccessIPv4();

			// Nothing found? Check floating IPs
			if( result == null ) {
				FloatingIPApi floatingIPApi = novaApi.getFloatingIPExtensionForZone( zoneName ).get();
				List<FloatingIP> ips = floatingIPApi.list().filter( new InstancePredicate( machineId )).toList();
				if( ips.size() > 0 )
					result = ips.get( 0 ).getIp();
			}
		}

		return result;
	}


	/**
	 * A predicate that finds the floating IPs associated with a given server.
	 * @author Vincent Zurczak - Linagora
	 */
	static class InstancePredicate implements Predicate<FloatingIP> {
		private final String instanceId;

		/**
		 * Constructor.
		 * @param instanceId
		 */
		public InstancePredicate( String instanceId ) {
			this.instanceId = instanceId;
		}

		@Override
		public boolean apply( FloatingIP input ) {
			return Objects.equals( input.getInstanceId(), this.instanceId );
		}

	}


	/**
	 * Creates a JCloud context for Nova.
	 * @param targetProperties the target properties
	 * @return a non-null object
	 * @throws TargetException if the target properties are invalid
	 */
	static NovaApi novaApi( Map<String,String> targetProperties ) throws TargetException {

		validate( targetProperties );
		return ContextBuilder
				.newBuilder( PROVIDER_NOVA )
				.endpoint( targetProperties.get( API_URL ))
				.credentials( identity( targetProperties ), targetProperties.get( PASSWORD ))
				.buildApi( NovaApi.class );
	}


	/**
	 * Creates a JCloud context for Swift.
	 * @param targetProperties the target properties
	 * @return a non-null object
	 * @throws TargetException if the target properties are invalid
	 */
	static SwiftApi swiftApi( Map<String,String> targetProperties ) throws TargetException {

		validate( targetProperties );
		return ContextBuilder
				.newBuilder( PROVIDER_SWIFT )
				.endpoint( targetProperties.get( API_URL ))
				.credentials( identity( targetProperties ), targetProperties.get( PASSWORD ))
				.buildApi( SwiftApi.class );
	}


	/**
	 * Creates a JCloud context for Neutron.
	 * @param targetProperties the target properties
	 * @return a non-null object
	 * @throws TargetException if the target properties are invalid
	 */
	// TODO: never used!!!???
	static NeutronApi neutronApi( Map<String,String> targetProperties ) throws TargetException {

		validate( targetProperties );
		return ContextBuilder
				.newBuilder( PROVIDER_NEUTRON )
				.endpoint( targetProperties.get( API_URL ))
				.credentials( identity( targetProperties ), targetProperties.get( PASSWORD ))
				.buildApi( NeutronApi.class );
	}


	/**
	 * @param novaApi the nova client
	 * @param targetProperties the target properties (not null)
	 * @return a zone name (either the specified one, or the first found otherwise)
	 */
	static String findZoneName( NovaApi novaApi, Map<String,String> targetProperties ) {

		String zoneName = targetProperties.get( REGION_NAME );
		if( Utils.isEmptyOrWhitespaces( zoneName ))
			zoneName = novaApi.getConfiguredZones().iterator().next();

		return zoneName;
	}


	/**
	 * Validates the basic target properties.
	 * @param targetProperties the properties
	 * @throws TargetException if an error occurred during the validation
	 */
	static void validate( Map<String,String> targetProperties ) throws TargetException {

		checkProperty( API_URL, targetProperties );
		checkProperty( IMAGE_NAME, targetProperties );
		checkProperty( TENANT_NAME, targetProperties );
		checkProperty( FLAVOR_NAME, targetProperties );
		checkProperty( SECURITY_GROUP, targetProperties );
		checkProperty( KEY_PAIR, targetProperties );
		checkProperty( USER, targetProperties );
		checkProperty( PASSWORD, targetProperties );
	}


	/**
	 * Validates the target properties, including storage ones.
	 * @param targetProperties
	 * @param appName
	 * @param instanceName
	 * @throws TargetException
	 */
	static void validateAll( Map<String,String> targetProperties, String appName, String instanceName )
	throws TargetException {

		// Basic checks
		validate( targetProperties );

		// Storage checks
		Set<String> mountPoints = new HashSet<> ();
		Set<String> volumeNames = new HashSet<> ();
		for( String s : findStorageIds( targetProperties )) {

			// Unit tests should guarantee there is a default value for the "mount point".
			String mountPoint = findStorageProperty( targetProperties, s, VOLUME_MOUNT_POINT_PREFIX );
			if( mountPoints.contains( mountPoint ))
				throw new TargetException( "Mount point '" + mountPoint + "' is already used by another volume for this VM." );

			mountPoints.add( mountPoint );

			// Same thing for the volume name
			String volumeName = findStorageProperty( targetProperties, s, VOLUME_NAME_PREFIX );
			volumeName = expandVolumeName( volumeName, appName, instanceName );
			if( volumeNames.contains( volumeName ))
				throw new TargetException( "Volume name '" + volumeName + "' is already used by another volume for this VM." );

			volumeNames.add( volumeName );

			// Validate volume size
			String volumesize = findStorageProperty( targetProperties, s, VOLUME_SIZE_GB_PREFIX );
			try {
				Integer.valueOf( volumesize );

			} catch( NumberFormatException e ) {
				throw new TargetException( "The volume size must be a valid integer.", e );
			}
		}
	}


	/**
	 * @param targetProperties the target properties (assumed to be valid)
	 * @return the identity
	 */
	static String identity( Map<String,String> targetProperties ) {
		return targetProperties.get( TENANT_NAME ) + ":" + targetProperties.get( USER );
	}


	/**
	 * Finds the storage IDs (used as property suffixes).
	 * @param targetProperties
	 * @return a non-null list
	 */
	static List<String> findStorageIds( Map<String,String> targetProperties ) {

		List<String> result = new ArrayList<> ();
		String prop = targetProperties.get( USE_BLOCK_STORAGE );
		if( ! Utils.isEmptyOrWhitespaces( prop )) {
			for( String s : Utils.splitNicely( prop, "," )) {
				if( ! Utils.isEmptyOrWhitespaces( s ))
					result.add( s );
			}
		}

		return result;
	}


	/**
	 * Finds a storage property for a given storage ID.
	 * @param targetProperties
	 * @param storageId
	 * @param propertyPrefix one of the constants defined in this class
	 * @return the property's value, or the default value otherwise, if one exists
	 */
	static String findStorageProperty( Map<String,String> targetProperties, String storageId, String propertyPrefix ) {

		String property = propertyPrefix + storageId;
		String value = targetProperties.get( property );
		return Utils.isEmptyOrWhitespaces( value ) ? DEFAULTS.get( propertyPrefix ) : value.trim();
	}


	/**
	 * Updates a volume name by replacing template variables.
	 * @param nameTemplate (not null)
	 * @param appName (not null)
	 * @param instanceName (not null)
	 * @return a non-null string
	 */
	static String expandVolumeName( String nameTemplate, String appName, String instanceName ) {

		String name = nameTemplate.replace( TPL_VOLUME_NAME, instanceName );
		name = name.replace( TPL_VOLUME_APP, appName );
		name = name.replaceAll( "[\\W_-]", "-" );

		return name;
	}


	private static void checkProperty( String propertyName, Map<String,String> targetProperties )
	throws TargetException {

		if( ! targetProperties.containsKey( propertyName ))
			throw new TargetException( "Property '" + propertyName + "' is missing." );

		if( Utils.isEmptyOrWhitespaces( targetProperties.get( propertyName )))
			throw new TargetException( "Property '" + propertyName + "' must have a value." );
	}
}
