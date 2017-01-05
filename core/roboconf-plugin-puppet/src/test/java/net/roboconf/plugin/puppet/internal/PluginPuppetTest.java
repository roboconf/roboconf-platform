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

package net.roboconf.plugin.puppet.internal;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.beans.Component;
import net.roboconf.core.model.beans.Import;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.utils.ProgramUtils;
import net.roboconf.core.utils.Utils;
import net.roboconf.plugin.api.PluginException;

/**
 * @author Vincent Zurczak - Linagora
 */
public class PluginPuppetTest {

	private final static File OUTPUT_DIR = new File( "/tmp/roboconf-test-for-puppet" );

	private final Instance inst = new Instance( "sample" )
			.component( new Component( "some-component" )
			.installerName( PluginPuppet.PLUGIN_NAME ));

	private final File instanceDirectory = InstanceHelpers.findInstanceDirectoryOnAgent( this.inst );
	private PluginPuppet plugin;

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
			List<String> command = new ArrayList<> ();
			command.add( "puppet" );
			command.add( "--version" );

			Logger logger = Logger.getLogger( getClass().getName());
			ProgramUtils.executeCommand( logger, command, null, null, null, null);

		} catch( Exception e ) {
			Logger logger = Logger.getLogger( getClass().getName());
			logger.warning( "Tests are skipped because Puppet is not installed." );
			Utils.logException( logger, e );

			this.running = false;
			Assume.assumeNoException( e );
		}
	}


	@Before
	public void resetPlugin() throws Exception {
		this.plugin = new PluginPuppet();
		this.plugin.setNames( "app", "test" );
		Utils.createDirectory( OUTPUT_DIR );
	}


	@After
	public void clearPreviousOutputs() throws Exception {
		Utils.deleteFilesRecursively( this.instanceDirectory );
		Utils.deleteFilesRecursively( OUTPUT_DIR );
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
	public void testInitialize_withVersion() throws Exception {

		Assume.assumeTrue( this.running );
		copyResources( "/with-version" );

		File[] subFiles = this.instanceDirectory.listFiles();
		Assert.assertNotNull( subFiles );
		Assert.assertEquals( 1, subFiles.length );

		File moduleDirectory = new File( this.instanceDirectory, "sysctl" );
		Assert.assertFalse( moduleDirectory.exists());

		this.plugin.initialize( this.inst );

		subFiles = this.instanceDirectory.listFiles();
		Assert.assertNotNull( subFiles );
		Assert.assertEquals( 2, subFiles.length );

		Assert.assertTrue( moduleDirectory.exists());
		Assert.assertTrue( moduleDirectory.isDirectory());
	}


	@Test
	public void testInitialize_withoutVersion() throws Exception {

		Assume.assumeTrue( this.running );
		copyResources( "/without-version" );

		File[] moduleDirectories = new File[ 2 ];
		moduleDirectories[ 0 ] = new File( this.instanceDirectory, "sysctl" );
		moduleDirectories[ 1 ] = new File( this.instanceDirectory, "redis" );

		for( File f : moduleDirectories )
			Assert.assertFalse( f.getAbsolutePath(), f.exists());

		this.plugin.initialize( this.inst );

		for( File f : moduleDirectories ) {
			Assert.assertTrue( f.getAbsolutePath(), f.exists());
			Assert.assertTrue( f.getAbsolutePath(), f.isDirectory());
		}
	}


	@Test
	public void testInitialize_inexistingDirectory() throws Exception {

		Assume.assumeTrue( this.running );
		Utils.deleteFilesRecursively( this.instanceDirectory );

		this.plugin.initialize( this.inst );
		Assert.assertFalse( this.instanceDirectory.exists());
	}


	@Test( expected = PluginException.class )
	public void testInitialize_withInvalidModule() throws Exception {

		Assume.assumeTrue( this.running );
		copyResources( "/with-invalid-module" );
		this.plugin.initialize( this.inst );
	}


	@Test
	public void testPuppetPlugin_withInit_deploy() throws Exception {

		Assume.assumeTrue( isLinuxSystem());
		copyResources( "/with-init" );

		this.plugin.deploy( this.inst );
		checkGeneratedFiles( "WithInit", "stopped" );
	}


	@Test
	public void testPuppetPlugin_withInit_start() throws Exception {

		Assume.assumeTrue( isLinuxSystem());
		copyResources( "/with-init" );

		this.plugin.start( this.inst );
		checkGeneratedFiles( "WithInit", "running" );
	}


	@Test
	public void testPuppetPlugin_withInit_stop() throws Exception {

		Assume.assumeTrue( isLinuxSystem());
		copyResources( "/with-init" );

		this.plugin.stop( this.inst );
		checkGeneratedFiles( "WithInit", "stopped" );
	}


	@Test
	public void testPuppetPlugin_withInit_undeploy() throws Exception {

		Assume.assumeTrue( isLinuxSystem());
		copyResources( "/with-init" );

		this.plugin.undeploy( this.inst );
		checkGeneratedFiles( "WithInit", "" );	// UNDEF
	}


	@Test
	public void testPuppetPlugin_withInit_update() throws Exception {

		Assume.assumeTrue( isLinuxSystem());
		copyResources( "/with-init" );

		Map<String,String> variables = new HashMap<> ();
		variables.put( "ip", "127.0.0.1" );

		this.plugin.update(
				this.inst,
				new Import( "/some/path", "component1", variables ),
				InstanceStatus.DEPLOYED_STARTED );

		checkGeneratedFiles( "WithInit", "" );	// UNDEF
	}


	@Test
	public void testPuppetPlugin_withOperations_deploy() throws Exception {

		Assume.assumeTrue( isLinuxSystem());
		copyResources( "/with-operations" );

		this.plugin.deploy( this.inst );
		checkGeneratedFiles( "WithOperations", "deploy" );
	}


	@Test
	public void testPuppetPlugin_withOperations_start() throws Exception {

		Assume.assumeTrue( isLinuxSystem());
		copyResources( "/with-operations" );

		this.plugin.start( this.inst );
		checkGeneratedFiles( "WithOperations", "start" );
	}


	@Test
	public void testPuppetPlugin_withOperations_stop() throws Exception {

		Assume.assumeTrue( isLinuxSystem());
		copyResources( "/with-operations" );

		this.plugin.stop( this.inst );
		checkGeneratedFiles( "WithOperations", "stop" );
	}


	@Test
	public void testPuppetPlugin_withOperations_undeploy() throws Exception {

		Assume.assumeTrue( isLinuxSystem());
		copyResources( "/with-operations" );

		this.plugin.undeploy( this.inst );
		checkGeneratedFiles( "WithOperations", "undeploy" );
	}


	@Test
	public void testPuppetPlugin_withOperations_update() throws Exception {

		Assume.assumeTrue( isLinuxSystem());
		copyResources( "/with-operations" );

		Map<String,String> variables = new HashMap<> ();
		variables.put( "ip", "127.0.0.1" );

		this.plugin.update(
				this.inst,
				new Import( "/some/path", "component1", variables ),
				InstanceStatus.DEPLOYED_STOPPED );

		checkGeneratedFiles( "WithOperations", "update" );
	}


	@Test( expected = PluginException.class )
	public void testPuppetPlugin_withInit_exception_deploy() throws Exception {

		Assume.assumeTrue( isLinuxSystem());
		copyResources( "/with-init-invalid" );
		this.plugin.deploy( this.inst );
	}


	@Test( expected = PluginException.class )
	public void testPuppetPlugin_withInit_exception_start() throws Exception {

		Assume.assumeTrue( isLinuxSystem());
		copyResources( "/with-init-invalid" );
		this.plugin.start( this.inst );
	}


	@Test( expected = PluginException.class )
	public void testPuppetPlugin_withInit_exception_stop() throws Exception {

		Assume.assumeTrue( isLinuxSystem());
		copyResources( "/with-init-invalid" );
		this.plugin.stop( this.inst );
	}


	@Test( expected = PluginException.class )
	public void testPuppetPlugin_withInit_exception_undeploy() throws Exception {

		Assume.assumeTrue( isLinuxSystem());
		copyResources( "/with-init-invalid" );
		this.plugin.undeploy( this.inst );
	}


	@Test( expected = PluginException.class )
	public void testPuppetPlugin_withInit_exception_update() throws Exception {

		Assume.assumeTrue( isLinuxSystem());
		copyResources( "/with-init-invalid" );

		Map<String,String> variables = new HashMap<> ();
		variables.put( "ip", "127.0.0.1" );

		this.plugin.update(
				this.inst,
				new Import( "/some/path", "component1", variables ),
				InstanceStatus.DEPLOYED_STARTED );
	}


	@Test( expected = PluginException.class )
	public void testNoModuleDirectory() throws Exception {

		Assume.assumeTrue( isLinuxSystem());
		Assert.assertTrue( new File( this.instanceDirectory, "whatever" ).mkdirs());
		Assert.assertTrue( new File( this.instanceDirectory, "whatever.txt" ).createNewFile());
		this.plugin.undeploy( this.inst );
	}


	@Test
	public void testNoScriptToExecute() throws Exception {

		Assume.assumeTrue( isLinuxSystem());
		Assert.assertTrue( new File( this.instanceDirectory, "roboconf_empty_puppet_module" ).mkdirs());
		this.plugin.undeploy( this.inst );
		// No error, the execution is skipped
	}


	@Test( expected = PluginException.class )
	public void testPuppetPlugin_withInit_nonZeroCode_deploy() throws Exception {

		Assume.assumeTrue( isLinuxSystem());
		copyResources( "/with-init-exit-1" );
		this.plugin.deploy( this.inst );
	}


	@Test( expected = PluginException.class )
	public void testPuppetPlugin_withInit_nonZeroCode_start() throws Exception {

		Assume.assumeTrue( isLinuxSystem());
		copyResources( "/with-init-exit-1" );
		this.plugin.start( this.inst );
	}


	@Test( expected = PluginException.class )
	public void testPuppetPlugin_withInit_nonZeroCode_stop() throws Exception {

		Assume.assumeTrue( isLinuxSystem());
		copyResources( "/with-init-exit-1" );
		this.plugin.stop( this.inst );
	}


	@Test( expected = PluginException.class )
	public void testPuppetPlugin_withInit_nonZeroCode_undeploy() throws Exception {

		Assume.assumeTrue( isLinuxSystem());
		copyResources( "/with-init-exit-1" );
		this.plugin.undeploy( this.inst );
	}


	@Test( expected = PluginException.class )
	public void testPuppetPlugin_withInit_nonZeroCode_update() throws Exception {

		Assume.assumeTrue( isLinuxSystem());
		copyResources( "/with-init-exit-1" );

		Map<String,String> variables = new HashMap<> ();
		variables.put( "ip", "127.0.0.1" );

		this.plugin.update(
				this.inst,
				new Import( "/some/path", "component1", variables ),
				InstanceStatus.DEPLOYED_STARTED );
	}


	@Test
	public void testPuppetPlugin_withInit_nonZeroCode_changesAndErrors() throws Exception {

		Assume.assumeTrue( isLinuxSystem());
		copyResources( "/with-init-changes-and-errors" );
		this.plugin.undeploy( this.inst );
	}


	private void checkGeneratedFiles( String prefix, String suffix ) {

		File fromTpl = new File( OUTPUT_DIR, prefix + ".tpl." + suffix );
		Assert.assertTrue( fromTpl.exists());
		Assert.assertTrue( fromTpl.length() > 0 );

		File fromStatic = new File( OUTPUT_DIR, prefix + ".file." + suffix );
		Assert.assertTrue( fromStatic.exists());
		Assert.assertTrue( fromStatic.length() > 0 );

		File[] subFiles = OUTPUT_DIR.listFiles();
		Assert.assertNotNull( subFiles );
		Assert.assertEquals( 2, subFiles.length );
	}


	private void copyResources( String resourcesPath ) throws Exception {
		File toCopy = TestUtils.findTestFile( resourcesPath );
		Utils.copyDirectory( toCopy, this.instanceDirectory);
	}


	private boolean isLinuxSystem() {
		return new File( "/tmp" ).exists();
	}
}
