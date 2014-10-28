/**
 * Copyright 2014 Linagora, Université Joseph Fourier
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

package net.roboconf.messaging;

/**
 * @author Vincent Zurczak - Linagora
 */
public interface MessagingConstants {

	/**
	 * The polling period used by a message processor when the message queue is empty.
	 */
	long MESSAGE_POLLING_PERIOD = 1000;

	/**
	 * The factory's name for RabbitMQ clients.
	 */
	String FACTORY_RABBIT_MQ = "factory.rabbit.mq";

	/**
	 * The factory's name for test clients.
	 */
	String FACTORY_TEST = "factory.test";
}