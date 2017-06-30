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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * An application template groups an identifier, graph definitions and instances.
 * @author Vincent Zurczak - Linagora
 */
public class ApplicationTemplate extends AbstractApplication implements Serializable {

	private static final long serialVersionUID = -4753958407033243184L;

	private String version, dslId, externalExportsPrefix;
	private Graphs graphs;

	// We use a list because an application's attributes may be modified.
	// With a set, this could result in unpredictable behaviors.
	private final List<Application> associatedApplications = new ArrayList<> ();

	/**
	 * Tags associated with this template.
	 */
	// Private to guarantee atomic operations on it.
	private final Set<String> tags = new TreeSet<> ();

	/**
	 * External exports.
	 * <p>
	 * Key = internal (graph) variable.<br />
	 * Value = name of the variable, seen from outside.
	 * </p>
	 * <p>
	 * Example: <code>exports: Toto.ip as test</code> and that the application's prefix
	 * is <b>APP</b> will result in an entry whose key is <code>Toto.ip</code>
	 * and whose value is<code>APP.test</code>.
	 * </p>
	 */
	public final Map<String,String> externalExports = new HashMap<> ();



	/**
	 * Constructor.
	 */
	public ApplicationTemplate() {
		// nothing
	}

	/**
	 * Constructor.
	 * @param name
	 */
	public ApplicationTemplate( String name ) {
		setName( name );
	}

	/**
	 * @return the version
	 */
	public String getVersion() {
		return this.version;
	}

	/**
	 * @param version the version to set
	 */
	public void setVersion( String version ) {
		this.version = version;
	}

	/**
	 * @return the dslId
	 */
	public String getDslId() {
		return this.dslId;
	}

	/**
	 * @param dslId the dslId to set
	 */
	public void setDslId( String dslId ) {
		this.dslId = dslId;
	}

	/**
	 * @return the graphs
	 */
	@Override
	public Graphs getGraphs() {
		return this.graphs;
	}

	/**
	 * @param graphs the graphs to set
	 */
	public void setGraphs( Graphs graphs ) {
		this.graphs = graphs;
	}

	/**
	 * @return the externalExportsPrefix
	 */
	public String getExternalExportsPrefix() {
		return this.externalExportsPrefix;
	}

	/**
	 * @param externalExportsPrefix the externalExportsPrefix to set
	 */
	public void setExternalExportsPrefix( String externalExportsPrefix ) {
		this.externalExportsPrefix = externalExportsPrefix;
	}

	@Override
	public boolean equals( Object obj ) {
		return obj instanceof ApplicationTemplate
				&& Objects.equals( this.name, ((ApplicationTemplate) obj ).getName())
				&& Objects.equals( this.version, ((ApplicationTemplate) obj ).getVersion());
	}

	@Override
	public int hashCode() {
		int i1 = this.name == null ? 29 : this.name.hashCode();
		int i2 = this.version == null ? 11 : this.version.hashCode();
		return i1 * i2;
	}

	/**
	 * Sets the name in a chain approach.
	 */
	public ApplicationTemplate name( String name ) {
		this.name = name;
		return this;
	}

	/**
	 * Sets the version in a chain approach.
	 */
	public ApplicationTemplate version( String version ) {
		this.version = version;
		return this;
	}

	/**
	 * Sets the description in a chain approach.
	 */
	public ApplicationTemplate description( String description ) {
		this.description = description;
		return this;
	}

	/**
	 * Sets the DSL ID in a chain approach.
	 */
	public ApplicationTemplate dslId( String dslId ) {
		this.dslId = dslId;
		return this;
	}

	/**
	 * Sets the graphs in a chain approach.
	 */
	public ApplicationTemplate graphs( Graphs graphs ) {
		this.graphs = graphs;
		return this;
	}

	/**
	 * Sets the directory in a chain approach.
	 */
	public ApplicationTemplate directory( File directory ) {
		this.directory = directory;
		return this;
	}

	/**
	 * @return the associated applications
	 */
	public List<Application> getAssociatedApplications() {
		synchronized( this.associatedApplications ) {
			return Collections.unmodifiableList( this.associatedApplications );
		}
	}

	/**
	 * Associates an application with this template.
	 * @param app an application (not null)
	 */
	void associateApplication( Application app ) {
		synchronized( this.associatedApplications ) {
			this.associatedApplications.add( app );
		}
	}

	/**
	 * Deletes the association between an application and this template.
	 * @param app an application (not null)
	 */
	void removeApplicationAssocation( Application app ) {
		synchronized( this.associatedApplications ) {
			this.associatedApplications.remove( app );
		}
	}

	/**
	 * Adds a tag.
	 * @param tag
	 */
	public void addTag( String tag ) {
		synchronized( this.tags ) {
			this.tags.add( tag );
		}
	}

	/**
	 * Replaces all the tags.
	 * @param tags a (potentially null) collection of tags
	 */
	public void setTags( Collection<String> tags ) {

		synchronized( this.tags ) {
			this.tags.clear();
			if( tags != null )
				this.tags.addAll( tags );
		}
	}

	/**
	 * @return the tags
	 */
	public Set<String> getTags() {
		return Collections.unmodifiableSet( this.tags );
	}
}
