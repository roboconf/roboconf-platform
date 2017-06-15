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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.core.model.runtime.EventType;

/**
 * @author Amadou Diarra - UGA
 */
public class BundleDecanterCollectorTest {

	private final TestApplication app = new TestApplication();
	private EventAdmin eventAdmin;
	private BundleDecanterCollector bdc;

	@Before
	public void configure() {
		this.eventAdmin = Mockito.mock(EventAdmin.class);
		this.bdc = new BundleDecanterCollector( this.eventAdmin );
	}

	@Test
	public void testBasics() {

		Assert.assertEquals( this.bdc.getId(), "decanterCollectorID" );

		this.bdc.enableNotifications();
		Assert.assertTrue( this.bdc.enabled.get());

		this.bdc.disableNotifications();
		Assert.assertFalse( this.bdc.enabled.get());

		this.bdc.application(this.app, EventType.CHANGED);
		this.bdc.applicationTemplate(this.app.getTemplate(), EventType.CREATED);
		this.bdc.raw( null);
	}

	@Test
	public void test_instance() {

		// Not a scoped instance
		this.bdc.disableNotifications();
		this.bdc.instance( this.app.getMySql(), this.app, EventType.CREATED);
		Mockito.verifyZeroInteractions(this.eventAdmin);

		this.bdc.enableNotifications();
		this.bdc.instance( this.app.getMySql(), this.app, EventType.CREATED);
		Mockito.verifyZeroInteractions(this.eventAdmin);


		// A scoped instance
		this.app.getMySqlVm().setStatus(InstanceStatus.DEPLOYED_STARTED);

		HashMap<String,Object> data = new HashMap<> ();
		data.put("type", "roboconf");
		data.put("appName", this.app.getName());
		data.put("instanceNumber", 1);
		Event event = new Event("decanter/collector/dm", data);

		this.bdc.enableNotifications();
		this.bdc.instance( this.app.getMySqlVm(), this.app, EventType.CHANGED );
		Mockito.verify(this.eventAdmin).postEvent(event);
	}

}
