/**
 * Copyright 2013-2015 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.core.model.beans;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.TreeMap;

import net.roboconf.core.model.helpers.InstanceHelpers;

/**
 * An instance object represents a running component instance.
 * @author Vincent Zurczak - Linagora
 */
public class Instance implements Serializable {

	/**
	 * A constant to store the IP address in {@link #data}.
	 * <p>Storing this information in a root instance is enough.</p>
	 */
	public static final String IP_ADDRESS = "ip.address";

	/**
	 * A constant to store the machine ID in {@link #data}.
	 * <p>Storing this information in a root instance is enough.</p>
	 */
	public static final String MACHINE_ID = "machine.id";

	/**
	 * A constant to store in {@link #data} and that indicates a root instance was "taken" by a target handler.
	 * <p>Storing this information in a scoped instance is enough.</p>
	 * <p>
	 * When a scoped instance is still NOT_DEPLOYED but that this key is present
	 * in {@link #data}, it means the creation of a machine was acknowledged by the
	 * DM but that the processing has not yet started for this instance.
	 * </p>
	 * <p>
	 * Such situations could occur when a user starts a lot of scoped instances
	 * at once.
	 * </p>
	 */
	public static final String TARGET_ACQUIRED = "target.acquired";

	/**
	 * A constant to store the application name in {@link #data}.
	 * <p>Storing this information in a root instance is enough.</p>
	 */
	public static final String APPLICATION_NAME = "application.name";


	private static final long serialVersionUID = -3320865356277185064L;

	private String name;
	private Component component;
	private Instance parent;
	private final Collection<Instance> children = new LinkedHashSet<Instance> ();
	private InstanceStatus status = InstanceStatus.NOT_DEPLOYED;

	public final Collection<String> channels = new HashSet<String> ();
	public final Map<String,String> overriddenExports = new HashMap<String,String> ();

	// Data can be accessed through several threads and for various reasons.
	// ConcurrentHashMap does not accept null values. We could wrap such a map
	// in a method or in a sub-class to prevent these NPE, but a synchronized map
	// should be enough and should prevent unpredictable reactions.
	public final Map<String,String> data = Collections.synchronizedMap( new LinkedHashMap<String,String>( 0 ));

	// At runtime, imported variables are grouped by prefix.
	// The prefix is a component or a facet name.
	private final Map<String,Collection<Import>> variablePrefixToImports = new TreeMap<String,Collection<Import>> ();


	/**
	 * Constructor.
	 */
	public Instance() {
		// nothing
	}

	/**
	 * Constructor.
	 * @param name the instance name
	 */
	public Instance( String name ) {
		this.name = name;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName( String name ) {
		this.name = name;
	}

	/**
	 * @return the status
	 */
	public synchronized InstanceStatus getStatus() {
		return this.status;
	}

	/**
	 * @param status the status to set
	 */
	public synchronized void setStatus( InstanceStatus status ) {
		this.status = status;
	}

	/**
	 * @return the component
	 */
	public Component getComponent() {
		return this.component;
	}

	/**
	 * @param component the component to set
	 */
	public void setComponent( Component component ) {
		this.component = component;
	}

	/**
	 * @return the parent
	 */
	public Instance getParent() {
		return this.parent;
	}

	/**
	 * @param parent the parent to set
	 */
	public void setParent( Instance parent ) {
		this.parent = parent;
	}

	/**
	 * @return the children
	 */
	public Collection<Instance> getChildren() {
		return this.children;
	}

	@Override
	public int hashCode() {
		return InstanceHelpers.computeInstancePath( this ).hashCode();
	}

	@Override
	public boolean equals( Object obj ) {
		return obj instanceof Instance
				&& InstanceHelpers.haveSamePath( this, (Instance) obj);
	}

	@Override
	public String toString() {
		return this.name;
	}


	/**
	 * Sets the name in a chain approach.
	 */
	public Instance name( String name ) {
		this.name = name;
		return this;
	}


	/**
	 * Sets the component in a chain approach.
	 */
	public Instance component( Component component ) {
		this.component = component;
		return this;
	}


	/**
	 * Sets the parent in a chain approach.
	 */
	public Instance parent( Instance parent ) {
		this.parent = parent;
		return this;
	}


	/**
	 * Adds a channel in a chain approach.
	 */
	public Instance channel( String channel ) {
		this.channels.add( channel );
		return this;
	}


	/**
	 * Sets the status in a chain approach.
	 */
	public synchronized Instance status( InstanceStatus status ) {
		this.status = status;
		return this;
	}


	/**
	 * @return the imports (not null, key: component or facet name, value: the associated imports)
	 */
	public Map<String,Collection<Import>> getImports() {
		return this.variablePrefixToImports;
	}


	/**
	 * @author Noël - LIG
	 */
	public enum InstanceStatus implements Serializable {
		NOT_DEPLOYED( true ),
		DEPLOYING( false ),
		DEPLOYED_STOPPED( true ),
		UNRESOLVED( true ),
		STARTING( false ),
		DEPLOYED_STARTED( true ),
		STOPPING( false ),
		UNDEPLOYING( false ),
		PROBLEM( false );


		private final boolean stable;

		/**
		 * Constructor.
		 * @param stable
		 */
		InstanceStatus( boolean stable ) {
			this.stable = stable;
		}


		/**
		 * A secured alternative to {@link InstanceStatus#valueOf(String)}.
		 * @param s a string (can be null)
		 * @return the associated runtime status, or {@link InstanceStatus#NOT_DEPLOYED} otherwise
		 */
		public static InstanceStatus whichStatus( String s ) {

			InstanceStatus result = InstanceStatus.NOT_DEPLOYED;
			for( InstanceStatus status : InstanceStatus.values()) {
				if( status.toString().equalsIgnoreCase( s )) {
					result = status;
					break;
				}
			}

			return result;
		}


		/**
		 * A secured way to determine whether a string designates an existing status.
		 * @param s a string (can be null)
		 * @return true if it is a state name, false otheriwse
		 */
		public static boolean isValidState( String s ) {

			boolean valid = false;
			for( InstanceStatus status : InstanceStatus.values()) {
				if( status.toString().equalsIgnoreCase( s )) {
					valid = true;
					break;
				}
			}

			return valid;
		}


		/**
		 * @return the stable
		 */
		public boolean isStable() {
			return this.stable;
		}
	}
}
