package net.roboconf.dm.internal.autonomic;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Logger;

import net.roboconf.core.model.helpers.ComponentHelpers;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.runtime.Application;
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
	private Component compToInstanciate;
	Map<String, AutonomicRule> rules;
	Logger logger = Logger.getLogger(getClass().getName());
	
	public RuleBasedEventHandler(Manager manager, ManagedApplication ma, MsgNotifAutonomic event) throws IOException {
		/*
		Manager manager = new Manager();
		// init manager...
		manager.setMessageServerIp(messageServerIp);
		manager.setMessageServerUsername("roboconf");
		manager.setMessageServerPassword("roboconf");
		manager.start();
		*/
		
		this.rules = RulesParser.parseRules(ma.getApplicationFilesDirectory());
		
		this.manager = manager;
		this.ma = ma;
		this.compToInstanciate = compToInstanciate;
	}

	public void handleEvent(MsgNotifAutonomic event) throws Exception {
		if(rules != null) {
			AutonomicRule rule = rules.get(event.getEventName());
			
			if("ReplicateService".equalsIgnoreCase(rule.getReactionId())) {
				createInstances(rule.getReactionInfo());
			} else if("StopService".equalsIgnoreCase(rule.getReactionId())) {
				//TODO
			} else if("Mail".equalsIgnoreCase(rule.getReactionId())) {
				//TODO
			} else if("Log".equalsIgnoreCase(rule.getReactionId())) {
				logger.info("AUTONOMIC Monitoring event of type \"Log\": " + rule.getReactionInfo());
			}
		}
	}

	public void createInstances( String componentTemplates ) throws IOException, ImpossibleInsertionException, TargetException {

		// We register new instances in the model
		Instance previousInstance = null;
		for( String s : componentTemplates.split("/")) {
			Component compToInstantiate = ComponentHelpers.findComponent(ma.getApplication().getGraphs(), s);
			if( compToInstantiate == null )
				throw new IOException("TODO:");
			
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
	}
}
