/**
 * Copyright 2013-2014 Linagora, Université Joseph Fourier
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

package net.roboconf.plugin.api.template;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import net.roboconf.core.model.runtime.Instance;
import net.roboconf.plugin.api.internal.template.InstanceBean;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

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
	 * @param instance
	 * @param templateFile
	 * @param writer
	 * @throws IOException
	 */
	public static void injectInstanceImports(Instance instance, File templateFile, Writer writer)
	throws IOException {

		MustacheFactory mf = new DefaultMustacheFactory( templateFile.getParentFile());
	    Mustache mustache = mf.compile( templateFile.getName());
	    mustache.execute(writer, new InstanceBean(instance)).flush();
	}


	public static void injectInstanceImports(Instance instance, String templateFilePath, Writer writer)
	throws IOException {
		injectInstanceImports(instance, new File( templateFilePath ), writer);
	}


	public static void injectInstanceImports(Instance instance, String templateFile, File out)
	throws IOException {
		injectInstanceImports(instance, templateFile, new FileWriter(out));
	}


	public static void injectInstanceImports(Instance instance, File templateFile, File out)
	throws IOException {
		injectInstanceImports(instance, templateFile.getAbsolutePath(), out);
	}
}
