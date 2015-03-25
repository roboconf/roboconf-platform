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

package net.roboconf.core.internal.tests;

import net.roboconf.core.Constants;
import net.roboconf.core.dsl.ParsingConstants;
import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.Component;
import net.roboconf.core.model.beans.Facet;
import net.roboconf.core.model.beans.Graphs;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ComplexApplicationFactory1 {

	public static final String ROOT_1 = "root1";
	public static final String ROOT_2 = "root2";
	public static final String GLASSFISH = "glassfish";
	public static final String TOMCAT = "tomcat";
	public static final String TOMCAT_8 = "tomcat8";
	public static final String MYSQL = "MySql";
	public static final String MONGO_DB = "MongoDB";
	public static final String APP_1 = "app1";
	public static final String APP_2 = "app2";
	public static final String APP_3 = "app3";

	public static final String FACET_VM = "VM";
	public static final String FACET_DEPLOYABLE = "deployable";
	public static final String FACET_JEE = "JEE";
	public static final String FACET_WEB = "Web";
	public static final String FACET_DATABASE = "Database";
	public static final String FACET_STORAGE = "Storage";


	/**
	 * Constructor.
	 */
	private ComplexApplicationFactory1() {
		// nothing
	}


	/**
	 * Creates an application with component inheritance and facets.
	 * @return an application
	 */
	public static Application newApplication() {

		Application app = new Application( "name" ).qualifier( "test" ).namespace( "net.roboconf" ).dslId( ParsingConstants.DSL_VERSION );
		Graphs graph = new Graphs();
		app.setGraphs( graph );

		// Roots
		Facet facetVm = new Facet( FACET_VM );
		Facet facetDeployable = new Facet( FACET_DEPLOYABLE );
		facetVm.addChild( facetDeployable );
		facetVm.exportedVariables.put( Constants.SPECIFIC_VARIABLE_IP, null );

		Component root1 = new Component( ROOT_1 ).installerName( Constants.TARGET_INSTALLER );
		root1.associateFacet( facetVm );
		graph.getRootComponents().add( root1 );

		Component root2 = new Component( ROOT_2 ).installerName( Constants.TARGET_INSTALLER );
		root2.associateFacet( facetVm );
		graph.getRootComponents().add( root2 );

		// Servers and so on...
		Facet facetJee = new Facet( FACET_JEE );
		facetJee.exportedVariables.put( "server-suffix", "/path" );
		facetJee.extendFacet( facetDeployable );

		Facet facetWeb = new Facet( FACET_WEB );
		facetWeb.exportedVariables.put( "server-suffix", "/path" );
		facetWeb.extendFacet( facetDeployable );

		Facet facetDb = new Facet( FACET_DATABASE );
		facetDb.exportedVariables.put( "port", "3306" );
		facetDb.extendFacet( facetDeployable );

		Facet facetStorage = new Facet( FACET_STORAGE );
		facetStorage.extendFacet( facetDb );

		Component glassfish = new Component( GLASSFISH ).installerName( "puppet" );
		glassfish.associateFacet( facetWeb );
		glassfish.associateFacet( facetJee );

		Component tomcat = new Component( TOMCAT ).installerName( "puppet" );
		tomcat.associateFacet( facetWeb );

		Component tomcat8 = new Component( TOMCAT_8 );
		tomcat8.extendComponent( tomcat );

		// Add redundant variables for the MySQL type
		Component mySql = new Component( MYSQL ).installerName( "script" );
		mySql.exportedVariables.put( "ip", null );
		mySql.exportedVariables.put( "port", "3306" );
		mySql.associateFacet( facetDb );

		Component mongoWithoutFacet = new Component( MONGO_DB ).installerName( "Chef" );
		mongoWithoutFacet.exportedVariables.put( "ip", null );
		mongoWithoutFacet.exportedVariables.put( "port", "28017" );
		root1.addChild( mongoWithoutFacet );

		// Applications
		Component app1 = new Component( APP_1 ).installerName( "file" );
		app1.importedVariables.put( "Database.port", true );
		tomcat.addChild( app1 );

		Component app2 = new Component( APP_2 ).installerName( "file" );
		app2.importedVariables.put( "MySql.ip", false );
		app2.importedVariables.put( "MySql.port", true );
		app2.importedVariables.put( "MongoDB.ip", true );
		app2.importedVariables.put( "MongoDB.port", true );
		tomcat.addChild( app2 );

		Component app3 = new Component( APP_3 ).installerName( "file" );
		tomcat8.addChild( app3 );

		return app;
	}
}
