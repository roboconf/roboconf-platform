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

package net.roboconf.iaas.ec2.internal;

import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;
import net.roboconf.iaas.api.IaasException;

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class IaasEc2Test {

	@Test( expected = IaasException.class )
	public void testInvalidConfiguration() throws Exception {

		IaasEc2 iaas = new IaasEc2();
		iaas.setIaasProperties( new HashMap<String,String> ());
	}


	@Test
	public void testValidConfiguration() throws Exception {

		Map<String, String> iaasProperties = new HashMap<String,String> ();
		IaasEc2 iaas = new IaasEc2();

		iaasProperties.put( Ec2Constants.EC2_ENDPOINT, "127.0.0.1" );
		iaasProperties.put( Ec2Constants.EC2_ACCESS_KEY, "my access key" );
		iaasProperties.put( Ec2Constants.EC2_SECRET_KEY, "my secret key" );
		iaasProperties.put( Ec2Constants.AMI_VM_NODE, "the node" );
		iaasProperties.put( Ec2Constants.VM_INSTANCE_TYPE, "tiny" );
		iaasProperties.put( Ec2Constants.SSH_KEY_NAME, "secret_key" );
		iaasProperties.put( Ec2Constants.SECURITY_GROUP_NAME, "WorldWideVisible" );

		iaas.setIaasProperties( iaasProperties );
	}


	@Test
	public void testGetIaasType() {
		Assert.assertEquals( IaasEc2.IAAS_TYPE, new IaasEc2().getIaasType());
	}
}
