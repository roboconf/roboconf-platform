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

package net.roboconf.maven;

import java.io.File;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import net.roboconf.core.errors.RoboconfErrorHelpers;
import net.roboconf.core.model.ModelError;
import net.roboconf.core.model.TargetValidator;

/**
 * The mojo in charge of checking the target properties.
 * @author Vincent Zurczak - Linagora
 */
@Mojo( name="validate-target", defaultPhase = LifecyclePhase.COMPILE )
public class ValidateTargetMojo extends AbstractMojo {

	@Parameter( defaultValue = "${project}", readonly = true )
	private MavenProject project;


	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {

		// Find the target directory
		File outputDirectory = new File( this.project.getBuild().getOutputDirectory());
		if( ! outputDirectory.isDirectory())
			throw new MojoExecutionException( "The target directory could not be found. " + outputDirectory );

		// Load and validate the target properties
		List<ModelError> errors = TargetValidator.parseDirectory( outputDirectory );
		MavenPluginUtils.formatErrors( errors, getLog());

		// Fail the build?
		if( RoboconfErrorHelpers.containsCriticalErrors( errors ))
			throw new MojoFailureException( "The project contains one or several errors." );
	}
}
