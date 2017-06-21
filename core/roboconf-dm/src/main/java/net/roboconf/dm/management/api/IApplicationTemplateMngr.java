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

package net.roboconf.dm.management.api;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.dm.management.exceptions.AlreadyExistingException;
import net.roboconf.dm.management.exceptions.InvalidApplicationException;
import net.roboconf.dm.management.exceptions.UnauthorizedActionException;

/**
 * @author Vincent Zurczak - Linagora
 */
public interface IApplicationTemplateMngr {

	/**
	 * Loads a new application template.
	 * @see ApplicationTemplateMngrDelegate#loadApplicationTemplate(File, File)
	 */
	ApplicationTemplate loadApplicationTemplate( File applicationFilesDirectory )
	throws AlreadyExistingException, InvalidApplicationException, IOException, UnauthorizedActionException;


	/**
	 * Deletes an application template.
	 * @param tplName the template's name
	 * @param tplVersion the template's version
	 */
	void deleteApplicationTemplate( String tplName, String tplVersion )
	throws UnauthorizedActionException, InvalidApplicationException, IOException;


	/**
	 * Restores templates from the configuration directory.
	 */
	void restoreTemplates();


	/**
	 * @return the application templates (never null)
	 */
	Set<ApplicationTemplate> getApplicationTemplates();


	/**
	 * Finds a template.
	 * <p>
	 * A template is identified by its name and its version.
	 * </p>
	 *
	 * @param name the template's name
	 * @param version the template's version
	 * @return an application template, or null if none was found
	 */
	ApplicationTemplate findTemplate( String name, String version );
}
