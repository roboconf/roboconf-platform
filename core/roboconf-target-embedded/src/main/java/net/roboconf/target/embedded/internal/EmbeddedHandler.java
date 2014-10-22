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

package net.roboconf.target.embedded.internal;

import java.util.Map;

import net.roboconf.target.api.TargetException;
import net.roboconf.target.api.TargetHandler;

/**
 * A target for embedded systems (e.g. the local host).
 * @author Pierre-Yves Gibello - Linagora
 */
public class EmbeddedHandler implements TargetHandler {

	public static final String TARGET_ID = "embedded";


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.target.api.TargetHandler#getTargetId()
	 */
	@Override
	public String getTargetId() {
		return TARGET_ID;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.target.api.TargetHandler#setTargetProperties(java.util.Map)
	 */
	@Override
	public void setTargetProperties( Map<String,String> targetProperties ) {
		// nothing
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.target.api.TargetHandler
	 * #createOrConfigureMachine(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public String createOrConfigureMachine(
			String messagingIp,
			String messagingUsername,
			String messagingPassword,
			String rootInstanceName,
			String applicationName )
	throws TargetException {

		return rootInstanceName + " (" + TARGET_ID + ")";
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.target.api.TargetHandler
	 * #terminateMachine(java.lang.String)
	 */
	@Override
	public void terminateMachine( String instanceId ) throws TargetException {
		// TBD shutdown script ?
	}
}
