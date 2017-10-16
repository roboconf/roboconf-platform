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

package net.roboconf.tooling.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.roboconf.core.Constants;
import net.roboconf.core.model.RuntimeModelIo;
import net.roboconf.core.model.RuntimeModelIo.ApplicationLoadResult;
import net.roboconf.core.model.beans.Component;
import net.roboconf.core.model.helpers.ComponentHelpers;
import net.roboconf.core.utils.ResourceUtils;
import net.roboconf.core.utils.Utils;

/**
 * @author Vincent Zurczak - Linagora
 */
public final class ProjectUtils {

	public static final String GRAPH_EP = "main.graph";
	public static final String INSTANCES_EP = "model.instances";

	private static final String TPL_NAME = "${NAME}";
	private static final String TPL_DESCRIPTION = "${DESCRIPTION}";
	private static final String TPL_VERSION = "${VERSION}";
	private static final String TPL_POM_GROUP = "${GROUPD_ID}";
	private static final String TPL_POM_ARTIFACT = "${ARTIFACT_ID}";
	private static final String TPL_POM_PLUGIN_VERSION = "${PLUGIN_VERSION}";

	private static final String[] ALL_DIRECTORIES = {
		Constants.PROJECT_DIR_DESC,
		Constants.PROJECT_DIR_GRAPH,
		Constants.PROJECT_DIR_INSTANCES,
		Constants.PROJECT_DIR_COMMANDS,
		Constants.PROJECT_DIR_RULES_AUTONOMIC,
		Constants.PROJECT_DIR_PROBES
	};

	private static final String[] RR_DIRECTORIES = {
		Constants.PROJECT_DIR_GRAPH,
		Constants.PROJECT_DIR_PROBES
	};


	/**
	 * Empty private constructor.
	 */
	private ProjectUtils() {
		// nothing
	}


	/**
	 * Creates a project for Roboconf.
	 * @param targetDirectory the directory into which the Roboconf files must be copied
	 * @param creationBean the creation properties
	 * @throws IOException if something went wrong
	 */
	public static void createProjectSkeleton( File targetDirectory, CreationBean creationBean ) throws IOException {

		if( creationBean.isMavenProject())
			createMavenProject( targetDirectory, creationBean );
		else
			createSimpleProject( targetDirectory, creationBean );
	}


	/**
	 * @return a non-null list of versions for the Roboconf Maven plug-in
	 */
	public static List<String> listMavenPluginVersions() {
		return Arrays.asList( "0.8", "0.9" );
	}


	/**
	 * Creates the recipes directories for a Roboconf components.
	 * @param applicationDirectory the application directory
	 * @return a non-null list of the created directories
	 * @throws IOException if something went wrong
	 */
	public static List<File> createRecipeDirectories( File applicationDirectory ) throws IOException {

		List<File> result = new ArrayList<> ();
		ApplicationLoadResult alr = RuntimeModelIo.loadApplicationFlexibly( applicationDirectory );
		if( alr.getApplicationTemplate() != null ) {
			for( Component c : ComponentHelpers.findAllComponents( alr.getApplicationTemplate())) {
				File directory = ResourceUtils.findInstanceResourcesDirectory( applicationDirectory, c );
				if( ! directory.exists())
					result.add( directory );

				Utils.createDirectory( directory );
			}
		}

		return result;
	}


	/**
	 * Creates a simple project for Roboconf.
	 * @param targetDirectory the directory into which the Roboconf files must be copied
	 * @param creationBean the creation properties
	 * @throws IOException if something went wrong
	 */
	private static void createSimpleProject( File targetDirectory, CreationBean creationBean )
	throws IOException {

		// Create the directory structure
		String[] directoriesToCreate = creationBean.isReusableRecipe() ? RR_DIRECTORIES : ALL_DIRECTORIES;
		for( String s : directoriesToCreate ) {
			File dir = new File( targetDirectory, s );
			Utils.createDirectory( dir );
		}

		// Create the descriptor
		InputStream in = ProjectUtils.class.getResourceAsStream( "/application-skeleton.props" );
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Utils.copyStreamSafely( in, out );
		String tpl = out.toString( "UTF-8" )
				.replace( TPL_NAME, creationBean.getProjectName())
				.replace( TPL_VERSION, creationBean.getProjectVersion())
				.replace( TPL_DESCRIPTION, creationBean.getProjectDescription());

		// Create the rest of the project
		completeProjectCreation( targetDirectory, tpl, creationBean );
	}


	/**
	 * Creates a Maven project for Roboconf.
	 * @param targetDirectory the directory into which the Roboconf files must be copied
	 * @param creationBean the creation properties
	 * @throws IOException if something went wrong
	 */
	private static void createMavenProject( File targetDirectory, CreationBean creationBean ) throws IOException {

		// Create the directory structure
		File rootDir = new File( targetDirectory, Constants.MAVEN_SRC_MAIN_MODEL );
		String[] directoriesToCreate = creationBean.isReusableRecipe() ? RR_DIRECTORIES : ALL_DIRECTORIES;
		for( String s : directoriesToCreate ) {
			File dir = new File( rootDir, s );
			Utils.createDirectory( dir );
		}

		// Create a POM?
		InputStream in;
		if( Utils.isEmptyOrWhitespaces( creationBean.getCustomPomLocation()))
			in = ProjectUtils.class.getResourceAsStream( "/pom-skeleton.xml" );
		else
			in = new FileInputStream( creationBean.getCustomPomLocation());

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Utils.copyStreamSafely( in, out );
		String tpl = out.toString( "UTF-8" )
				.replace( TPL_NAME, creationBean.getProjectName())
				.replace( TPL_POM_GROUP, creationBean.getGroupId())
				.replace( TPL_POM_PLUGIN_VERSION, creationBean.getPluginVersion())
				.replace( TPL_VERSION, creationBean.getProjectVersion())
				.replace( TPL_POM_ARTIFACT, creationBean.getArtifactId())
				.replace( TPL_DESCRIPTION, creationBean.getProjectDescription());

		File pomFile = new File( targetDirectory, "pom.xml" );
		Utils.copyStream( new ByteArrayInputStream( tpl.getBytes( StandardCharsets.UTF_8 )), pomFile );

		// Create the descriptor
		in = ProjectUtils.class.getResourceAsStream( "/application-skeleton.props" );
		out = new ByteArrayOutputStream();
		Utils.copyStreamSafely( in, out );
		tpl = out.toString( "UTF-8" )
				.replace( TPL_NAME, creationBean.getProjectName())
				.replace( TPL_DESCRIPTION, "${project.description}" );

		// If for some reason, the project version is already a Maven expression,
		// keep it untouched. Such a thing may cause troubles with a real POM,
		// as versions should not reference properties. But it may be used for tests anyway.
		if( ! creationBean.getProjectVersion().contains( "$" ))
			tpl = tpl.replace( TPL_VERSION, "${project.version}" );
		else
			tpl = tpl.replace( TPL_VERSION, creationBean.getProjectVersion());

		// Create the rest of the project
		completeProjectCreation( rootDir, tpl, creationBean );
	}


	/**
	 * Completes the creation of a Roboconf project.
	 * @param targetDirectory the directory into which the Roboconf files must be copied
	 * @param descriptorContent the descriptor's content
	 * @param creationBean the creation options
	 * @throws IOException if something went wrong
	 */
	private static void completeProjectCreation(
			File targetDirectory,
			String descriptorContent,
			CreationBean creationBean )
	throws IOException {

		// Create a sample graph file
		File f = new File( targetDirectory, Constants.PROJECT_DIR_GRAPH + "/" + GRAPH_EP );
		InputStream in = ProjectUtils.class.getResourceAsStream( "/graph-skeleton.graph" );
		Utils.copyStream( in, f );

		// Create other elements only if it is not a reusable recipe
		if( ! creationBean.isReusableRecipe()) {

			// Write the descriptor
			f = new File( targetDirectory, Constants.PROJECT_DIR_DESC + "/" + Constants.PROJECT_FILE_DESCRIPTOR );
			Utils.writeStringInto( descriptorContent, f );

			// Create a sample instances file
			f = new File( targetDirectory, Constants.PROJECT_DIR_INSTANCES + "/" + INSTANCES_EP );
			in = ProjectUtils.class.getResourceAsStream( "/instances-skeleton.instances" );
			Utils.copyStream( in, f );
		}
	}


	/**
	 * @author Vincent Zurczak - Linagora
	 */
	public static class CreationBean {
		private String projectName, projectDescription, projectVersion;
		private String artifactId, pluginVersion, groupId;
		private String customPomLocation;
		private boolean mavenProject = true;
		private boolean reusableRecipe = false;


		public String getProjectName() {
			return getNonNullString( this.projectName );
		}

		public CreationBean projectName( String projectName ) {
			this.projectName = projectName;
			return this;
		}

		public String getProjectDescription() {
			return getNonNullString( this.projectDescription );
		}

		public CreationBean projectDescription( String projectDescription ) {
			this.projectDescription = projectDescription;
			return this;
		}

		public boolean isMavenProject() {
			return this.mavenProject;
		}

		public CreationBean mavenProject( boolean mavenProject ) {
			this.mavenProject = mavenProject;
			return this;
		}

		public boolean isReusableRecipe() {
			return this.reusableRecipe;
		}

		public CreationBean reusableRecipe( boolean reusableRecipe ) {
			this.reusableRecipe = reusableRecipe;
			return this;
		}

		public String getProjectVersion() {
			return getNonNullString( this.projectVersion );
		}

		public CreationBean projectVersion( String projectVersion ) {
			this.projectVersion = projectVersion;
			return this;
		}

		public String getGroupId() {
			return this.groupId;
		}

		public CreationBean groupId( String groupId ) {
			this.groupId = groupId;
			return this;
		}

		public String getArtifactId() {
			return getNonNullString( this.artifactId );
		}

		public CreationBean artifactId( String artifactId ) {
			this.artifactId = artifactId;
			return this;
		}

		public String getPluginVersion() {
			return getNonNullString( this.pluginVersion );
		}

		public CreationBean pluginVersion( String pluginVersion ) {
			this.pluginVersion = pluginVersion;
			return this;
		}

		public String getCustomPomLocation() {
			return this.customPomLocation;
		}

		public CreationBean customPomLocation( String customPomLocation ) {
			this.customPomLocation = customPomLocation;
			return this;
		}

		static String getNonNullString( String s ) {
			return Utils.isEmptyOrWhitespaces( s ) ? "" : s.trim();
		}
	}
}
