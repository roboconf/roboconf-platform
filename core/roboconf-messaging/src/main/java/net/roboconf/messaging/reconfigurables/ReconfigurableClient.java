/**
 * Copyright 2014-2015 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.messaging.reconfigurables;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

import net.roboconf.core.utils.Utils;
import net.roboconf.messaging.MessagingConstants;
import net.roboconf.messaging.client.IClient;
import net.roboconf.messaging.factory.MessagingClientFactory;
import net.roboconf.messaging.factory.MessagingClientFactoryListener;
import net.roboconf.messaging.factory.MessagingClientFactoryRegistry;
import net.roboconf.messaging.processors.AbstractMessageProcessor;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

/**
 * A class that can switch dynamically between messaging types.
 * @param <T> a sub-class of {@link IClient}
 * @author Vincent Zurczak - Linagora
 */
public abstract class ReconfigurableClient<T extends IClient> implements IClient, MessagingClientFactoryListener {

	private final Logger logger = Logger.getLogger( getClass().getName());
	private AbstractMessageProcessor<T> messageProcessor;
	private String messagingType;
	private T messagingClient;
	private MessagingClientFactoryRegistry registry;

	protected ReconfigurableClient() {
		// Try to find the MessagingClientFactoryRegistry service.
		setRegistry(lookupMessagingClientFactoryRegistryService());
	}

	/**
	 * @return the {@code MessagingClientFactoryRegistry} associated to this client.
	 */
	public synchronized MessagingClientFactoryRegistry getRegistry() {
		return this.registry;
	}

	/**
	 * Sets the {@code MessagingClientFactoryRegistry} associated for this client.
	 * @param registry the {@code MessagingClientFactoryRegistry} for this client.
	 */
	public synchronized void setRegistry(MessagingClientFactoryRegistry registry) {
		if (this.registry != null) {
			this.registry.removeListener(this);
		}
		this.registry = registry;
		if (registry != null) {
			registry.addListener(this);
		}
	}

	/**
	 * Try to locate the {@code MessagingClientFactoryRegistry} service in an OSGi execution context.
	 * <p>
	 * NOTE: this method is, by definition, very dirty.
	 * </p>
	 * @return the located {@code MessagingClientFactoryRegistry} service, or {@code null} if the service cannot be
	 * found, or if there is no OSGi execution context.
	 */
	public static MessagingClientFactoryRegistry lookupMessagingClientFactoryRegistryService() {
		MessagingClientFactoryRegistry result = null;
		final Bundle bundle = FrameworkUtil.getBundle(ReconfigurableClient.class);
		if (bundle != null) {
			final BundleContext bundleContext = bundle.getBundleContext();
			if (bundleContext != null) {
				// There must be only *one* MessagingClientFactoryRegistry service.
				final ServiceReference<MessagingClientFactoryRegistry> reference =
						bundleContext.getServiceReference(MessagingClientFactoryRegistry.class);
				if (reference != null) {
					result = bundleContext.getService(reference);
					// The service will be unget when this bundle stops. No need to worry!
				}
			}
		}
		return result;
	}

	@Override
	public synchronized String getMessagingType() {
		return this.messagingType;
	}

	/**
	 * Changes the internal messaging client.
	 * @param factoryName the factory name (see {@link MessagingConstants})
	 */
	public void switchMessagingType( String factoryName ) {

		// Create a new client
		T newMessagingClient = null;
		try {
			newMessagingClient = createMessagingClient(factoryName);
			if( newMessagingClient != null ) {
				newMessagingClient.setMessageQueue( this.messageProcessor.getMessageQueue());
				openConnection(newMessagingClient);
			}

		} catch( IOException e ) {
			this.logger.warning( "An error occurred while creating a new messaging client. " + e.getMessage());
			Utils.logException( this.logger, e );
		}

		// Replace the current client
		// (the new client may be null, it is not a problem - see #getMessagingClient())
		T oldClient;
		synchronized( this ) {
			oldClient = this.messagingClient;
			this.messagingClient = newMessagingClient;
			this.messagingType = factoryName;
		}

		closeConnection(oldClient, "The previous client could not be terminated correctly.");
	}

	@Override
	public void addMessagingClientFactory( final MessagingClientFactory factory ) {
		synchronized( this ) {
			if (this.messagingClient == null && factory.getType().equals(this.messagingType)) {
				// This is the messaging factory we were expecting...
				// We can try to switch to this incoming factory right now!

				// Create a new client
				T newMessagingClient = null;
				try {
					newMessagingClient = createMessagingClient(factory.getType());
					if( newMessagingClient != null ) {
						newMessagingClient.setMessageQueue( this.messageProcessor.getMessageQueue());
						openConnection(newMessagingClient);
					}

				} catch( IOException e ) {
					this.logger.warning( "An error occurred while creating a new messaging client. " + e.getMessage());
					Utils.logException( this.logger, e );
				}

				// Replace the current client
				// (the new client may be null, it is not a problem - see #getMessagingClient())
				this.messagingClient = newMessagingClient;
			}
		}
	}

	@Override
	public void removeMessagingClientFactory( final MessagingClientFactory factory ) {
		T oldClient = null;
		synchronized( this ) {
			if (this.messagingClient != null && this.messagingClient.getMessagingType().equals(this.messagingType)) {
				// This is the messaging factory we were using...
				// We must release our messaging client right now.
				oldClient = this.messagingClient;
				this.messagingClient = null;
			}
		}
		closeConnection(oldClient, "The previous client could not be terminated correctly.");
	}

	@Override
	public Map<String, String> getConfiguration() {
		final T messagingClient;
		final Map<String, String> result;
		synchronized (this) {
			messagingClient = this.messagingClient;
		}
		if (messagingClient != null) {
			result = this.messagingClient.getConfiguration();
		} else {
			result = Collections.emptyMap();
		}
		return result;
	}

	@Override
	public boolean setConfiguration( final Map<String, String> configuration ) {
		final T messagingClient;
		final String messagingType;
		final boolean result;
		synchronized (this) {
			messagingClient = this.messagingClient;
			messagingType = this.messagingType;
		}
		if (messagingClient != null && messagingType.equals(configuration.get(MESSAGING_TYPE_PROPERTY))) {
			result = messagingClient.setConfiguration(configuration);
		} else {
			// Guard against inapplicable configurations.
			result = false;
		}
		return result;
	}

	/**
	 * Creates a new messaging client and opens a connection with the messaging server.
	 * @param factoryName the factory name (see {@link MessagingConstants})
	 * @return a new messaging client, or {@code null} if {@code factoryName} is {@code null} or cannot be found in the
	 * available messaging factories.
	 * @throws IOException if something went wrong
	 */
	protected abstract T createMessagingClient( String factoryName )
	throws IOException;


	/**
	 * Configures a newly created client.
	 * <p>
	 * For an agent, it consists into listening to messages coming from the DM.
	 * </p>
	 * <p>
	 * For the DM, it consists in listening to messages coming from various agents.
	 * </p>
	 *
	 * @param newMessagingClient the messaging client to configure
	 * @throws IOException if something went wrong
	 */
	protected abstract void openConnection( T newMessagingClient ) throws IOException;


	/**
	 * @return a dismiss client for the case where the internal client is null
	 */
	protected abstract T getDismissedClient();


	/**
	 * Configures the message processor.
	 * @param messageProcessor the message processor
	 */
	protected abstract void configureMessageProcessor( AbstractMessageProcessor<T> messageProcessor );


	/**
	 * Associates a message processor with this instance.
	 * <p>
	 * The message processor cannot be started before. This method will start it.
	 * </p>
	 * <p>
	 * This method must be invoked only once.
	 * </p>
	 * @param messageProcessor the message processor
	 */
	public void associateMessageProcessor( AbstractMessageProcessor<T> messageProcessor ) {

		if( this.messageProcessor != null )
			throw new IllegalArgumentException( "The message processor was already defined." );

		this.messageProcessor = messageProcessor;
		configureMessageProcessor( messageProcessor );
		this.messageProcessor.start();
	}


	/**
	 * @return the message processor
	 */
	public AbstractMessageProcessor<T> getMessageProcessor() {
		return this.messageProcessor;
	}


	/**
	 * @return true if the internal client exists and is connected, false otherwise
	 */
	public synchronized boolean hasValidClient() {
		return this.messagingClient != null
				&& this.messagingClient.isConnected();
	}


	/**
	 * @return a messaging client (never null)
	 */
	protected synchronized T getMessagingClient() {
		return this.messagingClient != null ? this.messagingClient : getDismissedClient();
	}


	/**
	 * Resets the internal client (sets it to null).
	 */
	protected synchronized void resetInternalClient() {
		this.messagingClient = null;
		this.messagingType = null;
	}


	/**
	 * Closes the connection of a messaging client.
	 * @param client the client (may be null)
	 * @param errorMessage the error message to log in case of problem
	 */
	static void closeConnection( IClient client, String errorMessage ) {

		if( client != null ) {
			try {
				client.closeConnection();

			} catch( Exception e ) {
				Logger logger = Logger.getLogger( ReconfigurableClient.class.getName());
				logger.warning( errorMessage + " " + e.getMessage());
				Utils.logException( logger, e );
			}
		}
	}
}
