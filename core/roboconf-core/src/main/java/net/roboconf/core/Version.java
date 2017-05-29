/**
 * Copyright 2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.core;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Vincent Zurczak - Linagora
 */
public final class Version {

	private final int major, minor, patch;
	private final String qualifier;


	/**
	 * Constructor.
	 * @param major
	 * @param minor
	 * @param patch
	 * @param qualifier
	 */
	private Version( int major, int minor, int patch, String qualifier ) {
		this.major = major;
		this.minor = minor;
		this.patch = patch;
		this.qualifier = qualifier;
	}


	/**
	 * @return the major
	 */
	public int getMajor() {
		return this.major;
	}


	/**
	 * @return the minor
	 */
	public int getMinor() {
		return this.minor;
	}


	/**
	 * @return the patch
	 */
	public int getPatch() {
		return this.patch;
	}


	/**
	 * @return the qualifier
	 */
	public String getQualifier() {
		return this.qualifier;
	}


	/**
	 * Creates a version object from a string.
	 * @param rawVersion a version as a string
	 * @return null if the version is invalid, or an object otherwise
	 */
	public static Version parseVersion( String rawVersion ) {

		Version result = null;
		Matcher m = Pattern.compile( "^(\\d+)\\.(\\d+)(\\.\\d+)?([.-].+)?$" ).matcher( rawVersion );
		if( m.find()) {
			result = new Version(
					Integer.parseInt( m.group( 1 )),
					Integer.parseInt( m.group( 2 )),
					m.group( 3 ) == null ? 0 : Integer.parseInt( m.group( 3 ).substring( 1 )),
					m.group( 4 ) == null ? null : m.group( 4 ).substring( 1 ));
		}

		return result;
	}
}
