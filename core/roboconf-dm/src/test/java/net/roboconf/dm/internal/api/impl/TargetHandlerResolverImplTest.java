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

package net.roboconf.dm.internal.api.impl;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import net.roboconf.core.Constants;
import net.roboconf.target.api.TargetException;
import net.roboconf.target.api.TargetHandler;

import org.junit.Test;
import org.mockito.Mockito;

/**
 * @author Vincent Zurczak - Linagora
 */
public class TargetHandlerResolverImplTest {

	@Test
	public void testFindTargetHandler() throws Exception {

		TargetHandler th1 = Mockito.mock( TargetHandler.class );
		Mockito.when( th1.getTargetId()).thenReturn( "t1" );

		TargetHandler th2 = Mockito.mock( TargetHandler.class );
		Mockito.when( th2.getTargetId()).thenReturn( "t2" );

		TargetHandlerResolverImpl mngr = new TargetHandlerResolverImpl();
		mngr.addTargetHandler( th1 );
		mngr.addTargetHandler( th2 );

		Map<String,String> targetProperties = new HashMap<>();
		targetProperties.put( Constants.TARGET_PROPERTY_HANDLER, "t2" );
		Assert.assertEquals( th2, mngr.findTargetHandler( targetProperties ));

		targetProperties.put( Constants.TARGET_PROPERTY_HANDLER, "t1" );
		Assert.assertEquals( th1, mngr.findTargetHandler( targetProperties ));
	}


	@Test( expected = TargetException.class )
	public void testFindTargetHandler_exception() throws Exception {

		Map<String,String> targetProperties = new HashMap<>();
		targetProperties.put( Constants.TARGET_PROPERTY_HANDLER, "t2" );

		TargetHandlerResolverImpl mngr = new TargetHandlerResolverImpl();
		mngr.findTargetHandler( targetProperties );
	}
}
