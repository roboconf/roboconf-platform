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

package net.roboconf.dm.environment.iaas;

import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;
import net.roboconf.iaas.ec2.IaasEc2;
import net.roboconf.iaas.local.IaasInMemory;

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class IaasResolverTest {

	@Test
	public void testFindIaasHandler() {
		IaasResolver resolver = new IaasResolver();

		Map<String, String> props = new HashMap<String, String>();
		Assert.assertNull( resolver.findIaasHandler( props ));

		props.put( IaasResolver.IAAS_TYPE, "local" );
		Assert.assertTrue( resolver.findIaasHandler( props ) instanceof IaasInMemory );

		props.put( IaasResolver.IAAS_TYPE, "ec2" );
		Assert.assertTrue( resolver.findIaasHandler( props ) instanceof IaasEc2 );
	}
}
