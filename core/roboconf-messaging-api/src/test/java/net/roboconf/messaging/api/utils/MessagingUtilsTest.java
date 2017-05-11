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

package net.roboconf.messaging.api.utils;

import org.junit.Assert;
import org.junit.Test;

import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.messaging.api.extensions.MessagingContext.RecipientKind;

/**
 * @author Vincent Zurczak - Linagora
 */
public class MessagingUtilsTest {

	@Test
	public void testBuildRoutingKeyForAgent_String() {

		Assert.assertEquals( "machine.root", MessagingUtils.buildTopicNameForAgent( "root" ));
		Assert.assertEquals( "machine.root", MessagingUtils.buildTopicNameForAgent("/root"));
		Assert.assertEquals( "machine.root", MessagingUtils.buildTopicNameForAgent( "/root/" ));
		Assert.assertEquals( "machine.root.docker", MessagingUtils.buildTopicNameForAgent( "/root/docker" ));
		Assert.assertNotSame(
				MessagingUtils.buildTopicNameForAgent( "root1" ),
				MessagingUtils.buildTopicNameForAgent( "root2" ));
	}


	@Test
	public void testBuildRoutingKeyForAgent_Instance() {
		Instance inst = new Instance( "my-root" );

		Assert.assertNotNull( MessagingUtils.buildTopicNameForAgent( inst ));
		Assert.assertEquals(
				MessagingUtils.buildTopicNameForAgent( inst ),
				MessagingUtils.buildTopicNameForAgent( inst.getName()));

		Instance childInstance = new Instance( "child" );
		InstanceHelpers.insertChild( inst, childInstance );
		Assert.assertEquals(
				MessagingUtils.buildTopicNameForAgent( childInstance ),
				MessagingUtils.buildTopicNameForAgent( inst ));
	}


	@Test
	public void testEscapeInstancePath() {

		Assert.assertEquals( "", MessagingUtils.escapeInstancePath( null ));
		Assert.assertEquals( "", MessagingUtils.escapeInstancePath( " " ));
		Assert.assertEquals( "root", MessagingUtils.escapeInstancePath( "/root" ));
		Assert.assertEquals( "root.server.app", MessagingUtils.escapeInstancePath( "/root/server/app" ));
	}


	@Test
	public void testBuildId() {

		String id = MessagingUtils.buildId( RecipientKind.DM, "domain", "app", "/root" );
		Assert.assertEquals( "[ domain ] DM", id );

		id = MessagingUtils.buildId( RecipientKind.AGENTS, "domain", "app", "/root" );
		Assert.assertEquals( "[ domain ] /root @ app", id );

		id = MessagingUtils.buildId( RecipientKind.INTER_APP, "domain", "app", "/root" );
		Assert.assertEquals( "[ domain ] /root @ app", id );
	}
}
