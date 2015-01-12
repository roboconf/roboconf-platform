/**
 * Copyright 2014-2015 Linagora, Université Joseph Fourier, Floralis
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

import junit.framework.Assert;
import net.roboconf.core.model.beans.Component;
import net.roboconf.core.model.beans.Import;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ImportHelpersTest {

	@Test
	public void testHasAllRequiredImports_optional() throws Exception {

		Component clusterNodeComponent = new Component( "cluster" ).installerName( "whatever" );
		clusterNodeComponent.importedVariables.put( "cluster.ip", Boolean.TRUE );
		clusterNodeComponent.importedVariables.put( "cluster.port", Boolean.TRUE );
		clusterNodeComponent.exportedVariables.put( "cluster.ip", null );
		clusterNodeComponent.exportedVariables.put( "cluster.port", "9007" );

		Instance i1 = new Instance( "inst 1" ).component( clusterNodeComponent );
		i1.overriddenExports.put( "cluster.ip", "192.168.1.15" );
		i1.setStatus( InstanceStatus.STARTING );

		Instance i2 = new Instance( "inst 2" ).component( clusterNodeComponent );
		i2.overriddenExports.put( "cluster.ip", "192.168.1.28" );

		// The cluster node does not know about the other node
		Assert.assertTrue( ImportHelpers.hasAllRequiredImports( i1, null ));
		Assert.assertTrue( ImportHelpers.hasAllRequiredImports( i2, null ));

		// The node is now aware of another node
		ImportHelpers.addImport( i1, "cluster", new Import( i2 ));
		i1.setStatus( InstanceStatus.STARTING );
		Assert.assertTrue( ImportHelpers.hasAllRequiredImports( i1, null ));
		Assert.assertTrue( ImportHelpers.hasAllRequiredImports( i2, null ));

		i1.getImports().clear();
		Assert.assertTrue( ImportHelpers.hasAllRequiredImports( i1, null ));
		Assert.assertTrue( ImportHelpers.hasAllRequiredImports( i2, null ));
	}


	@Test
	public void testHasAllRequiredImports_required() throws Exception {

		Component dbComponent = new Component( "database" ).installerName( "whatever" );
		dbComponent.exportedVariables.put( "database.ip", null );
		dbComponent.exportedVariables.put( "database.port", "3009" );
		dbComponent.exportedVariables.put( "database.collection", "whatever" );

		Component appServerComponent = new Component( "app-server" ).installerName( "whatever" );
		appServerComponent.exportedVariables.put( "app-server.ip", null );
		appServerComponent.exportedVariables.put( "app-server.port", "8009" );
		appServerComponent.importedVariables.put( "database.ip", Boolean.FALSE );
		appServerComponent.importedVariables.put( "database.port", Boolean.FALSE );
		appServerComponent.importedVariables.put( "database.collection", Boolean.TRUE );

		Instance appServer = new Instance( "app server" ).component( appServerComponent );
		appServer.overriddenExports.put( "app-server.ip", "192.168.1.15" );
		appServer.setStatus( InstanceStatus.STARTING );

		Instance database = new Instance( "database" ).component( dbComponent );
		database.overriddenExports.put( "database.ip", "192.168.1.28" );

		// The application server does not know about the database
		Assert.assertFalse( ImportHelpers.hasAllRequiredImports( appServer, Logger.getAnonymousLogger()));

		// The application server is now aware of the database
		ImportHelpers.addImport( appServer, "database", new Import( database ));
		Assert.assertTrue( ImportHelpers.hasAllRequiredImports( appServer, null ));

		appServer.getImports().clear();
		Assert.assertFalse( ImportHelpers.hasAllRequiredImports( appServer, null ));
	}


	@Test
	public void testHasAllRequiredImports_withWildcard() throws Exception {

		// Same test than "testHasAllRequiredImports_required"
		// but here, variable imports use a wild card.
		Component dbComponent = new Component( "database" ).installerName( "whatever" );
		dbComponent.exportedVariables.put( "database.ip", null );
		dbComponent.exportedVariables.put( "database.port", "3009" );
		dbComponent.exportedVariables.put( "database.collection", "whatever" );

		Component appServerComponent = new Component( "app-server" ).installerName( "whatever" );
		appServerComponent.exportedVariables.put( "app-server.ip", null );
		appServerComponent.exportedVariables.put( "app-server.port", "8009" );
		appServerComponent.importedVariables.put( "database.*", Boolean.FALSE );

		Instance appServer = new Instance( "app server" ).component( appServerComponent );
		appServer.overriddenExports.put( "app-server.ip", "192.168.1.15" );
		appServer.setStatus( InstanceStatus.STARTING );

		Instance database = new Instance( "database" ).component( dbComponent );
		database.overriddenExports.put( "database.ip", "192.168.1.28" );

		// The application server does not know about the database
		Assert.assertFalse( ImportHelpers.hasAllRequiredImports( appServer, Logger.getAnonymousLogger()));

		// The application server is now aware of the database
		ImportHelpers.addImport( appServer, "database", new Import( database ));
		Assert.assertTrue( ImportHelpers.hasAllRequiredImports( appServer, null ));

		appServer.getImports().clear();
		Assert.assertFalse( ImportHelpers.hasAllRequiredImports( appServer, null ));
	}


	@Test
	public void testUpdateImports() {

		Map<String,Collection<Import>> prefixToImports = new HashMap<String,Collection<Import>> ();
		prefixToImports.put( "comp", Arrays.asList(
				new Import( "/root1", "comp1" ),
				new Import( "/root2", "comp1" )));

		Instance inst = new Instance( "inst" );
		Assert.assertEquals( 0, inst.getImports().size());

		ImportHelpers.updateImports( inst, prefixToImports );
		Assert.assertEquals( 1, inst.getImports().size());

		Iterator<Import> iterator = inst.getImports().get( "comp" ).iterator();
		Assert.assertEquals( "/root1", iterator.next().getInstancePath());
		Assert.assertEquals( "/root2", iterator.next().getInstancePath());
		Assert.assertFalse( iterator.hasNext());

		prefixToImports.put( "comp", Arrays.asList( new Import( "/root1", "comp1" )));
		ImportHelpers.updateImports( inst, prefixToImports );
		Assert.assertEquals( 1, inst.getImports().size());

		iterator = inst.getImports().get( "comp" ).iterator();
		Assert.assertEquals( "/root1", iterator.next().getInstancePath());
		Assert.assertFalse( iterator.hasNext());

		ImportHelpers.updateImports( inst, null );
		Assert.assertEquals( 0, inst.getImports().size());
	}


	@Test
	public void testAddImport() {

		Map<String,Collection<Import>> prefixToImports = new HashMap<String,Collection<Import>> ();
		prefixToImports.put( "comp", new ArrayList<Import>( Arrays.asList(
				new Import( "/root1", "comp1" ),
				new Import( "/root2", "comp1" ))));

		Instance inst = new Instance( "inst" );
		inst.getImports().putAll( prefixToImports );
		Assert.assertEquals( 1, inst.getImports().keySet().size());
		Assert.assertTrue( inst.getImports().keySet().contains( "comp" ));

		ImportHelpers.addImport( inst, "wow", new Import( "/root", "comp1" ));
		Assert.assertEquals( 2, inst.getImports().keySet().size());
		Assert.assertTrue( inst.getImports().keySet().contains( "comp" ));
		Assert.assertTrue( inst.getImports().keySet().contains( "wow" ));

		Assert.assertEquals( 2, inst.getImports().get( "comp" ).size());
		ImportHelpers.addImport( inst, "comp", new Import( "/root3", "comp1" ));
		Assert.assertEquals( 3, inst.getImports().get( "comp" ).size());
		Assert.assertEquals( 2, inst.getImports().keySet().size());

		// We cannot insert the same import twice
		ImportHelpers.addImport( inst, "comp", new Import( "/root3", "comp1" ));
		Assert.assertEquals( 3, inst.getImports().get( "comp" ).size());
		Assert.assertEquals( 2, inst.getImports().keySet().size());
	}


	@Test
	public void testBuildTailoredImport() {

		String instancePath = "/whatever/this/is/a-test";
		Component comp = new Component( "comp" );
		comp.importedVariables.put( "comp1.port", Boolean.FALSE );
		comp.importedVariables.put( "comp1.ip", Boolean.FALSE );
		comp.importedVariables.put( "comp2.option", Boolean.TRUE );

		Instance inst = new Instance( "inst" ).component( comp );

		// Null map
		Import imp = ImportHelpers.buildTailoredImport( inst, instancePath, "comp", null );
		Assert.assertEquals( instancePath, imp.getInstancePath());
		Assert.assertEquals( 0, imp.getExportedVars().size());

		// Empty map
		imp = ImportHelpers.buildTailoredImport( inst, instancePath, "comp", new HashMap<String,String> ());
		Assert.assertEquals( 0, imp.getExportedVars().size());

		// Map with various variable
		Map<String,String> map = new HashMap<String,String> ();
		map.put( "comp1.ip", "127.0.0.1" );
		map.put( "comp2.option", "ciao!" );
		map.put( "not.a.valid.variable", "yeah" );
		map.put( null, "null" );
		map.put( "", "hop" );

		imp = ImportHelpers.buildTailoredImport( inst, instancePath, "comp", map );
		Assert.assertEquals( instancePath, imp.getInstancePath());
		Assert.assertEquals( 2, imp.getExportedVars().size());
		Assert.assertEquals( "comp", imp.getComponentName());
		Assert.assertEquals( "127.0.0.1", imp.getExportedVars().get( "comp1.ip" ));
		Assert.assertEquals( "ciao!", imp.getExportedVars().get( "comp2.option" ));
	}


	@Test
	public void testBuildTailoredImport_withWildcard() {

		String instancePath = "/whatever/this/is/a-test";
		Component comp = new Component( "comp" );
		comp.importedVariables.put( "comp1.*", Boolean.FALSE );
		comp.importedVariables.put( "comp2.option", Boolean.TRUE );

		Instance inst = new Instance( "inst" ).component( comp );

		// Null map
		Import imp = ImportHelpers.buildTailoredImport( inst, instancePath, "comp", null );
		Assert.assertEquals( instancePath, imp.getInstancePath());
		Assert.assertEquals( 0, imp.getExportedVars().size());

		// Empty map
		imp = ImportHelpers.buildTailoredImport( inst, instancePath, "comp", new HashMap<String,String> ());
		Assert.assertEquals( 0, imp.getExportedVars().size());

		// Map with various variable
		Map<String,String> map = new HashMap<String,String> ();
		map.put( "comp1.ip", "127.0.0.1" );
		map.put( "comp2.option", "ciao!" );
		map.put( "not.a.valid.variable", "yeah" );
		map.put( null, "null" );
		map.put( "", "hop" );

		imp = ImportHelpers.buildTailoredImport( inst, instancePath, "comp", map );
		Assert.assertEquals( instancePath, imp.getInstancePath());
		Assert.assertEquals( 2, imp.getExportedVars().size());
		Assert.assertEquals( "comp", imp.getComponentName());
		Assert.assertEquals( "127.0.0.1", imp.getExportedVars().get( "comp1.ip" ));
		Assert.assertEquals( "ciao!", imp.getExportedVars().get( "comp2.option" ));
	}


	@Test
	public void testFindImportByExportingInstance() {

		Collection<Import> imports = new HashSet<Import> ();
		imports.add( new Import( "/some/path", "comp1" ));
		imports.add( new Import( "/some/path/deeper", "comp1" ));
		imports.add( new Import( "/some/other-path", "comp1" ));

		Assert.assertNull( ImportHelpers.findImportByExportingInstance( null, null ));
		Assert.assertNull( ImportHelpers.findImportByExportingInstance( null, "/some/path" ));
		Assert.assertNull( ImportHelpers.findImportByExportingInstance( imports, null ));
		Assert.assertNull( ImportHelpers.findImportByExportingInstance( imports, "/wrong/path" ));
		Assert.assertEquals(
				"/some/path",
				ImportHelpers.findImportByExportingInstance( imports, "/some/path" ).getInstancePath());

		Assert.assertEquals(
				"/some/path/deeper",
				ImportHelpers.findImportByExportingInstance( imports, "/some/path/deeper" ).getInstancePath());
	}
}
