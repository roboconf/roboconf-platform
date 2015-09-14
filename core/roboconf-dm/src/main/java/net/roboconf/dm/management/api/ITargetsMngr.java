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

package net.roboconf.dm.management.api;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import net.roboconf.core.model.beans.AbstractApplication;
import net.roboconf.core.model.beans.Application;

/**
 * @author Vincent Zurczak - Linagora
 */
public interface ITargetsMngr {

	String ID = "id";


	/**
	 * Creates a new target.
	 * @param targetContent the target content
	 * @return the ID of the newly created target
	 * @throws IOException if something went wrong
	 */
	String createTarget( String targetContent ) throws IOException;

	/**
	 * Creates a new target.
	 * @param targetPropertiesFile a target.properties file
	 * @return the ID of the newly created target
	 * @throws IOException if something went wrong
	 */
	String createTarget( File targetPropertiesFile ) throws IOException;

	/**
	 * Updates an existing target.
	 * @param targetId the target's ID
	 * @param newTargetContent the new content of the target.properties file
	 * @throws IOException if something went wrong
	 */
	void updateTarget( String targetId, String newTargetContent ) throws IOException;

	/**
	 * Deletes a target.
	 * @param targetId the target's ID
	 * @throws IOException if something went wrong or if the target is being used
	 */
	void deleteTarget( String targetId ) throws IOException;

	/**
	 * Associates a target and a scoped instance within an application or application template.
	 * @param targetId a target ID
	 * @param app an application or an application template
	 * @param instancePath an instance path
	 * @throws IOException if something went wrong or if the association is not possible (e.g. instance is already deployed)
	 */
	void associateTargetWithScopedInstance( String targetId, AbstractApplication app, String instancePath ) throws IOException;

	/**
	 * Dissociates a target and a scoped instance within an application or application template.
	 * @param targetId a target ID
	 * @param app an application or application template
	 * @param instancePath an instance path
	 * @throws IOException if something went wrong or if dissociation is not possible (e.g. instance is already deployed)
	 */
	void dissociateTargetFromScopedInstance( String targetId, AbstractApplication app, String instancePath ) throws IOException;

	/**
	 * Finds the target properties of a scoped instance within an application or an application template.
	 * <p>
	 * This method injects the target ID in the result, provided properties
	 * were found.
	 * </p>
	 *
	 * @param app an application or application template
	 * @param instancePath an instance path
	 * @return a non-null map of properties (empty if the file was not found)
	 */
	Map<String,String> findTargetProperties( AbstractApplication app, String instancePath );

	/**
	 * Finds all the available targetsMngr for a given application or application template.
	 * <p>
	 * The result is built by listing all the targetsMngr and by filtering them with hints.
	 * Indeed, some targetsMngr may be "tagged" to be used (or let's visible) only for some
	 * applications or templates.
	 * </p>
	 *
	 * @param app an application or an application template
	 * @return a non-null list of targetsMngr
	 * @see #addHint(int, AbstractApplication)
	 * @see #removeHint(int, AbstractApplication)
	 */
	List<TargetBean> findTargets( AbstractApplication app );

	/**
	 * Adds a hint to a target ID.
	 * <p>
	 * A hint is not a rule. It is only a filter that can be used when
	 * retrieving targetsMngr for a given application or template. It could be seen
	 * as a scope. Indeed, some targetsMngr may be specific to an application or an
	 * application template. And in this case, we do not want them to appear for
	 * other applications.
	 * </p>
	 * <p>
	 * If the target ID is invalid, this method does nothing.
	 * </p>
	 *
	 * @param targetId a target ID
	 * @param app an application or an application template
	 * @see #findTargets(AbstractApplication)
	 */
	void addHint( String targetId, AbstractApplication app );

	/**
	 * Removes a hint from a target ID.
	 * <p>
	 * A hint is not a rule. It is only a filter that can be used when
	 * retrieving targetsMngr for a given application or template. It could be seen
	 * as a scope. Indeed, some targetsMngr may be specific to an application or an
	 * application template. And in this case, we do not want them to appear for
	 * other applications.
	 * </p>
	 * <p>
	 * If the target ID is invalid, this method does nothing.
	 * Same thing if there was no hint between this target and this application
	 * or template.
	 * </p>
	 *
	 * @param targetId a target ID
	 * @param app an application or an application template
	 * @see #findTargets(AbstractApplication)
	 */
	void removeHint( String targetId, AbstractApplication app );

	/**
	 * Mars a target as used or not used by an abstract application.
	 * @param targetId a target ID
	 * @param app an application
	 * @param instancePath an instance path
	 * @param used true if its used, false if it is not used (anymore)
	 */
	void markTargetAs( String targetId, Application app, String instancePath, boolean used );


	/**
	 * An informative bean that contains significant information to manage targetsMngr.
	 * @author Vincent Zurczak - Linagora
	 */
	public static class TargetBean {
		public int id;
		public String name, targetHandlerId;
	}
}
