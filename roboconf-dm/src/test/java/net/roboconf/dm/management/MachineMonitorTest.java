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

import junit.framework.Assert;
import net.roboconf.core.model.runtime.Application;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.core.model.runtime.Instance.InstanceStatus;

import org.junit.Test;

/**
 * Test MachineMonitor (timers are disabled).
 * @author Vincent Zurczak - Linagora
 */
public class MachineMonitorTest {

	@Test
	public void testMonitor_1() throws Exception {

		// Test an empty application
		MachineMonitor monitor = new MachineMonitor( new Application());
		monitor.stopTimer();
		Assert.assertTrue( monitor.getRootInstanceToMissedHeartBeatsCount().isEmpty());
	}


	@Test
	public void testMonitor_2() throws Exception {

		// Prepare our application
		Application app = new Application();
		app.setName(  "app1"  );

		Instance ri1 = new Instance( "ri1" );
		app.getRootInstances().add( ri1 );

		Instance ri2 = new Instance( "ri2" );
		ri2.setStatus( InstanceStatus.UNDEPLOYING );
		app.getRootInstances().add( ri2 );

		Instance ri3 = new Instance( "ri3" );
		ri3.setStatus( InstanceStatus.DEPLOYING );
		app.getRootInstances().add( ri3 );

		// They should not appear in the map
		MachineMonitor monitor = new MachineMonitor( app );
		monitor.stopTimer();
		Assert.assertTrue( monitor.getRootInstanceToMissedHeartBeatsCount().isEmpty());

		// Put one as candidate for problems
		ri1.setStatus( InstanceStatus.DEPLOYED_STARTED );
		monitor = new MachineMonitor( app );
		monitor.stopTimer();
		Assert.assertTrue( monitor.getRootInstanceToMissedHeartBeatsCount().isEmpty());

		for( int i=0; i<MachineMonitor.MISSED_HEARTBEATS_THRESHOLD + 5; i++ ) {
			Integer iteration = i+1;
			monitor.monitorAllMachine();
			Assert.assertEquals( "Iteration " + iteration, 1, monitor.getRootInstanceToMissedHeartBeatsCount().size());

			Integer count = monitor.getRootInstanceToMissedHeartBeatsCount().get( ri1 );
			Assert.assertEquals( "Iteration " + iteration, iteration, count );

			if( i < MachineMonitor.MISSED_HEARTBEATS_THRESHOLD )
				Assert.assertEquals( "Iteration " + iteration, InstanceStatus.DEPLOYED_STARTED, ri1.getStatus());
			else
				Assert.assertEquals( "Iteration " + iteration, InstanceStatus.PROBLEM, ri1.getStatus());
		}

		Integer count = monitor.getRootInstanceToMissedHeartBeatsCount().get( ri1 );
		Assert.assertEquals( InstanceStatus.PROBLEM, ri1.getStatus());
		Assert.assertEquals( 1, monitor.getRootInstanceToMissedHeartBeatsCount().size());

		// Back to a normal state
		monitor.acknowledgeHeartBeat( ri1 );
		count = monitor.getRootInstanceToMissedHeartBeatsCount().get( ri1 );
		Assert.assertEquals( 0, count.intValue());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, ri1.getStatus());
	}
}
