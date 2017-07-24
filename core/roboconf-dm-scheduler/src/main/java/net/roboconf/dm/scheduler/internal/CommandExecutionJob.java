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

import java.util.logging.Logger;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.runtime.CommandHistoryItem;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.management.Manager;

/**
 * @author Vincent Zurczak - Linagora
 */
public class CommandExecutionJob implements Job {

	private final Logger logger = Logger.getLogger( getClass().getName());


	@Override
	public void execute( JobExecutionContext context )
	throws JobExecutionException {

		String appName = (String) context.getJobDetail().getJobDataMap().get( RoboconfScheduler.APP_NAME );
		String jobName = (String) context.getJobDetail().getJobDataMap().get( RoboconfScheduler.JOB_NAME );
		String commandsFileName = (String) context.getJobDetail().getJobDataMap().get( RoboconfScheduler.CMD_NAME );

		try {
			Manager manager = (Manager) context.getScheduler().getContext().get( RoboconfScheduler.MANAGER );
			Application app = manager.applicationMngr().findApplicationByName( appName );

			// The web console finds jobs by names, not IDs, which remain internal to Quartz
			manager.commandsMngr().execute( app, commandsFileName, CommandHistoryItem.ORIGIN_SCHEDULER, jobName );

		} catch( Exception e ) {
			this.logger.warning( "An error occurred while executing job " + jobName + " (command file =" + commandsFileName + ")." );
			Utils.logException( this.logger, e );
		}
	}
}
