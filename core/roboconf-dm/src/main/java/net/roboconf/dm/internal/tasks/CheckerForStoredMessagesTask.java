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

package net.roboconf.dm.internal.tasks;

import java.util.TimerTask;
import java.util.logging.Logger;

import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.api.IApplicationMngr;
import net.roboconf.dm.management.api.IMessagingMngr;

/**
 * @author Vincent Zurczak - Linagora
 */
public class CheckerForStoredMessagesTask extends TimerTask {

	private final Logger logger = Logger.getLogger( getClass().getName());
	private final IMessagingMngr messagingMngr;
	private final IApplicationMngr appManager;


	/**
	 * Constructor.
	 * @param appManager
	 * @param messagingMngr
	 */
	public CheckerForStoredMessagesTask( IApplicationMngr appManager, IMessagingMngr messagingMngr ) {
		this.appManager = appManager;
		this.messagingMngr = messagingMngr;
	}


	@Override
	public void run() {

		this.logger.finest( "The task that checks stored messages runs." );
		for( ManagedApplication ma : this.appManager.getManagedApplications()) {
			for( Instance scopedInstance : InstanceHelpers.findAllScopedInstances( ma.getApplication()))
				this.messagingMngr.sendStoredMessages( ma, scopedInstance );
		}
	}
}
