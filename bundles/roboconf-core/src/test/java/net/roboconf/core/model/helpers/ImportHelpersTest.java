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
import net.roboconf.core.model.runtime.Component;
import net.roboconf.core.model.runtime.Import;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.core.model.runtime.Instance.InstanceStatus;

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ImportHelpersTest {

	@Test
	public void testHasAllRequiredImports_optional() throws Exception {

		Component clusterNodeComponent = new Component( "cluster" ).alias( "a cluster node" ).installerName( "whatever" );
		clusterNodeComponent.getImportedVariables().put( "cluster.ip", Boolean.TRUE );
		clusterNodeComponent.getImportedVariables().put( "cluster.port", Boolean.TRUE );
		clusterNodeComponent.getExportedVariables().put( "cluster.ip", null );
		clusterNodeComponent.getExportedVariables().put( "cluster.port", "9007" );

		Instance i1 = new Instance( "inst 1" ).component( clusterNodeComponent );
		i1.getExports().put( "cluster.ip", "192.168.1.15" );
		i1.setStatus( InstanceStatus.STARTING );

		Instance i2 = new Instance( "inst 2" ).component( clusterNodeComponent );
		i2.getExports().put( "cluster.ip", "192.168.1.28" );

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

		Component dbComponent = new Component( "database" ).alias( "a database" ).installerName( "whatever" );
		dbComponent.getExportedVariables().put( "database.ip", null );
		dbComponent.getExportedVariables().put( "database.port", "3009" );
		dbComponent.getExportedVariables().put( "database.collection", "whatever" );

		Component appServerComponent = new Component( "app-server" ).alias( "an application server" ).installerName( "whatever" );
		appServerComponent.getExportedVariables().put( "app-server.ip", null );
		appServerComponent.getExportedVariables().put( "app-server.port", "8009" );
		appServerComponent.getImportedVariables().put( "database.ip", Boolean.FALSE );
		appServerComponent.getImportedVariables().put( "database.port", Boolean.FALSE );
		appServerComponent.getImportedVariables().put( "database.collection", Boolean.TRUE );

		Instance appServer = new Instance( "app server" ).component( appServerComponent );
		appServer.getExports().put( "app-server.ip", "192.168.1.15" );
		appServer.setStatus( InstanceStatus.STARTING );

		Instance database = new Instance( "database" ).component( dbComponent );
		database.getExports().put( "database.ip", "192.168.1.28" );

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
				new Import( "/root1" ),
				new Import( "/root2" )));

		Instance inst = new Instance( "inst" );
		Assert.assertEquals( 0, inst.getImports().size());

		ImportHelpers.updateImports( inst, prefixToImports );
		Assert.assertEquals( 1, inst.getImports().size());

		Iterator<Import> iterator = inst.getImports().get( "comp" ).iterator();
		Assert.assertEquals( "/root1", iterator.next().getInstancePath());
		Assert.assertEquals( "/root2", iterator.next().getInstancePath());
		Assert.assertFalse( iterator.hasNext());

		prefixToImports.put( "comp", Arrays.asList( new Import( "/root1" )));
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
				new Import( "/root1" ),
				new Import( "/root2" ))));

		Instance inst = new Instance( "inst" );
		inst.getImports().putAll( prefixToImports );
		Assert.assertEquals( 1, inst.getImports().keySet().size());
		Assert.assertTrue( inst.getImports().keySet().contains( "comp" ));

		ImportHelpers.addImport( inst, "wow", new Import( "/root" ));
		Assert.assertEquals( 2, inst.getImports().keySet().size());
		Assert.assertTrue( inst.getImports().keySet().contains( "comp" ));
		Assert.assertTrue( inst.getImports().keySet().contains( "wow" ));

		Assert.assertEquals( 2, inst.getImports().get( "comp" ).size());
		ImportHelpers.addImport( inst, "comp", new Import( "/root3" ));
		Assert.assertEquals( 3, inst.getImports().get( "comp" ).size());
		Assert.assertEquals( 2, inst.getImports().keySet().size());

		// We cannot insert the same import twice
		ImportHelpers.addImport( inst, "comp", new Import( "/root3" ));
		Assert.assertEquals( 3, inst.getImports().get( "comp" ).size());
		Assert.assertEquals( 2, inst.getImports().keySet().size());
	}


	@Test
	public void testBuildTailoredImport() {

		String instancePath = "/whatever/this/is/a-test";
		Component comp = new Component( "comp" );
		comp.getImportedVariables().put( "comp1.port", Boolean.FALSE );
		comp.getImportedVariables().put( "comp1.ip", Boolean.FALSE );
		comp.getImportedVariables().put( "comp2.option", Boolean.TRUE );

		Instance inst = new Instance( "inst" ).component( comp );

		// Null map
		Import imp = ImportHelpers.buildTailoredImport( inst, instancePath, null );
		Assert.assertEquals( instancePath, imp.getInstancePath());
		Assert.assertEquals( 0, imp.getExportedVars().size());

		// Empty map
		imp = ImportHelpers.buildTailoredImport( inst, instancePath, new HashMap<String,String> ());
		Assert.assertEquals( 0, imp.getExportedVars().size());

		// Map with various variable
		Map<String,String> map = new HashMap<String,String> ();
		map.put( "comp1.ip", "127.0.0.1" );
		map.put( "comp2.option", "ciao!" );
		map.put( "not.a.valid.variable", "yeah" );
		map.put( null, "null" );
		map.put( "", "hop" );

		imp = ImportHelpers.buildTailoredImport( inst, instancePath, map );
		Assert.assertEquals( instancePath, imp.getInstancePath());
		Assert.assertEquals( 2, imp.getExportedVars().size());
		Assert.assertEquals( "127.0.0.1", imp.getExportedVars().get( "comp1.ip" ));
		Assert.assertEquals( "ciao!", imp.getExportedVars().get( "comp2.option" ));
	}


	@Test
	public void testFindImportByExportingInstance() {

		Collection<Import> imports = new HashSet<Import> ();
		imports.add( new Import( "/some/path" ));
		imports.add( new Import( "/some/path/deeper" ));
		imports.add( new Import( "/some/other-path" ));

		Assert.assertNull( ImportHelpers.findImportByExportingInstance( null, null ));
		Assert.assertNull( ImportHelpers.findImportByExportingInstance( null, "/some/path" ));
		Assert.assertNull( ImportHelpers.findImportByExportingInstance( imports, null ));
		Assert.assertNull( ImportHelpers.findImportByExportingInstance( imports, "/wrong/path" ));
		Assert.assertEquals( "/some/path", ImportHelpers.findImportByExportingInstance( imports, "/some/path" ).getInstancePath());
		Assert.assertEquals( "/some/path/deeper", ImportHelpers.findImportByExportingInstance( imports, "/some/path/deeper" ).getInstancePath());
	}
}
