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
import static net.roboconf.target.embedded.internal.EmbeddedHandler.TARGET_ID;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import net.roboconf.core.model.beans.Instance;
import net.roboconf.target.api.AbstractThreadedTargetHandler.MachineConfigurator;
import net.roboconf.target.api.TargetException;
import net.roboconf.target.api.TargetHandlerParameters;

/**
 * @author Vincent Zurczak - Linagora
 */
public class EmbeddedHandlerTest {

	private static final String NOTHING = "app_nothing";


	@Test
	public void testTargetEmbedded_noIpPool_basics() throws Exception {

		// Basics
		EmbeddedHandler target = new EmbeddedHandler();
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

		// Terminate machine should not throw any error at this stage
		TargetHandlerParameters parameters = new TargetHandlerParameters();
		parameters.setTargetProperties( new HashMap<String,String>( 0 ));
		parameters.getTargetProperties().put( IP_ADDRESSES, "192.168.1.1, 192.168.1.2" );
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
		Assert.assertTrue( target.usedIps.containsKey( "192.168.1.1" ));
		Assert.assertEquals( 1, target.machineIdToIp.size());
		Assert.assertEquals( "192.168.1.1", target.machineIdToIp.get( machineId ));

		// Configuration
		Assert.assertEquals( 0, target.getMachineIdToConfigurators().size());
		target.configureMachine( parameters, machineId );
		Assert.assertEquals( 1, target.getMachineIdToConfigurators().size());
		Assert.assertEquals( machineId, target.getMachineIdToConfigurators().keySet().iterator().next());
		Assert.assertEquals( 0, target.getCancelledMachineIds().size());

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
	}


	@Test( expected = TargetException.class )
	public void testIpList_noMoreIpAvailable() throws Exception {

		Map<String,String> targetProperties = new HashMap<> ();
		targetProperties.put( IP_ADDRESSES, "192.168.1.1, 192.168.1.2" );

		EmbeddedHandler target = new EmbeddedHandler();
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
}
