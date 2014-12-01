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

import java.net.InetAddress;
import java.net.UnknownHostException;

import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.messaging.messages.Message;

/**
 * Monitoring event (eg. threshold crossed). May be used for elasticity
 * (like adding a new VM when heavy load is detected), or any monitoring purpose.
 * 
 * @author Pierre-Yves Gibello - Linagora
 */
public class MsgMonitoringEvent extends Message {
	
	private static final long serialVersionUID = -8930645802175790064L;
	private String eventName;
	private String eventInfo;
	private final String rootInstanceName;
	private final String applicationName;
	private String ipAddress;
	private String hostname;

	/**
	 * Constructor.
	 * @param applicationName the application name
	 * @param rootInstanceName the root instance (machine) name
	 * @param eventName the event name
	 * @param eventInfo info about the event (eg. result of Nagios Livestatus query)
	 */
	public MsgMonitoringEvent(String eventName, String applicationName, String rootInstanceName, String eventInfo) {
		super();
		this.rootInstanceName = rootInstanceName;
		this.applicationName = applicationName;
		this.setEventName(eventName);
		this.setEventInfo(eventInfo);
		
		// TODO add ipAddress to constructor ??
		try {
			this.ipAddress = InetAddress.getLocalHost().getHostAddress();
			this.hostname = InetAddress.getLocalHost().getCanonicalHostName();
		} catch (UnknownHostException e) {
			this.ipAddress = null;
			this.hostname = null;
		}
	}

	/**
	 * Constructor.
	 * @param applicationName the application name
	 * @param rootInstance the root instance
	 * @param eventName the event name
	 * @param eventInfo info about the event (eg. result of Nagios Livestatus query)
	 */
	public MsgMonitoringEvent(String applicationName, Instance rootInstance, String eventName, String eventInfo) {
		this(applicationName, InstanceHelpers.findRootInstance( rootInstance ).getName(), eventName, eventInfo);
	}

	/**
	 * @return the event name
	 */
	public String getEventName() {
		return eventName;
	}

	public void setEventName(String eventName) {
		this.eventName = eventName;
	}

	/**
	 * @return the event info (eg. result of Nagios Livestatus query)
	 */
	public String getEventInfo() {
		return eventInfo;
	}

	public void setEventInfo(String eventInfo) {
		this.eventInfo = eventInfo;
	}
	
	/**
	 * @return the rootInstanceName
	 */
	public String getRootInstanceName() {
		return this.rootInstanceName;
	}

	/**
	 * @return the applicationName
	 */
	public String getApplicationName() {
		return this.applicationName;
	}
	
	/**
	 * @return the IP address
	 */
	public String getIpAddress() {
		return this.ipAddress;
	}
	
	/**
	 * @return the host name
	 */
	public String getHostname() {
		return this.hostname;
	}
	
	public String toString() {
		return "{\nevent name: " + eventName + "\n"
				+ "event info: " + getEventInfo() + "\n"
				+ "VM name: " + getRootInstanceName() + "\n"
				+ "application name:" + getApplicationName() + "\n"
				+ "host name " + getHostname() + "\n"
				+ "address " + getIpAddress() + "\n"
				+ "}";
	}
}
