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

package net.roboconf.integration.tests.dm.with.agents.in.memory.internal;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.roboconf.agent.internal.Agent;
import net.roboconf.messaging.api.MessagingConstants;
import net.roboconf.messaging.api.factory.IMessagingClientFactory;
import net.roboconf.target.api.TargetException;
import net.roboconf.target.api.TargetHandler;
import net.roboconf.target.api.TargetHandlerParameters;

/**
 * @author Vincent Zurczak - Linagora
 */
public class MyHandler implements TargetHandler {
	public final Map<String,Agent> agentIdToAgent = new ConcurrentHashMap<>();


	@Override
	public String getTargetId() {
		return "for test";
	}

	@Override
	public String createMachine( TargetHandlerParameters parameters )
	throws TargetException {

		final String messagingType = parameters.getMessagingProperties().get( MessagingConstants.MESSAGING_TYPE_PROPERTY );

		Agent agent = new Agent();
		agent.setApplicationName( parameters.getApplicationName());
		agent.setScopedInstancePath( parameters.getScopedInstancePath());
		agent.setSimulatePlugins( true );
		agent.setIpAddress( "127.0.0.1" );
		agent.setMessagingType(messagingType);
		agent.start();

		IMessagingClientFactory factory = agent.getMessagingClient().getRegistry().getMessagingClientFactory(messagingType);
		if( factory != null )
			factory.setConfiguration( parameters.getMessagingProperties());

		String key = parameters.getScopedInstancePath() + " @ " + parameters.getApplicationName();
		this.agentIdToAgent.put( key, agent );

		return key;
	}


	@Override
	public void configureMachine( TargetHandlerParameters parameters, String machineId )
	throws TargetException {
		// nothing
	}


	@Override
	public boolean isMachineRunning( TargetHandlerParameters parameters, String machineId )
	throws TargetException {
		return this.agentIdToAgent.containsKey( machineId );
	}


	@Override
	public void terminateMachine( TargetHandlerParameters parameters, String machineId ) throws TargetException {

		Agent agent = this.agentIdToAgent.remove( machineId );
		if( agent != null )
			agent.stop();
	}


	@Override
	public String retrievePublicIpAddress( TargetHandlerParameters parameters, String machineId )
	throws TargetException {
		return null;
	}
}
