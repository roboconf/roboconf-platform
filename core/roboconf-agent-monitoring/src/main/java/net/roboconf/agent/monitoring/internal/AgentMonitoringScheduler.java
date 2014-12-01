/**
 * 
 */
package net.roboconf.agent.monitoring.internal;

import java.io.File;
import java.util.logging.Logger;

import net.roboconf.agent.AgentMessagingInterface;
import net.roboconf.core.utils.Utils;
import net.roboconf.messaging.reconfigurables.ReconfigurableClientAgent;

/**
 * @author gibello
 *
 */
public class AgentMonitoringScheduler {

	// Injected by iPojo
	private AgentMessagingInterface agentInterface;
	
	Logger logger = Logger.getLogger(getClass().getName());
	MonitoringScheduler sched;
	
	// TODO: pass the messaging client of the agent to the thread...
	public void start() {
		File conf = new File(Thread.currentThread().getContextClassLoader()
				.getResource("nagiosevents.conf").getFile());
		try {
			sched = new MonitoringScheduler("appName", "VM1", conf);
			sched.startProcessing((ReconfigurableClientAgent)agentInterface.getMessagingClient());
		} catch(Exception e) {
			logger.warning(e.getMessage());
			Utils.logException(logger, e);
		}
	}

	public void stop() {
		if(sched != null) sched.stopProcessing();
	}
	
	/**
	 * Force injection of agentInterface field (for tests: normally injected by iPojo).
	 * @param agentInterface
	 */
	public void setAgentInterface(AgentMessagingInterface agentInterface) {
		this.agentInterface = agentInterface;
	}
}
