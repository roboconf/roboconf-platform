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

package net.roboconf.agent.monitoring.internal.file;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import net.roboconf.agent.monitoring.api.IMonitoringHandler;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.utils.Utils;
import net.roboconf.messaging.api.messages.from_agent_to_dm.MsgNotifAutonomic;

/**
 * Handler to check the existence of a file or a directory (useful for tests and demonstrations).
 * @author Pierre-Yves Gibello - Linagora
 */
public class FileHandler implements IMonitoringHandler {

	static final String HANDLER_NAME = "file";
	static final String DELETE_IF_EXISTS = "delete if exists";
	static final String NOTIFY_IF_NOT_EXISTS = "notify if not exists";

	private final Logger logger = Logger.getLogger( getClass().getName());

	private String applicationName, scopedInstancePath, eventId;
	String fileLocation;
	boolean deleteIfExists = false;
	boolean notifyIfNotExists = false;



	@Override
	public String getName() {
		return HANDLER_NAME;
	}


	@Override
	public void setAgentId( String applicationName, String scopedInstancePath ) {
		this.applicationName = applicationName;
		this.scopedInstancePath = scopedInstancePath;
	}


	@Override
	public void reset( Instance associatedInstance, String eventId, String fileContent ) {
		this.eventId = eventId;

		// We expect a single line / file, and that's it for now...
		fileContent = fileContent.trim();
		if( fileContent.contains( "\n" )) {
			this.logger.severe( "Invalid content for the 'file' handler in the agent's monitoring." );

		} else {
			// Back to defaults
			this.fileLocation = fileContent;
			this.notifyIfNotExists = false;
			this.deleteIfExists = false;

			// Update defaults if necessary
			if( this.fileLocation.toLowerCase().startsWith( DELETE_IF_EXISTS )) {
				this.deleteIfExists = true;
				this.fileLocation = this.fileLocation.substring( DELETE_IF_EXISTS.length()).trim();

			} else if( this.fileLocation.toLowerCase().startsWith( NOTIFY_IF_NOT_EXISTS )) {
				this.notifyIfNotExists = true;
				this.fileLocation = this.fileLocation.substring( NOTIFY_IF_NOT_EXISTS.length()).trim();
			}
		}
	}


	@Override
	public MsgNotifAutonomic process() {

		MsgNotifAutonomic result = null;
		try {
			if( this.fileLocation != null ) {
				File f = new File( this.fileLocation );
				String cause = null;

				// Check conditions
				if( ! f.exists() && this.notifyIfNotExists ) {
					cause = this.fileLocation + " does not exist.";

				} else if( f.exists()) {
					if( this.deleteIfExists )
						Utils.deleteFilesRecursively( f );

					// FIXME: does it make sense to NOT delete the file?
					cause = this.fileLocation + " was " + (this.deleteIfExists ? "deleted" : "checked") + ".";
				}

				// Create a message if necessary
				if( cause != null )
					result = new MsgNotifAutonomic( this.applicationName, this.scopedInstancePath, this.eventId, cause );
			}

		} catch( IOException e ) {
			this.logger.severe( "Cannot delete file " + this.fileLocation + ". Monitoring notification is discarded." );
			Utils.logException( this.logger, e );
		}

		return result;
	}
}
