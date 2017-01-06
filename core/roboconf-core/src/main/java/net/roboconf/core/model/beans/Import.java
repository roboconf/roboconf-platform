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

package net.roboconf.core.model.beans;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

import net.roboconf.core.model.helpers.InstanceHelpers;

/**
 * An Import is a list of variables an instance exports.
 * <p>
 * This structure is used by an instance to store what other instances
 * have exported.
 * </p>
 *
 * @author Noël - LIG
 */
public class Import implements Serializable {

	private static final long serialVersionUID = 1926254974053785327L;

	private final String instancePath;
	private final String componentName;
	private final Map<String,String> exportedVars = new HashMap<String,String> ();


	/**
	 * Constructor.
	 * @param instance
	 */
	public Import( Instance instance ) {
		this(
				InstanceHelpers.computeInstancePath( instance ),
				instance.getComponent() == null ? null : instance.getComponent().getName(),
				InstanceHelpers.findAllExportedVariables( instance ));
	}


	/**
	 * Constructor.
	 * @param instancePath
	 * @param componentName
	 */
	public Import( String instancePath, String componentName ) {
		this( instancePath, componentName, null );
	}


	/**
	 * Constructor.
	 * @param instancePath
	 * @param exportedVars
	 */
	public Import( String instancePath, String componentName, Map<String,String> exportedVars ) {
		this.instancePath = instancePath;
		this.componentName = componentName;
		if( exportedVars != null )
			this.exportedVars.putAll( exportedVars );
	}


	/**
	 * @return the exported variables (not null, key: variable name, value: variable value)
	 */
	public Map<String,String> getExportedVars() {
		return this.exportedVars;
	}


	/**
	 * @return the path of the instance that exports these variables
	 */
	public String getInstancePath() {
		return this.instancePath;
	}


	/**
	 * @return the componentName
	 */
	public String getComponentName() {
		return this.componentName;
	}


	@Override
	public boolean equals( Object obj ) {
		return obj instanceof Import
				&& Objects.equals( this.instancePath, ((Import) obj).instancePath );
	}


	@Override
	public int hashCode() {
		return this.instancePath.hashCode();
	}


	@Override
	public String toString() {

		StringBuilder sb = new StringBuilder();
		sb.append( this.instancePath );
		sb.append( " exports [ " );

		for( Iterator<Map.Entry<String,String>> it = this.exportedVars.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<String,String> entry = it.next();
			sb.append( entry.getKey());
			sb.append( "=" );
			sb.append( entry.getValue());
			if( it.hasNext())
				sb.append( ", " );
		}

		sb.append(" ] ");
		return sb.toString();
	}
}
