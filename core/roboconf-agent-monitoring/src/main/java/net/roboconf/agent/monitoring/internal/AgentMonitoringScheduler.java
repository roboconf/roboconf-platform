/**
 * 
 */
package net.roboconf.agent.monitoring.internal;

import java.util.logging.Logger;

import net.roboconf.agent.AgentMessagingInterface;
import net.roboconf.core.utils.Utils;

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
		logger.fine("AgentMonitoringScheduler start called");
		try {
			sched = new MonitoringScheduler(agentInterface);
			sched.startProcessing();
		} catch(Exception e) {
			logger.warning(e.getMessage());
			Utils.logException(logger, e);
		}
	}

	public void stop() {
		logger.fine("AgentMonitoringScheduler stop called");
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
