/**
 * Copyright 2015 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.doc.generator;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import net.roboconf.core.model.beans.Application;
import net.roboconf.doc.generator.internal.IRenderer;
import net.roboconf.doc.generator.internal.renderers.HtmlRenderer;
import net.roboconf.doc.generator.internal.renderers.MarkdownRenderer;

/**
 * @author Vincent Zurczak - Linagora
 */
public class RenderingManager {

	/**
	 * The available rendering solutions.
	 * @author Vincent Zurczak - Linagora
	 */
	public enum Renderer {
		HTML, PDF, MARKDOWN, FOP;
	}


	/**
	 * Renders a Roboconf application into a given format.
	 * @param outputDirectory the directory into which the documentation must be generated
	 * @param application an application
	 * @param applicationDirectory the application's directory
	 * @param renderer a renderer
	 * @param options the generation options (can be null)
	 * @throws IOException if something went wrong
	 */
	public void render( File outputDirectory, Application application, File applicationDirectory, Renderer renderer, Map<String,String> options )
	throws IOException {
		options = fixOptions( options );
		buildRenderer( outputDirectory, application, applicationDirectory, renderer ).render( options );
	}


	/**
	 * Fixes the options (handle null, add default options if not set, etc).
	 * @param options the received options
	 * @return a non-null map of options with the defaults set if necessary
	 */
	private Map<String,String> fixOptions( Map<String,String> options ) {

		if( options == null )
			options = new HashMap<String,String> ();

		if( ! options.containsKey( DocConstants.OPTION_IMG_BACKGROUND_COLOR ))
			options.put( DocConstants.OPTION_IMG_BACKGROUND_COLOR, DocConstants.DEFAULT_BACKGROUND_COLOR );

		if( ! options.containsKey( DocConstants.OPTION_IMG_FOREGROUND_COLOR ))
			options.put( DocConstants.OPTION_IMG_FOREGROUND_COLOR, DocConstants.DEFAULT_FOREGROUND_COLOR );

		if( ! options.containsKey( DocConstants.OPTION_IMG_HIGHLIGHT_BG_COLOR ))
			options.put( DocConstants.OPTION_IMG_HIGHLIGHT_BG_COLOR, DocConstants.DEFAULT_HIGHLIGHT_BG_COLOR );

		return options;
	}


	/**
	 * Builds the right renderer.
	 * @param outputDirectory
	 * @param application
	 * @param applicationDirectory
	 * @param renderer
	 * @return a renderer
	 */
	private IRenderer buildRenderer( File outputDirectory, Application application, File applicationDirectory, Renderer renderer ) {

		IRenderer result = null;
		switch( renderer ) {
		case HTML:
			result = new HtmlRenderer( outputDirectory, application, applicationDirectory );
			break;

		case MARKDOWN:
			result = new MarkdownRenderer( outputDirectory, application, applicationDirectory );
			break;

		default:
			break;
		}

		return result;
	}
}
