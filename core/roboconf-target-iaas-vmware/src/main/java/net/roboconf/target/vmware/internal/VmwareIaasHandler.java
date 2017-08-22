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
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.logging.Logger;

import com.vmware.vim25.DynamicProperty;
import com.vmware.vim25.VirtualMachineCloneSpec;
import com.vmware.vim25.VirtualMachineConfigSpec;
import com.vmware.vim25.VirtualMachineRelocateSpec;
import com.vmware.vim25.mo.ComputeResource;
import com.vmware.vim25.mo.Datacenter;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.Task;
import com.vmware.vim25.mo.VirtualMachine;

import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.userdata.UserDataHelpers;
import net.roboconf.core.utils.Utils;
import net.roboconf.target.api.AbstractThreadedTargetHandler;
import net.roboconf.target.api.TargetException;
import net.roboconf.target.api.TargetHandlerParameters;

/**
 * @author Pierre-Yves Gibello - Linagora
 */
public class VmwareIaasHandler extends AbstractThreadedTargetHandler {

	public static final String TARGET_ID = "iaas-vmware";

	static final String URL = "vmware.url";
	static final String USER = "vmware.user";
	static final String PASSWORD = "vmware.password";
	static final String IGNORE_CERTIFICATE = "vmware.ignorecert";
	static final String TEMPLATE = "vmware.template";
	static final String CLUSTER = "vmware.cluster";
	static final String DATA_CENTER = "vmware.datacenter";
	static final String VM_USER = "vmware.vmuser";
	static final String VM_PASSWORD = "vmware.vmpassword";

	private final Logger logger = Logger.getLogger( getClass().getName());


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

		this.logger.fine( "Creating a new VM @ VMware." );

		// For IaaS, we only expect root instance names to be passed
		if( InstanceHelpers.countInstances( parameters.getScopedInstancePath()) > 1 )
			throw new TargetException( "Only root instances can be passed in arguments." );

		String rootInstanceName = InstanceHelpers.findRootInstancePath( parameters.getScopedInstancePath());

		// Deal with the creation
		try {
			System.setProperty("org.xml.sax.driver","org.apache.xerces.parsers.SAXParser");
			Map<String,String> targetProperties = parameters.getTargetProperties();
			final String machineImageId = targetProperties.get( TEMPLATE );
			final ServiceInstance vmwareServiceInstance = getServiceInstance( targetProperties );

			final ComputeResource vmwareComputeResource = (ComputeResource)(
					new InventoryNavigator( vmwareServiceInstance.getRootFolder())
					.searchManagedEntity("ComputeResource", targetProperties.get( CLUSTER )));

			// Generate the user data first, so that nothing has been done on the IaaS if it fails
			String userData = UserDataHelpers.writeUserDataAsString(
					parameters.getMessagingProperties(),
					parameters.getDomain(),
					parameters.getApplicationName(),
					rootInstanceName );

			VirtualMachine vm = getVirtualMachine( vmwareServiceInstance, machineImageId );
			String vmwareDataCenter = targetProperties.get( DATA_CENTER );
			Folder vmFolder =
					((Datacenter)(new InventoryNavigator( vmwareServiceInstance.getRootFolder())
					.searchManagedEntity("Datacenter", vmwareDataCenter)))
					.getVmFolder();

			this.logger.fine("machineImageId=" + machineImageId);
			if (vm == null || vmFolder == null)
				throw new TargetException("VirtualMachine (= " + vm + " ) or Datacenter path (= " + vmFolder + " ) is NOT correct. Please, double check.");

			VirtualMachineCloneSpec cloneSpec = new VirtualMachineCloneSpec();
			cloneSpec.setLocation(new VirtualMachineRelocateSpec());
			cloneSpec.setPowerOn(false);
			cloneSpec.setTemplate(true);

			VirtualMachineConfigSpec vmSpec = new VirtualMachineConfigSpec();
			vmSpec.setAnnotation( userData );
			cloneSpec.setConfig(vmSpec);

			Task task = vm.cloneVM_Task( vmFolder, rootInstanceName, cloneSpec );
			this.logger.fine("Cloning the template: "+ machineImageId +" ...");
			String status = task.waitForTask();
			if (!status.equals(Task.SUCCESS))
				throw new TargetException("Failure: Virtual Machine cannot be cloned." );

			VirtualMachine vm2 = getVirtualMachine( vmwareServiceInstance, rootInstanceName );
			this.logger.fine("Transforming the clone template to Virtual machine ...");
			vm2.markAsVirtualMachine( vmwareComputeResource.getResourcePool(), null);

			DynamicProperty dprop = new DynamicProperty();
			dprop.setName("guestinfo.userdata");
			dprop.setVal(userData);
			vm2.getGuest().setDynamicProperty(new DynamicProperty[]{dprop});

			task = vm2.powerOnVM_Task(null);
			this.logger.fine("Starting the virtual machine: "+ rootInstanceName +" ...");
			status = task.waitForTask();
			if( ! status.equals( Task.SUCCESS ))
				throw new TargetException("Failure: Virtual Machine cannot be started." );

			return vm2.getName();

		} catch( Exception e ) {
			throw new TargetException( e );
		}
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.target.api.AbstractThreadedTargetHandler#machineConfigurator(
	 * net.roboconf.target.api.TargetHandlerParameters, java.lang.String)
	 */
	@Override
	public MachineConfigurator machineConfigurator( TargetHandlerParameters parameters, String machineId ) {

		String userData = "";
		try {
			userData = UserDataHelpers.writeUserDataAsString(
					parameters.getMessagingProperties(),
					parameters.getDomain(),
					parameters.getApplicationName(),
					parameters.getScopedInstancePath());

		} catch( IOException e ) {
			this.logger.severe( "User data could not be generated." );
			Utils.logException( this.logger, e );
		}

		String rootInstanceName = InstanceHelpers.findRootInstancePath( parameters.getScopedInstancePath());
		return new VmWareMachineConfigurator(
				parameters.getTargetProperties(),
				userData,
				rootInstanceName,
				parameters.getScopedInstance());
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.target.api.TargetHandler
	 * #isMachineRunning(net.roboconf.target.api.TargetHandlerParameters, java.lang.String)
	 */
	@Override
	public boolean isMachineRunning( TargetHandlerParameters parameters, String machineId )
	throws TargetException {

		boolean result;
		try {
			final ServiceInstance vmwareServiceInstance = getServiceInstance( parameters.getTargetProperties());
			VirtualMachine vm = getVirtualMachine( vmwareServiceInstance, machineId );
			result = vm != null;

		} catch( Exception e ) {
			throw new TargetException( e );
		}

		return result;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.target.api.TargetHandler
	 * #terminateMachine(net.roboconf.target.api.TargetHandlerParameters, java.lang.String)
	 */
	@Override
	public void terminateMachine( TargetHandlerParameters parameters, String machineId ) throws TargetException {

		try {
			cancelMachineConfigurator( machineId );

			final ServiceInstance vmwareServiceInstance = getServiceInstance( parameters.getTargetProperties());
			VirtualMachine vm = getVirtualMachine( vmwareServiceInstance, machineId );
			if (vm == null)
				throw new TargetException( "Error vm: " + machineId + " was not found");

			Task task = vm.powerOffVM_Task();
			try {
				if(!(task.waitForTask()).equals(Task.SUCCESS))
					throw new TargetException( "Error while trying to stop vm: " + machineId);

			} catch (InterruptedException ignore) { /*ignore*/ }

			task = vm.destroy_Task();
			try {
				if(!(task.waitForTask()).equals(Task.SUCCESS))
					throw new TargetException( "Error while trying to remove vm: " + machineId);

			} catch (InterruptedException ignore) { /*ignore*/ }

		} catch( Exception e ) {
			throw new TargetException(e);
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

		String result = null;
		try {
			ServiceInstance vmwareServiceInstance = VmwareIaasHandler.getServiceInstance( parameters.getTargetProperties());
			String rootInstanceName = InstanceHelpers.findRootInstancePath( parameters.getScopedInstancePath());
			VirtualMachine vm = VmwareIaasHandler.getVirtualMachine( vmwareServiceInstance, rootInstanceName );
			if( vm != null )
				result = vm.getGuest().getIpAddress();

		} catch( Exception e ) {
			throw new TargetException( e );
		}

		return result;
	}


	static VirtualMachine getVirtualMachine( ServiceInstance vmwareServiceInstance , String virtualmachineName )
	throws RemoteException {

		VirtualMachine result = null;
		if( ! Utils.isEmptyOrWhitespaces( virtualmachineName )) {
			Folder rootFolder = vmwareServiceInstance.getRootFolder();
			result = (VirtualMachine) new InventoryNavigator(rootFolder).searchManagedEntity("VirtualMachine", virtualmachineName);
		}

		return result;
	}


	static ServiceInstance getServiceInstance( Map<String,String> targetProperties )
	throws RemoteException, MalformedURLException {

		return new ServiceInstance(
				new URL(targetProperties.get( URL )),
				targetProperties.get( USER ),
				targetProperties.get( PASSWORD ),
				Boolean.parseBoolean(targetProperties.get( IGNORE_CERTIFICATE )));
	}
}
