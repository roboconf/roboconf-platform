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

package net.roboconf.target.jclouds.internal;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.jclouds.ContextBuilder;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.RunNodesException;
import org.jclouds.compute.domain.Hardware;
import org.jclouds.compute.domain.Image;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.Template;

import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.utils.Utils;
import net.roboconf.target.api.TargetException;
import net.roboconf.target.api.TargetHandler;
import net.roboconf.target.api.TargetHandlerParameters;

/**
 * FIXME: add user data support.
 * Like key pairs, userData is not a generic mechanism with JClouds.
 * However, running a script after may be better.
 *
 * @author Vincent ZURCZAK - Linagora
 */
public class JCloudsHandler implements TargetHandler {

	public static final String TARGET_ID = "jclouds";

	static final String PROVIDER_ID = "jclouds.provider-id";
	static final String IDENTITY = "jclouds.identity";
	static final String CREDENTIAL = "jclouds.credential";
	static final String ENDPOINT = "jclouds.endpoint";
	static final String IMAGE_NAME = "jclouds.image-name";
	static final String SECURITY_GROUP = "jclouds.security-group";
	static final String KEY_PAIR = "jclouds.key-pair";
	static final String HARDWARE_NAME = "jclouds.hardware-name";

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
	 * #createMachine(net.roboconf.target.api.TargetHandlerParameters)
	 */
	@Override
	public String createMachine( TargetHandlerParameters parameters ) throws TargetException {

		this.logger.fine( "Creating a new machine." );
		Map<String,String> targetProperties = parameters.getTargetProperties();
		final String providerId = targetProperties.get( PROVIDER_ID );
		ComputeService computeService = jcloudContext( targetProperties );

		// For IaaS, we only expect root instance names to be passed
		if( InstanceHelpers.countInstances( parameters.getScopedInstancePath()) > 1 )
			throw new TargetException( "Only root instances can be passed in arguments." );

		String rootInstanceName = InstanceHelpers.findRootInstancePath( parameters.getScopedInstancePath());
		String machineId = null;
		try {
			// Create a template from an image and a flavor/hardware
			Image image = null;
			String imageName = targetProperties.get( IMAGE_NAME );
			for( Image i : computeService.listImages()) {
				if( i.getName().equalsIgnoreCase( imageName )) {
					image = i;
					break;
				}
			}

			if( image == null )
				throw new TargetException( "No image named '" + imageName + "' was found." );

			Hardware hardware = null;
			String hardwareName = targetProperties.get( HARDWARE_NAME );
			for( Hardware h : computeService.listHardwareProfiles()) {
				if( h.getName().equalsIgnoreCase( hardwareName )) {
					hardware = h;
					break;
				}
			}

			if( hardware == null )
				throw new TargetException( "No hardware named '" + hardwareName + "' was found." );

			Template template = computeService.templateBuilder().fromImage( image ).hardwareId( hardware.getId()).build();
			template.getOptions().securityGroups( targetProperties.get( SECURITY_GROUP ));
			template.getOptions().userMetadata( "Application Name", parameters.getApplicationName());
			template.getOptions().userMetadata( "Root Instance Name", rootInstanceName );
			template.getOptions().userMetadata( "Domain", parameters.getDomain());
			template.getOptions().userMetadata( "Created by", "Roboconf" );

			// Specify our own key pair if the current provider supports it
			String keyPairName = targetProperties.get( KEY_PAIR );
			try {
				if( ! Utils.isEmptyOrWhitespaces( keyPairName )) {
					Method keyPairMethod = template.getOptions().getClass().getMethod( "keyPair", String.class );
					keyPairMethod.invoke( template.getOptions(), keyPairName );
				}

			} catch( Exception e ) {
				throw new TargetException( "Provider: " + providerId + " does not support specifying key pairs.", e );
			}

			String vmName = (parameters.getApplicationName() + "." + rootInstanceName).replaceAll( "\\.|\\s+", "-" );
			Set<? extends NodeMetadata> nodes = computeService.createNodesInGroup( vmName, 1, template );
			machineId = nodes.iterator().next().getId();

		} catch( RunNodesException e ) {
			throw new TargetException( "An error occurred while creating a new node with JClouds on provider " + providerId + ".", e );

		} finally {
			if( computeService != null )
				computeService.getContext().close();
		}

		return machineId;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.target.api.TargetHandler#configureMachine(
	 * net.roboconf.target.api.TargetHandlerParameters, java.lang.String)
	 */
	@Override
	public void configureMachine( TargetHandlerParameters parameters, String machineId )
	throws TargetException {

		// It may require to be configured from the DM => add the right marker
		parameters.getScopedInstance().data.put( Instance.READY_FOR_CFG_MARKER, "true" );
		this.logger.fine( "Configuring machine '" + machineId + "'..." );
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.target.api.TargetHandler
	 * #isMachineRunning(net.roboconf.target.api.TargetHandlerParameters, java.lang.String)
	 */
	@Override
	public boolean isMachineRunning( TargetHandlerParameters parameters, String machineId )
	throws TargetException {

		ComputeService computeService = jcloudContext( parameters.getTargetProperties());
		return computeService.getNodeMetadata( machineId ) != null;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.target.api.TargetHandler
	 * #terminateMachine(net.roboconf.target.api.TargetHandlerParameters, java.lang.String)
	 */
	@Override
	public void terminateMachine( TargetHandlerParameters parameters, String machineId ) throws TargetException {

		this.logger.fine( "Terminating machine " + machineId );
		ComputeService computeService = jcloudContext( parameters.getTargetProperties());
		computeService.destroyNode( machineId );
		computeService.getContext().close();
	}


	/* (non-Javadoc)
	 * @see net.roboconf.target.api.TargetHandler
	 * #retrievePublicIpAddress(net.roboconf.target.api.TargetHandlerParameters, java.lang.String)
	 */
	@Override
	public String retrievePublicIpAddress( TargetHandlerParameters parameters, String machineId )
	throws TargetException {

		ComputeService computeService = jcloudContext( parameters.getTargetProperties());
		NodeMetadata metadata = computeService.getNodeMetadata( machineId );

		String result = null;
		if( metadata != null
				&& metadata.getPublicAddresses() != null
				&& ! metadata.getPublicAddresses().isEmpty())
			result = metadata.getPublicAddresses().iterator().next();

		return result;
	}


	/**
	 * Creates a JCloud context.
	 * @param targetProperties the target properties
	 * @return a non-null object
	 * @throws TargetException if the target properties are invalid
	 */
	ComputeService jcloudContext( Map<String,String> targetProperties ) throws TargetException {

		validate( targetProperties );
		ComputeServiceContext context = ContextBuilder
				.newBuilder( targetProperties.get( PROVIDER_ID ))
				.endpoint( targetProperties.get( ENDPOINT ))
				.credentials( targetProperties.get( IDENTITY ), targetProperties.get( CREDENTIAL ))
				.buildView( ComputeServiceContext.class );

		return context.getComputeService();
	}


	/**
	 * Validates the target properties.
	 * @param targetProperties the properties
	 * @throws TargetException if an error occurred during the validation
	 */
	static void validate( Map<String,String> targetProperties ) throws TargetException {

		checkProperty( PROVIDER_ID, targetProperties );
		checkProperty( ENDPOINT, targetProperties );
		checkProperty( IMAGE_NAME, targetProperties );
		checkProperty( SECURITY_GROUP, targetProperties );
		checkProperty( IDENTITY, targetProperties );
		checkProperty( CREDENTIAL, targetProperties );
		checkProperty( HARDWARE_NAME, targetProperties );
	}


	private static void checkProperty( String propertyName, Map<String,String> targetProperties )
	throws TargetException {

		if( ! targetProperties.containsKey( propertyName ))
			throw new TargetException( "Property '" + propertyName + "' is missing." );

		if( Utils.isEmptyOrWhitespaces( targetProperties.get( propertyName )))
			throw new TargetException( "Property '" + propertyName + "' must have a value." );
	}
}
