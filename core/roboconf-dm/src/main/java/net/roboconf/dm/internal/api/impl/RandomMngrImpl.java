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

package net.roboconf.dm.internal.api.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.ExportedVariable;
import net.roboconf.core.model.beans.ExportedVariable.RandomKind;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.internal.api.IRandomMngr;
import net.roboconf.dm.internal.api.impl.beans.InstanceContext;
import net.roboconf.dm.internal.utils.ConfigurationUtils;
import net.roboconf.dm.management.api.IPreferencesMngr;

/**
 * @author Vincent Zurczak - Linagora
 */
public class RandomMngrImpl implements IRandomMngr {

	/**
	 * A map that associates AGENTS and random ports.
	 * <p>
	 * An agent is identified by an application and a scoped instance.
	 * </p>
	 */
	final Map<InstanceContext,List<Integer>> agentToRandomPorts = new HashMap<> ();

	private final Logger logger = Logger.getLogger( getClass().getName());
	private IPreferencesMngr preferencesMngr;


	/**
	 * @param preferencesMngr the preferencesMngr to set
	 */
	public void setPreferencesMngr( IPreferencesMngr preferencesMngr ) {
		this.preferencesMngr = preferencesMngr;
	}


	@Override
	public synchronized void generateRandomValues( Application application, Instance instance ) {

		// Exported variables that are random will be set a value
		for( ExportedVariable var : instance.getComponent().exportedVariables.values()) {

			// Not random?
			if( ! var.isRandom())
				continue;

			// Port
			if( var.getRandomKind() == RandomKind.PORT ) {
				// Acknowledge: verify a random value was not already set on it.
				// Otherwise, generate a random port and associate it.
				if( ! acknowledgePort( application, instance, var.getName()))
					generateRandomPort( application, instance, var.getName());
			}
		}

		// Save the updated model
		ConfigurationUtils.saveInstances( application );
	}


	@Override
	public void generateAllRandomValues( Application application ) {

		for( Instance instance : InstanceHelpers.getAllInstances( application )) {
			generateRandomValues( application, instance );
		}
	}


	@Override
	public synchronized void releaseRandomValues( Application application, Instance instance ) {

		// Only used for random ports.
		// We only remove the ports used by this instance.
		List<Integer> portsToRelease = new ArrayList<> ();
		for( ExportedVariable var : instance.getComponent().exportedVariables.values()) {
			if( ! var.isRandom()
					|| var.getRandomKind() != RandomKind.PORT )
				continue;

			String value = instance.overriddenExports.get( var.getName());
			if( value != null )
				portsToRelease.add( Integer.parseInt( value ));
		}

		// Update the cache
		InstanceContext ctx = findAgentContext( application, instance );
		List<Integer> ports = this.agentToRandomPorts.get( ctx );
		if( ports != null ) {
			ports.removeAll( portsToRelease );
			if( ports.isEmpty())
				this.agentToRandomPorts.remove( ctx );
		}
	}


	@Override
	public void releaseAllRandomValues( Application application ) {

		// Only used for random ports.
		// Find the contexts to remove
		List<InstanceContext> toRemove = new ArrayList<> ();
		for( InstanceContext ctx : this.agentToRandomPorts.keySet()) {
			if( ctx.getQualifier() == null
					&& Objects.equals( application.getName(), ctx.getName()))
				toRemove.add( ctx );
		}

		// Remove them
		for( InstanceContext ctx : toRemove )
			this.agentToRandomPorts.remove( ctx );
	}


	@Override
	public void restoreRandomValuesCache( Application application ) {

		// Only used for random ports.
		for( Instance instance : InstanceHelpers.getAllInstances( application )) {

			// Restore ALL the variables
			List<ExportedVariable> variablesToRegenerate = new ArrayList<> ();
			for( ExportedVariable var : instance.getComponent().exportedVariables.values()) {

				// We only care about random ports
				if( ! var.isRandom()
						|| var.getRandomKind() != RandomKind.PORT )
					continue;

				// Find the overridden value.
				// If it exists, restore it.
				String value = instance.overriddenExports.get( var.getName());
				if( value != null
						&& ! acknowledgePort( application, instance, var.getName()))
					variablesToRegenerate.add( var );
			}

			// Restoration may have failed for some. Regenerate new ports
			for( ExportedVariable var : variablesToRegenerate ) {
				this.logger.warning( "Generating a new random port for " + var.getName() + " in instance " + instance + " of " + application );
				generateRandomPort( application, instance, var.getName());
			}

			// Save the updated model?
			if( ! variablesToRegenerate.isEmpty())
				ConfigurationUtils.saveInstances( application );
		}
	}


	// Port Management

	public static final int PORT_MIN = 10000;
	public static final int PORT_MAX = 65500;


	/**
	 * Picks up port among the available ones in the allowed range (> 9999).
	 * <p>
	 * Once picked up, it is set as an overridden export on the instance.
	 * This method should be called only when {@link #acknowledgePort(Application, Instance, String)}
	 * returned <code>false</code>.
	 * </p>
	 *
	 * @param application the application
	 * @param instance the instance
	 * @param exportedVariableName the name of the exported variable
	 */
	private void generateRandomPort( Application application, Instance instance, String exportedVariableName ) {
		List<Integer> forbiddenPorts = new ArrayList<> ();

		// Forbidden ports specified in the preferences
		String preferences = this.preferencesMngr.get( IPreferencesMngr.FORBIDDEN_RANDOM_PORTS, "" );
		for( String s : Utils.splitNicely( preferences, "," )) {
			if( Utils.isEmptyOrWhitespaces( s ))
				continue;

			try {
				forbiddenPorts.add( Integer.parseInt( s ));

			} catch( NumberFormatException e ) {
				this.logger.severe( "An invalid port was found in the preferences: " + s );
			}
		}

		// Ports already in use
		InstanceContext ctx = findAgentContext( application, instance );
		List<Integer> portsUsedByAgent = this.agentToRandomPorts.get( ctx );
		if( portsUsedByAgent != null )
			forbiddenPorts.addAll( portsUsedByAgent );

		// Now, pick up a random port
		Integer randomPort = -1;
		for( int i=PORT_MIN; i<PORT_MAX && randomPort == -1; i++ ) {
			if( ! forbiddenPorts.contains( i ))
				randomPort = i;
		}

		// Save it in the cache
		this.logger.fine( "Associating a random port to " + exportedVariableName + " in instance " + instance + " of " + application );
		InstanceContext newCtx = findAgentContext( application, instance );
		List<Integer> associatedPorts = this.agentToRandomPorts.get( newCtx );
		if( associatedPorts == null ) {
			associatedPorts = new ArrayList<> ();
			this.agentToRandomPorts.put( newCtx, associatedPorts );
		}

		associatedPorts.add( randomPort );

		// Inject the variable value in the model.
		// Note: we could edit the graph variable directly.
		// But that would be bad, since several instances of the same component may have different ports. So,
		// we cannot put the information in the component. Instead, we put it as an overridden export.
		instance.overriddenExports.put( exportedVariableName, String.valueOf( randomPort ));
	}


	/**
	 * If the instance already has a value (overridden export) for a random variable, then use it.
	 * <p>
	 * Basically, we do not have to define an overridden export.
	 * We only have to update the cache to not pick up the same port later.
	 * </p>
	 *
	 * @param application the application
	 * @param instance the instance
	 * @param exportedVariableName the name of the exported variable
	 * @return
	 */
	private boolean acknowledgePort( Application application, Instance instance, String exportedVariableName ) {

		boolean acknowledged = false;
		String value = instance.overriddenExports.get( exportedVariableName );
		if( value != null ) {

			// If there is an overridden value, use it
			this.logger.fine( "Acknowledging random port value for " + exportedVariableName + " in instance " + instance + " of " + application );
			Integer portValue = Integer.parseInt( value );
			InstanceContext ctx = findAgentContext( application, instance );

			List<Integer> associatedPorts = this.agentToRandomPorts.get( ctx );
			if( associatedPorts == null ) {
				associatedPorts = new ArrayList<> ();
				this.agentToRandomPorts.put( ctx, associatedPorts );
			}

			// Verify it is not already used.
			// And cache it so that we do not pick it up later.
			if( associatedPorts.contains( portValue )) {
				this.logger.warning( "Random port already used! Failed to acknowledge/restore " + exportedVariableName + " in instance " + instance + " of " + application );
				acknowledged = false;

			} else {
				associatedPorts.add( portValue );
				acknowledged = true;
			}
		}

		return acknowledged;
	}


	// Miscellaneous


	/**
	 * Builds an instance context corresponding to an agent.
	 * @param application an application
	 * @param instance an instance
	 * @return an instance context made up of the application and the right scoped instance
	 */
	private InstanceContext findAgentContext( Application application, Instance instance ) {

		Instance scopedInstance = InstanceHelpers.findScopedInstance( instance );
		return new InstanceContext( application, scopedInstance );
	}
}
