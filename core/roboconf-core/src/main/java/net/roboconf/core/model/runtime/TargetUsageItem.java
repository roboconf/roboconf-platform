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

package net.roboconf.core.model.runtime;

import java.util.Objects;

/**
 * A bean that describes associations between applications and targets.
 * @author Vincent Zurczak - Linagora
 */
public class TargetUsageItem {

	private String name, version;
	private boolean isReferencing, isUsing;


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
	 * @return the isReferencing
	 */
	public boolean isReferencing() {
		return this.isReferencing;
	}

	/**
	 * @param isReferencing the isReferencing to set
	 */
	public void setReferencing( boolean isReferencing ) {
		this.isReferencing = isReferencing;
	}

	/**
	 * @return the isUsing
	 */
	public boolean isUsing() {
		return this.isUsing;
	}

	/**
	 * @param isUsing the isUsing to set
	 */
	public void setUsing( boolean isUsing ) {
		this.isUsing = isUsing;
	}


	@Override
	public int hashCode() {
		int i1 = this.name == null ? 83 : this.name.hashCode();
		int i2 = this.version == null ? 11 : this.version.hashCode();

		return i1 + i2;
	}


	@Override
	public boolean equals( Object obj ) {
		return obj instanceof TargetUsageItem
				&& Objects.equals( this.name, ((TargetUsageItem) obj ).name )
				&& Objects.equals( this.version, ((TargetUsageItem) obj ).version );
	}
}
