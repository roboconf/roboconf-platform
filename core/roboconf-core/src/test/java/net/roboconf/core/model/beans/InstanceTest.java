/**
 * Copyright 2014-2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.core.model.beans;

import org.junit.Assert;
import net.roboconf.core.model.beans.Instance.InstanceStatus;

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class InstanceTest {

	@Test
	public void testWichStatus() {
		Assert.assertEquals( InstanceStatus.STARTING, InstanceStatus.whichStatus( "starting" ));
		Assert.assertEquals( InstanceStatus.STARTING, InstanceStatus.whichStatus( "startiNG" ));
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, InstanceStatus.whichStatus( "start" ));
	}


	@Test
	public void testIsValidState() {
		Assert.assertTrue( InstanceStatus.isValidState( "starting" ));
		Assert.assertTrue( InstanceStatus.isValidState( "StarTing" ));
		Assert.assertFalse( InstanceStatus.isValidState( "Startin" ));
	}


	@Test
	public void testChain() {

		Instance inst = new Instance().name( "ins" ).status( InstanceStatus.DEPLOYING ).component( null ).parent( null );
		Assert.assertEquals( 0, inst.channels.size());
		Assert.assertEquals( "ins", inst.getName());
		Assert.assertEquals( InstanceStatus.DEPLOYING, inst.getStatus());
		Assert.assertNull( inst.getComponent());
		Assert.assertNull( inst.getParent());

		Assert.assertEquals( 1, inst.channel( "woo" ).channels.size());
		Assert.assertEquals( 2, inst.channel( "yeah" ).channels.size());
		Assert.assertEquals( 2, inst.channel( "woo" ).channels.size());
	}


	@Test
	public void testEquals() {

		Instance hop = new Instance( "hop" );
		Assert.assertFalse( hop.equals( null ));
		Assert.assertFalse( hop.equals( new Instance( "hop2" )));

		Assert.assertEquals( hop, hop );
		Assert.assertEquals( hop, new Instance ("hop" ));
	}
}
