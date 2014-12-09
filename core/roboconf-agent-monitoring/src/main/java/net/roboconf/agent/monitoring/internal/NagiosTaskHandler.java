/**
 * Copyright 2013-2014 Linagora, Université Joseph Fourier
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

import java.io.IOException;
import java.net.UnknownHostException;

import net.roboconf.messaging.messages.Message;
import net.roboconf.messaging.messages.from_agent_to_dm.MsgNotifAutonomic;
 
/**
 * Periodic task to check for monitoring events on Nagios (polling).
 * @author Pierre-Yves Gibello - Linagora
 */
public class NagiosTaskHandler extends MonitoringTaskHandler {

	private String query[];

	public NagiosTaskHandler(String eventName, String applicationName, String vmInstanceName, String query[]) {
		super(eventName, applicationName, vmInstanceName);
		this.query = query;
	}

	@Override
	public Message process() {
		
		//TODO config for host/port !!! Here shinken default assumed...
		LivestatusClient client = new LivestatusClient("localhost", 50000);
		
		try {
			MsgNotifAutonomic message = new MsgNotifAutonomic(eventName,
					applicationName, vmInstanceName, client.queryLivestatus(query));
		
			return message;
			
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}

}
