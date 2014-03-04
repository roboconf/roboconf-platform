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

package net.roboconf.dm.internal;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import net.roboconf.core.model.runtime.Application;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.dm.environment.IEnvironmentInterface;
import net.roboconf.messaging.messages.Message;

/**
 * A class to mock the messaging server and the IaaS.
 * @author Vincent Zurczak - Linagora
 */
public class TestEnvironmentInterface implements IEnvironmentInterface {

	public final Map<Message,Instance> messageToRootInstance = new HashMap<Message,Instance> ();
	public final Map<Instance,Boolean> machineToRunningStatus = new HashMap<Instance,Boolean> ();

	public AtomicBoolean resourcesWereCleaned = new AtomicBoolean( false );
	public AtomicBoolean resourcesWereInitialized = new AtomicBoolean( false );



	@Override
	public void cleanResources() {
		this.resourcesWereCleaned.set( true );
	}


	@Override
	public void initializeResources() {
		this.resourcesWereInitialized.set( true );
	}


	@Override
	public void setMessageServerIp( String messageServerIp ) {
		// nothing, we do not care
	}


	@Override
	public void setApplication( Application application ) {
		// nothing, we do not care
	}


	@Override
	public void setApplicationFilesDirectory( File applicationFilesDirectory ) {
		// nothing, we do not care
	}


	@Override
	public void sendMessage( Message message, Instance rootInstance ) {
		this.messageToRootInstance.put( message, rootInstance );
	}


	@Override
	public void terminateMachine( Instance rootInstance ) {
		this.machineToRunningStatus.put( rootInstance, false );
	}


	@Override
	public void createMachine( Instance rootInstance ) {
		this.machineToRunningStatus.put( rootInstance, true );
	}
}
