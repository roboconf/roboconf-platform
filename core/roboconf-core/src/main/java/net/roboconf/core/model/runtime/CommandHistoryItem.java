/**
 * Copyright 2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.core.model.runtime;

/**
 * @author Vincent Zurczak - Linagora
 */
public class CommandHistoryItem {

	private final String applicationName, commandName, origin, executionResult;
	private final long start, duration;


	/**
	 * Constructor.
	 * @param applicationName
	 * @param commandName
	 * @param origin
	 * @param executionResult
	 * @param start (in nanoseconds)
	 * @param duration (in nanoseconds)
	 */
	public CommandHistoryItem(
			String applicationName,
			String commandName,
			String origin,
			String executionResult,
			long start,
			long duration ) {

		this.applicationName = applicationName;
		this.commandName = commandName;
		this.origin = origin;
		this.executionResult = executionResult;
		this.start = start;
		this.duration = duration;
	}

	public String getApplicationName() {
		return this.applicationName;
	}

	public String getCommandName() {
		return this.commandName;
	}

	public String getOrigin() {
		return this.origin;
	}

	public String getExecutionResult() {
		return this.executionResult;
	}

	public long getStart() {
		return this.start;
	}

	public long getDuration() {
		return this.duration;
	}
}
