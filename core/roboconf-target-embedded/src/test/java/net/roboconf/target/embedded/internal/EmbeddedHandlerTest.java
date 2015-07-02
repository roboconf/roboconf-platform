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

package net.roboconf.target.embedded.internal;

import java.util.HashMap;

import junit.framework.Assert;
import net.roboconf.core.model.beans.Instance;

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class EmbeddedHandlerTest {

	@Test
	public void testTargetEmbedded() throws Exception {

		EmbeddedHandler target = new EmbeddedHandler();
		Assert.assertEquals( EmbeddedHandler.TARGET_ID, target.getTargetId());
		target.terminateMachine( null, null );
		target.terminateMachine( new HashMap<String,String>(), "anything" );

		Assert.assertFalse( target.isMachineRunning( null, "nothing (" + EmbeddedHandler.TARGET_ID + ")" ));
		Assert.assertNotNull( target.createMachine( null, null, "nothing", "app" ));
		Assert.assertTrue( target.isMachineRunning( null, "nothing (" + EmbeddedHandler.TARGET_ID + ")" ));

		Assert.assertNotNull( target.createMachine( new HashMap<String,String>(), new HashMap<String,String>(), null, null ));
		target.configureMachine(
				new HashMap<String,String>( 0 ),
				new HashMap<String,String>( 0 ),
				null, null, null,
				new Instance());

		target.terminateMachine( new HashMap<String,String>(), null );
		target.terminateMachine( null, "anything" );
	}
}
