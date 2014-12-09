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
import java.util.logging.Logger;

import net.roboconf.agent.AgentMessagingInterface;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.messaging.messages.Message;

/**
 * Scheduler for periodic monitoring checks (polling).
 * @author Pierre-Yves Gibello - Linagora
 */
public class MonitoringScheduler extends TimerTask {

	private Logger logger = Logger.getLogger(getClass().getName());
	private Timer timer = new Timer();
	List<MonitoringTaskHandler> handlerList = new LinkedList<MonitoringTaskHandler>();
	//private String applicationName;
	/*private String messageServerIp;
	private String messageServerUsername;
	private String messageServerPassword;*/
	//private ReconfigurableClientAgent messagingClient;
	//private String vmInstanceName;
	private AgentMessagingInterface agentInterface;
	
	public MonitoringScheduler(AgentMessagingInterface agentInterface) throws FileNotFoundException, IOException {
		this.agentInterface = agentInterface;
	}

    /**
     * Start the task processing.
     */
    public void startProcessing() {
    	//TODO make period configurable ?
    	this.logger.fine("Autonomic monitoring scheduler started");
    	timer.schedule(this, 0, 10000);
    }

	/**
     * Stop the task processing.
     */
    public void stopProcessing() {
    	this.logger.fine("Autonomic monitoring scheduler stopped");
        timer.cancel();
    }

	@Override
	public void run() {
		
		this.logger.fine("MonitoringScheduler looking for autonomic rules...");

		// Root Instance may not yet have been injected: skip !
		if(this.agentInterface.getRootInstance() == null) {
			this.logger.fine("agentInterface.getRootInstance() is null... RootInstance not yet injected, skipping monitoring scheduling !");
			return;
		}
		
		for (Instance inst : InstanceHelpers.buildHierarchicalList(this.agentInterface.getRootInstance())) {

			File dir = InstanceHelpers.findInstanceDirectoryOnAgent(inst, inst.getComponent().getInstallerName());

			File conf = null;
			for(File f : dir.listFiles()) {
				if(f.getName().endsWith(".measures")) conf = f;
			}
			
			//File conf = new File(Thread.currentThread().getContextClassLoader()
					//.getResource("nagiosevents.conf").getFile());
			
			this.logger.fine("Monitoring instance " + inst.getName() + ", measures config =" + conf);
			
			if(conf != null) {
				BufferedReader reader = null;
				String line;
				String eventName = null;
				String eventInfo[] = null;

				try {
					reader = new BufferedReader(new FileReader(conf));
					while((line = reader.readLine()) != null) {

						if(line.startsWith("[EVENT")) { // Found event declaration, extract name
							eventName = line.substring(6).trim();
							if(eventName.endsWith("]")) eventName = eventName.substring(0, eventName.length()-1).trim();
							
							String tokens[] = eventName.split("\\s+");

							//FIXME check number of tokens...
							String parserName = tokens[0];
							eventName = tokens[1];

							if("nagios".equalsIgnoreCase(parserName)) {
								this.logger.fine("Found nagios rule");
								eventInfo = (new NagiosEventParser()).parse(reader);

								handlerList.add(new NagiosTaskHandler(eventName,
										this.agentInterface.getApplicationName(), this.agentInterface.getRootInstance().getName(),
									eventInfo));
								
							} else if("file".equalsIgnoreCase(parserName)) {
								this.logger.fine("Found file rule");
								eventInfo = (new FileEventParser()).parse(reader);
								handlerList.add(new FileTaskHandler(eventName,
										this.agentInterface.getApplicationName(), this.agentInterface.getRootInstance().getName(),
										eventInfo));
							}
						}
					}
				} catch (FileNotFoundException e) {
					// ignore
					this.logger.finest("FileNotFoundException: " + e.getMessage());
				} catch (IOException e) {
					this.logger.finest("IOException: " + e.getMessage());
					try { if(reader != null) reader.close(); } catch(Exception ioe) { /* ignore */ }
				} finally {
					try { if(reader != null) reader.close(); } catch(Exception e) { /* ignore */ }
				}
			}
		}
		
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

				this.agentInterface.getMessagingClient().sendMessageToTheDm(msg);

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				//try { if (channel != null) channel.close(); } catch(Exception e) { /*ignore*/ }
			}
		}
	}
	
	/*
	public static void main(String args[]) throws Exception {
		File conf = new File(Thread.currentThread().getContextClassLoader()
				.getResource("nagiosevents.conf").getFile());
		MonitoringScheduler sched = new MonitoringScheduler("appName", "VM1", conf);
		sched.startProcessing("localhost", "roboconf", "roboconf");
	}*/
}
