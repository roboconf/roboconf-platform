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

package net.roboconf.dm.internal.api;

import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.Instance;

/**
 * An API to deal with variable values generated automatically.
 * <p>
 * This API is made internal as it does not make sense to
 * make it visible to other bundles.
 * </p>
 *
 * @author Vincent Zurczak - Linagora
 */
public interface IRandomMngr {

	/**
	 * Finds all the exported variables whose value must be generated and generates them.
	 * <p>
	 * Generated random values are injected in the model as overridden exports.
	 * </p>
	 * <p>
	 * Expected to be invoked when an instance is added.
	 * </p>
	 *
	 * @param application a non-null application
	 * @param instance a non-null instance
	 */
	void generateRandomValues( Application application, Instance instance );

	/**
	 * Finds all the exported variables whose value must be generated and generates them.
	 * <p>
	 * Generated random values are injected in the model as overridden exports.
	 * </p>
	 * <p>
	 * Expected to be invoked when an application is created.
	 * </p>
	 *
	 * @param application a non-null application
	 * @param instance a non-null instance
	 */
	void generateAllRandomValues( Application application );

	/**
	 * If random values were locked (e.g. port numbers), then release them.
	 * <p>
	 * This method does not delete the overridden exports {@link #generateRandomValues(Application, Instance)}
	 * creates. It is assumed we release random values when we delete a model object. So, we do not care
	 * about updating the model.
	 * </p>
	 * <p>
	 * Expected to be invoked when an instance is deleted from the model.
	 * </p>
	 *
	 * @param application a non-null application
	 * @param instance a non-null instance
	 */
	void releaseRandomValues( Application application, Instance instance );

	/**
	 * If random values were locked (e.g. port numbers), then release them.
	 * <p>
	 * This method does not delete the overridden exports {@link #generateRandomValues(Application, Instance)}
	 * creates. It is assumed we release random values when we delete a model object. So, we do not care
	 * about updating the model.
	 * </p>
	 * <p>
	 * Expected to be invoked when an application is deleted.
	 * </p>
	 *
	 * @param application a non-null application
	 */
	void releaseAllRandomValues( Application application );


	/**
	 * Restores the cached values for a given application.
	 * <p>
	 * This method also guarantees that if a random port is set as an overridden export value,
	 * this value will be preserved and not included in future random selections for this instance.
	 * </p>
	 * <p>
	 * Expected to be invoked when an application is restored.
	 * </p>
	 *
	 * @param application an application
	 */
	void restoreRandomValuesCache( Application application );
}
