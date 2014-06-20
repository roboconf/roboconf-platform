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

package net.roboconf.core.agents;

import java.util.Properties;

import junit.framework.Assert;

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class DataHelpersTest {

	@Test
	public void testWriteAndRead() throws Exception {

		String rawProperties = DataHelpers.writeIaasDataAsString( "192.168.1.24", "user", "pwd", "app", "root" );
		Properties props = DataHelpers.readIaasData( rawProperties );
		Assert.assertEquals( "app", props.getProperty( DataHelpers.APPLICATION_NAME ));
		Assert.assertEquals( "root", props.getProperty( DataHelpers.ROOT_INSTANCE_NAME ));
		Assert.assertEquals( "192.168.1.24", props.getProperty( DataHelpers.MESSAGING_IP ));
		Assert.assertEquals( "pwd", props.getProperty( DataHelpers.MESSAGING_PASSWORD ));
		Assert.assertEquals( "user", props.getProperty( DataHelpers.MESSAGING_USERNAME ));

		rawProperties = DataHelpers.writeIaasDataAsString( null, "user", "pwd", "app", "root" );
		props = DataHelpers.readIaasData( rawProperties );
		Assert.assertEquals( "app", props.getProperty( DataHelpers.APPLICATION_NAME ));
		Assert.assertEquals( "root", props.getProperty( DataHelpers.ROOT_INSTANCE_NAME ));
		Assert.assertEquals( null, props.getProperty( DataHelpers.MESSAGING_IP ));
		Assert.assertEquals( "pwd", props.getProperty( DataHelpers.MESSAGING_PASSWORD ));
		Assert.assertEquals( "user", props.getProperty( DataHelpers.MESSAGING_USERNAME ));

		rawProperties = DataHelpers.writeIaasDataAsString( "192.168.1.24", null, "pwd", "app", "root" );
		props = DataHelpers.readIaasData( rawProperties );
		Assert.assertEquals( "app", props.getProperty( DataHelpers.APPLICATION_NAME ));
		Assert.assertEquals( "root", props.getProperty( DataHelpers.ROOT_INSTANCE_NAME ));
		Assert.assertEquals( "192.168.1.24", props.getProperty( DataHelpers.MESSAGING_IP ));
		Assert.assertEquals( "pwd", props.getProperty( DataHelpers.MESSAGING_PASSWORD ));
		Assert.assertEquals( null, props.getProperty( DataHelpers.MESSAGING_USERNAME ));

		rawProperties = DataHelpers.writeIaasDataAsString( "192.168.1.24", "user", null, "app", "root" );
		props = DataHelpers.readIaasData( rawProperties );
		Assert.assertEquals( "app", props.getProperty( DataHelpers.APPLICATION_NAME ));
		Assert.assertEquals( "root", props.getProperty( DataHelpers.ROOT_INSTANCE_NAME ));
		Assert.assertEquals( "192.168.1.24", props.getProperty( DataHelpers.MESSAGING_IP ));
		Assert.assertEquals( null, props.getProperty( DataHelpers.MESSAGING_PASSWORD ));
		Assert.assertEquals( "user", props.getProperty( DataHelpers.MESSAGING_USERNAME ));

		rawProperties = DataHelpers.writeIaasDataAsString( "192.168.1.24", "user", "pwd", null, "root" );
		props = DataHelpers.readIaasData( rawProperties );
		Assert.assertEquals( null, props.getProperty( DataHelpers.APPLICATION_NAME ));
		Assert.assertEquals( "root", props.getProperty( DataHelpers.ROOT_INSTANCE_NAME ));
		Assert.assertEquals( "192.168.1.24", props.getProperty( DataHelpers.MESSAGING_IP ));
		Assert.assertEquals( "pwd", props.getProperty( DataHelpers.MESSAGING_PASSWORD ));
		Assert.assertEquals( "user", props.getProperty( DataHelpers.MESSAGING_USERNAME ));

		rawProperties = DataHelpers.writeIaasDataAsString( "192.168.1.24", "user", "pwd", "app", null );
		props = DataHelpers.readIaasData( rawProperties );
		Assert.assertEquals( "app", props.getProperty( DataHelpers.APPLICATION_NAME ));
		Assert.assertEquals( null, props.getProperty( DataHelpers.ROOT_INSTANCE_NAME ));
		Assert.assertEquals( "192.168.1.24", props.getProperty( DataHelpers.MESSAGING_IP ));
		Assert.assertEquals( "pwd", props.getProperty( DataHelpers.MESSAGING_PASSWORD ));
		Assert.assertEquals( "user", props.getProperty( DataHelpers.MESSAGING_USERNAME ));
	}
}
