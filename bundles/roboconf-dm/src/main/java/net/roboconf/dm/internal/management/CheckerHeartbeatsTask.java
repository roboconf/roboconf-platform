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

package net.roboconf.dm.internal.management;

import java.util.TimerTask;

import net.roboconf.dm.management.Manager;

/**
 * @author Vincent Zurczak - Linagora
 */
public class CheckerHeartbeatsTask extends TimerTask {

	private final Manager manager;


	/**
	 * Constructor.
	 * @param manager
	 */
	public CheckerHeartbeatsTask( Manager manager ) {
		this.manager = manager;
	}


	/*
	 * (non-Javadoc)
	 * @see java.util.TimerTask#run()
	 */
	@Override
	public void run() {
		for( ManagedApplication ma : this.manager.getAppNameToManagedApplication().values())
			ma.checkStates();
	}
}
