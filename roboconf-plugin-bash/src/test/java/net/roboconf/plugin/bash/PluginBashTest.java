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

import static org.junit.Assert.*;

import java.io.File;
import java.util.HashMap;
import java.util.List;

import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.io.RuntimeModelIo;
import net.roboconf.core.model.io.RuntimeModelIo.ApplicationLoadResult;
import net.roboconf.core.model.runtime.Import;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.core.model.runtime.Instance.InstanceStatus;
import net.roboconf.core.utils.Utils;

import org.junit.Test;

public class PluginBashTest {

	/**
	 * Test Bash plugin (scripts only) on a real instance (from a fully functional app).
	 * The bash scripts produce files (some based on templates)
	 * for each operation (deploy/start/stop/undeploy).
	 * @throws Exception
	 */
	@SuppressWarnings("serial")
	@Test
	public void testBashPlugin_Script() throws Exception {
		// Check for /tmp directory (skip if not present & writable)
		File tmp = new File("/tmp");
		if(! tmp.exists() && tmp.canWrite()) return;

		PluginBash plugin = new PluginBash();
		Instance inst = findInstance("/bashplugin-unit-tests", "BashScript");
		//System.out.println("*** INSTANCE NAME=" + inst.getName());

		File instanceDirectory = InstanceHelpers.findInstanceDirectoryOnAgent(inst, plugin.getPluginName());
		
		Utils.copyDirectory(TestUtils.findTestFile("/bashplugin-unit-tests/graph/BashScript"),
				instanceDirectory);
		
		File file;
		
		plugin.deploy(inst);
		file = new File("/tmp/BashScriptFile.deploy");
		assertTrue(file.exists());
		file.delete();

		plugin.start(inst);
		file = new File("/tmp/BashScriptFile.start");
		assertTrue(file.exists());
		file.delete();
		
		// Test update, passing changed import + status
		Import importChanged = new Import(
			InstanceHelpers.computeInstancePath(inst) + "Test",
			inst.getComponent().getName(),
			new HashMap<String, String>() {{ put("ip", "127.0.0.1"); }});
		InstanceStatus statusChanged = InstanceStatus.DEPLOYED_STARTED;
		plugin.update(inst, importChanged, statusChanged);
		file = new File("/tmp/BashScriptFile.update");
		assertTrue(file.exists());
		file.delete();

		plugin.stop(inst);
		file = new File("/tmp/BashScriptFile.stop");
		assertTrue(file.exists());
		file.delete();

		plugin.undeploy(inst);
		file = new File("/tmp/BashScriptFile.undeploy");
		assertTrue(file.exists());
		file.delete();

		Utils.deleteFilesRecursively(instanceDirectory);
	}

	/**
	 * Test Bash plugin (templates only) on a real instance (from a fully functional app).
	 * The bash scripts produce files (some based on templates)
	 * for each operation (deploy/start/stop/undeploy).
	 * @throws Exception
	 */
	@SuppressWarnings("serial")
	@Test
	public void testBashPlugin_Template() throws Exception {
		// Check for /tmp directory (skip if not present & writable)
		File tmp = new File("/tmp");
		if(! tmp.exists() && tmp.canWrite()) return;

		PluginBash plugin = new PluginBash();
		Instance inst = findInstance("/bashplugin-unit-tests", "BashTemplate");
		//System.out.println("*** INSTANCE NAME=" + inst.getName());

		File instanceDirectory = InstanceHelpers.findInstanceDirectoryOnAgent(inst, plugin.getPluginName());
		
		Utils.copyDirectory(TestUtils.findTestFile("/bashplugin-unit-tests/graph/BashTemplate"),
				instanceDirectory);
		
		File file;
		
		plugin.deploy(inst);
		file = new File("/tmp/BashTemplateFile.deploy");
		assertTrue(file.exists());
		file.delete();

		plugin.start(inst);
		file = new File("/tmp/BashTemplateFile.start");
		assertTrue(file.exists());
		file.delete();

		// Test update, passing changed import + status
		Import importChanged = new Import(
			InstanceHelpers.computeInstancePath(inst) + "Test",
			inst.getComponent().getName(),
			new HashMap<String, String>() {{ put("ip", "127.0.0.1"); }});
		InstanceStatus statusChanged = InstanceStatus.DEPLOYED_STARTED;
		plugin.update(inst, importChanged, statusChanged);
		file = new File("/tmp/BashTemplateFile.update");
		assertTrue(file.exists());
		file.delete();
	
		plugin.stop(inst);
		file = new File("/tmp/BashTemplateFile.stop");
		assertTrue(file.exists());
		file.delete();

		plugin.undeploy(inst);
		file = new File("/tmp/BashTemplateFile.undeploy");
		assertTrue(file.exists());
		file.delete();

		Utils.deleteFilesRecursively(instanceDirectory);
	}

	private Instance findInstance(String appDirPath, String instanceName) throws Exception {
		File appDir = TestUtils.findTestFile(appDirPath);
		ApplicationLoadResult result = RuntimeModelIo.loadApplication(appDir);
		List<Instance> instances = null;
		for(Instance root : result.getApplication().getRootInstances()) {
			instances = InstanceHelpers.buildHierarchicalList(root);
			
			for(Instance inst : instances) {
				if(inst.getName().equals(instanceName)) return inst;
			}
		}
		return null;
	}
}
