/**
 * Copyright 2014-2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.agent.monitoring.internal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import net.roboconf.agent.monitoring.api.IMonitoringHandler;
import net.roboconf.agent.monitoring.internal.file.FileHandler;
import net.roboconf.agent.monitoring.internal.nagios.NagiosHandler;
import net.roboconf.agent.monitoring.internal.rest.RestHandler;
import net.roboconf.agent.monitoring.internal.tests.MyAgentInterface;
import net.roboconf.core.internal.tests.TestUtils;

import org.junit.Test;
import org.mockito.Mockito;

/**
 * @author Vincent Zurczak - Linagora
 */
public class AgentMonitoringTest {

	@Test
	public void testStartAndStop() {

		AgentMonitoring am = new AgentMonitoring();
		am.setAgentInterface( new MyAgentInterface( null ));
		am.stop();
		am.start();
		am.start();
		am.stop();
	}


	@Test
	@SuppressWarnings( "unchecked" )
	public void testExtensibilityNotifications_defaultHandlers() throws Exception {

		AgentMonitoring am = new AgentMonitoring();
		am.setAgentInterface( new MyAgentInterface( null ));

		List<IMonitoringHandler> handlers = TestUtils.getInternalField( am, "handlers", List.class );
		Assert.assertEquals( 3, handlers.size());
		Assert.assertEquals( FileHandler.class, handlers.get( 0 ).getClass());
		Assert.assertEquals( NagiosHandler.class, handlers.get( 1 ).getClass());
		Assert.assertEquals( RestHandler.class, handlers.get( 2 ).getClass());
	}


	@Test
	@SuppressWarnings( "unchecked" )
	public void testExtensibilityNotifications_handlers() throws Exception {

		AgentMonitoring am = new AgentMonitoring();
		am.setAgentInterface( new MyAgentInterface( null ));

		List<IMonitoringHandler> handlers = TestUtils.getInternalField( am, "handlers", List.class );
		handlers.clear();

		Assert.assertEquals( 0, handlers.size());
		am.handlerAppears( null );
		Assert.assertEquals( 0, handlers.size());
		am.handlerAppears( newMock( "hey" ));
		Assert.assertEquals( 1, handlers.size());
		am.handlerDisappears( newMock( "hey" ));
		Assert.assertEquals( 0, handlers.size());

		am.handlerDisappears( newMock( "ho" ));
		Assert.assertEquals( 0, handlers.size());

		am.handlerDisappears( null );
		Assert.assertEquals( 0, handlers.size());

		am.handlerAppears( newMock( "oops" ));
		Assert.assertEquals( 1, handlers.size());

		am.handlerAppears( newMock( "new_oops" ));
		Assert.assertEquals( 2, handlers.size());
	}


	private final Map<String,IMonitoringHandler> nameToMock = new HashMap<> ();
	private IMonitoringHandler newMock( String name ) {

		IMonitoringHandler result = this.nameToMock.get( name );

		if( result == null ) {
			result = Mockito.mock( IMonitoringHandler.class );
			Mockito.when( result.getName()).thenReturn( name );
			this.nameToMock.put( name, result );
		}

		return result;
	}
}
