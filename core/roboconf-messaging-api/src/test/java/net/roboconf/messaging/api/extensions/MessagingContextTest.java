/**
 * Copyright 2015-2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.messaging.api.extensions;

import org.junit.Assert;
import org.junit.Test;

import net.roboconf.messaging.api.extensions.MessagingContext.RecipientKind;
import net.roboconf.messaging.api.extensions.MessagingContext.ThoseThat;

/**
 * @author Vincent Zurczak - Linagora
 */
public class MessagingContextTest {

	@Test
	public void testEquals() {

		MessagingContext ctx = new MessagingContext( RecipientKind.DM, "domain", "whatever" );
		Assert.assertEquals( ctx, new MessagingContext( RecipientKind.DM, "domain", "whatever", null, "whatever" ));
		Assert.assertEquals( ctx, ctx );

		Assert.assertFalse( ctx.equals( new MessagingContext( RecipientKind.DM, "domain", null )));
		Assert.assertFalse( ctx.equals( new MessagingContext( RecipientKind.DM, "domain", "whatever2" )));
		Assert.assertFalse( ctx.equals( new MessagingContext( RecipientKind.AGENTS, "domain", null )));
		Assert.assertFalse( ctx.equals( new MessagingContext( RecipientKind.INTER_APP, "domain", null )));
		Assert.assertFalse( ctx.equals( new Object()));

		ctx = new MessagingContext( RecipientKind.INTER_APP, "domain", "facet", ThoseThat.EXPORT, "app" );
		Assert.assertEquals( ctx, new MessagingContext( RecipientKind.INTER_APP, "domain", "facet", ThoseThat.EXPORT, "app2" ));
		Assert.assertEquals( ctx, new MessagingContext( RecipientKind.INTER_APP, "domain", "facet", ThoseThat.EXPORT, "app3" ));
		Assert.assertEquals( ctx, ctx );

		Assert.assertFalse( ctx.equals( new MessagingContext( RecipientKind.INTER_APP, "domain", "facet", ThoseThat.IMPORT, "app" )));
		Assert.assertFalse( ctx.equals( new MessagingContext( RecipientKind.INTER_APP, "domain", "facet2", ThoseThat.EXPORT, "app" )));
		Assert.assertFalse( ctx.equals( new MessagingContext( RecipientKind.AGENTS, "domain", "facet", ThoseThat.EXPORT, "app" )));
		Assert.assertFalse( ctx.equals( new Object()));

		ctx = new MessagingContext( RecipientKind.AGENTS, "domain", "facet", ThoseThat.EXPORT, null );
		Assert.assertFalse( ctx.equals( new MessagingContext( RecipientKind.AGENTS, "domain", "facet", ThoseThat.IMPORT, null )));
		Assert.assertFalse( ctx.equals( new MessagingContext( RecipientKind.INTER_APP, "domain", "facet", ThoseThat.EXPORT, null )));
		Assert.assertFalse( ctx.equals( new MessagingContext( RecipientKind.AGENTS, "domain", "facet2", ThoseThat.EXPORT, null )));
		Assert.assertFalse( ctx.equals( new MessagingContext( RecipientKind.AGENTS, "domain", "facet", ThoseThat.EXPORT, "app" )));
		Assert.assertEquals( ctx, ctx );
	}


	@Test
	public void testHashCode() {

		MessagingContext ctx1 = new MessagingContext( RecipientKind.DM, "domain", null );
		Assert.assertEquals( ctx1.hashCode(), ctx1.hashCode());
		Assert.assertNotSame( ctx1.hashCode(), new MessagingContext( RecipientKind.DM, "domain", "whatever" ).hashCode());

		MessagingContext ctx2 = new MessagingContext( RecipientKind.INTER_APP, "domain", "facet", ThoseThat.EXPORT, "app" );
		Assert.assertEquals( ctx2.hashCode(), ctx2.hashCode());
		Assert.assertNotSame( ctx1.hashCode(), ctx2.hashCode());
	}


	@Test
	public void testConstructors() {

		// DM
		MessagingContext ctx = new MessagingContext( RecipientKind.DM, "domain", null );
		Assert.assertEquals( RecipientKind.DM, ctx.getKind());
		Assert.assertNull( ctx.getApplicationName());
		Assert.assertNull( ctx.getComponentOrFacetName());
		Assert.assertNull( ctx.getAgentDirection());

		ctx = new MessagingContext( RecipientKind.DM, "domain", "whatever" );
		Assert.assertEquals( RecipientKind.DM, ctx.getKind());
		Assert.assertEquals( "whatever", ctx.getApplicationName());
		Assert.assertNull( ctx.getComponentOrFacetName());
		Assert.assertNull( ctx.getAgentDirection());

		// Inter-application
		ctx = new MessagingContext( RecipientKind.INTER_APP, "domain", "whatever" );
		Assert.assertEquals( RecipientKind.INTER_APP, ctx.getKind());
		Assert.assertNull( ctx.getApplicationName());
		Assert.assertNull( ctx.getComponentOrFacetName());
		Assert.assertNull( ctx.getAgentDirection());

		ctx = new MessagingContext( RecipientKind.INTER_APP, "domain", null );
		Assert.assertEquals( RecipientKind.INTER_APP, ctx.getKind());
		Assert.assertNull( ctx.getApplicationName());
		Assert.assertNull( ctx.getComponentOrFacetName());
		Assert.assertNull( ctx.getAgentDirection());

		ctx = new MessagingContext( RecipientKind.INTER_APP, "domain", "facet", null, "whatever" );
		Assert.assertEquals( RecipientKind.INTER_APP, ctx.getKind());
		Assert.assertNull( ctx.getApplicationName());
		Assert.assertEquals( "facet", ctx.getComponentOrFacetName());
		Assert.assertNull( ctx.getAgentDirection());

		ctx = new MessagingContext( RecipientKind.INTER_APP, "domain", "facet", ThoseThat.EXPORT, "whatever" );
		Assert.assertEquals( RecipientKind.INTER_APP, ctx.getKind());
		Assert.assertNull( ctx.getApplicationName());
		Assert.assertEquals( "facet", ctx.getComponentOrFacetName());
		Assert.assertEquals( ThoseThat.EXPORT, ctx.getAgentDirection());

		// Agents
		ctx = new MessagingContext( RecipientKind.AGENTS, "domain", "whatever" );
		Assert.assertEquals( RecipientKind.AGENTS, ctx.getKind());
		Assert.assertEquals( "whatever", ctx.getApplicationName());
		Assert.assertNull( ctx.getComponentOrFacetName());
		Assert.assertNull( ctx.getAgentDirection());

		ctx = new MessagingContext( RecipientKind.AGENTS, "domain", null );
		Assert.assertEquals( RecipientKind.AGENTS, ctx.getKind());
		Assert.assertNull( ctx.getApplicationName());
		Assert.assertNull( ctx.getComponentOrFacetName());
		Assert.assertNull( ctx.getAgentDirection());

		ctx = new MessagingContext( RecipientKind.AGENTS, "domain", "facet", null, "whatever" );
		Assert.assertEquals( RecipientKind.AGENTS, ctx.getKind());
		Assert.assertEquals( "whatever", ctx.getApplicationName());
		Assert.assertEquals( "facet", ctx.getComponentOrFacetName());
		Assert.assertNull( ctx.getAgentDirection());

		ctx = new MessagingContext( RecipientKind.AGENTS, "domain", "facet", ThoseThat.IMPORT, "whatever" );
		Assert.assertEquals( RecipientKind.AGENTS, ctx.getKind());
		Assert.assertEquals( "whatever", ctx.getApplicationName());
		Assert.assertEquals( "facet", ctx.getComponentOrFacetName());
		Assert.assertEquals( ThoseThat.IMPORT, ctx.getAgentDirection());
		Assert.assertEquals( "those.that.import.facet", ctx.getTopicName());
	}


	@Test
	public void testGetTopicName() {

		// DM
		MessagingContext ctx = new MessagingContext( RecipientKind.DM, "domain", null );
		Assert.assertEquals( "", ctx.getTopicName());

		ctx = new MessagingContext( RecipientKind.DM, "domain", "whatever" );
		Assert.assertEquals( "whatever", ctx.getTopicName());

		// Inter-application
		ctx = new MessagingContext( RecipientKind.INTER_APP, "domain", "whatever" );
		Assert.assertEquals( "", ctx.getTopicName());

		ctx = new MessagingContext( RecipientKind.INTER_APP, "domain", null );
		Assert.assertEquals( "", ctx.getTopicName());

		ctx = new MessagingContext( RecipientKind.INTER_APP, "domain", "facet", null, "whatever" );
		Assert.assertEquals( "facet", ctx.getTopicName());

		ctx = new MessagingContext( RecipientKind.INTER_APP, "domain", "facet", ThoseThat.EXPORT, "whatever" );
		Assert.assertEquals( "those.that.export.facet", ctx.getTopicName());

		// Agents
		ctx = new MessagingContext( RecipientKind.AGENTS, "domain", "whatever" );
		Assert.assertEquals( "", ctx.getTopicName());

		ctx = new MessagingContext( RecipientKind.AGENTS, "domain", null );
		Assert.assertEquals( "", ctx.getTopicName());

		ctx = new MessagingContext( RecipientKind.AGENTS, "domain", "facet", null, "whatever" );
		Assert.assertEquals( "facet", ctx.getTopicName());

		ctx = new MessagingContext( RecipientKind.AGENTS, "domain", "facet", ThoseThat.IMPORT, "whatever" );
		Assert.assertEquals( "those.that.import.facet", ctx.getTopicName());
	}


	@Test
	public void testToString() {

		// DM
		MessagingContext ctx = new MessagingContext( RecipientKind.DM, "domain", null );
		Assert.assertEquals( "(DM)", ctx.toString());

		ctx = new MessagingContext( RecipientKind.DM, "domain", "whatever" );
		Assert.assertEquals( "whatever (DM)", ctx.toString());

		// Inter-application
		ctx = new MessagingContext( RecipientKind.INTER_APP, "domain", "whatever" );
		Assert.assertEquals( "(INTER_APP)", ctx.toString());

		ctx = new MessagingContext( RecipientKind.INTER_APP, "domain", null );
		Assert.assertEquals( "(INTER_APP)", ctx.toString());

		ctx = new MessagingContext( RecipientKind.INTER_APP, "domain", "facet", null, "whatever" );
		Assert.assertEquals( "facet (INTER_APP)", ctx.toString());

		ctx = new MessagingContext( RecipientKind.INTER_APP, "domain", "facet", ThoseThat.EXPORT, "whatever" );
		Assert.assertEquals( "those.that.export.facet (INTER_APP)", ctx.toString());

		// Agents
		ctx = new MessagingContext( RecipientKind.AGENTS, "domain", "whatever" );
		Assert.assertEquals( "@ whatever (AGENTS)", ctx.toString());

		ctx = new MessagingContext( RecipientKind.AGENTS, "domain", null );
		Assert.assertEquals( "(AGENTS)", ctx.toString());

		ctx = new MessagingContext( RecipientKind.AGENTS, "domain", "facet", null, "whatever" );
		Assert.assertEquals( "facet @ whatever (AGENTS)", ctx.toString());

		ctx = new MessagingContext( RecipientKind.AGENTS, "domain", "facet", ThoseThat.IMPORT, "whatever" );
		Assert.assertEquals( "those.that.import.facet @ whatever (AGENTS)", ctx.toString());
	}
}
