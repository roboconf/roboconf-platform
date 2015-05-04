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

import java.io.IOException;

import junit.framework.Assert;
import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.dm.management.ManagedApplication;

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ApplicationMngrDelegateTest {

	@Test
	public void testIsTemplateUsed() {

		ApplicationMngrDelegate mngr = new ApplicationMngrDelegate();
		ApplicationTemplate tpl = new ApplicationTemplate( "lamp" );
		Assert.assertFalse( mngr.isTemplateUsed( tpl ));

		ManagedApplication ma = new ManagedApplication( new Application( "app", tpl ));
		mngr.getNameToManagedApplication().put( "app", ma );
		Assert.assertTrue( mngr.isTemplateUsed( tpl ));

		ApplicationTemplate tpl2 = new ApplicationTemplate( "lamp" ).qualifier( "v2" );
		Assert.assertFalse( mngr.isTemplateUsed( tpl2 ));
	}


	@Test( expected = IOException.class )
	public void testInvalidApplicationName() throws Exception {

		ApplicationMngrDelegate mngr = new ApplicationMngrDelegate();
		mngr.createApplication( null, "desc", new ApplicationTemplate(), null );
	}
}
