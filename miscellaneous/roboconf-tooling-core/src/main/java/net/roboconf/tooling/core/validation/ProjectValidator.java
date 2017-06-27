/**
 * Copyright 2016-2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.tooling.core.validation;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import net.roboconf.core.Constants;
import net.roboconf.core.errors.RoboconfError;
import net.roboconf.core.errors.RoboconfErrorComparator;
import net.roboconf.core.errors.RoboconfErrorHelpers;
import net.roboconf.core.model.RuntimeModelIo;
import net.roboconf.core.model.RuntimeModelIo.ApplicationLoadResult;
import net.roboconf.core.model.RuntimeModelIo.InstancesLoadResult;
import net.roboconf.core.utils.Utils;

/**
 * @author Vincent Zurczak - Linagora
 */
public final class ProjectValidator {

	/**
	 * Private empty constructor.
	 */
	private ProjectValidator() {
		// nothing
	}


	/**
	 * Validates a project.
	 * <p>
	 * It first tries to load it as a Roboconf project.
	 * Then, it validates all the files within the project, even those
	 * that are not reachable from the project descriptor.
	 * </p>
	 *
	 * @param appDirectory the application's directory (must exist)
	 * @return a non-null list of errors, with the resolved location (sorted by error code)
	 */
	public static List<RoboconfError> validateProject( File appDirectory ) {

		// Load the application
		ApplicationLoadResult alr = RuntimeModelIo.loadApplication( appDirectory );

		// Then, search for all the graph files and validate them
		File dir = new File( appDirectory, Constants.PROJECT_DIR_GRAPH );
		if( dir.exists()) {
			for( File f : Utils.listAllFiles( dir, Constants.FILE_EXT_GRAPH )) {
				RuntimeModelIo.loadGraph( f, dir, alr );
			}
		}

		// Eventually, search for all the instance files and validate them
		dir = new File( appDirectory, Constants.PROJECT_DIR_INSTANCES );
		if( dir.exists()
				&& alr.getApplicationTemplate().getGraphs() != null ) {

			for( File f : Utils.listAllFiles( dir, Constants.FILE_EXT_INSTANCES )) {
				InstancesLoadResult ilr = RuntimeModelIo.loadInstances( f, dir, alr.getApplicationTemplate().getGraphs(), null );
				alr.getObjectToSource().putAll( ilr.getObjectToSource());
				alr.getLoadErrors().addAll( ilr.getLoadErrors());
			}
		}

		// Now, remove duplicate errors and resolve locations
		Collection<RoboconfError> errors = new HashSet<> ();
		errors.addAll( RoboconfErrorHelpers.resolveErrorsWithLocation( alr ));

		// Eventually, sort everything
		List<RoboconfError> errorsList = new ArrayList<>( errors );
		Collections.sort( errorsList, new RoboconfErrorComparator());

		return errorsList;
	}
}
