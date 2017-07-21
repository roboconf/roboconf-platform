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

	/**
	 * Code indicating a command execution was successful.
	 */
	public static final int EXECUTION_OK = 1;

	/**
	 * Code indicating a command execution was successful but that instructions were skipped.
	 */
	public static final int EXECUTION_OK_WITH_SKIPPED = 2;

	/**
	 * Code indicating a command execution failed.
	 */
	public static final int EXECUTION_ERROR = 3;

	/**
	 * Code indicating a command was executed by the scheduler.
	 */
	public static final int ORIGIN_SCHEDULER = 1;

	/**
	 * Code indicating a command was executed from the REST API.
	 */
	public static final int ORIGIN_REST_API = 2;

	/**
	 * Code indicating a command was executed by the autonomic.
	 */
	public static final int ORIGIN_AUTONOMIC = 3;

	/**
	 * Code indicating a command was executed from another command.
	 */
	public static final int ORIGIN_OTHER_COMMAND = 4;

	private final String applicationName, commandName, originDetails;
	private final int origin, executionResult;
	private final long start, duration;


	/**
	 * Constructor.
	 * @param applicationName the application name
	 * @param commandName the command name
	 * @param origin one of {@link #ORIGIN_AUTONOMIC}, {@link #ORIGIN_REST_API} or {@link #ORIGIN_SCHEDULER}
	 * @param originDetails a string indicating the origin details (e.g. job name)
	 * @param executionResult one of {@link #EXECUTION_OK}, {@link #EXECUTION_OK_WITH_SKIPPED} or {@link #EXECUTION_ERROR}
	 * @param start (in milliseconds)
	 * @param duration (in nanoseconds)
	 */
	public CommandHistoryItem(
			String applicationName,
			String commandName,
			int origin,
			String originDetails,
			int executionResult,
			long start,
			long duration ) {

		this.applicationName = applicationName;
		this.commandName = commandName;
		this.origin = origin;
		this.originDetails = originDetails;
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

	public long getStart() {
		return this.start;
	}

	public long getDuration() {
		return this.duration;
	}

	public String getOriginDetails() {
		return this.originDetails;
	}

	public int getOrigin() {
		return this.origin;
	}

	public int getExecutionResult() {
		return this.executionResult;
	}
}
