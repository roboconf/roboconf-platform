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

package net.roboconf.doc.generator;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.doc.generator.internal.IRenderer;
import net.roboconf.doc.generator.internal.renderers.FopRenderer;
import net.roboconf.doc.generator.internal.renderers.HtmlRenderer;
import net.roboconf.doc.generator.internal.renderers.MarkdownRenderer;
import net.roboconf.doc.generator.internal.renderers.PdfRenderer;

/**
 * @author Vincent Zurczak - Linagora
 */
public class RenderingManager {

	private final Logger logger = Logger.getLogger( getClass().getName());


	/**
	 * The available rendering solutions.
	 * @author Vincent Zurczak - Linagora
	 */
	public enum Renderer {
		HTML, PDF, MARKDOWN, FOP;

		/**
		 * Finds a renderer by name (case-insensitive).
		 * @param renderer a render name
		 * @return a render, or null if none matched
		 */
		public static Renderer which( String renderer ) {

			Renderer result = null;
			for( Renderer r : values()) {
				if( r.toString().equalsIgnoreCase( renderer )) {
					result = r;
					break;
				}
			}

			return result;
		}
	}


	/**
	 * Renders a Roboconf application into a given format.
	 * @param outputDirectory the directory into which the documentation must be generated
	 * @param applicationTemplate an application template
	 * @param applicationDirectory the application's directory
	 * @param renderer a renderer
	 * @param options the generation options (can be null)
	 * @param typeAnnotations the type annotations (found by the converter - can be null)
	 * @throws IOException if something went wrong
	 */
	public void render(
			File outputDirectory,
			ApplicationTemplate applicationTemplate,
			File applicationDirectory,
			Renderer renderer,
			Map<String,String> options,
			Map<String,String> typeAnnotations )
	throws IOException {

		options = fixOptions( options );
		buildRenderer( outputDirectory, applicationTemplate, applicationDirectory, renderer, typeAnnotations ).render( options );
	}


	/**
	 * Renders a Roboconf application into a given format.
	 * <p>
	 * When several renderers are provided, images generation is performed only once.
	 * </p>
	 *
	 * @param outputDirectory the directory into which the documentation must be generated
	 * @param applicationTemplate an application template
	 * @param applicationDirectory the application's directory
	 * @param renderers a non-null list of render names
	 * @param options the generation options (can be null)
	 * @param typeAnnotations the type annotations (found by the converter - can be null)
	 * @throws IOException if something went wrong
	 */
	public void render(
			File outputDirectory,
			ApplicationTemplate applicationTemplate,
			File applicationDirectory,
			List<String> renderers,
			Map<String,String> options,
			Map<String,String> typeAnnotations )
	throws IOException {

		options = fixOptions( options );
		if( renderers.size() > 1 )
			options.put( DocConstants.OPTION_GEN_IMAGES_ONCE, "true" );

		for( String renderer : renderers ) {
			Renderer r = Renderer.which( renderer );
			if( r == null ) {
				this.logger.severe( "No renderer called '" + renderer +"' was found. Skipping it." );
				continue;
			}

			String subDirName = renderer.toLowerCase();
			String locale = options.get( DocConstants.OPTION_LOCALE );
			if( locale != null )
				subDirName += "_" + locale;

			File subDir = new File( outputDirectory, subDirName );
			buildRenderer( subDir, applicationTemplate, applicationDirectory, r, typeAnnotations ).render( options );
		}
	}


	/**
	 * Fixes the options (handle null, add default options if not set, etc).
	 * @param options the received options
	 * @return a non-null map of options with the defaults set if necessary
	 */
	private Map<String,String> fixOptions( Map<String,String> options ) {

		if( options == null )
			options = new HashMap<> ();

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
	 * @param applicationTemplate
	 * @param applicationDirectory
	 * @param renderer
	 * @param typeAnnotations
	 * @return a renderer
	 */
	private IRenderer buildRenderer(
			File outputDirectory,
			ApplicationTemplate applicationTemplate,
			File applicationDirectory,
			Renderer renderer,
			Map<String,String> typeAnnotations ) {

		IRenderer result = null;
		switch( renderer ) {
		case HTML:
			result = new HtmlRenderer( outputDirectory, applicationTemplate, applicationDirectory, typeAnnotations );
			break;

		case MARKDOWN:
			result = new MarkdownRenderer( outputDirectory, applicationTemplate, applicationDirectory, typeAnnotations );
			break;

		case FOP:
			result = new FopRenderer( outputDirectory, applicationTemplate, applicationDirectory, typeAnnotations );
			break;

		case PDF:
			result = new PdfRenderer( outputDirectory, applicationTemplate, applicationDirectory, typeAnnotations );
			break;

		default:
			break;
		}

		return result;
	}
}
