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
import net.roboconf.dm.rest.services.internal.resources.ApplicationResource;
import net.roboconf.dm.rest.services.internal.resources.ManagementResource;

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class RestApplicationTest {

	@Test
	public void testSingleton() {

		ApplicationResource appRes = new ApplicationResource();
		ManagementResource mngrRes = new ManagementResource();

		RestApplication app = new RestApplication( appRes, mngrRes );
		List<Object> singleton = new ArrayList<Object>( app.getSingletons());

		Assert.assertEquals( 2, singleton.size());
	}
}
