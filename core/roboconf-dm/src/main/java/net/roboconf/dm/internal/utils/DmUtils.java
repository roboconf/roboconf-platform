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

package net.roboconf.dm.internal.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.runtime.EventType;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.api.IInstancesMngr;
import net.roboconf.dm.management.api.INotificationMngr;

/**
 * @author Vincent Zurczak - Linagora
 */
public final class DmUtils {

	/**
	 * Private empty constructor.
	 */
	private DmUtils() {
		// nothing
	}


	/**
	 * Updates a scoped instance and its children to be marked as not deployed.
	 * @param scopedInstance a non-null scoped instance
	 * @param ma the managed application
	 * @param notificationMngr the notification manager (not null)
	 * @param instanceMngr the instances manager (not null)
	 */
	public static void markScopedInstanceAsNotDeployed(
			Instance scopedInstance,
			ManagedApplication ma,
			INotificationMngr notificationMngr,
			IInstancesMngr instanceMngr ) {

		scopedInstance.data.remove( Instance.IP_ADDRESS );
		scopedInstance.data.remove( Instance.MACHINE_ID );
		scopedInstance.data.remove( Instance.TARGET_ACQUIRED );
		scopedInstance.data.remove( Instance.RUNNING_FROM );
		scopedInstance.data.remove( Instance.READY_FOR_CFG_MARKER );

		// Update states
		List<Instance> instancesToDelete = new ArrayList<> ();
		for( Instance i : InstanceHelpers.buildHierarchicalList( scopedInstance )) {
			InstanceStatus oldstatus = i.getStatus();
			i.setStatus( InstanceStatus.NOT_DEPLOYED );

			// Send a notification only if there was a change
			if( oldstatus != InstanceStatus.NOT_DEPLOYED )
				notificationMngr.instance( i, ma.getApplication(), EventType.CHANGED );

			// DM won't send old imports upon restart...
			i.getImports().clear();


			// Do we need to delete the instance?
			if( i.data.containsKey( Instance.DELETE_WHEN_NOT_DEPLOYED ))
				instancesToDelete.add( i );
		}

		// Delete instances
		Collections.reverse( instancesToDelete );
		for( Instance i : instancesToDelete ) {
			try {
				instanceMngr.removeInstance( ma, i, true );

			} catch( Exception e ) {
				Logger logger = Logger.getLogger( DmUtils.class.getName());
				logger.severe(
						"An error occurred while deleting an instance marked with DELETE_WHEN_NOT_DEPLOYED. Instance = "
						+ InstanceHelpers.computeInstancePath( i )
						+ " App = " + ma.getName());

				Utils.logException( logger, e );
			}
		}
	}
}
