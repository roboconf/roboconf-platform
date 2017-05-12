/**
 * Copyright 2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.messaging.api.jmx;

/**
 * The interface to monitor the activity related to the messaging.
 * @author Vincent Zurczak - Linagora
 */
public interface MessagingApiMBean {

	// Sent messages

	/**
	 * @return the number of messages that were successfully sent
	 */
	long getSentMessagesCount();

	/**
	 * @return the number of messages that failed to be sent
	 */
	long getFailedSendingCount();

	/**
	 * @return the time stamp indicating the last time a message failed to be sent
	 */
	long getTimestampOfLastSendingFailure();

	/**
	 * @return the time stamp indicating the last time a message was successfully sent
	 */
	long getTimestampOfLastSentMessage();


	// Received messages

	/**
	 * @return the number of messages that were received
	 */
	long getReceivedMessagesCount();

	/**
	 * @return the number of messages that failed to be received (e.g. deserialization error)
	 */
	long getFailedReceptionCount();

	/**
	 * @return the time stamp indicating the last time a message failed to be received
	 */
	long getTimestampOfLastReceptionFailure();

	/**
	 * @return the time stamp indicating the last time a message was successfully received
	 */
	long getTimestampOfLastReceivedMessage();


	// Current state

	/**
	 * @return true if the current client is connected
	 */
	boolean isConnected();

	/**
	 * @return a human-readable identifier for the client's owner
	 */
	String getId();

	/**
	 * @return the messaging type for this client
	 */
	String getMessagingType();


	// Reset operation

	/**
	 * Method to reset all the counters.
	 */
	void reset();
}
