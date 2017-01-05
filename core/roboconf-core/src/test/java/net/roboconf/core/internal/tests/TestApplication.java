/**
 * Copyright 2015-2017 Linagora, Université Joseph Fourier, Floralis
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

import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.helpers.InstanceHelpers;

/**
 * @author Vincent Zurczak - Linagora
 */
public class TestApplication extends Application {

	private static final long serialVersionUID = 6625970180959007726L;
	private final Instance tomcatVm, mySqlVm, tomcat, mySql, war;


	/**
	 * Constructor.
	 */
	public TestApplication() {
		super( "test", new TestApplicationTemplate());

		TestApplicationTemplate tpl = (TestApplicationTemplate) getTemplate();
		this.tomcatVm = InstanceHelpers.findInstanceByPath( this, InstanceHelpers.computeInstancePath( tpl.getTomcatVm()));
		this.mySqlVm = InstanceHelpers.findInstanceByPath( this, InstanceHelpers.computeInstancePath( tpl.getMySqlVm()));
		this.tomcat = InstanceHelpers.findInstanceByPath( this, InstanceHelpers.computeInstancePath( tpl.getTomcat()));
		this.mySql = InstanceHelpers.findInstanceByPath( this, InstanceHelpers.computeInstancePath( tpl.getMySql()));
		this.war = InstanceHelpers.findInstanceByPath( this, InstanceHelpers.computeInstancePath( tpl.getWar()));
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
		return obj instanceof TestApplication
				&& super.equals( obj );
	}


	@Override
	public int hashCode() {
		return super.hashCode();
	}
}
