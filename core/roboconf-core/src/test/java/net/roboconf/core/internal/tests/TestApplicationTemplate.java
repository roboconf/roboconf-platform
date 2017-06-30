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

package net.roboconf.core.internal.tests;

import net.roboconf.core.Constants;
import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.core.model.beans.Component;
import net.roboconf.core.model.beans.ExportedVariable;
import net.roboconf.core.model.beans.Graphs;
import net.roboconf.core.model.beans.ImportedVariable;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.helpers.InstanceHelpers;

/**
 * @author Vincent Zurczak - Linagora
 */
public class TestApplicationTemplate extends ApplicationTemplate {

	private static final long serialVersionUID = -8616929491081715953L;
	private final Instance tomcatVm, mySqlVm, tomcat, mySql, war;


	/**
	 * Constructor.
	 */
	public TestApplicationTemplate() {
		super();
		setName( "test-app" );
		setVersion( "1.0.1" );

		// Root instances
		Component vmComponent = new Component( "vm" ).installerName( Constants.TARGET_INSTALLER );
		this.tomcatVm = new Instance( "tomcat-vm" ).component( vmComponent );
		this.mySqlVm = new Instance( "mysql-vm" ).component( vmComponent );

		// Children instances
		Component tomcatComponent = new Component( "tomcat" ).installerName( "puppet" );
		this.tomcat = new Instance( "tomcat-server" ).component( tomcatComponent );

		Component mySqlComponent = new Component( "mysql" ).installerName( "puppet" );
		mySqlComponent.addExportedVariable( new ExportedVariable( "port", "3306" ));
		mySqlComponent.addExportedVariable( new ExportedVariable( "ip", null ));
		this.mySql = new Instance( "mysql-server" ).component( mySqlComponent );

		Component warComponent = new Component( "war" ).installerName( "script" );
		warComponent.addExportedVariable( new ExportedVariable( "port", "8080" ));
		warComponent.addExportedVariable( new ExportedVariable( "ip", null ));
		warComponent.addImportedVariable( new ImportedVariable( "mysql.port", false, false ));
		warComponent.addImportedVariable( new ImportedVariable( "mysql.ip", false, false ));
		this.war = new Instance( "hello-world" ).component( warComponent );

		// Make the glue
		InstanceHelpers.insertChild( this.tomcatVm, this.tomcat );
		InstanceHelpers.insertChild( this.tomcat, this.war );
		InstanceHelpers.insertChild( this.mySqlVm, this.mySql );

		vmComponent.addChild( mySqlComponent );
		vmComponent.addChild( tomcatComponent );
		tomcatComponent.addChild( warComponent );

		setGraphs( new Graphs());
		getGraphs().getRootComponents().add( vmComponent );
		getRootInstances().add( this.mySqlVm );
		getRootInstances().add( this.tomcatVm );
	}


	/**
	 * @return the tomcatVm
	 */
	public Instance getTomcatVm() {
		return this.tomcatVm;
	}


	/**
	 * @return the mySqlVm
	 */
	public Instance getMySqlVm() {
		return this.mySqlVm;
	}


	/**
	 * @return the tomcat
	 */
	public Instance getTomcat() {
		return this.tomcat;
	}


	/**
	 * @return the mySql
	 */
	public Instance getMySql() {
		return this.mySql;
	}


	/**
	 * @return the war
	 */
	public Instance getWar() {
		return this.war;
	}


	@Override
	public boolean equals( Object obj ) {
		return super.equals( obj )
				&& obj instanceof TestApplicationTemplate;
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}
}
