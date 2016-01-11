/**
 * Copyright 2013-2016 Linagora, Université Joseph Fourier, Floralis
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

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import net.roboconf.core.Constants;
import net.roboconf.core.ErrorCode;
import net.roboconf.core.RoboconfError;
import net.roboconf.core.commands.CommandsParser;
import net.roboconf.core.dsl.ParsingModelIo;
import net.roboconf.core.dsl.converters.FromGraphDefinition;
import net.roboconf.core.dsl.converters.FromInstanceDefinition;
import net.roboconf.core.dsl.converters.FromInstances;
import net.roboconf.core.dsl.parsing.FileDefinition;
import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.core.model.beans.Graphs;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.helpers.RoboconfErrorHelpers;
import net.roboconf.core.utils.Utils;

/**
 * @author Vincent Zurczak - Linagora
 */
public final class RuntimeModelIo {

	/**
	 * Constructor.
	 */
	private RuntimeModelIo() {
		// nothing
	}


	/**
	 * Loads an application from a directory.
	 * <p>
	 * The directory structure must be the following one:
	 * </p>
	 * <ul>
	 * 		<li>descriptor</li>
	 * 		<li>graph</li>
	 * 		<li>instances (optional)</li>
	 * </ul>
	 *
	 * @param projectDirectory the project directory
	 * @return a load result (never null)
	 */
	public static ApplicationLoadResult loadApplication( File projectDirectory ) {

		ApplicationLoadResult result = new ApplicationLoadResult();
		ApplicationTemplate app = new ApplicationTemplate();
		result.applicationTemplate = app;

		ApplicationTemplateDescriptor appDescriptor = null;
		File descDirectory = new File( projectDirectory, Constants.PROJECT_DIR_DESC );


		// Read the application descriptor
		DESC: if( ! descDirectory.exists()) {
			RoboconfError error = new RoboconfError( ErrorCode.PROJ_NO_DESC_DIR );
			error.setDetails( "Directory path: " + projectDirectory.getAbsolutePath());
			result.loadErrors.add( error );

		} else {
			File descriptorFile = new File( descDirectory, Constants.PROJECT_FILE_DESCRIPTOR );
			if( ! descriptorFile.exists()) {
				result.loadErrors.add( new RoboconfError( ErrorCode.PROJ_NO_DESC_FILE ));
				break DESC;
			}

			try {
				appDescriptor = ApplicationTemplateDescriptor.load( descriptorFile );
				app.setName( appDescriptor.getName());
				app.setDescription( appDescriptor.getDescription());
				app.setQualifier( appDescriptor.getQualifier());
				app.setDslId( appDescriptor.getDslId());
				app.setExternalExportsPrefix( appDescriptor.getExternalExportsPrefix());

				for( Map.Entry<String,String> entry : appDescriptor.externalExports.entrySet())
					app.externalExports.put( entry.getKey(), app.getExternalExportsPrefix() + "." + entry.getValue());

				Collection<ModelError> errors = RuntimeModelValidator.validate( appDescriptor );
				result.loadErrors.addAll( errors );

			} catch( IOException e ) {
				RoboconfError error = new RoboconfError( ErrorCode.PROJ_READ_DESC_FILE );
				StringBuilder sb = new StringBuilder( "IO exception." );
				if( e.getMessage() != null ) {
					sb.append( " " );
					sb.append( e.getMessage());
				}

				error.setDetails( sb.toString());
				result.loadErrors.add( error );
			}
		}

		return loadApplication( projectDirectory, appDescriptor, result );
	}


	/**
	 * Loads an application from a directory.
	 * <p>
	 * This method allows to load an application which does not have a descriptor.
	 * If it has one, it will be read. Otherwise, a default one will be generated.
	 * This is convenient for reusable recipes.
	 * </p>
	 *
	 * @param projectDirectory the project directory
	 * @return a load result (never null)
	 */
	public static ApplicationLoadResult loadApplicationFlexibly( File projectDirectory ) {

		File descDirectory = new File( projectDirectory, Constants.PROJECT_DIR_DESC );
		ApplicationLoadResult result;
		if( descDirectory.exists()) {
			result = loadApplication( projectDirectory );

		} else {
			ApplicationTemplateDescriptor appDescriptor = new ApplicationTemplateDescriptor();
			appDescriptor.setName( Constants.GENERATED );
			appDescriptor.setDslId( Constants.GENERATED );
			appDescriptor.setQualifier( Constants.GENERATED );

			ApplicationLoadResult alr = new ApplicationLoadResult();
			alr.applicationTemplate = new ApplicationTemplate( Constants.GENERATED ).dslId( Constants.GENERATED ).qualifier( Constants.GENERATED );

			File graphDirectory = new File( projectDirectory, Constants.PROJECT_DIR_GRAPH );
			File[] graphFiles = graphDirectory.listFiles( new GraphFileFilter());
			if( graphFiles != null && graphFiles.length > 0 )
				appDescriptor.setGraphEntryPoint( graphFiles[ 0 ].getName());

			result = loadApplication( projectDirectory, appDescriptor, alr );
		}

		return result;
	}


	/**
	 * Loads an application from a directory.
	 * @param projectDirectory the project directory
	 * @param appDescriptor the application's descriptor
	 * @param result the result to populate
	 * @return a load result (never null)
	 */
	private static ApplicationLoadResult loadApplication(
			File projectDirectory,
			ApplicationTemplateDescriptor appDescriptor,
			ApplicationLoadResult result ) {

		ApplicationTemplate app = result.applicationTemplate;
		result.applicationTemplate.setDirectory( projectDirectory );


		// Load the graph
		File graphDirectory = new File( projectDirectory, Constants.PROJECT_DIR_GRAPH );
		GRAPH: if( ! graphDirectory.exists()) {
			RoboconfError error = new RoboconfError( ErrorCode.PROJ_NO_GRAPH_DIR );
			error.setDetails( "Directory path: " + projectDirectory.getAbsolutePath());
			result.loadErrors.add( error );

		} else if( appDescriptor != null
				&& ! Utils.isEmptyOrWhitespaces( appDescriptor.getGraphEntryPoint())) {

			File mainGraphFile = new File( graphDirectory, appDescriptor.getGraphEntryPoint());
			if( ! mainGraphFile.exists()) {
				RoboconfError error = new RoboconfError( ErrorCode.PROJ_MISSING_GRAPH_EP );
				error.setDetails( "Expected path: " + mainGraphFile.getAbsolutePath());
				result.loadErrors.add( error );
				break GRAPH;
			}

			FromGraphDefinition fromDef = new FromGraphDefinition( graphDirectory );
			Graphs graph = fromDef.buildGraphs( mainGraphFile );
			if( ! fromDef.getErrors().isEmpty()) {
				result.loadErrors.addAll( fromDef.getErrors());
				break GRAPH;
			}

			Collection<ModelError> errors = RuntimeModelValidator.validate( graph );
			result.loadErrors.addAll( errors );

			errors = RuntimeModelValidator.validate( graph, projectDirectory );
			result.loadErrors.addAll( errors );

			result.objectToSource.putAll( fromDef.getObjectToSource());
			app.setGraphs( graph );
		}


		// Load the instances
		File instDirectory = new File( projectDirectory, Constants.PROJECT_DIR_INSTANCES );
		INST: if( appDescriptor != null && instDirectory.exists()) {

			if( app.getGraphs() == null ) {
				result.loadErrors.add( new RoboconfError( ErrorCode.CO_GRAPH_COULD_NOT_BE_BUILT ));
				break INST;
			}

			if( Utils.isEmptyOrWhitespaces( appDescriptor.getInstanceEntryPoint()))
				break INST;

			File mainInstFile = new File( instDirectory, appDescriptor.getInstanceEntryPoint());
			InstancesLoadResult ilr = loadInstances( mainInstFile, instDirectory, app.getGraphs(), app.getName());

			result.objectToSource.putAll( ilr.getObjectToSource());
			result.loadErrors.addAll( ilr.getLoadErrors());
			app.getRootInstances().addAll( ilr.getRootInstances());
		}


		// Commands
		File commandsDirectory = new File( projectDirectory, Constants.PROJECT_DIR_COMMANDS );
		if( app.getGraphs() != null && commandsDirectory.exists()) {

			for( File f : Utils.listAllFiles( commandsDirectory )) {
				CommandsParser parser = new CommandsParser( app, f );
				result.loadErrors.addAll( parser.getParsingErrors());
			}
		}


		// Validate the entire application
		if( ! RoboconfErrorHelpers.containsCriticalErrors( result.loadErrors )) {
			Collection<ModelError> errors = RuntimeModelValidator.validate( app );
			result.loadErrors.addAll( errors );
		}

		return result;
	}


	/**
	 * A bean that stores both the application and loading errors.
	 * @author Vincent Zurczak - Linagora
	 */
	public static class ApplicationLoadResult {
		ApplicationTemplate applicationTemplate;
		final Collection<RoboconfError> loadErrors = new ArrayList<RoboconfError> ();
		final Map<Object,SourceReference> objectToSource = new HashMap<Object,SourceReference> ();

		/**
		 * @return the application (can be null)
		 */
		public ApplicationTemplate getApplicationTemplate() {
			return this.applicationTemplate;
		}

		/**
		 * @return the load errors (never null)
		 */
		public Collection<RoboconfError> getLoadErrors() {
			return this.loadErrors;
		}

		/**
		 * @return the objectToSource
		 */
		public Map<Object,SourceReference> getObjectToSource() {
			return this.objectToSource;
		}
	}


	/**
	 * A bean that stores both root instances and loading errors.
	 * @author Vincent Zurczak - Linagora
	 */
	public static class InstancesLoadResult {
		Collection<Instance> rootInstances = new ArrayList<Instance> ();
		final Collection<RoboconfError> loadErrors = new ArrayList<RoboconfError> ();
		final Map<Object,SourceReference> objectToSource = new HashMap<Object,SourceReference> ();

		/**
		 * @return the root instances (never null)
		 */
		public Collection<Instance> getRootInstances() {
			return this.rootInstances;
		}

		/**
		 * @return the load errors (never null)
		 */
		public Collection<RoboconfError> getLoadErrors() {
			return this.loadErrors;
		}

		/**
		 * @return the objectToSource
		 */
		public Map<Object,SourceReference> getObjectToSource() {
			return this.objectToSource;
		}
	}


	/**
	 * Loads instances from a file.
	 * @param instancesFile the file definition of the instances (can have imports)
	 * @param rootDirectory the root directory that contains instance definitions, used to resolve imports
	 * @param graph the graph to use to resolve instances
	 * @param applicationName the application name
	 * @return a non-null result
	 */
	public static InstancesLoadResult loadInstances( File instancesFile, File rootDirectory, Graphs graph, String applicationName ) {

		InstancesLoadResult result = new InstancesLoadResult();
		INST: {
			if( ! instancesFile.exists()) {
				RoboconfError error = new RoboconfError( ErrorCode.PROJ_MISSING_INSTANCE_EP );
				error.setDetails( "Expected path: " + instancesFile.getAbsolutePath());
				result.loadErrors.add( error );
				break INST;
			}

			FromInstanceDefinition fromDef = new FromInstanceDefinition( rootDirectory );
			Collection<Instance> instances = fromDef.buildInstances( graph, instancesFile );
			if( ! fromDef.getErrors().isEmpty()) {
				result.loadErrors.addAll( fromDef.getErrors());
				break INST;
			}

			Collection<ModelError> errors = RuntimeModelValidator.validate( instances );
			result.loadErrors.addAll( errors );
			result.objectToSource.putAll( fromDef.getObjectToSource());
			result.getRootInstances().addAll( instances );
		}

		for( Instance rootInstance : result.rootInstances )
			rootInstance.data.put( Instance.APPLICATION_NAME, applicationName );

		return result;
	}


	/**
	 * Writes all the instances into a file.
	 * @param targetFile the file to save
	 * @param rootInstances the root instances (not null)
	 * @throws IOException if something went wrong
	 */
	public static void writeInstances( File targetFile, Collection<Instance> rootInstances ) throws IOException {

		FileDefinition def = new FromInstances().buildFileDefinition( rootInstances, targetFile, false, true );
		ParsingModelIo.saveRelationsFile( def, false, "\n" );
	}


	/**
	 * @author Vincent Zurczak - Linagora
	 */
	static class GraphFileFilter implements FileFilter {
		@Override
		public boolean accept( File f ) {
			return f.isFile() && f.getName().toLowerCase().endsWith( ".graph" );
		}
	}
}
