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

package net.roboconf.dm.templating.internal.templates;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Logger;

import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.context.JavaBeanValueResolver;
import com.github.jknack.handlebars.context.MapValueResolver;
import com.github.jknack.handlebars.context.MethodValueResolver;

import net.roboconf.core.model.beans.Application;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.templating.internal.contexts.ApplicationContextBean;
import net.roboconf.dm.templating.internal.contexts.ContextUtils;
import net.roboconf.dm.templating.internal.resolvers.ComponentPathResolver;

/**
 * @author Vincent Zurczak - Linagora
 */
public final class TemplateUtils {

	/**
	 * Constructor.
	 */
	private TemplateUtils() {
		// nothing
	}


	/**
	 * Finds an application name from a template file.
	 *
	 * @param templateDir the templates root directory, the one being watched by the TemplateWatcher
	 * @param templateFile the template file
	 * @return name of the application targeted by the given template file, or {@code null} for a template global to all applications
	 * @throws IllegalStateException if the provided {@code templateFile} does not match
	 */
	public static String findApplicationName( final File templateDir, final File templateFile ) {

		final File parentDir = templateFile.getParentFile();
		final String appName;

		// No intermediate directory, the template is global!
		if( templateDir.equals( parentDir ))
			appName = null;

		// One intermediate directory: the template is specific to one application.
		// The name of the application is the name of the intermediate directory.
		else if( templateDir.equals( parentDir.getParentFile()))
			appName = parentDir.getName();

		// Too many levels!
		else
			throw new IllegalArgumentException( "Not a template file: " + templateFile );

		return appName;
	}


	/**
	 * Generates files from templates for a given application.
	 * <p>
	 * All the generated files for a given application are located
	 * under <code>outputDirectory/application-name</code>. Even global templates
	 * are output there.
	 * </p>
	 *
	 * @param app an application (not null)
	 * @param outputDirectory the output directory (not null)
	 * @param templates a non-null collection of templates
	 * @param logger a logger
	 * @throws IOException if something went wrong
	 */
	public static void generate( Application app, File outputDirectory, Collection<TemplateEntry> templates, Logger logger )
	throws IOException {

		// Create the context on the fly
		ApplicationContextBean appCtx = ContextUtils.toContext( app );
		Context wrappingCtx = Context
				.newBuilder( appCtx )
				.resolver(
					MapValueResolver.INSTANCE,
					JavaBeanValueResolver.INSTANCE,
					MethodValueResolver.INSTANCE,
					new ComponentPathResolver()
				).build();

		// Deal with the templates
		try {
			for( TemplateEntry template : templates ) {

				logger.fine( "Processing template " + template.getTemplateFile() + " to application " + app + "." );

				File target;
				String targetFilePath = template.getTargetFilePath();
				if( ! Utils.isEmptyOrWhitespaces( targetFilePath )) {
					target = new File( targetFilePath.replace( "${app}", app.getName()));

				} else {
					String filename = template.getTemplateFile().getName().replaceFirst( "\\.tpl$", "" );
					target = new File( outputDirectory, app.getName() + "/" + filename );
				}

				Utils.createDirectory( target.getParentFile());
				String output = template.getTemplate().apply( wrappingCtx );
				Utils.writeStringInto( output, target );

				logger.fine( "Template " + template.getTemplateFile() + " was processed with application " + app + ". Output is in " + target );
			}

		} finally {
			wrappingCtx.destroy();
		}
	}


	/**
	 * Deletes the generated files for a given application.
	 * @param app an application (not null)
	 * @param outputDirectory the output directory (not null)
	 */
	public static void deleteGeneratedFiles( Application app, File outputDirectory ) {

		File target = new File( outputDirectory, app.getName());
		Utils.deleteFilesRecursivelyAndQuietly( target );
	}


	/**
	 * Finds the templates that can apply to a given application.
	 * @param appName an application name (can be null to match all)
	 * @param templates a non-null collection of templates
	 * @return a non-null collection of templates
	 */
	public static Collection<TemplateEntry> findTemplatesForApplication( String appName, Collection<TemplateEntry> templates ) {

		final Collection<TemplateEntry> result = new ArrayList<> ();
		for( TemplateEntry te : templates ) {
			// TE.appName == null => OK. It applies to this application.
			// TE.appName == appName => OK. It also applies.
			if( te.getAppName() == null || te.getAppName().equals( appName ))
				result.add( te );
		}

		return result;
	}
}
