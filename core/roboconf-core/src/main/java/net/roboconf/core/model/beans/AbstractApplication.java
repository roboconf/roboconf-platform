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

package net.roboconf.core.model.beans;

import java.io.File;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArraySet;

import net.roboconf.core.utils.Utils;

/**
 * @author Vincent Zurczak - Linagora
 */
public abstract class AbstractApplication {

	protected final Collection<Instance> rootInstances = new CopyOnWriteArraySet<> ();
	protected String name, displayName, description;
	protected File directory;


	/**
	 * @return the root instances
	 */
	public Collection<Instance> getRootInstances() {
		return this.rootInstances;
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
	public final void setName( String name ) {

		// "display name" can contain accents.
		// "name" cannot, we replace them by their equivalent without accent.
		if( name == null ) {
			this.name = null;
			this.displayName = null;

		} else if( Utils.isEmptyOrWhitespaces( name )) {
			this.displayName = name.trim();
			this.name = name.trim();

		} else {
			this.displayName = name.trim();
			this.name = Utils.cleanNameWithAccents( name );
		}
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return this.description;
	}

	/**
	 * @param description the description to set
	 */
	public void setDescription( String description ) {
		this.description = description;
	}

	/**
	 * @return the directory
	 */
	public File getDirectory() {
		return this.directory;
	}

	/**
	 * @param directory the directory to set
	 */
	public void setDirectory( File directory ) {
		this.directory = directory;
	}

	/**
	 * @return the graph
	 */
	public abstract Graphs getGraphs();


	/**
	 * @return the displayName
	 */
	public String getDisplayName() {
		return this.displayName;
	}

	@Override
	public String toString() {
		return this.name;
	}
}
