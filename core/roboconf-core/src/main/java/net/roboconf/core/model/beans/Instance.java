/**
 * Copyright 2013-2017 Linagora, Université Joseph Fourier, Floralis
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArraySet;

import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.utils.RoboconfFlexMap;

/**
 * An instance object represents a running component instance.
 * @author Vincent Zurczak - Linagora
 */
public class Instance implements Serializable {

	/**
	 * A constant to store the IP address in {@link #data}.
	 * <p>Storing this information in a scoped instance is enough.</p>
	 */
	public static final String IP_ADDRESS = "ip.address";

	/**
	 * A constant to store the machine ID in {@link #data}.
	 * <p>Storing this information in a scoped instance is enough.</p>
	 */
	public static final String MACHINE_ID = "machine.id";

	/**
	 * A constant to store in {@link #data} and that indicates a scoped instance was "taken" by a target handler.
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
	 * <p>Storing this information in a scoped instance is enough.</p>
	 */
	public static final String APPLICATION_NAME = "application.name";

	/**
	 * A constant to store the last problem in {@link #data}.
	 * <p>Storing this information in a scoped instance is enough.</p>
	 */
	public static final String LAST_PROBLEM = "last.problem";

	/**
	 * A constant to indicate a scoped instance is ready for post-configuration.
	 * <p>
	 * To set only once a target handler has (successfully) completed its configuration process.
	 * </p>
	 * <p>
	 * Storing this information in a scoped instance is enough.
	 * </p>
	 */
	public static final String READY_FOR_CFG_MARKER = "ready.for.local.script.configuration";

	/**
	 * A constant to store the timestamp of the first heart beat received for this instance.
	 * <p>
	 * To be stored in {@link #data}.
	 * Storing this information in a scoped instance is enough.
	 * </p>
	 * <p>
	 * If an instance goes into the PROBLEM state, and that a new heart beat comes after, then
	 * we should overwrite this information.
	 * </p>
	 */
	public static final String RUNNING_FROM = "running.from";

	/**
	 * A constant to indicate this instance should be removed from the model once it is not deployed.
	 * <p>
	 * This constant is used for batch instructions where we want to delete instance eventually.
	 * A batch could indicate to undeploy and instance and then delete it. Since the undeployment may
	 * take some time, the deletion would fail if we did not have an asynchronous solution to delete
	 * an instance. We do it by adding this marker on the instance.
	 * </p>
	 */
	public static final String DELETE_WHEN_NOT_DEPLOYED = "delete.when.not.deployed";


	private static final long serialVersionUID = -3320865356277185064L;

	private String name;
	private Component component;
	private Instance parent;
	private final Collection<Instance> children = new CopyOnWriteArraySet<> ();
	private InstanceStatus status = InstanceStatus.NOT_DEPLOYED;

	public final Collection<String> channels = new HashSet<> ();
	public final Map<String,String> overriddenExports = new HashMap<> ();

	// Data can be accessed through several threads and for various reasons.
	// ConcurrentHashMap does not accept null values. So, we use our own wrapper.
	public final Map<String,String> data = new RoboconfFlexMap<>( "@!xyz!@" );

	// At runtime, imported variables are grouped by prefix.
	// The prefix is a component or a facet name.
	private final Map<String,Collection<Import>> variablePrefixToImports = new TreeMap<> ();


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
		WAITING_FOR_ANCESTOR( true ),
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
			InstanceStatus result = exactStatus( s );
			return result == null ? NOT_DEPLOYED : result;
		}


		/**
		 * A secured alternative to {@link InstanceStatus#valueOf(String)}.
		 * @param s a string (can be null)
		 * @return the associated runtime status, or null otherwise
		 */
		public static InstanceStatus exactStatus( String s ) {

			InstanceStatus result = null;
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
