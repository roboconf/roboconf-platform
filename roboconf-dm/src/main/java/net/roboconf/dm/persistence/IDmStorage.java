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

package net.roboconf.dm.persistence;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Vincent Zurczak - Linagora
 */
public interface IDmStorage {

	/**
	 * @return true if the restoration process requires sending messages to the agents
	 */
	boolean requiresAgentContact();

	/**
	 * Saves the manager's state.
	 * @param managerState the manager's state
	 * @throws IOException if the save operation failed
	 */
	void saveManagerState( DmStorageBean managerState ) throws IOException;

	/**
	 * Restores the manager's state.
	 * @throws IOException
	 */
	DmStorageBean restoreManagerState() throws IOException;



	/**
	 * A bean to store the manager's state.
	 * @author Vincent Zurczak - Linagora
	 */
	public static class DmStorageBean {
		private String messagingServerIp;
		private final Collection<DmStorageApplicationBean> applications = new ArrayList<IDmStorage.DmStorageApplicationBean> ();


		public DmStorageBean messagingServerIp( String messagingServerIp ) {
			this.messagingServerIp = messagingServerIp;
			return this;
		}

		public String getMessagingServerIp() {
			return messagingServerIp;
		}

		public Collection<DmStorageApplicationBean> getApplications() {
			return applications;
		}
	}


	/**
	 * A bean to store information about an application.
	 * @author Vincent Zurczak - Linagora
	 */
	public static class DmStorageApplicationBean {
		private String applicationName, applicationDirectoryPath;
		private final List<DmStorageRootInstanceBean> rootInstances = new ArrayList<IDmStorage.DmStorageRootInstanceBean> ();


		public DmStorageApplicationBean applicationName( String applicationName ) {
			this.applicationName = applicationName;
			return this;
		}

		public DmStorageApplicationBean applicationDirectoryPath( String applicationDirectoryPath ) {
			this.applicationDirectoryPath = applicationDirectoryPath;
			return this;
		}

		public DmStorageApplicationBean applicationDirectoryPath( File applicationDirectory ) {
			if( applicationDirectory != null )
				this.applicationDirectoryPath = applicationDirectory.getAbsolutePath();

			return this;
		}

		public String getApplicationName() {
			return applicationName;
		}

		public String getApplicationDirectoryPath() {
			return applicationDirectoryPath;
		}

		public List<DmStorageRootInstanceBean> getRootInstances() {
			return rootInstances;
		}
	}


	/**
	 * A bean to store information about root instances whose state is not <strong>NOT DEPLOYED</strong>.
	 * @author Vincent Zurczak - Linagora
	 */
	public static class DmStorageRootInstanceBean {
		private String rootInstanceName, ipAddress, machineId, componentName;


		public DmStorageRootInstanceBean rootInstanceName( String rootInstanceName ) {
			this.rootInstanceName = rootInstanceName;
			return this;
		}

		public DmStorageRootInstanceBean ipAddress( String ipAddress ) {
			this.ipAddress = ipAddress;
			return this;
		}

		public DmStorageRootInstanceBean machineId( String machineId ) {
			this.machineId = machineId;
			return this;
		}

		public DmStorageRootInstanceBean componentName( String componentName ) {
			this.componentName = componentName;
			return this;
		}

		public String getRootInstanceName() {
			return rootInstanceName;
		}

		public String getIpAddress() {
			return ipAddress;
		}

		public String getMachineId() {
			return machineId;
		}

		public String getComponentName() {
			return componentName;
		}
	}
}
