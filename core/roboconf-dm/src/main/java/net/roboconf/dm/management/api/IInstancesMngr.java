/**
 * Copyright 2015-2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.dm.management.api;

import java.io.IOException;

import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.exceptions.ImpossibleInsertionException;
import net.roboconf.dm.management.exceptions.UnauthorizedActionException;
import net.roboconf.target.api.TargetException;
import net.roboconf.target.api.TargetHandler;

/**
 * @author Vincent Zurczak - Linagora
 */
public interface IInstancesMngr {

	/**
	 * Adds an instance.
	 * @param ma the managed application
	 * @param parentInstance the parent instance
	 * @param instance the instance to insert (not null)
	 * @throws ImpossibleInsertionException if the instance could not be added
	 */
	void addInstance( ManagedApplication ma, Instance parentInstance, Instance instance )
	throws ImpossibleInsertionException, IOException;

	/**
	 * Invoked when an instance was modified and that we need to propagate these changes outside the DM.
	 * @param instance an instance (not null)
	 * @param ma the associated application
	 */
	void instanceWasUpdated( Instance instance, ManagedApplication ma );

	/**
	 * Removes an instance.
	 * <p>
	 * Equivalent to <code>removeInstance( ma, instance, true )</code>.
	 * </p>
	 *
	 * @param ma the managed application
	 * @param instance the instance to remove (not null)
	 * @throws UnauthorizedActionException if we try to remove an instance that seems to be running
	 * @throws IOException if an error occurred with the messaging
	 */
	void removeInstance( ManagedApplication ma, Instance instance )
	throws UnauthorizedActionException, IOException;

	/**
	 * Removes an instance or prepares Roboconf to delete it once it is possible.
	 * <p>
	 * If the instance can be deleted immediately, then no matter the value of the
	 * <code>immediate</code> parameter, we delete it immediately.
	 * </p>
	 *
	 * @param ma the managed application
	 * @param instance the instance to remove (not null)
	 * @param immediate true to delete it immediately, false to ask Roboconf to delete it when it is possible
	 * @throws UnauthorizedActionException if we try to remove immediately an instance that seems to be running
	 * @throws IOException if an error occurred with the messaging
	 */
	void removeInstance( ManagedApplication ma, Instance instance, boolean immediate )
	throws UnauthorizedActionException, IOException;;

	/**
	 * Restores instances states for a given application.
	 * <p>
	 * This method is invoked when the DM restarts or when it was reconfigured.
	 * It must verify that agents (their VM) that are associated with the given
	 * target handler, are still running. If so, the DM must send them a message
	 * to retrieve the remote states of the children instances.
	 * </p>
	 * <p>
	 * If the VM is not online anymore (e.g. external shutdown), then the instance
	 * and its children must be marked as not deployed.
	 * </p>
	 * <p>
	 * Scoped instances that are not associated with the given handler must be ignored.
	 * </p>
	 *
	 * @param ma a managed application (not null)
	 * @param targetHandler a target handler (not null)
	 */
	void restoreInstanceStates( ManagedApplication ma, TargetHandler targetHandler );

	/**
	 * Notifies all the agents they must re-export their variables.
	 * <p>
	 * Such an operation can be used when the messaging server was down and
	 * that messages were lost.
	 * </p>
	 * @throws IOException
	 */
	void resynchronizeAgents( ManagedApplication ma ) throws IOException;

	/**
	 * Changes the state of an instance.
	 * @param ma the managed application
	 * @param instance the instance whose state must be updated
	 * @param newStatus the new status
	 * @throws IOException if an error occurred with the messaging
	 * @throws TargetException if an error occurred with a target
	 */
	void changeInstanceState( ManagedApplication ma, Instance instance, InstanceStatus newStatus )
	throws IOException, TargetException;

	/**
	 * Deploys and starts all the instances of an application.
	 * @param ma an application
	 * @param instance the instance from which we deploy and start (can be null)
	 * <p>
	 * This instance and all its children will be deployed and started.
	 * If null, then all the application instances are considered.
	 * </p>
	 *
	 * @throws IOException if a problem occurred with the messaging
	 */
	void deployAndStartAll( ManagedApplication ma, Instance instance ) throws IOException;

	/**
	 * Stops all the started instances of an application.
	 * @param ma an application
	 * @param instance the instance from which we stop (can be null)
	 * <p>
	 * This instance and all its children will be stopped.
	 * If null, then all the application instances are considered.
	 * </p>
	 *
	 * @throws IOException if a problem occurred with the messaging
	 */
	void stopAll( ManagedApplication ma, Instance instance ) throws IOException;

	/**
	 * Undeploys all the instances of an application.
	 * @param ma an application
	 * @param instance the instance from which we undeploy (can be null)
	 * <p>
	 * This instance and all its children will be undeployed.
	 * If null, then all the application instances are considered.
	 * </p>
	 *
	 * @throws IOException if a problem occurred with the messaging
	 */
	void undeployAll( ManagedApplication ma, Instance instance ) throws IOException;
}
