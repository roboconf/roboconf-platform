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

package net.roboconf.agent.mbeans;

import java.lang.management.ManagementFactory;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import net.roboconf.agent.internal.Agent;
import net.roboconf.plugin.api.PluginException;
import net.roboconf.plugin.api.PluginInterface;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test plugin MBean implementation.
 * @author Pierre-Yves Gibello - Linagora
 *
 */
public class PluginMBeanTest {

	MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
	ObjectName objectName;
	PluginInterface plugin;

	@Before
	public void initialize() throws MalformedObjectNameException, MBeanRegistrationException, NotCompliantMBeanException {
		Agent agent = new Agent();
		agent.setSimulatePlugins(true);
		this.plugin = agent.findPlugin(null);
		this.objectName = new ObjectName("net.roboconf:type=agent-plugins");
		try {
			this.mbs.registerMBean(new PluginStats(), this.objectName);
		} catch (InstanceAlreadyExistsException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testPluginMBean() throws AttributeNotFoundException, InstanceNotFoundException, MBeanException, ReflectionException, PluginException {

		int count;

		this.mbs.invoke(this.objectName, "reset", null, null);

		count = (int)this.mbs.getAttribute(this.objectName, "InitializeCount");
		Assert.assertEquals(0, count);

		count = (int)this.mbs.getAttribute(this.objectName, "DeployCount");
		Assert.assertEquals(0, count);

		count = (int)this.mbs.getAttribute(this.objectName, "UndeployCount");
		Assert.assertEquals(0, count);

		count = (int)this.mbs.getAttribute(this.objectName, "StartCount");
		Assert.assertEquals(0, count);

		count = (int)this.mbs.getAttribute(this.objectName, "StopCount");
		Assert.assertEquals(0, count);

		count = (int)this.mbs.getAttribute(this.objectName, "UpdateCount");
		Assert.assertEquals(0, count);

		count = (int)this.mbs.getAttribute(this.objectName, "ErrorCount");
		Assert.assertEquals(0, count);

		plugin.initialize(null);
		plugin.deploy(null);
		plugin.start(null);
		plugin.update(null, null, null);
		plugin.stop(null);
		plugin.undeploy(null);

		count = (int)this.mbs.getAttribute(this.objectName, "InitializeCount");
		Assert.assertEquals(1, count);

		count = (int)this.mbs.getAttribute(this.objectName, "DeployCount");
		Assert.assertEquals(1, count);

		count = (int)this.mbs.getAttribute(this.objectName, "UndeployCount");
		Assert.assertEquals(1, count);

		count = (int)this.mbs.getAttribute(this.objectName, "StartCount");
		Assert.assertEquals(1, count);

		count = (int)this.mbs.getAttribute(this.objectName, "StopCount");
		Assert.assertEquals(1, count);

		count = (int)this.mbs.getAttribute(this.objectName, "UpdateCount");
		Assert.assertEquals(1, count);

		count = (int)this.mbs.getAttribute(this.objectName, "ErrorCount");
		Assert.assertEquals(0, count);
	}
}
