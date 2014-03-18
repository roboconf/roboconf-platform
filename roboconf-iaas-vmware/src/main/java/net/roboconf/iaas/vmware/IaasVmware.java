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

package net.roboconf.iaas.vmware;

import java.net.URL;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.logging.Logger;

import net.roboconf.iaas.api.IaasInterface;
import net.roboconf.iaas.api.exceptions.CommunicationToIaasException;
import net.roboconf.iaas.api.exceptions.IaasException;
import net.roboconf.iaas.api.exceptions.InvalidIaasPropertiesException;

import com.vmware.vim25.DynamicProperty;
import com.vmware.vim25.GuestProgramSpec;
import com.vmware.vim25.InvalidProperty;
import com.vmware.vim25.NamePasswordAuthentication;
import com.vmware.vim25.RuntimeFault;
import com.vmware.vim25.VirtualMachineCloneSpec;
import com.vmware.vim25.VirtualMachineConfigSpec;
import com.vmware.vim25.VirtualMachineRelocateSpec;
import com.vmware.vim25.mo.ComputeResource;
import com.vmware.vim25.mo.Datacenter;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.GuestOperationsManager;
import com.vmware.vim25.mo.GuestProcessManager;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.Task;
import com.vmware.vim25.mo.VirtualMachine;

/**
 * @author Pierre-Yves Gibello - Linagora
 */
public class IaasVmware implements IaasInterface {

	//TODO replace all println() with logger...
	private Logger logger;
	private ServiceInstance vmwareServiceInstance;
	private ComputeResource vmwareComputeResource;
	private String vmwareDataCenter;
	private String machineImageId;
	private  Map<String, String> iaasProperties;

	/**
	 * Constructor.
	 */
	public IaasVmware() {
		this.logger = Logger.getLogger( getClass().getName());
	}


	/**
	 * @param logger the logger to set
	 */
	public void setLogger( Logger logger ) {
		this.logger = logger;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.iaas.api.IaasInterface
	 * #setIaasProperties(net.roboconf.iaas.api.IaasProperties)
	 */
	@Override
	public void setIaasProperties(Map<String, String> iaasProperties) throws InvalidIaasPropertiesException {

		this.iaasProperties = iaasProperties;
		this.machineImageId = iaasProperties.get("vmware.template");
		this.vmwareDataCenter = iaasProperties.get("vmware.datacenter");

		try {
			this.vmwareServiceInstance = new ServiceInstance(
					new URL(iaasProperties.get("vmware.url")),
					iaasProperties.get("vmware.user"),
					iaasProperties.get("vmware.password"),
					Boolean.parseBoolean(iaasProperties.get("vmware.ignorecert")));
			//vmwareServiceInstance.getPropertyCollector().
			this.vmwareComputeResource = (ComputeResource)(new InventoryNavigator(vmwareServiceInstance.getRootFolder())
				.searchManagedEntity("ComputeResource", iaasProperties.get("vmware.cluster")));
		} catch(Exception e) {
			throw new InvalidIaasPropertiesException(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see net.roboconf.iaas.api.IaasInterface
	 * #createVM(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public String createVM(
			String machineImageId,
			String ipMessagingServer,
			String channelName,
			String applicationName)
	throws IaasException, CommunicationToIaasException {

		if(machineImageId == null || "".equals(machineImageId))
			machineImageId = this.machineImageId;

		try {
			
			//String instanceId = null;
			VirtualMachine vm = getVirtualMachine(machineImageId);
			//Folder vmFolder = this.vmwareServiceInstance.getRootFolder();
			Folder vmFolder = ((Datacenter)(new InventoryNavigator(this.vmwareServiceInstance.getRootFolder())
				.searchManagedEntity("Datacenter", this.vmwareDataCenter))).getVmFolder();

			// TODO remove system.out logs (in the whole file...)
			System.out.println("machineImageId=" + machineImageId);
			if (vm == null || vmFolder == null)
				throw new IaasException("VirtualMachine (= " + vm + " ) or Datacenter path (= " + vmFolder + " ) is NOT correct. Pls double check.");

			VirtualMachineCloneSpec cloneSpec = new VirtualMachineCloneSpec();
			cloneSpec.setLocation(new VirtualMachineRelocateSpec());
			cloneSpec.setPowerOn(false);
			cloneSpec.setTemplate(true);

			VirtualMachineConfigSpec vmSpec = new VirtualMachineConfigSpec();
			String userData = "applicationName=" + applicationName
					+ "\\nmachineName=" + channelName //TBD machineName=channelName
					+ "\\nchannelName=" + channelName
					+ "\\nipMessagingServer=" + ipMessagingServer;
			vmSpec.setAnnotation(userData);
			
			cloneSpec.setConfig(vmSpec);

			Task task = vm.cloneVM_Task(vmFolder, channelName, cloneSpec);
			System.out.println("Cloning the template: "+machineImageId+" ...");	
			String status = task.waitForTask();
			if (!status.equals(Task.SUCCESS)) {
				throw new IaasException("Failure: Virtual Machine cannot be cloned");
			}

			VirtualMachine vm2 = getVirtualMachine(channelName);		
			System.out.println("Transforming the clone template to Virtual machine ...");
			vm2.markAsVirtualMachine(this.vmwareComputeResource.getResourcePool(), null); // host=null means IaaS-managed choice
			
			DynamicProperty dprop = new DynamicProperty();
            dprop.setName("guestinfo.userdata");
            dprop.setVal(userData);
            vm2.getGuest().setDynamicProperty(new DynamicProperty[]{dprop});
			
			task = vm2.powerOnVM_Task(null);
			System.out.println("Starting the virtual machine: "+channelName+" ...");	
			status = task.waitForTask();
			if (!status.equals(Task.SUCCESS)) {
				throw new IaasException("Failure -: Virtual Machine cannot be started");
			}
			
			Thread.sleep(10000); // VMWare tools not yet started (!)
			
			GuestOperationsManager gom = vmwareServiceInstance.getGuestOperationsManager();
			//GuestAuthManager gam = gom.getAuthManager(vm2);
		    NamePasswordAuthentication npa = new NamePasswordAuthentication();
		    npa.username = iaasProperties.get("vmware.vmuser");
		    npa.password = iaasProperties.get("vmware.vmpassword");
		    GuestProgramSpec spec = new GuestProgramSpec();

		    spec.programPath = "/bin/echo";
		    spec.arguments = "$\'" + userData + "\' > /tmp/roboconf.properties";
		    //System.out.println(spec.programPath + " " + spec.arguments);
		    
		    GuestProcessManager gpm = gom.getProcessManager(vm2);
		    long pid = gpm.startProgramInGuest(npa, spec);
		    System.out.println("pid: " + pid);

			return vm2.getName();
			//return instanceId;
		} catch(RemoteException e) {
			e.printStackTrace();
			throw new CommunicationToIaasException(e);
		} catch (InterruptedException e) {
			e.printStackTrace();
			throw new CommunicationToIaasException(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see net.roboconf.iaas.api.IaasInterface
	 * #terminateVM(java.lang.String)
	 */
	@Override
	public void terminateVM( String instanceId ) throws IaasException, CommunicationToIaasException {
		try {
			VirtualMachine vm = getVirtualMachine(instanceId);
			if (vm == null) {
				throw new IaasException("error vm: "+instanceId+" not found");
			}

			Task task = vm.powerOffVM_Task();
			try {
				if(!(task.waitForTask()).equals(Task.SUCCESS)) {		
					throw new IaasException("error when trying to stop vm: "+instanceId);
				}
			} catch (InterruptedException ignore) { /*ignore*/ }
			task = vm.destroy_Task();
			try {
				if(!(task.waitForTask()).equals(Task.SUCCESS)) {		
					throw new IaasException("error when trying to remove vm: "+instanceId);
				}
			} catch (InterruptedException ignore) { /*ignore*/ }
		} catch(RemoteException e) {
			throw new CommunicationToIaasException(e);
		}
	}

	private VirtualMachine getVirtualMachine(String virtualmachineName)
	throws InvalidProperty, RuntimeFault, RemoteException {
		if(virtualmachineName == null || virtualmachineName.equals(""))
			return null;
				
		Folder rootFolder = this.vmwareServiceInstance.getRootFolder();
		return (VirtualMachine) new InventoryNavigator(rootFolder).searchManagedEntity("VirtualMachine", virtualmachineName);
	}

}
