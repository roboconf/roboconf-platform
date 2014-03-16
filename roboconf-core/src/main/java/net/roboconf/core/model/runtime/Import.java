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

package net.roboconf.core.model.runtime;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import net.roboconf.core.internal.utils.Utils;

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
	private final Map<String,String> exportedVars = new HashMap<String,String> ();


	/**
	 * Constructor.
	 * @param instancePath
	 */
	public Import( String instancePath ) {
		this.instancePath = instancePath;
	}


	/**
	 * Constructor.
	 * @param instancePath
	 * @param exportedVars
	 */
	public Import( String instancePath, Map<String,String> exportedVars ) {
		super();
		this.instancePath = instancePath;
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


	@Override
	public boolean equals( Object obj ) {
		return obj instanceof Import
				&& Utils.areEqual( this.instancePath, ((Import) obj).instancePath );
	}


	@Override
	public int hashCode() {
		return this.instancePath == null ? 17 : this.instancePath.hashCode();
	}


	@Override
	public String toString() {

		StringBuilder sb = new StringBuilder();
		sb.append( this.instancePath );
		sb.append( " exports [ " );

		for( Entry<String,String> entry : this.exportedVars.entrySet())
			sb.append(entry.getKey() + "=" + entry.getValue() + ", ");

		sb.append(" ] ");
		return sb.toString();
	}
}
