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

package net.roboconf.plugin.puppet;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import junit.framework.Assert;
import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.io.RuntimeModelIo;
import net.roboconf.core.model.io.RuntimeModelIo.ApplicationLoadResult;
import net.roboconf.core.model.runtime.Component;
import net.roboconf.core.model.runtime.Import;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.core.model.runtime.Instance.InstanceStatus;
import net.roboconf.core.utils.ProgramUtils;
import net.roboconf.core.utils.Utils;
import net.roboconf.plugin.puppet.PluginPuppet.PuppetState;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class PluginPuppetTest {

	final Instance instance = new Instance( "test-instance" );
	final PluginPuppet plugin = new PluginPuppet();
	private boolean running = true;


	/**
	 * A method to check that Puppet is installed on the machine.
	 * <p>
	 * If it is not running, tests in this class will be skipped.
	 * </p>
	 */
	@Before
	public void checkPuppetIsInstalled() throws Exception {

		Assume.assumeTrue( this.running );
		try {
			List<String> command = new ArrayList<String> ();
			command.add( "puppet" );
			command.add( "--version" );

			Logger logger = Logger.getLogger( getClass().getName());
			ProgramUtils.executeCommand( logger, command, null );

		} catch( Exception e ) {
			Logger logger = Logger.getLogger( getClass().getName());
			logger.warning( "Tests are skipped because Puppet is not installed." );
			logger.finest( Utils.writeException( e ));

			this.running = false;
			Assume.assumeNoException( e );
		}
	}


	@After
	public void clearTestDirectory() throws Exception {

		File instanceDirectory = InstanceHelpers.findInstanceDirectoryOnAgent( this.instance, this.plugin.getPluginName());
		Utils.deleteFilesRecursively( instanceDirectory );
	}


	@Test
	public void testInstallPuppetModules_withVersion() throws Exception {

		File instanceDirectory = InstanceHelpers.findInstanceDirectoryOnAgent( this.instance, this.plugin.getPluginName());
		if( ! instanceDirectory.exists()
				&& ! instanceDirectory.mkdirs())
			throw new IOException( "Failed to create the instance's directory." );

		File propFile = TestUtils.findTestFile( "/with-version/modules.properties" );
		this.plugin.installPuppetModules( this.instance, propFile.getParentFile());

		File[] subFiles = instanceDirectory.listFiles();

		Assert.assertNotNull( subFiles );
		Assert.assertEquals( 1, subFiles.length );
		Assert.assertTrue( subFiles[ 0 ].isDirectory());
	}


	@Test
	public void testInstallPuppetModules_withoutVersion() throws Exception {

		File instanceDirectory = InstanceHelpers.findInstanceDirectoryOnAgent( this.instance, this.plugin.getPluginName());
		if( ! instanceDirectory.exists()
				&& ! instanceDirectory.mkdirs())
			throw new IOException( "Failed to create the instance's directory." );

		File propFile = TestUtils.findTestFile( "/without-version/modules.properties" );
		this.plugin.installPuppetModules( this.instance, propFile.getParentFile());

		File[] subFiles = instanceDirectory.listFiles();

		Assert.assertNotNull( subFiles );
		Assert.assertEquals( 2, subFiles.length );
		Assert.assertTrue( subFiles[ 0 ].isDirectory());
		Assert.assertTrue( subFiles[ 1 ].isDirectory());
	}

	/**
	 * Test Puppet plugin on a real instance (from a fully functional app).
	 * Puppet module with only one init.pp manifest.
	 * The manifest produces files (based on templates) for each operation
	 * (deploy/start/stop/undeploy).
	 * @throws Exception
	 */
	@SuppressWarnings("serial")
	@Test
	public void testPuppetPlugin_WithInit() throws Exception {
		// Check for /tmp directory (skip if not present & writable)
		File tmp = new File("/tmp");
		if(! tmp.exists() && tmp.canWrite()) return;

		PluginPuppet plugin = new PluginPuppet();
		Instance inst = findInstance("/puppetplugin-unit-tests", "WithInit");
		//System.out.println("*** INSTANCE NAME=" + inst.getName());

		File instanceDirectory = InstanceHelpers.findInstanceDirectoryOnAgent(inst, plugin.getPluginName());
		
		Utils.copyDirectory(TestUtils.findTestFile("/puppetplugin-unit-tests/graph/WithInit"),
				instanceDirectory);

		File file;
		
		plugin.deploy(inst);
		file = new File("/tmp/WithInitFile.stopped");
		Assert.assertTrue(file.exists());
		file.delete();
		file = new File("/tmp/WithInitTemplate.stopped");
		Assert.assertTrue(file.exists());
		file.delete();

		plugin.start(inst);
		file = new File("/tmp/WithInitFile.running");
		Assert.assertTrue(file.exists());
		file.delete();
		file = new File("/tmp/WithInitTemplate.running");
		Assert.assertTrue(file.exists());
		file.delete();

		// Test update, passing changed import + status
		Import importChanged = new Import(
			InstanceHelpers.computeInstancePath(inst) + "Test",
			new HashMap<String, String>() {{ put("ip", "127.0.0.1"); }});
		InstanceStatus statusChanged = InstanceStatus.DEPLOYED_STARTED;
		plugin.update(inst, importChanged, statusChanged);
		file = new File("/tmp/WithInitFile.");
		Assert.assertTrue(file.exists());
		file.delete();
		file = new File("/tmp/WithInitTemplate.");
		Assert.assertTrue(file.exists());
		file.delete();

		plugin.stop(inst);
		file = new File("/tmp/WithInitFile.stopped");
		Assert.assertTrue(file.exists());
		file.delete();
		file = new File("/tmp/WithInitTemplate.stopped");
		Assert.assertTrue(file.exists());
		file.delete();

		plugin.undeploy(inst);
		file = new File("/tmp/WithInitFile.");
		Assert.assertTrue(file.exists());
		file.delete();
		file = new File("/tmp/WithInitTemplate.");
		Assert.assertTrue(file.exists());
		file.delete();

		Utils.deleteFilesRecursively(instanceDirectory);
	}

	/**
	 * Test Puppet plugin on a real instance (from a fully functional app).
	 * Puppet module with one manifest per operation.
	 * The manifest produces files (based on templates) for each operation
	 * (deploy/start/stop/undeploy).
	 * @throws Exception
	 */
	@SuppressWarnings("serial")
	@Test
	public void testPuppetPlugin_WithOperations() throws Exception {
		// Check for /tmp directory (skip if not present & writable)
		File tmp = new File("/tmp");
		if(! tmp.exists() && tmp.canWrite()) return;
				
		PluginPuppet plugin = new PluginPuppet();
		Instance inst = findInstance("/puppetplugin-unit-tests", "WithOperations");
		//System.out.println("*** INSTANCE NAME=" + inst.getName());

		File instanceDirectory = InstanceHelpers.findInstanceDirectoryOnAgent(inst, plugin.getPluginName());
		
		Utils.copyDirectory(TestUtils.findTestFile("/puppetplugin-unit-tests/graph/WithOperations"),
				instanceDirectory);

		File file;
		
		plugin.deploy(inst);
		file = new File("/tmp/WithOperationsFile.deploy");
		Assert.assertTrue(file.exists());
		file.delete();
		file = new File("/tmp/WithOperationsTemplate.deploy");
		Assert.assertTrue(file.exists());
		file.delete();

		plugin.start(inst);
		file = new File("/tmp/WithOperationsFile.start");
		Assert.assertTrue(file.exists());
		file.delete();
		file = new File("/tmp/WithOperationsTemplate.start");
		Assert.assertTrue(file.exists());
		file.delete();

		// Test update, passing changed import + status
		Import importChanged = new Import(
			InstanceHelpers.computeInstancePath(inst) + "Test",
			new HashMap<String, String>() {{ put("ip", "127.0.0.1"); }});
		InstanceStatus statusChanged = InstanceStatus.DEPLOYED_STARTED;
		plugin.update(inst, importChanged, statusChanged);
		file = new File("/tmp/WithOperationsFile.update");
		Assert.assertTrue(file.exists());
		file.delete();
		file = new File("/tmp/WithOperationsTemplate.update");
		Assert.assertTrue(file.exists());
		file.delete();
		
		plugin.stop(inst);
		file = new File("/tmp/WithOperationsFile.stop");
		Assert.assertTrue(file.exists());
		file.delete();
		file = new File("/tmp/WithOperationsTemplate.stop");
		Assert.assertTrue(file.exists());
		file.delete();
		
		plugin.undeploy(inst);
		file = new File("/tmp/WithOperationsFile.undeploy");
		Assert.assertTrue(file.exists());
		file.delete();
		file = new File("/tmp/WithOperationsTemplate.undeploy");
		Assert.assertTrue(file.exists());
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

	@Test
	public void testFormatExportedVariables() {

		Map<String,String> exports = new LinkedHashMap<String,String> ();
		exports.put( "port", "3306" );
		exports.put( "MySQL.port", "3306" );
		exports.put( "ip", "" );
		exports.put( "MySQL.ip", null );

		String expected = "port => '3306', port => '3306', ip => undef, ip => undef";
		Assert.assertEquals( expected, this.plugin.formatExportedVariables( exports ));
	}


	@Test
	public void testFormatInstanceImports_noImportAtAll() {

		Instance instance = new Instance( "test" ).component( new Component( "test-component" ));
		Assert.assertEquals( "", this.plugin.formatInstanceImports( instance ));
	}


	@Test
	public void testFormatInstanceImports_noImport() {

		Component component = new Component( "test-component" );
		component.getImportedVariables().put( "MySQL.port", false );
		Instance instance = new Instance( "test" ).component( component );

		Assert.assertEquals( "mysql => undef", this.plugin.formatInstanceImports( instance ));
	}


	@Test
	public void testFormatInstanceImports_oneImportOneVariable() {

		Map<String,String> exports = new LinkedHashMap<String,String> ();
		exports.put( "MySQL.port", "3306" );
		Import imp = new Import( "/toto", exports );

		Component component = new Component( "test-component" );
		component.getImportedVariables().put( "MySQL.port", false );

		Instance instance = new Instance( "test" ).component( component );
		instance.getImports().put( "MySQL", Arrays.asList( imp ));

		Assert.assertEquals(
				"mysql => { '/toto' => { port => '3306' }}",
				this.plugin.formatInstanceImports( instance ));
	}


	@Test
	public void testFormatInstanceImports_oneImportTwoVariables() {

		Map<String,String> exports = new LinkedHashMap<String,String> ();
		exports.put( "MySQL.port", "3306" );
		exports.put( "MySQL.ip", "172.16.20.12" );
		Import imp = new Import( "/toto", exports );

		Component component = new Component( "test-component" );
		component.getImportedVariables().put( "MySQL.port", false );
		component.getImportedVariables().put( "MySQL.ip", false );

		Instance instance = new Instance( "test" ).component( component );
		instance.getImports().put( "MySQL", Arrays.asList( imp ));

		Assert.assertEquals(
				"mysql => { '/toto' => { port => '3306', ip => '172.16.20.12' }}",
				this.plugin.formatInstanceImports( instance ));
	}


	@Test
	public void testFormatInstanceImports_twoImportsTwoVariables() {

		List<Import> imports = new ArrayList<Import> ();
		for( int i=0; i<2; i++ ) {
			Map<String,String> exports = new LinkedHashMap<String,String> ();
			exports.put( "MySQL.port", String.valueOf( 3306 + i ));
			exports.put( "MySQL.ip", "172.16.20." + String.valueOf( 12 + i ));
			imports.add( new Import( "/toto-" + i, exports ));
		}

		Component component = new Component( "test-component" );
		component.getImportedVariables().put( "MySQL.port", false );
		component.getImportedVariables().put( "MySQL.ip", false );

		Instance instance = new Instance( "test" ).component( component );
		instance.getImports().put( "MySQL", imports );

		Assert.assertEquals(
				"mysql => { '/toto-0' => { port => '3306', ip => '172.16.20.12' }, '/toto-1' => { port => '3307', ip => '172.16.20.13' }}",
				this.plugin.formatInstanceImports( instance ));
	}


	@Test
	public void testPuppetState() {

		for( PuppetState state : PuppetState.values()) {
			Assert.assertEquals( state.toString(), state.toString().toLowerCase(), state.toString());
		}
	}


	@Test
	public void testGenerateCodeToExecute() {

		Instance instance = new Instance( "test" ).component( new Component( "test-component" ));
		String expectedPrefix = "class{'roboconf_test-component': runningState => ";

		for( PuppetState state : PuppetState.values()) {
			String s = this.plugin.generateCodeToExecute( "roboconf_test-component", instance, state, null, false );
			Assert.assertTrue( state.toString(), s.startsWith( expectedPrefix + state.toString()));
		}

	}

}
