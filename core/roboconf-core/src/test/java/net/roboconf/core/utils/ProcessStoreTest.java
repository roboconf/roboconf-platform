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

package net.roboconf.core.utils;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Pierre-Yves Gibello - Linagora
 */
public class ProcessStoreTest {

	String applicationName = "app1";
	String scopedInstancePath = "/anything";

	@Test
	public void testProcessFunctions() throws IOException {
		Assert.assertNull(ProcessStore.getProcess(this.applicationName, this.scopedInstancePath));

		// Use a cross-OS command ("date" looks quite universal !)
		Process process = (new ProcessBuilder("date")).start();

		ProcessStore.setProcess(this.applicationName, this.scopedInstancePath, process);
		Assert.assertEquals(ProcessStore.getProcess(this.applicationName, this.scopedInstancePath), process);
		Assert.assertSame(ProcessStore.getProcess(this.applicationName, this.scopedInstancePath), process);

		ProcessStore.setProcess(null, null, process);
		Assert.assertEquals(ProcessStore.getProcess(null, null), process);

		ProcessStore.clearProcess(this.applicationName, this.scopedInstancePath);
		Assert.assertEquals(ProcessStore.getProcess(null, null), process);
		Assert.assertNull(ProcessStore.getProcess(this.applicationName, this.scopedInstancePath));

		ProcessStore.clearProcess(null, null);
		Assert.assertNull(ProcessStore.getProcess(null, null));
	}
}
