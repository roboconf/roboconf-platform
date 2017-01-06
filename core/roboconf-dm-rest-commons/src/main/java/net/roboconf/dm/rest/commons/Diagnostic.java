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

package net.roboconf.dm.rest.commons;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Vincent Zurczak - Linagora
 */
public class Diagnostic {

	private String instancePath;
	private final List<DependencyInformation> dependenciesInformation;


	/**
	 * Constructor.
	 */
	public Diagnostic() {
		this.dependenciesInformation = new ArrayList<Diagnostic.DependencyInformation> ();
	}

	/**
	 * Constructor.
	 * @param instancePath
	 */
	public Diagnostic( String instancePath ) {
		this();
		this.instancePath = instancePath;
	}

	/**
	 * @return the instancePath
	 */
	public String getInstancePath() {
		return this.instancePath;
	}

	/**
	 * @param instancePath the instancePath to set
	 */
	public void setInstancePath( String instancePath ) {
		this.instancePath = instancePath;
	}

	/**
	 * @return the dependenciesInformation
	 */
	public List<DependencyInformation> getDependenciesInformation() {
		return this.dependenciesInformation;
	}


	/**
	 * @author Vincent Zurczak - Linagora
	 */
	public static class DependencyInformation {
		private String dependencyName;
		private boolean optional, resolved;


		/**
		 * Constructor.
		 */
		public DependencyInformation() {
			// nothing
		}

		/**
		 * Constructor.
		 * @param dependencyName
		 * @param optional
		 * @param resolved
		 */
		public DependencyInformation( String dependencyName, boolean optional, boolean resolved ) {
			this.dependencyName = dependencyName;
			this.optional = optional;
			this.resolved = resolved;
		}

		/**
		 * @return the dependencyName
		 */
		public String getDependencyName() {
			return this.dependencyName;
		}

		/**
		 * @return the optional
		 */
		public boolean isOptional() {
			return this.optional;
		}

		/**
		 * @return the resolved
		 */
		public boolean isResolved() {
			return this.resolved;
		}

		/**
		 * @param dependencyName the dependencyName to set
		 */
		public void setDependencyName( String dependencyName ) {
			this.dependencyName = dependencyName;
		}

		/**
		 * @param optional the optional to set
		 */
		public void setOptional( boolean optional ) {
			this.optional = optional;
		}

		/**
		 * @param resolved the resolved to set
		 */
		public void setResolved( boolean resolved ) {
			this.resolved = resolved;
		}
	}
}
