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

import java.io.IOException;
import java.util.Collection;
import java.util.Set;

import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.exceptions.AlreadyExistingException;
import net.roboconf.dm.management.exceptions.InvalidApplicationException;
import net.roboconf.dm.management.exceptions.UnauthorizedActionException;

/**
 * @author Vincent Zurczak - Linagora
 */
public interface IApplicationMngr {

	/**
	 * Finds an application by name.
	 * @param applicationName the application name (not null)
	 * @return an application, or null if it was not found
	 */
	Application findApplicationByName( String applicationName );

	/**
	 * Finds a managed application by name.
	 * @param applicationName the application name (not null)
	 * @return a managed application, or null if it was not found
	 */
	ManagedApplication findManagedApplicationByName( String applicationName );

	/**
	 * Creates a new application from a template.
	 * @return a managed application (never null)
	 * @throws IOException
	 * @throws AlreadyExistingException
	 * @throws InvalidApplicationException
	 */
	ManagedApplication createApplication( String name, String description, String tplName, String tplQualifier )
	throws IOException, AlreadyExistingException, InvalidApplicationException;

	/**
	 * Creates a new application from a template.
	 * @return a managed application (never null)
	 * @throws IOException
	 * @throws AlreadyExistingException
	 */
	ManagedApplication createApplication( String name, String description, ApplicationTemplate tpl )
	throws IOException, AlreadyExistingException;

	/**
	 * Updates an application with a new description.
	 * @param app the application to update
	 * @param newDesc the new description
	 * @throws IOException
	 */
	void updateApplication( ManagedApplication ma, String newDesc )
	throws IOException;

	/**
	 * Deletes an application.
	 * @param ma the managed application
	 * @throws UnauthorizedActionException if parts of the application are still running
	 * @throws IOException if errors occurred with the messaging or the removal of resources
	 */
	void deleteApplication( ManagedApplication ma )
	throws UnauthorizedActionException, IOException;

	/**
	 * Restores applications from the configuration directory.
	 */
	void restoreApplications();

	/**
	 * Determines whether a template is used by an application.
	 * @param tpl an application template (not null)
	 * @return true if at least one application uses it, false otherwise
	 */
	boolean isTemplateUsed( ApplicationTemplate tpl );

	/**
	 * @return a non-null collection of managed applications
	 */
	Collection<ManagedApplication> getManagedApplications();

	/**
	 * Binds (one-way direction) or unbinds two applications for external exports.
	 * @param ma the application into which a binding must be created
	 * @param externalExportPrefix the name of the prefix for external variables
	 * @param applicationName the name of the application to use (must be associated with the template)
	 * @param bind true to bind, false to unbind
	 * @throws UnauthorizedActionException if the application does not exist, or if it is not associated with the right template
	 * @throws IOException if an error occurred while creating the binding
	 */
	void bindOrUnbindApplication( ManagedApplication ma, String externalExportPrefix, String applicationName, boolean bind )
	throws UnauthorizedActionException, IOException;

	/**
	 * Replaces application bindings for a given prefix.
	 * @param ma the application into which a binding must be created
	 * @param externalExportPrefix the name of the prefix for external variables
	 * @param applicationNames a non-null set of application names (must be associated with the template)
	 * @throws UnauthorizedActionException if the application does not exist, or if it is not associated with the right template
	 * @throws IOException if an error occurred while creating the binding
	 */
	void replaceApplicationBindings( ManagedApplication ma, String externalExportPrefix, Set<String> applicationNames )
	throws UnauthorizedActionException, IOException;
}
