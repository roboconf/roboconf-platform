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

package net.roboconf.plugin.bash;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import net.roboconf.core.internal.utils.ProgramUtils;
import net.roboconf.core.model.helpers.VariableHelpers;
import net.roboconf.core.model.runtime.Import;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.plugin.api.ExecutionLevel;
import net.roboconf.plugin.api.PluginInterface;

/**
 * @author Noël - LIG
 * @author Linh-Manh Pham - LIG
 * @author Pierre-Yves Gibello - Linagora
 * TODO: OMG...
 */
public class PluginBash implements PluginInterface {

	private final Logger logger = Logger.getLogger( getClass().getName());
	private ExecutionLevel executionLevel;
	private File dumpDirectory;
	private String agentName;


	@Override
	public void setExecutionLevel( ExecutionLevel executionLevel ) {
		this.executionLevel = executionLevel;
	}


	@Override
	public void setDumpDirectory( File dumpDirectory ) {
		this.dumpDirectory = dumpDirectory;
	}


	@Override
	public void setAgentName( String agentName ) {
		this.agentName = agentName;
	}


	@Override
	public void deploy( Instance instance ) throws Exception {
		this.logger.fine( this.agentName + " is deploying instance " + instance.getName());
		prepareAndExecuteCommand( "setup", instance );
	}


	@Override
	public void start( Instance instance ) throws Exception {
		this.logger.fine( this.agentName + " is starting instance " + instance.getName());
		prepareAndExecuteCommand( "start", instance );
	}


	@Override
	public void update( Instance instance ) throws Exception {
		this.logger.fine( this.agentName + " is updating instance " + instance.getName());
		prepareAndExecuteCommand( "update", instance );
	}


	@Override
	public void stop( Instance instance ) throws Exception {
		this.logger.fine( this.agentName + " is stopping instance " + instance.getName());
		prepareAndExecuteCommand( "stop", instance );
	}


	@Override
	public void undeploy( Instance instance ) throws Exception {
		// TODO
	}


	private void prepareAndExecuteCommand(String action, Instance instance) throws Exception {

		this.logger.info("Calling of bash " + action);
		String[] command = { "bash", "/tmp/roboconf/bash/" + instance.getComponent().getName().toLowerCase() + "/script/" + action + ".sh" };
		// Map that will contain each value that is going to use the script
		Map<String, String> environmentVars = new HashMap<String, String>();
		// We add to it simple vars composing the instance
		Map<String, String> vars = formatExportedVars(instance);
		environmentVars.putAll(vars);
		// Now put each imported vars if there are
		Map<String, String> importedVars = formatImportedVars(instance);
		environmentVars.putAll(importedVars);
		// PHAM Linh: To put Instance Name, Container Name into set of environment vars 20/02/2013
		environmentVars.put("instanceName", instance.getName());

		// PHAM Linh: To put Instance Name, Container Name into set of environment vars 20/02/2013
		//TODO is plugin necessary here ? now ClassNotFound is ignored (except stack trace)...
		if (action.equals("setup")) {
			this.logger.info("Setup Action!");
			invokeRemoteMethod("setup", instance, command, environmentVars);
		} else if (action.equals("start")) {
			this.logger.info("Start Action!");
			invokeRemoteMethod("start", instance, command, environmentVars);
		} else if (action.equals("stop")) {
			this.logger.info("Stop Action!");
			invokeRemoteMethod("stop", instance, command, environmentVars);
		} else if (action.equals("update")) {
			this.logger.info("Update Action!");
			invokeRemoteMethod("update", instance, command, environmentVars);
		} else {
			this.logger.info("Invalid Action!");
		}

		//TODO provide default plugin instead of doing this ??
		ProgramUtils.executeCommand( this.logger, command, environmentVars );
	}


	@SuppressWarnings("rawtypes")
	private Class getRemoteClass(String containerTypeName, String configuratorType, String instanceName) throws MalformedURLException, ClassNotFoundException {
		URL classUrl;
		String jarFileName = "Roboconf-Container"+containerTypeName+".jar";
		String jarPath = "/tmp/roboconf/"+configuratorType+"/"+containerTypeName.toLowerCase()+"/";
		String pathURL = "file://"+jarPath+jarFileName;
		this.logger.info("Class loaded from "+pathURL);
		classUrl = new URL(pathURL);
		this.logger.info("RC1");
		URL[] classUrls = { classUrl };
		this.logger.info("RC2");

		URLClassLoader ucl = new URLClassLoader(classUrls);
		this.logger.info("RC3");
		String containerTypeNameLowerCase = containerTypeName.toLowerCase();
		this.logger.info("RC4");
		String classToLoad = "net.roboconf.core.model.server.container."+containerTypeNameLowerCase+".Container"+containerTypeName;
		this.logger.info("RC5");
		Class c = ucl.loadClass(classToLoad);
		this.logger.info("RC6");
		return c;
	}


	public void invokeRemoteMethod(String methodName, Instance instance, String[] command, Map<String, String> environmentVars) {
		if (methodName.equals("setup")) {
			HashMap<String,String> argsSetup = new HashMap<String,String>(environmentVars);
			argsSetup.put("configurator", command[0]);
			argsSetup.put("scriptPath", command[1]);
			invokeRemoteSetupMethod(instance.getName(), "bash", instance.getName(), argsSetup);
		} else if (methodName.equals("start")) {
			HashMap<String,String> argsStart = new HashMap<String,String>(environmentVars);
			argsStart.put("configurator", command[0]);
			argsStart.put("scriptPath", command[1]);
			invokeRemoteStartMethod(instance.getName(), "bash", instance.getName(), argsStart);
		} else if (methodName.equals("stop")) {
			HashMap<String,String> argsStop = new HashMap<String,String>(environmentVars);
			argsStop.put("configurator", command[0]);
			argsStop.put("scriptPath", command[1]);
			invokeRemoteStopMethod(instance.getName(), "bash", instance.getName(), argsStop);
		} else if (methodName.equals("update")) {
			HashMap<String,String> argsUpdate = new HashMap<String,String>(environmentVars);
			argsUpdate.put("configurator", command[0]);
			argsUpdate.put("scriptPath", command[1]);
			invokeRemoteUpdateMethod(instance.getName(), "bash", instance.getName(), argsUpdate);
		}
	}


	private void invokeRemoteSetupMethod (String containerTypeName, String configuratorType, String instanceName, HashMap<String,String> argsSetup) {
		try {
			@SuppressWarnings("rawtypes")
			Class c = getRemoteClass(containerTypeName, configuratorType, instanceName);
	    	Object whatInstance = c.newInstance();
	    	@SuppressWarnings("unchecked")
			Method myMethodSetup = c.getDeclaredMethod("setup", new Class[] {HashMap.class});
	    	myMethodSetup.invoke(whatInstance, argsSetup);
		} catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
        	e.printStackTrace();
        }
	}


	private void invokeRemoteStartMethod (String containerTypeName, String configuratorType, String instanceName, HashMap<String,String> argsStart) {
		try {
			this.logger.info("2");
			@SuppressWarnings("rawtypes")
			Class c = getRemoteClass(containerTypeName, configuratorType, instanceName);
	    	Object whatInstance = c.newInstance();
	    	@SuppressWarnings("unchecked")
			Method myMethodStart = c.getDeclaredMethod("start", new Class[] {HashMap.class});
            myMethodStart.invoke(whatInstance, argsStart);
		} catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
        	e.printStackTrace();
        }
	}


	private void invokeRemoteStopMethod (String containerTypeName, String configuratorType, String instanceName, HashMap<String,String> argsStop) {
		try {
			@SuppressWarnings("rawtypes")
			Class c = getRemoteClass(containerTypeName, configuratorType, instanceName);
	    	Object whatInstance = c.newInstance();
	    	@SuppressWarnings("unchecked")
			Method myMethodStop = c.getDeclaredMethod("stop", new Class[] {HashMap.class});
	    	myMethodStop.invoke(whatInstance, argsStop);
		} catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
        	e.printStackTrace();
        }
	}


	private void invokeRemoteUpdateMethod (String containerTypeName, String configuratorType, String instanceName, HashMap<String,String> argsUpdate) {
		try {
			@SuppressWarnings("rawtypes")
			Class c = getRemoteClass(containerTypeName, configuratorType, instanceName);
	    	Object whatInstance = c.newInstance();
	    	@SuppressWarnings("unchecked")
			Method myMethodUpdate = c.getDeclaredMethod("update", new Class[] {HashMap.class});
            myMethodUpdate.invoke(whatInstance, argsUpdate);
		} catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
        	e.printStackTrace();
        }
	}


	private Map<String, String> formatExportedVars(Instance instance) {
		// The map we will return
		Map<String, String> exportedVars = new HashMap<String, String>();
		for(Entry<String, String> entry : instance.getComponent().getExportedVariables().entrySet()) {
			String vname = VariableHelpers.parseVariableName(entry.getKey()).getValue();
			exportedVars.put(vname, entry.getValue());
		}
		return exportedVars;
	}


	/**
	 * Simple vars are formatted a simple way ("myVarName=varValue"),
	 * while Imported vars must be formatted in a more complex way.
	 * Taking the example of Apache needing workers in the example Apache-Tomcat-SQL,
	 * we chose to adopt the following style:
	 * ==========================
	 * "workers_size=3"
	 * "workers_0_name=tomcat1"
	 * "workers_0_ip=127.0.0.1"
	 * "workers_0_portAJP=8009"
	 * "workers_1_name=tomcat2"
	 * .....
	 * "workers_2_portAJP=8010"
	 * ==========================
	 * With this way of formatting vars, the Bash script will know
	 * everything it needs to use these vars
	 *
	 * @param instance
	 * @return
	 */
	private Map<String, String> formatImportedVars(Instance instance) {

		// The map we will return
		Map<String, String> importedVars = new HashMap<String, String>();
		for( Map.Entry<String,Collection<Import>> entry : instance.getImports().entrySet()) {
			Collection<Import> importList = entry.getValue();
			String importTypeName = entry.getKey();

			// For each ImportType, put the number of Import it has, so the script knows
			importedVars.put(importTypeName + "_size", "" + importList.size());

			// Now put each var contained in an Import
			int i = 0;
			for(Import imprt : importList) {
				// "workers_0_name=tomcat1"

				int index = imprt.getInstancePath().lastIndexOf( '/' );
				String instanceName = imprt.getInstancePath().substring( index + 1 );

				importedVars.put(importTypeName + "_" + i + "_name", instanceName);
				for (Entry<String, String> entry2 : imprt.getExportedVars().entrySet()) {
					// "workers_0_ip=127.0.0.1"
					String vname = VariableHelpers.parseVariableName(entry2.getKey()).getValue();
					importedVars.put(importTypeName + "_" + i + "_" + vname, entry2.getValue());
				}
				++i;
			}
		}

		return importedVars;
	}
}
