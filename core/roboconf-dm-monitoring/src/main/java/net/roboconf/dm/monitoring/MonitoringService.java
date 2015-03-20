/**
 * Copyright 2013-2015 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.dm.monitoring;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import net.roboconf.core.model.beans.Application;

/**
 * Interface for monitoring template management.
 *
 * @author Pierre Bourret - Université Joseph Fourier
 */
public interface MonitoringService {

	/**
	 * The name of the directory where application monitoring templates are read from, relative to the Roboconf
	 * configuration directory.
	 */
	String MONITORING_TEMPLATE_DIRECTORY = "monitoring-template";

	/**
	 * The name of the directory where application monitoring generated files are written/updated, relative to the
	 * Roboconf configuration directory.
	 */
	String MONITORING_TARGET_DIRECTORY = "monitoring-generated";

	/**
	 * The extension of monitoring template files.
	 */
	String MONITORING_TEMPLATE_FILE_EXTENSION = ".tpl";

	/**
	 * Get the target directory, where monitoring reports are generated.
	 *
	 * @return the target directory.
	 */
	File getTargetDirectory();

	/**
	 * Add a monitoring template.
	 *
	 * @param application the application scope for the template to add, or {@code null} to add a global template.
	 * @param name        the name of the template to add.
	 * @param content     the content of the template. The stream is not closed by this method, and must be manually
	 *                    closed afterwards.
	 * @return {@code true} is the template was successfully added, {@code false} if there is already a template with
	 * the same name.
	 * @throws java.io.IOException if the template cannot be added because of an IO error.
	 */
	boolean addTemplate( Application application, String name, InputStream content ) throws IOException;

	/**
	 * Get the current list of the monitoring templates, for the given application.
	 * <p>
	 * The returned set <em>does not</em> contain the global template identifiers, as they may collide with the
	 * application-specific template identifiers. In order to get the global template, call this method with the
	 * {@code application} parameter set to {@code null}.
	 * </p>
	 *
	 * @param application the application scope for the template to list, or {@code null} to list global templates
	 *                    only.
	 * @return the current list of the monitoring templates. The returned list is an immutable snapshot.
	 */
	Set<String> listTemplates( Application application );

	/**
	 * Remove a monitoring template.
	 *
	 * @param application the application scope for the template to remove, or {@code null} to remove a global
	 *                    template.
	 * @param name        the name of the template to remove.
	 * @return {@code true} is the template was successfully removed, {@code false} if there was no template with such a
	 * name.
	 * @throws java.io.IOException if the template cannot be removed because of an IO error.
	 */
	boolean removeTemplate( Application application, String name ) throws IOException;

}
