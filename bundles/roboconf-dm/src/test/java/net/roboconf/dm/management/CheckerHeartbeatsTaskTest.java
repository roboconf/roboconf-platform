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

package net.roboconf.dm.management;

import net.roboconf.core.internal.tests.TestApplication;

import org.junit.Before;
import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class CheckerHeartbeatsTaskTest {

	@Before
	public void shutdownDm() {
		Manager.INSTANCE.shutdown();
	}


	@Test
	public void testRun_noApplication() {

		CheckerHeartbeatsTask task = new CheckerHeartbeatsTask();
		task.run();
	}


	@Test
	public void testRun() {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app, null );
		Manager.INSTANCE.getAppNameToManagedApplication().put( app.getName(), ma );

		CheckerHeartbeatsTask task = new CheckerHeartbeatsTask();
		task.run();
	}
}
