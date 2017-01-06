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
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import net.roboconf.core.model.RuntimeModelIo;
import net.roboconf.core.model.RuntimeModelIo.ApplicationLoadResult;
import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.doc.generator.DocConstants;
import net.roboconf.doc.generator.RenderingManager;

/**
 * The <strong>documentation</strong> mojo.
 * @author Vincent Zurczak - Linagora
 */
@Mojo( name="documentation" )
@Execute( phase = LifecyclePhase.PREPARE_PACKAGE )
public class GenerateDocumentationMojo extends AbstractMojo {

	@Parameter( defaultValue = "${project}", readonly = true )
	private MavenProject project;

	@Parameter
	private List<String> renderers;

	@Parameter
	private List<String> locales;

	@Parameter
	private Map<String,String> options;



	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {

		// Reload the application (it was already validated).
		// - Sharing complex objects amongst mojos appears to be quite complicated.
		File appDirectory = new File( this.project.getBasedir(), MavenPluginConstants.TARGET_MODEL_DIRECTORY );
		ApplicationLoadResult alr = RuntimeModelIo.loadApplicationFlexibly( appDirectory );

		Map<String,String> annotations = alr.getTypeAnnotations();
		ApplicationTemplate app = alr.getApplicationTemplate();
		if( app == null )
			throw new MojoExecutionException( "The application object could not be loaded." );

		// Prepare the output directory
		File docDirectory = new File( this.project.getBasedir(), MavenPluginConstants.TARGET_DOC_DIRECTORY );

		// Fix the options (add the right prefix if necessary)
		if( this.options == null )
			this.options = new HashMap<>( 0 );

		Map<String,String> fixedOptions = new HashMap<>( this.options.size());
		for( Map.Entry<String,String> entry : this.options.entrySet()) {
			String key = entry.getKey().toLowerCase();
			if( ! key.startsWith( DocConstants.OPTION_PREFIX ))
				key = DocConstants.OPTION_PREFIX + key;

			fixedOptions.put( key, entry.getValue());
		}

		// Start generating the documentation
		try {
			if( this.locales == null || this.locales.isEmpty()) {
				new RenderingManager().render( docDirectory, app, appDirectory, this.renderers, fixedOptions, annotations );

			} else for( String locale : this.locales ) {
				fixedOptions.put( DocConstants.OPTION_LOCALE, locale );
				new RenderingManager().render( docDirectory, app, appDirectory, this.renderers, fixedOptions, annotations );
			}

		} catch( IOException e ) {
			throw new MojoExecutionException( "Exception while generating project documentation.", e );
		}
	}
}
