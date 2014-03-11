/**
 * Copyright 2014 Linagora, Université Joseph Fourier
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

package net.roboconf.testing;

import net.roboconf.core.model.runtime.Component;
import net.roboconf.dm.management.ManagedApplication;

/**
 * @author Vincent Zurczak - Linagora
 */
public final class ApplicationTester {

	public static final String IN_MEMORY_AGENT = "in-memory-agent";


	/**
	 * Replaces IaaS components by in-memory ones.
	 * @param ma a managed application
	 */
	public static void patch( ManagedApplication ma ) {

		for( Component c : ma.getApplication().getGraphs().getRootComponents()) {
			if( "iaas".equalsIgnoreCase( c.getInstallerName()))
				c.setInstallerName( IN_MEMORY_AGENT );
		}
	}
}
