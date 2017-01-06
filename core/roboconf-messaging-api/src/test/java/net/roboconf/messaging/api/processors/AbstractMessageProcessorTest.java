/**
 * Copyright 2014-2017 Linagora, Université Joseph Fourier, Floralis
 *
 * The present code is developed in the scope of the joint LINAGORA -
 * Université Joseph Fourier - Floralis research program and is designated
 * as a "Result" pursuant to the terms and conditions of the LINAGORA
 * - Université Joseph Fourier - Floralis research program. Each copyright
 * holder of Results enumerated here above fully & independently holds complete
 * ownership of the complete Intellectual Property rights applicable to the whole
 * of said Results, and may freely exploit it in any manner which does not infringe
 * the moral rights of the other copyright holders.
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

package net.roboconf.messaging.api.processors;

import org.junit.Assert;
import net.roboconf.messaging.api.AbstractMessageProcessor;
import net.roboconf.messaging.api.MessagingConstants;
import net.roboconf.messaging.api.business.IDmClient;
import net.roboconf.messaging.api.messages.Message;
import net.roboconf.messaging.api.messages.from_dm_to_agent.MsgCmdResynchronize;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class AbstractMessageProcessorTest {

	private AbstractMessageProcessor<IDmClient> processor;


	@Before
	public void initializeProcessor() {
		this.processor = new EmptyTestDmMessageProcessor();
	}


	@After
	public void terminateProcessor() {
		if( this.processor != null )
			this.processor.stopProcessor();
	}


	@Test
	public void testInterrupt() throws Exception {

		Assert.assertFalse( this.processor.isRunning());
		this.processor.start();

		Thread.sleep( 200 );
		Assert.assertTrue( this.processor.isRunning());

		this.processor.interrupt();
		Thread.sleep( 200 );
		Assert.assertFalse( this.processor.isRunning());
	}


	@Test
	public void testStartAndStop() throws Exception {

		Assert.assertFalse( this.processor.isRunning());
		this.processor.start();

		Thread.sleep( 200 );
		Assert.assertTrue( this.processor.isRunning());

		this.processor.stopProcessor();;
		Thread.sleep( 200 );
		Assert.assertFalse( this.processor.isRunning());
	}


	@Test
	public void testProcessing() throws Exception {

		// The message is processed...
		this.processor.start();
		this.processor.storeMessage( new MsgCmdResynchronize());
		Thread.sleep( 1000 );
		this.processor.stopProcessor();

		Assert.assertEquals( 0, this.processor.getMessageQueue().size());
	}


	/**
	 * @author Vincent Zurczak - Linagora
	 */
	public static class EmptyTestDmMessageProcessor extends AbstractMessageProcessor<IDmClient> {

		/**
		 * Constructor.
		 */
		public EmptyTestDmMessageProcessor() {
			super( MessagingConstants.FACTORY_TEST);
		}

		@Override
		protected void processMessage( Message message ) {
			// nothing
		}
	}
}
