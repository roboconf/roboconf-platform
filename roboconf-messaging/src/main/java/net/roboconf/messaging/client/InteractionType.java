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

package net.roboconf.messaging.client;

/**
 * @author Vincent Zurczak - Linagora
 */
public enum InteractionType {

	AGENT_TO_AGENT, AGENT_TO_DM, DM_TO_AGENT;


	public String getExchangeName() {

		String result = null;
		switch( this ) {
		case AGENT_TO_AGENT:
			result = "";
			break;

		case AGENT_TO_DM:
		case DM_TO_AGENT:
			result = "";
			break;

		default:
			break;
		}

		return result;
	}
}
