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

package net.roboconf.karaf.commands.dm.logs;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.logging.Level;

import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.management.api.IApplicationMngr;
import net.roboconf.dm.management.api.IMessagingMngr;
import net.roboconf.messaging.api.messages.Message;
import net.roboconf.messaging.api.messages.from_dm_to_agent.MsgCmdGatherLogs;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * @author Vincent Zurczak - Linagora
 */
public class GatherLogsCommandTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private final TestApplication app = new TestApplication();
	private final ManagedApplication ma = new ManagedApplication( this.app );

	private GatherLogsCommand gl;
	private Manager manager;

	private IApplicationMngr applicationMngr;
	private IMessagingMngr messagingMngr;


	@Before
	public void setupCommand() throws Exception {
		this.app.directory( this.folder.newFolder());

		this.applicationMngr = Mockito.mock( IApplicationMngr.class );
		Mockito.when( this.applicationMngr.findManagedApplicationByName( this.app.getName())).thenReturn( this.ma );

		this.manager = Mockito.mock( Manager.class );
		Mockito.when( this.manager.applicationMngr()).thenReturn( this.applicationMngr );

		this.messagingMngr = Mockito.mock( IMessagingMngr.class );
		Mockito.when( this.manager.messagingMngr()).thenReturn( this.messagingMngr );

		this.gl = new GatherLogsCommand();
		this.gl.manager = this.manager;
	}


	@Test
	public void testExecute_invalidApplicationName() throws Exception {

		this.gl.applicationName = "invalid";
		this.gl.scopedInstancePath = null;

		ByteArrayOutputStream os = new ByteArrayOutputStream();
		this.gl.out = new PrintStream( os, true, "UTF-8" );

		this.gl.execute();
		Assert.assertTrue( os.toString( "UTF-8" ).contains( "Unknown application" ));
		Mockito.verify( this.applicationMngr, Mockito.times( 1 )).findManagedApplicationByName( this.gl.applicationName );
		Mockito.verifyZeroInteractions( this.messagingMngr );
	}


	@Test
	public void testExecute_invalidInstance() throws Exception {

		this.gl.applicationName = this.app.getName();
		this.gl.scopedInstancePath = "not here";

		ByteArrayOutputStream os = new ByteArrayOutputStream();
		this.gl.out = new PrintStream( os, true, "UTF-8" );

		this.gl.execute();
		Assert.assertTrue( os.toString( "UTF-8" ).contains( "There is no" ));
		Mockito.verify( this.applicationMngr, Mockito.times( 1 )).findManagedApplicationByName( this.gl.applicationName );
		Mockito.verifyZeroInteractions( this.messagingMngr );
	}


	@Test
	public void testExecute_notAScopedInstance() throws Exception {

		this.gl.applicationName = this.app.getName();
		this.gl.scopedInstancePath = InstanceHelpers.computeInstancePath( this.app.getWar());

		ByteArrayOutputStream os = new ByteArrayOutputStream();
		this.gl.out = new PrintStream( os, true, "UTF-8" );

		this.gl.execute();
		Assert.assertTrue( os.toString( "UTF-8" ).contains( "is not a scoped instance" ));
		Mockito.verify( this.applicationMngr, Mockito.times( 1 )).findManagedApplicationByName( this.gl.applicationName );
		Mockito.verifyZeroInteractions( this.messagingMngr );
	}


	@Test
	public void testExecute_valid_noSpecifiedInstance_notDeployed() throws Exception {

		this.gl.applicationName = this.app.getName();
		this.gl.scopedInstancePath = null;

		ByteArrayOutputStream os = new ByteArrayOutputStream();
		this.gl.out = new PrintStream( os, true, "UTF-8" );

		this.gl.execute();
		Mockito.verify( this.applicationMngr, Mockito.times( 1 )).findManagedApplicationByName( this.gl.applicationName );
		Mockito.verifyZeroInteractions( this.messagingMngr );
	}


	@Test
	public void testExecute_valid_noSpecifiedInstance_allDeployed() throws Exception {

		this.gl.applicationName = this.app.getName();
		this.gl.scopedInstancePath = null;

		for( Instance inst : InstanceHelpers.findAllScopedInstances( this.app ))
			inst.setStatus( InstanceStatus.DEPLOYED_STARTED );

		ByteArrayOutputStream os = new ByteArrayOutputStream();
		this.gl.out = new PrintStream( os, true, "UTF-8" );

		this.gl.execute();
		Mockito.verify( this.applicationMngr, Mockito.times( 1 )).findManagedApplicationByName( this.gl.applicationName );

		ArgumentCaptor<Message> msg = ArgumentCaptor.forClass( Message.class );
		Mockito.verify( this.messagingMngr, Mockito.times( 2 )).sendMessageSafely(
				Mockito.any( ManagedApplication.class ),
				Mockito.any( Instance.class ),
				msg.capture());

		Assert.assertEquals( MsgCmdGatherLogs.class, msg.getValue().getClass());

		String s = os.toString( "UTF-8" ).trim();
		Assert.assertTrue( s.startsWith( "On reception" ));
		Assert.assertTrue( s.endsWith( "/roboconf-logs..." ));
	}


	@Test
	public void testExecute_valid_withSpecifiedInstance_deployed() throws Exception {

		this.gl.applicationName = this.app.getName();
		this.gl.scopedInstancePath = InstanceHelpers.computeInstancePath( this.app.getTomcatVm());
		this.app.getTomcatVm().setStatus( InstanceStatus.DEPLOYED_STARTED );

		// Hack for code coverage
		this.gl.logger.setLevel( Level.FINE );
		// End of hack

		ByteArrayOutputStream os = new ByteArrayOutputStream();
		this.gl.out = new PrintStream( os, true, "UTF-8" );

		this.gl.execute();
		Mockito.verify( this.applicationMngr, Mockito.times( 1 )).findManagedApplicationByName( this.gl.applicationName );

		String s = os.toString( "UTF-8" ).trim();
		Assert.assertTrue( s.startsWith( "On reception" ));
		Assert.assertTrue( s.endsWith( "/roboconf-logs..." ));

		ArgumentCaptor<Message> msg = ArgumentCaptor.forClass( Message.class );
		Mockito.verify( this.messagingMngr, Mockito.times( 1 )).sendMessageSafely(
				Mockito.eq( this.ma ),
				Mockito.eq( this.app.getTomcatVm()),
				msg.capture());

		Assert.assertEquals( MsgCmdGatherLogs.class, msg.getValue().getClass());
	}


	@Test
	public void testExecute_valid_withSpecifiedInstance_notDeployed() throws Exception {

		this.gl.applicationName = this.app.getName();
		this.gl.scopedInstancePath = InstanceHelpers.computeInstancePath( this.app.getTomcatVm());

		// Hack for code coverage
		this.gl.logger.setLevel( Level.FINE );
		// End of hack

		ByteArrayOutputStream os = new ByteArrayOutputStream();
		this.gl.out = new PrintStream( os, true, "UTF-8" );

		this.gl.execute();
		Mockito.verify( this.applicationMngr, Mockito.times( 1 )).findManagedApplicationByName( this.gl.applicationName );
		Mockito.verifyZeroInteractions( this.messagingMngr );

		String s = os.toString( "UTF-8" ).trim();
		Assert.assertTrue( s.startsWith( "No message will be sent to " ));
		Assert.assertTrue( s.contains( "On reception" ));
		Assert.assertTrue( s.endsWith( "/roboconf-logs..." ));
	}
}
