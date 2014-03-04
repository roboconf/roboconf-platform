/**
 * Copyright 2013 Linagora, Université Joseph Fourier
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

package net.roboconf.iaas.ec2;

import java.util.Properties;

import junit.framework.Assert;
import net.roboconf.iaas.api.exceptions.InvalidIaasPropertiesException;
import net.roboconf.iaas.ec2.internal.Ec2Constants;

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class IaasEc2Test {

	@Test
	public void testConfigurationParsing() {

		// Empty configuration
		Properties iaasProperties = new Properties();
		IaasEc2 ec2 = new IaasEc2();
		try {
			ec2.setIaasProperties( iaasProperties );
			Assert.fail( "An invalid configuration should have been detected." );

		} catch( InvalidIaasPropertiesException e ) {
			Assert.assertTrue( e.getMessage().toLowerCase().contains( "endpoint" ));
		}

		// Fill-in everything
		iaasProperties.put( Ec2Constants.EC2_ENDPOINT, "127.0.0.1" );
		iaasProperties.put( Ec2Constants.EC2_ACCESS_KEY, "my access key" );
		iaasProperties.put( Ec2Constants.EC2_SECRET_KEY, "my secret key" );
		iaasProperties.put( Ec2Constants.AMI_VM_NODE, "the node" );
		iaasProperties.put( Ec2Constants.VM_INSTANCE_TYPE, "tiny" );
		iaasProperties.put( Ec2Constants.SSH_KEY_NAME, "secret_key" );
		iaasProperties.put( Ec2Constants.SECURITY_GROUP_NAME, "WorldWideVisible" );
		try {
			ec2.setIaasProperties( iaasProperties );

		} catch( InvalidIaasPropertiesException e ) {
			Assert.fail( "An invalid configuration was detected while it was valid." );
		}
	}
}
