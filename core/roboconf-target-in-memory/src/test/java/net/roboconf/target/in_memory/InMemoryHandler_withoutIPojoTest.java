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

package net.roboconf.target.in_memory;

import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;
import net.roboconf.target.api.TargetException;

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class InMemoryHandler_withoutIPojoTest {

	@Test
	public void checkBasics() throws Exception {

		InMemoryHandler target = new InMemoryHandler();
		Assert.assertEquals( InMemoryHandler.TARGET_ID, target.getTargetId());

		target.terminateMachine( null, "whatever" );
	}


	@Test( expected = TargetException.class )
	public void testCreateVm() throws Exception {

		new InMemoryHandler().createOrConfigureMachine( null, "127.0.0.1", "roboconf", "roboconf", "vm", "my app" );
	}


	@Test( expected = TargetException.class )
	public void testCreateVm_withDelay() throws Exception {

		InMemoryHandler handler = new InMemoryHandler();
		handler.setDefaultDelay( 100L );
		Assert.assertEquals( 100L, handler.getDefaultDelay());

		Map<String,String> targetProperties = new HashMap<String,String>( 1 );
		targetProperties.put( InMemoryHandler.DELAY, "20L" );

		handler.createOrConfigureMachine( targetProperties, "127.0.0.1", "roboconf", "roboconf", "vm", "my app" );
	}


	@Test( expected = TargetException.class )
	public void testCreateVm_withDefaultDelay() throws Exception {

		InMemoryHandler handler = new InMemoryHandler();
		handler.setDefaultDelay( 10L );
		Assert.assertEquals( 10L, handler.getDefaultDelay());

		Map<String,String> targetProperties = new HashMap<String,String>( 0 );
		handler.createOrConfigureMachine( targetProperties, "127.0.0.1", "roboconf", "roboconf", "vm", "my app" );
	}
}