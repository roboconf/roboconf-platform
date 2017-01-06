/**
 * Copyright 2016-2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.core.model.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ScheduledJobTest {

	@Test
	public void testCompareTo() {

		// Create jobs
		ScheduledJob job1 = new ScheduledJob();
		ScheduledJob job2 = new ScheduledJob();
		ScheduledJob job3 = new ScheduledJob();
		ScheduledJob job4 = new ScheduledJob();
		ScheduledJob job5 = new ScheduledJob();

		job3.setJobName( "job3" );
		job4.setJobName( "job4" );
		job5.setJobName( "job5" );

		// Insert them in a list in "the wrong order"
		List<ScheduledJob> jobs = new ArrayList<> ();
		jobs.add( job5 );
		jobs.add( job1 );
		jobs.add( job3 );
		jobs.add( job4 );
		jobs.add( job2 );

		// Sort
		Collections.sort( jobs );

		// Verify the sorting
		Assert.assertEquals( job1, jobs.get( 0 ));
		Assert.assertEquals( job2, jobs.get( 1 ));
		Assert.assertEquals( job3, jobs.get( 2 ));
		Assert.assertEquals( job4, jobs.get( 3 ));
		Assert.assertEquals( job5, jobs.get( 4 ));
	}


	@Test
	public void testEquals() {

		ScheduledJob job3 = new ScheduledJob();
		ScheduledJob job4 = new ScheduledJob();
		ScheduledJob job5 = new ScheduledJob();

		job3.setJobName( "job3" );
		job5.setJobName( "job3" );

		Assert.assertEquals( job3, job5 );
		Assert.assertEquals( job3.hashCode(), job5.hashCode());

		Assert.assertNotEquals( job4, job5 );
		Assert.assertNotEquals( job4.hashCode(), job5.hashCode());
	}
}
