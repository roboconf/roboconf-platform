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

import java.io.File;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.roboconf.core.model.helpers.InstanceHelpers;

/**
 * An application groups an identifier, graph definitions and instances.
 * @author Vincent Zurczak - Linagora
 */
public class Application extends AbstractApplication implements Serializable {

	private static final long serialVersionUID = -4753958407033243184L;

	private final ApplicationTemplate template;
	private final Map<String,Set<String>> applicationBindings = new ConcurrentHashMap<> ();


	/**
	 * Constructor.
	 * @param template
	 */
	public Application( ApplicationTemplate template ) {
		this.template = template;

		// We must duplicate all the instances
		if( template != null ) {
			template.associateApplication( this );
			for( Instance rootInstance : template.getRootInstances())
				getRootInstances().add( InstanceHelpers.replicateInstance( rootInstance ));
		}
	}

	/**
	 * Constructor.
	 * @param name
	 * @param template
	 */
	public Application( String name, ApplicationTemplate template ) {
		this( template );
		setName( name );
	}

	/**
	 * @return the template
	 */
	public ApplicationTemplate getTemplate() {
		return this.template;
	}

	@Override
	public Graphs getGraphs() {
		return this.template == null ? null : this.template.getGraphs();
	}

	/**
	 * A shortcut method to access the template's external exports mapping.
	 * @return a non-null map
	 */
	public Map<String,String> getExternalExports() {
		return this.template != null ? this.template.externalExports : new HashMap<String,String>( 0 );
	}

	@Override
	public boolean equals( Object obj ) {
		return obj instanceof Application
				&& Objects.equals( this.name, ((Application) obj ).getName());
	}

	@Override
	public int hashCode() {
		return this.name == null ? 29 : this.name.hashCode();
	}

	/**
	 * Sets the name in a chain approach.
	 */
	public Application name( String name ) {
		this.name = name;
		return this;
	}

	/**
	 * Sets the description in a chain approach.
	 */
	public Application description( String description ) {
		this.description = description;
		return this;
	}

	/**
	 * Sets the directory in a chain approach.
	 */
	public Application directory( File directory ) {
		this.directory = directory;
		return this;
	}

	/**
	 * Removes the association between this application and its template.
	 */
	public void removeAssociationWithTemplate() {
		if( this.template != null )
			this.template.removeApplicationAssocation( this );
	}


	/**
	 * Binds an external export prefix with an application name.
	 * <p>
	 * No error is thrown if the bound already existed.
	 * </p>
	 *
	 * @param externalExportPrefix an external export prefix (not null)
	 * @param applicationName an application name (not null)
	 */
	public void bindWithApplication( String externalExportPrefix, String applicationName ) {

		Set<String> bounds = this.applicationBindings.get( externalExportPrefix );
		if( bounds == null ) {
			bounds = new LinkedHashSet<> ();
			this.applicationBindings.put( externalExportPrefix, bounds );
		}

		bounds.add( applicationName );
	}


	/**
	 * Unbinds an external export prefix from an application name.
	 * <p>
	 * No error is thrown if the bound did not exist.
	 * </p>
	 *
	 * @param externalExportPrefix an external export prefix (not null)
	 * @param applicationName an application name (not null)
	 * @return true if bindings were modified, false if no binding existed
	 */
	public boolean unbindFromApplication( String externalExportPrefix, String applicationName ) {

		boolean result = false;
		Set<String> bounds = this.applicationBindings.get( externalExportPrefix );
		if( bounds != null ) {
			result = bounds.remove( applicationName );
			if( bounds.isEmpty())
				this.applicationBindings.remove( externalExportPrefix );
		}

		return result;
	}


	/**
	 * Replaces application bindings for a given prefix.
	 * @param externalExportPrefix an external export prefix (not null)
	 * @param applicationNames a non-null set of application names
	 * @return true if bindings were modified, false otherwise
	 */
	public boolean replaceApplicationBindings( String externalExportPrefix, Set<String> applicationNames ) {

		// There is a change if the set do not have the same size or if they do not contain the same
		// number of element. If no binding had been registered previously, then we only check whether
		// the new set contains something.
		boolean changed = false;
		Set<String> oldApplicationNames = this.applicationBindings.remove( externalExportPrefix );
		if( oldApplicationNames == null ) {
			changed = ! applicationNames.isEmpty();

		} else if( oldApplicationNames.size() != applicationNames.size()) {
			changed = true;

		} else {
			oldApplicationNames.removeAll( applicationNames );
			changed = ! oldApplicationNames.isEmpty();
		}

		// Do not register keys when there is no binding
		if( ! applicationNames.isEmpty())
			this.applicationBindings.put( externalExportPrefix, applicationNames );

		return changed;
	}


	/**
	 * @return the applicationBindings
	 */
	public Map<String,Set<String>> getApplicationBindings() {
		return Collections.unmodifiableMap( this.applicationBindings );
	}
}
