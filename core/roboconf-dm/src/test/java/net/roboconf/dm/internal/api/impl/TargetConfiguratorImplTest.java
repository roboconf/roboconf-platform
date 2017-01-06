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

package net.roboconf.dm.internal.api.impl;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.dm.internal.api.impl.TargetConfiguratorImpl.ConfigurationRunnable;
import net.roboconf.dm.internal.api.impl.TargetConfiguratorImpl.ProgramUtilsProxy;
import net.roboconf.dm.management.api.ITargetHandlerResolver;
import net.roboconf.target.api.TargetException;
import net.roboconf.target.api.TargetHandler;
import net.roboconf.target.api.TargetHandlerParameters;

/**
 * @author Vincent Zurczak - Linagora
 */
public class TargetConfiguratorImplTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private TargetConfiguratorImpl targetConfigurator;
	private ExecutorService executor;


	@Before
	public void configure() {

		this.executor = Mockito.mock( ExecutorService.class );
		this.targetConfigurator = new TargetConfiguratorImpl();
		this.targetConfigurator.executor = this.executor;
	}


	@Test
	public void testStartAndStop() {

		this.targetConfigurator.start();
		Assert.assertNotNull( this.targetConfigurator.executor );
		Assert.assertNotEquals( this.executor, this.targetConfigurator.executor );

		this.targetConfigurator.stop();
		Assert.assertNull( this.targetConfigurator.executor );

		// Invoking "stop" several times does not throw any error
		this.targetConfigurator.stop();
	}


	@Test
	public void testReportCandidate_sameInstancePath_differentApplications() throws Exception {

		for( int i=1; i<4; i++ ) {
			TestApplication app = new TestApplication();
			app.setName( "app" + i );

			TargetHandlerParameters parameters = new TargetHandlerParameters()
					.applicationName( app.getName())
					.scopedInstancePath( InstanceHelpers.computeInstancePath( app.getMySqlVm()))
					.domain( "domain" )
					.targetConfigurationScript( this.folder.newFile());

			this.targetConfigurator.reportCandidate( parameters, app.getMySqlVm());
			Assert.assertEquals( i, this.targetConfigurator.candidates.size());
		}
	}


	@Test
	public void testReportCandidate_noConfigScript() throws Exception {

		TestApplication app = new TestApplication();
		app.setName( "app" );

		TargetHandlerParameters parameters = new TargetHandlerParameters()
				.applicationName( app.getName())
				.scopedInstancePath( InstanceHelpers.computeInstancePath( app.getMySqlVm()))
				.domain( "domain" )
				.targetConfigurationScript( null );

		this.targetConfigurator.reportCandidate( parameters, app.getMySqlVm());
		Assert.assertEquals( 1, this.targetConfigurator.candidates.size());
	}


	@Test
	public void testReportCandidate_inexistingConfigScript() throws Exception {

		TestApplication app = new TestApplication();
		app.setName( "app" );

		File f = this.folder.newFile();
		Assert.assertTrue( f.delete());

		TargetHandlerParameters parameters = new TargetHandlerParameters()
				.applicationName( app.getName())
				.scopedInstancePath( InstanceHelpers.computeInstancePath( app.getMySqlVm()))
				.domain( "domain" )
				.targetConfigurationScript( f );

		this.targetConfigurator.reportCandidate( parameters, app.getMySqlVm());
		Assert.assertEquals( 1, this.targetConfigurator.candidates.size());
	}


	@Test
	public void testCancelCandidate() throws Exception {

		TestApplication app = new TestApplication();
		app.setName( "app" );

		TargetHandlerParameters parameters1 = new TargetHandlerParameters()
				.applicationName( app.getName())
				.scopedInstancePath( InstanceHelpers.computeInstancePath( app.getMySqlVm()))
				.domain( "domain" );

		TargetHandlerParameters parameters2 = new TargetHandlerParameters()
				.applicationName( app.getName())
				.scopedInstancePath( InstanceHelpers.computeInstancePath( app.getTomcatVm()))
				.domain( "domain" );

		this.targetConfigurator.reportCandidate( parameters1, app.getMySqlVm());
		Assert.assertEquals( 1, this.targetConfigurator.candidates.size());

		this.targetConfigurator.reportCandidate( parameters2, app.getTomcatVm());
		Assert.assertEquals( 2, this.targetConfigurator.candidates.size());

		this.targetConfigurator.cancelCandidate( parameters1, app.getMySqlVm());
		Assert.assertEquals( 1, this.targetConfigurator.candidates.size());

		this.targetConfigurator.cancelCandidate( parameters2, app.getTomcatVm());
		Assert.assertEquals( 0, this.targetConfigurator.candidates.size());

		this.targetConfigurator.cancelCandidate( parameters2, app.getTomcat());
		Assert.assertEquals( 0, this.targetConfigurator.candidates.size());
	}


	@Test
	public void testVerifyCandidates_noExecutor() {

		this.targetConfigurator.verifyCandidates();
		Mockito.verifyZeroInteractions( this.executor );
	}


	@Test
	public void testVerifyCandidates_noMarker() throws Exception {

		// Add a candidates
		TestApplication app = new TestApplication();
		app.setName( "app" );

		TargetHandlerParameters parameters = new TargetHandlerParameters()
				.applicationName( app.getName())
				.scopedInstancePath( InstanceHelpers.computeInstancePath( app.getMySqlVm()))
				.domain( "domain" )
				.targetConfigurationScript( this.folder.newFile());

		this.targetConfigurator.reportCandidate( parameters, app.getMySqlVm());
		Assert.assertEquals( 1, this.targetConfigurator.candidates.size());

		// Verify the candidates
		this.targetConfigurator.verifyCandidates();

		// Still here
		Assert.assertEquals( 1, this.targetConfigurator.candidates.size());

		// Nothing scheduled
		Mockito.verifyZeroInteractions( this.executor );
	}


	@Test
	public void testVerifyCandidates_withMarker_everythingOk() throws Exception {

		// Add a candidates
		TestApplication app = new TestApplication();
		app.setName( "app" );

		TargetHandlerParameters parameters = new TargetHandlerParameters()
				.applicationName( app.getName())
				.scopedInstancePath( InstanceHelpers.computeInstancePath( app.getMySqlVm()))
				.domain( "domain" )
				.targetConfigurationScript( this.folder.newFile());

		this.targetConfigurator.setTargetHandlerResolver( Mockito.mock( ITargetHandlerResolver.class ));
		this.targetConfigurator.reportCandidate( parameters, app.getMySqlVm());
		Assert.assertEquals( 1, this.targetConfigurator.candidates.size());

		// Mark the instance
		app.getMySqlVm().data.put( Instance.READY_FOR_CFG_MARKER, "true" );

		// Make sure it is not undeployed
		app.getMySqlVm().setStatus( InstanceStatus.DEPLOYING );

		// Verify the candidates
		this.targetConfigurator.verifyCandidates();

		// Not here anymore
		Assert.assertEquals( 0, this.targetConfigurator.candidates.size());

		// No more markers
		Assert.assertFalse( app.getMySqlVm().data.containsKey( Instance.READY_FOR_CFG_MARKER ));

		// Something was scheduled
		Mockito.verify( this.executor, Mockito.only()).execute( Mockito.any( ConfigurationRunnable.class ));
	}


	@Test
	public void testVerifyCandidates_withMarker_inexistingScript() throws Exception {

		// Add a candidates
		TestApplication app = new TestApplication();
		app.setName( "app" );

		File f =  this.folder.newFile();
		TargetHandlerParameters parameters = new TargetHandlerParameters()
				.applicationName( app.getName())
				.scopedInstancePath( InstanceHelpers.computeInstancePath( app.getMySqlVm()))
				.domain( "domain" )
				.targetConfigurationScript( f );

		this.targetConfigurator.setTargetHandlerResolver( Mockito.mock( ITargetHandlerResolver.class ));
		this.targetConfigurator.reportCandidate( parameters, app.getMySqlVm());
		Assert.assertEquals( 1, this.targetConfigurator.candidates.size());

		// Mark the instance
		app.getMySqlVm().data.put( Instance.READY_FOR_CFG_MARKER, "true" );

		// Delete the script
		Assert.assertTrue( f.delete());

		// Verify the candidates
		this.targetConfigurator.verifyCandidates();

		// Not here anymore (e.g. the target was deleted)
		Assert.assertEquals( 0, this.targetConfigurator.candidates.size());

		// No more markers
		Assert.assertFalse( app.getMySqlVm().data.containsKey( Instance.READY_FOR_CFG_MARKER ));

		// Nothing scheduled
		Mockito.verifyZeroInteractions( this.executor );
	}


	@Test
	public void testVerifyCandidates_withMarker_noScript() throws Exception {

		// Add a candidates
		TestApplication app = new TestApplication();
		app.setName( "app" );

		TargetHandlerParameters parameters = new TargetHandlerParameters()
				.applicationName( app.getName())
				.scopedInstancePath( InstanceHelpers.computeInstancePath( app.getMySqlVm()))
				.domain( "domain" );

		this.targetConfigurator.setTargetHandlerResolver( Mockito.mock( ITargetHandlerResolver.class ));
		this.targetConfigurator.reportCandidate( parameters, app.getMySqlVm());
		Assert.assertEquals( 1, this.targetConfigurator.candidates.size());

		// Mark the instance
		app.getMySqlVm().data.put( Instance.READY_FOR_CFG_MARKER, "true" );

		// Verify the candidates
		this.targetConfigurator.verifyCandidates();

		// Not here anymore (e.g. the target was deleted)
		Assert.assertEquals( 0, this.targetConfigurator.candidates.size());

		// No more markers
		Assert.assertFalse( app.getMySqlVm().data.containsKey( Instance.READY_FOR_CFG_MARKER ));

		// Nothing scheduled
		Mockito.verifyZeroInteractions( this.executor );
	}


	@Test
	public void testVerifyCandidates_withMarker_noTargetHandler() throws Exception {

		// Add a candidates
		TestApplication app = new TestApplication();
		app.setName( "app" );

		TargetHandlerParameters parameters = new TargetHandlerParameters()
				.applicationName( app.getName())
				.scopedInstancePath( InstanceHelpers.computeInstancePath( app.getMySqlVm()))
				.domain( "domain" )
				.targetConfigurationScript( this.folder.newFile());

		this.targetConfigurator.reportCandidate( parameters, app.getMySqlVm());
		Assert.assertEquals( 1, this.targetConfigurator.candidates.size());

		// Mark the instance
		app.getMySqlVm().data.put( Instance.READY_FOR_CFG_MARKER, "true" );

		// Verify the candidates
		this.targetConfigurator.verifyCandidates();

		// Not a candidate anymore
		Assert.assertEquals( 0, this.targetConfigurator.candidates.size());

		// No more markers
		Assert.assertFalse( app.getMySqlVm().data.containsKey( Instance.READY_FOR_CFG_MARKER ));

		// Nothing scheduled
		Mockito.verifyZeroInteractions( this.executor );
	}


	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void testConfigurationRunnable_run_ok() throws Exception {

		// Prepare
		TestApplication app = new TestApplication();
		app.setName( "app" );

		String machineId = "sdg5465sdf1";
		app.getMySqlVm().data.put( Instance.MACHINE_ID, machineId );
		File scriptFile = this.folder.newFile();

		TargetHandlerParameters parameters = new TargetHandlerParameters()
				.applicationName( app.getName())
				.scopedInstancePath( InstanceHelpers.computeInstancePath( app.getMySqlVm()))
				.domain( "domain" )
				.targetConfigurationScript( scriptFile )
				.messagingProperties( new HashMap<String,String>( 0 ))
				.targetProperties( new HashMap<String,String>( 0 ));

		TargetHandler targetHandler = Mockito.mock( TargetHandler.class );
		Mockito.when( targetHandler.retrievePublicIpAddress( parameters, machineId )).thenReturn( "127.0.0.1" );

		ProgramUtilsProxy programUtils = Mockito.mock( ProgramUtilsProxy.class );
		ITargetHandlerResolver thResolver = Mockito.mock( ITargetHandlerResolver.class );
		Mockito.when( thResolver.findTargetHandler( parameters.getTargetProperties())).thenReturn( targetHandler );

		// Execute
		ConfigurationRunnable runnable = new ConfigurationRunnable( parameters, app.getMySqlVm(), thResolver );
		runnable.programUtils = programUtils;
		runnable.run();

		// Verify
		Mockito.verify( thResolver, Mockito.only()).findTargetHandler( parameters.getTargetProperties());
		Mockito.verify( targetHandler, Mockito.only()).retrievePublicIpAddress( parameters, machineId );

		ArgumentCaptor<Map<String,String>> environmentVarsArg = ArgumentCaptor.forClass((Class) Map.class );
		Mockito.verify( programUtils, Mockito.only()).executeCommand(
				Mockito.any( Logger.class ),
				Mockito.eq( new String[] { scriptFile.getAbsolutePath()}),
				Mockito.eq( scriptFile.getParentFile()),
				environmentVarsArg.capture(),
				Mockito.eq( app.getName()),
				Mockito.eq( parameters.getScopedInstancePath()));

		Map<String,String> environmentVars = environmentVarsArg.getValue();
		Assert.assertNotNull( environmentVars );
		Assert.assertEquals( "127.0.0.1", environmentVars.get( "IP_ADDRESS" ));
		Assert.assertEquals( app.getName(), environmentVars.get( "APPLICATION_NAME" ));
		Assert.assertEquals( parameters.getScopedInstancePath(), environmentVars.get( "SCOPED_INSTANCE_PATH" ));
		Assert.assertEquals( "domain", environmentVars.get( "DOMAIN" ));
		Assert.assertNotNull( environmentVars.get( "USER_DATA" ));
		Assert.assertEquals( 5, environmentVars.size());
	}


	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void testConfigurationRunnable_run_ok_noMachineId_noIp() throws Exception {

		// Prepare
		TestApplication app = new TestApplication();
		app.setName( "app" );

		File scriptFile = this.folder.newFile();
		TargetHandlerParameters parameters = new TargetHandlerParameters()
				.applicationName( app.getName())
				.scopedInstancePath( InstanceHelpers.computeInstancePath( app.getMySqlVm()))
				.domain( "domain" )
				.targetConfigurationScript( scriptFile )
				.messagingProperties( new HashMap<String,String>( 0 ))
				.targetProperties( new HashMap<String,String>( 0 ));

		TargetHandler targetHandler = Mockito.mock( TargetHandler.class );
		ProgramUtilsProxy programUtils = Mockito.mock( ProgramUtilsProxy.class );
		ITargetHandlerResolver thResolver = Mockito.mock( ITargetHandlerResolver.class );
		Mockito.when( thResolver.findTargetHandler( parameters.getTargetProperties())).thenReturn( targetHandler );

		// Execute
		ConfigurationRunnable runnable = new ConfigurationRunnable( parameters, app.getMySqlVm(), thResolver );
		runnable.programUtils = programUtils;
		runnable.run();

		// Verify
		Mockito.verify( thResolver, Mockito.only()).findTargetHandler( parameters.getTargetProperties());
		Mockito.verify( targetHandler, Mockito.only()).retrievePublicIpAddress( parameters, null );

		ArgumentCaptor<Map<String,String>> environmentVarsArg = ArgumentCaptor.forClass((Class) Map.class );
		Mockito.verify( programUtils, Mockito.only()).executeCommand(
				Mockito.any( Logger.class ),
				Mockito.eq( new String[] { scriptFile.getAbsolutePath()}),
				Mockito.eq( scriptFile.getParentFile()),
				environmentVarsArg.capture(),
				Mockito.eq( app.getName()),
				Mockito.eq( parameters.getScopedInstancePath()));

		Map<String,String> environmentVars = environmentVarsArg.getValue();
		Assert.assertNotNull( environmentVars );
		Assert.assertEquals( "", environmentVars.get( "IP_ADDRESS" ));
		Assert.assertEquals( app.getName(), environmentVars.get( "APPLICATION_NAME" ));
		Assert.assertEquals( parameters.getScopedInstancePath(), environmentVars.get( "SCOPED_INSTANCE_PATH" ));
		Assert.assertEquals( "domain", environmentVars.get( "DOMAIN" ));
		Assert.assertNotNull( environmentVars.get( "USER_DATA" ));
		Assert.assertEquals( 5, environmentVars.size());
	}


	@Test
	public void testConfigurationRunnable_run_noTargetHandler() throws Exception {

		// Prepare
		TestApplication app = new TestApplication();
		app.setName( "app" );

		String machineId = "sdg5465sdf1";
		app.getMySqlVm().data.put( Instance.MACHINE_ID, machineId );

		TargetHandlerParameters parameters = new TargetHandlerParameters()
				.applicationName( app.getName())
				.scopedInstancePath( InstanceHelpers.computeInstancePath( app.getMySqlVm()))
				.domain( "domain" )
				.targetConfigurationScript( this.folder.newFile())
				.messagingProperties( new HashMap<String,String>( 0 ))
				.targetProperties( new HashMap<String,String>( 0 ));

		ITargetHandlerResolver thResolver = Mockito.mock( ITargetHandlerResolver.class );
		ProgramUtilsProxy programUtils = Mockito.mock( ProgramUtilsProxy.class );

		// Execute
		ConfigurationRunnable runnable = new ConfigurationRunnable( parameters, app.getMySqlVm(), thResolver );
		runnable.programUtils = programUtils;
		runnable.run();

		// Verify
		Mockito.verify( thResolver, Mockito.only()).findTargetHandler( parameters.getTargetProperties());
		Mockito.verifyZeroInteractions( programUtils );
	}


	@Test
	public void testConfigurationRunnable_run_inexistingScript() throws Exception {

		// Prepare
		TestApplication app = new TestApplication();
		app.setName( "app" );

		String machineId = "sdg5465sdf1";
		app.getMySqlVm().data.put( Instance.MACHINE_ID, machineId );

		File scriptFile = this.folder.newFile();
		Assert.assertTrue( scriptFile.delete());

		TargetHandlerParameters parameters = new TargetHandlerParameters()
				.applicationName( app.getName())
				.scopedInstancePath( InstanceHelpers.computeInstancePath( app.getMySqlVm()))
				.domain( "domain" )
				.targetConfigurationScript( scriptFile )
				.messagingProperties( new HashMap<String,String>( 0 ))
				.targetProperties( new HashMap<String,String>( 0 ));

		ITargetHandlerResolver thResolver = Mockito.mock( ITargetHandlerResolver.class );
		TargetHandler targetHandler = Mockito.mock( TargetHandler.class );
		ProgramUtilsProxy programUtils = Mockito.mock( ProgramUtilsProxy.class );
		Mockito.when( thResolver.findTargetHandler( parameters.getTargetProperties())).thenReturn( targetHandler );

		// Execute
		ConfigurationRunnable runnable = new ConfigurationRunnable( parameters, app.getMySqlVm(), thResolver );
		runnable.programUtils = programUtils;
		runnable.run();

		// Verify
		Mockito.verify( thResolver, Mockito.only()).findTargetHandler( parameters.getTargetProperties());
		Mockito.verifyZeroInteractions( programUtils );
	}


	@Test
	public void testConfigurationRunnable_run_withException() throws Exception {

		// Prepare
		TestApplication app = new TestApplication();
		app.setName( "app" );

		String machineId = "sdg5465sdf1";
		app.getMySqlVm().data.put( Instance.MACHINE_ID, machineId );

		File scriptFile = this.folder.newFile();
		Assert.assertTrue( scriptFile.delete());

		TargetHandlerParameters parameters = new TargetHandlerParameters()
				.applicationName( app.getName())
				.scopedInstancePath( InstanceHelpers.computeInstancePath( app.getMySqlVm()))
				.domain( "domain" )
				.targetConfigurationScript( scriptFile )
				.messagingProperties( new HashMap<String,String>( 0 ))
				.targetProperties( new HashMap<String,String>( 0 ));

		ITargetHandlerResolver thResolver = Mockito.mock( ITargetHandlerResolver.class );
		TargetHandler targetHandler = Mockito.mock( TargetHandler.class );
		ProgramUtilsProxy programUtils = Mockito.mock( ProgramUtilsProxy.class );
		Mockito.when( thResolver.findTargetHandler( parameters.getTargetProperties())).thenReturn( targetHandler );
		Mockito.doThrow( new TargetException( "for test!" )).when( thResolver ).findTargetHandler( parameters.getTargetProperties());

		// Execute
		ConfigurationRunnable runnable = new ConfigurationRunnable( parameters, app.getMySqlVm(), thResolver );
		runnable.programUtils = programUtils;
		runnable.run();

		// Verify
		Mockito.verify( thResolver, Mockito.only()).findTargetHandler( parameters.getTargetProperties());
		Mockito.verifyZeroInteractions( programUtils );
	}
}
