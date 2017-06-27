/**
 * Copyright 2016-2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.core.model;

import static net.roboconf.core.errors.ErrorDetails.directory;
import static net.roboconf.core.errors.ErrorDetails.name;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

import net.roboconf.core.Constants;
import net.roboconf.core.errors.ErrorCode;
import net.roboconf.core.errors.ErrorDetails;
import net.roboconf.core.model.beans.Component;
import net.roboconf.core.utils.ResourceUtils;
import net.roboconf.core.utils.Utils;

/**
 * @author Vincent Zurczak - Linagora
 */
public class TargetValidator {

	private final Logger logger = Logger.getLogger( getClass().getName());
	private boolean failed = false;
	private String fileName;
	private Object modelObject;

	private final List<ModelError> errors = new ArrayList<> ();
	private Properties props;



	/**
	 * Constructor.
	 * @param propertiesFileContent
	 */
	public TargetValidator( String propertiesFileContent ) {

		this.props = new Properties();
		try {
			this.props.load( new ByteArrayInputStream( propertiesFileContent.getBytes( StandardCharsets.UTF_8 )));

		} catch( Exception e ) {
			this.failed = true;
			Utils.logException( this.logger, e );
		}
	}


	/**
	 * Constructor.
	 * @param propertiesFile
	 */
	public TargetValidator( File propertiesFile ) {

		try {
			this.props = Utils.readPropertiesFile( propertiesFile );
			this.fileName = propertiesFile.getName();

		} catch( Exception e ) {
			this.failed = true;
			Utils.logException( this.logger, e );
		}
	}


	/**
	 * Constructor.
	 * @param propertiesFile
	 * @param modelObject
	 */
	public TargetValidator( File propertiesFile, Object modelObject ) {
		this( propertiesFile );
		this.modelObject = modelObject;
	}


	/**
	 * Validates target properties.
	 */
	public void validate() {

		ErrorDetails details = null;
		if( this.fileName != null )
			details = ErrorDetails.file( this.fileName );

		if( this.failed ) {
			this.errors.add( new ModelError( ErrorCode.REC_TARGET_INVALID_FILE_OR_CONTENT, this.modelObject, details ));

		} else {
			String id = this.props.getProperty( Constants.TARGET_PROPERTY_ID );
			if( Utils.isEmptyOrWhitespaces( id ))
				this.errors.add( new ModelError( ErrorCode.REC_TARGET_NO_ID, this.modelObject, details ));

			String handler = this.props.getProperty( Constants.TARGET_PROPERTY_HANDLER );
			if( Utils.isEmptyOrWhitespaces( handler ))
				this.errors.add( new ModelError( ErrorCode.REC_TARGET_NO_HANDLER, this.modelObject, details ));

			String name = this.props.getProperty( Constants.TARGET_PROPERTY_NAME );
			if( Utils.isEmptyOrWhitespaces( name ))
				this.errors.add( new ModelError( ErrorCode.REC_TARGET_NO_NAME, this.modelObject, details ));
		}
	}


	/**
	 * @return the errors
	 */
	public List<ModelError> getErrors() {
		return this.errors;
	}


	/**
	 * @return the properties
	 */
	public Properties getProperties() {
		return this.props;
	}


	/**
	 * Parses a directory with one or several target properties files.
	 * @param directory an existing directory
	 * @return a non-null list of errors
	 */
	public static List<ModelError> parseDirectory( File directory ) {
		return parseDirectory( directory, null );
	}


	/**
	 * Parses a directory with one or several properties files.
	 * @param directory an existing directory
	 * @return a non-null list of errors
	 */
	public static List<ModelError> parseDirectory( File directory, Component c ) {

		// Validate all the properties
		List<ModelError> result = new ArrayList<> ();
		Set<String> targetIds = new HashSet<> ();
		for( File f : Utils.listDirectFiles( directory, Constants.FILE_EXT_PROPERTIES )) {

			TargetValidator tv = new TargetValidator( f, c );
			tv.validate();
			result.addAll( tv.getErrors());

			String id = tv.getProperties().getProperty( Constants.TARGET_PROPERTY_ID );
			if( targetIds.contains( id ))
				result.add( new ModelError( ErrorCode.REC_TARGET_CONFLICTING_ID, tv.modelObject, name( id )));

			targetIds.add( id );
		}

		// There should be properties files
		if( targetIds.isEmpty())
			result.add( new ModelError( ErrorCode.REC_TARGET_NO_PROPERTIES, null, directory( directory )));

		return result;
	}


	/**
	 * Parses the target properties for a given component.
	 * @param projectDirectory the project's directory
	 * @param c a component
	 * @return a non-null list of errors
	 */
	public static List<ModelError> parseTargetProperties( File projectDirectory, Component c ) {

		List<ModelError> errors;
		File dir = ResourceUtils.findInstanceResourcesDirectory( projectDirectory, c );
		if( dir.isDirectory()
				&& ! Utils.listAllFiles( dir, Constants.FILE_EXT_PROPERTIES ).isEmpty())
			errors = parseDirectory( dir, c );
		else
			errors = new ArrayList<>( 0 );

		return errors;
	}
}
