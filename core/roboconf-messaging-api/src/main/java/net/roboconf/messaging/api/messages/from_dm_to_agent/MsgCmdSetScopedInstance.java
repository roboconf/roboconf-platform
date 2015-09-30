/**
 * Copyright 2013-2015 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.messaging.api.messages.from_dm_to_agent;

import java.util.HashMap;
import java.util.Map;

import net.roboconf.core.model.beans.Instance;
import net.roboconf.messaging.api.messages.Message;

/**
 * @author Noël - LIG
 */
public class MsgCmdSetScopedInstance extends Message {

	private static final long serialVersionUID = 411037586577734609L;
	private final Instance scopedInstance;
	private final Map<String,String> externalExports;


	/**
	 * Constructor.
	 * @param scopedInstance
	 * @param externalExports
	 */
	public MsgCmdSetScopedInstance( Instance scopedInstance, Map<String,String> externalExports ) {
		this.scopedInstance = scopedInstance;
		this.externalExports = new HashMap<String,String> ();
		if( externalExports != null )
			this.externalExports.putAll( externalExports );
	}

	/**
	 * Constructor.
	 * @param scopedInstance
	 */
	public MsgCmdSetScopedInstance( Instance scopedInstance ) {
		this( scopedInstance, null );
	}

	/**
	 * @return the scoped instance
	 */
	public Instance getScopedInstance() {
		return this.scopedInstance;
	}

	/**
	 * @return the externalExports
	 */
	public Map<String,String> getExternalExports() {
		return this.externalExports;
	}
}
