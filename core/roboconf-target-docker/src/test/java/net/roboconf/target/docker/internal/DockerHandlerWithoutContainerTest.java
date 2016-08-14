/**
 * Copyright 2014-2016 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.target.docker.internal;

import java.util.HashMap;

import org.junit.Assert;
import org.junit.Test;

import net.roboconf.target.api.TargetException;
import net.roboconf.target.api.TargetHandlerParameters;

/**
 * @author Pierre-Yves Gibello - Linagora
 */
public class DockerHandlerWithoutContainerTest {

	@Test
	public void testGetTargetId() {
		Assert.assertEquals( DockerHandler.TARGET_ID, new DockerHandler().getTargetId());
	}


	@Test( expected = TargetException.class )
	public void testTerminate_noConnection() throws Exception {
		new DockerHandler().terminateMachine( new HashMap<String,String> (), "whatever" );
	}


	@Test
	public void testIsRunning_noConnection() throws Exception {
		boolean running = new DockerHandler().isMachineRunning( new HashMap<String,String> (), "whatever" );
		Assert.assertFalse( running );
	}


	@Test( expected = TargetException.class )
	public void testCreateMachine_invalidConfiguration() throws Exception {
		new DockerHandler().createMachine( new TargetHandlerParameters().targetProperties( new HashMap<String,String>( 0 )));
	}
}
