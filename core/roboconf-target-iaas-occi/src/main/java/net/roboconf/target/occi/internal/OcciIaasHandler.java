/**
 * Copyright 2014-2017 Linagora, Université Joseph Fourier, Floralis
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
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Logger;

import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.userdata.UserDataHelpers;
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
	static final String USER = "occi.user";
	static final String PASSWORD = "occi.password";
	static final String RENDERING = "occi.rendering"; // http or json, default http
	static final String BACKEND = "occi.backend"; // Backend IaaS (eg. "vmware" or "openstack")

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

		// Deal with the creation
		try {
			UUID id = UUID.randomUUID();
			Map<String, String> targetProperties = parameters.getTargetProperties();

			String userData = UserDataHelpers.writeUserDataAsString(
					parameters.getMessagingProperties(),
					parameters.getDomain(),
					parameters.getApplicationName(),
					parameters.getScopedInstancePath());

			if("json".equalsIgnoreCase(targetProperties.get(RENDERING))) {
				return OcciVMUtils.createVMJson(targetProperties.get(SERVER_IP_PORT),
					id.toString(),
					targetProperties.get(IMAGE),
					targetProperties.get(TITLE),
					targetProperties.get(SUMMARY),
					userData,
					targetProperties.get(USER),
					targetProperties.get(PASSWORD),
					targetProperties,
					false);
			} else {
				return OcciVMUtils.createVM(targetProperties.get(SERVER_IP_PORT),
						id.toString(),
						targetProperties.get(IMAGE),
						targetProperties.get(TITLE),
						targetProperties.get(SUMMARY),
						userData,
						targetProperties.get(USER),
						targetProperties.get(PASSWORD),
						targetProperties);
			}
		} catch( Exception e ) {
			throw new TargetException(e);
		}
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.target.api.AbstractThreadedTargetHandler#machineConfigurator(
	 * net.roboconf.target.api.TargetHandlerParameters, java.lang.String)
	 */
	@Override
	public MachineConfigurator machineConfigurator( TargetHandlerParameters parameters, String machineId )
	throws TargetException {

		try {
			Properties userData = UserDataHelpers.writeUserDataAsProperties(
						parameters.getMessagingProperties(),
						parameters.getDomain(),
						parameters.getApplicationName(),
						parameters.getScopedInstancePath());

			String rootInstanceName = InstanceHelpers.findRootInstancePath( parameters.getScopedInstancePath());
			return new OcciMachineConfigurator(
					machineId,
					parameters.getTargetProperties(),
					userData,
					rootInstanceName,
					parameters.getScopedInstance());

		} catch( IOException e ) {
			throw new TargetException( e );
		}
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.target.api.TargetHandler
	 * #isMachineRunning(net.roboconf.target.api.TargetHandlerParameters, java.lang.String)
	 */
	@Override
	public boolean isMachineRunning( TargetHandlerParameters parameters, String machineId )
	throws TargetException {
		// TODO remove next line when APIs get compatible...
		String postfix = (parameters.getTargetProperties().get(CloudautomationMixins.PROVIDER_ENDPOINT) != null ? "" : "/compute");
		return OcciVMUtils.isVMRunning(parameters.getTargetProperties().get(SERVER_IP_PORT) + postfix, machineId);
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.target.api.TargetHandler
	 * #terminateMachine(net.roboconf.target.api.TargetHandlerParameters, java.lang.String)
	 */
	@Override
	public void terminateMachine( TargetHandlerParameters parameters, String machineId ) throws TargetException {
		try {
			// TODO remove next line when APIs get compatible...
			String postfix = (parameters.getTargetProperties().get(CloudautomationMixins.PROVIDER_ENDPOINT) != null ? "" : "/compute");
			OcciVMUtils.deleteVM( parameters.getTargetProperties().get(SERVER_IP_PORT) + postfix, machineId);
		} catch(TargetException ignore) {
			//ignore
		}
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.target.api.TargetHandler
	 * #retrievePublicIpAddress(net.roboconf.target.api.TargetHandlerParameters, java.lang.String)
	 */
	@Override
	public String retrievePublicIpAddress( TargetHandlerParameters parameters, String machineId )
	throws TargetException {
		// TODO: implement this method as it will provide an alternative to user data
		return null;
	}
}
