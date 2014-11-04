/**
 * Copyright 2014 Linagora, Université Joseph Fourier
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

package net.roboconf.dm.internal.environment.messaging;

import java.io.IOException;

import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.messaging.client.IDmClient;
import net.roboconf.messaging.reconfigurables.ReconfigurableClientDm;

/**
 * @author Vincent Zurczak - Linagora
 */
public class RCDm extends ReconfigurableClientDm {

	private final Manager manager;


	/**
	 * Constructor.
	 * @param manager
	 */
	public RCDm( Manager manager ) {
		super();
		this.manager = manager;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.messaging.reconfigurables.ReconfigurableClientDm
	 * #openConnection(net.roboconf.messaging.client.IDmClient)
	 */
	@Override
	protected void openConnection( IDmClient newMessagingClient ) throws IOException {
		super.openConnection( newMessagingClient );
		for( ManagedApplication ma : this.manager.getAppNameToManagedApplication().values())
			newMessagingClient.listenToAgentMessages( ma.getApplication(), ListenerCommand.START );
	}
}
