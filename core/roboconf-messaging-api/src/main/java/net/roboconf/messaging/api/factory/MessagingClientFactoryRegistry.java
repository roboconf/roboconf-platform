/**
 * Copyright 2014-2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.messaging.api.factory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

import net.roboconf.core.utils.Utils;

/**
 * A registry for {@link MessagingClientFactoryRegistry} objects.
 * <p>
 * This class can, in an OSGi context, listen to the {@code MessagingClientFactoryRegistry} environmental services and
 * add/remove them automatically, so they are available to the
 * {@link net.roboconf.messaging.api.reconfigurables.ReconfigurableClient}s. In a test environment, or more generally
 * without a containing OSGi framework, {@code MessagingClientFactoryRegistry} objects must be registered manually.
 * </p>
 *
 * @author Pierre Bourret - Université Joseph Fourier
 */
public class MessagingClientFactoryRegistry {

	/**
	 * The messaging client factories.
	 */
	private final ConcurrentHashMap<String, IMessagingClientFactory> factories = new ConcurrentHashMap<> ();

	/**
	 * The messaging client factory listeners.
	 */
	private final ConcurrentLinkedQueue<MessagingClientFactoryListener> listeners = new ConcurrentLinkedQueue<> ();

	/**
	 * The logger.
	 */
	private final Logger logger = Logger.getLogger( getClass().getName());



	/**
	 * @return the messaging client factory with the given type, or {@code null} if there is no registered factory
	 * with the given type.
	 */
	public IMessagingClientFactory getMessagingClientFactory(String type) {

		IMessagingClientFactory result = null;
		if (type != null)
			result = this.factories.get(type);

		return result;
	}

	/**
	 * Adds a messaging client factory to this registry, unless a similar one was already registered.
	 * @param factory the messaging client factory to add.
	 * @return {@code true} if the factory has been added, {@code false} if a factory with the same type was already registered.
	 */
	public boolean addMessagingClientFactory(IMessagingClientFactory factory) {

		final String type = factory.getType();
		this.logger.fine("Adding messaging client factory: " + type);
		final boolean result = this.factories.putIfAbsent(type, factory) == null;
		if (result)
			notifyListeners(factory, true);

		return result;
	}

	/**
	 * Removes a messaging client factory from this registry.
	 * @param factory the messaging client factory to remove.
	 * @return {@code true} if the factory has been removed, {@code false} if the factory was not registered.
	 */
	public boolean removeMessagingClientFactory(IMessagingClientFactory factory) {

		final String type = factory.getType();
		this.logger.fine("Removing messaging client factory: " + type);
		final boolean result = this.factories.remove(type, factory);
		if (result)
			notifyListeners(factory, false);

		return result;
	}

	/**
	 * Adds the given messaging client factory listener.
	 * <p>
	 * The given listener will begin to be notified of the {@code IMessagingClientFactory}-related events.
	 * </p>
	 * @param listener the listener to add.
	 */
	public void addListener(MessagingClientFactoryListener listener) {
		this.listeners.add(listener);
	}

	/**
	 * Removes the given messaging client factory listener.
	 * <p>
	 * The given listener will stop to be notified of the {@code IMessagingClientFactory}-related events.
	 * </p>
	 * @param listener the listener to remove.
	 */
	public void removeListener(MessagingClientFactoryListener listener) {
		this.listeners.remove(listener);
	}


	/**
	 * Notifies the messaging client factory listeners that a factory has been added/removed.
	 * @param factory the incoming/outgoing messaging client factory.
	 * @param isAdded flag indicating whether the factory has been added or removed.
	 */
	private void notifyListeners(IMessagingClientFactory factory, boolean isAdded) {

		for (MessagingClientFactoryListener listener : this.listeners) {
			try {
				if (isAdded)
					listener.addMessagingClientFactory(factory);
				else
					listener.removeMessagingClientFactory(factory);

			} catch (Throwable t) {
				// Log the exception, but *do not* interrupt the notification of the other listeners.
				this.logger.warning("Messaging client factory listener has thrown an exception: " + listener);
				Utils.logException(this.logger, new RuntimeException(t));
			}
		}
	}
}
