/**
 * Copyright 2013-2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.plugin.script.internal;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.beans.Component;
import net.roboconf.core.model.beans.Import;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.utils.Utils;
import net.roboconf.plugin.api.PluginException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Pierre-Yves Gibello - Linagora
 */
public class PluginScriptTest {

	// This is hard-coded path for Linux, but this is because the Script plugin
	// only makes sense for Linux systems. Tests that try to execute scripts
	// first check that we are indeed on a Linux system. Otherwise, they are skipped.
	private final static File OUTPUT_DIR = new File( "/tmp/roboconf-test-for-bash" );

	private final Instance inst = new Instance( "sample" )
			.component( new Component( "some-component" )
			.installerName( PluginScript.PLUGIN_NAME ));

	private final File instanceDirectory = InstanceHelpers.findInstanceDirectoryOnAgent( this.inst );
	private PluginScript plugin;


	@Before
	public void resetPlugin() throws Exception {

		// New plugin instance
		this.plugin = new PluginScript();
		this.plugin.setNames( "app", "test" );

		// Useful to watch real bash content on debug (and for code coverage)
		this.inst.overriddenExports.put( "facet.prop1", "value1" );
		this.inst.overriddenExports.put( "some-component.prop2", "value2" );
		this.inst.overriddenExports.put( "prop3", "value3" );

		Map<String,String> exportedVariables1 = new HashMap<String,String> ();
		exportedVariables1.put( "ip", "http://192.168.1.15" );
		exportedVariables1.put( "port", "80" );

		Map<String,String> exportedVariables2 = new HashMap<String,String>( exportedVariables1 );
		exportedVariables1.put( "ip", "http://192.168.1.84" );

		Import imp1 = new Import( "/vm1/apache", "apache", exportedVariables1 );
		Import imp2 = new Import( "/vm1/apache", "apache", exportedVariables2 );
		this.inst.getImports().clear();
		this.inst.getImports().put( "apache", Arrays.asList( imp1, imp2 ));
	}


	@After
	public void clearPreviousOutputs() throws Exception {
		Utils.deleteFilesRecursively( this.instanceDirectory );
		Utils.deleteFilesRecursively( OUTPUT_DIR );
	}


	@Test
	public void testPluginName() {
		Assert.assertEquals( PluginScript.PLUGIN_NAME, new PluginScript().getPluginName());
	}


	@Test
	public void testSetNames() {

		Assert.assertNotNull( this.plugin.agentId );
		this.plugin.agentId = null;
		Assert.assertNull( this.plugin.agentId );

		this.plugin.setNames( "app", null );
		Assert.assertNotNull( this.plugin.agentId );

		this.plugin.setNames( null, "test" );
		Assert.assertNotNull( this.plugin.agentId );
	}


	@Test
	public void testInitialize() throws Exception {
		Instance inst = new Instance("whatever").component(new Component("whatever").installerName( PluginScript.PLUGIN_NAME ));
		this.plugin.initialize( inst );
	}


	@Test
	public void testDeploy() throws Exception {

		Assume.assumeTrue( isLinuxSystem());
		copyResources( "/BashScript" );

		File file = new File( OUTPUT_DIR, "BashScriptFile.deploy" );
		Assert.assertFalse( file.exists());
		this.plugin.deploy( this.inst );
		assertTrue( file.exists());
	}


	@Test( expected = PluginException.class )
	public void testDeploy_exception() throws Exception {

		Assume.assumeTrue( isLinuxSystem());
		copyResources( "/BashScriptError" );
		this.plugin.deploy( this.inst );
	}


	@Test
	public void testDeploy_template() throws Exception {

		Assume.assumeTrue( isLinuxSystem());
		copyResources( "/BashTemplate" );

		File file = new File( OUTPUT_DIR, "BashTemplateFile.deploy" );
		Assert.assertFalse( file.exists());
		this.plugin.deploy( this.inst );
		assertTrue( file.exists());
	}


	@Test
	public void testStart() throws Exception {

		Assume.assumeTrue( isLinuxSystem());
		copyResources( "/BashScript" );

		File file = new File( OUTPUT_DIR, "BashScriptFile.start" );
		Assert.assertFalse( file.exists());
		this.plugin.start( this.inst );
		assertTrue( file.exists());
	}


	@Test( expected = PluginException.class )
	public void testStart_exception() throws Exception {

		Assume.assumeTrue( isLinuxSystem());
		copyResources( "/BashScriptError" );
		this.plugin.start( this.inst );
	}


	@Test
	public void testStart_template() throws Exception {

		Assume.assumeTrue( isLinuxSystem());
		copyResources( "/BashTemplate" );

		File file = new File( OUTPUT_DIR, "BashTemplateFile.start" );
		Assert.assertFalse( file.exists());
		this.plugin.start( this.inst );
		assertTrue( file.exists());
	}


	@Test
	public void testStop() throws Exception {

		Assume.assumeTrue( isLinuxSystem());
		copyResources( "/BashScript" );

		File file = new File( OUTPUT_DIR, "BashScriptFile.stop" );
		Assert.assertFalse( file.exists());
		this.plugin.stop( this.inst );
		assertTrue( file.exists());
	}


	@Test( expected = PluginException.class )
	public void testStop_exception() throws Exception {

		Assume.assumeTrue( isLinuxSystem());
		copyResources( "/BashScriptError" );
		this.plugin.stop( this.inst );
	}


	@Test
	public void testStop_template() throws Exception {

		Assume.assumeTrue( isLinuxSystem());
		copyResources( "/BashTemplate" );

		File file = new File( OUTPUT_DIR, "BashTemplateFile.stop" );
		Assert.assertFalse( file.exists());
		this.plugin.stop( this.inst );
		assertTrue( file.exists());
	}


	@Test
	public void testUndeploy() throws Exception {

		Assume.assumeTrue( isLinuxSystem());
		copyResources( "/BashScript" );

		File file = new File( OUTPUT_DIR, "BashScriptFile.undeploy" );
		Assert.assertFalse( file.exists());
		this.plugin.undeploy( this.inst );
		assertTrue( file.exists());
	}


	@Test( expected = PluginException.class )
	public void testUndeploy_exception() throws Exception {

		Assume.assumeTrue( isLinuxSystem());
		copyResources( "/BashScriptError" );
		this.plugin.undeploy( this.inst );
	}


	@Test
	public void testUndeploy_template() throws Exception {

		Assume.assumeTrue( isLinuxSystem());
		copyResources( "/BashTemplate" );

		File file = new File( OUTPUT_DIR, "BashTemplateFile.undeploy" );
		Assert.assertFalse( file.exists());
		this.plugin.undeploy( this.inst );
		assertTrue( file.exists());
	}


	@Test
	public void testUpdate() throws Exception {

		Assume.assumeTrue( isLinuxSystem());
		copyResources( "/BashScript" );

		File file = new File( OUTPUT_DIR, "BashScriptFile.update" );
		Assert.assertFalse( file.exists());

		Import importChanged = new Import( this.inst );
		InstanceStatus statusChanged = InstanceStatus.DEPLOYED_STARTED;
		this.plugin.update( this.inst, importChanged, statusChanged );

		assertTrue( file.exists());
	}


	@Test( expected = PluginException.class )
	public void testUpdate_exception() throws Exception {

		Assume.assumeTrue( isLinuxSystem());
		copyResources( "/BashScriptError" );

		Import importChanged = new Import( this.inst );
		InstanceStatus statusChanged = InstanceStatus.DEPLOYED_STARTED;
		this.plugin.update( this.inst, importChanged, statusChanged );
	}


	@Test
	public void testUpdate_template() throws Exception {

		Assume.assumeTrue( isLinuxSystem());
		copyResources( "/BashTemplate" );

		File file = new File( OUTPUT_DIR, "BashTemplateFile.update" );
		Assert.assertFalse( file.exists());

		Map<String,String> exports = new HashMap<String,String> ();
		exports.put( "ip", "127.0.0.1" );
		exports.put( "port", "8091" );

		Import importChanged = new Import(
				InstanceHelpers.computeInstancePath( this.inst ),
				this.inst.getComponent().getName(),
				exports );

		InstanceStatus statusChanged = InstanceStatus.DEPLOYED_STARTED;
		this.plugin.update( this.inst, importChanged, statusChanged );

		assertTrue( file.exists());
	}


	@Test
	public void testUpdate_scriptWithExports() throws Exception {

		Assume.assumeTrue( isLinuxSystem());
		copyResources( "/BashScript" );

		File file = new File( OUTPUT_DIR, "BashScriptFile.update" );
		Assert.assertFalse( file.exists());

		Map<String,String> exports = new HashMap<String,String> ();
		exports.put( "ip", "127.0.0.1" );
		exports.put( "port", "8091" );

		Import importChanged = new Import(
				InstanceHelpers.computeInstancePath( this.inst ),
				this.inst.getComponent().getName(),
				exports );

		InstanceStatus statusChanged = InstanceStatus.DEPLOYED_STARTED;
		this.plugin.update( this.inst, importChanged, statusChanged );
		assertTrue( file.exists());
	}


	@Test( expected = PluginException.class )
	public void testInvalidTemplate() throws Exception {

		Assume.assumeTrue( isLinuxSystem());
		copyResources( "/BashTemplateInvalid" );
		this.plugin.deploy( this.inst );
	}


	@Test
	public void testInexistingTemplate() throws Exception {

		Assume.assumeTrue( isLinuxSystem());
		copyResources( "/BashTemplateInvalid" );

		Assert.assertFalse( OUTPUT_DIR.exists());
		this.plugin.start( this.inst );
		Assert.assertFalse( OUTPUT_DIR.exists());
	}


	/**
	 * @return true if it seems to be a Linux system
	 * <p>
	 * Strong assumption to suppose Bash is installed, but whatever...
	 * </p>
	 */
	private boolean isLinuxSystem() {
		return new File( "/tmp" ).exists();
	}


	private void copyResources( String resourcesPath ) throws Exception {
		File toCopy = TestUtils.findTestFile( resourcesPath );
		Utils.copyDirectory( toCopy, this.instanceDirectory);
	}
}
