/**
 * Copyright 2013-2014 Linagora, Université Joseph Fourier, Floralis
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
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;
import net.roboconf.core.model.beans.Component;
import net.roboconf.core.model.beans.Import;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.plugin.puppet.internal.PluginPuppet.PuppetState;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author Vincent Zurczak - Linagora
 */
public class BasicPluginPuppetTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();
	final PluginPuppet plugin = new PluginPuppet();



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
		component.importedVariables.put( "MySQL.port", false );
		Instance instance = new Instance( "test" ).component( component );

		Assert.assertEquals( "mysql => undef", this.plugin.formatInstanceImports( instance ));
	}


	@Test
	public void testFormatInstanceImports_emptyImport() {

		// We should never have this use case, but still, we do not want to get something wrong
		Map<String,String> exports = new LinkedHashMap<String,String> ();
		exports.put( "MySQL.port", "3306" );

		Component component = new Component( "test-component" );
		component.importedVariables.put( "MySQL.port", false );

		Instance instance = new Instance( "test" ).component( component );
		instance.getImports().put( "MySQL", new ArrayList<Import>( 0 ));
		// We have a variable prefix, but no real associated import (empty collection)

		Assert.assertEquals( "mysql => undef", this.plugin.formatInstanceImports( instance ));
	}


	@Test
	public void testFormatInstanceImports_oneImportOneVariable() {

		Map<String,String> exports = new LinkedHashMap<String,String> ();
		exports.put( "MySQL.port", "3306" );
		Import imp = new Import( "/toto", "component1", exports );

		Component component = new Component( "test-component" );
		component.importedVariables.put( "MySQL.port", false );

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
		Import imp = new Import( "/toto", "component1", exports );

		Component component = new Component( "test-component" );
		component.importedVariables.put( "MySQL.port", false );
		component.importedVariables.put( "MySQL.ip", false );

		Instance instance = new Instance( "test" ).component( component );
		instance.getImports().put( "MySQL", Arrays.asList( imp ));

		Assert.assertEquals(
				"mysql => { '/toto' => { port => '3306', ip => '172.16.20.12' }}",
				this.plugin.formatInstanceImports( instance ));
	}


	@Test
	public void testFormatInstanceImports_twoImportsTwoVariables() {

		List<Import> mySqlImports = new ArrayList<Import> ();
		for( int i=0; i<2; i++ ) {
			Map<String,String> exports = new LinkedHashMap<String,String> ();
			exports.put( "MySQL.port", String.valueOf( 3306 + i ));
			exports.put( "MySQL.ip", "172.16.20." + String.valueOf( 12 + i ));
			mySqlImports.add( new Import( "/toto-" + i, "component1", exports ));
		}

		List<Import> somethingImports = new ArrayList<Import> ();
		Map<String,String> exports = new LinkedHashMap<String,String> ();
		exports.put( "Something.test", "true" );
		somethingImports.add( new Import( "/oops", "component2", exports ));

		Component component = new Component( "test-component" );
		component.importedVariables.put( "MySQL.port", false );
		component.importedVariables.put( "MySQL.ip", false );
		component.importedVariables.put( "Something.test", false );

		Instance instance = new Instance( "test" ).component( component );
		instance.getImports().put( "MySQL", mySqlImports );
		instance.getImports().put( "Something", somethingImports );

		String expected =
				"mysql => { '/toto-0' => { port => '3306', ip => '172.16.20.12' }, '/toto-1' => { port => '3307', ip => '172.16.20.13' }},"
				+ " something => { '/oops' => { test => 'true' }}";

		Assert.assertEquals( expected, this.plugin.formatInstanceImports( instance ));
	}


	@Test
	public void testPuppetState() {
		for( PuppetState state : PuppetState.values())
			Assert.assertEquals( state.toString(), state.toString().toLowerCase(), state.toString());
	}


	@Test
	public void testGenerateCodeToExecute() {

		Instance instance = new Instance( "test" ).component( new Component( "test-component" ));
		String expectedPrefix = "class{'roboconf_test-component': runningState => ";

		// Try all the states
		for( PuppetState state : PuppetState.values()) {
			String s = this.plugin.generateCodeToExecute( "roboconf_test-component", instance, state, null, false );
			Assert.assertTrue( state.toString(), s.startsWith( expectedPrefix + state.toString()));
		}

		// Try with the changed import
		Import imp = new Import( "/vm/sth", "some component" );
		String s = this.plugin.generateCodeToExecute( "roboconf_test-component", instance, PuppetState.RUNNING, imp, false );
		Assert.assertTrue( s.endsWith( ", importComponent => some component}" ));

		// And when the component is null
		imp = new Import( "/vm/sth", null );
		s = this.plugin.generateCodeToExecute( "roboconf_test-component", instance, PuppetState.RUNNING, imp, false );
		Assert.assertTrue( s.endsWith( ", importComponent => undef}" ));
	}


	@Test
	public void testCallPuppetScriptBasics() throws Exception {

		// Check that invalid parameters are skipped
		this.plugin.callPuppetScript( null, "deploy", PuppetState.STOPPED, null, false, new File( "whatever" ));
		this.plugin.callPuppetScript( new Instance( "inst" ), "deploy", PuppetState.STOPPED, null, false, null );
		this.plugin.callPuppetScript( new Instance( "inst" ), "deploy", PuppetState.STOPPED, null, false, new File( "whatever" ));

		File f = this.folder.newFile();
		this.plugin.callPuppetScript( new Instance( "inst" ), "deploy", PuppetState.STOPPED, null, false, f );
	}
}
