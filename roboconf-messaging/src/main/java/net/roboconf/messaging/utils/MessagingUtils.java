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

package net.roboconf.messaging.utils;

/**
 * @author Vincent Zurczak - Linagora
 */
public final class MessagingUtils {

	/**
	 * Empty private constructor.
	 */
	private MessagingUtils() {
		// nothing
	}


	/**
	 * Builds the queue name for a machine (= root instance).
	 * @param applicationName the application name
	 * @param rootInstanceName the name of the root instance (or machine, this is the same)
	 * @return a non-null string (the queue name)
	 */
	public static String buildMachineQueueName( String applicationName, String rootInstanceName ) {
		return applicationName + ".machine." + rootInstanceName;
	}


	/**
	 * Builds the DM exchange name for an application.
	 * @param applicationName the application name
	 * @return a non-null string (the queue name)
	 */
	public static String buildDmExchangeName( String applicationName ) {
		return applicationName + ".dm";
	}
}
