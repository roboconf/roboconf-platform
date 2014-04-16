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

package net.roboconf.dm.internal;

import net.roboconf.core.model.helpers.ComponentHelpers;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.runtime.Application;
import net.roboconf.core.model.runtime.Component;
import net.roboconf.core.model.runtime.Graphs;
import net.roboconf.core.model.runtime.Instance;

/**
 * @author Vincent Zurczak - Linagora
 */
public class TestApplication extends Application {

	private static final long serialVersionUID = -8616929491081715953L;
	private final Instance tomcatVm, mySqlVm, tomcat, mySql, war;


	/**
	 * Constructor.
	 */
	public TestApplication() {
		super();
		setName( "test-app" );
		setQualifier( "test" );

		// Root instances
		Component vmComponent = new Component( "vm" ).installerName( "iaas" ).alias( "A virtual machine" );
		this.tomcatVm = new Instance( "tomcat-vm" ).component( vmComponent );
		this.mySqlVm = new Instance( "mysql-vm" ).component( vmComponent );

		// Children instances
		Component tomcatComponent = new Component( "tomcat" ).installerName( "puppet" ).alias( "An application server" );
		this.tomcat = new Instance( "tomcat-server" ).component( tomcatComponent );

		Component mySqlComponent = new Component( "mysql" ).installerName( "puppet" ).alias( "A database" );
		this.mySql = new Instance( "mysql-server" ).component( mySqlComponent );

		Component warComponent = new Component( "war" ).installerName( "bash" ).alias( "A WAR application" );
		this.war = new Instance( "hello-world" ).component( warComponent );

		// Make the glue
		InstanceHelpers.insertChild( this.tomcatVm, this.tomcat );
		InstanceHelpers.insertChild( this.tomcat, this.war );
		InstanceHelpers.insertChild( this.mySqlVm, this.mySql );

		ComponentHelpers.insertChild( vmComponent, mySqlComponent );
		ComponentHelpers.insertChild( vmComponent, tomcatComponent );
		ComponentHelpers.insertChild( tomcatComponent, warComponent );

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
				&& obj instanceof TestApplication;
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}
}
