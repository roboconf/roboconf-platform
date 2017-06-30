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

package net.roboconf.core.model;

import java.io.File;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.ApplicationTemplate;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ApplicationDescriptorTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();


	@Test
	public void testSaveAndLoad() throws Exception {

		ApplicationDescriptor desc1 = new ApplicationDescriptor();
		saveAndCompare( desc1 );

		desc1.setDescription( UUID.randomUUID().toString());
		saveAndCompare( desc1 );

		desc1.setName( UUID.randomUUID().toString());
		saveAndCompare( desc1 );

		desc1.setTemplateName( "my-tpl" );
		saveAndCompare( desc1 );

		desc1.setTemplateVersion( "version 1" );
		saveAndCompare( desc1 );

		desc1.setDescription( "A string\nwith\nmany\n\tline\nbreaks!" );
		saveAndCompare( desc1 );

		desc1.setName( "avé les àçents" );
		saveAndCompare( desc1 );
	}


	private void saveAndCompare( ApplicationDescriptor desc ) throws Exception {

		File f = this.folder.newFile();

		ApplicationTemplate tpl = new ApplicationTemplate( desc.getTemplateName()).version( desc.getTemplateVersion());
		Application app = new Application( desc.getName(), tpl ).description( desc.getDescription());

		ApplicationDescriptor.save( f, app );
		ApplicationDescriptor desc2 = ApplicationDescriptor.load( f );

		Assert.assertEquals( desc.getDescription(), desc2.getDescription());
		Assert.assertEquals( desc.getName(), desc2.getName());
		Assert.assertEquals( desc.getTemplateName(), desc2.getTemplateName());
		Assert.assertEquals( desc.getTemplateVersion(), desc2.getTemplateVersion());
	}
}
