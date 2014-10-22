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

package net.roboconf.messaging.messages.from_dm_to_agent;

import java.util.Map;

import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.messaging.messages.Message;

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

	private final String parentInstancePath, instanceName, componentName, channel;
	private Map<String,String> data;
	private Map<String,String> overridenExports;


	/**
	 * Constructor.
	 * @param instance
	 */
	public MsgCmdAddInstance( Instance instance ) {

		this.parentInstancePath = instance.getParent() == null ? null : InstanceHelpers.computeInstancePath( instance.getParent());
		this.instanceName = instance.getName();
		this.componentName = instance.getComponent() != null ? instance.getComponent().getName() : null;
		this.channel = instance.getChannel();

		this.data = instance.getData();
		this.overridenExports = instance.getOverriddenExports();
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
	 * @return the channel
	 */
	public String getChannel() {
		return this.channel;
	}

	/**
	 * @return the data
	 */
	public Map<String,String> getData() {
		return this.data;
	}

	/**
	 * @return the overridenExports
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
	 * @param overridenExports the overridenExports to set
	 */
	public void setOverridenExports( Map<String,String> overridenExports ) {
		this.overridenExports = overridenExports;
	}
}
