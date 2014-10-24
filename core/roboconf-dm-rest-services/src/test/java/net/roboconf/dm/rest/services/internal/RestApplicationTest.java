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

package net.roboconf.dm.rest.services.internal;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;
import net.roboconf.dm.management.Manager;
import net.roboconf.messaging.MessagingConstants;

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class RestApplicationTest {

	@Test
	public void testSingleton() {

		RestApplication app = new RestApplication( new Manager( MessagingConstants.FACTORY_TEST ));
		List<Object> singleton = new ArrayList<Object>( app.getSingletons());
		Assert.assertEquals( 2, singleton.size());
	}
}
