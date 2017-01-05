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

package net.roboconf.messaging.api.messages.from_dm_to_agent;

import java.util.Collection;
import java.util.Map;

import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.messaging.api.messages.Message;

/**
 * A class defining a new (single) instance.
 * <p>
 * We do not send the entire instance object, because it draws
 * the entire graph.
 * </p>
 *
 * @author Noël - LIG
 */
public class MsgCmdAddInstance extends Message {

	private static final long serialVersionUID = 2695002034974872770L;

	private final String parentInstancePath, instanceName, componentName;
	private Map<String,String> data;
	private Map<String,String> overridenExports;
	private final Collection<String> channels;


	/**
	 * Constructor.
	 * @param instance
	 */
	public MsgCmdAddInstance( Instance instance ) {

		this.parentInstancePath = instance.getParent() == null ? null : InstanceHelpers.computeInstancePath( instance.getParent());
		this.instanceName = instance.getName();
		this.componentName = instance.getComponent() != null ? instance.getComponent().getName() : null;

		this.channels = instance.channels;
		this.data = instance.data;
		this.overridenExports = instance.overriddenExports;
	}

	/**
	 * @return the parentInstancePath
	 */
	public String getParentInstancePath() {
		return this.parentInstancePath;
	}

	/**
	 * @return the instanceName
	 */
	public String getInstanceName() {
		return this.instanceName;
	}

	/**
	 * @return the componentName
	 */
	public String getComponentName() {
		return this.componentName;
	}

	/**
	 * @return the channels
	 */
	public Collection<String> getChannels() {
		return this.channels;
	}

	/**
	 * @return the data
	 */
	public Map<String,String> getData() {
		return this.data;
	}

	/**
	 * @return the overriddenExports
	 */
	public Map<String,String> getOverridenExports() {
		return this.overridenExports;
	}

	/**
	 * @param data the data to set
	 */
	public void setData( Map<String,String> data ) {
		this.data = data;
	}

	/**
	 * @param overridenExports the overriddenExports to set
	 */
	public void setOverridenExports( Map<String,String> overridenExports ) {
		this.overridenExports = overridenExports;
	}
}
