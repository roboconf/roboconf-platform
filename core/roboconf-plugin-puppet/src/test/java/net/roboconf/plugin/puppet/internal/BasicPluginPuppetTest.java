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
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import net.roboconf.core.model.beans.Component;
import net.roboconf.core.model.beans.Import;
import net.roboconf.core.model.beans.ImportedVariable;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.plugin.puppet.internal.PluginPuppet.PuppetState;

/**
 * @author Vincent Zurczak - Linagora
 */
public class BasicPluginPuppetTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();
	final PluginPuppet plugin = new PluginPuppet();



	@Test
	public void testFormatExportedVariables() {

		Map<String,String> exports = new LinkedHashMap<> ();
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
		component.addImportedVariable( new ImportedVariable( "MySQL.port", false, false ));
		Instance instance = new Instance( "test" ).component( component );

		Assert.assertEquals( "mysql => undef", this.plugin.formatInstanceImports( instance ));
	}


	@Test
	public void testFormatInstanceImports_emptyImport() {

		// We should never have this use case, but still, we do not want to get something wrong
		Component component = new Component( "test-component" );
		component.addImportedVariable( new ImportedVariable( "MySQL.port", false, false ));

		Instance instance = new Instance( "test" ).component( component );
		instance.getImports().put( "MySQL", new ArrayList<Import>( 0 ));
		// We have a variable prefix, but no real associated import (empty collection)

		Assert.assertEquals( "mysql => undef", this.plugin.formatInstanceImports( instance ));
	}


	@Test
	public void testFormatInstanceImports_oneImportOneVariable() {

		Map<String,String> exports = new LinkedHashMap<> ();
		exports.put( "MySQL.port", "3306" );
		Import imp = new Import( "/toto", "component1", exports );

		Component component = new Component( "test-component" );
		component.addImportedVariable( new ImportedVariable( "MySQL.port", false, false ));

		Instance instance = new Instance( "test" ).component( component );
		instance.getImports().put( "MySQL", Arrays.asList( imp ));

		Assert.assertEquals(
				"mysql => { '/toto' => { port => '3306' }}",
				this.plugin.formatInstanceImports( instance ));
	}


	@Test
	public void testFormatInstanceImports_oneImportTwoVariables() {

		Map<String,String> exports = new HashMap<> ();
		exports.put( "MySQL.port", "3306" );
		exports.put( "MySQL.ip", "172.16.20.12" );
		Import imp = new Import( "/toto", "component1", exports );

		Component component = new Component( "test-component" );
		component.addImportedVariable( new ImportedVariable( "MySQL.port", false, false ));
		component.addImportedVariable( new ImportedVariable( "MySQL.ip", false, false ));

		Instance instance = new Instance( "test" ).component( component );
		instance.getImports().put( "MySQL", Arrays.asList( imp ));

		String expected1 = "mysql => { '/toto' => { port => '3306', ip => '172.16.20.12' }}";
		String expected2 = "mysql => { '/toto' => { ip => '172.16.20.12', port => '3306' }}";
		String result = this.plugin.formatInstanceImports( instance );

		Assert.assertTrue( expected1.equals( result ) || expected2.equals( result ));
	}


	@Test
	public void testFormatInstanceImports_twoImportsTwoVariables() {

		List<Import> mySqlImports = new ArrayList<> ();
		for( int i=0; i<2; i++ ) {
			Map<String,String> exports = new LinkedHashMap<> ();
			exports.put( "MySQL.port", String.valueOf( 3306 + i ));
			exports.put( "MySQL.ip", "172.16.20." + String.valueOf( 12 + i ));
			mySqlImports.add( new Import( "/toto-" + i, "component1", exports ));
		}

		List<Import> somethingImports = new ArrayList<> ();
		Map<String,String> exports = new LinkedHashMap<> ();
		exports.put( "Something.test", "true" );
		somethingImports.add( new Import( "/oops", "component2", exports ));

		Component component = new Component( "test-component" );
		component.addImportedVariable( new ImportedVariable( "MySQL.port", false, false ));
		component.addImportedVariable( new ImportedVariable( "MySQL.ip", false, false ));
		component.addImportedVariable( new ImportedVariable( "Something.test", false, false ));

		Instance instance = new Instance( "test" ).component( component );
		instance.getImports().put( "MySQL", mySqlImports );
		instance.getImports().put( "Something", somethingImports );

		String expected1 =
				"mysql => { '/toto-0' => { port => '3306', ip => '172.16.20.12' }, '/toto-1' => { port => '3307', ip => '172.16.20.13' }},"
				+ " something => { '/oops' => { test => 'true' }}";

		String expected2 =
				"mysql => { '/toto-0' => { ip => '172.16.20.12', port => '3306' }, '/toto-1' => { ip => '172.16.20.13', port => '3307' }},"
				+ " something => { '/oops' => { test => 'true' }}";

		String result = this.plugin.formatInstanceImports( instance );
		Assert.assertTrue( expected1.equals( result ) || expected2.equals( result ));
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
		Assert.assertTrue( s.endsWith( ", component => some component}}" ));

		// And when the component is null
		imp = new Import( "/vm/sth", null );
		s = this.plugin.generateCodeToExecute( "roboconf_test-component", instance, PuppetState.RUNNING, imp, false );
		Assert.assertTrue( s.endsWith( ", component => undef}}" ));
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
