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

package net.roboconf.dm.scheduler.internal;

import java.util.Set;
import java.util.logging.Logger;

import org.quartz.TriggerKey;
import org.quartz.impl.matchers.GroupMatcher;

import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.runtime.EventType;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.management.events.IDmListener;

/**
 * A listener that allows to delete scheduled jobs for a given application.
 * @author Vincent Zurczak - Linagora
 */
public class ManagerListener implements IDmListener {

	static final String ID = "Roboconf Scheduler";

	private final Logger logger = Logger.getLogger( getClass().getName());
	private final RoboconfScheduler scheduler;


	/**
	 * Constructor.
	 * @param scheduler
	 */
	public ManagerListener( RoboconfScheduler scheduler ) {
		this.scheduler = scheduler;
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public void enableNotifications() {
		// nothing
	}

	@Override
	public void disableNotifications() {
		// nothing
	}

	@Override
	public void applicationTemplate( ApplicationTemplate tpl, EventType eventType ) {
		// nothing
	}

	@Override
	public void instance( Instance instance, Application application, EventType eventType ) {
		// nothing
	}

	@Override
	public void raw( String message, Object... data ) {
		// nothing
	}


	@Override
	public void application( Application application, EventType eventType ) {

		if( eventType == EventType.DELETED ) {
			this.logger.fine( "Application " + application + " was deleted. Associated jobs are about to be deleted." );
			try {
				Set<TriggerKey> tks = this.scheduler.scheduler.getTriggerKeys( GroupMatcher.triggerGroupEquals( application.getName()));
				for( TriggerKey tk : tks ) {
					try {
						this.scheduler.deleteJob( tk.getName());

					} catch( Exception e ) {
						this.logger.warning( "An error occurred while unscheduling job " + tk.getName() + ". " + e.getMessage());
						Utils.logException( this.logger, e );
					}
				}

			} catch( Exception e ) {
				this.logger.warning( "An error occurred while listing jobs for application " + application + ". " + e.getMessage());
				Utils.logException( this.logger, e );
			}
		}
	}
}
