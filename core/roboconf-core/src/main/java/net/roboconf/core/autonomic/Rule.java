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

package net.roboconf.core.autonomic;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Vincent Zurczak - Linagora
 */
public class Rule {

	public static final int NO_TIMING_WINDOW = -1;
	private final List<String> commandsToInvoke = new ArrayList<String> ();

	// FIXME: this event will be replaced later by a combination
	// of events (with boolean expressions).
	private String eventName;
	private String ruleName;

	private int timingWindow = NO_TIMING_WINDOW;
	private int delayBetweenSucceedingInvocations = 0;


	/**
	 * @return the rule name
	 */
	public String getRuleName() {
		return this.ruleName;
	}

	/**
	 * @return the event name
	 */
	public String getEventName() {
		return this.eventName;
	}

	/**
	 * @return the delay between two invocations within the same application (in seconds)
	 */
	public int getDelayBetweenSucceedingInvocations() {
		return this.delayBetweenSucceedingInvocations;
	}

	/**
	 * The period (in seconds) during which we consider the event as valid.
	 * <p>
	 * {@link #NO_TIMING_WINDOW} to indicate there is no timing window.
	 * </p>
	 * @return the timing window
	 */
	public int getTimingWindow() {
		return this.timingWindow;
	}

	/**
	 * @return a non-null list of (ordered) commands to invoke
	 */
	public List<String> getCommandsToInvoke() {
		return this.commandsToInvoke;
	}

	/**
	 * @param timingWindow the timingWindow to set
	 */
	public void setTimingWindow( int timingWindow ) {
		this.timingWindow = timingWindow;
	}

	/**
	 * @param delayBetweenSucceedingInvocations the delayBetweenSucceedingInvocations to set
	 */
	public void setDelayBetweenSucceedingInvocations( int delayBetweenSucceedingInvocations ) {
		this.delayBetweenSucceedingInvocations = delayBetweenSucceedingInvocations;
	}

	/**
	 * @param eventName the eventName to set
	 */
	public void setEventName( String eventName ) {
		this.eventName = eventName;
	}

	/**
	 * @param ruleName the ruleName to set
	 */
	public void setRuleName( String ruleName ) {
		this.ruleName = ruleName;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return this.ruleName;
	}
}
