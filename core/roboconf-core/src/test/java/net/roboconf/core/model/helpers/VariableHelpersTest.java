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

package net.roboconf.core.model.helpers;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.model.beans.Component;
import net.roboconf.core.model.beans.ExportedVariable;
import net.roboconf.core.model.beans.ExportedVariable.RandomKind;
import net.roboconf.core.model.beans.Facet;
import net.roboconf.core.model.beans.ImportedVariable;
import net.roboconf.core.model.beans.Instance;

/**
 * @author Vincent Zurczak - Linagora
 */
public class VariableHelpersTest {

	@Test
	public void testParseVariableName() {

		String[][] values = {
				{ "facet", "variable" },
				{ "FacetName", "complex.variable.name" },
				{ "facet-Name", "" },
				{ "", "some_variable" }
		};

		for( String[] value : values ) {
			String s = value[ 0 ] + "." + value[ 1 ];
			Map.Entry<String,String> entry = VariableHelpers.parseVariableName( s );
			Assert.assertEquals( "Invalid component or facet name for " + s, value[ 0 ], entry.getKey());
			Assert.assertEquals( "Invalid simple name for " + s, value[ 1 ], entry.getValue());
		}

		Map.Entry<String,String> entry = VariableHelpers.parseVariableName( "noPrefix" );
		Assert.assertEquals( "noPrefix", entry.getValue());
		Assert.assertEquals( "", entry.getKey());
	}


	@Test
	public void testParseExportedVariable() {

		String[][] values = {
				{ "variableName", "" },
				{ "variableName", "default value" },
				{ "complex.variable.name", "51" },
				{ "", "oops" }
		};

		for( String[] value : values ) {
			String s = value[ 0 ] + " = " + value[ 1 ];
			Map.Entry<String,String> entry = VariableHelpers.parseExportedVariable( s );
			Assert.assertEquals( "Invalid variable name for " + s, value[ 0 ], entry.getKey());
			Assert.assertEquals( "Invalid default value for " + s, value[ 1 ], entry.getValue());
		}
	}


	@Test
	public void testFindPrefixesForExportedVariables_withComponentVariables() {

		Component component = new Component( "comp" );
		component.addExportedVariable( new ExportedVariable( "comp.ip", "" ));
		component.addExportedVariable( new ExportedVariable( "comp.split.property", "" ));
		component.addExportedVariable( new ExportedVariable( "comp.port", "8000" ));

		Instance instance = new Instance( "inst" ).component( component );

		Set<String> prefixes = VariableHelpers.findPrefixesForExportedVariables( instance );
		Assert.assertEquals( 1, prefixes.size());
		Assert.assertTrue( prefixes.contains( "comp" ));

		Facet facet = new Facet( "facet" );
		facet.addExportedVariable( new ExportedVariable( "something", "value" ));
		component.associateFacet( facet );

		prefixes = VariableHelpers.findPrefixesForExportedVariables( instance );
		Assert.assertEquals( 2, prefixes.size());
		Assert.assertTrue( prefixes.contains( "comp" ));
		Assert.assertTrue( prefixes.contains( "facet" ));
	}


	@Test
	public void testFindPrefixesForExportedVariables_withVariable() {

		Instance instance = new Instance( "inst" ).component( new Component( "comp" ));
		Set<String> prefixes = VariableHelpers.findPrefixesForExportedVariables( instance );
		Assert.assertEquals( 0, prefixes.size());
	}


	@Test
	public void testFindPrefixesForExportedVariables_withInstanceVariablesOnly() {

		Instance instance = new Instance( "inst" ).component( new Component( "comp" ));
		instance.overriddenExports.put( "comp.ip", "" );
		instance.overriddenExports.put( "comp.split.property", "" );
		instance.overriddenExports.put( "comp.port", "8000" );
		instance.overriddenExports.put( "facet.desc", "some description" );

		Set<String> prefixes = VariableHelpers.findPrefixesForExportedVariables( instance );
		Assert.assertEquals( 2, prefixes.size());
		Assert.assertTrue( prefixes.contains( "comp" ));
		Assert.assertTrue( prefixes.contains( "facet" ));
	}


	@Test
	public void testFindPrefixesForImportedVariables() {

		Component component = new Component( "comp" );
		component.addImportedVariable( new ImportedVariable( "comp.ip", false, false ));
		component.addImportedVariable( new ImportedVariable( "comp.split.property", false, false ));
		component.addImportedVariable( new ImportedVariable( "comp.port", false, false ));
		component.addImportedVariable( new ImportedVariable( "facet.desc", false, false ));

		Instance instance = new Instance( "inst" ).component( component );

		Set<String> prefixes = VariableHelpers.findPrefixesForImportedVariables( instance );
		Assert.assertEquals( 2, prefixes.size());
		Assert.assertTrue( prefixes.contains( "comp" ));
		Assert.assertTrue( prefixes.contains( "facet" ));

		component.importedVariables.clear();
		prefixes = VariableHelpers.findPrefixesForImportedVariables( instance );
		Assert.assertEquals( 0, prefixes.size());
	}


	@Test
	public void testFindPrefixesForMandatoryImportedVariables() {

		Component component = new Component( "comp" );
		component.addImportedVariable( new ImportedVariable( "comp.ip", false, false ));
		component.addImportedVariable( new ImportedVariable( "comp.split.property", false, false ));
		component.addImportedVariable( new ImportedVariable( "comp.port", false, false ));
		component.addImportedVariable( new ImportedVariable( "facet.desc", true, false ));
		component.addImportedVariable( new ImportedVariable( "facet-n.prop1", true, false ));
		component.addImportedVariable( new ImportedVariable( "facet-n.prop2", false, false ));

		Instance instance = new Instance( "inst" ).component( component );

		Set<String> prefixes = VariableHelpers.findPrefixesForMandatoryImportedVariables( instance );
		Assert.assertEquals( 2, prefixes.size());
		Assert.assertTrue( prefixes.contains( "comp" ));
		Assert.assertTrue( prefixes.contains( "facet-n" ));

		component.importedVariables.clear();
		prefixes = VariableHelpers.findPrefixesForMandatoryImportedVariables( instance );
		Assert.assertEquals( 0, prefixes.size());
	}


	@Test
	public void testUpdateNetworkVariables() {

		Map<String,String> map = new HashMap<> ();
		map.put( "comp.ip", "" );
		map.put( "ip", "" );
		map.put( "not-ip", "" );

		final String ip = "127.0.0.1";
		VariableHelpers.updateNetworkVariables( map, ip );
		Assert.assertEquals( ip, map.get( "comp.ip" ));
		Assert.assertEquals( ip, map.get( "ip" ));
		Assert.assertEquals( "", map.get( "not-ip" ));
	}


	@Test
	public void testFindPrefixesForExternalImports() {

		TestApplication app = new TestApplication();
		ImportedVariable var1 = new ImportedVariable( "something.else", true, true );
		ImportedVariable var2 = new ImportedVariable( "other.stuff", true, true );

		app.getWar().getComponent().importedVariables.put( var1.getName(),  var1 );
		app.getWar().getComponent().importedVariables.put( var2.getName(),  var2 );

		Set<String> prefixes = VariableHelpers.findPrefixesForExternalImports( app );
		Assert.assertEquals( 2, prefixes.size());
		Assert.assertTrue( prefixes.contains( "other" ));
		Assert.assertTrue( prefixes.contains( "something" ));
	}


	@Test
	public void testParseExportedVariables_simpleVariables() {

		Map<String,ExportedVariable> variables = VariableHelpers.parseExportedVariables( "key = value" );
		Assert.assertEquals( 1, variables.size());

		ExportedVariable var = variables.get( "key" );
		Assert.assertEquals( "value", var.getValue());
		Assert.assertFalse( var.isRandom());

		variables = VariableHelpers.parseExportedVariables( "key=value" );
		Assert.assertEquals( 1, variables.size());

		var = variables.get( "key" );
		Assert.assertEquals( "value", var.getValue());
		Assert.assertFalse( var.isRandom());

		variables = VariableHelpers.parseExportedVariables( "" );
		Assert.assertEquals( 0, variables.size());

		variables = VariableHelpers.parseExportedVariables( "    " );
		Assert.assertEquals( 0, variables.size());

		variables = VariableHelpers.parseExportedVariables( "ip" );
		Assert.assertEquals( 1, variables.size());

		var = variables.get( "ip" );
		Assert.assertNull( var.getValue());
		Assert.assertFalse( var.isRandom());
	}


	@Test
	public void testParseExportedVariables_simpleListOfVariables() {

		Map<String,ExportedVariable> variables = VariableHelpers.parseExportedVariables( "key1 = value1, key2=value2 , key3   =  value3  " );
		Assert.assertEquals( 3, variables.size());

		ExportedVariable var = variables.get( "key1" );
		Assert.assertEquals( "value1", var.getValue());
		Assert.assertFalse( var.isRandom());

		var = variables.get( "key2" );
		Assert.assertEquals( "value2", var.getValue());
		Assert.assertFalse( var.isRandom());

		var = variables.get( "key3" );
		Assert.assertEquals( "value3", var.getValue());
		Assert.assertFalse( var.isRandom());
	}


	@Test
	public void testParseExportedVariables_variableWithComplexValue() {

		Map<String,ExportedVariable> variables = VariableHelpers.parseExportedVariables( "key1 = \"value1\"" );
		Assert.assertEquals( 1, variables.size());

		ExportedVariable var = variables.get( "key1" );
		Assert.assertEquals( "value1", var.getValue());
		Assert.assertFalse( var.isRandom());

		variables = VariableHelpers.parseExportedVariables( "  key1=\"  value1  \" " );
		Assert.assertEquals( 1, variables.size());

		var = variables.get( "key1" );
		Assert.assertEquals( "  value1  ", var.getValue());
		Assert.assertFalse( var.isRandom());
	}


	@Test
	public void testParseExportedVariables_listWithMixedValues() {

		Map<String,ExportedVariable> variables = VariableHelpers.parseExportedVariables( "key1 = \"value1 is here\" , key2= value2, key3 = \"key33\", key4 = oops " );
		Assert.assertEquals( 4, variables.size());

		ExportedVariable var = variables.get( "key1" );
		Assert.assertEquals( "value1 is here", var.getValue());
		Assert.assertFalse( var.isRandom());

		var = variables.get( "key2" );
		Assert.assertEquals( "value2", var.getValue());
		Assert.assertFalse( var.isRandom());

		var = variables.get( "key3" );
		Assert.assertEquals( "key33", var.getValue());
		Assert.assertFalse( var.isRandom());

		var = variables.get( "key4" );
		Assert.assertEquals( "oops", var.getValue());
		Assert.assertFalse( var.isRandom());
	}


	@Test
	public void testParseExportedVariables_listWithMixedValuesAndRandom() {

		Map<String,ExportedVariable> variables = VariableHelpers.parseExportedVariables( "key1 = \"value1 is here\" , random[port] key2= value2, key3 = \"key33\", key4 = oops " );
		Assert.assertEquals( 4, variables.size());

		ExportedVariable var = variables.get( "key1" );
		Assert.assertEquals( "value1 is here", var.getValue());
		Assert.assertFalse( var.isRandom());

		var = variables.get( "key2" );
		Assert.assertEquals( "value2", var.getValue());
		Assert.assertTrue( var.isRandom());
		Assert.assertEquals( RandomKind.PORT, var.getRandomKind());

		var = variables.get( "key3" );
		Assert.assertEquals( "key33", var.getValue());
		Assert.assertFalse( var.isRandom());

		var = variables.get( "key4" );
		Assert.assertEquals( "oops", var.getValue());
		Assert.assertFalse( var.isRandom());
	}
}
