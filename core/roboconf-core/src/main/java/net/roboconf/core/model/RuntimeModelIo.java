/**
 * Copyright 2013-2014 Linagora, Université Joseph Fourier, Floralis
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import net.roboconf.core.Constants;
import net.roboconf.core.ErrorCode;
import net.roboconf.core.RoboconfError;
import net.roboconf.core.dsl.ParsingModelIo;
import net.roboconf.core.dsl.converters.FromGraphDefinition;
import net.roboconf.core.dsl.converters.FromInstanceDefinition;
import net.roboconf.core.dsl.converters.FromInstances;
import net.roboconf.core.dsl.parsing.FileDefinition;
import net.roboconf.core.model.beans.Application;
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
		Application app = new Application();

		ApplicationDescriptor appDescriptor = null;
		File descDirectory = new File( projectDirectory, Constants.PROJECT_DIR_DESC );
		File instDirectory = new File( projectDirectory, Constants.PROJECT_DIR_INSTANCES );
		File graphDirectory = new File( projectDirectory, Constants.PROJECT_DIR_GRAPH );


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
				appDescriptor = ApplicationDescriptor.load( descriptorFile );
				app.setName( appDescriptor.getName());
				app.setDescription( appDescriptor.getDescription());
				app.setQualifier( appDescriptor.getQualifier());
				app.setNamespace( appDescriptor.getNamespace());
				app.setDslId( appDescriptor.getDslId());

				Collection<RoboconfError> errors = RuntimeModelValidator.validate( appDescriptor );
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


		// Load the graph
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

			Collection<RoboconfError> errors = RuntimeModelValidator.validate( graph );
			result.loadErrors.addAll( errors );

			errors = RuntimeModelValidator.validate( graph, projectDirectory );
			result.loadErrors.addAll( errors );

			app.setGraphs( graph );
		}


		// Load the instances
		INST: if( appDescriptor != null && instDirectory.exists()) {

			if( app.getGraphs() == null ) {
				result.loadErrors.add( new RoboconfError( ErrorCode.CO_GRAPH_COULD_NOT_BE_BUILT ));
				break INST;
			}

			if( Utils.isEmptyOrWhitespaces( appDescriptor.getInstanceEntryPoint()))
				break INST;

			File mainInstFile = new File( instDirectory, appDescriptor.getInstanceEntryPoint());
			InstancesLoadResult ilr = loadInstances( mainInstFile, instDirectory, app.getGraphs(), app.getName());

			result.loadErrors.addAll( ilr.getLoadErrors());
			app.getRootInstances().addAll( ilr.getRootInstances());
		}


		// Validate the entire application
		if( ! RoboconfErrorHelpers.containsCriticalErrors( result.loadErrors )) {
			Collection<RoboconfError> errors = RuntimeModelValidator.validate( app );
			result.loadErrors.addAll( errors );
		}


		// Complete the result
		result.application = app;
		return result;
	}


	/**
	 * A bean that stores both the application and loading errors.
	 */
	public static class ApplicationLoadResult {
		Application application;
		final Collection<RoboconfError> loadErrors = new ArrayList<RoboconfError> ();

		/**
		 * @return the application (can be null)
		 */
		public Application getApplication() {
			return this.application;
		}

		/**
		 * @return the load errors (never null)
		 */
		public Collection<RoboconfError> getLoadErrors() {
			return this.loadErrors;
		}
	}


	/**
	 * A bean that stores both root instances and loading errors.
	 */
	public static class InstancesLoadResult {
		Collection<Instance> rootInstances = new ArrayList<Instance> ();
		final Collection<RoboconfError> loadErrors = new ArrayList<RoboconfError> ();

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

			Collection<RoboconfError> errors = RuntimeModelValidator.validate( instances );
			result.loadErrors.addAll( errors );
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
}
