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

package net.roboconf.dm.management;

import java.io.File;

import net.roboconf.core.model.beans.Application;

/**
 * Interface for Roboconf application's templating support.
 * <p>
 * This interface is <em>not</em> intended to be used by consumers.
 * They should use the {@code net.roboconf.dm.templating.TemplatingService} interface instead.
 * </p>
 *
 * @author Pierre Bourret - Université Joseph Fourier
 */
public interface TemplatingManagerService {

	/**
	 * Starts the monitoring, using the provided Roboconf configuration directory.
	 * @param configDir the Roboconf configuration directory.
	 */
	void startTemplating( File configDir );


	/**
	 * Adds the given application to the monitoring manager, so it can start to monitor it.
	 * <p>
	 * This method is called by the Roboconf DM when an
	 * application is added, or when binding to the monitoring manager.
	 * </p>
	 *
	 * @param app the application to add.
	 * @throws IllegalStateException if monitoring is currently stopped.
	 */
	void addApplication( Application app );


	/**
	 * Notifies the monitoring manager that the given application model has changed, and that the monitoring reports must
	 * be regenerated.
	 *
	 * @param app the changing application.
	 * @throws IllegalStateException if monitoring is currently stopped.
	 */
	void updateApplication( Application app );


	/**
	 * Removes the given application from the monitoring manager, so it stops to be monitored.<p>This method is called
	 * by the Roboconf DM when an application is removed.</p><p>The monitoring templates and reports specific to the
	 * application being removed are <em>kept and left untouched.</em></p>
	 *
	 * @param app the application to remove.
	 * @throws IllegalStateException if monitoring is currently stopped.
	 */
	void removeApplication( Application app );


	/**
	 * Stops the monitoring. All the applications that are currently monitored are removed, and must be re-added after
	 * this service has been {@linkplain #startTemplating(File) started} again.
	 */
	void stopTemplating();

}
