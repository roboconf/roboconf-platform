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

package net.roboconf.agent.internal;

import java.io.File;
import java.util.logging.Logger;

import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.plugin.api.ExecutionLevel;
import net.roboconf.plugin.api.PluginInterface;
import net.roboconf.plugin.bash.PluginBash;
import net.roboconf.plugin.logger.PluginLogger;
import net.roboconf.plugin.puppet.PluginPuppet;

/**
 * @author Vincent Zurczak - Linagora
 */
public class PluginManager {

	private ExecutionLevel executionLevel = ExecutionLevel.RUNNING;
	private File dumpDirectory;


	/**
	 * Finds the right plug-in.
	 * @param instance an instance (not null)
	 * @param logger a logger (not null)
	 * @return the right plug-in, or null if none was found
	 */
	public PluginInterface findPlugin( Instance instance, Logger logger ) {

		PluginInterface result = null;
		String installerName = instance.getComponent().getInstallerName();

		if( "bash".equalsIgnoreCase( installerName ))
			result = new PluginBash();

		else if( "puppet".equalsIgnoreCase( installerName ))
			result = new PluginPuppet();

		else if( "logger".equalsIgnoreCase( installerName ))
			result = new PluginLogger();

		else
			logger.severe( "No plugin was found for instance " + instance.getName() + " with installer " + installerName + "." );

		if( result != null ) {
			result.setExecutionLevel( this.executionLevel );
			result.setDumpDirectory( this.dumpDirectory );
			result.setAgentName( "Agent " + InstanceHelpers.findRootInstance( instance ).getName());
		}

		return result;
	}


	/**
	 * @param executionLevel the executionLevel to set
	 */
	public void setExecutionLevel( ExecutionLevel executionLevel ) {
		this.executionLevel = executionLevel;
	}


	/**
	 * @param dumpDirectory the dumpDirectory to set.
	 * p>
	 * Only required if execution level is
	 * {@link ExecutionLevel#GENERATE_FILES}.
	 * </p>
	 */
	public void setDumpDirectory( File dumpDirectory ) {
		this.dumpDirectory = dumpDirectory;
	}
}
