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

package net.roboconf.karaf.commands.dm.diagnostics;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.runtime.EventType;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.management.api.IApplicationMngr;
import net.roboconf.dm.management.api.IDebugMngr;
import net.roboconf.karaf.commands.dm.diagnostics.DiagnosticMessagingCommand.DiagnosticListener;

/**
 * @author Vincent Zurczak - Linagora
 */
public class DiagnosticMessagingCommandTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private final TestApplication app = new TestApplication();
	private final ManagedApplication ma = new ManagedApplication( this.app );

	private DiagnosticMessagingCommand cmd;
	private Manager manager;

	private IApplicationMngr applicationMngr;
	private IDebugMngr debugMngr;


	@Before
	public void setupCommand() throws Exception {
		this.app.directory( this.folder.newFolder());

		this.debugMngr = Mockito.mock( IDebugMngr.class );
		this.manager = Mockito.mock( Manager.class );
		Mockito.when( this.manager.debugMngr()).thenReturn( this.debugMngr );

		this.applicationMngr = Mockito.mock( IApplicationMngr.class );
		Mockito.when( this.applicationMngr.findManagedApplicationByName( this.app.getName())).thenReturn( this.ma );
		Mockito.when( this.manager.applicationMngr()).thenReturn( this.applicationMngr );

		this.cmd = new DiagnosticMessagingCommand();
		this.cmd.manager = this.manager;
		this.cmd.waitingDelay = 0;
	}


	@Test
	public void testExecute_invalidApplicationName() throws Exception {

		this.cmd.applicationName = "invalid";
		this.cmd.scopedInstancePath = null;

		ByteArrayOutputStream os = new ByteArrayOutputStream();
		this.cmd.out = new PrintStream( os, true, "UTF-8" );

		this.cmd.execute();
		Assert.assertTrue( os.toString( "UTF-8" ).contains( "Unknown application" ));
		Mockito.verify( this.applicationMngr, Mockito.times( 1 )).findManagedApplicationByName( this.cmd.applicationName );
		Mockito.verifyZeroInteractions( this.debugMngr );
	}


	@Test
	public void testExecute_invalidInstance() throws Exception {

		this.cmd.applicationName = this.app.getName();
		this.cmd.scopedInstancePath = "not here";

		ByteArrayOutputStream os = new ByteArrayOutputStream();
		this.cmd.out = new PrintStream( os, true, "UTF-8" );

		this.cmd.execute();
		Assert.assertTrue( os.toString( "UTF-8" ).contains( "There is no" ));
		Mockito.verify( this.applicationMngr, Mockito.times( 1 )).findManagedApplicationByName( this.cmd.applicationName );
		Mockito.verifyZeroInteractions( this.debugMngr );
	}


	@Test
	public void testExecute_notAScopedInstance() throws Exception {

		this.cmd.applicationName = this.app.getName();
		this.cmd.scopedInstancePath = InstanceHelpers.computeInstancePath( this.app.getWar());

		ByteArrayOutputStream os = new ByteArrayOutputStream();
		this.cmd.out = new PrintStream( os, true, "UTF-8" );

		this.cmd.execute();
		Assert.assertTrue( os.toString( "UTF-8" ).contains( "is not a scoped instance" ));
		Mockito.verify( this.applicationMngr, Mockito.times( 1 )).findManagedApplicationByName( this.cmd.applicationName );
		Mockito.verifyZeroInteractions( this.debugMngr );
	}


	@Test
	public void testExecute_valid_noSpecifiedInstance_notDeployed() throws Exception {

		this.cmd.applicationName = this.app.getName();
		this.cmd.scopedInstancePath = null;

		ByteArrayOutputStream os = new ByteArrayOutputStream();
		this.cmd.out = new PrintStream( os, true, "UTF-8" );

		this.cmd.execute();
		Mockito.verify( this.applicationMngr, Mockito.times( 1 )).findManagedApplicationByName( this.cmd.applicationName );
		Mockito.verify( this.debugMngr, Mockito.times( 1 )).pingMessageQueue( Mockito.anyString());
	}


	@Test
	public void testExecute_valid_noSpecifiedInstance_allDeployed() throws Exception {

		this.cmd.applicationName = this.app.getName();
		this.cmd.scopedInstancePath = null;

		for( Instance inst : InstanceHelpers.findAllScopedInstances( this.app ))
			inst.setStatus( InstanceStatus.DEPLOYED_STARTED );

		ByteArrayOutputStream os = new ByteArrayOutputStream();
		this.cmd.out = new PrintStream( os, true, "UTF-8" );

		this.cmd.execute();
		Mockito.verify( this.applicationMngr, Mockito.times( 1 )).findManagedApplicationByName( this.cmd.applicationName );
		Mockito.verify( this.debugMngr, Mockito.times( 1 )).pingMessageQueue( Mockito.anyString());
		Mockito.verify( this.debugMngr, Mockito.times( 2 )).pingAgent(
				Mockito.any( ManagedApplication.class ),
				Mockito.any( Instance.class ),
				Mockito.anyString());

		String s = os.toString( "UTF-8" ).trim();
		Assert.assertTrue( s.startsWith( "Pinging the DM..." ));
		Assert.assertTrue( s.contains( "Pinging agent" ));
		Assert.assertFalse( s.contains( "null" ));
		Assert.assertTrue( s.contains( "[ FAILURE ]" ));
		Assert.assertFalse( s.contains( "[ SUCCESS ]" ));
	}


	@Test
	public void testExecute_valid_withSpecifiedInstance_deployed() throws Exception {

		this.cmd.applicationName = this.app.getName();
		this.cmd.scopedInstancePath = InstanceHelpers.computeInstancePath( this.app.getTomcatVm());
		this.app.getTomcatVm().setStatus( InstanceStatus.DEPLOYED_STARTED );

		ByteArrayOutputStream os = new ByteArrayOutputStream();
		this.cmd.out = new PrintStream( os, true, "UTF-8" );

		this.cmd.execute();
		Mockito.verify( this.applicationMngr, Mockito.times( 1 )).findManagedApplicationByName( this.cmd.applicationName );
		Mockito.verify( this.debugMngr, Mockito.times( 1 )).pingMessageQueue( Mockito.anyString());
		Mockito.verify( this.debugMngr, Mockito.times( 1 )).pingAgent(
				Mockito.eq( this.ma ),
				Mockito.eq( this.app.getTomcatVm()),
				Mockito.anyString());

		String s = os.toString( "UTF-8" ).trim();
		Assert.assertTrue( s.startsWith( "Pinging the DM..." ));
		Assert.assertTrue( s.contains( "Pinging agent" ));
		Assert.assertFalse( s.contains( "null" ));
		Assert.assertTrue( s.contains( "[ FAILURE ]" ));
		Assert.assertFalse( s.contains( "[ SUCCESS ]" ));
	}


	@Test
	public void testExecute_valid_withSpecifiedInstance_deployed_pingResponded() throws Exception {

		this.cmd.applicationName = this.app.getName();
		this.cmd.scopedInstancePath = InstanceHelpers.computeInstancePath( this.app.getTomcatVm());
		this.app.getTomcatVm().setStatus( InstanceStatus.DEPLOYED_STARTED );

		ByteArrayOutputStream os = new ByteArrayOutputStream();
		this.cmd.out = new PrintStream( os, true, "UTF-8" );

		this.cmd.waitingDelay = 2;
		new Thread() {
			@Override
			public void run() {

				try {
					Thread.sleep( 1000 );
					for( String uuid : DiagnosticMessagingCommandTest.this.cmd.uuidToTarget.keySet())
						DiagnosticMessagingCommandTest.this.cmd.uuidToFound.put( uuid, Boolean.TRUE );

				} catch( InterruptedException e ) {
					e.printStackTrace();
				}
			};
		}.start();

		this.cmd.execute();
		Mockito.verify( this.applicationMngr, Mockito.times( 1 )).findManagedApplicationByName( this.cmd.applicationName );
		Mockito.verify( this.debugMngr, Mockito.times( 1 )).pingMessageQueue( Mockito.anyString());
		Mockito.verify( this.debugMngr, Mockito.times( 1 )).pingAgent(
				Mockito.eq( this.ma ),
				Mockito.eq( this.app.getTomcatVm()),
				Mockito.anyString());

		String s = os.toString( "UTF-8" ).trim();
		Assert.assertTrue( s.startsWith( "Pinging the DM..." ));
		Assert.assertTrue( s.contains( "Pinging agent" ));
		Assert.assertFalse( s.contains( "null" ));
		Assert.assertFalse( s.contains( "[ FAILURE ]" ));
		Assert.assertTrue( s.contains( "[ SUCCESS ]" ));
	}


	@Test
	public void testDiagnosticListener() {

		Map<String,Boolean> uuidToFound = new HashMap<> ();
		DiagnosticListener listener = new DiagnosticListener( uuidToFound );

		// Code coverage
		listener.application( Mockito.mock( Application.class ), EventType.CREATED );
		listener.applicationTemplate( Mockito.mock( ApplicationTemplate.class ), EventType.CREATED );
		listener.instance( Mockito.mock( Instance.class ), Mockito.mock( Application.class ), EventType.CREATED );
		listener.enableNotifications();
		listener.disableNotifications();
		Assert.assertNotNull( listener.getId());

		// Raw method
		Assert.assertEquals( 0, uuidToFound.size());

		listener.raw( "nothing" );
		Assert.assertEquals( 1, uuidToFound.size());
		Assert.assertTrue( uuidToFound.get( "nothing" ));

		listener.raw( "PONG:something" );
		Assert.assertEquals( 2, uuidToFound.size());
		Assert.assertTrue( uuidToFound.get( "nothing" ));
		Assert.assertTrue( uuidToFound.get( "something" ));
	}
}
