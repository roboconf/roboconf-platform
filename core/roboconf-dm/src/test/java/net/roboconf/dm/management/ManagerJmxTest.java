/**
 * Copyright 2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.dm.management;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.internal.tests.TestApplicationTemplate;
import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.dm.internal.test.TestManagerWrapper;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ManagerJmxTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();


	@Test
	public void testJmxMethods() throws Exception {

		// Setup
		Manager manager = new Manager();
		manager.configurationMngr().setWorkingDirectory( this.folder.newFolder());
		TestManagerWrapper mngrWrapper = new TestManagerWrapper( manager );

		ApplicationTemplate tpl = new TestApplicationTemplate();
		mngrWrapper.getApplicationTemplates().put( tpl, Boolean.TRUE );
		mngrWrapper.addManagedApplication( new ManagedApplication( new TestApplication().name( "app1" ).directory( this.folder.newFolder())));
		mngrWrapper.addManagedApplication( new ManagedApplication( new TestApplication().name( "app2" ).directory( this.folder.newFolder())));

		// Check the JMX methods
		Assert.assertEquals( 1, manager.getApplicationTemplateCount());
		Assert.assertEquals( 2, manager.getApplicationCount());
		Assert.assertEquals(
				2 * InstanceHelpers.getAllInstances( new TestApplication()).size(),
				manager.getInstancesCount());

		Assert.assertEquals(
				2 * InstanceHelpers.findAllScopedInstances( new TestApplication()).size(),
				manager.getScopedInstancesCount());
	}
}
