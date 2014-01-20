/**
 * Copyright 2013-2014 Linagora, Université Joseph Fourier
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

package net.roboconf.core.model.runtime.impl;

import java.util.Collection;
import java.util.HashSet;

import net.roboconf.core.model.runtime.Application;
import net.roboconf.core.model.runtime.Graphs;
import net.roboconf.core.model.runtime.Instance;

/**
 * A basic implementation of the {@link Application} interface.
 * @author Vincent Zurczak - Linagora
 */
public class ApplicationImpl implements Application {

	private String name, version;
	private Graphs graphs;
	private final Collection<Instance> rootInstances = new HashSet<Instance> ();


	/**
	 * @return the name
	 */
	@Override
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
	 * @return the version
	 */
	@Override
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
	 * @return the rootInstances
	 */
	@Override
	public Collection<Instance> getRootInstances() {
		return this.rootInstances;
	}
}
