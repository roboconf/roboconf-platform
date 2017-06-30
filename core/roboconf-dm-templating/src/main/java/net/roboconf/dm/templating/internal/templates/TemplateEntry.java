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

package net.roboconf.dm.templating.internal.templates;

import java.io.File;

import com.github.jknack.handlebars.Template;

/**
 * Entry for a template being watched by the {@link TemplateWatcher}.
 * @author Pierre Bourret - Université Joseph Fourier
 */
public final class TemplateEntry {

	final File templateFile;
	final String appName, targetFilePath;
	final Template template;


	/**
	 * Constructor.
	 * @param templateFile the template file
	 * @param targetFilePath the path of the target file (null to output in the default's directory)
	 * @param appName the name of the application scoped by this template, or {@code null} for a global template entry
	 * @param template the compiled Handlebars template
	 * @param root true if the template is in the root directory, false if it is in a child directory
	 */
	public TemplateEntry( File templateFile, String targetFilePath, Template template, String appName ) {
		this.templateFile = templateFile;
		this.targetFilePath = targetFilePath;
		this.appName = appName;
		this.template = template;
	}

	/**
	 * @return the file
	 */
	public File getTemplateFile() {
		return this.templateFile;
	}

	/**
	 * @return the appName
	 */
	public String getAppName() {
		return this.appName;
	}

	/**
	 * @return the template
	 */
	public Template getTemplate() {
		return this.template;
	}

	/**
	 * @return the targetFilePath
	 */
	public String getTargetFilePath() {
		return this.targetFilePath;
	}
}
