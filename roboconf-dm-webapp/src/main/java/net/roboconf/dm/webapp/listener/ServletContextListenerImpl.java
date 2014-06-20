/**
 * Copyright 2013-2014 Linagora, Université Joseph Fourier
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

package net.roboconf.dm.webapp.listener;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import net.roboconf.core.utils.Utils;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.management.ManagerConfiguration;

/**
 * @author Noël - LIG
 */
public class ServletContextListenerImpl implements ServletContextListener {

	private final Logger logger = Logger.getLogger( getClass().getName());


	@Override
	public void contextInitialized( ServletContextEvent sce ) {

		// Load and/or create a configuration
		ManagerConfiguration conf = null;
		File defaultDir = ManagerConfiguration.findConfigurationDirectory();
		if( defaultDir.exists()) {
			try {
				conf = ManagerConfiguration.loadConfiguration( defaultDir );

			} catch( IOException e ) {
				this.logger.severe( "Failed to load the default configuration. " + e.getMessage());
				this.logger.finest( Utils.writeException( e ));
			}
		}

		if( conf == null ) {
			try {
				conf = ManagerConfiguration.createConfiguration( defaultDir );

			} catch( IOException e ) {
				this.logger.severe( "Failed to create a configuration. " + e.getMessage());
				this.logger.finest( Utils.writeException( e ));
			}
		}

		// Initialize the DM
		try {
			Manager.INSTANCE.initialize( conf );

		} catch( IOException e ) {
			this.logger.severe( "The DM initialization failed. " + e.getMessage());
			this.logger.finest( Utils.writeException( e ));
		}
	}


	@Override
	public void contextDestroyed( ServletContextEvent sce ) {
		Manager.INSTANCE.shutdown();
	}
}
