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

package net.roboconf.dm.templating.internal.contexts;

import java.util.Date;

import org.junit.Assert;

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ApplicationContextBeanTest {

	@Test
	public void checkDateGetter() {

		ApplicationContextBean bean = new ApplicationContextBean();
		Date date = new Date();
		bean.lastModified = date;

		Assert.assertNotSame( date, bean.getLastModified());
		Assert.assertEquals( date, bean.getLastModified());
	}



	@Test
	public void testToString() {

		// Application
		ApplicationContextBean ab = new ApplicationContextBean();
		Assert.assertNull( ab.toString());

		ab.name = "test";
		Assert.assertEquals( ab.name, ab.toString());

		// Variable
		VariableContextBean vb = new VariableContextBean();
		Assert.assertNull( vb.toString());

		vb.name = "tesst";
		Assert.assertEquals( vb.name, vb.toString());

		// Instance
		InstanceContextBean ib = new InstanceContextBean();
		Assert.assertNull( ib.toString());

		ib.name = "odg";
		Assert.assertEquals( ib.name, ib.toString());

		// Import
		ImportContextBean impB = new ImportContextBean();
		Assert.assertNotNull( impB.toString());

		impB.instance = ib;
		Assert.assertTrue( impB.toString().endsWith( impB.instance.toString()));
	}
}
