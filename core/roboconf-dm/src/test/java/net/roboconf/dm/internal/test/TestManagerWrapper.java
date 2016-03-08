/**
 * Copyright 2015-2016 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.dm.internal.test;

import java.util.Map;

import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.management.api.IApplicationMngr;
import net.roboconf.messaging.api.extensions.IMessagingClient;
import net.roboconf.messaging.api.internal.client.test.TestClientFactory;
import net.roboconf.messaging.api.reconfigurables.ReconfigurableClientDm;

/**
 * A wrapper for the manager that provides access to internal fields.
 * <p>
 * Given the OSGi context, we should not expose some internal fields
 * and methods of the manager. However, we need such an exposure for tests.
 * This class aims at providing it.
 * </p>
 * <p>
 * This class uses Java reflection.
 * Hopefully, this should also work with PAX-Exam tests.
 * </p>
 *
 * @author Vincent Zurczak - Linagora
 */
public final class TestManagerWrapper {

	private final Manager manager;


	/**
	 * Constructor.
	 * @param manager
	 */
	public TestManagerWrapper( Manager manager ) {
		this.manager = manager;
	}


	/**
	 * @return the manager
	 */
	public Manager getManager() {
		return this.manager;
	}


	/**
	 * @return the DM's (reconfigurable) messaging client
	 * @throws IllegalAccessException
	 */
	public ReconfigurableClientDm getMessagingClient() throws IllegalAccessException {
		return TestUtils.getInternalField( this.manager, "messagingClient", ReconfigurableClientDm.class );
	}


	/**
	 * @return the DM's (internal) messaging client
	 * @throws IllegalAccessException
	 */
	public IMessagingClient getInternalMessagingClient() throws IllegalAccessException {
		ReconfigurableClientDm client = getMessagingClient();
		return getInternalMessagingClient( client );
	}


	/**
	 * @return the non-null map that registers applications
	 * @throws IllegalAccessException
	 */
	public Map<String,ManagedApplication> getNameToManagedApplication() throws IllegalAccessException {
		return getNameToManagedApplication( this.manager.applicationMngr());
	}


	/**
	 * @return the non-null map that registers application templates.
	 * @throws IllegalAccessException
	 */
	@SuppressWarnings( "unchecked" )
	public Map<ApplicationTemplate,Boolean> getApplicationTemplates() throws IllegalAccessException {
		return TestUtils.getInternalField( this.manager.applicationTemplateMngr(), "templates", Map.class );
	}


	/**
	 * Configures the messaging client.
	 * <p>
	 * This method MUST be invoked when (and only when) tests do not
	 * run in an OSGi environment.
	 * </p>
	 *
	 * @throws IllegalAccessException
	 */
	public void configureMessagingForTest() throws IllegalAccessException {
		this.manager.addMessagingFactory( new TestClientFactory());
	}


	/**
	 * @param client a messaging client
	 * @return the internal messaging client
	 * @throws IllegalAccessException
	 */
	public static IMessagingClient getInternalMessagingClient( ReconfigurableClientDm client )
	throws IllegalAccessException {
		return TestUtils.getInternalField( client, "messagingClient", IMessagingClient.class );
	}


	/**
	 * @param mngr an application manager
	 * @return the non-null map that registers applications
	 * @throws IllegalAccessException
	 */
	@SuppressWarnings( "unchecked" )
	public static Map<String,ManagedApplication> getNameToManagedApplication( IApplicationMngr mngr )
	throws IllegalAccessException {
		return TestUtils.getInternalField( mngr, "nameToManagedApplication", Map.class );
	}
}
