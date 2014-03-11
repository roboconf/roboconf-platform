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

import net.roboconf.core.internal.utils.Utils;
import net.roboconf.core.model.runtime.Application;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.dm.environment.iaas.IaasHelpers;
import net.roboconf.iaas.api.IaasInterface;
import net.roboconf.iaas.api.exceptions.CommunicationToIaasException;
import net.roboconf.iaas.api.exceptions.IaasException;
import net.roboconf.iaas.api.exceptions.InvalidIaasPropertiesException;
import net.roboconf.messaging.client.IMessageServerClient;
import net.roboconf.messaging.client.InteractionType;
import net.roboconf.messaging.client.MessageServerClientFactory;
import net.roboconf.messaging.messages.Message;

/**
 * A class to interact with RabbitMQ and IaaS environments.
 * @author Vincent Zurczak - Linagora
 */
public class DmEnvironmentInterface implements IEnvironmentInterface {

	private Application application;
	private File applicationFilesDirectory;
	private String messageServerIp;
	private IMessageServerClient messageServerClient;


	/**
	 * Constructor.
	 */
	protected DmEnvironmentInterface() {
		// nothing
	}


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

		this.messageServerClient = MessageServerClientFactory.create();
		this.messageServerClient.setApplicationName( this.application.getName());
		this.messageServerClient.setMessageServerIp( this.messageServerIp );

		final Logger logger = Logger.getLogger( getClass().getName());
		try {
			this.messageServerClient.openConnection();
			new Thread( "Roboconf's Message Processor Thread" ) {
				@Override
				public void run() {
					try {
						DmEnvironmentInterface.this.messageServerClient.subscribeTo( InteractionType.AGENT_TO_DM, null );

					} catch( IOException e ) {
						logger.severe( "A message processor could not start on the Deployment Manager." );
						logger.finest( Utils.writeException( e ));
					}
				};

			}.start();

		} catch( IOException e ) {
			logger.severe( "A receiver for messages could not be initialized on the Deployment Manager." );
			logger.finest( Utils.writeException( e ));
		}
	}


	@Override
	public void cleanResources() {

		if( this.messageServerClient != null ) {
			final Logger logger = Logger.getLogger( getClass().getName());
			try {
				this.messageServerClient.closeConnection();

			} catch( IOException e ) {
				logger.finest( Utils.writeException( e ));
			}
		}
	}


	@Override
	public void sendMessage( Message message, Instance rootInstance ) {

		final Logger logger = Logger.getLogger( getClass().getName());
		try {
			this.messageServerClient.publish( InteractionType.DM_TO_AGENT, rootInstance.getName(), message );

		} catch( IOException e ) {
			logger.severe( "A message could not be send to " + rootInstance.getName());
			logger.finest( Utils.writeException( e ));
		}
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
