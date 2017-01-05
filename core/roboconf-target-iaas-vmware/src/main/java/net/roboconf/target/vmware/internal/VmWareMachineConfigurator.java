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

package net.roboconf.target.vmware.internal;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

import com.vmware.vim25.GuestProgramSpec;
import com.vmware.vim25.NamePasswordAuthentication;
import com.vmware.vim25.mo.GuestOperationsManager;
import com.vmware.vim25.mo.GuestProcessManager;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.VirtualMachine;

import net.roboconf.core.model.beans.Instance;
import net.roboconf.target.api.AbstractThreadedTargetHandler.MachineConfigurator;
import net.roboconf.target.api.TargetException;

/**
 * A machine configurator for VMWare.
 * @author Vincent Zurczak - Linagora
 */
public class VmWareMachineConfigurator implements MachineConfigurator {

	private final Logger logger = Logger.getLogger( getClass().getName());
	private final Map<String,String> targetProperties;
	private final String userData, rootInstanceName;
	private final Instance scopedInstance;
	private int vmwareToolsCheckCount = 0;

	private ServiceInstance vmwareServiceInstance;


	/**
	 * Constructor.
	 */
	public VmWareMachineConfigurator(
			Map<String,String> targetProperties,
			String userData,
			String rootInstanceName,
			Instance scopedInstance ) {

		this.targetProperties = targetProperties;
		this.userData = userData;
		this.rootInstanceName = rootInstanceName;
		this.scopedInstance = scopedInstance;
	}


	@Override
	public Instance getScopedInstance() {
		return this.scopedInstance;
	}


	@Override
	public void close() throws IOException {
		// nothing
	}


	@Override
	public boolean configure() throws TargetException {

		try {
			// Is the VM up?
			if( this.vmwareServiceInstance == null )
				this.vmwareServiceInstance = VmwareIaasHandler.getServiceInstance( this.targetProperties );

			VirtualMachine vm = VmwareIaasHandler.getVirtualMachine( this.vmwareServiceInstance, this.rootInstanceName );
			if( vm == null ) {
				this.logger.warning( "No VM named '" + this.rootInstanceName + "' could be found. The VM might not yet be ready." );
				return false;
			}

			// If yes, write user data.
			GuestOperationsManager gom = this.vmwareServiceInstance.getGuestOperationsManager();
			NamePasswordAuthentication npa = new NamePasswordAuthentication();
			npa.username = this.targetProperties.get( VmwareIaasHandler.VM_USER );
			npa.password = this.targetProperties.get( VmwareIaasHandler.VM_PASSWORD );
			GuestProgramSpec spec = new GuestProgramSpec();

			spec.programPath = "/bin/echo";
			spec.arguments = "$\'" + this.userData + "\' > /tmp/roboconf.properties";
			this.logger.fine(spec.programPath + " " + spec.arguments);

			GuestProcessManager gpm = gom.getProcessManager( vm );
			gpm.startProgramInGuest(npa, spec);

			return true;

		} catch( Exception e ) {
			if(++ this.vmwareToolsCheckCount < 20) {
				this.logger.fine("VMWare tools not yet started... check #" + this.vmwareToolsCheckCount + " failed");
				return false;
			}
			throw new TargetException( e );
		}
	}
}
