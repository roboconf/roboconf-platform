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

package net.roboconf.dm.rest.client.exceptions;

/**
 * An exception that stores the response code and message.
 * @author Vincent Zurczak - Linagora
 */
public class RestException extends Exception {

	private static final long serialVersionUID = 9148345461406336650L;
	private final int responseStatus;
	private final String responseMessage;

	/**
	 * Constructor.
	 * @param responseStatus the response status
	 * @param responseMessage the message
	 */
	public RestException( int responseStatus, String responseMessage ) {
		super( responseStatus + " " + responseMessage );
		this.responseStatus = responseStatus;
		this.responseMessage = responseMessage;
	}

	/**
	 * @return the responseStatus
	 */
	public int getResponseStatus() {
		return this.responseStatus;
	}

	/**
	 * @return the responseMessage
	 */
	public String getResponseMessage() {
		return this.responseMessage;
	}
}
