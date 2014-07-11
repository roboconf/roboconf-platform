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

package net.roboconf.core.model.runtime;

import junit.framework.Assert;
import net.roboconf.core.model.runtime.Instance.InstanceStatus;

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class InstanceTest {

	@Test
	public void testWichStatus() {
		Assert.assertEquals( InstanceStatus.STARTING, InstanceStatus.wichStatus( "starting" ));
		Assert.assertEquals( InstanceStatus.STARTING, InstanceStatus.wichStatus( "startiNG" ));
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, InstanceStatus.wichStatus( "start" ));
	}


	@Test
	public void testChain() {

		Instance inst = new Instance().name( "ins" ).channel( "ch" ).status( InstanceStatus.DEPLOYING ).component( null ).parent( null );
		Assert.assertEquals( "ch", inst.getChannel());
		Assert.assertEquals( "ins", inst.getName());
		Assert.assertEquals( InstanceStatus.DEPLOYING, inst.getStatus());
		Assert.assertNull( inst.getComponent());
		Assert.assertNull( inst.getParent());
	}
}
