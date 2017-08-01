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

package net.roboconf.agent.jmx;

import java.lang.management.ManagementFactory;
import java.util.Arrays;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import net.roboconf.agent.internal.Agent;
import net.roboconf.agent.internal.PluginProxy;
import net.roboconf.core.model.beans.Component;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.plugin.api.PluginException;
import net.roboconf.plugin.api.PluginInterface;

/**
 * Test plugin MBean implementation.
 * @author Pierre-Yves Gibello - Linagora
 */
public class PluginMBeanTest {

	private static final String PL_NAME = "mockito";
	private static MBeanServer mbs;
	private static ObjectName objectName;

	final Instance inst = new Instance( "i" ).component( new Component( "c" ).installerName( PL_NAME ));


	@BeforeClass
	public static void initialize() throws Exception {

		mbs = ManagementFactory.getPlatformMBeanServer();
		objectName = new ObjectName("net.roboconf:type=agent-plugins");
		try {
			mbs.registerMBean( new PluginStats(), objectName );

		} catch (InstanceAlreadyExistsException e) {
			e.printStackTrace();
		}
	}


	@Test
	public void testPluginMBean() throws Exception {

		// Prepare
		Agent agent = new Agent();
		agent.setSimulatePlugins( false );

		PluginInterface mockPlugin = Mockito.mock( PluginInterface.class );
		Mockito.when( mockPlugin.getPluginName()).thenReturn( PL_NAME );

		agent.pluginAppears( mockPlugin );
		PluginInterface proxyfiedPlugin = agent.findPlugin( this.inst );
		Assert.assertSame( mockPlugin, ((PluginProxy) proxyfiedPlugin).getPlugin());

		// Execute and verify
		mbs.invoke(objectName, "reset", null, null);

		Assert.assertEquals( 0, (int) mbs.getAttribute(objectName, "InitializeCount"));
		Assert.assertEquals( 0, (int) mbs.getAttribute(objectName, "DeployCount"));
		Assert.assertEquals( 0, (int) mbs.getAttribute(objectName, "UndeployCount"));
		Assert.assertEquals( 0, (int) mbs.getAttribute(objectName, "StartCount"));
		Assert.assertEquals( 0, (int) mbs.getAttribute(objectName, "StopCount"));
		Assert.assertEquals( 0, (int) mbs.getAttribute(objectName, "UpdateCount"));
		Assert.assertEquals( 0, (int) mbs.getAttribute(objectName, "ErrorCount"));

		proxyfiedPlugin.initialize( this.inst );
		proxyfiedPlugin.deploy( this.inst );
		proxyfiedPlugin.start( this.inst );
		proxyfiedPlugin.update( this.inst, null, null );
		proxyfiedPlugin.stop( this.inst );
		proxyfiedPlugin.undeploy( this.inst );

		Assert.assertEquals( 1, (int) mbs.getAttribute(objectName, "InitializeCount"));
		Assert.assertEquals( 1, (int) mbs.getAttribute(objectName, "DeployCount"));
		Assert.assertEquals( 1, (int) mbs.getAttribute(objectName, "UndeployCount"));
		Assert.assertEquals( 1, (int) mbs.getAttribute(objectName, "StartCount"));
		Assert.assertEquals( 1, (int) mbs.getAttribute(objectName, "StopCount"));
		Assert.assertEquals( 1, (int) mbs.getAttribute(objectName, "UpdateCount"));
		Assert.assertEquals( 0, (int) mbs.getAttribute(objectName, "ErrorCount"));
	}


	@Test
	public void testPluginMBean_withErrors() throws Exception {

		// Prepare - all the non-mocked methods will throw an exception
		Agent agent = new Agent();
		agent.setSimulatePlugins( false );

		PluginInterface mockPlugin = Mockito.mock( PluginInterface.class, new Answer<Void> () {
			@Override
			public Void answer( InvocationOnMock invocation ) throws Throwable {

				if( ! Arrays.asList( "getPluginName", "setNames" ).contains( invocation.getMethod().getName()))
					throw new PluginException( "for test" );

				return null;
			}
		});

		Mockito.when( mockPlugin.getPluginName()).thenReturn( PL_NAME );
		agent.pluginAppears( mockPlugin );
		PluginInterface proxyfiedPlugin = agent.findPlugin( this.inst );
		Assert.assertSame( mockPlugin, ((PluginProxy) proxyfiedPlugin).getPlugin());

		// Execute and verify
		mbs.invoke(objectName, "reset", null, null);

		Assert.assertEquals( 0, (int) mbs.getAttribute(objectName, "InitializeCount"));
		Assert.assertEquals( 0, (int) mbs.getAttribute(objectName, "DeployCount"));
		Assert.assertEquals( 0, (int) mbs.getAttribute(objectName, "UndeployCount"));
		Assert.assertEquals( 0, (int) mbs.getAttribute(objectName, "StartCount"));
		Assert.assertEquals( 0, (int) mbs.getAttribute(objectName, "StopCount"));
		Assert.assertEquals( 0, (int) mbs.getAttribute(objectName, "UpdateCount"));
		Assert.assertEquals( 0, (int) mbs.getAttribute(objectName, "ErrorCount"));

		try {
			proxyfiedPlugin.initialize( this.inst );
			Assert.fail( "An exception was expected." );
		} catch( PluginException e ) {
			// nothing
		}

		try {
			proxyfiedPlugin.deploy( this.inst );
			Assert.fail( "An exception was expected." );
		} catch( PluginException e ) {
			// nothing
		}

		try {
			proxyfiedPlugin.start( this.inst );
			Assert.fail( "An exception was expected." );
		} catch( PluginException e ) {
			// nothing
		}

		try {
			proxyfiedPlugin.update( this.inst, null, null );
			Assert.fail( "An exception was expected." );
		} catch( PluginException e ) {
			// nothing
		}

		try {
			proxyfiedPlugin.stop( this.inst );
			Assert.fail( "An exception was expected." );
		} catch( PluginException e ) {
			// nothing
		}

		try {
			proxyfiedPlugin.undeploy( this.inst );
			Assert.fail( "An exception was expected." );
		} catch( PluginException e ) {
			// nothing
		}

		Assert.assertEquals( 0, (int) mbs.getAttribute(objectName, "InitializeCount"));
		Assert.assertEquals( 0, (int) mbs.getAttribute(objectName, "DeployCount"));
		Assert.assertEquals( 0, (int) mbs.getAttribute(objectName, "UndeployCount"));
		Assert.assertEquals( 0, (int) mbs.getAttribute(objectName, "StartCount"));
		Assert.assertEquals( 0, (int) mbs.getAttribute(objectName, "StopCount"));
		Assert.assertEquals( 0, (int) mbs.getAttribute(objectName, "UpdateCount"));
		Assert.assertEquals( 6, (int) mbs.getAttribute(objectName, "ErrorCount"));
	}
}
