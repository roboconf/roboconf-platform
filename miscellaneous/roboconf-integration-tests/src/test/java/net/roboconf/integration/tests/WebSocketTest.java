/**
 * Copyright 2014-2015 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.integration.tests;

import java.net.URI;

import junit.framework.Assert;
import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.utils.UriUtils;
import net.roboconf.integration.probes.AbstractTest;
import net.roboconf.integration.probes.DmTest;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.ProbeBuilder;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;

/**
 * @author Vincent Zurczak - Linagora
 */
@RunWith( PaxExam.class )
@ExamReactorStrategy( PerMethod.class )
public class WebSocketTest extends DmTest {

	@ProbeBuilder
	public TestProbeBuilder probeConfiguration( TestProbeBuilder probe ) {

		// We need to specify the classes we need
		// and that come from external modules.
		probe.addTest( AbstractTest.class );
		probe.addTest( DmTest.class );
		probe.addTest( TestUtils.class );

		return probe;
	}


	@Override
	@Test
	public void run() throws Exception {

		// Wait for the REST services to be online.
		// By default, these tests only wait for the manager to be available. We must in addition,
		// be sure that the REST services are online. The most simple solution is to wait for the
		// applications listing to work.
		URI targetUri = UriUtils.urlToUri( "http://localhost:8181/applications" );
		for( int i=0; i<10; i++ ) {
			Thread.sleep( 1000 );
			String s = TestUtils.readUriContent( targetUri );
			if( "[]".equals( s ))
				break;
		}

		// Try to connect to our web socket.
		WebSocketClient client = new WebSocketClient();
		TestWebsocket socket = new TestWebsocket();
        try {
            client.start();
            URI echoUri = new URI( "ws://localhost:8181/roboconf-dm-websocket" );
            ClientUpgradeRequest request = new ClientUpgradeRequest();
            client.connect( socket, echoUri, request );
            Thread.sleep( 2000 );

		} finally {
			client.stop();
		}

        // Did the connection work?
        Assert.assertTrue( socket.wasConnected());
	}


	/**
	 * @author Vincent Zurczak - Linagora
	 */
    static final class TestWebsocket extends WebSocketAdapter {
    	private boolean wasConnected = false;

    	@Override
    	public void onWebSocketConnect( Session sess ) {
    		this.wasConnected = true;
    	}

		public boolean wasConnected() {
			return this.wasConnected;
		}
    }
}
