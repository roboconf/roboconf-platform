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

	@Override
	public void contextInitialized( ServletContextEvent sce ) {

		File defaultDir = ManagerConfiguration.findConfigurationDirectory();
		ManagerConfiguration conf = ManagerConfiguration.createConfigurationDirectory( defaultDir );
		try {
			Manager.INSTANCE.initialize( conf );

		} catch( IOException e ) {
			Logger logger = Logger.getLogger( getClass().getName());
			logger.severe( "The DM initialization failed. " + e.getMessage());
			logger.finest( Utils.writeException( e ));
		}
	}


	@Override
	public void contextDestroyed( ServletContextEvent sce ) {
		Manager.INSTANCE.shutdown();
	}
}
