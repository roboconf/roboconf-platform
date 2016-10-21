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

package net.roboconf.target.occi.internal;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import net.roboconf.core.agents.DataHelpers;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.utils.Utils;
import net.roboconf.target.api.AbstractThreadedTargetHandler;
import net.roboconf.target.api.TargetException;
import net.roboconf.target.api.TargetHandlerParameters;

/**
 * @author Pierre-Yves Gibello - Linagora
 */
public class OcciIaasHandler extends AbstractThreadedTargetHandler {

	public static final String TARGET_ID = "iaas-occi";

	static final String SERVER_IP_PORT = "occi.serverIpPort";
	static final String IMAGE = "occi.image";
	static final String TITLE = "occi.title";
	static final String SUMMARY = "occi.summary";

	private final Logger logger = Logger.getLogger(getClass().getName());


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
	 * @see net.roboconf.target.api.TargetHandler
	 * #createMachine(net.roboconf.target.api.TargetHandlerParameters)
	 */
	@Override
	public String createMachine( TargetHandlerParameters parameters ) throws TargetException {

		this.logger.fine( "Creating a new VM @ OCCI." );

		// For IaaS, we only expect root instance names to be passed
		if( InstanceHelpers.countInstances( parameters.getScopedInstancePath()) > 1 )
			throw new TargetException( "Only root instances can be passed in arguments." );

		String rootInstanceName = InstanceHelpers.findRootInstancePath(parameters.getScopedInstancePath());

		// Deal with the creation
		try {
			UUID id = UUID.randomUUID();
			Map<String, String> properties = parameters.getTargetProperties();
			return OcciVMUtils.createVM(properties.get(SERVER_IP_PORT),
					id.toString(),
					properties.get(IMAGE),
					properties.get(TITLE),
					properties.get(SUMMARY));
		} catch( Exception e ) {
			throw new TargetException( e );
		}
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.target.api.AbstractThreadedTargetHandler#machineConfigurator(
	 * net.roboconf.target.api.TargetHandlerParameters, java.lang.String, net.roboconf.core.model.beans.Instance)
	 */
	@Override
	public MachineConfigurator machineConfigurator(
			TargetHandlerParameters parameters,
			String machineId,
			Instance scopedInstance ) {

		String userData = "";
		try {
			userData = DataHelpers.writeUserDataAsString(
					parameters.getMessagingProperties(),
					parameters.getDomain(),
					parameters.getApplicationName(),
					parameters.getScopedInstancePath());

		} catch( IOException e ) {
			this.logger.severe( "User data could not be generated." );
			Utils.logException( this.logger, e );
		}

		String rootInstanceName = InstanceHelpers.findRootInstancePath( parameters.getScopedInstancePath());
		return new OcciMachineConfigurator( parameters.getTargetProperties(), userData, rootInstanceName, scopedInstance );
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.target.api.TargetHandler
	 * #isMachineRunning(java.util.Map, java.lang.String)
	 */
	@Override
	public boolean isMachineRunning( Map<String,String> targetProperties, String machineId )
	throws TargetException {

		boolean result = false;
		//TODO implement method
		return result;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.target.api.TargetHandler
	 * #terminateMachine(java.util.Map, java.lang.String)
	 */
	@Override
	public void terminateMachine( Map<String, String> targetProperties, String machineId ) throws TargetException {
		OcciVMUtils.deleteVM(targetProperties.get(SERVER_IP_PORT), machineId);
	}
}
