/**
 * Copyright 2014 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.target.jclouds.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import net.roboconf.core.utils.Utils;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ToRunByHand {

	private static final String PROPS_LOCATION = "/home/vzurczak/Bureau/jclouds.properties";
	private static final String MSG_IP = "whatever";
	private static final String MSG_USER = "roboconf";
	private static final String MSG_PWD = "roboconf";


	/**
	 * A test that starts a new VM, passes user data, waits 5 minutes and terminates the VM.
	 * @param args
	 * @throws Exception
	 */
	public static void main( String args[] ) throws Exception {

		if( ! new File( PROPS_LOCATION ).exists())
			throw new IllegalArgumentException( "The properties file does not exist." );

		Map<String,String> conf = new HashMap<String,String> ();
		Properties p = new Properties();
		InputStream in = null;
		try {
			in = new FileInputStream( PROPS_LOCATION );
			p.load( in );

		} catch( Exception e ) {
			Utils.closeQuietly( in );
		}

		for( Map.Entry<Object,Object> entry : p.entrySet())
			conf.put( entry.getKey().toString(), entry.getValue().toString());

		JCloudsHandler target = new JCloudsHandler();
		String serverId = null;
		try {
			serverId = target.createOrConfigureMachine( conf, MSG_IP, MSG_USER, MSG_PWD, "root", "app" );

			// 5 minutes
			Thread.sleep( 60000 * 5 );

		} finally {
			if( serverId != null )
				target.terminateMachine( conf, serverId );
		}
	}
}
