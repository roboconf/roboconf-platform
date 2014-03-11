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

import net.roboconf.core.model.runtime.Instance;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.management.exceptions.AlreadyExistingException;
import net.roboconf.dm.management.exceptions.ImpossibleInsertionException;
import net.roboconf.dm.management.exceptions.InexistingException;
import net.roboconf.dm.management.exceptions.InvalidApplicationException;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ApplicationTesterDemo {

	public void runTest() {

		// Initialize the DM
		Manager.INSTANCE.tryToChangeMessageServerIp( "your message server's IP" );

		// Load the application
		ManagedApplication ma = null;
		File applicationFilesDirectory = new File( "wherever you want" );
		try {
			ma = Manager.INSTANCE.loadNewApplication( applicationFilesDirectory );

		} catch( AlreadyExistingException e ) {
			e.printStackTrace();

		} catch( InvalidApplicationException e ) {
			e.printStackTrace();

		} finally {
			if( ma == null )
				return;
		}

		// Patch the application to run partially and thus debug it on this machine
		ApplicationTester.patch( ma );

		// Run administration actions through the DM...
		try {
			Manager.INSTANCE.addInstance(
					ma.getApplication().getName(),
					null,
					new Instance( "rootInstance" ));

		} catch( InexistingException e ) {
			e.printStackTrace();

		} catch( ImpossibleInsertionException e ) {
			e.printStackTrace();
		}
	}
}
