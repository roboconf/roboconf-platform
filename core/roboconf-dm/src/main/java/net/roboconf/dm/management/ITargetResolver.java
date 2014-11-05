/**
 * Copyright 2014 Linagora, Université Joseph Fourier, Floralis
 *
 * The present code is developed in the scope of their joint LINAGORA -
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

import java.util.List;

import net.roboconf.core.model.runtime.Instance;
import net.roboconf.target.api.TargetException;
import net.roboconf.target.api.TargetHandler;

/**
 * An interface to define how we resolve deployment targetHandlers handlers.
 * @author Vincent Zurczak - Linagora
 */
public interface ITargetResolver {

	/**
	 * Finds the right targetHandlers handler for a given instance.
	 * @param ma the managed application
	 * @param targetHandlers the list of available targetHandlers handlers (can be null)
	 * @param instance the (root) instance associated with a deployment targetHandlers
	 * @return a handler for a deployment targetHandlers
	 * @throws TargetException if no handler was found
	 */
	TargetHandler findTargetHandler( List<TargetHandler> target, ManagedApplication ma, Instance instance )
	throws TargetException;
}
