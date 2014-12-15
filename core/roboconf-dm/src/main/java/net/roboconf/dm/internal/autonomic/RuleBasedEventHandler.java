package net.roboconf.dm.internal.autonomic;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import net.roboconf.core.model.helpers.ComponentHelpers;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.runtime.Component;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.management.exceptions.ImpossibleInsertionException;
import net.roboconf.messaging.messages.from_agent_to_dm.MsgNotifAutonomic;
import net.roboconf.target.api.TargetException;

public class RuleBasedEventHandler {

	private Manager manager;
	private ManagedApplication ma;
	Map<String, AutonomicRule> rules;
	Logger logger = Logger.getLogger(getClass().getName());
	
	private Map<String, List<Instance>> vmsForTemplate = new HashMap<String, List<Instance>>();
	
	public RuleBasedEventHandler(Manager manager, ManagedApplication ma, MsgNotifAutonomic event) throws IOException {
		this.rules = RulesParser.parseRules(ma.getApplicationFilesDirectory());
		this.manager = manager;
		this.ma = ma;
	}

	/**
	 * React upon autonomic monitoring message (aka "autonomic event").
	 * @param event The autonomic event message
	 * @throws Exception
	 */
	public void handleEvent(MsgNotifAutonomic event) throws Exception {
		if(rules != null) {
			AutonomicRule rule = rules.get(event.getEventName());

			if("ReplicateService".equalsIgnoreCase(rule.getReactionId())) { // EVENT_ID ReplicateService ComponentTemplate
				createInstances(rule.getReactionInfo());
			} else if("StopService".equalsIgnoreCase(rule.getReactionId())) { // EVENT_ID StopService ComponentTemplate
				undeployInstances(rule.getReactionInfo());
			} else if("Mail".equalsIgnoreCase(rule.getReactionId())) { // EVENT_ID Mail DestinationEmail
				//TODO
			} else if("Log".equalsIgnoreCase(rule.getReactionId())) { // EVENT_ID Log LogMessage
				logger.info("AUTONOMIC Monitoring event of type \"Log\": " + rule.getReactionInfo());
			}
		}
	}

	/**
	 * Instantiate a new VM with instances on it.
	 * @param componentTemplates The component names, separated by "/" (eg. VM_name/Software_container_name/App_artifact_name)
	 * @throws IOException
	 * @throws ImpossibleInsertionException
	 * @throws TargetException
	 */
	public void createInstances(String componentTemplates) throws IOException, ImpossibleInsertionException, TargetException {

		String templates[] = componentTemplates.split("/");
		
		// First check that all component to instantiate are valid and found...
		// Necessary, not to create a VM then try to instantiate a fake component there !
		for( String s : templates) {
			Component compToInstantiate = ComponentHelpers.findComponent(ma.getApplication().getGraphs(), s);
			if( compToInstantiate == null )
				throw new IOException("Component " + s + " not found in application " + ma.getApplication().getName()
						+ ": can\'t instanciate new VM !");
		}

		// We register new instances in the model
		Instance previousInstance = null;
		for( String s : templates) {
			Component compToInstantiate = ComponentHelpers.findComponent(ma.getApplication().getGraphs(), s);
			// compToInstantiate should never be null (check done above).
			
			String instanceName;
			if( previousInstance == null ) {
				// All the root instances must have a different name
				//String rootName = 
				//ma.getApplication().getRootInstances()
				// TODO generate unique name ??
				instanceName = compToInstantiate.getName() + "_" + System.currentTimeMillis();
			} else {
				instanceName = compToInstantiate.getName() + " instance";
			}
			
			Instance currentInstance = new Instance( instanceName ).component(compToInstantiate);
			manager.addInstance(ma, previousInstance, currentInstance);
			previousInstance = currentInstance;
		}
		
		// Now, deploy and start all
		Instance rootInstance = InstanceHelpers.findRootInstance(previousInstance);
		
		manager.deployAndStartAll(ma, rootInstance);

		List<Instance> vmList = vmsForTemplate.get(componentTemplates);
		if(vmList == null) vmList = new LinkedList<Instance>();
		vmList.add(rootInstance);
		vmsForTemplate.put(componentTemplates, vmList);
	}
	
	/**
	 * Undeploy VM (eventually with instances on it).
	 * @param componentTemplates The component template used to create the undeployed VM (if multiple VMs were created using the same
	 * template, a FIFO strategy is used).
	 * @return
	 * @throws TargetException 
	 * @throws IOException 
	 */
	public boolean undeployInstances(String componentTemplates) throws IOException, TargetException {
		
		List<Instance> vmList = vmsForTemplate.get(componentTemplates);
		if(vmList == null) return false;
		
		if(vmList.size() <= 1) vmsForTemplate.remove(componentTemplates);
		Instance vmInstance = (vmList.size() > 0 ? vmList.remove(0) : null); // FIFO
		
		if(vmInstance != null) manager.undeployAll(ma, vmInstance);
		return true;
	}
}
