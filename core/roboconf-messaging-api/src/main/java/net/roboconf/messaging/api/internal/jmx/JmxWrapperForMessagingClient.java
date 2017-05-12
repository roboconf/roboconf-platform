/**
 * Copyright 2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.messaging.api.internal.jmx;

import java.io.IOException;
import java.util.Date;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import net.roboconf.core.model.beans.Application;
import net.roboconf.core.utils.Utils;
import net.roboconf.messaging.api.extensions.IMessagingClient;
import net.roboconf.messaging.api.extensions.MessagingContext;
import net.roboconf.messaging.api.extensions.MessagingContext.RecipientKind;
import net.roboconf.messaging.api.internal.client.dismiss.DismissClient;
import net.roboconf.messaging.api.jmx.MessagingApiMBean;
import net.roboconf.messaging.api.jmx.RoboconfMessageQueue;
import net.roboconf.messaging.api.messages.Message;
import net.roboconf.messaging.api.utils.MessagingUtils;
import net.roboconf.messaging.api.utils.OsgiHelper;

/**
 * A class that wraps a real messaging client and deals with JMX stuff.
 * <p>
 * There is no need to make this class exposed outside this bundle.
 * </p>
 *
 * @author Vincent Zurczak - Linagora
 */
public class JmxWrapperForMessagingClient implements IMessagingClient, MessagingApiMBean {

	// Sent messages
	private final AtomicLong sentMessagesCount = new AtomicLong();
	private final AtomicLong failedSendingCount = new AtomicLong();
	private final AtomicLong timestampOfLastSendingFailure = new AtomicLong();
	private final AtomicLong timestampOfLastSentMessage = new AtomicLong();

	// The wrapped elements
	private final IMessagingClient messagingClient;
	private RoboconfMessageQueue messageQueue;
	private String id;

	// Logger
	private final Logger logger = Logger.getLogger( getClass().getName());

	// OSGi stuff
	ServiceRegistration<MessagingApiMBean> serviceReg;


	/**
	 * Constructor.
	 * @param messagingClient
	 */
	public JmxWrapperForMessagingClient( IMessagingClient messagingClient ) {
		this( messagingClient, new OsgiHelper());
	}


	/**
	 * Constructor for tests only.
	 * @param messagingClient
	 * @param osgiHelper
	 */
	JmxWrapperForMessagingClient( IMessagingClient messagingClient, OsgiHelper osgiHelper ) {
		this.messagingClient = messagingClient != null ? messagingClient : new DismissClient();

		// Register the object as service in the OSGi registry.
		// Apache Aries should then map it to a MBean if the JMX management
		// is available. It is a little bit dirty.
		BundleContext bundleCtx = osgiHelper.findBundleContext();
		if( bundleCtx != null ) {

			this.logger.fine( "Running in an OSGi environment. Trying to register a MBean for the messaging." );
			Dictionary<String,String> properties = new Hashtable<> ();
			properties.put( "jmx.objectname", "net.roboconf:type=messaging" );
			try {
				this.serviceReg = bundleCtx.registerService( MessagingApiMBean.class, this, properties );
				this.logger.fine( "A MBean was successfully registered for the messaging." );

			} catch( Exception e ) {
				this.logger.severe( "A MBean could not be registered for the messaging." );
				Utils.logException( this.logger, e );
			}
		}
	}


	/**
	 * Unregisters the service that was created from this class instance.
	 * <p>
	 * This method MUST be invoked every time a client is not necessary anymore.
	 * </p>
	 */
	public void unregisterService() {

		try {
			if( this.serviceReg != null ) {
				this.logger.finer( "Running in an OSGi environment. Trying to unregister a MBean for the messaging." );
				this.serviceReg.unregister();
				this.logger.finer( "Running in an OSGi environment. The MBean was unregistered." );
			}

		} catch( Exception e ) {
			this.logger.severe( "A MBean could not be unregistered for the messaging." );
			Utils.logException( this.logger, e );

		} finally {
			this.serviceReg = null;
		}
	}


	/**
	 * @return true if the wrapped messaging client was null and thus replaced by a DismissClient
	 */
	public boolean isDismissClient() {
		return this.messagingClient instanceof DismissClient;
	}


	@Override
	public synchronized void reset() {

		this.sentMessagesCount.set( 0 );
		this.failedSendingCount.set( 0 );
		this.timestampOfLastSendingFailure.set( 0 );
		this.timestampOfLastSentMessage.set( 0 );

		if( this.messageQueue != null )
			this.messageQueue.reset();
	}


	@Override
	public String getId() {
		return this.id;
	}


	// Sent messages

	@Override
	public synchronized long getFailedSendingCount() {
		return this.failedSendingCount.get();
	}


	@Override
	public synchronized long getSentMessagesCount() {
		return this.sentMessagesCount.get();
	}


	@Override
	public synchronized long getTimestampOfLastSendingFailure() {
		return this.timestampOfLastSendingFailure.get();
	}


	@Override
	public synchronized long getTimestampOfLastSentMessage() {
		return this.timestampOfLastSentMessage.get();
	}


	// Received messages

	@Override
	public synchronized long getFailedReceptionCount() {
		return this.messageQueue == null ? 0 : this.messageQueue.getFailedReceptionCount();
	}


	@Override
	public synchronized long getReceivedMessagesCount() {
		return this.messageQueue == null ? 0 : this.messageQueue.getReceivedMessagesCount();
	}


	@Override
	public synchronized long getTimestampOfLastReceptionFailure() {
		return this.messageQueue == null ? 0 : this.messageQueue.getTimestampOfLastReceptionFailure();
	}


	@Override
	public synchronized long getTimestampOfLastReceivedMessage() {
		return this.messageQueue == null ? 0 : this.messageQueue.getTimestampOfLastReceivedMessage();
	}


	// Simple wrapped methods

	@Override
	public void closeConnection() throws IOException {
		this.messagingClient.closeConnection();
	}


	@Override
	public void openConnection() throws IOException {
		this.messagingClient.openConnection();
	}


	@Override
	public void deleteMessagingServerArtifacts( Application application )
	throws IOException {
		this.messagingClient.deleteMessagingServerArtifacts( application );
	}


	@Override
	public Map<String,String> getConfiguration() {
		return this.messagingClient.getConfiguration();
	}


	@Override
	public String getMessagingType() {
		return this.messagingClient.getMessagingType();
	}


	@Override
	public boolean isConnected() {
		return this.messagingClient.isConnected();
	}


	@Override
	public void subscribe( MessagingContext ctx ) throws IOException {
		this.messagingClient.subscribe( ctx );
	}


	@Override
	public void unsubscribe( MessagingContext ctx ) throws IOException {
		this.messagingClient.unsubscribe( ctx );
	}


	// Complex wrapped methods

	@Override
	public void setMessageQueue( RoboconfMessageQueue messageQueue ) {

		synchronized( this ) {
			this.messageQueue = messageQueue;
		}

		this.messagingClient.setMessageQueue( messageQueue );
	}


	@Override
	public void publish( MessagingContext ctx, Message msg ) throws IOException {

		try {
			this.messagingClient.publish( ctx, msg );
			this.sentMessagesCount.incrementAndGet();
			this.timestampOfLastSentMessage.set( new Date().getTime());

		} catch( IOException e ) {
			this.failedSendingCount.incrementAndGet();
			this.timestampOfLastSendingFailure.set( new Date().getTime());
			throw e;
		}
	}


	@Override
	public void setOwnerProperties(
			RecipientKind ownerKind,
			String domain,
			String applicationName,
			String scopedInstancePath ) {

		this.id = MessagingUtils.buildId( ownerKind, domain, applicationName, scopedInstancePath );
		this.messagingClient.setOwnerProperties( ownerKind, domain, applicationName, scopedInstancePath );
	}
}
