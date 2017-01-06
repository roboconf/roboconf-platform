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

/**
 * @author Vincent Zurczak - Linagora
 */
public interface DocConstants {

	/**
	 * The application's directory that contain additional information.
	 */
	String DOC_DIR = "doc";

	/**
	 * The section name dedicated to components.
	 */
	String SECTION_COMPONENTS = "components/";

	/**
	 * The section name dedicated to facets.
	 */
	String SECTION_FACETS = "facets/";

	/**
	 * The section name dedicated to instances.
	 */
	String SECTION_INSTANCES = "instances/";


	/**
	 * The file suffix for all the custom information.
	 */
	String FILE_SUFFIX = ".txt";

	/**
	 * The file extension for custom summary about a component.
	 */
	String COMP_SUMMARY = ".summary" + FILE_SUFFIX;

	/**
	 * The file extension for extra information about a component.
	 */
	String COMP_EXTRA = ".extra" + FILE_SUFFIX;

	/**
	 * The file extension for custom summary about a component.
	 */
	String FACET_DETAILS = ".facet" + FILE_SUFFIX;

	/**
	 * The file name prefix for application descriptions.
	 */
	String APP_DESC_PREFIX = "app.desc";

	/**
	 * The prefix for all the options.
	 */
	String OPTION_PREFIX = "option.";



	/**
	 * The option to specify the background color in generated images.
	 * <p>
	 * Default to {@link #DEFAULT_BACKGROUND_COLOR}.
	 * </p>
	 */
	String OPTION_IMG_BACKGROUND_COLOR = OPTION_PREFIX + "img.background.color";

	/**
	 * The option to specify the foreground color in generated images.
	 * <p>
	 * Default to {@link #DEFAULT_FOREGROUND_COLOR}.
	 * </p>
	 */
	String OPTION_IMG_FOREGROUND_COLOR = OPTION_PREFIX + "img.foreground.color";

	/**
	 * The option to specify the background color of highlighted components in generated images.
	 * <p>
	 * Default to {@link #DEFAULT_HIGHLIGHT_BG_COLOR}.
	 * </p>
	 */
	String OPTION_IMG_HIGHLIGHT_BG_COLOR = OPTION_PREFIX + "img.highlight.bg.color";

	/**
	 * The option to indicate we deal with a recipe and not a complete application.
	 */
	String OPTION_RECIPE = OPTION_PREFIX + "recipe";



	/**
	 * The option to embed a custom CSS file for HTML outputs.
	 * <p>
	 * An URL is expected (http or file).
	 * </p>
	 */
	String OPTION_HTML_CSS_FILE = OPTION_PREFIX + "html.css.file";

	/**
	 * The option to reference a custom CSS file for HTML outputs.
	 * <p>
	 * Instead of copying a CSS file, we will reference an existing one
	 * (e.g. on a remote server).
	 * </p>
	 */
	String OPTION_HTML_CSS_REFERENCE = OPTION_PREFIX + "html.css.reference";

	/**
	 * The option to specify the output should be divided in several HTML files.
	 */
	String OPTION_HTML_EXPLODED = OPTION_PREFIX + "html.exploded";

	/**
	 * The option to specify the path of an image file to use as the header image.
	 * <p>
	 * By default, a Roboconf image is used.
	 * </p>
	 */
	String OPTION_HTML_HEADER_IMAGE_FILE = OPTION_PREFIX + "html.header.image.file";



	/**
	 * The option to specify the locale for output files.
	 * <p>
	 * Example: en_US, fr_FR.
	 * </p>
	 */
	String OPTION_LOCALE = OPTION_PREFIX + "locale";

	/**
	 * The option to generate images only once in a generation row.
	 * <p>
	 * If we generate HTML and PDF documentation, these are two generate
	 * operations but this option will prevent generating images twice.
	 * </p>
	 */
	String OPTION_GEN_IMAGES_ONCE = OPTION_PREFIX + "gen.images.once";



	/**
	 * The default background color for generated images.
	 */
	String DEFAULT_BACKGROUND_COLOR = "#ffffff";

	/**
	 * The default foreground color for generated images.
	 */
	String DEFAULT_FOREGROUND_COLOR = "#b23e4b";

	/**
	 * The default background color for highlighted components in generated images.
	 */
	String DEFAULT_HIGHLIGHT_BG_COLOR = "#f3df20";
}
