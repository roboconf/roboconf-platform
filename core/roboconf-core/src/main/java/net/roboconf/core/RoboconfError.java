/**
 * Copyright 2014-2016 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.core;

/**
 * An error instantiates an {@link ErrorCode}.
 * @author Vincent Zurczak - Linagora
 */
public class RoboconfError {

	private ErrorCode errorCode;
	private String details;


	/**
	 * Constructor.
	 * @param errorCode an error code
	 */
	public RoboconfError( ErrorCode errorCode ) {
		this.errorCode = errorCode;
	}


	/**
	 * Constructor.
	 * @param errorCode an error code
	 */
	public RoboconfError( ErrorCode errorCode, String details ) {
		this.errorCode = errorCode;
		this.details = details;
	}

	/**
	 * @return the errorCode
	 */
	public ErrorCode getErrorCode() {
		return this.errorCode;
	}

	/**
	 * @param errorCode the errorCode to set
	 */
	public void setErrorCode( ErrorCode errorCode ) {
		this.errorCode = errorCode;
	}

	/**
	 * @return the details
	 */
	public String getDetails() {
		return this.details;
	}

	/**
	 * @param details the details to set
	 */
	public void setDetails( String details ) {
		this.details = details;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return this.errorCode.getMsg();
	}
}
