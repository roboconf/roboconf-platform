/**
 * 
 */
package net.roboconf.agent.monitoring.internal;

import java.util.Timer;
import java.util.TimerTask;
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
	private Timer timer;

	// TODO: pass the messaging client of the agent to the thread...
	public void start() {
		logger.fine("AgentMonitoringScheduler start called");
		try {
			TimerTask sched = new MonitoringScheduler(agentInterface);
			if(this.timer != null) {
				this.timer.cancel();
			}
			this.timer = new Timer("Monitoring Timer @ Agent", true);
			this.timer.scheduleAtFixedRate(sched, 0, 10000);
			this.logger.fine("Autonomic monitoring scheduler started");
		} catch(Exception e) {
			logger.warning(e.getMessage());
			Utils.logException(logger, e);
		}
	}

	public void stop() {
		logger.fine("AgentMonitoringScheduler stop called");
		if(this.timer != null) {
			this.timer.cancel();
			this.timer = null;
		}
		this.logger.fine("Autonomic monitoring scheduler stopped");
	}
	
	/**
	 * Force injection of agentInterface field (for tests: normally injected by iPojo).
	 * @param agentInterface
	 */
	public void setAgentInterface(AgentMessagingInterface agentInterface) {
		this.agentInterface = agentInterface;
	}
}
