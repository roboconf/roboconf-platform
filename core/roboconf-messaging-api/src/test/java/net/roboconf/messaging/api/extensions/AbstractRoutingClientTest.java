/**
 * Copyright 2015-2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.messaging.api.extensions;

import java.io.IOException;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import net.roboconf.messaging.api.extensions.MessagingContext.RecipientKind;
import net.roboconf.messaging.api.jmx.RoboconfMessageQueue;
import net.roboconf.messaging.api.messages.Message;

/**
 * @author Vincent Zurczak - Linagora
 */
public class AbstractRoutingClientTest {

	@Test
	public void testCanProceed() {

		// A connection is required.
		CanProceedTest c1 = new CanProceedTest( true );
		c1.connected.set( true );
		Assert.assertTrue( c1.canProceed());

		c1.connected.set( false );
		Assert.assertFalse( c1.canProceed());

		// We do not care about the connection.
		CanProceedTest c2 = new CanProceedTest( false );
		c2.connected.set( true );
		Assert.assertTrue( c2.canProceed());

		c2.connected.set( false );
		Assert.assertTrue( c2.canProceed());
	}


	/**
	 * @author Vincent Zurczak - Linagora
	 */
	private static class CanProceedTest extends AbstractRoutingClient<String> {

		/**
		 * Constructor.
		 * @param connectionIsRequired
		 */
		public CanProceedTest( boolean connectionIsRequired ) {
			super( null, RecipientKind.DM );
			this.connectionIsRequired = connectionIsRequired;
		}

		@Override
		public void setMessageQueue( RoboconfMessageQueue messageQueue ) {
			// nothing
		}

		@Override
		protected Map<String,String> getStaticContextToObject() {
			return null;
		}

		@Override
		protected void process( String obj, Message message ) throws IOException {
			// nothing
		}

		@Override
		public String getMessagingType() {
			return "whatever";
		}
	}
}
