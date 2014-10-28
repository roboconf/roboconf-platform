/**
 * Copyright 2014 Linagora, Université Joseph Fourier
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

package net.roboconf.dm.internal.management;

import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.dm.internal.test.TestTargetResolver;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.messaging.MessagingConstants;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class CheckerHeartbeatsTaskTest {

	private Manager manager;


	@Before
	public void resetManager() {
		this.manager = new Manager( MessagingConstants.FACTORY_TEST );
		this.manager.setTargetResolver( new TestTargetResolver());
		this.manager.update();
	}


	@After
	public void cleanManager() {
		this.manager.shutdown();
	}


	@Test
	public void testRun_noApplication() {

		CheckerHeartbeatsTask task = new CheckerHeartbeatsTask( this.manager );
		task.run();
	}


	@Test
	public void testRun() {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app, null );
		this.manager.getAppNameToManagedApplication().put( app.getName(), ma );

		CheckerHeartbeatsTask task = new CheckerHeartbeatsTask( this.manager );
		task.run();
	}
}