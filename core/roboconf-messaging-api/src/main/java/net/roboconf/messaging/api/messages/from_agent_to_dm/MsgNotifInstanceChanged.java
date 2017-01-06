/**
 * Copyright 2014-2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.messaging.api.messages.from_agent_to_dm;

import java.util.Collection;
import java.util.Map;

import net.roboconf.core.model.beans.Import;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.messaging.api.messages.Message;

/**
 * @author Noël - LIG
 */
public class MsgNotifInstanceChanged extends Message {

	private static final long serialVersionUID = -5023778542512797206L;

	private final String applicationName;
	private final String instancePath;
	private final Map<String,Collection<Import>> newImports;
	private InstanceStatus newStatus;


	/**
	 * Constructor.
	 * @param applicationName the application name
	 * @param instance the changed instance
	 */
	public MsgNotifInstanceChanged( String applicationName, Instance instance ) {
		super();
		this.instancePath = InstanceHelpers.computeInstancePath( instance );
		this.newImports = instance.getImports();
		this.newStatus = instance.getStatus();
		this.applicationName = applicationName;
	}

	/**
	 * @return the applicationName
	 */
	public String getApplicationName() {
		return this.applicationName;
	}

	/**
	 * @return the instancePath
	 */
	public String getInstancePath() {
		return this.instancePath;
	}

	/**
	 * @return the newImports
	 */
	public Map<String, Collection<Import>> getNewImports() {
		return this.newImports;
	}

	/**
	 * @return the newStatus
	 */
	public InstanceStatus getNewStatus() {
		return this.newStatus;
	}

	/**
	 * @param newStatus the new status
	 */
	public void setNewStatus( InstanceStatus newStatus ) {
		this.newStatus = newStatus;
	}
}
