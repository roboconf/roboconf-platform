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

package net.roboconf.core.userdata;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

import javax.xml.bind.DatatypeConverter;

import net.roboconf.core.utils.Utils;

/**
 * A set of helpers to write and read data for agents.
 * @author Vincent Zurczak - Linagora
 */
public final class UserDataHelpers {

	public static final String SCOPED_INSTANCE_PATH = "scoped.instance.path";
	public static final String APPLICATION_NAME = "application.name";
	public static final String DOMAIN = "domain";

	/**
	 * A prefix for messaging properties that indicates a file content should be added in the user data.
	 * <p>
	 * Assuming a property "p" contains a reference to a file path ("/tmp/test.properties"),
	 * and assuming we want to make the file content available to the agent, then we would have
	 * the following messaging properties:
	 * </p>
	 * <ul>
	 * 	<li>p = /tmp/test.properties</li>
	 *  <li>{@value #ENCODE_FILE_CONTENT_PREFIX}p = ""</li>
	 *  <li></li>
	 * </ul>
	 * <p>
	 * Then, {@link #writeUserDataAsString(Map, String, String, String)} will populate
	 * the value. And {@link #readUserData(String)} will write the file content somewhere
	 * and update the properties accordingly.
	 * </p>
	 * <p>
	 * Notice the file content is encoded with base 64.
	 * </p>
	 */
	public static final String ENCODE_FILE_CONTENT_PREFIX = "@file@";


	/**
	 * Constructor.
	 */
	private UserDataHelpers() {
		// nothing
	}


	/**
	 * Writes user data as a string.
	 * <p>
	 * If file contents should be sent as well, use {@link #ENCODE_FILE_CONTENT_PREFIX}.
	 * </p>
	 *
	 * @param domain the domain (used among other things in the messaging)
	 * @param messagingConfiguration the messaging configuration
	 * @param applicationName the application name
	 * @param scopedInstancePath the scoped instance's path (the instance associated with the agent)
	 * @return a non-null string
	 * @throws IOException if something went wrong
	 */
	public static String writeUserDataAsString(
			Map<String,String> messagingConfiguration,
			String domain,
			String applicationName,
			String scopedInstancePath )
	throws IOException {

		Properties props = writeUserDataAsProperties( messagingConfiguration, domain, applicationName, scopedInstancePath );
		StringWriter writer = new StringWriter();
		props.store( writer, "" );

		return writer.toString();
	}


	/**
	 * Writes user data as properties.
	 * <p>
	 * If file contents should be sent as well, use {@link #ENCODE_FILE_CONTENT_PREFIX}.
	 * </p>
	 *
	 * @param domain the domain (used among other things in the messaging)
	 * @param applicationName the application name
	 * @param scopedInstancePath the scoped instance's path (the instance associated with the agent)
	 * @param messagingConfiguration a map containing the messaging configuration
	 * @return a non-null object
	 * @throws IOException if files had to be loaded and were not found
	 */
	public static Properties writeUserDataAsProperties(
			Map<String,String> messagingConfiguration,
			String domain,
			String applicationName,
			String scopedInstancePath )
	throws IOException {

		Properties result = new Properties();
		if( applicationName != null )
			result.setProperty( APPLICATION_NAME, applicationName );

		if( scopedInstancePath != null )
			result.setProperty( SCOPED_INSTANCE_PATH, scopedInstancePath );

		if( domain != null )
			result.setProperty( DOMAIN, domain );

		if( messagingConfiguration != null ) {
			for( Map.Entry<String,String> e : messagingConfiguration.entrySet()) {
				if( e.getValue() != null )
					result.setProperty( e.getKey(), e.getValue());
			}
		}

		interceptLoadingFiles( result );
		return result;
	}


	/**
	 * Reads user data.
	 * <p>
	 * This method handles the {@link #ENCODE_FILE_CONTENT_PREFIX} prefix.
	 * File contents are written on the disk and references are updated if necessary.
	 * </p>
	 *
	 * @param rawProperties the user data as a string
	 * @param outputDirectory a directory into which files should be written
	 * <p>
	 * If null, files sent with {@link #ENCODE_FILE_CONTENT_PREFIX} will not be written.
	 * </p>
	 *
	 * @return a non-null object
	 * @throws IOException if something went wrong
	 */
	public static Properties readUserData( String rawProperties, File outputDirectory ) throws IOException {

		Properties result = new Properties();
		StringReader reader = new StringReader( rawProperties );
		result.load( reader );

		interceptWritingFiles( result, outputDirectory );
		return result;
	}


	/**
	 * Intercepts the properties and loads file contents.
	 * @param props non-null properties
	 * @throws IOException if files had to be loaded and were not found
	 */
	static void interceptLoadingFiles( Properties props ) throws IOException {

		Logger logger = Logger.getLogger( UserDataHelpers.class.getName());
		Set<String> keys = props.stringPropertyNames();
		for( String key : keys ) {
			if( ! key.startsWith( ENCODE_FILE_CONTENT_PREFIX ))
				continue;

			String realKey = key.substring( ENCODE_FILE_CONTENT_PREFIX.length());
			String value = props.getProperty( realKey );
			if( value == null ) {
				logger.fine( "No file was specified for " + realKey + ". Skipping it..." );
				continue;
			}

			File f = new File( value );
			if( ! f.exists())
				throw new IOException( "File " + f + " was not found." );

			String fileContent = Utils.readFileContent( f );
			String encodedFileContent = encodeToBase64( fileContent );
			props.put( key, encodedFileContent );
		}
	}


	/**
	 * Intercepts the properties and writes file contents.
	 * @param props non-null properties
	 * @param outputDirectory a directory into which files should be written
	 * <p>
	 * If null, files sent with {@link #ENCODE_FILE_CONTENT_PREFIX} will not be written.
	 * </p>
	 *
	 * @throws IOException
	 */
	static void interceptWritingFiles( Properties props, File outputDirectory ) throws IOException {

		if( outputDirectory == null )
			return;

		Logger logger = Logger.getLogger( UserDataHelpers.class.getName());
		Set<String> keys = props.stringPropertyNames();
		for( String key : keys ) {
			if( ! key.startsWith( ENCODE_FILE_CONTENT_PREFIX ))
				continue;

			// Get the file content
			String encodedFileContent = props.getProperty( key );
			String fileContent = decodeFromBase64( encodedFileContent );

			// Write it to the disk
			String realKey = key.substring( ENCODE_FILE_CONTENT_PREFIX.length());
			String value = props.getProperty( realKey );
			if( value == null ) {
				logger.fine( "No file content was provided for " + realKey + ". Skipping it..." );
				continue;
			}

			Utils.createDirectory( outputDirectory );
			File output = new File( outputDirectory, new File( value ).getName());
			Utils.writeStringInto( fileContent, output );

			// Update the properties
			props.remove( key );
			props.setProperty( realKey, output.getAbsolutePath());
		}
	}


	/**
	 * Encodes a string with Base 64.
	 * @param s a non-null string
	 * @return a non-null string (encoded with base 64)
	 */
	static String encodeToBase64( String s ) {
		// FIXME: switch to Base64 once we are on Java 8+
		return DatatypeConverter.printBase64Binary( s.getBytes( StandardCharsets.UTF_8 ));
	}


	/**
	 * Decodes a string with Base 64.
	 * @param s a non-null string (encoded with base 64)
	 * @return a non-null string
	 */
	static String decodeFromBase64( String s ) {
		// FIXME: switch to Base64 once we are on Java 8+
		return new String( DatatypeConverter.parseBase64Binary( s ), StandardCharsets.UTF_8 );
	}
}
