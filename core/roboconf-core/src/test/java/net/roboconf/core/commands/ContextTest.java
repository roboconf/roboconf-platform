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

package net.roboconf.core.commands;

import java.io.File;
import java.util.List;

import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.helpers.InstanceHelpers;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ContextTest {

	private TestApplication app;
	private Context context;


	@Before
	public void initialize() throws Exception {
		this.app = new TestApplication();
		this.context = new Context( this.app, new File( "whatever" ));
	}


	@Test
	public void testConstructor() {

		List<Instance> instances = InstanceHelpers.getAllInstances( this.app );
		Assert.assertEquals( instances.size(), this.context.instancePathToComponentName.size());

		for( Instance instance : instances ) {
			String path = InstanceHelpers.computeInstancePath( instance );
			Assert.assertEquals( instance.getComponent().getName(), this.context.instancePathToComponentName.get( path ));
		}
	}


	@Test
	public void testInstanceExists() {

		Assert.assertTrue( this.context.instanceExists( "/tomcat-vm" ));
		Assert.assertTrue( this.context.instanceExists( "/tomcat-vm/tomcat-server" ));
		Assert.assertFalse( this.context.instanceExists( "/tomcat-vm/invalid" ));
		Assert.assertFalse( this.context.instanceExists( "invalid" ));
	}


	@Test
	public void testResolveInstance() {

		Assert.assertEquals( "vm", this.context.resolveInstance( "/tomcat-vm" ).getComponent().getName());
		Assert.assertEquals( "tomcat", this.context.resolveInstance( "/tomcat-vm/tomcat-server" ).getComponent().getName());
		Assert.assertNull( this.context.resolveInstance( "/tomcat-vm/invalid" ));

		this.context.instancePathToComponentName.put( "not a path", "whatever" );
		Assert.assertNull( this.context.resolveInstance( "not a path" ));
		Assert.assertNull( this.context.resolveInstance( "" ));

		this.context.instancePathToComponentName.put( "", "vm" );
		Assert.assertNull( this.context.resolveInstance( "" ));
	}


	@Test
	public void testGetName() {

		Assert.assertEquals( "test", new Context( this.app, new File( "test" )).getName());
		Assert.assertNull( new Context( this.app, null ).getName());
	}
}
