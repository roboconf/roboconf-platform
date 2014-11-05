/**
 * Copyright 2013-2014 Linagora, Université Joseph Fourier, Floralis
 *
 * The present code is developed in the scope of their joint LINAGORA -
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

package net.roboconf.plugin.logger.internal;

import java.util.logging.Logger;

import net.roboconf.core.model.runtime.Import;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.core.model.runtime.Instance.InstanceStatus;
import net.roboconf.plugin.api.PluginException;
import net.roboconf.plugin.api.PluginInterface;

/**
 * @author Vincent Zurczak - Linagora
 */
public class PluginLogger implements PluginInterface {

	private final Logger logger = Logger.getLogger( getClass().getName());
	private String agentId;



	@Override
	public String getPluginName() {
		return "logger";
	}


	@Override
	public void setNames( String applicationName, String rootInstanceName ) {
		this.agentId = "'" + rootInstanceName + "' agent";
	}


	@Override
	public void initialize( Instance instance ) throws PluginException {
		String name = instance != null ? instance.getName() : null;
		this.logger.fine( this.agentId + " is initializing the plug-in for " + name + "." );
	}


	@Override
	public void deploy( Instance instance ) throws PluginException {
		String name = instance != null ? instance.getName() : null;
		this.logger.info( this.agentId + " is deploying instance " + name + "." );
	}


	@Override
	public void start( Instance instance ) throws PluginException {
		String name = instance != null ? instance.getName() : null;
		this.logger.info( this.agentId + " is starting instance " + name + "." );
	}


	@Override
	public void update( Instance instance, Import importChanged, InstanceStatus statusChanged ) throws PluginException {
		String name = instance != null ? instance.getName() : null;
		this.logger.info( this.agentId + " is updating instance " + name + "." );
	}


	@Override
	public void stop( Instance instance ) throws PluginException {
		String name = instance != null ? instance.getName() : null;
		this.logger.info( this.agentId + " is stopping instance " + name + "." );
	}


	@Override
	public void undeploy( Instance instance ) throws PluginException {
		String name = instance != null ? instance.getName() : null;
		this.logger.info( this.agentId + " is undeploying instance " + name + "." );
	}
}
