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

package net.roboconf.target.api;

import java.util.Iterator;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import net.roboconf.target.api.AbstractThreadedTargetHandler.CancelledMachines;

/**
 * @author Vincent Zurczak - Linagora
 */
public class CancelledMachinesTest {

	@Test
	public void testWorkflow() {

		CancelledMachines cm = new CancelledMachines();
		Assert.assertEquals( 0, cm.removeSnapshot().size());

		cm.addMachineId( "test1" );
		Assert.assertEquals( 1, cm.getCancelledIds().size());

		Set<String> snapshot = cm.removeSnapshot();
		Assert.assertEquals( 1, snapshot.size());
		Assert.assertEquals( "test1", snapshot.iterator().next());

		cm.addMachineId( "test2" );
		cm.addMachineId( "test3" );
		Assert.assertEquals( 2, cm.getCancelledIds().size());
		Assert.assertEquals( 2, cm.getCancelledIds().size());

		snapshot = cm.removeSnapshot();
		Assert.assertEquals( 2, snapshot.size());

		Iterator<String> it = snapshot.iterator();
		Assert.assertEquals( "test2", it.next());
		Assert.assertEquals( "test3", it.next());

		Assert.assertEquals( 0, cm.removeSnapshot().size());
	}
}
