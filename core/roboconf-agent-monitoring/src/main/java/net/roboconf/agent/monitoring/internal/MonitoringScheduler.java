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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;

import net.roboconf.messaging.MessagingConstants;
import net.roboconf.messaging.internal.utils.RabbitMqUtils;
import net.roboconf.messaging.internal.utils.SerializationUtils;
import net.roboconf.messaging.messages.Message;
import net.roboconf.messaging.reconfigurables.ReconfigurableClientAgent;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;

/**
 * Scheduler for periodic monitoring checks (polling).
 * @author Pierre-Yves Gibello - Linagora
 */
public class MonitoringScheduler extends TimerTask {

	private Timer timer = new Timer();
	List<MonitoringTaskHandler> handlerList = new LinkedList<MonitoringTaskHandler>();
	private String applicationName;
	private String messageServerIp;
	private String messageServerUsername;
	private String messageServerPassword;
	private ReconfigurableClientAgent messagingClient;
	private String vmInstanceName;
	
	public MonitoringScheduler(String applicationName, String vmInstanceName, File conf) throws FileNotFoundException, IOException {
		this.applicationName = applicationName;
		this.vmInstanceName = vmInstanceName;
		
		BufferedReader reader = new BufferedReader(new FileReader(conf));
		String line;
		String eventName = null;
		String eventInfo[] = null;

		try {
			while((line = reader.readLine()) != null) {

				if(line.startsWith("[EVENT")) { // Found event declaration, extract name
					eventName = line.substring(6).trim();
					if(eventName.endsWith("]")) eventName = eventName.substring(0, eventName.length()-1).trim();
					eventInfo = (new NagiosEventParser()).parse(reader);

					handlerList.add(new NagiosTaskHandler(eventName,
							applicationName, vmInstanceName,
							eventInfo));
				}
			}
		} finally {
			try { reader.close(); } catch(Exception e) { /* ignore */ }
		} 
	}
	
	/**
     * Start the task processing.
	 * @throws IOException 
     */
    public void startProcessing(String messageServerIp, String messageServerUsername, String messageServerPassword) throws IOException {
    	this.messageServerIp = messageServerIp;
    	this.messageServerUsername = messageServerUsername;
    	this.messageServerPassword = messageServerPassword;
    	
    	ReconfigurableClientAgent c = new ReconfigurableClientAgent();
    	c.setApplicationName(this.applicationName);
    	c.setRootInstanceName(this.vmInstanceName);
    	c.switchMessagingClient(messageServerIp, messageServerUsername, messageServerPassword, MessagingConstants.FACTORY_RABBIT_MQ);

    	startProcessing(c);
    }
    
    /**
     * Start the task processing.
     */
    public void startProcessing(ReconfigurableClientAgent messagingClient) {

    	this.messagingClient = messagingClient;
    	
    	//TODO make period configurable ?
    	timer.schedule(this, 0, 10000);
    }

	/**
     * Stop the task processing.
     */
    public void stopProcessing() {
        timer.cancel();
    }

	@Override
	public void run() {
		
		for(MonitoringTaskHandler handler : handlerList) {
			Message msg = handler.process();
			if(msg == null) continue;

			//Channel channel = null;
			try {
				/*
				ConnectionFactory factory = new ConnectionFactory();
				RabbitMqUtils.configureFactory(factory,
						this.messageServerIp, 
						this.messageServerUsername,
						this.messageServerPassword);

				channel = factory.newConnection().createChannel();

				// TODO Now we'll use agent2DM channel (use specific one instead ??)

				// Exchange declaration is idempotent.
				// TODO replace next 2 lines with RabbitMqUtils.declareApplicationExchanges(applicationName, channel) ?
				String dmExchangeName = RabbitMqUtils.buildExchangeName(applicationName, true); // Listen to DM messages
				channel.exchangeDeclare(dmExchangeName, "fanout");

				// Queue declaration is idempotent
				String queueName = applicationName + ".monitoring";
				//String queueName = channel.queueDeclare().getQueue();
				//logger.info("Queue name: " + queueName);
				channel.queueDeclare(queueName, true, false, true, null);

				// queueBind is idempotent
				channel.queueBind(queueName, dmExchangeName, "");
				
				channel.basicPublish(dmExchangeName, "", null,
						SerializationUtils.serializeObject(msg));
				*/

				messagingClient.sendMessageToTheDm(msg);

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				//try { if (channel != null) channel.close(); } catch(Exception e) { /*ignore*/ }
			}
		}
	}
	
	public static void main(String args[]) throws Exception {
		File conf = new File(Thread.currentThread().getContextClassLoader()
				.getResource("nagiosevents.conf").getFile());
		MonitoringScheduler sched = new MonitoringScheduler("appName", "VM1", conf);
		sched.startProcessing("localhost", "roboconf", "roboconf");
	}
}
