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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import net.roboconf.core.Constants;
import net.roboconf.core.internal.tests.ComplexApplicationFactory1;
import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.internal.tests.TestApplicationTemplate;
import net.roboconf.core.model.beans.AbstractType;
import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.core.model.beans.Component;
import net.roboconf.core.model.beans.ExportedVariable;
import net.roboconf.core.model.beans.Facet;
import net.roboconf.core.model.beans.Graphs;
import net.roboconf.core.model.beans.ImportedVariable;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ComponentHelpersTest {

	@Test
	public void testFindComponent() {

		Graphs g = new Graphs();
		Assert.assertNull( ComponentHelpers.findComponent( g, "c" ));

		Component c1 = new Component( "c1" );
		g.getRootComponents().add( c1 );
		Assert.assertEquals( c1, ComponentHelpers.findComponent( g, "c1" ));

		Component c2 = new Component( "c2" );
		g.getRootComponents().add( c2 );
		Assert.assertEquals( c2, ComponentHelpers.findComponent( g, "c2" ));

		Component c21 = new Component( "c21" );
		c2.addChild( c21 );
		Assert.assertEquals( c21, ComponentHelpers.findComponent( g, "c21" ));

		Component duplicateC1 = new Component( "c1" );
		g.getRootComponents().add( duplicateC1 );
		Assert.assertNotNull( ComponentHelpers.findComponent( g, "c1" ));

		TestApplication app = new TestApplication();
		Assert.assertNotNull( ComponentHelpers.findComponent( app, "tomcat" ));
		Assert.assertNull( ComponentHelpers.findComponent( app, "tommy" ));
	}


	@Test
	public void testFindComponentFrom() {

		TestApplicationTemplate app = new TestApplicationTemplate();
		Component comp = app.getTomcatVm().getComponent();

		Assert.assertNull( ComponentHelpers.findComponentFrom( comp, "inexisting" ));
		Assert.assertNull( ComponentHelpers.findComponentFrom( null, "inexisting" ));
		Assert.assertEquals( comp, ComponentHelpers.findComponentFrom( comp, comp.getName()));

		Component targetComp = app.getTomcat().getComponent();
		Assert.assertEquals( targetComp, ComponentHelpers.findComponentFrom( comp, targetComp.getName()));

		targetComp = app.getWar().getComponent();
		Assert.assertEquals( targetComp, ComponentHelpers.findComponentFrom( comp, targetComp.getName()));
	}


	@Test
	public void testSearchForLoop() {

		Component c1 = new Component( "c1" );
		Assert.assertNull( ComponentHelpers.searchForLoop( c1 ));

		Component c11 = new Component( "c11" );
		c1.addChild( c11 );
		Assert.assertNull( ComponentHelpers.searchForLoop( c1 ));

		Component c12 = new Component( "c1" );
		c1.addChild( c12 );
		Assert.assertEquals( "c1 -> c1", ComponentHelpers.searchForLoop( c1 ));
		Assert.assertNull( ComponentHelpers.searchForLoop( c11 ));
		Assert.assertNull( ComponentHelpers.searchForLoop( c12 ));

		c12.setName( "c12" );
		Component c121 = new Component( "c1" );
		c12.addChild( c121 );
		Assert.assertEquals( "c1 -> c12 -> c1", ComponentHelpers.searchForLoop( c1 ));
		Assert.assertNull( ComponentHelpers.searchForLoop( c11 ));
		Assert.assertNull( ComponentHelpers.searchForLoop( c12 ));
		Assert.assertNull( ComponentHelpers.searchForLoop( c121 ));

		c121.setName( "c121" );
		c121.addChild( c1 );
		Assert.assertEquals( "c1 -> c12 -> c121 -> c1", ComponentHelpers.searchForLoop( c1 ));
		Assert.assertEquals( "c12 -> c121 -> c1 -> c12", ComponentHelpers.searchForLoop( c12 ));
		Assert.assertEquals( "c121 -> c1 -> c12 -> c121", ComponentHelpers.searchForLoop( c121 ));
		Assert.assertNull( ComponentHelpers.searchForLoop( c11 ));
	}


	@Test
	public void testFindAllComponents_simple() {

		ApplicationTemplate app = new ApplicationTemplate();
		Assert.assertEquals( 0, ComponentHelpers.findAllComponents( app ).size());

		Graphs graphs = new Graphs();
		app.setGraphs( graphs );
		Component comp1 = new Component( "comp1" );
		graphs.getRootComponents().add( comp1 );
		Assert.assertEquals( 1, ComponentHelpers.findAllComponents( app ).size());

		comp1.addChild( new Component( "comp-2" ));
		Assert.assertEquals( 2, ComponentHelpers.findAllComponents( app ).size());

		Component comp3 = new Component( "comp_3" );
		graphs.getRootComponents().add( comp3 );
		comp3.addChild( new Component( "comp-2" ));
		Assert.assertEquals( 3, ComponentHelpers.findAllComponents( app ).size());
	}


	@Test
	public void testFindAllComponents_complex1() {

		ApplicationTemplate app = ComplexApplicationFactory1.newApplication();
		Assert.assertEquals( 10, ComponentHelpers.findAllComponents( app ).size());
	}


	@Test
	public void testFindAllComponents_appInstance() {

		ApplicationTemplate tpl = ComplexApplicationFactory1.newApplication();
		Application app = new Application( tpl );
		Assert.assertEquals( 10, ComponentHelpers.findAllComponents( app ).size());

		app = new Application( null );
		Assert.assertEquals( 0, ComponentHelpers.findAllComponents( app ).size());
	}


	@Test
	public void testFindAllComponents_extended() {

		ApplicationTemplate app = ComplexApplicationFactory1.newApplication();
		app.getGraphs().getRootComponents().clear();

		Component root1 = new Component( "root1" ).installerName( Constants.TARGET_INSTALLER );
		Component root2 = new Component( "root2" );
		Component root3 = new Component( "root3" );
		Assert.assertEquals( 0, ComponentHelpers.findAllComponents( app ).size());

		app.getGraphs().getRootComponents().add( root1 );
		Assert.assertEquals( 1, ComponentHelpers.findAllComponents( app ).size());

		root2.extendComponent( root1 );
		Assert.assertEquals( 2, ComponentHelpers.findAllComponents( app ).size());

		root3.extendComponent( root2 );
		Assert.assertEquals( 3, ComponentHelpers.findAllComponents( app ).size());

		app.getGraphs().getRootComponents().add( root2 );
		app.getGraphs().getRootComponents().add( root3 );
		Assert.assertEquals( 3, ComponentHelpers.findAllComponents( app ).size());

		Component childComponent = new Component( "child" ).installerName( "script" );
		root3.addChild( childComponent );
		Assert.assertEquals( 4, ComponentHelpers.findAllComponents( app ).size());
	}


	@Test
	public void testFindAllChildren_complex1() {

		ApplicationTemplate app = ComplexApplicationFactory1.newApplication();
		Component root1 = ComponentHelpers.findComponent( app.getGraphs(), ComplexApplicationFactory1.ROOT_1 );
		Assert.assertNotNull( root1 );
		Assert.assertEquals( 1, root1.getChildren().size());

		Collection<Component> children = ComponentHelpers.findAllChildren( root1 );
		Assert.assertEquals( 5, children.size());
		Assert.assertTrue( children.contains( new Component( ComplexApplicationFactory1.GLASSFISH )));
		Assert.assertTrue( children.contains( new Component( ComplexApplicationFactory1.TOMCAT )));
		Assert.assertTrue( children.contains( new Component( ComplexApplicationFactory1.TOMCAT_8 )));
		Assert.assertTrue( children.contains( new Component( ComplexApplicationFactory1.MYSQL )));
		Assert.assertTrue( children.contains( new Component( ComplexApplicationFactory1.MONGO_DB )));

		Component root2 = ComponentHelpers.findComponent( app.getGraphs(), ComplexApplicationFactory1.ROOT_2 );
		Assert.assertNotNull( root2 );
		Assert.assertEquals( 0, root2.getChildren().size());

		children = ComponentHelpers.findAllChildren( root2 );
		Assert.assertEquals( 4, children.size());
		Assert.assertTrue( children.contains( new Component( ComplexApplicationFactory1.GLASSFISH )));
		Assert.assertTrue( children.contains( new Component( ComplexApplicationFactory1.TOMCAT )));
		Assert.assertTrue( children.contains( new Component( ComplexApplicationFactory1.TOMCAT_8 )));
		Assert.assertTrue( children.contains( new Component( ComplexApplicationFactory1.MYSQL )));

		Component c = ComponentHelpers.findComponent( app.getGraphs(), ComplexApplicationFactory1.GLASSFISH );
		Assert.assertNotNull( c );
		Assert.assertEquals( 0, c.getChildren().size());
		Assert.assertEquals( 0, ComponentHelpers.findAllChildren( c ).size());

		c = ComponentHelpers.findComponent( app.getGraphs(), ComplexApplicationFactory1.MONGO_DB );
		Assert.assertNotNull( c );
		Assert.assertEquals( 0, c.getChildren().size());
		Assert.assertEquals( 0, ComponentHelpers.findAllChildren( c ).size());

		c = ComponentHelpers.findComponent( app.getGraphs(), ComplexApplicationFactory1.TOMCAT_8 );
		Assert.assertNotNull( c );
		Assert.assertEquals( 1, c.getChildren().size());
		Assert.assertEquals( ComplexApplicationFactory1.APP_3, c.getChildren().iterator().next().getName());

		List<Component> lastChildren = new ArrayList<Component> ();
		lastChildren.addAll( ComponentHelpers.findAllChildren( c ));
		Assert.assertEquals( 3, lastChildren.size());

		Assert.assertEquals( ComplexApplicationFactory1.APP_1, lastChildren.get( 0 ).getName());
		Assert.assertEquals( ComplexApplicationFactory1.APP_2, lastChildren.get( 1 ).getName());
		Assert.assertEquals( ComplexApplicationFactory1.APP_3, lastChildren.get( 2 ).getName());
	}


	@Test
	public void testFindAllChildren_testApp() {

		TestApplicationTemplate app = new TestApplicationTemplate();

		Collection<Component> children = ComponentHelpers.findAllChildren( app.getTomcat().getComponent());
		Assert.assertEquals( 1, children.size());
		Assert.assertTrue( children.contains( app.getWar().getComponent()));

		children = ComponentHelpers.findAllChildren( app.getTomcatVm().getComponent());
		Assert.assertEquals( 2, children.size());
		Assert.assertTrue( children.contains( app.getTomcat().getComponent()));
		Assert.assertTrue( children.contains( app.getMySql().getComponent()));

		children = ComponentHelpers.findAllChildren( app.getMySqlVm().getComponent());
		Assert.assertEquals( 2, children.size());
		Assert.assertTrue( children.contains( app.getTomcat().getComponent()));
		Assert.assertTrue( children.contains( app.getMySql().getComponent()));

		Assert.assertEquals( 0, ComponentHelpers.findAllChildren( app.getMySql().getComponent()).size());
		Assert.assertEquals( 0, ComponentHelpers.findAllChildren( app.getWar().getComponent()).size());
	}


	@Test
	public void testFixVariableName() {

		Component comp = new Component( "coMp" );
		Assert.assertEquals( "coMp.ip", ComponentHelpers.fixVariableName( comp, "ip" ));
		Assert.assertEquals( "coMp.ip", ComponentHelpers.fixVariableName( comp, "coMp.ip" ));

		Facet facet = new Facet( "f" );
		Assert.assertEquals( "f.port", ComponentHelpers.fixVariableName( facet, "port" ));
		Assert.assertEquals( "f.ip", ComponentHelpers.fixVariableName( facet, "f.ip" ));
	}


	@Test
	public void testFindFacets() {

		ApplicationTemplate app = ComplexApplicationFactory1.newApplication();
		Component root1 = ComponentHelpers.findComponent( app.getGraphs(), ComplexApplicationFactory1.ROOT_1 );
		Assert.assertNotNull( root1 );
		Assert.assertEquals( 1, root1.getFacets().size());

		Facet vm = root1.getFacets().iterator().next();
		Assert.assertEquals( ComplexApplicationFactory1.FACET_VM, vm.getName());
		Assert.assertEquals( 1, vm.getChildren().size());

		Facet deployable = (Facet) vm.getChildren().iterator().next();
		Assert.assertEquals( ComplexApplicationFactory1.FACET_DEPLOYABLE, deployable.getName());

		Collection<Facet> facets = ComponentHelpers.findAllExtendingFacets( deployable );
		Assert.assertEquals( 4, facets.size());
		Assert.assertTrue( facets.contains( new Facet( ComplexApplicationFactory1.FACET_DATABASE )));
		Assert.assertTrue( facets.contains( new Facet( ComplexApplicationFactory1.FACET_JEE )));
		Assert.assertTrue( facets.contains( new Facet( ComplexApplicationFactory1.FACET_STORAGE )));
		Assert.assertTrue( facets.contains( new Facet( ComplexApplicationFactory1.FACET_WEB )));

		Assert.assertEquals( 0, ComponentHelpers.findAllExtendingFacets( vm ).size());
		Assert.assertEquals( 0, ComponentHelpers.findAllExtendedFacets( vm ).size());

		Facet web = findFacetByName( facets, ComplexApplicationFactory1.FACET_WEB );
		Assert.assertNotNull( web );
		Assert.assertEquals( 0, ComponentHelpers.findAllExtendingFacets( web ).size());

		Facet db = findFacetByName( facets, ComplexApplicationFactory1.FACET_DATABASE );
		Assert.assertNotNull( db );

		Facet storage = findFacetByName( facets, ComplexApplicationFactory1.FACET_STORAGE );
		Assert.assertNotNull( storage );

		facets = ComponentHelpers.findAllExtendingFacets( db );
		Assert.assertEquals( 1, facets.size());
		Assert.assertTrue( facets.contains( new Facet( ComplexApplicationFactory1.FACET_STORAGE )));

		facets = ComponentHelpers.findAllExtendedFacets( db );
		Assert.assertEquals( 1, facets.size());
		Assert.assertTrue( facets.contains( new Facet( ComplexApplicationFactory1.FACET_DEPLOYABLE )));

		facets = ComponentHelpers.findAllExtendingFacets( storage );
		Assert.assertEquals( 0, facets.size());

		facets = ComponentHelpers.findAllExtendedFacets( storage );
		Assert.assertEquals( 2, facets.size());
		Assert.assertTrue( facets.contains( new Facet( ComplexApplicationFactory1.FACET_DEPLOYABLE )));
		Assert.assertTrue( facets.contains( new Facet( ComplexApplicationFactory1.FACET_DATABASE )));
	}


	@Test
	public void testFindAllFacets() {

		ApplicationTemplate app = ComplexApplicationFactory1.newApplication();
		Component root1 = ComponentHelpers.findComponent( app.getGraphs(), ComplexApplicationFactory1.ROOT_1 );
		Assert.assertNotNull( root1 );

		Collection<Facet> facets = ComponentHelpers.findAllFacets( root1 );
		Assert.assertEquals( 1, facets.size());
		Assert.assertTrue( facets.contains( new Facet( ComplexApplicationFactory1.FACET_VM )));

		Component mongoDb = ComponentHelpers.findComponent( app.getGraphs(), ComplexApplicationFactory1.MONGO_DB );
		Assert.assertNotNull( mongoDb );
		facets = ComponentHelpers.findAllFacets( mongoDb );
		Assert.assertEquals( 0, facets.size());

		Component glassfish = ComponentHelpers.findComponent( app.getGraphs(), ComplexApplicationFactory1.GLASSFISH );
		Assert.assertNotNull( glassfish );
		facets = ComponentHelpers.findAllFacets( glassfish );
		Assert.assertEquals( 3, facets.size());
		Assert.assertTrue( facets.contains( new Facet( ComplexApplicationFactory1.FACET_JEE )));
		Assert.assertTrue( facets.contains( new Facet( ComplexApplicationFactory1.FACET_WEB )));
		Assert.assertTrue( facets.contains( new Facet( ComplexApplicationFactory1.FACET_DEPLOYABLE )));

		Component tomcat = ComponentHelpers.findComponent( app.getGraphs(), ComplexApplicationFactory1.TOMCAT );
		Assert.assertNotNull( tomcat );
		facets = ComponentHelpers.findAllFacets( tomcat );
		Assert.assertEquals( 2, facets.size());
		Assert.assertTrue( facets.contains( new Facet( ComplexApplicationFactory1.FACET_WEB )));
		Assert.assertTrue( facets.contains( new Facet( ComplexApplicationFactory1.FACET_DEPLOYABLE )));

		Component tomcat8 = ComponentHelpers.findComponent( app.getGraphs(), ComplexApplicationFactory1.TOMCAT_8 );
		Assert.assertNotNull( tomcat8 );
		facets = ComponentHelpers.findAllFacets( tomcat8 );
		Assert.assertEquals( 2, facets.size());
		Assert.assertTrue( facets.contains( new Facet( ComplexApplicationFactory1.FACET_WEB )));
		Assert.assertTrue( facets.contains( new Facet( ComplexApplicationFactory1.FACET_DEPLOYABLE )));
	}


	@Test
	public void testFindAllExtendedComponents_complex1() {

		ApplicationTemplate app = ComplexApplicationFactory1.newApplication();
		Component tomcat = ComponentHelpers.findComponent( app.getGraphs(), ComplexApplicationFactory1.TOMCAT );
		Assert.assertNotNull( tomcat );

		Collection<Component> extended = ComponentHelpers.findAllExtendedComponents( tomcat );
		Assert.assertEquals( 1, extended.size());
		Assert.assertTrue( extended.contains( tomcat ));

		Component tomcat8 = ComponentHelpers.findComponent( app.getGraphs(), ComplexApplicationFactory1.TOMCAT_8 );
		Assert.assertNotNull( tomcat8 );

		extended = ComponentHelpers.findAllExtendedComponents( tomcat8 );
		Assert.assertEquals( 2, extended.size());
		Assert.assertTrue( extended.contains( tomcat ));
		Assert.assertTrue( extended.contains( tomcat8 ));
	}


	@Test
	public void testFindAllExtendedComponents_cycle() {

		Component c1 = new Component( "c1" );
		Component c2 = new Component( "c2" );
		Component c3 = new Component( "c3" );

		c3.extendComponent( c2 );
		c2.extendComponent( c1 );

		Collection<Component> extended = ComponentHelpers.findAllExtendedComponents( c1 );
		Assert.assertEquals( 1, extended.size());

		extended = ComponentHelpers.findAllExtendedComponents( c2 );
		Assert.assertEquals( 2, extended.size());
		Assert.assertTrue( extended.contains( c1 ));
		Assert.assertTrue( extended.contains( c2 ));

		extended = ComponentHelpers.findAllExtendedComponents( c3 );
		Assert.assertEquals( 3, extended.size());
		Assert.assertTrue( extended.contains( c1 ));
		Assert.assertTrue( extended.contains( c2 ));
		Assert.assertTrue( extended.contains( c3 ));

		c1.extendComponent( c3 );
		Assert.assertEquals( 3, ComponentHelpers.findAllExtendedComponents( c1 ).size());
		Assert.assertEquals( 3, ComponentHelpers.findAllExtendedComponents( c2 ).size());
		Assert.assertEquals( 3, ComponentHelpers.findAllExtendedComponents( c3 ).size());
	}


	@Test
	public void testFindAllExtendingComponents_complex1() {

		ApplicationTemplate app = ComplexApplicationFactory1.newApplication();
		Component tomcat8 = ComponentHelpers.findComponent( app.getGraphs(), ComplexApplicationFactory1.TOMCAT_8 );
		Assert.assertNotNull( tomcat8 );

		Collection<Component> extending = ComponentHelpers.findAllExtendingComponents( tomcat8 );
		Assert.assertEquals( 0, extending.size());

		Component tomcat = ComponentHelpers.findComponent( app.getGraphs(), ComplexApplicationFactory1.TOMCAT );
		Assert.assertNotNull( tomcat );
		extending = ComponentHelpers.findAllExtendingComponents( tomcat );
		Assert.assertEquals( 1, extending.size());
		Assert.assertTrue( extending.contains( tomcat8 ));
	}


	@Test
	public void testFindAllExtendingComponents_cycle() {

		Component c1 = new Component( "c1" );
		Component c2 = new Component( "c2" );
		Component c3 = new Component( "c3" );

		c3.extendComponent( c2 );
		c2.extendComponent( c1 );

		Collection<Component> extending = ComponentHelpers.findAllExtendingComponents( c3 );
		Assert.assertEquals( 0, extending.size());

		extending = ComponentHelpers.findAllExtendingComponents( c2 );
		Assert.assertEquals( 1, extending.size());
		Assert.assertTrue( extending.contains( c3 ));

		extending = ComponentHelpers.findAllExtendingComponents( c1 );
		Assert.assertEquals( 2, extending.size());
		Assert.assertTrue( extending.contains( c2 ));
		Assert.assertTrue( extending.contains( c3 ));

		c1.extendComponent( c3 );
		Assert.assertEquals( 2, ComponentHelpers.findAllExtendingComponents( c1 ).size());
		Assert.assertEquals( 2, ComponentHelpers.findAllExtendingComponents( c2 ).size());
		Assert.assertEquals( 2, ComponentHelpers.findAllExtendingComponents( c3 ).size());
	}


	@Test
	public void testFindAllAncestors_testApp() {

		TestApplicationTemplate app = new TestApplicationTemplate();

		Collection<Component> ancestors = ComponentHelpers.findAllAncestors( app.getTomcat().getComponent());
		Assert.assertEquals( 1, ancestors.size());
		Assert.assertTrue( ancestors.contains( app.getMySqlVm().getComponent()));
		Assert.assertTrue( ancestors.contains( app.getTomcatVm().getComponent()));

		ancestors = ComponentHelpers.findAllAncestors( app.getMySql().getComponent());
		Assert.assertEquals( 1, ancestors.size());
		Assert.assertTrue( ancestors.contains( app.getMySqlVm().getComponent()));
		Assert.assertTrue( ancestors.contains( app.getTomcatVm().getComponent()));

		ancestors = ComponentHelpers.findAllAncestors( app.getWar().getComponent());
		Assert.assertEquals( 1, ancestors.size());
		Assert.assertTrue( ancestors.contains( app.getTomcat().getComponent()));

		Assert.assertEquals( 0, ComponentHelpers.findAllAncestors( app.getMySqlVm().getComponent()).size());
		Assert.assertEquals( 0, ComponentHelpers.findAllAncestors( app.getTomcatVm().getComponent()).size());
	}


	@Test
	public void testFindAllAncestors_complex1() {

		ApplicationTemplate app = ComplexApplicationFactory1.newApplication();

		Component root1 = ComponentHelpers.findComponent( app.getGraphs(), ComplexApplicationFactory1.ROOT_1 );
		Assert.assertNotNull( root1 );
		Collection<Component> ancestors = ComponentHelpers.findAllAncestors( root1 );
		Assert.assertEquals( 0, ancestors.size());

		Component root2 = ComponentHelpers.findComponent( app.getGraphs(), ComplexApplicationFactory1.ROOT_2 );
		Assert.assertNotNull( root2 );
		ancestors = ComponentHelpers.findAllAncestors( root2 );
		Assert.assertEquals( 0, ancestors.size());

		Component mongoDb = ComponentHelpers.findComponent( app.getGraphs(), ComplexApplicationFactory1.MONGO_DB );
		Assert.assertNotNull( mongoDb );
		ancestors = ComponentHelpers.findAllAncestors( mongoDb );
		Assert.assertEquals( 1, ancestors.size());
		Assert.assertTrue( ancestors.contains( root1 ));

		Component glassfish = ComponentHelpers.findComponent( app.getGraphs(), ComplexApplicationFactory1.GLASSFISH );
		Assert.assertNotNull( glassfish );
		ancestors = ComponentHelpers.findAllAncestors( glassfish );
		Assert.assertEquals( 2, ancestors.size());
		Assert.assertTrue( ancestors.contains( root1 ));
		Assert.assertTrue( ancestors.contains( root2 ));

		Component tomcat = ComponentHelpers.findComponent( app.getGraphs(), ComplexApplicationFactory1.TOMCAT );
		Assert.assertNotNull( tomcat );
		ancestors = ComponentHelpers.findAllAncestors( tomcat );
		Assert.assertEquals( 2, ancestors.size());
		Assert.assertTrue( ancestors.contains( root1 ));
		Assert.assertTrue( ancestors.contains( root2 ));

		Component tomcat8 = ComponentHelpers.findComponent( app.getGraphs(), ComplexApplicationFactory1.TOMCAT_8 );
		Assert.assertNotNull( tomcat8 );
		ancestors = ComponentHelpers.findAllAncestors( tomcat8 );
		Assert.assertEquals( 2, ancestors.size());
		Assert.assertTrue( ancestors.contains( root1 ));
		Assert.assertTrue( ancestors.contains( root2 ));
	}


	@Test
	public void testFindAllImportedVariables() {

		Component root = new Component( "root" );
		Component serverWithApp = new Component( "server with app" );
		Component serverWithAnotherApp = new Component( "server with another app" );
		Component db = new Component( "database" );

		root.addChild( db );
		root.addChild( serverWithAnotherApp );
		root.addChild( serverWithApp );

		serverWithApp.addImportedVariable( new ImportedVariable( "database.ip", false, false ));
		serverWithApp.addImportedVariable( new ImportedVariable( "database.port", true, false ));
		serverWithApp.addExportedVariable( new ExportedVariable( "url", "something" ));

		serverWithAnotherApp.extendComponent( serverWithApp );

		// Override the optional or mandatory aspect of an imported variable
		serverWithAnotherApp.addImportedVariable( new ImportedVariable( "database.port", false, false ));
		serverWithAnotherApp.addImportedVariable( new ImportedVariable( "whatever", false, false ));

		db.addExportedVariable( new ExportedVariable( "ip", null ));
		db.addExportedVariable( new ExportedVariable( "port", "3306" ));

		Assert.assertEquals( 0, ComponentHelpers.findAllImportedVariables( db ).size());
		Assert.assertEquals( 0, ComponentHelpers.findAllImportedVariables( root ).size());

		Map<String,ImportedVariable> imports = ComponentHelpers.findAllImportedVariables( serverWithApp );
		Assert.assertEquals( 2, imports.size());
		Assert.assertTrue( imports.containsKey( "database.ip" ));
		Assert.assertTrue( imports.containsKey( "database.port" ));
		Assert.assertTrue( imports.get( "database.port" ).isOptional());
		Assert.assertFalse( imports.get( "database.ip" ).isOptional());

		imports = ComponentHelpers.findAllImportedVariables( serverWithAnotherApp );
		Assert.assertEquals( 3, imports.size());
		Assert.assertTrue( imports.containsKey( "database.ip" ));
		Assert.assertTrue( imports.containsKey( "database.port" ));
		Assert.assertTrue( imports.containsKey( "whatever" ));
		Assert.assertFalse( imports.get( "database.ip" ).isOptional());
		Assert.assertFalse( imports.get( "database.port" ).isOptional());
		Assert.assertFalse( imports.get( "whatever" ).isOptional());
	}


	@Test
	public void testFindAllExportedVariables_simple() {

		Component comp = new Component( "comp" );
		Map<String,String> exports = ComponentHelpers.findAllExportedVariables( comp );
		Assert.assertEquals( 0, exports.size());

		comp.addExportedVariable( new ExportedVariable( "test", "ok" ));
		exports = ComponentHelpers.findAllExportedVariables( comp );
		Assert.assertEquals( 1, exports.size());
		Assert.assertEquals( "ok", exports.get( "comp.test" ));
	}


	@Test
	public void testFindAllExportedVariables_withFacet() {

		Component comp = new Component( "comp" );
		comp.addExportedVariable( new ExportedVariable( "test", "ok" ));

		Facet f = new Facet( "facet" );
		f.addExportedVariable( new ExportedVariable( "property", "value" ));
		comp.associateFacet( f );

		Map<String,String> exports = ComponentHelpers.findAllExportedVariables( comp );
		Assert.assertEquals( 3, exports.size());
		Assert.assertEquals( "ok", exports.get( "comp.test" ));
		Assert.assertEquals( "value", exports.get( "facet.property" ));
		Assert.assertEquals( "value", exports.get( "comp.property" ));

		// Override a facet property
		comp.addExportedVariable( new ExportedVariable( "facet.property", "something different" ));
		exports = ComponentHelpers.findAllExportedVariables( comp );
		Assert.assertEquals( 3, exports.size());
		Assert.assertEquals( "ok", exports.get( "comp.test" ));
		Assert.assertEquals( "something different", exports.get( "facet.property" ));
		Assert.assertEquals( "something different", exports.get( "comp.property" ));

		// Global override
		comp.exportedVariables.remove( "facet.property" );
		comp.addExportedVariable( new ExportedVariable( "property", "great" ));
		exports = ComponentHelpers.findAllExportedVariables( comp );

		Assert.assertEquals( 3, exports.size());
		Assert.assertEquals( "ok", exports.get( "comp.test" ));
		Assert.assertEquals( "great", exports.get( "facet.property" ));
		Assert.assertEquals( "great", exports.get( "comp.property" ));

		// Local override
		comp.exportedVariables.remove( "property" );
		comp.addExportedVariable( new ExportedVariable( "comp.property", "ha! ha!" ));
		exports = ComponentHelpers.findAllExportedVariables( comp );

		Assert.assertEquals( 3, exports.size());
		Assert.assertEquals( "ok", exports.get( "comp.test" ));
		Assert.assertEquals( "value", exports.get( "facet.property" ));
		Assert.assertEquals( "ha! ha!", exports.get( "comp.property" ));

		// Verify we find the same thing with the abstract method
		Map<String,String> abstractExports = ComponentHelpers.findAllExportedVariables((AbstractType) comp);
		Assert.assertEquals( abstractExports.size(), exports.size());

		abstractExports = ComponentHelpers.findAllExportedVariables((AbstractType) f);
		Assert.assertEquals( 1, abstractExports.size());
	}


	@Test
	public void testFindAllExportedVariables_withComponentExtension() {

		Component comp = new Component( "comp" );
		comp.addExportedVariable( new ExportedVariable( "test", "ok" ));

		Component parent = new Component( "parent" );
		parent.addExportedVariable( new ExportedVariable( "property", "value" ));
		comp.extendComponent( parent );

		Map<String,String> exports = ComponentHelpers.findAllExportedVariables( comp );
		Assert.assertEquals( 3, exports.size());
		Assert.assertEquals( "ok", exports.get( "comp.test" ));
		Assert.assertEquals( "value", exports.get( "parent.property" ));
		Assert.assertEquals( "value", exports.get( "comp.property" ));

		// Override a parent property
		comp.addExportedVariable( new ExportedVariable( "parent.property", "something different" ));
		exports = ComponentHelpers.findAllExportedVariables( comp );
		Assert.assertEquals( 3, exports.size());
		Assert.assertEquals( "ok", exports.get( "comp.test" ));
		Assert.assertEquals( "something different", exports.get( "parent.property" ));
		Assert.assertEquals( "something different", exports.get( "comp.property" ));

		// Global override
		comp.exportedVariables.remove( "parent.property" );
		comp.addExportedVariable( new ExportedVariable( "property", "great" ));
		exports = ComponentHelpers.findAllExportedVariables( comp );

		Assert.assertEquals( 3, exports.size());
		Assert.assertEquals( "ok", exports.get( "comp.test" ));
		Assert.assertEquals( "great", exports.get( "parent.property" ));
		Assert.assertEquals( "great", exports.get( "comp.property" ));

		// Local override
		comp.exportedVariables.remove( "property" );
		comp.addExportedVariable( new ExportedVariable( "comp.property", "ha! ha!" ));
		exports = ComponentHelpers.findAllExportedVariables( comp );

		Assert.assertEquals( 3, exports.size());
		Assert.assertEquals( "ok", exports.get( "comp.test" ));
		Assert.assertEquals( "value", exports.get( "parent.property" ));
		Assert.assertEquals( "ha! ha!", exports.get( "comp.property" ));
	}


	@Test
	public void testFindAllExportedVariables_veryComplex() {

		// Create a model
		Component root = new Component( "root" );
		Component serverWithApp = new Component( "server with app" );
		Component serverWithAnotherApp = new Component( "server with another app" );
		Component db = new Component( "database" );

		root.addChild( db );
		root.addChild( serverWithAnotherApp );
		root.addChild( serverWithApp );
		serverWithAnotherApp.extendComponent( serverWithApp );

		Facet serverFacet = new Facet( "server facet" );
		Facet anotherServerFacet = new Facet( "another server facet" );
		anotherServerFacet.extendFacet( serverFacet );
		serverFacet.addExportedVariable( new ExportedVariable( "url-suffix", "some/path" ));

		serverWithApp.associateFacet( anotherServerFacet );
		serverWithApp.addExportedVariable( new ExportedVariable( "ip", null ));
		serverWithApp.addExportedVariable( new ExportedVariable( serverWithApp.getName() + ".port", "8080" ));
		serverWithApp.addExportedVariable( new ExportedVariable( "env", "test" ));

		// Override the value of a variable that comes from a super facet/component
		serverWithAnotherApp.addExportedVariable( new ExportedVariable( serverFacet.getName() + ".url-suffix", "another/path" ));
		serverWithAnotherApp.addExportedVariable( new ExportedVariable( "whatever", "something" ));
		serverWithAnotherApp.addExportedVariable( new ExportedVariable( "env", "prod" ));

		db.addExportedVariable( new ExportedVariable( "ip", null ));
		db.addExportedVariable( new ExportedVariable( "port", "3306" ));

		// Check assertions
		Assert.assertEquals( 0, ComponentHelpers.findAllExportedVariables( root ).size());

		Map<String,String> exports = ComponentHelpers.findAllExportedVariables( db );
		Assert.assertEquals( 2, exports.size());
		Assert.assertTrue( exports.containsKey( "database.ip" ));
		Assert.assertNull( exports.get( "database.ip" ));
		Assert.assertEquals( "3306", exports.get( "database.port" ));

		exports = ComponentHelpers.findAllExportedVariables( serverWithApp );
		Assert.assertEquals( 5, exports.size());
		Assert.assertTrue( exports.containsKey( serverWithApp.getName() + ".ip" ));
		Assert.assertNull( exports.get( serverWithApp.getName() + ".ip" ));

		Assert.assertEquals( "8080", exports.get( serverWithApp.getName() + ".port" ));
		Assert.assertEquals( "test", exports.get( serverWithApp.getName() + ".env" ));
		Assert.assertEquals( "some/path", exports.get( serverWithApp.getName() + ".url-suffix" ));
		Assert.assertEquals( "some/path", exports.get( serverFacet.getName() + ".url-suffix" ));

		// Since there is inheritance, there will be variables duplication
		exports = ComponentHelpers.findAllExportedVariables( serverWithAnotherApp );
		Assert.assertEquals( 10, exports.size());
		Assert.assertTrue( exports.containsKey( serverWithApp.getName() + ".ip" ));
		Assert.assertNull( exports.get( serverWithApp.getName() + ".ip" ));

		Assert.assertTrue( exports.containsKey( serverWithAnotherApp.getName() + ".ip" ));
		Assert.assertNull( exports.get( serverWithAnotherApp.getName() + ".ip" ));

		Assert.assertEquals( "8080", exports.get( serverWithApp.getName() + ".port" ));
		Assert.assertEquals( "8080", exports.get( serverWithAnotherApp.getName() + ".port" ));

		Assert.assertEquals( "another/path", exports.get( serverFacet.getName() + ".url-suffix" ));
		Assert.assertEquals( "some/path", exports.get( serverWithApp.getName() + ".url-suffix" ));
		Assert.assertEquals( "some/path", exports.get( serverWithAnotherApp.getName() + ".url-suffix" ));

		Assert.assertEquals( "something", exports.get( serverWithAnotherApp.getName() + ".whatever" ));

		Assert.assertEquals( "prod", exports.get( serverWithApp.getName() + ".env" ));
		Assert.assertEquals( "prod", exports.get( serverWithAnotherApp.getName() + ".env" ));
	}


	@Test
	public void testExtractNames() {

		List<Component> components = new ArrayList<Component> ();
		components.add( new Component( "c1" ));
		components.add( new Component( "c2" ));

		List<String> names = ComponentHelpers.extractNames( components );
		Assert.assertEquals( 2, names.size());
		Assert.assertEquals( "c1", names.get( 0 ));
		Assert.assertEquals( "c2", names.get( 1 ));
	}


	@Test
	public void testFindComponentDependenciesFor_namesOnly() {
		ApplicationTemplate app = ComplexApplicationFactory1.newApplication();

		// Root with no dependency
		Component comp = ComponentHelpers.findComponent( app.getGraphs(), ComplexApplicationFactory1.ROOT_1 );
		Assert.assertNotNull( comp );

		Map<String,Boolean> map = ComponentHelpers.findComponentDependenciesFor( comp );
		Assert.assertEquals( 0, map.size());

		// Application with several dependencies
		comp = ComponentHelpers.findComponent( app.getGraphs(), ComplexApplicationFactory1.APP_1 );
		Assert.assertNotNull( comp );

		map = ComponentHelpers.findComponentDependenciesFor( comp );
		Assert.assertEquals( 1, map.size());

		Assert.assertTrue( map.containsKey( ComplexApplicationFactory1.FACET_DATABASE ));
		Assert.assertTrue( map.get( ComplexApplicationFactory1.FACET_DATABASE ));

		// Another application
		comp = ComponentHelpers.findComponent( app.getGraphs(), ComplexApplicationFactory1.APP_2 );
		Assert.assertNotNull( comp );

		map = ComponentHelpers.findComponentDependenciesFor( comp );
		Assert.assertEquals( 2, map.size());

		Assert.assertTrue( map.containsKey( ComplexApplicationFactory1.MYSQL ));
		Assert.assertFalse( map.get( ComplexApplicationFactory1.MYSQL ));

		Assert.assertTrue( map.containsKey( ComplexApplicationFactory1.MONGO_DB ));
		Assert.assertTrue( map.get( ComplexApplicationFactory1.MONGO_DB ));

		// And another one
		comp = ComponentHelpers.findComponent( app.getGraphs(), ComplexApplicationFactory1.APP_3 );
		Assert.assertNotNull( comp );

		map = ComponentHelpers.findComponentDependenciesFor( comp );
		Assert.assertEquals( 0, map.size());
	}


	@Test
	public void testFindComponentDependenciesFor_resolvedComponents() {
		ApplicationTemplate app = ComplexApplicationFactory1.newApplication();

		// Root with no dependency
		Component comp = ComponentHelpers.findComponent( app.getGraphs(), ComplexApplicationFactory1.ROOT_1 );
		Assert.assertNotNull( comp );

		Map<Component,Boolean> map = ComponentHelpers.findComponentDependenciesFor( comp, app );
		Assert.assertEquals( 0, map.size());

		// Application with several dependencies
		comp = ComponentHelpers.findComponent( app.getGraphs(), ComplexApplicationFactory1.APP_1 );
		Assert.assertNotNull( comp );

		map = ComponentHelpers.findComponentDependenciesFor( comp, app );
		Assert.assertEquals( 1, map.size());

		// We look for a Database facet. And only one component has it: MySql.
		Component key = new Component( ComplexApplicationFactory1.MYSQL );
		Assert.assertTrue( map.containsKey( key ));
		Assert.assertTrue( map.get( key ));

		// Another application
		comp = ComponentHelpers.findComponent( app.getGraphs(), ComplexApplicationFactory1.APP_2 );
		Assert.assertNotNull( comp );

		map = ComponentHelpers.findComponentDependenciesFor( comp, app );
		Assert.assertEquals( 2, map.size());

		key = new Component( ComplexApplicationFactory1.MYSQL );
		Assert.assertTrue( map.containsKey( key ));
		Assert.assertFalse( map.get( key ));

		key = new Component( ComplexApplicationFactory1.MONGO_DB );
		Assert.assertTrue( map.containsKey( key ));
		Assert.assertTrue( map.get( key ));

		// And another one
		comp = ComponentHelpers.findComponent( app.getGraphs(), ComplexApplicationFactory1.APP_3 );
		Assert.assertNotNull( comp );

		map = ComponentHelpers.findComponentDependenciesFor( comp, app );
		Assert.assertEquals( 0, map.size());
	}


	@Test
	public void testFindComponentsThatDependOn() {
		ApplicationTemplate app = ComplexApplicationFactory1.newApplication();

		// Root with no dependency
		Component comp = ComponentHelpers.findComponent( app.getGraphs(), ComplexApplicationFactory1.ROOT_1 );
		Assert.assertNotNull( comp );

		Map<Component,Boolean> map = ComponentHelpers.findComponentsThatDependOn( comp, app );
		Assert.assertEquals( 0, map.size());

		// Application with several dependencies
		comp = ComponentHelpers.findComponent( app.getGraphs(), ComplexApplicationFactory1.MYSQL );
		Assert.assertNotNull( comp );

		map = ComponentHelpers.findComponentsThatDependOn( comp, app );
		Assert.assertEquals( 2, map.size());

		Component key = new Component( ComplexApplicationFactory1.APP_1 );
		Assert.assertTrue( map.containsKey( key ));
		Assert.assertTrue( map.get( key ));

		key = new Component( ComplexApplicationFactory1.APP_2 );
		Assert.assertTrue( map.containsKey( key ));
		Assert.assertFalse( map.get( key ));

		// And another one
		comp = ComponentHelpers.findComponent( app.getGraphs(), ComplexApplicationFactory1.MONGO_DB );
		Assert.assertNotNull( comp );

		map = ComponentHelpers.findComponentsThatDependOn( comp, app );
		Assert.assertEquals( 1, map.size());

		key = new Component( ComplexApplicationFactory1.APP_2 );
		Assert.assertTrue( map.containsKey( key ));
		Assert.assertTrue( map.get( key ));

		// And again, another one
		comp = ComponentHelpers.findComponent( app.getGraphs(), ComplexApplicationFactory1.APP_3 );
		Assert.assertNotNull( comp );

		map = ComponentHelpers.findComponentsThatDependOn( comp, app );
		Assert.assertEquals( 0, map.size());
	}


	private Facet findFacetByName( Collection<Facet> facets, String facetName ) {

		Facet result = null;
		for( Facet f : facets ) {
			if( facetName.equals( f.getName())) {
				result = f;
				break;
			}
		}

		return result;
	}
}
