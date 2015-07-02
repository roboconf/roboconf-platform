/**
 * Copyright 2015 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.karaf.commands.dm.targets;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class TargetCompleterTest {

	@Test
	public void testComplete() {

		// Nothing typed in yet
		TargetCompleter completer = new TargetCompleter();
		List<String> candidates = new ArrayList<> ();

		Assert.assertEquals( 0, candidates.size());
		Assert.assertEquals( 0, completer.complete( "", 0, candidates ));
		Assert.assertEquals( SupportedTarget.allString().size(), candidates.size());

		// Complete "opens"
		candidates = new ArrayList<> ();

		Assert.assertEquals( 0, candidates.size());
		Assert.assertEquals( 0, completer.complete( "opens", 0, candidates ));
		Assert.assertEquals( 1, candidates.size());
		Assert.assertEquals( SupportedTarget.OPENSTACK.toString().toLowerCase(), candidates.get( 0 ).trim());

		// Complete "opeNs" (case insensitive)
		candidates = new ArrayList<> ();

		Assert.assertEquals( 0, candidates.size());
		Assert.assertEquals( 0, completer.complete( "opeNs", 0, candidates ));
		Assert.assertEquals( 1, candidates.size());
		Assert.assertEquals( SupportedTarget.OPENSTACK.toString().toLowerCase(), candidates.get( 0 ).trim());

		// Unknown
		candidates = new ArrayList<> ();

		Assert.assertEquals( 0, candidates.size());
		Assert.assertEquals( -1, completer.complete( "Unknown", 0, candidates ));
	}
}
