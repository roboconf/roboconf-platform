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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import net.roboconf.core.model.helpers.InstancesHelper;
import net.roboconf.core.model.runtime.Application;
import net.roboconf.core.model.runtime.Component;
import net.roboconf.core.model.runtime.Instance;

/**
 * A basic implementation of the {@link Application} interface.
 * @author Vincent Zurczak - Linagora
 */
public class InstanceImpl implements Instance {

	private String name, channel;
	private Component component;
	private Instance container;
	private final Collection<Instance> children = new HashSet<Instance> ();
	private final Map<String,String> overridenExports = new HashMap<String,String> ();


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
	 * @return the channel
	 */
	@Override
	public String getChannel() {
		return this.channel;
	}

	/**
	 * @param channel the channel to set
	 */
	public void setChannel( String channel ) {
		this.channel = channel;
	}

	/**
	 * @return the component
	 */
	@Override
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
	 * @return the container
	 */
	@Override
	public Instance getContainer() {
		return this.container;
	}

	/**
	 * @param container the container to set
	 */
	public void setContainer( Instance container ) {
		this.container = container;
	}

	/**
	 * @return the children
	 */
	@Override
	public Collection<Instance> getChildren() {
		return this.children;
	}

	/**
	 * @return the overridenExports
	 */
	@Override
	public Map<String, String> getOverridenExports() {
		return this.overridenExports;
	}

	@Override
	public int hashCode() {
		return InstancesHelper.computeInstancePath( this ).hashCode();
	}

	@Override
	public boolean equals( Object obj ) {
		return obj instanceof Instance
				&& InstancesHelper.haveSamePath( this, (Instance) obj);
	}

	@Override
	public String toString() {
		return getName();
	}
}
