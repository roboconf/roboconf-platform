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

package net.roboconf.dm.internal.api;

import net.roboconf.core.model.beans.Instance;
import net.roboconf.target.api.TargetHandlerParameters;

/**
 * A private API to schedule configurations of remote machines.
 * @author Vincent Zurczak - Linagora
 */
public interface ITargetConfigurator {

	/**
	 * Starts the executor.
	 */
	void start();

	/**
	 * Stops the executor.
	 */
	void stop();

	/**
	 * Reports a scoped instance that may have to be configured later.
	 * @param scopedInstance a non-null scoped instance
	 * @param parameters the target parameters
	 */
	void reportCandidate( TargetHandlerParameters parameters, Instance scopedInstance );

	/**
	 * Cancels an awaiting configuration for a given scoped instance.
	 * @param scopedInstance a non-null scoped instance
	 * @param parameters the target parameters
	 */
	void cancelCandidate( TargetHandlerParameters parameters, Instance scopedInstance );

	/**
	 * This method verifies if candidates needs and are ready to be configured.
	 * <p>
	 * A candidate is ready to be configured by a script if a configuration script exists
	 * and if its "data" map contains the {@link Instance#READY_FOR_CFG_MARKER} key.
	 * </p>
	 * <p>
	 * The configuration process is added to a queue that is processed
	 * in a separate thread.
	 * </p>
	 */
	void verifyCandidates();
}
