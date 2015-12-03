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

package net.roboconf.target.ec2.internal;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import net.roboconf.target.api.TargetException;

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class Ec2IaasHandlerTest {

	@Test( expected = TargetException.class )
	public void testInvalidConfiguration() throws Exception {
		 Ec2IaasHandler.parseProperties( new HashMap<String,String> ());
	}


	@Test
	public void testValidConfiguration() throws Exception {

		Map<String, String> targetProperties = new HashMap<String,String> ();
		targetProperties.put( Ec2Constants.EC2_ENDPOINT, "127.0.0.1" );
		targetProperties.put( Ec2Constants.EC2_ACCESS_KEY, "my access key" );
		targetProperties.put( Ec2Constants.EC2_SECRET_KEY, "my secret key" );
		targetProperties.put( Ec2Constants.AMI_VM_NODE, "the node" );
		targetProperties.put( Ec2Constants.VM_INSTANCE_TYPE, "tiny" );
		targetProperties.put( Ec2Constants.SSH_KEY_NAME, "secret_key" );
		targetProperties.put( Ec2Constants.SECURITY_GROUP_NAME, "WorldWideVisible" );

		Ec2IaasHandler.parseProperties( targetProperties );
	}


	@Test
	public void testGetTargetId() {
		Assert.assertEquals( Ec2IaasHandler.TARGET_ID, new Ec2IaasHandler().getTargetId());
	}
}
