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

package net.roboconf.target.embedded.internal;

import static net.roboconf.target.embedded.internal.EmbeddedHandler.IP_ADDRESSES;
import static net.roboconf.target.embedded.internal.EmbeddedHandler.PRIVATE_BACKUP_FILE;
import static net.roboconf.target.embedded.internal.EmbeddedHandler.PRIVATE_BACKUP_IDS_TO_IPS;
import static net.roboconf.target.embedded.internal.EmbeddedHandler.PRIVATE_BACKUP_USED_IPS;
import static net.roboconf.target.embedded.internal.EmbeddedHandler.TARGET_ID;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.utils.Utils;
import net.roboconf.target.api.AbstractThreadedTargetHandler.MachineConfigurator;
import net.roboconf.target.api.TargetException;
import net.roboconf.target.api.TargetHandlerParameters;

/**
 * @author Vincent Zurczak - Linagora
 */
public class EmbeddedHandlerTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();
	private static final String NOTHING = "app_nothing";


	@Test
	public void testTargetEmbedded_noIpPool_basics() throws Exception {

		// Basics
		EmbeddedHandler target = new EmbeddedHandler();
		target.karafData = this.folder.newFolder().getAbsolutePath();
		Assert.assertEquals( TARGET_ID, target.getTargetId());

		// Terminate machine should not throw any error at this stage
		Instance scopedInstance = new Instance();
		TargetHandlerParameters parameters = new TargetHandlerParameters();
		parameters.setTargetProperties( new HashMap<String,String>( 0 ));
		parameters.setMessagingProperties( new HashMap<String,String>( 0 ));
		parameters = parameters
				.applicationName( "app" )
				.domain( "domain" )
				.scopedInstancePath( "nothing" )
				.scopedInstance( scopedInstance );

		target.terminateMachine( parameters, null );
		target.terminateMachine( parameters, "anything" );

		// Same thing for isMachineRunning
		Assert.assertFalse( target.isMachineRunning( null, NOTHING ));
	}


	@Test
	public void testTargetEmbedded_noIpPool() throws Exception {

		// Basics
		EmbeddedHandler target = new EmbeddedHandler();
		target.karafData = this.folder.newFolder().getAbsolutePath();
		Assert.assertEquals( TARGET_ID, target.getTargetId());

		// Terminate machine should not throw any error at this stage
		Instance scopedInstance = new Instance();
		TargetHandlerParameters parameters = new TargetHandlerParameters();
		parameters.setTargetProperties( new HashMap<String,String>( 0 ));
		parameters.setMessagingProperties( new HashMap<String,String>( 0 ));
		parameters = parameters
				.applicationName( "app" )
				.domain( "domain" )
				.scopedInstancePath( "nothing" )
				.scopedInstance( scopedInstance );

		// Let's try to create a machine
		String machineId = target.createMachine( parameters );
		Assert.assertEquals( NOTHING, machineId );
		Assert.assertTrue( target.isMachineRunning( null, machineId ));
		Assert.assertEquals( 0, target.usedIps.size());
		Assert.assertEquals( 1, target.machineIdToIp.size());
		Assert.assertEquals( "", target.machineIdToIp.get( machineId ));

		// Configuration
		Assert.assertEquals( 0, target.getMachineIdToConfigurators().size());
		target.configureMachine( parameters, machineId );
		Assert.assertEquals( 0, target.getMachineIdToConfigurators().size());
		Assert.assertEquals( 0, target.getCancelledMachineIds().size());

		// Terminate it
		target.terminateMachine( parameters, machineId );
		Assert.assertFalse( target.isMachineRunning( null, machineId ));
		Assert.assertEquals( 0, target.usedIps.size());
		Assert.assertEquals( 0, target.machineIdToIp.size());
		Assert.assertEquals( 0, target.getMachineIdToConfigurators().size());

		Assert.assertEquals( 1, target.getCancelledMachineIds().size());
		Assert.assertEquals( machineId, target.getCancelledMachineIds().iterator().next());
	}


	@Test
	public void testTargetEmbedded_withIpPool_basics() throws Exception {

		EmbeddedHandler target = new EmbeddedHandler();
		target.karafData = this.folder.newFolder().getAbsolutePath();

		// Terminate machine should not throw any error at this stage
		TargetHandlerParameters parameters = new TargetHandlerParameters();
		parameters.setTargetProperties( new HashMap<String,String>( 0 ));
		parameters.getTargetProperties().put( IP_ADDRESSES, "192.168.1.1, 192.168.1.2" );
		parameters.setMessagingProperties( new HashMap<String,String>( 0 ));
		parameters = parameters
				.applicationName( "app" )
				.domain( "domain" )
				.scopedInstancePath( "nothing" );

		target.terminateMachine( parameters, null );
		target.terminateMachine( parameters, "anything" );

		// Same thing for isMachineRunning
		Assert.assertFalse( target.isMachineRunning( null, NOTHING ));
	}


	@Test
	public void testTargetEmbedded_withIpPool() throws Exception {

		EmbeddedHandler target = new EmbeddedHandler();
		target.karafData = this.folder.newFolder().getAbsolutePath();

		// Terminate machine should not throw any error at this stage
		TargetHandlerParameters parameters = new TargetHandlerParameters();
		parameters.setTargetProperties( new HashMap<String,String>( 0 ));
		parameters.getTargetProperties().put( IP_ADDRESSES, "999.168.1.1, 999.168.1.2" );
		parameters.setMessagingProperties( new HashMap<String,String>( 0 ));
		parameters = parameters
				.applicationName( "app" )
				.domain( "domain" )
				.scopedInstancePath( "nothing" );

		// Let's try to create a machine
		String machineId = target.createMachine( parameters );
		Assert.assertEquals( NOTHING, machineId );
		Assert.assertTrue( target.isMachineRunning( null, machineId ));
		Assert.assertEquals( 1, target.usedIps.size());
		Assert.assertTrue( target.usedIps.containsKey( "999.168.1.1" ));
		Assert.assertEquals( 1, target.machineIdToIp.size());
		Assert.assertEquals( "999.168.1.1", target.machineIdToIp.get( machineId ));

		// Configuration
		Assert.assertEquals( 0, target.getMachineIdToConfigurators().size());
		target.configureMachine( parameters, machineId );
		Assert.assertEquals( 1, target.getMachineIdToConfigurators().size());
		Assert.assertEquals( machineId, target.getMachineIdToConfigurators().keySet().iterator().next());
		Assert.assertEquals( 0, target.getCancelledMachineIds().size());

		// Verify what was saved up in the persisted cache
		final File backup = new File( target.karafData, PRIVATE_BACKUP_FILE );
		Properties props = Utils.readPropertiesFile( backup );
		Assert.assertEquals( "999.168.1.1", props.get( PRIVATE_BACKUP_USED_IPS ));
		Assert.assertEquals( machineId + "=999.168.1.1", props.get( PRIVATE_BACKUP_IDS_TO_IPS ));

		// Verify it is considered as running
		Assert.assertTrue( target.isMachineRunning( parameters, machineId ));
		Assert.assertEquals( "999.168.1.1", target.retrievePublicIpAddress( parameters, machineId ));

		// Terminate it
		target.terminateMachine( parameters, machineId );
		Assert.assertFalse( target.isMachineRunning( null, machineId ));

		// A new job must have been scheduled to unconfigure the agent
		Assert.assertEquals( 1, target.usedIps.size());
		Assert.assertEquals( 0, target.machineIdToIp.size());

		Map<String,MachineConfigurator> jobs = target.getMachineIdToConfigurators();
		Assert.assertEquals( 2, jobs.size());
		Assert.assertEquals( ConfiguratorOnCreation.class, jobs.get( machineId ).getClass());
		Assert.assertEquals( ConfiguratorOnTermination.class, jobs.get( "#STOP# " + machineId ).getClass());

		Assert.assertEquals( 1, target.getCancelledMachineIds().size());
		Assert.assertEquals( machineId, target.getCancelledMachineIds().iterator().next());

		// Verify the persisted cache
		props = Utils.readPropertiesFile( backup );
		Assert.assertEquals( "999.168.1.1", props.get( PRIVATE_BACKUP_USED_IPS ));
		Assert.assertEquals( machineId + "=999.168.1.1", props.get( PRIVATE_BACKUP_IDS_TO_IPS ));

		// Force the execution of the machine configurator
		@SuppressWarnings({ "unchecked" })
		Map<String,MachineConfigurator> machineIdToConfigurators = TestUtils.getInternalField( target, "machineIdToConfigurators", Map.class );
		Assert.assertNotNull( machineIdToConfigurators );

		MachineConfigurator stopConfigurator = machineIdToConfigurators.get( "#STOP# app_nothing" );
		Assert.assertNotNull( stopConfigurator );

		// Force the configurator to run
		try {
			((ConfiguratorOnTermination) stopConfigurator).configure();
			Assert.fail( "The SSH connection should have failed." );

		} catch( Exception e ) {
			// nothing
		}

		props = Utils.readPropertiesFile( backup );
		Assert.assertEquals( "", props.get( PRIVATE_BACKUP_USED_IPS ));
		Assert.assertEquals( "", props.get( PRIVATE_BACKUP_IDS_TO_IPS ));
	}


	@Test( expected = TargetException.class )
	public void testIpList_noMoreIpAvailable() throws Exception {

		Map<String,String> targetProperties = new HashMap<> ();
		targetProperties.put( IP_ADDRESSES, "192.168.1.1, 192.168.1.2" );

		EmbeddedHandler target = new EmbeddedHandler();
		target.karafData = this.folder.newFolder().getAbsolutePath();
		try {
			Assert.assertNotNull( target.createMachine( new TargetHandlerParameters()
				.applicationName( "app" )
				.domain( "domain" )
				.scopedInstancePath( "nothing1" )
				.targetProperties( targetProperties )));

			Assert.assertNotNull( target.createMachine( new TargetHandlerParameters()
					.applicationName( "app" )
					.domain( "domain" )
					.scopedInstancePath( "nothing2" )
					.targetProperties( targetProperties )));

		} catch( Exception e ) {
			Assert.fail( "No exception should have been thrown here." );
		}

		Assert.assertNotNull( target.createMachine( new TargetHandlerParameters()
				.applicationName( "app" )
				.domain( "domain" )
				.scopedInstancePath( "nothing3" )
				.targetProperties( targetProperties )));
	}


	@Test
	public void testIpList_verifyPersistedCache() throws Exception {

		Map<String,String> targetProperties = new HashMap<> ();
		targetProperties.put( IP_ADDRESSES, "192.168.1.1, 192.168.1.2" );

		EmbeddedHandler target = new EmbeddedHandler();
		target.karafData = this.folder.newFolder().getAbsolutePath();

		Assert.assertNotNull( target.createMachine( new TargetHandlerParameters()
				.applicationName( "app" )
				.domain( "domain" )
				.scopedInstancePath( "nothing1" )
				.targetProperties( targetProperties )));

		Assert.assertNotNull( target.createMachine( new TargetHandlerParameters()
				.applicationName( "app" )
				.domain( "domain" )
				.scopedInstancePath( "nothing2" )
				.targetProperties( targetProperties )));

		// Verify what was saved up in the persisted cache
		final File backup = new File( target.karafData, PRIVATE_BACKUP_FILE );
		Properties props = Utils.readPropertiesFile( backup );
		Assert.assertTrue( Arrays.asList(
				"192.168.1.1, 192.168.1.2",
				"192.168.1.2, 192.168.1.1" ).contains( props.get( PRIVATE_BACKUP_USED_IPS )));

		Assert.assertTrue( Arrays.asList(
				"app_nothing1=192.168.1.1, app_nothing2=192.168.1.2",
				"app_nothing2=192.168.1.2, app_nothing1=192.168.1.1" ).contains( props.get( PRIVATE_BACKUP_IDS_TO_IPS )));
	}


	@Test
	public void testCacheRestoration() throws Exception {

		// Create 2 handlers
		EmbeddedHandler target1 = new EmbeddedHandler();
		target1.karafData = this.folder.newFolder().getAbsolutePath();

		EmbeddedHandler target2 = new EmbeddedHandler();
		target2.karafData = target1.karafData;

		// Initialize one
		target1.usedIps.put( "192.168.1.1", 1 );
		target1.usedIps.put( "192.168.1.3", 1 );
		target1.usedIps.put( "192.168.1.5", 1 );
		target1.machineIdToIp.put( "m1", "192.168.1.3" );
		target1.machineIdToIp.put( "m2", "192.168.1.1" );
		target1.machineIdToIp.put( "m3", "192.168.1.5" );

		try {
			// Start should not erase the map
			target1.start();
			Assert.assertEquals( 3, target1.usedIps.size());
			Assert.assertEquals( 3, target1.machineIdToIp.size());

			// Force the save process
			EmbeddedHandler.save( target1 );

			// Now, start the second target and verify the start method restore the values
			Assert.assertEquals( 0, target2.usedIps.size());
			Assert.assertEquals( 0, target2.machineIdToIp.size());
			target2.start();

			Assert.assertEquals( 3, target2.usedIps.size());
			Assert.assertEquals( 3, target2.machineIdToIp.size());

			Assert.assertEquals( target1.usedIps, target2.usedIps );
			Assert.assertEquals( target1.machineIdToIp, target2.machineIdToIp );

		} finally {
			target1.stop();
			target2.stop();
		}
	}
}
