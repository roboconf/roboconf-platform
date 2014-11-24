/**
 * Copyright 2014 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.core.model;

import java.io.File;
import java.util.UUID;

import junit.framework.Assert;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ApplicationDescriptorTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();


	@Test
	public void testSaveAndLoad() throws Exception {

		File f = this.folder.newFile();

		ApplicationDescriptor desc1 = new ApplicationDescriptor();
		desc1.setDescription( UUID.randomUUID().toString());
		desc1.setName( UUID.randomUUID().toString());
		desc1.setQualifier( UUID.randomUUID().toString());
		desc1.setGraphEntryPoint( UUID.randomUUID().toString());
		desc1.setInstanceEntryPoint( UUID.randomUUID().toString());

		ApplicationDescriptor.save( f, desc1 );
		ApplicationDescriptor desc2 = ApplicationDescriptor.load( f );

		Assert.assertEquals( desc1.getDescription(), desc2.getDescription());
		Assert.assertEquals( desc1.getName(), desc2.getName());
		Assert.assertEquals( desc1.getQualifier(), desc2.getQualifier());
		Assert.assertEquals( desc1.getGraphEntryPoint(), desc2.getGraphEntryPoint());
		Assert.assertEquals( desc1.getInstanceEntryPoint(), desc2.getInstanceEntryPoint());
	}
}