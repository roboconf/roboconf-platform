package net.roboconf.agent.monitoring.internal;

import org.junit.After;
import org.junit.Before;

import junit.framework.Assert;
import net.roboconf.agent.AgentMessagingInterface;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.messaging.MessagingConstants;
import net.roboconf.messaging.client.IAgentClient;
import net.roboconf.messaging.internal.client.MessageServerClientFactory;
import net.roboconf.messaging.internal.client.test.TestClientAgent;

public class Test {
	
	TestClientAgent client;
	AgentMessagingMock mock;
	AgentMonitoringScheduler scheduler;

	@Before
	public void setupClient() {
		
		MessageServerClientFactory factory = new MessageServerClientFactory();
		client = (TestClientAgent) factory.createAgentClient(MessagingConstants.FACTORY_TEST);
		mock = new AgentMessagingMock(client);

		scheduler = new AgentMonitoringScheduler();
		scheduler.setAgentInterface(mock); // Inject by hand (no iPojo in unit tests...)
		scheduler.start();
	}
	
	@After
	public void shutdownClient() {
		scheduler.stop();
	}

	public void testMachin() {
		
		Assert.assertEquals(0, client.messagesForTheDm.size());
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		Assert.assertEquals(1, client.messagesForTheDm.size());
	}
	
	
	public static class AgentMessagingMock implements AgentMessagingInterface {
		IAgentClient client;
		
		public AgentMessagingMock(IAgentClient client) {
			this.client = client;
		}

		@Override
		public IAgentClient getMessagingClient() {
			return client;
		}

		@Override
		public String getApplicationName() {
			return "testApp";
		}

		@Override
		public Instance getRootInstance() {
			return new Instance("VM1");
		}
	}
}
