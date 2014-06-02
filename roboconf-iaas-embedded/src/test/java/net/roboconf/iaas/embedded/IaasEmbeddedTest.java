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

package net.roboconf.iaas.embedded;

import java.util.HashMap;

import junit.framework.Assert;

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class IaasEmbeddedTest {

	@Test
	public void testIaasEmbedded() throws Exception {

		IaasEmbedded iaas = new IaasEmbedded();
		iaas.terminateVM( null );
		iaas.terminateVM( "anything" );

		Assert.assertNotNull( iaas.createVM( "ami", "ip", "nothing", "app" ));
		Assert.assertNotNull( iaas.createVM( null, null, null, null ));

		iaas.setIaasProperties( null );
		iaas.setIaasProperties( new HashMap<String,String>( 0 ));

		iaas.terminateVM( null );
		iaas.terminateVM( "anything" );
	}
}
