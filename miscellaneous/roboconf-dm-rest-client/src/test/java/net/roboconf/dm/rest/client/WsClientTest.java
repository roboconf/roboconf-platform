/**
 * Copyright 2014 Linagora, Université Joseph Fourier
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

package net.roboconf.dm.rest.client;

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class WsClientTest {

	@Test
	public void testDestroy() {

		// There is no real service running, we just test the client creation and destruction
		WsClient client = new WsClient( "http://localhost:9998" );
		client.destroy();

		// Test a second call
		client.destroy();
	}
}