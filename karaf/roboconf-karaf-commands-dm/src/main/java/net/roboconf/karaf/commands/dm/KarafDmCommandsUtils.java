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

package net.roboconf.karaf.commands.dm;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;

/**
 * @author Vincent Zurczak - Linagora
 */
public final class KarafDmCommandsUtils {

	/**
	 * Constructor.
	 */
	private KarafDmCommandsUtils() {
		// nothing
	}


	/**
	 * Finds instances for a given application.
	 *
	 * @param manager
	 * @param applicationName
	 * @param scopedInstancePath
	 * @param out
	 * @return an object with non-null list of instances and a (nullable) managed application
	 */
	public static RbcfInfo findInstances(
			Manager manager,
			String applicationName,
			String scopedInstancePath,
			PrintStream out ) {

		ManagedApplication ma = null;
		List<Instance> scopedInstances = new ArrayList<> ();
		Instance scopedInstance;

		if(( ma = manager.applicationMngr().findManagedApplicationByName( applicationName )) == null )
			out.println( "Unknown application: " + applicationName + "." );

		else if( scopedInstancePath == null )
			scopedInstances.addAll( InstanceHelpers.findAllScopedInstances( ma.getApplication()));

		else if(( scopedInstance = InstanceHelpers.findInstanceByPath( ma.getApplication(), scopedInstancePath )) == null )
			out.println( "There is no " + scopedInstancePath + " instance in " + applicationName + "." );

		else if( ! InstanceHelpers.isTarget( scopedInstance ))
			out.println( "Instance " + scopedInstancePath + " is not a scoped instance in " + applicationName + "." );

		else
			scopedInstances.add( scopedInstance );

		return new RbcfInfo( scopedInstances, ma );
	}


	/**
	 * @author Vincent Zurczak - Linagora
	 */
	public static final class RbcfInfo {

		List<Instance> scopedInstances;
		ManagedApplication ma;


		/**
		 * Constructor.
		 * @param scopedInstances
		 * @param ma
		 */
		private RbcfInfo( List<Instance> scopedInstances, ManagedApplication ma ) {
			this.scopedInstances = scopedInstances;
			this.ma = ma;
		}

		/**
		 * @return the scopedInstances
		 */
		public List<Instance> getScopedInstances() {
			return this.scopedInstances;
		}

		/**
		 * @return the ma
		 */
		public ManagedApplication getManagedApplication() {
			return this.ma;
		}
	}
}
