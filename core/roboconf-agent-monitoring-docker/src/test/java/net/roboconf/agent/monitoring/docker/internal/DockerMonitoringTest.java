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

package net.roboconf.agent.monitoring.docker.internal;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import net.roboconf.core.model.beans.Component;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.messaging.api.messages.from_agent_to_dm.MsgNotifAutonomic;


/**
 * @author Vincent Zurczak - Linagora
 */
public class DockerMonitoringTest {

	@Test
	public void testGetName() {
		DockerMonitoringHandler handler = new DockerMonitoringHandler();
		Assert.assertEquals( DockerMonitoringHandler.HANDLER_NAME, handler.getName());
	}


	@Test
	public void testSetAgentId() {

		DockerMonitoringHandler handler = new DockerMonitoringHandler();
		Assert.assertNull( handler.applicationName );
		Assert.assertNull( handler.scopedInstancePath );

		handler.setAgentId( "app", "root" );
		Assert.assertEquals( "app", handler.applicationName );
		Assert.assertEquals( "root", handler.scopedInstancePath );
	}


	@Test
	public void testReset() {

		DockerMonitoringHandler handler = new DockerMonitoringHandler();
		Assert.assertNull( handler.eventName );
		Assert.assertNull( handler.containerName );

		handler.reset( new Instance( "inst" ).component( new Component( "c" ) ), "ev1", "" );
		Assert.assertEquals( "ev1", handler.eventName );
		Assert.assertEquals( "inst", handler.containerName );

		handler.reset( new Instance( "inst" ).component( new Component( "c" )), "ev1", "oops" );
		Assert.assertEquals( "ev1", handler.eventName );
		Assert.assertEquals( "oops", handler.containerName );

		handler.reset( new Instance( "inst" ).component( new Component( "c" )), "ev2", "ROBOCONF_INSTANCE_NAME" );
		Assert.assertEquals( "ev2", handler.eventName );
		Assert.assertEquals( "inst", handler.containerName );

		handler.reset( new Instance( "inst" ).component( new Component( "c" )), "ev3", "ROBOCONF_INSTANCE_PATH" );
		Assert.assertEquals( "ev3", handler.eventName );
		Assert.assertEquals( "/inst", handler.containerName );

		handler.reset( new Instance( "inst" ).component( new Component( "c" )), "ev4", "ROBOCONF_CLEAN_INSTANCE_PATH" );
		Assert.assertEquals( "ev4", handler.eventName );
		Assert.assertEquals( "inst", handler.containerName );

		Instance parentInstance = new Instance( "parent" );
		Instance childInstance = new Instance( "child" ).component( new Component( "c" ));
		InstanceHelpers.insertChild( parentInstance, childInstance );

		handler.reset( childInstance, "ev5", "ROBOCONF_CLEAN_INSTANCE_PATH" );
		Assert.assertEquals( "ev5", handler.eventName );
		Assert.assertEquals( "parent_child", handler.containerName );

		handler.reset( childInstance, "ev6", "ROBOCONF_CLEAN_REVERSED_INSTANCE_PATH" );
		Assert.assertEquals( "ev6", handler.eventName );
		Assert.assertEquals( "child_parent", handler.containerName );

		handler.reset( childInstance, "ev7", null );
		Assert.assertEquals( "ev7", handler.eventName );
		Assert.assertEquals( "child_parent", handler.containerName );
	}


	@Test
	public void testContainerIsRunning_notRunning() {

		// No matter if Docker is running or not.
		// This test should always succeed.
		DockerMonitoringHandler handler = new DockerMonitoringHandler();
		Assert.assertFalse( handler.containerIsRunning( "unexisting_sdfnklioef54rgjef" ));
	}


	@Test
	public void testProcess() {

		DockerMonitoringHandler handler = new DockerMonitoringHandler();
		handler.setAgentId( "my-app", "/root" );
		handler.reset( new Instance( "inst" ).component( new Component( "c" )), "ev1", null );

		handler = Mockito.spy( handler );

		Mockito.when( handler.containerIsRunning( "inst" )).thenReturn( false );
		MsgNotifAutonomic msg = handler.process();
		Assert.assertEquals( "my-app", msg.getApplicationName());
		Assert.assertEquals( "/root", msg.getScopedInstancePath());
		Assert.assertEquals( "ev1", msg.getEventName());
		Assert.assertNull( msg.getEventInfo());

		Mockito.when( handler.containerIsRunning( "inst" )).thenReturn( true );
		Assert.assertNull( handler.process());
	}
}
