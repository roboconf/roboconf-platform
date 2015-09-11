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

package net.roboconf.dm.management.api;

import java.io.IOException;

import net.roboconf.core.model.beans.Instance;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.messaging.api.messages.from_dm_to_dm.MsgEcho;

/**
 * @author Vincent Zurczak - Linagora
 */
public interface IDebugMngr {

	/**
	 * Pings the DM through the messaging queue.
	 * @param message the content of the Echo message to send
	 * @throws IOException if something bad happened
	 */
	void pingMessageQueue( String message ) throws IOException;

	/**
	 * Pings an agent through the messaging queue.
	 * @param app the application
	 * @param scopedInstance the scoped instance
	 * @param message the echo messages's content
	 * @throws IOException if something bad happened
	 */
	void pingAgent( ManagedApplication app, Instance scopedInstance, String message ) throws IOException;

	/**
	 * Invokes when an ECHO message was received.
	 * @param message an ECHO message
	 */
	void notifyMsgEchoReceived( MsgEcho message );
}
