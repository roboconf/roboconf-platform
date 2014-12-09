package net.roboconf.agent.monitoring.internal;

import java.io.File;

import net.roboconf.messaging.messages.Message;
import net.roboconf.messaging.messages.from_agent_to_dm.MsgNotifAutonomic;

public class FileTaskHandler extends MonitoringTaskHandler {

	String queryType;
	String fileName;
	
	public FileTaskHandler(String eventName, String applicationName, String vmInstanceName, String query[]) {
		super(eventName, applicationName, vmInstanceName);
		
		if(query.length > 1) {
			String tokens[] = query[0].split("\\s+");
			
			if(tokens.length > 1) {
				queryType = tokens[0];
				fileName = tokens[1];
			}
		}
	}

	@Override
	public Message process() {
		
		if("IfExistsDelete".equalsIgnoreCase(this.queryType)) {
			File f = new File(this.fileName);
			if(f.exists() && f.isFile()) {
					if(f.delete()) {
						MsgNotifAutonomic message = new MsgNotifAutonomic(eventName,
								applicationName, vmInstanceName, "File " + this.fileName +" deleted");
					
						return message;
					}
			}
		}
		
		return null;
	}

}
