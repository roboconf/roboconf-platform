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

package net.roboconf.testing;

import java.io.File;
import java.io.IOException;

import net.roboconf.core.actions.ApplicationAction;
import net.roboconf.core.model.helpers.ComponentHelpers;
import net.roboconf.core.model.runtime.Component;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.management.exceptions.AlreadyExistingException;
import net.roboconf.dm.management.exceptions.BulkActionException;
import net.roboconf.dm.management.exceptions.InexistingException;
import net.roboconf.dm.management.exceptions.InvalidActionException;
import net.roboconf.dm.management.exceptions.InvalidApplicationException;
import net.roboconf.dm.management.exceptions.UnauthorizedActionException;
import net.roboconf.plugin.api.ExecutionLevel;

/**
 * @author Vincent Zurczak - Linagora
 */
public class Debug {

	public static void main( String[] args ) {

		// Initialize the DM
		Manager.INSTANCE.tryToChangeMessageServerIp( "127.0.0.1" );

		// Load the application
		ManagedApplication ma = null;
		File applicationFilesDirectory = new File( "./sample" );
		try {
			ma = Manager.INSTANCE.loadNewApplication( applicationFilesDirectory );

		} catch( AlreadyExistingException e ) {
			e.printStackTrace();

		} catch( InvalidApplicationException e ) {
			e.printStackTrace();

		} catch( IOException e ) {
			e.printStackTrace();

		} finally {
			if( ma == null )
				return;
		}

		// Patch the application to run partially and thus debug it on this machine
		InMemoryIaasResolver testResolver = new InMemoryIaasResolver();
		testResolver.setExecutionLevel( ExecutionLevel.LOG );
		Manager.INSTANCE.setIaasResolver( testResolver );

		// We can even change the plug-ins
		for( Component c : ComponentHelpers.findAllComponents( ma.getApplication()))
			c.setInstallerName( "logger" );

		// Run administration actions through the DM...
		try {
			// Deploy everything
			Manager.INSTANCE.perform(
					ma.getApplication().getName(),
					ApplicationAction.deploy.toString(),
					null,
					true );

		} catch( InexistingException e ) {
			e.printStackTrace();

		} catch( InvalidActionException e ) {
			e.printStackTrace();

		} catch( UnauthorizedActionException e ) {
			e.printStackTrace();

		} catch( BulkActionException e ) {
			e.printStackTrace();
		}

		try {
			for( ;; ) {
				// nothing
			}

		} finally {
			try {
				Manager.INSTANCE.shutdownApplication( ma.getApplication().getName());
				Manager.INSTANCE.deleteApplication( ma.getApplication().getName());

			} catch( InexistingException e ) {
				e.printStackTrace();

			} catch( UnauthorizedActionException e ) {
				e.printStackTrace();

			} catch( BulkActionException e ) {
				e.printStackTrace();
			}
		}
	}
}
