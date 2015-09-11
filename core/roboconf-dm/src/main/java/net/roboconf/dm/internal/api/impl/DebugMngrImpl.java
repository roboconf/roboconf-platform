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

package net.roboconf.dm.internal.api.impl;

import java.io.IOException;
import java.util.logging.Logger;

import net.roboconf.core.model.beans.Instance;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.api.IDebugMngr;
import net.roboconf.dm.management.api.IMessagingMngr;
import net.roboconf.dm.management.api.INotificationMngr;
import net.roboconf.messaging.api.messages.from_dm_to_dm.MsgEcho;

/**
 * @author Noël - LIG
 * @author Pierre-Yves Gibello - Linagora
 * @author Vincent Zurczak - Linagora
 * @author Pierre Bourret - Université Joseph Fourier
 */
public class DebugMngrImpl implements IDebugMngr {

	private final Logger logger = Logger.getLogger( getClass().getName());
	private final IMessagingMngr messagingMngr;
	private final INotificationMngr notificationMngr;


	/**
	 * Constructor.
	 * @param messagingMngr
	 * @param notificationMngr
	 */
	public DebugMngrImpl( IMessagingMngr messagingMngr, INotificationMngr notificationMngr ) {
		this.messagingMngr = messagingMngr;
		this.notificationMngr = notificationMngr;
	}


	@Override
	public void pingMessageQueue( String message ) throws IOException {

		final MsgEcho sentMessage = new MsgEcho( message );
		this.messagingMngr.sendMessage( sentMessage );
		this.logger.fine( "Sent Echo message on debug queue. Message=" + message + ", UUID=" + sentMessage.getUuid());
	}


	@Override
	public void pingAgent( ManagedApplication app, Instance scopedInstance, String message ) throws IOException {

		MsgEcho ping = new MsgEcho( "PING:" + message );
		this.messagingMngr.sendMessage( app, scopedInstance, ping );
		this.logger.fine( "Sent PING request message=" + message + " to application=" + app + ", agent=" + scopedInstance );
	}


	@Override
	public void notifyMsgEchoReceived( MsgEcho message ) {
		this.notificationMngr.raw( message.getContent());
	}
}
