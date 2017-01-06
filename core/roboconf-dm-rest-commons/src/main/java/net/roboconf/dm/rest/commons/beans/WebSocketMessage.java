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

package net.roboconf.dm.rest.commons.beans;

import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.runtime.EventType;

/**
 * A class to serialize web sockets messages as JSon.
 * @author Vincent Zurczak - Linagora
 */
public class WebSocketMessage {

	private Application application;
	private EventType eventType;
	private ApplicationTemplate applicationTemplate;
	private Instance instance;
	private String message;


	/**
	 * Constructor.
	 * @param application
	 * @param eventType
	 */
	public WebSocketMessage( Application application, EventType eventType ) {
		this.application = application;
		this.eventType = eventType;
	}

	/**
	 * Constructor.
	 * @param applicationTemplate
	 * @param eventType
	 */
	public WebSocketMessage( ApplicationTemplate applicationTemplate, EventType eventType ) {
		this.applicationTemplate = applicationTemplate;
		this.eventType = eventType;
	}

	/**
	 * Constructor.
	 * @param instance
	 * @param application
	 * @param eventType
	 */
	public WebSocketMessage( Instance instance, Application application, EventType eventType ) {
		this.application = application;
		this.eventType = eventType;
		this.instance = instance;
	}

	/**
	 * Constructor.
	 * @param message
	 */
	public WebSocketMessage( String message ) {
		this.message = message;
	}

	/**
	 * @return the application
	 */
	public Application getApplication() {
		return this.application;
	}

	/**
	 * @return the eventType
	 */
	public EventType getEventType() {
		return this.eventType;
	}

	/**
	 * @return the applicationTemplate
	 */
	public ApplicationTemplate getApplicationTemplate() {
		return this.applicationTemplate;
	}

	/**
	 * @return the instance
	 */
	public Instance getInstance() {
		return this.instance;
	}

	/**
	 * @return the message
	 */
	public String getMessage() {
		return this.message;
	}
}
