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

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import net.roboconf.core.model.helpers.InstanceHelpers;

/**
 * An application groups an identifier, graph definitions and instances.
 * @author Vincent Zurczak - Linagora
 */
public class Application extends AbstractApplication implements Serializable {

	private static final long serialVersionUID = -4753958407033243184L;

	private final ApplicationTemplate template;
	public final Map<String,String> applicationBindings = new HashMap<String,String> ();


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
		this.name = name;
	}

	/**
	 * @return the template
	 */
	public ApplicationTemplate getTemplate() {
		return this.template;
	}

	/**
	 * A shortcut method to access the template's external exports mapping.
	 * @return a non-null map
	 */
	public Map<String,String> getExternalExports() {
		return this.template != null ? this.template.externalExports : null;
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
}
