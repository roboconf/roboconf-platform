/**
 * Copyright 2016-2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.karaf.commands.common;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import net.roboconf.core.runtime.IReconfigurable;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ReloadConfigurationCommandTest {

	@Test
	public void testExecute_noReconfigurable() throws Exception {

		ReloadConfigurationCommand cmd = new ReloadConfigurationCommand();
		Assert.assertNull( cmd.execute());
	}


	@Test
	public void testExecute_emptyReconfigurableList() throws Exception {

		ReloadConfigurationCommand cmd = new ReloadConfigurationCommand();
		cmd.reconfigurables = new ArrayList<>( 0 );
		Assert.assertNull( cmd.execute());
	}


	@Test
	public void testExecute_twoReconfigurables() throws Exception {

		ReloadConfigurationCommand cmd = new ReloadConfigurationCommand();

		IReconfigurable r1 = Mockito.mock( IReconfigurable.class );
		IReconfigurable r2 = Mockito.mock( IReconfigurable.class );
		cmd.reconfigurables = Arrays.asList( r1, r2 );

		Assert.assertNull( cmd.execute());
		Mockito.verify( r1, Mockito.only()).reconfigure();
		Mockito.verify( r2, Mockito.only()).reconfigure();
	}


	@Test
	public void testExecute_testException() throws Exception {

		ReloadConfigurationCommand cmd = new ReloadConfigurationCommand();

		IReconfigurable r1 = Mockito.mock( IReconfigurable.class );
		Mockito.doThrow( new RuntimeException( "for test" )).when( r1 ).reconfigure();
		cmd.reconfigurables = Arrays.asList( r1 );

		Assert.assertNull( cmd.execute());
		Mockito.verify( r1, Mockito.only()).reconfigure();
	}
}
