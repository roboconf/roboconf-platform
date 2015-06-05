/**
 * Copyright 2015 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.dm.internal.delegates;

import junit.framework.Assert;
import net.roboconf.core.model.beans.ApplicationTemplate;

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ApplicationTemplateMngrDelegateTest {

	@Test
	public void testFindTemplate() {

		ApplicationTemplateMngrDelegate mngr = new ApplicationTemplateMngrDelegate();
		Assert.assertNull( mngr.findTemplate( "lamp", null ));

		ApplicationTemplate tpl = new ApplicationTemplate( "lamp" );
		mngr.templates.put( tpl, Boolean.TRUE );
		Assert.assertEquals( tpl, mngr.findTemplate( "lamp", null ));

		ApplicationTemplate tpl2 = new ApplicationTemplate( "lamp" ).qualifier( "v2" );
		mngr.templates.put( tpl2, Boolean.TRUE );

		Assert.assertFalse( tpl.equals( tpl2 ));
		Assert.assertEquals( tpl2, mngr.findTemplate( "lamp", "v2" ));
		Assert.assertEquals( tpl, mngr.findTemplate( "lamp", null ));
		Assert.assertNull( mngr.findTemplate( "lamp", "v3" ));
		Assert.assertNull( mngr.findTemplate( "lamp2", null ));
	}
}
