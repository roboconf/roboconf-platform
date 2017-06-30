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

package net.roboconf.messaging.api.reconfigurables;

import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.logging.Logger;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import net.roboconf.core.utils.Utils;
import net.roboconf.messaging.api.AbstractMessageProcessor;
import net.roboconf.messaging.api.MessagingConstants;
import net.roboconf.messaging.api.business.IClient;
import net.roboconf.messaging.api.extensions.IMessagingClient;
import net.roboconf.messaging.api.extensions.MessagingContext.RecipientKind;
import net.roboconf.messaging.api.factory.IMessagingClientFactory;
import net.roboconf.messaging.api.factory.MessagingClientFactoryListener;
import net.roboconf.messaging.api.factory.MessagingClientFactoryRegistry;
import net.roboconf.messaging.api.internal.jmx.JmxWrapperForMessagingClient;
import net.roboconf.messaging.api.utils.OsgiHelper;

/**
 * A class that can switch dynamically between messaging types.
 * @param <T> a sub-class of {@link IMessagingClient}
 * @author Vincent Zurczak - Linagora
 */
public abstract class ReconfigurableClient<T extends IClient> implements IClient, MessagingClientFactoryListener {

	protected final Logger logger = Logger.getLogger( getClass().getName());

	private AbstractMessageProcessor<T> messageProcessor;
	private String messagingType;
	private JmxWrapperForMessagingClient messagingClient;
	private MessagingClientFactoryRegistry registry;

	protected String domain;
	PrintStream console = System.out;


	/**
	 * Constructor.
	 */
	protected ReconfigurableClient() {
		resetInternalClient();

		// Try to find the MessagingClientFactoryRegistry service.
		setRegistry( lookupMessagingClientFactoryRegistryService( new OsgiHelper()));
	}


	@Override
	public void setDomain( String domain ) {
		this.domain = domain;
	}


	@Override
	public String getDomain() {
		return this.domain;
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
	public synchronized void setRegistry( MessagingClientFactoryRegistry registry ) {

		if( this.registry != null )
			this.registry.removeListener(this);

		this.registry = registry;
		if (registry != null)
			registry.addListener(this);
	}


	/**
	 * Try to locate the {@code MessagingClientFactoryRegistry} service in an OSGi execution context.
	 * <p>
	 * NOTE: this method is, by definition, quite dirty.
	 * </p>
	 * TODO: what happens when the registry component is being started, but the service is not yet registered. Wait? How long?
	 * @return the located {@code MessagingClientFactoryRegistry} service, or {@code null} if the service cannot be
	 * found, or if there is no OSGi execution context.
	 */
	public static MessagingClientFactoryRegistry lookupMessagingClientFactoryRegistryService( OsgiHelper osgiHelper ) {

		MessagingClientFactoryRegistry result = null;
		final Logger logger = Logger.getLogger( ReconfigurableClient.class.getName());

		BundleContext bundleCtx = osgiHelper.findBundleContext();
		if( bundleCtx != null ) {
			logger.info( "The messaging registry is used in an OSGi environment." );

			// There must be only *one* MessagingClientFactoryRegistry service.
			final ServiceReference<MessagingClientFactoryRegistry> reference =
					bundleCtx.getServiceReference( MessagingClientFactoryRegistry.class );

			// The service will be unget when this bundle stops. No need to worry!
			if( reference != null ) {
				logger.fine( "The service reference was found." );
				result = bundleCtx.getService( reference );
			}

		} else {
			logger.info( "The messaging registry is NOT used in an OSGi environment." );
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
		this.logger.fine( "The messaging is requested to switch its type to " + factoryName + "." );
		JmxWrapperForMessagingClient newMessagingClient = null;
		try {
			IMessagingClient rawClient = createMessagingClient( factoryName );
			if( rawClient != null ) {
				newMessagingClient = new JmxWrapperForMessagingClient( rawClient );
				newMessagingClient.setMessageQueue( this.messageProcessor.getMessageQueue());
				openConnection( newMessagingClient );
			}

		} catch( Exception e ) {
			this.logger.warning( "An error occurred while creating a new messaging client. " + e.getMessage());
			Utils.logException( this.logger, e );

			// #594: print a message to be visible in a console
			StringBuilder sb = new StringBuilder();
			sb.append( "\n\n**** WARNING ****\n" );
			sb.append( "Connection failed at " );
			sb.append( new SimpleDateFormat( "HH:mm:ss, 'on' EEEE dd (MMMM)" ).format( new Date()));
			sb.append( ".\n" );
			sb.append( "The messaging configuration may be invalid.\n" );
			sb.append( "Or the messaging server may not be started yet.\n\n" );
			sb.append( "Consider using the 'roboconf:force-reconnect' command if you forgot to start the messaging server.\n" );
			sb.append( "**** WARNING ****\n" );
			this.console.println( sb.toString());
		}

		// Replace the current client
		IMessagingClient oldClient;
		synchronized( this ) {

			// Simple changes
			this.messagingType = factoryName;
			oldClient = this.messagingClient;

			// The messaging client can NEVER be null
			if( newMessagingClient != null )
				this.messagingClient = newMessagingClient;
			else
				resetInternalClient();
		}

		terminateClient( oldClient, "The previous client could not be terminated correctly.", this.logger );
	}


	@Override
	public void addMessagingClientFactory( final IMessagingClientFactory factory ) {

		this.logger.fine( "A new messaging factory was added: " + factory.getType());
		synchronized( this ) {
			if( this.messagingClient.isDismissClient()
					&& factory.getType().equals( this.messagingType )) {

				// This is the messaging factory we were expecting...
				// We can try to switch to this incoming factory right now!
				switchMessagingType( this.messagingType );
			}
		}
	}


	@Override
	public void removeMessagingClientFactory( IMessagingClientFactory factory ) {

		String factoryType = factory != null ? factory.getType() : null;
		IMessagingClient oldClient = null;
		synchronized( this ) {
			if( this.messagingClient != null
					&& this.messagingClient.getMessagingType().equals( factoryType )) {

				// This is the messaging factory we were using...
				// We must release our messaging client right now.
				oldClient = resetInternalClient();
			}
		}

		terminateClient( oldClient, "The previous client could not be terminated correctly.", this.logger );
		this.logger.fine( "A messaging factory was removed: " + factoryType );
	}


	@Override
	public Map<String,String> getConfiguration() {

		final Map<String,String> result;
		synchronized( this ) {
			result = this.messagingClient.getConfiguration();
		}

		return result;
	}


	/**
	 * Creates a new messaging client.
	 * @param factoryName the factory name (see {@link MessagingConstants})
	 * @return a new messaging client, or {@code null} if {@code factoryName} is {@code null} or cannot be found in the
	 * available messaging factories.
	 * @throws IOException if something went wrong
	 */
	protected IMessagingClient createMessagingClient( String factoryName ) throws IOException {

		IMessagingClient client = null;
		MessagingClientFactoryRegistry registry = getRegistry();
		if( registry != null ) {
			IMessagingClientFactory factory = registry.getMessagingClientFactory(factoryName);
			if( factory != null )
				client = factory.createClient( this );
		}

		return client;
	}


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
	protected abstract void openConnection( IMessagingClient newMessagingClient ) throws IOException;


	/**
	 * Configures the message processor.
	 * @param messageProcessor the message processor
	 */
	protected abstract void configureMessageProcessor( AbstractMessageProcessor<T> messageProcessor );


	/**
	 * @return the kind of this client's owner (DM or AGENT).
	 */
	public abstract RecipientKind getOwnerKind();


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

		// The dismissed client always return false for this statement.
		return this.messagingClient.isConnected();
	}


	/**
	 * @return the messagingClient
	 */
	public IMessagingClient getMessagingClient() {
		return this.messagingClient;
	}


	/**
	 * Resets the internal client.
	 */
	protected synchronized IMessagingClient resetInternalClient() {

		IMessagingClient oldClient = this.messagingClient;
		this.messagingClient = new JmxWrapperForMessagingClient( null );
		return oldClient;
	}


	/**
	 * Closes the connection of a messaging client and terminates it properly.
	 * @param client the client (never null)
	 * @param errorMessage the error message to log in case of problem
	 * @param logger a logger
	 */
	static void terminateClient( IMessagingClient client, String errorMessage, Logger logger ) {

		try {
			logger.fine( "The reconfigurable client is requesting its internal connection to be closed." );
			if( client != null )
				client.closeConnection();

		} catch( Exception e ) {
			logger.warning( errorMessage + " " + e.getMessage());
			Utils.logException( logger, e );

		} finally {
			// "unregisterService" was not merged with "closeConnection"
			// on purpose. What is specific to JMX is restricted to this class
			// and this bundle. Sub-classes may use "closeConnection" without
			// any side effect on the JMX part.
			if( client instanceof JmxWrapperForMessagingClient )
				((JmxWrapperForMessagingClient) client).unregisterService();
		}
	}
}
