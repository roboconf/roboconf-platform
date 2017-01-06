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
 * An informative "bean" that contains significant information to manage targets.
 * <p>
 * This class is made available in the core because it is used in several bundles.
 * As the core is intended to be the location for most of the common items, this
 * choice is consistent.
 * </p>
 *
 * @author Vincent Zurczak - Linagora
 */
public class TargetWrapperDescriptor {

	private String id, name, description, handler;
	private boolean isDefault = false;


	@Override
	public boolean equals( Object obj ) {
		return obj instanceof TargetWrapperDescriptor
				&& Objects.equals( this.id, ((TargetWrapperDescriptor) obj).id );
	}

	@Override
	public int hashCode() {
		return this.id == null ? 37 : this.id.hashCode();
	}


	/**
	 * @return the id
	 */
	public String getId() {
		return this.id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId( String id ) {
		this.id = id;
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
	 * @return the handler
	 */
	public String getHandler() {
		return this.handler;
	}

	/**
	 * @param handler the handler to set
	 */
	public void setHandler( String handler ) {
		this.handler = handler;
	}

	/**
	 * @return the isDefault
	 */
	public boolean isDefault() {
		return this.isDefault;
	}

	/**
	 * @param isDefault the isDefault to set
	 */
	public void setDefault( boolean isDefault ) {
		this.isDefault = isDefault;
	}
}
