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

package net.roboconf.core.actions;

/**
 * Actions to apply on instances.
 * <ul>
 * <li>deploy: ask an agent to deploy the instance files.</li>
 * <li>undeploy: ask the agent to undeploy the instance files.</li>
 * <li>start: start the instance.</li>
 * <li>stop: stop the instance.</li>
 * <li>remove: remove the instance from the model.</li>
 * </ul>
 * <p>
 * Model additions are performed through different means.
 * </p>
 *
 * @author Vincent Zurczak - Linagora
 */
public enum ApplicationAction {
	deploy, undeploy, start, stop, remove;

	public static ApplicationAction whichAction( String s ) {

		ApplicationAction result = null;
		for( ApplicationAction action : ApplicationAction.values()) {
			if( action.toString().equalsIgnoreCase( s )) {
				result = action;
				break;
			}
		}

		return result;
	}
}
