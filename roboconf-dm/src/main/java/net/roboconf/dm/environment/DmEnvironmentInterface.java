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

package net.roboconf.dm.environment;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;

import net.roboconf.core.model.runtime.Application;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.dm.environment.iaas.IaasHelpers;
import net.roboconf.dm.environment.messaging.DmMessageProcessor;
import net.roboconf.iaas.api.IaasInterface;
import net.roboconf.iaas.api.exceptions.CommunicationToIaasException;
import net.roboconf.iaas.api.exceptions.IaasException;
import net.roboconf.iaas.api.exceptions.InvalidIaasPropertiesException;
import net.roboconf.messaging.client.IMessageServerClient;
import net.roboconf.messaging.client.MessageServerClientRabbitMq;
import net.roboconf.messaging.messages.Message;
import net.roboconf.messaging.processing.MessageProcessorThread;
import net.roboconf.messaging.utils.MessagingUtils;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;

/**
 * A class to interact with RabbitMQ and IaaS environments.
 * @author Vincent Zurczak - Linagora
 */
class DmEnvironmentInterface implements IEnvironmentInterface {

	private Application application;
	private File applicationFilesDirectory;
	private String messageServerIp;

	private MessageProcessorThread	messageHandler;
	private IMessageServerClient messageServerClient;



	@Override
	public void setApplication( Application application ) {
		this.application = application;
	}


	@Override
	public void setApplicationFilesDirectory( File applicationFilesDirectory ) {
		this.applicationFilesDirectory = applicationFilesDirectory;
	}


	@Override
	public void setMessageServerIp( String messageServerIp ) {
		this.messageServerIp = messageServerIp;
	}


	@Override
	public void initializeResources() {

		this.messageServerClient = new MessageServerClientRabbitMq( this.messageServerIp, this.application.getName());
		try {
			ConnectionFactory factory = new ConnectionFactory();
			factory.setHost( this.messageServerIp );
			Connection connection = factory.newConnection();
			Channel channel = connection.createChannel();

			String exchangeName = MessagingUtils.buildDmExchangeName( this.application.getName());
			channel.exchangeDeclare( exchangeName, "fanout" );
			String queueName = channel.queueDeclare().getQueue();
			channel.queueBind( queueName, exchangeName, "" );

			QueueingConsumer consumer = new QueueingConsumer( channel );
			channel.basicConsume( queueName, true, consumer );
			this.messageHandler = new MessageProcessorThread( consumer, new DmMessageProcessor( this.application ));
			this.messageHandler.start();

		} catch( IOException e ) {
			Logger.getLogger( getClass().getName()).severe( "A receiver for messages could not be initialized on the Deployment Manager." );
		}
	}


	@Override
	public void cleanResources() {

		if( this.messageServerClient != null )
			this.messageServerClient.closeConnection();

		if( this.messageHandler != null )
			this.messageHandler.halt();

		this.messageHandler = null;
		/* TBD should close connections here ??
		try { channel.close(); } catch(IOException ignore) {}
		try { connection.close(); } catch(IOException ignore) {}
		*/
	}


	@Override
	public void sendMessage( Message message, Instance rootInstance ) {
		this.messageServerClient.sendMessage( message, rootInstance );
	}


	@Override
	public void terminateMachine( Instance instance ) throws IaasException {

		IaasInterface iaasInterface = findIaasInterface( instance );
		String machineId = instance.getData().get( Instance.MACHINE_ID );
		try {
			iaasInterface.terminateVM( machineId );

		} catch( CommunicationToIaasException e ) {
			throw new IaasException( e );
		}
	}


	@Override
	public void createMachine( Instance instance ) throws IaasException {

		IaasInterface iaasInterface = findIaasInterface( instance );
		try {
			String machineId = iaasInterface.createVM( this.messageServerIp, "", this.application.getName());
			// FIXME: the channel name is skipped here
			// As soon as we know what it is useful for, re-add it (it is in the instance)

			instance.getData().put( Instance.MACHINE_ID, machineId );

		} catch( CommunicationToIaasException e ) {
			throw new IaasException( e );
		}
	}


	private IaasInterface findIaasInterface( Instance instance ) throws IaasException {

		// FIXME: Not very "plug-in-like"
		IaasInterface iaasInterface;
		try {
			String installerName = instance.getComponent().getInstallerName();
			if( ! "iaas".equalsIgnoreCase( installerName ))
				throw new IaasException( "Unsupported installer name: " + installerName );

			Properties props = IaasHelpers.loadIaasProperties( this.applicationFilesDirectory, instance );
			iaasInterface = IaasHelpers.findIaasHandler( props );
			if( iaasInterface == null )
				throw new IaasException( "No IaaS handler was found for " + instance.getName() + "." );

			iaasInterface.setIaasProperties( props );

		} catch( IOException e ) {
			throw new IaasException( e );

		} catch( InvalidIaasPropertiesException e ) {
			throw new IaasException( e );
		}

		return iaasInterface;
	}
}
