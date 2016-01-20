/**
 * Copyright 2016 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.dm.internal.api.impl.beans;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import net.roboconf.core.autonomic.Rule;
import net.roboconf.core.model.beans.Application;

/**
 * An autonomic context related to a given application.
 * @author Vincent Zurczak - Linagora
 */
public class AutonomicApplicationContext {

	private final Logger logger = Logger.getLogger( getClass().getName());
	public final Map<String,Long> eventNameToLastRecordTime = new HashMap<> ();
	private final Map<String,Rule> ruleNameToRule = new ConcurrentHashMap<> ();
	private final Map<String,Long> ruleNameToLastExecution = new HashMap<> ();

	private final AtomicInteger vmCount = new AtomicInteger( 0 );
	private final Application app;


	/**
	 * Constructor.
	 * @param app
	 */
	public AutonomicApplicationContext( Application app ) {
		this.app = app;
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return this.app.toString();
	}


	/**
	 * @return number of created VM for this applications / context (autonomic)
	 */
	public int getVmCount() {
		return this.vmCount.get();
	}


	/**
	 * Records the date of the execution for a given rule.
	 * @param ruleName the rule name
	 */
	public void recordPreExecution( String ruleName ) {
		this.ruleNameToLastExecution.put( ruleName, new Date().getTime());
	}


	/**
	 * Finds the rules to execute after an event was recorded.
	 * @return a non-null list of rules
	 */
	public List<Rule> findRulesToExecute() {

		this.logger.fine( "Looking for rules to execute after an event was recorded for application " + this.app );
		List<Rule> result = new ArrayList<> ();
		long now = new Date().getTime();
		for( Rule rule : this.ruleNameToRule.values()) {

			// Could we execute it?
			Long lastExecutionTime = this.ruleNameToLastExecution.get( rule.getRuleName());
			if( lastExecutionTime != null && (now - lastExecutionTime) < (rule.getDelayBetweenSucceedingInvocations() * 1000)) {
				this.logger.finer( "Ignoring the rule " + rule.getRuleName() + " since the execution delay has not yet expired." );
				continue;
			}

			// Other checks?
			/*
			 * Long story...
			 * Formerly, there were no commands mechanism. Reactions to events
			 * were more simple and atomic (one reaction => one simple command, not several scripted ones).
			 *
			 * But, at this time, permissions were more strictly controlled.
			 * Beyond execution time, we were also checking the last execution had completed correctly.
			 * As an example, if the reaction to an event was "replicate this vm", we were checking that
			 * the new VM and all its children had been deployed and started before executing it again.
			 * There were some strong hypothesis behind. In fact, in real use cases, we cannot perform such verifications.
			 *
			 * Indeed, it is possible to determine from a command file what should be the name and the state
			 * of application instances when the execution completes. We would then be able to compare the real
			 * states with the foreseen ones. However, many things can prevent the real states from reaching or
			 * remaining in the expected ones. As an example, a user may manually modify a state with the REST API.
			 * Other commands, triggered by other events, may modify states and give contradictory instructions.
			 *
			 * In such a situation, the last execution would never been considered as completed.
			 * This method would then prevent an event from being processed anymore. And the autonomic would become
			 * useless. As a reminder, messages are processed one after the other in both the DM and agents. But even
			 * if (autonomic) messages are not processed concurrently, such contradictory situations may occur. Trying to
			 * cover such complex verifications would certainly fail and raise more bugs and problems than benefits.
			 *
			 * The only reasonable assumption that can be made is a safety delay between two succeeding
			 * processing for a same event. This also has the advantage of being simple to understand and simple to predict.
			 * If we had to provide something to help users detecting contradictions in the autonomic, it would be better
			 * to have an analysis tool rather than finding workarounds for them at runtime.
			 */
			// So, no other check related to a previous execution.

			// Are there matching events?
			long interestingPeriod = now - rule.getTimingWindow() * 1000;
			Long lastRecord = this.eventNameToLastRecordTime.get( rule.getEventName());
			if( lastRecord != null && lastRecord >= interestingPeriod )
				result.add( rule );
		}

		return result;
	}
}
