/**
 * Copyright 2015-2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.core.commands;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import net.roboconf.core.model.beans.AbstractApplication;
import net.roboconf.core.model.beans.Component;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.helpers.ComponentHelpers;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.utils.Utils;

/**
 * A context used to validate and update command instructions.
 * <p>
 * Every time a command instruction is created, it is passed a context.
 * This context allows to resolve instances (or simulate new instances).
 * It also contains variables to be injected in instructions.
 * </p>
 *
 * @author Vincent Zurczak - Linagora
 */
public class Context {

	/**
	 * Map associating (real and virtual) instances with their component name.
	 */
	public final Map<String,String> instancePathToComponentName = new HashMap<> ();

	/**
	 * Map associating variable names and values.
	 */
	public final Map<String,String> variables = new HashMap<> ();

	/**
	 * Variables that could not be resolved.
	 * <p>
	 * Examples: query variables (e.g. ${EXISTING_INDEX ...}) may fail as there may not
	 * exist any index that satisfies the requirements. Such variables are meant to find
	 * existing instances.
	 * </p>
	 */
	public final Set<String> disabledVariables = new HashSet<> ();

	private final Logger logger = Logger.getLogger( getClass().getName());
	private final AbstractApplication app;
	private final File commandFile;


	/**
	 * Constructor.
	 * @param app
	 * @param commandFile
	 */
	public Context( AbstractApplication app, File commandFile ) {
		this.app = app;
		this.commandFile = commandFile;

		for( Instance instance : InstanceHelpers.getAllInstances( app ))
			this.instancePathToComponentName.put( InstanceHelpers.computeInstancePath( instance ), instance.getComponent().getName());
	}


	/**
	 * @return the commandFile
	 */
	protected File getCommandFile() {
		return this.commandFile;
	}


	/**
	 * @return the name of the commands file
	 */
	protected String getName() {
		return this.commandFile != null ? this.commandFile.getName() : null;
	}


	/**
	 * @return the app
	 */
	protected AbstractApplication getApp() {
		return this.app;
	}


	/**
	 * Determines whether an instance exists in the current context.
	 * @param instancePath a non-null instance path
	 * @return true if it exists, false otherwise
	 */
	public boolean instanceExists( String instancePath ) {

		Instance instance = resolveInstance( instancePath );
		return instance != null;
	}


	/**
	 * Resolves an instance, be it into the application or defined by a previous command instruction.
	 * <p>
	 * This method should be used as it simulates some modifications made to the application by command
	 * instructions. When validating instructions, it will always be more reliable than directly picking
	 * up instances in the application.
	 * </p>
	 *
	 * @param instancePath a non-null instance path
	 * @return an instance, or null if it was never created
	 */
	public Instance resolveInstance( String instancePath ) {

		Instance instance = null;
		String componentName = this.instancePathToComponentName.get( instancePath );
		if( componentName != null ) {

			String instanceName = InstanceHelpers.findInstanceName( instancePath );
			Component component;
			if( DefineVariableCommandInstruction.FAKE_COMPONENT_NAME.equals( componentName ))
				component = new Component( DefineVariableCommandInstruction.FAKE_COMPONENT_NAME );
			else
				component = ComponentHelpers.findComponent( this.app, componentName );

			if( ! Utils.isEmptyOrWhitespaces( instanceName ) && component != null )
				instance = new Instance( instanceName ).component( component );
			else
				this.logger.warning( "Instance's component of " + instancePath + " could not be resolved." );
		}

		return instance;
	}
}
