/**
 * Copyright 2014-2015 Linagora, Université Joseph Fourier, Floralis
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
import net.roboconf.core.utils.Utils;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ApplicationTemplateDescriptorTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();


	@Test
	public void testSaveAndLoad() throws Exception {

		ApplicationTemplateDescriptor desc1 = new ApplicationTemplateDescriptor();
		saveAndCompare( desc1 );

		desc1.setDescription( UUID.randomUUID().toString());
		saveAndCompare( desc1 );

		desc1.setName( UUID.randomUUID().toString());
		saveAndCompare( desc1 );

		desc1.setQualifier( UUID.randomUUID().toString());
		saveAndCompare( desc1 );

		desc1.setDslId( UUID.randomUUID().toString());
		saveAndCompare( desc1 );

		desc1.setGraphEntryPoint( UUID.randomUUID().toString());
		saveAndCompare( desc1 );

		desc1.setInstanceEntryPoint( UUID.randomUUID().toString());
		saveAndCompare( desc1 );

		desc1.externalExports.put( "Test.to", "to" );
		saveAndCompare( desc1 );

		desc1.externalExports.put( "Test2.to", "too" );
		saveAndCompare( desc1 );

		desc1.setExternalExportsPrefix( "whatever" );
		saveAndCompare( desc1 );
	}


	@Test
	public void testInvalidExternalExports_1() throws Exception {

		File f = this.folder.newFile();
		Utils.writeStringInto( "exports = toto IS titi", f );

		ApplicationTemplateDescriptor desc = ApplicationTemplateDescriptor.load( f );
		Assert.assertEquals( 0, desc.externalExports.size());
		Assert.assertEquals( 1, desc.invalidExternalExports.size());
		Assert.assertEquals( "toto IS titi", desc.invalidExternalExports.iterator().next());
	}


	@Test
	public void testInvalidExternalExports_2() throws Exception {

		File f = this.folder.newFile();
		Utils.writeStringInto( "exports: HAProxy.ip as lb-ip HAProxy.httpPort as lb-port", f );

		ApplicationTemplateDescriptor desc = ApplicationTemplateDescriptor.load( f );
		Assert.assertEquals( 0, desc.externalExports.size());
		Assert.assertEquals( 1, desc.invalidExternalExports.size());
		Assert.assertEquals( "HAProxy.ip as lb-ip HAProxy.httpPort as lb-port", desc.invalidExternalExports.iterator().next());
	}


	@Test
	public void testValidExternalExports() throws Exception {

		File f = this.folder.newFile();
		Utils.writeStringInto( "exports: HAProxy.ip as lb-ip, HAProxy.httpPort as lb-port", f );

		ApplicationTemplateDescriptor desc = ApplicationTemplateDescriptor.load( f );
		Assert.assertEquals( 0, desc.invalidExternalExports.size());
		Assert.assertEquals( 2, desc.externalExports.size());

		Assert.assertEquals( "lb-ip", desc.externalExports.get( "HAProxy.ip" ));
		Assert.assertEquals( "lb-port", desc.externalExports.get( "HAProxy.httpPort" ));
	}


	private void saveAndCompare( ApplicationTemplateDescriptor desc1 ) throws Exception {

		File f = this.folder.newFile();

		ApplicationTemplateDescriptor.save( f, desc1 );
		ApplicationTemplateDescriptor desc2 = ApplicationTemplateDescriptor.load( f );

		Assert.assertEquals( desc1.getDescription(), desc2.getDescription());
		Assert.assertEquals( desc1.getName(), desc2.getName());
		Assert.assertEquals( desc1.getQualifier(), desc2.getQualifier());
		Assert.assertEquals( desc1.getDslId(), desc2.getDslId());
		Assert.assertEquals( desc1.getGraphEntryPoint(), desc2.getGraphEntryPoint());
		Assert.assertEquals( desc1.getInstanceEntryPoint(), desc2.getInstanceEntryPoint());
		Assert.assertEquals( desc1.externalExports, desc2.externalExports );
		Assert.assertEquals( desc1.invalidExternalExports, desc2.invalidExternalExports );
	}
}
