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

package net.roboconf.agent;

/**
 * @author Vincent Zurczak - Linagora
 */
public interface IAgent {

	/**
	 * Starts listening and processing messages.
	 * <p>
	 * If the agent was already started, this method does nothing.
	 * </p>
	 */
	void start();

	/**
	 * Stops listening and processing messages.
	 * <p>
	 * This method does not interrupt the current message processing.
	 * But it will not process any further message, unless the start method
	 * is called after.
	 * </p>
	 * <p>
	 * If the agent is already stopped, this method does nothing.
	 * </p>
	 */
	void stop();
}
