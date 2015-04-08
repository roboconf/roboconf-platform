/**
 * Copyright 2014-2015 Linagora, Université Joseph Fourier, Floralis
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

import net.roboconf.agent.monitoring.internal.MonitoringHandler;
import net.roboconf.core.utils.Utils;
import net.roboconf.messaging.messages.from_agent_to_dm.MsgNotifAutonomic;

/**
 * Handler to check the existence of a file or a directory (useful for tests and demonstrations).
 * @author Pierre-Yves Gibello - Linagora
 */
public class FileHandler extends MonitoringHandler {

	static final String DELETE_IF_EXISTS = "delete if exists";
	static final String NOTIFY_IF_NOT_EXISTS = "notify if not exists";
	private final Logger logger = Logger.getLogger( getClass().getName());

	private String fileLocation;
	private boolean deleteIfExists = false;
	private boolean notifyIfNotExists = false;


	/**
	 * Constructor.
	 * @param eventId
	 * @param applicationName
	 * @param vmInstanceName
	 * @param fileContent
	 */
	public FileHandler( String eventName, String applicationName, String vmInstanceName, String fileContent ) {
		super( eventName, applicationName, vmInstanceName );

		// We expect a single line / file, and that's it for now...
		fileContent = fileContent.trim();
		if( fileContent.contains( "\n" )) {
			this.logger.severe( "Invalid content for the 'file' handler in the agent's monitoring." );

		} else {
			this.fileLocation = fileContent;
			if( this.fileLocation.toLowerCase().startsWith( DELETE_IF_EXISTS )) {
				this.deleteIfExists = true;
				this.fileLocation = this.fileLocation.substring( DELETE_IF_EXISTS.length()).trim();

			} else if( this.fileLocation.toLowerCase().startsWith( NOTIFY_IF_NOT_EXISTS )) {
				this.notifyIfNotExists = true;
				this.fileLocation = this.fileLocation.substring( NOTIFY_IF_NOT_EXISTS.length()).trim();

			}
		}
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.agent.monitoring.internal.MonitoringHandler
	 * #process()
	 */
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


	/**
	 * @return the fileLocation
	 */
	public String getFileLocation() {
		return this.fileLocation;
	}

	/**
	 * @return the deleteIfExists
	 */
	public boolean isDeleteIfExists() {
		return this.deleteIfExists;
	}

	/**
	 * @return the notifyIfNotExists
	 */
	public boolean isNotifyIfNotExists() {
		return this.notifyIfNotExists;
	}
}
