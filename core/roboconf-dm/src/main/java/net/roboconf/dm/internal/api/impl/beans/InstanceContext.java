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

package net.roboconf.dm.internal.api.impl.beans;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.roboconf.core.model.beans.AbstractApplication;
import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.helpers.InstanceHelpers;

/**
 * A class to ease searching when dealing when instances / components and several applications or templates.
 * @author Vincent Zurczak - Linagora
 */
public class InstanceContext {

	// Components names start with '@'.
	private String instancePathOrComponentName;
	private final String name, qualifier;


	/**
	 * Constructor.
	 * @param app
	 * @param inst
	 */
	public InstanceContext( AbstractApplication app, Instance inst ) {
		this( app );
		this.instancePathOrComponentName = inst == null ? null : InstanceHelpers.computeInstancePath( inst );
	}


	/**
	 * Constructor.
	 * @param name
	 * @param qualifier
	 * @param instancePathOrComponentName
	 */
	public InstanceContext( String name, String qualifier, String instancePathOrComponentName ) {
		this.name = name;
		this.qualifier = qualifier;
		this.instancePathOrComponentName = instancePathOrComponentName;
	}


	/**
	 * Constructor.
	 * @param app
	 */
	public InstanceContext( AbstractApplication app ) {
		this.name = app.getName();
		this.qualifier = app instanceof ApplicationTemplate ? ((ApplicationTemplate) app).getVersion() : null;
	}


	/**
	 * Constructor.
	 * @param app
	 * @param instancePathOrComponentName
	 */
	public InstanceContext( AbstractApplication app, String instancePathOrComponentName ) {
		this( app );
		this.instancePathOrComponentName = instancePathOrComponentName;
	}


	@Override
	public String toString() {
		return this.name + "::" + this.qualifier + "::" + this.instancePathOrComponentName;
	}


	/**
	 * Parses a string to resolve a target mapping key.
	 * @param s a string (can be null)
	 * @return a target mapping key (never null)
	 */
	public static InstanceContext parse( String s ) {

		String name = null, qualifier = null, instancePathOrComponentName = null;
		if( s != null ) {
			Matcher m = Pattern.compile( "(.*)::(.*)::(.*)" ).matcher( s );
			if( m.matches()) {
				name = m.group( 1 ).equals( "null" ) ? null : m.group( 1 );
				qualifier = m.group( 2 ).equals( "null" ) ? null : m.group( 2 );
				instancePathOrComponentName = m.group( 3 ).equals( "null" ) ? null : m.group( 3 );
			}
		}

		return new InstanceContext( name, qualifier, instancePathOrComponentName );
	}


	@Override
	public int hashCode() {
		int i1 = this.name == null ? 11 : this.name.hashCode();
		int i2 = this.qualifier == null ? 3 : this.qualifier.hashCode();
		return i1 + i2;
	}


	@Override
	public boolean equals( Object obj ) {
		return obj instanceof InstanceContext
				&& Objects.equals( this.name, ((InstanceContext) obj).name )
				&& Objects.equals( this.qualifier, ((InstanceContext) obj).qualifier )
				&& Objects.equals( this.instancePathOrComponentName, ((InstanceContext) obj).instancePathOrComponentName );
	}


	/**
	 * @return the name
	 */
	public String getName() {
		return this.name;
	}


	/**
	 * @return the qualifier
	 */
	public String getQualifier() {
		return this.qualifier;
	}


	/**
	 * @return the instancePathOrComponentName
	 */
	public String getInstancePathOrComponentName() {
		return this.instancePathOrComponentName;
	}
}
