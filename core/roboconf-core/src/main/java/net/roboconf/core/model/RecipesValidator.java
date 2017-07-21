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

package net.roboconf.core.model;

import static net.roboconf.core.errors.ErrorDetails.component;
import static net.roboconf.core.errors.ErrorDetails.expected;
import static net.roboconf.core.errors.ErrorDetails.file;
import static net.roboconf.core.errors.ErrorDetails.name;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.roboconf.core.Constants;
import net.roboconf.core.errors.ErrorCode;
import net.roboconf.core.errors.ErrorDetails;
import net.roboconf.core.model.beans.Component;
import net.roboconf.core.model.beans.ImportedVariable;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.helpers.ComponentHelpers;
import net.roboconf.core.model.helpers.VariableHelpers;
import net.roboconf.core.utils.ProgramUtils;
import net.roboconf.core.utils.ResourceUtils;
import net.roboconf.core.utils.Utils;

/**
 * Modularity-breaking validator for recipes.
 * <p>
 * A class that centralizes validation for recipes. Recipes handlers are usually
 * plugged through extensions. It would have been logical to plug extra-validation in the same
 * way. However, Roboconf's core module aims at being a stand-alone library that can be used at
 * runtime but also in external tools.
 * </p>
 * <p>
 * This is why we allow this class to break modularity.<br>
 * And once again, it aims at bringing useful feedback to users.
 * </p>
 *
 * @author Vincent Zurczak - Linagora
 * @author Amadou Diarra -  UGA
 */
public final class RecipesValidator {

	public static final String SCRIPTS_DIR_NAME = "scripts";


	/**
	 * Private empty constructor.
	 */
	private RecipesValidator() {
		// nothing
	}


	/**
	 * Validates the recipes of a component.
	 * @param applicationFilesDirectory the application's directory
	 * @param component the component
	 * @return a non-null list of errors
	 */
	public static List<ModelError> validateComponentRecipes( File applicationFilesDirectory, Component component ) {

		List<ModelError> result;
		if( "puppet".equalsIgnoreCase( component.getInstallerName()))
			result = validatePuppetComponent( applicationFilesDirectory, component );
		else if( "script".equalsIgnoreCase( component.getInstallerName()))
			result = validateScriptComponent( applicationFilesDirectory, component );
		else
			result = Collections.emptyList();

		return result;
	}


	/**
	 * Validates a component associated with the Puppet installer.
	 * @param applicationFilesDirectory the application's directory
	 * @param component the component
	 * @return a non-null list of errors
	 */
	private static List<ModelError> validateScriptComponent( File applicationFilesDirectory, Component component ) {
		List<ModelError> result = new ArrayList<> ();

		// There must be a "scripts" directory
		File directory = ResourceUtils.findInstanceResourcesDirectory( applicationFilesDirectory, component );
		List<File> subDirs = Utils.listAllFiles( directory );

		if( !subDirs.isEmpty()) {
			File scriptsDir = new File( directory, SCRIPTS_DIR_NAME );
			if( ! scriptsDir.exists())
				result.add( new ModelError( ErrorCode.REC_SCRIPT_NO_SCRIPTS_DIR, component, component( component )));
		}

		return result;
	}


	/**
	 * Validates a component associated with the Puppet installer.
	 * @param applicationFilesDirectory the application's directory
	 * @param component the component
	 * @return a non-null list of errors
	 */
	private static List<ModelError> validatePuppetComponent( File applicationFilesDirectory, Component component ) {
		List<ModelError> result = new ArrayList<> ();

		// Check imports
		for( ImportedVariable var : ComponentHelpers.findAllImportedVariables( component ).values()) {
			if( var.getName().endsWith( "." + Constants.WILDCARD )) {
				result.add( new ModelError( ErrorCode.REC_PUPPET_DISLIKES_WILDCARD_IMPORTS, component, component( component )));
				break;
			}
		}

		// There must be a Puppet module that starts with "roboconf_"
		File directory = ResourceUtils.findInstanceResourcesDirectory( applicationFilesDirectory, component );
		File[] children = directory.listFiles();
		children = children == null ? new File[ 0 ] : children;
		List<File> modules = new ArrayList<> ();
		for( File f : children ) {
			if( f.isDirectory() && f.getName().toLowerCase().startsWith( "roboconf_" ))
				modules.add( f );
		}

		if( modules.isEmpty())
			result.add( new ModelError( ErrorCode.REC_PUPPET_HAS_NO_RBCF_MODULE, component, component( component )));
		else if( modules.size() > 1 )
			result.add( new ModelError( ErrorCode.REC_PUPPET_HAS_TOO_MANY_RBCF_MODULES, component, component( component )));

		// Analyze the module parameters
		if( modules.size() == 1 ) {
			File pp1 = new File( modules.get( 0 ), "manifests/update.pp" );
			File pp2 = new File( modules.get( 0 ), "manifests/init.pp" );
			File withUpdateParams = pp1.exists() ? pp1 : pp2;

			// Validate the files
			children = new File( modules.get( 0 ), "manifests" ).listFiles();
			children = children == null ? new File[ 0 ] : children;
			for( File f : children ) {
				try {
					if( f.isFile() && f.getName().toLowerCase().endsWith( ".pp" ))
						checkPuppetFile( f, f.equals( withUpdateParams ), component, result );

				} catch( IOException e ) {
					Logger logger = Logger.getLogger( RecipesValidator.class.getName());
					logger.warning( "The content of the Puppet file '" + f + "' could not be read." );
					Utils.logException( logger, e );
				}
			}
		}

		return result;
	}


	/**
	 * Reads a Puppet script and validates it.
	 * @param pp the Puppet script
	 * @param withUpdateParams true if update parameters should be present
	 * @param component the component
	 * @param errors a non-null list of errors
	 * @throws IOException if the file content could be read
	 */
	private static void checkPuppetFile( File pp, boolean withUpdateParams, Component component, Collection<ModelError> errors )
	throws IOException {

		// Try to use the Puppet validator
		String[] cmd = { "puppet", "parser", "validate", pp.getAbsolutePath()};
		Logger logger = Logger.getLogger( RecipesValidator.class.getName());
		try {
			int execCode = ProgramUtils.executeCommand( logger, cmd, null, null, null, null );
			if( execCode != 0 )
				errors.add( new ModelError( ErrorCode.REC_PUPPET_SYNTAX_ERROR, component, component( component ), file( pp )));

		} catch( Exception e ) {
			logger.info( "Puppet parser is not available on the machine." );
		}

		// We do not validate with puppet-lint.
		// Indeed, this tool is mostly about coding style and conventions.

		// Extract the script parameters
		Pattern pattern = Pattern.compile( "class [^(]+\\(([^)]+)\\)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL );
		String content = Utils.readFileContent( pp );
		Matcher m = pattern.matcher( content );

		Set<String> params = new HashSet<> ();
		if( ! m.find())
			return;

		for( String s : m.group( 1 ).split( "," )) {
			Entry<String,String> entry = VariableHelpers.parseExportedVariable( s.trim());
			params.add( entry.getKey());
		}

		// Check the update parameters
		if( withUpdateParams ) {
			if( ! params.remove( "$importDiff" ))
				errors.add( new ModelError( ErrorCode.REC_PUPPET_MISSING_PARAM_IMPORT_DIFF, component, component( component ), file( pp )));
		}

		// Prevent errors with start.pp, etc
		params.remove( "$importDiff" );

		// Check the other ones
		if( ! params.remove( "$runningState" ))
			errors.add( new ModelError( ErrorCode.REC_PUPPET_MISSING_PARAM_RUNNING_STATE, component, component( component ), file( pp )));

		// Imports imply some variables are expected
		Instance fake = new Instance( "fake" ).component( component );
		for( String facetOrComponentName : VariableHelpers.findPrefixesForImportedVariables( fake )) {
			if( ! params.remove( "$" + facetOrComponentName.toLowerCase())) {
				ErrorDetails[] details = new ErrorDetails[] {
						name( component.getName()),
						file( pp ),
						expected( facetOrComponentName.toLowerCase())
				};

				errors.add( new ModelError( ErrorCode.REC_PUPPET_MISSING_PARAM_FROM_IMPORT, component, details ));
			}
		}
	}
}
