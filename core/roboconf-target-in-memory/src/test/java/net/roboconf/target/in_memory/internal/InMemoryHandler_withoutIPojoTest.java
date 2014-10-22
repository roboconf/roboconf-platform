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

package net.roboconf.target.in_memory.internal;

import junit.framework.Assert;
import net.roboconf.target.api.TargetException;
import net.roboconf.target.in_memory.InMemoryHandler;

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class InMemoryHandler_withoutIPojoTest {

	@Test
	public void checkBasics() throws Exception {

		InMemoryHandler target = new InMemoryHandler();
		Assert.assertEquals( InMemoryHandler.TARGET_ID, target.getTargetId());

		target.terminateMachine( "whatever" );
	}


	@Test( expected = TargetException.class )
	public void testCreateVm() throws Exception {

		new InMemoryHandler().createOrConfigureMachine( "127.0.0.1", "roboconf", "roboconf", "vm", "my app" );
	}
}
