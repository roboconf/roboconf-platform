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

package net.roboconf.target.openstack.internal;

import static net.roboconf.messaging.api.MessagingConstants.FACTORY_TEST;
import static net.roboconf.messaging.api.MessagingConstants.MESSAGING_TYPE_PROPERTY;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.utils.Utils;
import net.roboconf.target.api.TargetHandlerParameters;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ToRunByHand {

	private static final String PROPS_LOCATION = "/data1/targets/openstack.ow2.properties";

	/**
	 * A test that starts a new VM, passes user data, waits 5 minutes and terminates the VM.
	 * @throws Exception
	 */
	public static void main( String args[] ) throws Exception {

		if( ! new File( PROPS_LOCATION ).exists())
			throw new IllegalArgumentException( "The properties file does not exist." );

		Map<String,String> conf = new HashMap<>();
		Properties p = Utils.readPropertiesFile( new File( PROPS_LOCATION ));
		for( Map.Entry<Object,Object> entry : p.entrySet())
			conf.put( entry.getKey().toString(), entry.getValue().toString());

		Map<String, String> msgCfg = Collections.singletonMap(MESSAGING_TYPE_PROPERTY, FACTORY_TEST);
		TargetHandlerParameters parameters = new TargetHandlerParameters()
				.targetProperties( conf )
				.messagingProperties( msgCfg )
				.scopedInstancePath( "root" )
				.scopedInstance( new Instance( "root" ))
				.applicationName( "app" )
				.domain( "def-domain" );

		String serverId = null;
		OpenstackIaasHandler target = new OpenstackIaasHandler();
		target.start();
		try {
			serverId = target.createMachine( parameters );
			target.configureMachine( parameters, serverId );

			// 1 minute
			Thread.sleep( 60000 );

			System.out.print( "Check about machine " + serverId );
			if( target.isMachineRunning( parameters, serverId ))
				System.out.println( ": it is running. IP = " + target.retrievePublicIpAddress( parameters, serverId ));
			else
				System.out.println( ": it does NOT run." );

			// 4 minutes
			Thread.sleep( 60000 * 1 );

		} catch( Exception e ) {
			e.printStackTrace();

		} finally {
			if( serverId != null )
				target.terminateMachine( parameters, serverId );

			target.stop();
		}
	}
}
