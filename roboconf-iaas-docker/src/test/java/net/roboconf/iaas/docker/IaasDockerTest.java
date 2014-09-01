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

package net.roboconf.iaas.docker;

import java.util.HashMap;

import junit.framework.Assert;
import net.roboconf.iaas.api.IaasException;
import net.roboconf.iaas.docker.internal.DockerConstants;

import org.junit.Test;

/**
 * @author Pierre-Yves Gibello - Linagora
 */
public class IaasDockerTest {

	@Test
	public void testConfigurationParsing() {

		// Empty configuration
		HashMap<String, String> iaasProperties = new HashMap<String, String>();
		IaasDocker iaas = new IaasDocker();
		try {
			iaas.setIaasProperties( iaasProperties );
			Assert.fail( "An invalid configuration should have been detected." );

		} catch( IaasException e ) {
			Assert.assertTrue(true);
		}

		// Move on
		iaasProperties.put(DockerConstants.IMAGE_ID, "test" );
		try {
			iaas.setIaasProperties( iaasProperties );
			Assert.assertTrue(true);

		} catch( IaasException e ) {
			Assert.fail("An invalid configuration should have been detected.");
		}

		//TODO more complete tests ?

	}
}
