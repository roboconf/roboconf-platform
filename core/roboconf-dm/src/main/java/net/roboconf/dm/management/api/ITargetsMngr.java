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
import java.util.List;
import java.util.Map;

import net.roboconf.core.model.beans.AbstractApplication;
import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.runtime.TargetUsageItem;
import net.roboconf.core.model.runtime.TargetWrapperDescriptor;
import net.roboconf.dm.management.exceptions.UnauthorizedActionException;

/**
 * @author Vincent Zurczak - Linagora
 */
public interface ITargetsMngr {


	// CRUD operations on targets


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
	 * @param the application template that loads this target (can be null)
	 * @return the ID of the newly created target
	 * @throws IOException if something went wrong
	 */
	String createTarget( File targetPropertiesFile, ApplicationTemplate creator ) throws IOException;

	/**
	 * Updates an existing target.
	 * @param targetId the target's ID
	 * @param newTargetContent the new content of the target.properties file
	 * @throws IOException if the update failed
	 * @throws UnauthorizedActionException if the target does not exist
	 */
	void updateTarget( String targetId, String newTargetContent ) throws IOException, UnauthorizedActionException;

	/**
	 * Deletes a target.
	 * <p>
	 * Deletion is only permitted if the target is not used.
	 * </p>
	 *
	 * @param targetId the target's ID
	 * @throws IOException if something went wrong
	 * @throws UnauthorizedActionException if the target is being used
	 * @see #lockAndGetTarget(AbstractApplication, String)
	 * @see #unlockTarget(AbstractApplication, String)
	 */
	void deleteTarget( String targetId ) throws IOException, UnauthorizedActionException;


	// Associating targets and application instances


	/**
	 * Associates a target and a scoped instance within an application or application template.
	 * <p>
	 * If the instance path/component name is null, then the target is considered to be the default one for this application.
	 * </p>
	 * <p>
	 * If the instance/component was already associated with another target, this association is removed.
	 * </p>
	 * <p>
	 * If the instance path/component name does not match anything in the application, then nothing is saved.
	 * </p>
	 *
	 * @param targetId a target ID
	 * @param app an application or an application template
	 * @param instancePathOrComponentName an instance path (null to set the default for the application) or a component name (starts with '@')
	 * @throws IOException if something went wrong
	 * @throws UnauthorizedActionException if the instance is already deployed with another target
	 */
	void associateTargetWith( String targetId, AbstractApplication app, String instancePathOrComponentName )
	throws IOException, UnauthorizedActionException;

	/**
	 * Dissociates a target and a scoped instance within an application or application template.
	 * <p>
	 * If the instance path/component name is null, no action is undertaken. To override the default target for a given
	 * application, just invoke {@link #associateTargetWithScopedInstance(String, AbstractApplication, String)}.
	 * </p>
	 *
	 * @param app an application or application template
	 * @param instancePathOrComponentName an instance path or a component name (starts with '@')
	 * @throws IOException if something went wrong
	 * @throws UnauthorizedActionException if the instance is already deployed with another target
	 */
	void dissociateTargetFrom( AbstractApplication app, String instancePathOrComponentName )
	throws IOException, UnauthorizedActionException;

	/**
	 * Copies the target mapping of the application template to the application.
	 * <p>
	 * If there was already a target mapping for the application, it is overwritten.
	 * </p>
	 * <p>
	 * If there is no mapping for the application's template, then nothing is copied.
	 * </p>
	 *
	 * @param app an application
	 * @throws IOException if something went wrong
	 */
	void copyOriginalMapping( Application app ) throws IOException;

	/**
	 * Removes any reference to an application after it was deleted.
	 * @param app an application or application template
	 * @throws IOException if something went wrong
	 */
	void applicationWasDeleted( AbstractApplication app ) throws IOException;


	// Finding targets


	/**
	 * @author Vincent Zurczak - Linagora
	 */
	public interface TargetProperties {

		/**
		 * @return the properties as a map
		 */
		Map<String,String> asMap();

		/**
		 * @return the properties as a string
		 */
		String asString();

		/**
		 * @return the properties file (or null if no matching target was found)
		 */
		File getSourceFile();
	}


	/**
	 * Finds the target properties of a scoped instance within an application or an application template.
	 * <p>
	 * Notice that this variable does not expand anything (e.g. IP addresses).
	 * </p>
	 *
	 * @param app an application or application template
	 * @param instancePath an instance path
	 * @return target properties (not null, empty if the file was not found)
	 * @see #lockAndGetTarget(AbstractApplication, String)
	 */
	TargetProperties findTargetProperties( AbstractApplication app, String instancePath );

	/**
	 * Finds the properties for a given target.
	 * <p>
	 * Notice that this variable does not expand anything (e.g. IP addresses).
	 * </p>
	 *
	 * @param targetId the target ID
	 * @return target properties (not null, empty if the file was not found)
	 */
	TargetProperties findTargetProperties( String targetId );

	/**
	 * Finds the target ID for a scoped instance within an application or an application template.
	 * @param app an application or application template
	 * @param instancePath an instance path or null to find the default target for this application
	 * @param strict if set to true, only a target that was explicitly associated will be returned (no default)
	 * @return a string, or null if no associated target was found
	 */
	String findTargetId( AbstractApplication app, String instancePath, boolean strict );

	/**
	 * Finds the target ID for a scoped instance within an application or an application template.
	 * <p>
	 * Equivalent to <code>findTargetId( app, instancePath, false );</code>
	 * </p>
	 *
	 * @param app an application or application template
	 * @param instancePath an instance path or null to find the default target for this application
	 * @return a string, or null if no associated target was found
	 */
	String findTargetId( AbstractApplication app, String instancePath );

	/**
	 * @return a non-null list of targets
	 */
	List<TargetWrapperDescriptor> listAllTargets();

	/**
	 * @param targetId a non-null target ID
	 * @return a wrapper describing the given target, or null if it was not found
	 */
	TargetWrapperDescriptor findTargetById( String targetId );


	// Definitions and relations with hints (contextual help to reduce the number of choices when associating
	// a target and an application instance). Indeed, some targets may be very specific
	// and we thus do not need to list them for some applications as it would not make sense.


	/**
	 * Lists all the available targets for a given application or application template.
	 * <p>
	 * The result is built by listing all the targets and by filtering them with hints.
	 * Indeed, some targets may be "tagged" to be used (or let's say visible) only for some
	 * applications or templates.
	 * </p>
	 * <p>
	 * If <code>app</code> is a template, then we only search for the targets associated with
	 * it. If <code>app</code> is an application, we either search for it or for its template.
	 * </p>
	 *
	 * @param app an application or an application template
	 * @return a non-null list of targetsMngr
	 * @see #addHint(int, AbstractApplication)
	 * @see #removeHint(int, AbstractApplication)
	 */
	List<TargetWrapperDescriptor> listPossibleTargets( AbstractApplication app );

	/**
	 * Adds a hint to a target ID.
	 * <p>
	 * A hint is not a rule. It is only a filter that can be used when
	 * retrieving targets for a given application or template. It could be seen
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
	 * @throws IOException if the hint could not be saved
	 * @see #findTargets(AbstractApplication)
	 */
	void addHint( String targetId, AbstractApplication app ) throws IOException;

	/**
	 * Removes a hint from a target ID.
	 * <p>
	 * A hint is not a rule. It is only a filter that can be used when
	 * retrieving targets for a given application or template. It could be seen
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
	 * @throws IOException if the hint removal could not be saved
	 * @see #findTargets(AbstractApplication)
	 */
	void removeHint( String targetId, AbstractApplication app ) throws IOException;


	// Atomic operations that prevent conflicting actions.
	// As an example, we do not want to be able to delete a target when it is already used.


	/**
	 * Almost equivalent to {@link #findTargetProperties(AbstractApplication, String)}.
	 * <p>
	 * However, there are two differences. First, it locks the target.
	 * And second, it expands properties with information extracted from the instance.
	 * </p>
	 * <p>
	 * Once locked, the target cannot be deleted until it has been released.
	 * However, it can be updated (at the user's risks and perils).
	 * </p>
	 *
	 * @param app an application
	 * @param scopedInstance a scoped instance
	 * @return target properties (not null, empty if the file was not found)
	 * @throws IOException if the target could not be locked or properties not be read
	 * <p>
	 * In case of exception, you will have to explicitly invoke {@link #unlockTarget(AbstractApplication, Instance)}.
	 * </p>
	 */
	TargetProperties lockAndGetTarget( Application app, Instance scopedInstance ) throws IOException;


	/**
	 * Unlocks a target.
	 * <p>
	 * If the target had not been locked, this method does nothing.
	 * </p>
	 *
	 * @param app an application
	 * @param scopedInstance a scoped instance
	 * @throws IOException if the target could not be unlocked
	 */
	void unlockTarget( Application app, Instance scopedInstance ) throws IOException;


	// Diagnostics


	/**
	 * Finds usage statistics for a given target.
	 * @param targetId a non-null target ID
	 * @return a non-null list of usage item
	 */
	List<TargetUsageItem> findUsageStatistics( String targetId );


	/**
	 * Finds the scripts that will be sent and executed by an agent once the VM is created.
	 * @param targetId a target ID
	 * @return a non-null map (key = the file location, relative to the instance's directory, value = file content)
	 * @throws IOException if something went wrong while reading a file
	 */
	Map<String,byte[]> findScriptResourcesForAgent( String targetId ) throws IOException;


	/**
	 * Finds the scripts that will be sent and executed by an agent once the VM is created.
	 * @param app an application
	 * @param scopedInstance a scopedInstance
	 * @return a non-null map (key = the file location, relative to the instance's directory, value = file content)
	 * @throws IOException if something went wrong while reading a file
	 */
	Map<String,byte[]> findScriptResourcesForAgent( AbstractApplication app, Instance scopedInstance ) throws IOException;


	/**
	 * Finds the script the DM has to execute to complete the configuration of a machine.
	 * @param app an application
	 * @param scopedInstance a scopedInstance
	 * @return an existing file if such a script was found, null otherwise
	 */
	File findScriptForDm( AbstractApplication app, Instance scopedInstance );
}
