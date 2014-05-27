/**
 * Copyright 2013-2014 Linagora, Université Joseph Fourier
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

package net.roboconf.plugin.logger;

import java.io.File;
import java.util.logging.Logger;

import net.roboconf.core.model.runtime.Import;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.core.model.runtime.Instance.InstanceStatus;
import net.roboconf.plugin.api.ExecutionLevel;
import net.roboconf.plugin.api.PluginException;
import net.roboconf.plugin.api.PluginInterface;

/**
 * @author Vincent Zurczak - Linagora
 */
public class PluginLogger implements PluginInterface {

	private final Logger logger = Logger.getLogger( getClass().getName());
	private String agentName;



	@Override
	public String getPluginName() {
		return "logger";
	}


	@Override
	public void setExecutionLevel( ExecutionLevel executionLevel ) {
		this.logger.fine( "The execution level is: " + executionLevel );
	}


	@Override
	public void setDumpDirectory( File dumpDirectory ) {
		this.logger.fine( "The dump directory is: " + dumpDirectory );
	}


	@Override
	public void setAgentName( String agentName ) {
		this.agentName = agentName;
	}


	@Override
	public void initialize( Instance instance ) throws PluginException {
		this.logger.fine( this.agentName + " is initializing the plug-in for " + instance.getName() + "." );
	}


	@Override
	public void deploy( Instance instance ) throws PluginException {
		this.logger.info( this.agentName + " is deploying instance " + instance.getName() + "." );
	}


	@Override
	public void start( Instance instance ) throws PluginException {
		this.logger.info( this.agentName + " is starting instance " + instance.getName() + "." );
	}


	@Override
	public void update(Instance instance, Import importChanged, InstanceStatus statusChanged) throws PluginException {
		this.logger.info( this.agentName + " is updating instance " + instance.getName() + "." );
	}


	@Override
	public void stop( Instance instance ) throws PluginException {
		this.logger.info( this.agentName + " is stopping instance " + instance.getName() + "." );
	}


	@Override
	public void undeploy( Instance instance ) throws PluginException {
		this.logger.info( this.agentName + " is undeploying instance " + instance.getName() + "." );
	}
}
