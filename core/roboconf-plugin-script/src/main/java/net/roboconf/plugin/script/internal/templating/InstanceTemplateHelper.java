/**
 * Copyright 2013-2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.plugin.script.internal.templating;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.utils.Utils;

/**
 * Provides methods for injecting Instance data into a template file.
 * @author gcrosmarie - Linagora
 */
public final class InstanceTemplateHelper {

	/**
	 * Private constructor.
	 */
	private InstanceTemplateHelper() {
		// nothing
	}


	/**
	 * Reads the import values of the instances and injects them into the template file.
	 * <p>
	 * See test resources to see the associated way to write templates
	 * </p>
	 *
	 * @param instance the instance whose imports must be injected
	 * @param templateFile the template file
	 * @param writer a writer
	 * @throws IOException if something went wrong
	 */
	public static void injectInstanceImports(Instance instance, File templateFile, Writer writer)
	throws IOException {

		MustacheFactory mf = new DefaultMustacheFactory( templateFile.getParentFile());
		Mustache mustache = mf.compile( templateFile.getName());
		mustache.execute(writer, new InstanceBean( instance )).flush();
	}


	/**
	 * Reads the import values of the instances and injects them into the template file.
	 * <p>
	 * See test resources to see the associated way to write templates
	 * </p>
	 *
	 * @param instance the instance whose imports must be injected
	 * @param templateFilePath the path of the template file
	 * @param writer a writer
	 * @throws IOException if something went wrong
	 */
	public static void injectInstanceImports(Instance instance, String templateFilePath, Writer writer)
	throws IOException {
		injectInstanceImports(instance, new File( templateFilePath ), writer);
	}


	/**
	 * Reads the import values of the instances and injects them into the template file.
	 * <p>
	 * See test resources to see the associated way to write templates
	 * </p>
	 *
	 * @param instance the instance whose imports must be injected
	 * @param templateFilePath the path of the template file
	 * @param targetFile the file to write into
	 * @throws IOException if something went wrong
	 */
	public static void injectInstanceImports(Instance instance, String templateFilePath, File targetFile)
	throws IOException {

		OutputStream os = null;
		try {
			os = new FileOutputStream( targetFile );
			OutputStreamWriter writer = new OutputStreamWriter( os, StandardCharsets.UTF_8.newEncoder());
			injectInstanceImports( instance, templateFilePath, writer );

		} finally {
			Utils.closeQuietly( os );
		}
	}


	/**
	 * Reads the import values of the instances and injects them into the template file.
	 * <p>
	 * See test resources to see the associated way to write templates
	 * </p>
	 *
	 * @param instance the instance whose imports must be injected
	 * @param templateFile the template file
	 * @param out the file to write into
	 * @throws IOException if something went wrong
	 */
	public static void injectInstanceImports(Instance instance, File templateFile, File out)
	throws IOException {
		injectInstanceImports( instance, templateFile.getAbsolutePath(), out );
	}
}
