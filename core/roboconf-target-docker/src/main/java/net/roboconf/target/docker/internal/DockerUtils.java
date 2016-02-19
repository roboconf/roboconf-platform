/**
 * Copyright 2015-2016 Linagora, Université Joseph Fourier, Floralis
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
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig.DockerClientConfigBuilder;
import com.github.dockerjava.jaxrs.DockerCmdExecFactoryImpl;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

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
	 * Verifies the Docker client configuration.
	 * @param targetProperties the target properties
	 * @throws TargetException if the configuration is invalid
	 */
	public static void verifyDockerClient( Map<String,String> targetProperties )  throws TargetException {

		String imageId = targetProperties.get( DockerHandler.IMAGE_ID );
		String generate = targetProperties.get( DockerHandler.GENERATE_IMAGE );
		if( imageId == null && ! Boolean.parseBoolean( generate ))
			throw new TargetException( "The " + DockerHandler.IMAGE_ID + " parameter was not specified, or enable image generation." );
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
		verifyDockerClient( targetProperties );

		String edpt = targetProperties.get( DockerHandler.ENDPOINT );
		if( Utils.isEmptyOrWhitespaces( edpt ))
			edpt = "http://localhost:4243";

		// The configuration is straight-forward.
		DockerClientConfigBuilder config =
				DockerClientConfig.createDefaultConfigBuilder()
				.withUri( edpt )
				.withUsername( targetProperties.get( DockerHandler.USER ))
				.withPassword( targetProperties.get( DockerHandler.PASSWORD ))
				.withEmail( targetProperties.get( DockerHandler.EMAIL ))
				.withVersion( targetProperties.get( DockerHandler.VERSION ));

		// We must force the factory because otherwise, its finding relies on services loaders.
		// And this Java mechanism does not work in OSGi.
		DockerClientBuilder clientBuilder = DockerClientBuilder
				.getInstance( config.build())
				.withDockerCmdExecFactory( new DockerCmdExecFactoryImpl());

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
			for( String s : img.getRepoTags()) {
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

		// Basically, we had two choices:
		// 1. Map our properties to the Java REST API.
		// 2. By-pass it and send our custom JSon object.
		//
		// The second option is much more complicated.
		// So, we use Java reflection and some hacks to match Docker properties
		// with the setter methods available in the API. The API remains in charge
		// of generating the right JSon objects.
		Map<String,String> hackedSetterNames = new HashMap<> ();
		hackedSetterNames.put( "withMemory", "withMemoryLimit" );

		// Deal with the options
		for( Map.Entry<String,String> entry : options.entrySet()) {
			String optionValue = entry.getValue();

			// Now, guess what option to set
			String methodName = entry.getKey().replace( "-", " " ).trim();
			methodName = WordUtils.capitalize( methodName );
			methodName = methodName.replace( " ", "" );
			methodName = "with" + methodName;

			String alternativeName = hackedSetterNames.get( methodName );
			if( alternativeName != null )
				methodName = alternativeName;

			Method _m = null;
			for( Method m : cmd.getClass().getMethods()) {
				if( methodName.equalsIgnoreCase( m.getName())) {
					_m = m;
					break;
				}
			}

			// Handle errors
			List<Class<?>> types = new ArrayList<> ();
			types.add( String.class );
			types.add( String[].class );
			types.add( long.class );
			types.add( int.class );
			types.add( boolean.class );
			types.add( Capability[].class );

			if( _m == null )
				throw new TargetException( "Nothing matched the " + entry.getKey() + " option in the REST API. Please, report it." );
			else if( _m.getParameterTypes().length != 1 )
				throw new TargetException( "No method matched the " + entry.getKey() + " option in the REST API. Please, report it." );
			else if( ! types.contains( _m.getParameterTypes()[ 0 ]))
				throw new TargetException( "The " + entry.getKey() + " option is not supported by Roboconf. Please, add a feature request." );

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
		if( clazz == int.class )
			result = Integer.parseInt( rawValue );
		else if( clazz == long.class )
			result = Long.parseLong( rawValue );
		else if( clazz == boolean.class )
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
	 * Builds the command to pass to a new Docker container.
	 * @param cmd the value of the docker.run.command property
	 * @param messagingConfiguration the messaging configuration (not null)
	 * @param applicationName the application's name
	 * @param scopedInstancePath the scoped instance's path
	 * @return a non-null list of arguments
	 */
	public static List<String> buildRunCommand(
			String cmd,
			Map<String,String> messagingConfiguration,
			String applicationName,
			String scopedInstancePath ) {

		// We get the custom command line (docker.run.exec property) to run and:
		// - If nothing/invalid is specified (args == null), we use the standard agent start command
		// - If an empty command is explicitly specified (args.isEmpty()), there must be a RUN line in the Dockerfile.
		// - Else we use the provided command line. We may need to inject agent & messaging configuration.
		List<String> args = parseRunExecLine( cmd );
		if( args == null ) {

			// No docker.run.exec property (or invalid), fall back to the default command line.
			// Build the command line, passing the agent & messaging configuration.
			// Command line is:
			// - Agent's start.sh script
			// - messaging provider-specific configuration file,
			// - agent.application-name=<<name of the application>>
			// - agent.scoped-instance-path=<<path of the scoped instance>>
			// - agent.messaging-type=<<type of messaging>>
			// - each of the messaging configuration properties, prefixed by "msg."
			args = new ArrayList<> ();
			args.add("/usr/local/roboconf-agent/start.sh");
			args.add( "etc/net.roboconf.messaging." + DockerHandler.MARKER_MESSAGING_TYPE + ".cfg");
			args.add( "agent.application-name=" + DockerHandler.MARKER_APPLICATION_NAME );
			args.add( "agent.scoped-instance-path=" + DockerHandler.MARKER_INSTANCE_PATH );
			args.add( "agent.messaging-type=" + DockerHandler.MARKER_MESSAGING_TYPE );
			args.add( DockerHandler.MARKER_MESSAGING_CONFIGURATION );
		}

		// Now proceed to argument substitution, using the special markers.
		for( int i=0; i<args.size(); i++ ) {

			// The current argument, that may be substituted.
			String arg = args.get( i );

			// The string to substitute to the marker, or null if nothing to substitute.
			final String s;

			// The index (in arg) and length of the marker to replace.
			final int j, l;

			if( arg.contains( DockerHandler.MARKER_MESSAGING_TYPE )) {
				j = arg.indexOf(DockerHandler.MARKER_MESSAGING_TYPE);
				l = DockerHandler.MARKER_MESSAGING_TYPE.length();
				s = messagingConfiguration.containsKey( DockerHandler.MESSAGING_TYPE )
						? messagingConfiguration.get( DockerHandler.MESSAGING_TYPE ) : "";

			} else if( arg.contains( DockerHandler.MARKER_APPLICATION_NAME )) {
				j = arg.indexOf(DockerHandler.MARKER_APPLICATION_NAME);
				l = DockerHandler.MARKER_APPLICATION_NAME.length();
				s = applicationName;

			} else if( arg.contains( DockerHandler.MARKER_INSTANCE_PATH )) {
				j = arg.indexOf(DockerHandler.MARKER_INSTANCE_PATH);
				l = DockerHandler.MARKER_INSTANCE_PATH.length();
				s = scopedInstancePath;

			} else {
				if( arg.equals(DockerHandler.MARKER_MESSAGING_CONFIGURATION )) {

					// A bit more special: remove the whole argument and appends all
					// the messaging configuration, prefixed by "msg.".
					args.remove( i );
					for( Map.Entry<String,String> e : messagingConfiguration.entrySet()) {
						if( DockerHandler.MESSAGING_TYPE.equals( e.getKey()))
							continue;

						args.add(i, "msg." + e.getKey() + '=' + e.getValue());
						i++;
					}

					// We've gone one position to far...
					i--;
				}

				// No in-string substitution.
				j = -1;
				l = 0;
				s = null;
			}

			// Proceed to in-string substitution.
			if( s != null ) {
				arg = arg.substring(0, j) + s + arg.substring(j + l, arg.length());
				args.set(i, arg);
			}
		}

		return args;
	}


	/**
	 * Parses the given {@code docker.run.exec} property value.
	 * @param runExecLine the {@code docker.run.exec} property value.
	 * @return the {@code docker.run.exec} command + arguments array.
	 */
	public static List<String> parseRunExecLine( String runExecLine ) {

		List<String> result = null;
		if( ! Utils.isEmptyOrWhitespaces( runExecLine )) {
			try {
				Gson gson = new Gson();
				String[] array = gson.fromJson(runExecLine, String[].class);

				// The returned collection must support the remove operation!
				// Array.asList() returns an unmodifiable collection.
				result = new ArrayList<>( Arrays.asList( array ));

			} catch( JsonSyntaxException e ) {
				Logger logger = Logger.getLogger( DockerUtils.class.getName());
				logger.warning("Cannot parse property " + DockerHandler.RUN_EXEC + ": " + runExecLine);
				Utils.logException( logger, e );
			}
		}

		return result;
	}
}
