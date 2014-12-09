package net.roboconf.dm.internal.autonomic;

public class AutonomicRule {
	String reactionId;
	String reactionInfo;
	
	public AutonomicRule(String reactionId, String componentTemplate) {
		super();
		this.reactionId = reactionId;
		this.reactionInfo = componentTemplate;
	}

	public String getReactionId() {
		return reactionId;
	}
	public void setReactionId(String reactionId) {
		this.reactionId = reactionId;
	}
	public String getReactionInfo() {
		return reactionInfo;
	}
	public void setReactionInfo(String reactionInfo) {
		this.reactionInfo = reactionInfo;
	}
}
