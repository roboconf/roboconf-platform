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

package net.roboconf.dm.internal.api.impl.beans;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import net.roboconf.core.autonomic.Rule;
import net.roboconf.core.model.beans.Application;

/**
 * An autonomic context related to a given application.
 * @author Vincent Zurczak - Linagora
 */
public class AutonomicApplicationContext {

	public final Map<String,Rule> ruleNameToRule = new ConcurrentHashMap<> ();

	// eventNameToLastRecordTime => time is in nanoseconds.
	// ruleNameToLastExecution => time is in nanoseconds.
	// ruleNameToLastTrigger values include a time stamp which is in nanoseconds.
	final Map<String,Long> eventNameToLastRecordTime = new HashMap<> ();
	final Map<String,Long> ruleNameToLastExecution = new HashMap<> ();
	final Map<String,String> ruleNameToLastTrigger = new HashMap<> ();
	final AtomicInteger vmCount = new AtomicInteger( 0 );

	private final Logger logger = Logger.getLogger( getClass().getName());
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
	 * @return number of created VM for this application / context (autonomic)
	 */
	public AtomicInteger getVmCount() {
		return this.vmCount;
	}


	/**
	 * Registers an event and its time of registration (in nanoseconds).
	 * @param eventName the vent to register
	 */
	public void registerEvent( String eventName ) {
		this.eventNameToLastRecordTime.put( eventName, System.nanoTime());
	}


	/**
	 * Records the date of the execution for a given rule.
	 * @param ruleName the rule name
	 */
	public void recordPreExecution( String ruleName ) {
		this.ruleNameToLastExecution.put( ruleName, System.nanoTime());
	}


	/**
	 * Finds the rules to execute after an event was recorded.
	 * @return a non-null list of rules
	 */
	public List<Rule> findRulesToExecute() {

		this.logger.fine( "Looking for rules to execute after an event was recorded for application " + this.app );
		List<Rule> result = new ArrayList<> ();
		long now = System.nanoTime();

		/*
		 * For all the rules, find if there are events that should trigger its execution.
		 */
		for( Rule rule : this.ruleNameToRule.values()) {

			/*
			 * A rule can be added if...
			 * 1 - If its last execution occurred more than "the rule's delay" ago.
			 * 2 - This event did not already trigger the execution of the rule.
			 * 3 - If this rule has no timing window OR if the event occurred within the timing window.
			 */

			// Check the condition "1", about the last execution.
			long validPeriodStart = now - TimeUnit.SECONDS.toNanos( rule.getDelayBetweenSucceedingInvocations());
			Long lastExecutionTime = this.ruleNameToLastExecution.get( rule.getRuleName());
			if( lastExecutionTime != null
					&& lastExecutionTime - validPeriodStart > 0 ) {

				this.logger.finer( "Ignoring the rule " + rule.getRuleName() + " since the execution delay has not yet expired." );
				continue;
			}

			// Check the condition "2", or said differently, did this event record already trigger
			// the execution of this rule?

			// No record? Then the rule cannot be triggered.
			Long lastRecord = this.eventNameToLastRecordTime.get( rule.getEventName());
			if( lastRecord == null )
				continue;

			// FIXME: for the moment, a rule is activated by a single event.
			// But later, it will be boolean combination of events. So, keep
			// the string builder to generate it.
			StringBuilder sbTrigger = new StringBuilder();
			sbTrigger.append( rule.getEventName());
			sbTrigger.append( lastRecord );

			String lastTrigger = this.ruleNameToLastTrigger.get( rule.getRuleName());
			if( Objects.equals( lastTrigger, sbTrigger.toString())) {
				this.logger.finer( "Ignoring the rule " + rule.getRuleName() + " since no new event occurred since its last execution." );
				continue;
			}

			// Check the condition "3", the one that prevents a recent event from "spamming".
			// Too old events may not be relevant. This is why rules can define a timing window.
			// FIXME: with one event as a trigger, it does not really make sense.
			// But with a combination of events, it will.

			// Either there is no timing window...
			// ... or the last record occurred between 'now' and 'now - timing window'.
			validPeriodStart = now - TimeUnit.SECONDS.toNanos( rule.getTimingWindow());
			if( rule.getTimingWindow() != Rule.NO_TIMING_WINDOW
					&& lastRecord - validPeriodStart < 0 ) {

				this.logger.finer( "Ignoring the rule " + rule.getRuleName() + " since no new event occurred since its last execution." );
				continue;
			}

			this.logger.finer( "Rule " + rule.getRuleName() + " was found following the occurrence of the " + rule.getEventName() + " event." );
			this.ruleNameToLastTrigger.put( rule.getRuleName(), sbTrigger.toString());
			result.add( rule );


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
		}

		return result;
	}
}
