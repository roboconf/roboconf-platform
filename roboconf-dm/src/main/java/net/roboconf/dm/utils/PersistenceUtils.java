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

package net.roboconf.dm.utils;

import java.util.ArrayList;
import java.util.Collection;

import net.roboconf.core.model.runtime.Instance;
import net.roboconf.core.model.runtime.Instance.InstanceStatus;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.persistence.IDmStorage.DmStorageApplicationBean;
import net.roboconf.dm.persistence.IDmStorage.DmStorageBean;
import net.roboconf.dm.persistence.IDmStorage.DmStorageRootInstanceBean;

/**
 * @author Vincent Zurczak - Linagora
 */
public class PersistenceUtils {

	/**
	 * Retrieves the manager's state and stores it into a bean to be persisted later.
	 * <p>
	 * This is a convenience method.
	 * </p>
	 *
	 * @param alreadyLoadedApplications a non-null list of already loaded {@link ManagedApplication}s
	 * @param newApplication an application which is being loaded but not yet registered into the manager
	 * @param messagingServerIp the IP address of the messaging server
	 * @return a non-null bean with information to be persisted
	 */
	public static DmStorageBean retrieveManagerState(
			Collection<ManagedApplication> alreadyLoadedApplications,
			ManagedApplication newApplication,
			String messagingServerIp  ) {

		Collection<ManagedApplication> apps = new ArrayList<ManagedApplication> ();
		apps.addAll( alreadyLoadedApplications );
		apps.add( newApplication );

		return retrieveManagerState( apps, messagingServerIp );
	}


	/**
	 * Retrieves the manager's state and stores it into a bean to be persisted later.
	 * @param applications a non-null list of {@link ManagedApplication}s
	 * @param messagingServerIp the IP address of the messaging server
	 * @return a non-null bean with information to be persisted
	 */
	public static DmStorageBean retrieveManagerState( Collection<ManagedApplication> applications, String messagingServerIp  ) {

		DmStorageBean result = new DmStorageBean();
		result.messagingServerIp( messagingServerIp );

		for( ManagedApplication ma : applications ) {
			DmStorageApplicationBean appBean = new DmStorageApplicationBean();
			result.getApplications().add( appBean );

			appBean.applicationName( ma.getApplication().getName());
			appBean.applicationDirectoryPath( ma.getApplicationFilesDirectory());

			for( Instance rootInstance : ma.getApplication().getRootInstances()) {
				if( rootInstance.getStatus() == InstanceStatus.NOT_DEPLOYED )
					continue;

				DmStorageRootInstanceBean rootInstanceBean = new DmStorageRootInstanceBean();
				appBean.getRootInstances().add( rootInstanceBean );

				rootInstanceBean.rootInstanceName( rootInstance.getName());
				rootInstanceBean.ipAddress( rootInstance.getData().get( Instance.IP_ADDRESS ));
				rootInstanceBean.machineId( rootInstance.getData().get( Instance.MACHINE_ID ));
				rootInstanceBean.componentName( rootInstance.getComponent().getName());
			}
		}

		return result;
	}
}
