/**
 * Copyright 2016-2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.dm.internal.api.impl;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.userdata.UserDataHelpers;
import net.roboconf.core.utils.ProgramUtils;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.internal.api.ITargetConfigurator;
import net.roboconf.dm.management.api.ITargetHandlerResolver;
import net.roboconf.target.api.TargetHandler;
import net.roboconf.target.api.TargetHandlerParameters;

/**
 * @author Vincent Zurczak - Linagora
 */
public class TargetConfiguratorImpl implements ITargetConfigurator {

	ExecutorService executor;
	final Map<String,TargetConfiugrationBean> candidates = new ConcurrentHashMap<> ();

	private ITargetHandlerResolver targetHandlerResolver;


	@Override
	public void start() {
		// Everything runs within a single and separate thread.
		this.executor = Executors.newSingleThreadExecutor();
	}


	@Override
	public void stop() {

		if( this.executor != null ) {
			this.executor.shutdownNow();
			this.executor = null;
		}
	}


	/**
	 * @param targetHandlerResolver the targetHandlerResolver to set
	 */
	public void setTargetHandlerResolver( ITargetHandlerResolver targetHandlerResolver ) {
		this.targetHandlerResolver = targetHandlerResolver;
	}


	@Override
	public void reportCandidate( TargetHandlerParameters parameters, Instance scopedInstance ) {

		String key = parameters.getApplicationName() + "_" + parameters.getScopedInstancePath();
		this.candidates.put( key, new TargetConfiugrationBean( parameters, scopedInstance ));
	}


	@Override
	public void cancelCandidate( TargetHandlerParameters parameters, Instance scopedInstance ) {
		String key = parameters.getApplicationName() + "_" + parameters.getScopedInstancePath();
		this.candidates.remove( key );
	}


	@Override
	public void verifyCandidates() {

		// Prevent stupid NPEs
		if( this.executor == null )
			return;

		// Checks all the candidates
		for( Map.Entry<String,TargetConfiugrationBean> entry : this.candidates.entrySet()) {
			Instance scopedInstance = entry.getValue().scopedInstance;
			TargetHandlerParameters parameters = entry.getValue().parameters;
			File script = parameters.getTargetConfigurationScript();

			// Marked?
			if( scopedInstance.data.containsKey( Instance.READY_FOR_CFG_MARKER )) {

				// Remove the marker
				scopedInstance.data.remove( Instance.READY_FOR_CFG_MARKER );

				// If there is a script...
				// If there is a resolver for target handlers...
				if( script != null
						&& script.exists()
						&& this.targetHandlerResolver != null )
					this.executor.execute( new ConfigurationRunnable( parameters, scopedInstance, this.targetHandlerResolver ));

				// Remove it from the list of candidates
				this.candidates.remove( entry.getKey());
			}
		}
	}


	/**
	 * @author Vincent Zurczak - Linagora
	 */
	static class TargetConfiugrationBean {
		private final TargetHandlerParameters parameters;
		private final Instance scopedInstance;

		/**
		 * Constructor.
		 * @param parameters
		 * @param scopedInstance
		 */
		public TargetConfiugrationBean( TargetHandlerParameters parameters, Instance scopedInstance ) {
			this.parameters = parameters;
			this.scopedInstance = scopedInstance;
		}
	}


	/**
	 * Just a proxy class to ease testing of script execution.
	 * <p>
	 * This class can be mocked for tests.
	 * </p>
	 *
	 * FIXME: we may need to push such a class as a replacement of ProgramUtils.
	 * @author Vincent Zurczak - Linagora
	 */
	static class ProgramUtilsProxy {

		int executeCommand(
				final Logger logger,
				final String[] command,
				final File workingDir,
				final Map<String,String> environmentVars,
				final String applicationName,
				final String scopedInstancePath)
		throws IOException, InterruptedException {
			return ProgramUtils.executeCommand( logger, command, workingDir, environmentVars, applicationName, scopedInstancePath);
		}
	}


	/**
	 * @author Vincent Zurczak - Linagora
	 */
	static class ConfigurationRunnable implements Runnable {

		private final Logger logger = Logger.getLogger( getClass().getName());
		private final TargetHandlerParameters parameters;
		private final ITargetHandlerResolver targetHandlerResolver;
		private final Instance scopedInstance;
		ProgramUtilsProxy programUtils;


		/**
		 * Constructor.
		 * @param parameters
		 * @param scopedInstance
		 * @param targetHandlerResolver
		 */
		public ConfigurationRunnable(
				TargetHandlerParameters parameters,
				Instance scopedInstance,
				ITargetHandlerResolver targetHandlerResolver ) {

			this.parameters = parameters;
			this.scopedInstance = scopedInstance;
			this.targetHandlerResolver = targetHandlerResolver;
			this.programUtils = new ProgramUtilsProxy();
		}


		@Override
		public void run() {

			String suffix = this.parameters.getScopedInstancePath() + " in " + this.parameters.getApplicationName();
			try {
				// Retrieve the target handler
				TargetHandler th = this.targetHandlerResolver.findTargetHandler( this.parameters.getTargetProperties());

				// Retrieve the IP address
				String machineId = this.scopedInstance.data.get( Instance.MACHINE_ID );
				if( Utils.isEmptyOrWhitespaces( machineId ))
					this.logger.warning( "No machine ID was found for " + suffix );

				String publicIpAddress = th.retrievePublicIpAddress( this.parameters, machineId );
				if( Utils.isEmptyOrWhitespaces( publicIpAddress )) {
					publicIpAddress = "";
					this.logger.warning( "No public IP address could be retrieved for " + suffix );
				}

				// Prepare the execution of the script
				File script = this.parameters.getTargetConfigurationScript();
				if( script.exists()) {
					script.setExecutable( true );
					String[] command = { script.getAbsolutePath()};

					String userData = UserDataHelpers.writeUserDataAsString(
							this.parameters.getMessagingProperties(),
							this.parameters.getDomain(),
							this.parameters.getApplicationName(),
							this.parameters.getScopedInstancePath());

					Map<String,String> vars = new HashMap<> ();
					vars.put( "IP_ADDRESS", publicIpAddress );
					vars.put( "APPLICATION_NAME", this.parameters.getApplicationName());
					vars.put( "SCOPED_INSTANCE_PATH", this.parameters.getScopedInstancePath());
					vars.put( "DOMAIN", this.parameters.getDomain());
					vars.put( "USER_DATA", userData );

					this.programUtils.executeCommand(
							this.logger,
							command,
							script.getParentFile(),
							vars,
							this.parameters.getApplicationName(),
							this.parameters.getScopedInstancePath());
				}

			} catch( Throwable t ) {
				// Wrap ALL the exceptions.
				// Notice that all the exceptions are not bugs! As an example, it is possible the
				// target handler was uninstalled. In this case, a TargetException is thrown and caught by this block.
				this.logger.severe( "Failed to run local configuration script for target " + suffix );
				Utils.logException( this.logger, t );
			}
		}
	}
}
