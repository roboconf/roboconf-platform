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

package net.roboconf.dm.internal.test;

import java.util.Map;

import net.roboconf.core.utils.Utils;
import net.roboconf.target.api.TargetException;
import net.roboconf.target.api.TargetHandler;

/**
 * @author Vincent Zurczak - Linagora
 */
public class TargetHandlerMock implements TargetHandler {

	private final String installerName;


	/**
	 * Constructor.
	 * @param installerName
	 */
	public TargetHandlerMock( String installerName ) {
		this.installerName = installerName;
	}

	@Override
	public void terminateMachine( String machineId ) throws TargetException {
		// nothing
	}

	@Override
	public void setTargetProperties( Map<String,String> targetProperties ) throws TargetException {
		// nothing
	}

	@Override
	public String getTargetId() {
		return this.installerName;
	}

	@Override
	public String createOrConfigureMachine( String messagingIp, String messagingUsername, String messagingPassword, String rootInstanceName, String applicationName )
	throws TargetException {
		return "whatever";
	}

	@Override
	public boolean equals( Object obj ) {
		return obj instanceof TargetHandlerMock
				&& Utils.areEqual( this.installerName, ((TargetHandlerMock) obj ).installerName );
	}

	@Override
	public int hashCode() {
		return this.installerName == null ? 11 : this.installerName.hashCode();
	}
}
