/**
 * Copyright 2015-2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.target.docker.internal;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.lang.WordUtils;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse.ContainerState;
import com.github.dockerjava.api.model.Capability;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DefaultDockerClientConfig.Builder;
import com.github.dockerjava.core.DockerClientBuilder;

import net.roboconf.core.utils.Utils;
import net.roboconf.target.api.TargetException;

/**
 * @author Vincent Zurczak - Linagora
 */
public final class DockerUtils {

	/**
	 * Private empty constructor.
	 */
	private DockerUtils() {
		// nothing
	}


	/**
	 * Creates a Docker client from target properties.
	 * @param targetProperties a non-null map
	 * @return a Docker client
	 * @throws TargetException if something went wrong
	 */
	public static DockerClient createDockerClient( Map<String,String> targetProperties ) throws TargetException {

		// Validate what needs to be validated.
		Logger logger = Logger.getLogger( DockerHandler.class.getName());
		logger.fine( "Setting the target properties." );

		String edpt = targetProperties.get( DockerHandler.ENDPOINT );
		if( Utils.isEmptyOrWhitespaces( edpt ))
			edpt = DockerHandler.DEFAULT_ENDPOINT;

		// The configuration is straight-forward.
		Builder config =
				DefaultDockerClientConfig.createDefaultConfigBuilder()
				.withDockerHost( edpt )
				.withRegistryUsername( targetProperties.get( DockerHandler.USER ))
				.withRegistryPassword( targetProperties.get( DockerHandler.PASSWORD ))
				.withRegistryEmail( targetProperties.get( DockerHandler.EMAIL ))
				.withApiVersion( targetProperties.get( DockerHandler.VERSION ));

		// Build the client.
		DockerClientBuilder clientBuilder = DockerClientBuilder.getInstance( config.build());

		return clientBuilder.build();
	}


	/**
	 * Deletes a Docker image if it exists.
	 * @param imageId the image ID (not null)
	 * @param dockerClient a Docker client
	 */
	public static void deleteImageIfItExists( String imageId, DockerClient dockerClient ) {

		if( imageId != null ) {
			List<Image> images = dockerClient.listImagesCmd().exec();
			if( findImageById( imageId, images ) != null )
				dockerClient.removeImageCmd( imageId ).withForce( true ).exec();
		}
	}


	/**
	 * Finds an image by ID or by tag.
	 * @param name an image ID or a tag name (can be null)
	 * @param dockerClient a Docker client (not null)
	 * @return an image, or null if none matched
	 */
	public static Image findImageByIdOrByTag( String name, DockerClient dockerClient ) {

		Image image = null;
		if( ! Utils.isEmptyOrWhitespaces( name )) {
			Logger logger = Logger.getLogger( DockerUtils.class.getName());

			List<Image> images = dockerClient.listImagesCmd().exec();
			if(( image = DockerUtils.findImageById( name, images )) != null )
				logger.fine( "Found a Docker image with ID " + name );
			else if(( image = DockerUtils.findImageByTag( name, images )) != null )
				logger.fine( "Found a Docker image with tag " + name );
		}

		return image;
	}


	/**
	 * Finds an image by ID.
	 * @param imageId the image ID (not null)
	 * @param images a non-null list of images
	 * @return an image, or null if none was found
	 */
	public static Image findImageById( String imageId, List<Image> images ) {

		Image result = null;
		for( Image img : images ) {
			if( img.getId().equals(imageId)) {
				result = img;
				break;
			}
		}

		return result;
	}


	/**
	 * Finds an image by tag.
	 * @param imageTag the image tag (not null)
	 * @param images a non-null list of images
	 * @return an image, or null if none was found
	 */
	public static Image findImageByTag( String imageTag, List<Image> images ) {

		Image result = null;
		for( Image img : images ) {
			String[] tags = img.getRepoTags();
			if( tags == null )
				continue;

			for( String s : tags ) {
				if( s.contains( imageTag )) {
					result = img;
					break;
				}
			}
		}

		return result;
	}


	/**
	 * Finds a container by ID or by name.
	 * @param name the container ID or name (not null)
	 * @param dockerClient a Docker client
	 * @return a container, or null if none was found
	 */
	public static Container findContainerByIdOrByName( String name, DockerClient dockerClient ) {

		Container result = null;
		List<Container> containers = dockerClient.listContainersCmd().withShowAll( true ).exec();
		for( Container container : containers ) {
			List<String> names = Arrays.asList( container.getNames());

			// Docker containers are prefixed with '/'.
			// At least, those we created, since their parent is the Docker daemon.
			if( container.getId().equals( name )
					|| names.contains( "/" + name )) {
				result = container;
				break;
			}
		}

		return result;
	}


	/**
	 * Gets the state of a container.
	 * @param containerId the container ID
	 * @param dockerClient the Docker client
	 * @return a container state, or null if the container was not found
	 */
	public static ContainerState getContainerState( String containerId, DockerClient dockerClient ) {

		ContainerState result = null;
		try {
			InspectContainerResponse resp = dockerClient.inspectContainerCmd( containerId ).exec();
			if( resp != null )
				result = resp.getState();

		} catch( Exception e ) {
			// nothing
		}

		return result;
	}


	/**
	 * Finds the options and tries to configure them on the creation command.
	 * @param options the options (key = name, value = option value)
	 * @param cmd a non-null command to create a container
	 * @throws TargetException
	 */
	public static void configureOptions( Map<String,String> options, CreateContainerCmd cmd )
	throws TargetException {

		Logger logger = Logger.getLogger( DockerUtils.class.getName());

		// Basically, we had two choices:
		// 1. Map our properties to the Java REST API.
		// 2. By-pass it and send our custom JSon object.
		//
		// The second option is much more complicated.
		// So, we use Java reflection and some hacks to match Docker properties
		// with the setter methods available in the API. The API remains in charge
		// of generating the right JSon objects.
		Map<String,List<String>> hackedSetterNames = new HashMap<> ();

		// List known types
		List<Class<?>> types = new ArrayList<> ();
		types.add( String.class );
		types.add( String[].class );
		types.add( long.class );
		types.add( Long.class );
		types.add( int.class );
		types.add( Integer.class );
		types.add( boolean.class );
		types.add( Boolean.class );
		types.add( Capability[].class );

		// Deal with the options
		for( Map.Entry<String,String> entry : options.entrySet()) {
			String optionValue = entry.getValue();

			// Now, guess what option to set
			String methodName = entry.getKey().replace( "-", " " ).trim();
			methodName = WordUtils.capitalize( methodName );
			methodName = methodName.replace( " ", "" );
			methodName = "with" + methodName;

			Method _m = null;
			for( Method m : cmd.getClass().getMethods()) {

				boolean sameMethod = methodName.equalsIgnoreCase( m.getName());
				boolean methodWithAlias = hackedSetterNames.containsKey( methodName )
						&& hackedSetterNames.get( methodName ).contains( m.getName());

				if( sameMethod || methodWithAlias ) {

					// Only one parameter?
					if( m.getParameterTypes().length != 1 ) {
						logger.warning( "A method was found for " + entry.getKey() + " but it does not have the right number of parameters." );
						continue;
					}

					// The right type?
					if( ! types.contains( m.getParameterTypes()[ 0 ])) {

						// Since Docker-java 3.x, there are two methods to set cap-add and cap-drop.
						// One takes an array as parameter, the other takes a list.
						logger.warning(
								"A method was found for " + entry.getKey() + " but it does not have the right parameter type. "
								+ "Skipping it. You may want to add a feature request." );

						continue;
					}

					// That's probably the right one.
					_m = m;
					break;
				}
			}

			// Handle errors
			if( _m == null )
				throw new TargetException( "Nothing matched the " + entry.getKey() + " option in the REST API. Please, report it." );

			// Try to set the option in the REST client
			try {
				Object o = prepareParameter( optionValue, _m.getParameterTypes()[ 0 ]);
				_m.invoke( cmd, o );

			} catch( ReflectiveOperationException | IllegalArgumentException e ) {
				throw new TargetException( "Option " + entry.getKey() + " could not be set." );
			}
		}
	}


	/**
	 * Prepares the parameter to pass it to the REST API.
	 * @param rawValue the raw value, as a string
	 * @param clazz the class associated with the input parameter
	 * @return the object, converted to the right class
	 * @throws TargetException
	 */
	public static Object prepareParameter( String rawValue, Class<?> clazz ) throws TargetException {

		// Simple types
		Object result;
		if( clazz == int.class || clazz == Integer.class )
			result = Integer.parseInt( rawValue );
		else if( clazz == long.class || clazz == Long.class )
			result = Long.parseLong( rawValue );
		else if( clazz == boolean.class || clazz == Boolean.class )
			result = Boolean.parseBoolean( rawValue );

		// Arrays of string
		else if( clazz == String[].class ) {
			List<String> parts = Utils.splitNicely( rawValue, "," );
			result = parts.toArray( new String[ parts.size()]);
		}

		// Capabilities
		else if( clazz == Capability[].class ) {
			List<Capability> caps = new ArrayList<> ();
			for( String s : Utils.splitNicely( rawValue, "," )) {
				try {
					caps.add( Capability.valueOf( s ));
				} catch( Exception e ) {
					throw new TargetException( "Unknown capability: " + s );
				}
			}

			result = caps.toArray( new Capability[ caps.size()]);
		}

		// Default: keep the string
		else
			result = rawValue;

		return result;
	}


	/**
	 * Handles Boolean values.
	 * <p>
	 * Docker-java 3.x annotates state methods with "@CheckNotNull".
	 * So, we must verify the state attributes are not null (why Boolean instead of boolean?!).
	 * </p>
	 *
	 * @param bool a Boolean value
	 * @return the boolean value, or <code>false</code> if it was null
	 */
	public static boolean extractBoolean( Boolean bool ) {
		return bool != null ? bool.booleanValue() : false;
	}


	/**
	 * A constant linked to {@link #findDefaultImageVersion(String)}.
	 */
	public static final String LATEST = "latest";


	/**
	 * Finds the version of the default Docker image.
	 * @param rawVersion the raw version (e.g. ManifestUtils.findBundleVersion())
	 * @return {@value #LATEST} if the raw version is null or a snapshot, a specific version otherwise
	 */
	public static String findDefaultImageVersion( String rawVersion ) {

		String rbcfVersion = rawVersion;
		if( rbcfVersion == null
				|| rbcfVersion.toLowerCase().endsWith( "snapshot" ))
			rbcfVersion = LATEST;

		return rbcfVersion;
	}


	/**
	 * Builds a container name.
	 * @param scopedInstancePath a scoped instance path
	 * @param applicationName an application name
	 * @return a non-null string
	 */
	public static String buildContainerNameFrom( String scopedInstancePath, String applicationName ) {

		String containerName = scopedInstancePath + "_from_" + applicationName;
		containerName = containerName.replaceFirst( "^/", "" ).replace( "/", "-" ).replaceAll( "\\s+", "_" );

		// Prevent container names from being too long (see #480)
		if( containerName.length() > 61 )
			containerName = containerName.substring( 0, 61 );

		return containerName;
	}
}
