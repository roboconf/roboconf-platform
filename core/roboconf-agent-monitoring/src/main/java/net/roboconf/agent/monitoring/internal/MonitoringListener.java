/**
 * Copyright 2013-2014 Linagora, Université Joseph Fourier
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

package net.roboconf.agent.monitoring.internal;

import java.util.logging.Level;
import java.util.logging.Logger;

import net.roboconf.messaging.processors.AbstractMessageProcessor;
import net.roboconf.messaging.internal.utils.RabbitMqUtils;
import net.roboconf.messaging.messages.Message;
import net.roboconf.messaging.messages.from_agent_to_dm.MsgNotifHeartbeat;
import net.roboconf.messaging.messages.from_agent_to_dm.MsgNotifMachineDown;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;

/**
 * For test...
 * @author Pierre-Yves Gibello - Linagora
 */
public class MonitoringListener {

	public static void main(String[] args) throws Exception {
		if(args.length < 4) throw new Exception("Mandatory args: messageServerIp, user, password, appName");
		String messageServerIp = args[0];
		String messageServerUsername = args[1];
		String messageServerPassword = args[2];
		String applicationName = args[3];
		
		final Logger logger = Logger.getLogger(MonitoringListener.class.getName());
		logger.setLevel(Level.ALL);
		
		ConnectionFactory factory = new ConnectionFactory();
		RabbitMqUtils.configureFactory(factory, messageServerIp, messageServerUsername, messageServerPassword);
		
		Channel channel = factory.newConnection().createChannel();

		// TODO Now we'll use agent2DM channel (use specific one instead ??)

		// Exchange declaration is idempotent.
		// TODO replace next 2 lines with RabbitMqUtils.declareApplicationExchanges(applicationName, channel) ?
		String dmExchangeName = RabbitMqUtils.buildExchangeName(applicationName, true); // Listen to DM messages
		channel.exchangeDeclare(dmExchangeName, "fanout");

		// Queue declaration is idempotent
		String queueName = applicationName + ".admin";
		//String queueName = channel.queueDeclare().getQueue();
		logger.info("Queue name: " + queueName);
		channel.queueDeclare(queueName, true, false, true, null);

		// queueBind is idempotent
		channel.queueBind(queueName, dmExchangeName, "");

		// Start to listen to the queue
		final QueueingConsumer consumer = new QueueingConsumer(channel);
		String consumerTag = channel.basicConsume(queueName, true, consumer);

		logger.info("Start listening...");

		final MonitoringMessageProcessor messageProcessor = new MonitoringMessageProcessor();
		messageProcessor.start();

		new Thread("Roboconf - Queue listener for monitoring") {
			@Override
			public void run() {
				RabbitMqUtils.listenToRabbitMq(
					"Monitoring Listener",
					logger,
					consumer,
					messageProcessor.getMessageQueue());
			};

		}.start();
	}
}

class MonitoringMessageProcessor extends AbstractMessageProcessor {

	public MonitoringMessageProcessor() {
		super("Roboconf Monitoring - Message processor");
	}

	private final Logger logger = Logger.getLogger(MonitoringMessageProcessor.class.getName());

	/**
	 * Processes a message (dispatch method).
	 * @param message (not null)
	 */
	@Override
	public void processMessage(Message message) {
		if(message instanceof MsgMonitoringEvent) {
			processMsgMonitoringEvent((MsgMonitoringEvent)message);
		}
		else if(message instanceof MsgNotifMachineDown )
			processMsgNotifMachineDown((MsgNotifMachineDown)message);
		else if(message instanceof MsgNotifHeartbeat)
			processMsgNotifHeartbeat((MsgNotifHeartbeat)message);
		else
			this.logger.warning("Monitoring listener: got a message of class " + message.getClass().getName());
	}

	private void processMsgMonitoringEvent(MsgMonitoringEvent message) {
		this.logger.info("Monitoring listener: EVENT !");
		this.logger.info(message.toString());
	}

	private void processMsgNotifHeartbeat(MsgNotifHeartbeat message) {
		this.logger.info("Monitoring listener: Heartbeat !");
		this.logger.info(message.toString());
	}

	private void processMsgNotifMachineDown(MsgNotifMachineDown message) {
		this.logger.info("Monitoring listener: Machine up !");
		this.logger.info(message.toString());
	}
}