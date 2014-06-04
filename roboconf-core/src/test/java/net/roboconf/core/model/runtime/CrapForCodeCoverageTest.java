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

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class CrapForCodeCoverageTest {

	@Test
	public void testCrap() {

		Instance inst = new Instance();
		Assert.assertNull( inst.toString());
		Assert.assertNotNull( inst.name( "azerty" ).toString());

		Component comp = new Component();
		Assert.assertNull( comp.toString());
		Assert.assertNotNull( comp.name( "azerty" ).toString());

		Application app = new Application();
		Assert.assertNull( app.toString());
		Assert.assertNotNull( app.name( "azerty" ).toString());
	}
}
