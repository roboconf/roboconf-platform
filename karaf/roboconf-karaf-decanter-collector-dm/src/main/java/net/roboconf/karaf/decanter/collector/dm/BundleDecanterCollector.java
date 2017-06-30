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

package net.roboconf.karaf.decanter.collector.dm;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.runtime.EventType;
import net.roboconf.dm.management.events.IDmListener;

/**
 * @author Amadou Diarra - UGA
 */
public class BundleDecanterCollector implements IDmListener {

	EventAdmin eventAdmin;
	final AtomicBoolean enabled = new AtomicBoolean( false );


	/**
	 * Constructs a bundle to collect informations.
	 * */
	public BundleDecanterCollector(EventAdmin eventAdmin) {
		this.eventAdmin = eventAdmin;
	}

	@Override
	public String getId() {
		return "decanterCollectorID";
	}

	@Override
	public void enableNotifications() {
		this.enabled.set(true);
	}

	@Override
	public void disableNotifications() {
		this.enabled.set(false);
	}

	@Override
	public void application(Application application, EventType eventType) {
		// nothing
	}

	@Override
	public void applicationTemplate(ApplicationTemplate tpl, EventType eventType) {
		// nothing
	}

	@Override
	public void instance(Instance instance, Application application, EventType eventType) {

		if( this.enabled.get() && InstanceHelpers.isTarget(instance)) {

			List<Instance> scopedInstances = InstanceHelpers.findAllScopedInstances(application);
			int upInstances = 0;
			for(Instance ins : scopedInstances) {
				if(ins.getStatus() == InstanceStatus.DEPLOYED_STARTED) {
					upInstances++;
				}
			}

			HashMap<String,Object> data = new HashMap<>();
			data.put("type", "roboconf");
			data.put("appName", application.getName());
			data.put("instanceNumber", upInstances);
			Event event = new Event("decanter/collector/dm", data);
			this.eventAdmin.postEvent(event);
		}
	}

	@Override
	public void raw(String message, Object... data) {
		// nothing
	}
}
