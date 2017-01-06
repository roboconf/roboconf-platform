/**
 * Copyright 2013-2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.agent.internal.misc;

import java.util.logging.Logger;

import net.roboconf.core.model.beans.Import;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.plugin.api.PluginException;
import net.roboconf.plugin.api.PluginInterface;

/**
 * @author Vincent Zurczak - Linagora
 */
public class PluginMock implements PluginInterface {

	public static final String INIT_PROPERTY = "initialized";
	public static final String PLUGIN_NAME = "mock";

	private final Logger logger = Logger.getLogger( getClass().getName());
	private String agentId;
	private InstanceStatus currentState = InstanceStatus.NOT_DEPLOYED;



	@Override
	public String getPluginName() {
		return PLUGIN_NAME;
	}


	@Override
	public void setNames( String applicationName, String rootInstanceName ) {
		this.agentId = "'" + rootInstanceName + "' agent";
	}


	@Override
	public void initialize( Instance instance ) throws PluginException {
		String name = instance != null ? instance.getName() : null;
		this.logger.fine( this.agentId + " is initializing the plug-in for " + name + "." );

		// This class is mainly used for test and dev' purpose.
		// So, we can store the information in the instance without any major impact.
		if( instance != null )
			instance.data.put( INIT_PROPERTY, "true" );
	}


	@Override
	public void deploy( Instance instance ) throws PluginException {
		String name = instance != null ? instance.getName() : null;
		this.logger.info( this.agentId + " is deploying instance " + name + "." );
		this.currentState = InstanceStatus.DEPLOYED_STOPPED;
	}


	@Override
	public void start( Instance instance ) throws PluginException {
		String name = instance != null ? instance.getName() : null;
		this.logger.info( this.agentId + " is starting instance " + name + "." );
		this.currentState = InstanceStatus.DEPLOYED_STARTED;
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
		this.currentState = InstanceStatus.DEPLOYED_STOPPED;
	}


	@Override
	public void undeploy( Instance instance ) throws PluginException {
		String name = instance != null ? instance.getName() : null;
		this.logger.info( this.agentId + " is undeploying instance " + name + "." );
		this.currentState = InstanceStatus.NOT_DEPLOYED;
	}


	public InstanceStatus getCurrentState() {
		return this.currentState;
	}
}
